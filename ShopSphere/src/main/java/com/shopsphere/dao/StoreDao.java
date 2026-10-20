package com.shopsphere.dao;

import static com.shopsphere.dao.Jdbc.*;

import com.shopsphere.util.Database;
import java.sql.*;
import java.util.*;

public final class StoreDao {
  public static final String PRODUCT_SELECT =
      "SELECT p.*, c.name AS category, ROUND(p.price*(100-p.discount)/100,2) AS sale_price, (SELECT"
          + " ROUND(AVG(r.rating),1) FROM reviews r WHERE r.product_id=p.id) AS rating, (SELECT"
          + " COUNT(*) FROM reviews r WHERE r.product_id=p.id) AS review_count FROM products p JOIN"
          + " categories c ON c.id=p.category_id ";

  public List<Map<String, Object>> products(String search, String category, String sort)
      throws SQLException {
    String ordering =
        switch (sort == null ? "" : sort) {
          case "price-low" -> "sale_price ASC";
          case "price-high" -> "sale_price DESC";
          case "name" -> "p.name ASC";
          default -> "p.id ASC";
        };
    var args = new ArrayList<Object>();
    String sql = PRODUCT_SELECT + "WHERE p.status=TRUE AND c.status=TRUE";
    if (search != null && !search.isBlank()) {
      sql += " AND (LOWER(p.name) LIKE ? OR LOWER(p.brand) LIKE ? OR LOWER(p.description) LIKE ?)";
      for (int i = 0; i < 3; i++) args.add("%" + search.toLowerCase(Locale.ROOT) + "%");
    }
    if (category != null && !category.isBlank()) {
      sql += " AND c.id=?";
      args.add(Long.parseLong(category));
    }
    try (var c = Database.open()) {
      return query(c, sql + " ORDER BY " + ordering, args.toArray());
    }
  }

  public Map<String, Object> product(long id) throws SQLException {
    try (var c = Database.open()) {
      return one(c, PRODUCT_SELECT + " WHERE p.id=?", id);
    }
  }

  public List<Map<String, Object>> categories() throws SQLException {
    try (var c = Database.open()) {
      return query(c, "SELECT * FROM categories ORDER BY id");
    }
  }

  public List<Map<String, Object>> cart(long user) throws SQLException {
    try (var c = Database.open()) {
      return cart(c, user);
    }
  }

  public List<Map<String, Object>> cart(Connection c, long user) throws SQLException {
    return query(
        c,
        "SELECT p.*, ct.quantity, ROUND(p.price*(100-p.discount)/100,2) AS sale_price,"
            + " ROUND(p.price*(100-p.discount)/100,2)*ct.quantity AS line_total FROM cart ct JOIN"
            + " products p ON p.id=ct.product_id WHERE ct.user_id=? ORDER BY p.id",
        user);
  }

  public List<Map<String, Object>> wishlist(long user) throws SQLException {
    try (var c = Database.open()) {
      return query(
          c,
          PRODUCT_SELECT
              + "JOIN wishlist w ON w.product_id=p.id WHERE w.user_id=? ORDER BY w.id DESC",
          user);
    }
  }

  public List<Map<String, Object>> addresses(long user) throws SQLException {
    try (var c = Database.open()) {
      return query(c, "SELECT * FROM addresses WHERE user_id=? ORDER BY id DESC", user);
    }
  }

  public List<Map<String, Object>> orders(long user) throws SQLException {
    try (var c = Database.open()) {
      return query(c, "SELECT * FROM orders WHERE user_id=? ORDER BY id DESC", user);
    }
  }

  public Map<String, Object> order(long id, long user, boolean admin) throws SQLException {
    try (var c = Database.open()) {
      return admin
          ? one(c, "SELECT * FROM orders WHERE id=?", id)
          : one(c, "SELECT * FROM orders WHERE id=? AND user_id=?", id, user);
    }
  }

  public List<Map<String, Object>> items(long order) throws SQLException {
    try (var c = Database.open()) {
      return query(c, "SELECT * FROM order_items WHERE order_id=? ORDER BY id", order);
    }
  }

  public List<Map<String, Object>> reviews(long id) throws SQLException {
    try (var c = Database.open()) {
      return query(
          c,
          "SELECT r.*,u.name FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.product_id=?"
              + " ORDER BY r.id DESC",
          id);
    }
  }

  public List<Map<String, Object>> list(String kind) throws SQLException {
    String sql =
        switch (kind) {
          case "products" -> PRODUCT_SELECT + "ORDER BY p.id DESC";
          case "orders" ->
              "SELECT o.*,u.name,u.email FROM orders o JOIN users u ON u.id=o.user_id ORDER BY o.id"
                  + " DESC";
          case "users" ->
              "SELECT id,name,email,mobile,role,status,created_at FROM users ORDER BY id DESC";
          case "coupons" -> "SELECT * FROM coupons ORDER BY id DESC";
          case "categories" -> "SELECT * FROM categories ORDER BY id";
          default -> throw new IllegalArgumentException("Unknown section");
        };
    try (var c = Database.open()) {
      return query(c, sql);
    }
  }

  public Map<String, Object> stats() throws SQLException {
    try (var c = Database.open()) {
      var stats = new LinkedHashMap<String, Object>();
      stats.put(
          "revenue",
          one(
                  c,
                  "SELECT COALESCE(SUM(total_amount),0) AS n FROM orders WHERE order_status <>"
                      + " 'CANCELLED'")
              .get("n"));
      for (String name : List.of("orders", "users", "products"))
        stats.put(name, one(c, "SELECT COUNT(*) AS n FROM " + name).get("n"));
      stats.put(
          "low_stock",
          query(c, "SELECT * FROM products WHERE stock < 10 AND status=TRUE ORDER BY stock"));
      return stats;
    }
  }
}
