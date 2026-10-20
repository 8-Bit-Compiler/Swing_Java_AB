package com.shopsphere.service;

import static com.shopsphere.dao.Jdbc.*;

import com.shopsphere.dao.StoreDao;
import com.shopsphere.util.*;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class ShopService {
  public final StoreDao dao = new StoreDao();

  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  public static String text(String value, int min, int max, String label) {
    value = value == null ? "" : value.trim();
    require(
        value.length() >= min && value.length() <= max,
        label + " must contain " + min + "–" + max + " characters.");
    return value;
  }

  public static BigDecimal money(Object v) {
    return new BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP);
  }

  private static void lockUser(Connection c, long user) throws SQLException {
    require(
        one(c, "SELECT id FROM users WHERE id=? AND status=TRUE FOR UPDATE", user) != null,
        "Account unavailable.");
  }

  public Map<String, Object> login(String email, String password) throws SQLException {
    try (var c = Database.open()) {
      var u =
          one(
              c,
              "SELECT * FROM users WHERE email=? AND status=TRUE",
              email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
      require(
          u != null
              && Passwords.verify(password == null ? "" : password, (String) u.get("password")),
          "Email or password is incorrect.");
      u.remove("password");
      return u;
    }
  }

  public void register(String name, String email, String password) throws SQLException {
    name = text(name, 2, 100, "Name");
    email = text(email, 5, 190, "Email").toLowerCase(Locale.ROOT);
    require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter a valid email address.");
    require(
        password != null && password.length() >= 8 && password.length() <= 128,
        "Password must contain 8–128 characters.");
    try (var c = Database.open()) {
      require(
          one(c, "SELECT id FROM users WHERE email=?", email) == null,
          "This email is already registered.");
      try {
        insert(
            c,
            "INSERT INTO users(name,email,password) VALUES(?,?,?)",
            name,
            email,
            Passwords.hash(password));
      } catch (SQLIntegrityConstraintViolationException e) {
        throw new IllegalArgumentException("This email is already registered.");
      }
    }
  }

  public Map<String, Object> user(long id) throws SQLException {
    try (var c = Database.open()) {
      return one(c, "SELECT id,name,email,mobile,role,status FROM users WHERE id=?", id);
    }
  }

  public void profile(long user, String name, String mobile) throws SQLException {
    name = text(name, 2, 100, "Name");
    mobile = text(mobile, 0, 20, "Mobile");
    require(mobile.isEmpty() || mobile.matches("[+0-9 ()-]{7,20}"), "Enter a valid mobile number.");
    try (var c = Database.open()) {
      update(c, "UPDATE users SET name=?,mobile=? WHERE id=?", name, mobile, user);
    }
  }

  public void cart(long user, long product, int quantity, boolean add) throws SQLException {
    require(quantity >= 0 && quantity <= 99, "Quantity must be between 0 and 99.");
    try (var c = Database.open()) {
      c.setAutoCommit(false);
      try {
        lockUser(c, user);
        if (!add && quantity == 0) {
          update(c, "DELETE FROM cart WHERE user_id=? AND product_id=?", user, product);
          c.commit();
          return;
        }
        var p =
            one(
                c,
                "SELECT p.* FROM products p JOIN categories cat ON cat.id=p.category_id WHERE"
                    + " p.id=? AND p.status=TRUE AND cat.status=TRUE",
                product);
        require(p != null, "Product unavailable.");
        var current =
            one(c, "SELECT quantity FROM cart WHERE user_id=? AND product_id=?", user, product);
        int next =
            quantity + (add && current != null ? ((Number) current.get("quantity")).intValue() : 0);
        require(
            next <= 99 && next <= ((Number) p.get("stock")).intValue(),
            "Requested quantity exceeds available stock (or the 99 item limit).");
        if (next == 0)
          update(c, "DELETE FROM cart WHERE user_id=? AND product_id=?", user, product);
        else if (current == null)
          insert(
              c,
              "INSERT INTO cart(user_id,product_id,quantity) VALUES(?,?,?)",
              user,
              product,
              next);
        else
          update(
              c,
              "UPDATE cart SET quantity=? WHERE user_id=? AND product_id=?",
              next,
              user,
              product);
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }

  public void wishlist(long user, long product) throws SQLException {
    try (var c = Database.open()) {
      c.setAutoCommit(false);
      try {
        lockUser(c, user);
        require(
            one(c, "SELECT id FROM products WHERE id=?", product) != null, "Product not found.");
        if (update(c, "DELETE FROM wishlist WHERE user_id=? AND product_id=?", user, product) == 0)
          insert(c, "INSERT INTO wishlist(user_id,product_id) VALUES(?,?)", user, product);
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }

  public long address(long user, String line, String city, String state, String pin, String type)
      throws SQLException {
    line = text(line, 5, 300, "Street address");
    city = text(city, 2, 100, "City");
    state = text(state, 2, 100, "State");
    require(
        pin != null && pin.matches("[1-9][0-9]{5}"), "Enter a valid six-digit Indian PIN code.");
    require(List.of("Home", "Work").contains(type), "Choose Home or Work.");
    try (var c = Database.open()) {
      return insert(
          c,
          "INSERT INTO addresses(user_id,address_line,city,state,pincode,address_type)"
              + " VALUES(?,?,?,?,?,?)",
          user,
          line,
          city,
          state,
          pin,
          type);
    }
  }

  public void deleteAddress(long user, long id) throws SQLException {
    try (var c = Database.open()) {
      require(
          one(c, "SELECT id FROM orders WHERE address_id=?", id) == null,
          "An order uses this address; keep it for your records.");
      update(c, "DELETE FROM addresses WHERE id=? AND user_id=?", id, user);
    }
  }

  public BigDecimal subtotal(List<Map<String, Object>> cart) {
    return cart.stream()
        .map(r -> money(r.get("line_total")))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Map<String, Object> coupon(Connection c, String code, long user, BigDecimal subtotal)
      throws SQLException {
    var cp =
        one(
            c,
            "SELECT * FROM coupons WHERE code=? AND status=TRUE FOR UPDATE",
            code.trim().toUpperCase(Locale.ROOT));
    require(cp != null, "Coupon not found.");
    require(
        !((java.sql.Date) cp.get("expiry_date")).toLocalDate().isBefore(LocalDate.now()),
        "This coupon has expired.");
    require(
        subtotal.compareTo(money(cp.get("minimum_order"))) >= 0,
        "Coupon minimum spend: ₹" + cp.get("minimum_order"));
    require(
        one(c, "SELECT id FROM coupon_usage WHERE coupon_id=? AND user_id=?", cp.get("id"), user)
            == null,
        "You have already used this coupon.");
    return cp;
  }

  public BigDecimal discount(Map<String, Object> cp, BigDecimal subtotal) {
    if (cp == null) return BigDecimal.ZERO;
    var value = money(cp.get("discount_value"));
    var amount =
        "PERCENT".equals(cp.get("discount_type"))
            ? subtotal.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
            : value;
    return amount.min(money(cp.get("maximum_discount"))).min(subtotal);
  }

  public long checkout(
      long user, long address, String code, String method, String token, boolean declined)
      throws SQLException {
    require(List.of("MOCK_CARD", "COD").contains(method), "Choose a valid payment method.");
    require(token != null && token.matches("[a-zA-Z0-9-]{20,100}"), "Invalid checkout request.");
    try (var c = Database.open()) {
      c.setAutoCommit(false);
      try {
        lockUser(c, user);
        var previous =
            one(c, "SELECT id FROM orders WHERE checkout_token=? AND user_id=?", token, user);
        if (previous != null) {
          c.commit();
          return id(previous);
        }
        var addr = one(c, "SELECT * FROM addresses WHERE id=? AND user_id=?", address, user);
        require(addr != null, "Select one of your saved addresses.");
        // Lock products in ID order before re-reading prices and availability.
        var ids = query(c, "SELECT product_id FROM cart WHERE user_id=? ORDER BY product_id", user);
        require(!ids.isEmpty(), "Your bag is empty.");
        for (var row : ids)
          one(c, "SELECT id FROM products WHERE id=? FOR UPDATE", row.get("product_id"));
        var rows = dao.cart(c, user);
        BigDecimal subtotal = subtotal(rows);
        for (var row : rows) {
          var cat = one(c, "SELECT status FROM categories WHERE id=?", row.get("category_id"));
          require(
              Boolean.TRUE.equals(row.get("status")) && Boolean.TRUE.equals(cat.get("status")),
              row.get("name") + " is no longer available.");
          require(
              ((Number) row.get("stock")).intValue() >= ((Number) row.get("quantity")).intValue(),
              row.get("name") + " has insufficient stock.");
        }
        var cp = code == null || code.isBlank() ? null : coupon(c, code, user, subtotal);
        BigDecimal discount = discount(cp, subtotal), total = subtotal.subtract(discount);
        require(
            !("MOCK_CARD".equals(method) && declined),
            "The mock payment was declined. No money was charged; your bag is unchanged.");
        String payment = "COD".equals(method) ? "PENDING" : "PAID";
        String snapshot =
            addr.get("address_line")
                + ", "
                + addr.get("city")
                + ", "
                + addr.get("state")
                + " "
                + addr.get("pincode");
        long order =
            insert(
                c,
                "INSERT INTO"
                    + " orders(user_id,address_id,shipping_address,subtotal,discount,total_amount,payment_method,payment_status,order_status,checkout_token)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                user,
                address,
                snapshot,
                subtotal,
                discount,
                total,
                method,
                payment,
                "PLACED",
                token);
        for (var row : rows) {
          int quantity = ((Number) row.get("quantity")).intValue();
          require(
              update(
                      c,
                      "UPDATE products SET stock=stock-? WHERE id=? AND stock>=?",
                      quantity,
                      row.get("id"),
                      quantity)
                  == 1,
              "Stock changed. Try again.");
          insert(
              c,
              "INSERT INTO order_items(order_id,product_id,product_name,quantity,price)"
                  + " VALUES(?,?,?,?,?)",
              order,
              row.get("id"),
              row.get("name"),
              quantity,
              row.get("sale_price"));
        }
        insert(
            c,
            "INSERT INTO"
                + " payments(order_id,payment_method,transaction_reference,amount,payment_status)"
                + " VALUES(?,?,?,?,?)",
            order,
            method,
            ("COD".equals(method) ? "COD-" : "MOCK-") + UUID.randomUUID(),
            total,
            payment);
        if (cp != null)
          insert(
              c,
              "INSERT INTO coupon_usage(coupon_id,user_id,order_id) VALUES(?,?,?)",
              cp.get("id"),
              user,
              order);
        update(c, "DELETE FROM cart WHERE user_id=?", user);
        c.commit();
        return order;
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }

  public void review(long user, long product, int rating, String body) throws SQLException {
    require(rating >= 1 && rating <= 5, "Rating must be 1–5.");
    body = text(body, 5, 1500, "Review");
    try (var c = Database.open()) {
      require(
          one(
                  c,
                  "SELECT oi.id FROM order_items oi JOIN orders o ON o.id=oi.order_id WHERE"
                      + " o.user_id=? AND oi.product_id=? AND o.order_status='DELIVERED'",
                  user,
                  product)
              != null,
          "Reviews are available after your order is delivered.");
      if (update(
              c,
              "UPDATE reviews SET rating=?,review_text=? WHERE user_id=? AND product_id=?",
              rating,
              body,
              user,
              product)
          == 0)
        insert(
            c,
            "INSERT INTO reviews(user_id,product_id,rating,review_text) VALUES(?,?,?,?)",
            user,
            product,
            rating,
            body);
    }
  }

  public void requireAdmin(long user) throws SQLException {
    var u = user(user);
    require(
        u != null && Boolean.TRUE.equals(u.get("status")) && "ADMIN".equals(u.get("role")),
        "Administrator access required.");
  }

  public void saveProduct(
      long actor,
      long id,
      long category,
      String name,
      String brand,
      String description,
      BigDecimal price,
      BigDecimal discount,
      int stock,
      String image,
      boolean active)
      throws SQLException {
    requireAdmin(actor);
    name = text(name, 2, 150, "Product name");
    brand = text(brand, 1, 100, "Brand");
    description = text(description, 5, 2000, "Description");
    require(
        price.signum() > 0 && price.compareTo(new BigDecimal("99999999")) <= 0,
        "Price must be positive and below ₹99,999,999.");
    require(
        discount.signum() >= 0 && discount.compareTo(new BigDecimal("100")) < 0,
        "Discount must be 0–99.99%.");
    require(stock >= 0 && stock <= 1000000, "Stock must be 0–1,000,000.");
    require(image != null && image.matches("/assets/[a-z-]+\\.svg"), "Choose a built-in image.");
    try (var c = Database.open()) {
      require(
          one(c, "SELECT id FROM categories WHERE id=?", category) != null, "Category not found.");
      if (id == 0)
        insert(
            c,
            "INSERT INTO"
                + " products(category_id,name,brand,description,price,discount,stock,image_url,status)"
                + " VALUES(?,?,?,?,?,?,?,?,?)",
            category,
            name,
            brand,
            description,
            price,
            discount,
            stock,
            image,
            active);
      else
        require(
            update(
                    c,
                    "UPDATE products SET"
                        + " category_id=?,name=?,brand=?,description=?,price=?,discount=?,stock=?,image_url=?,status=?"
                        + " WHERE id=?",
                    category,
                    name,
                    brand,
                    description,
                    price,
                    discount,
                    stock,
                    image,
                    active,
                    id)
                == 1,
            "Product not found.");
    }
  }

  public void category(long actor, long id, String name, String description, boolean active)
      throws SQLException {
    requireAdmin(actor);
    name = text(name, 2, 100, "Category name");
    description = text(description, 0, 500, "Description");
    try (var c = Database.open()) {
      if (id == 0)
        insert(
            c,
            "INSERT INTO categories(name,description,status) VALUES(?,?,?)",
            name,
            description,
            active);
      else
        update(
            c,
            "UPDATE categories SET name=?,description=?,status=? WHERE id=?",
            name,
            description,
            active,
            id);
    }
  }

  public void coupon(
      long actor,
      long id,
      String code,
      String type,
      BigDecimal value,
      BigDecimal minimum,
      BigDecimal max,
      LocalDate expiry,
      boolean active)
      throws SQLException {
    requireAdmin(actor);
    code = text(code, 3, 30, "Code").toUpperCase(Locale.ROOT);
    require(
        code.matches("[A-Z0-9_-]+"),
        "Use letters, numbers, underscores or hyphens for the coupon code.");
    require(List.of("PERCENT", "FIXED").contains(type), "Invalid discount type.");
    require(
        value.signum() > 0 && minimum.signum() >= 0 && max.signum() > 0, "Invalid coupon amounts.");
    require(
        !"PERCENT".equals(type) || value.compareTo(new BigDecimal("100")) <= 0,
        "Percentage cannot exceed 100.");
    try (var c = Database.open()) {
      if (id == 0)
        insert(
            c,
            "INSERT INTO"
                + " coupons(code,discount_type,discount_value,minimum_order,maximum_discount,expiry_date,status)"
                + " VALUES(?,?,?,?,?,?,?)",
            code,
            type,
            value,
            minimum,
            max,
            java.sql.Date.valueOf(expiry),
            active);
      else
        update(
            c,
            "UPDATE coupons SET"
                + " code=?,discount_type=?,discount_value=?,minimum_order=?,maximum_discount=?,expiry_date=?,status=?"
                + " WHERE id=?",
            code,
            type,
            value,
            minimum,
            max,
            java.sql.Date.valueOf(expiry),
            active,
            id);
    }
  }

  public void userStatus(long actor, long target, boolean active) throws SQLException {
    requireAdmin(actor);
    require(actor != target, "You cannot disable your own account.");
    try (var c = Database.open()) {
      require(
          one(c, "SELECT id FROM users WHERE id=? AND role='CUSTOMER'", target) != null,
          "Only customer accounts can be changed.");
      update(c, "UPDATE users SET status=? WHERE id=?", active, target);
    }
  }

  public void orderStatus(long actor, long order, String next) throws SQLException {
    requireAdmin(actor);
    try (var c = Database.open()) {
      c.setAutoCommit(false);
      try {
        var o = one(c, "SELECT * FROM orders WHERE id=? FOR UPDATE", order);
        require(o != null, "Order not found.");
        String old = (String) o.get("order_status");
        Map<String, List<String>> transitions =
            Map.of(
                "PLACED",
                List.of("PROCESSING", "CANCELLED"),
                "PROCESSING",
                List.of("SHIPPED", "CANCELLED"),
                "SHIPPED",
                List.of("DELIVERED"),
                "DELIVERED",
                List.of(),
                "CANCELLED",
                List.of());
        require(
            transitions.getOrDefault(old, List.of()).contains(next),
            "Invalid order transition: " + old + " → " + next);
        String payment = (String) o.get("payment_status");
        if ("CANCELLED".equals(next)) {
          for (var item :
              query(c, "SELECT * FROM order_items WHERE order_id=? ORDER BY product_id", order))
            update(
                c,
                "UPDATE products SET stock=stock+? WHERE id=?",
                item.get("quantity"),
                item.get("product_id"));
          payment = "PAID".equals(payment) ? "REFUNDED" : "CANCELLED";
          update(c, "DELETE FROM coupon_usage WHERE order_id=?", order);
        }
        if ("DELIVERED".equals(next) && "COD".equals(o.get("payment_method"))) payment = "PAID";
        update(
            c,
            "UPDATE orders SET order_status=?,payment_status=? WHERE id=?",
            next,
            payment,
            order);
        update(c, "UPDATE payments SET payment_status=? WHERE order_id=?", payment, order);
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }

  public boolean stock(long actor, long product, int quantity) throws SQLException {
    requireAdmin(actor);
    require(quantity >= 0, "Stock cannot be negative.");
    try (var c = Database.open()) {
      return update(c, "UPDATE products SET stock=? WHERE id=?", quantity, product) == 1;
    }
  }
}
