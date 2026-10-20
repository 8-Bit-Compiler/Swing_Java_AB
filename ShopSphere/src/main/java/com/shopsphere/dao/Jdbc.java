package com.shopsphere.dao;

import java.sql.*;
import java.util.*;

public final class Jdbc {
  public static List<Map<String, Object>> query(Connection c, String sql, Object... args)
      throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      bind(ps, args);
      try (var rs = ps.executeQuery()) {
        var list = new ArrayList<Map<String, Object>>();
        var md = rs.getMetaData();
        while (rs.next()) {
          var row = new LinkedHashMap<String, Object>();
          for (int i = 1; i <= md.getColumnCount(); i++)
            row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
          list.add(row);
        }
        return list;
      }
    }
  }

  public static Map<String, Object> one(Connection c, String sql, Object... args)
      throws SQLException {
    var rows = query(c, sql, args);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      bind(ps, args);
      return ps.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args) throws SQLException {
    try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(ps, args);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
  }

  public static long id(Map<String, Object> m) {
    return ((Number) m.get("id")).longValue();
  }
}
