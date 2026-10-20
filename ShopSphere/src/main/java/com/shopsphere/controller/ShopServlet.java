package com.shopsphere.controller;

import com.shopsphere.dao.Jdbc;
import com.shopsphere.service.ShopService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ShopServlet extends HttpServlet {
  private final ShopService service = new ShopService();

  public void init() {
    getServletContext()
        .log("ShopSphere controller initialized: " + getServletConfig().getServletName());
  }

  private long number(HttpServletRequest r, String key) {
    String v = r.getParameter(key);
    return v == null || v.isBlank() ? 0 : Long.parseLong(v);
  }

  private String p(HttpServletRequest r, String key) {
    return r.getParameter(key);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> user(HttpServletRequest r) {
    return (Map<String, Object>) r.getSession().getAttribute("user");
  }

  private long uid(HttpServletRequest r) {
    return Jdbc.id(user(r));
  }

  private void view(HttpServletRequest r, HttpServletResponse s, String name) throws Exception {
    r.setAttribute("page", name);
    Object flash = r.getSession().getAttribute("flash");
    if (flash != null) {
      r.setAttribute("flash", flash);
      r.getSession().removeAttribute("flash");
    }
    r.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(r, s);
  }

  private void redirect(HttpServletRequest r, HttpServletResponse s, String path, String message)
      throws IOException {
    if (message != null) r.getSession().setAttribute("flash", message);
    s.sendRedirect(r.getContextPath() + path);
  }

  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      var session = r.getSession();
      String lang = p(r, "lang");
      if (List.of("en", "hi").contains(lang == null ? "" : lang))
        session.setAttribute("lang", lang);
      String language = (String) session.getAttribute("lang");
      if (language == null) language = "en";
      r.setAttribute(
          "messages", ResourceBundle.getBundle("messages", Locale.forLanguageTag(language)));
      r.setAttribute("categories", service.dao.categories());
      if (user(r) != null) {
        var cart = service.dao.cart(uid(r));
        r.setAttribute(
            "bagCount", cart.stream().mapToInt(x -> ((Number) x.get("quantity")).intValue()).sum());
      }
      String path = r.getServletPath();
      if (path.isEmpty()) path = "/";
      switch (path) {
        case "/":
        case "/products":
          r.setAttribute(
              "products", service.dao.products(p(r, "q"), p(r, "category"), p(r, "sort")));
          view(r, s, "store");
          break;
        case "/login":
        case "/register":
          view(r, s, path.substring(1));
          break;
        case "/product":
          {
            var product = service.dao.product(number(r, "id"));
            if (product == null || !Boolean.TRUE.equals(product.get("status"))) {
              s.sendError(404);
              return;
            }
            r.setAttribute("product", product);
            r.setAttribute("reviews", service.dao.reviews(number(r, "id")));
            r.setAttribute(
                "related",
                service.dao.products(null, product.get("category_id").toString(), null).stream()
                    .filter(x -> !x.get("id").equals(product.get("id")))
                    .limit(3)
                    .toList());
            var cookie = new Cookie("recentProduct", Long.toString(number(r, "id")));
            cookie.setMaxAge(60 * 60 * 24 * 30);
            cookie.setPath(r.getContextPath().isEmpty() ? "/" : r.getContextPath());
            cookie.setHttpOnly(true);
            cookie.setSecure(r.isSecure());
            cookie.setAttribute("SameSite", "Lax");
            s.addCookie(cookie);
            view(r, s, "product");
            break;
          }
        case "/cart":
        case "/checkout":
          {
            var cart = service.dao.cart(uid(r));
            r.setAttribute("cart", cart);
            r.setAttribute("subtotal", service.subtotal(cart));
            if ("/checkout".equals(path)) {
              if (cart.isEmpty()) {
                redirect(r, s, "/cart", "Your bag is empty.");
                return;
              }
              r.setAttribute("addresses", service.dao.addresses(uid(r)));
              if (session.getAttribute("checkoutToken") == null)
                session.setAttribute("checkoutToken", UUID.randomUUID().toString());
            }
            view(r, s, path.substring(1));
            break;
          }
        case "/wishlist":
          r.setAttribute("products", service.dao.wishlist(uid(r)));
          view(r, s, "wishlist");
          break;
        case "/orders":
          r.setAttribute("orders", service.dao.orders(uid(r)));
          view(r, s, "orders");
          break;
        case "/order":
          {
            var order =
                service.dao.order(number(r, "id"), uid(r), "ADMIN".equals(user(r).get("role")));
            if (order == null) {
              s.sendError(404);
              return;
            }
            r.setAttribute("order", order);
            r.setAttribute("items", service.dao.items(number(r, "id")));
            view(r, s, "order");
            break;
          }
        case "/profile":
          r.setAttribute("addresses", service.dao.addresses(uid(r)));
          view(r, s, "profile");
          break;
        case "/admin":
        case "/admin/":
        case "/admin/dashboard":
          r.setAttribute("stats", service.dao.stats());
          r.setAttribute("recentOrders", service.dao.list("orders").stream().limit(6).toList());
          view(r, s, "dashboard");
          break;
        case "/admin/products":
        case "/admin/categories":
        case "/admin/orders":
        case "/admin/users":
        case "/admin/coupons":
          {
            String section = path.substring(7);
            r.setAttribute("section", section);
            r.setAttribute("rows", service.dao.list(section));
            if (number(r, "edit") > 0)
              r.setAttribute(
                  "edit",
                  service.dao.list(section).stream()
                      .filter(x -> Jdbc.id(x) == number(r, "edit"))
                      .findFirst()
                      .orElse(null));
            view(r, s, "admin");
            break;
          }
        case "/demo":
          view(r, s, "demo");
          break;
        case "/demo/sql":
        case "/demo/xml":
        case "/demo/jsp":
          view(r, s, "demo-" + path.substring(6));
          break;
        case "/health":
          s.setContentType("application/json");
          s.getWriter().print("{\"status\":\"ok\",\"application\":\"ShopSphere\"}");
          break;
        default:
          s.sendError(404);
      }
    } catch (IllegalArgumentException e) {
      r.setAttribute("error", e.getMessage());
      try {
        view(r, s, "error");
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    String path = r.getServletPath();
    try {
      switch (path) {
        case "/login":
          {
            var u = service.login(p(r, "email"), p(r, "password"));
            r.changeSessionId();
            r.getSession().setAttribute("user", u);
            r.getSession().setAttribute("csrf", UUID.randomUUID().toString());
            redirect(
                r,
                s,
                "ADMIN".equals(u.get("role")) ? "/admin" : "/",
                "Welcome back, " + u.get("name") + ".");
            break;
          }
        case "/register":
          service.register(p(r, "name"), p(r, "email"), p(r, "password"));
          redirect(r, s, "/login", "Your account is ready. Sign in to start exploring.");
          break;
        case "/logout":
          r.getSession().invalidate();
          redirect(r, s, "/", "You have signed out.");
          break;
        case "/cart":
          service.cart(
              uid(r),
              number(r, "product"),
              (int) number(r, "quantity"),
              "add".equals(p(r, "action")));
          r.getSession().removeAttribute("checkoutToken");
          redirect(r, s, "/cart", "Your bag has been updated.");
          break;
        case "/wishlist":
          service.wishlist(uid(r), number(r, "product"));
          redirect(r, s, "/wishlist", "Your saved items have been updated.");
          break;
        case "/profile":
          {
            switch (Optional.ofNullable(p(r, "action")).orElse("profile")) {
              case "address":
                service.address(
                    uid(r),
                    p(r, "address_line"),
                    p(r, "city"),
                    p(r, "state"),
                    p(r, "pincode"),
                    p(r, "address_type"));
                break;
              case "delete-address":
                service.deleteAddress(uid(r), number(r, "id"));
                break;
              default:
                service.profile(uid(r), p(r, "name"), p(r, "mobile"));
                r.getSession().setAttribute("user", service.user(uid(r)));
            }
            redirect(
                r,
                s,
                "checkout".equals(p(r, "return")) ? "/checkout" : "/profile",
                "Your details have been saved.");
            break;
          }
        case "/checkout":
          {
            String token = p(r, "token");
            ShopService.require(
                Objects.equals(token, r.getSession().getAttribute("checkoutToken")),
                "Checkout form expired. Reopen checkout.");
            long order =
                service.checkout(
                    uid(r),
                    number(r, "address"),
                    p(r, "coupon"),
                    p(r, "payment"),
                    token,
                    "decline".equals(p(r, "mock_result")));
            redirect(
                r, s, "/order?id=" + order, "Order confirmed. Thank you for shopping with us!");
            break;
          }
        case "/review":
          service.review(uid(r), number(r, "product"), (int) number(r, "rating"), p(r, "body"));
          redirect(
              r, s, "/product?id=" + number(r, "product"), "Thanks for sharing your experience.");
          break;
        case "/admin/products":
          service.saveProduct(
              uid(r),
              number(r, "id"),
              number(r, "category"),
              p(r, "name"),
              p(r, "brand"),
              p(r, "description"),
              new BigDecimal(p(r, "price")),
              new BigDecimal(p(r, "discount")),
              (int) number(r, "stock"),
              p(r, "image"),
              "on".equals(p(r, "active")));
          redirect(r, s, path, "Product saved.");
          break;
        case "/admin/categories":
          service.category(
              uid(r),
              number(r, "id"),
              p(r, "name"),
              p(r, "description"),
              "on".equals(p(r, "active")));
          redirect(r, s, path, "Category saved.");
          break;
        case "/admin/coupons":
          service.coupon(
              uid(r),
              number(r, "id"),
              p(r, "code"),
              p(r, "type"),
              new BigDecimal(p(r, "value")),
              new BigDecimal(p(r, "minimum")),
              new BigDecimal(p(r, "maximum")),
              LocalDate.parse(p(r, "expiry")),
              "on".equals(p(r, "active")));
          redirect(r, s, path, "Coupon saved.");
          break;
        case "/admin/orders":
          service.orderStatus(uid(r), number(r, "id"), p(r, "status"));
          redirect(r, s, path, "Order updated.");
          break;
        case "/admin/users":
          service.userStatus(uid(r), number(r, "id"), "true".equals(p(r, "active")));
          redirect(r, s, path, "Customer updated.");
          break;
        default:
          s.sendError(404);
      }
    } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
      redirect(
          r,
          s,
          failurePath(path, r),
          e.getMessage() == null ? "Check the entered values." : e.getMessage());
    } catch (java.sql.SQLIntegrityConstraintViolationException e) {
      redirect(r, s, failurePath(path, r), "This record already exists or is still in use.");
    } catch (Exception e) {
      getServletContext().log("Request failed", e);
      redirect(r, s, failurePath(path, r), "The request could not be completed. Please try again.");
    }
  }

  private String failurePath(String path, HttpServletRequest r) {
    return switch (path) {
      case "/review" -> "/product?id=" + number(r, "product");
      case "/logout" -> "/";
      default -> path;
    };
  }

  public void destroy() {
    getServletContext().log("ShopSphere controller destroyed");
  }
}
