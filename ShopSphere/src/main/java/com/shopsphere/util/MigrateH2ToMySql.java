package com.shopsphere.util;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public final class MigrateH2ToMySql {
  private static final String DATABASE = "shopsphere";
  private static final String APP_USER = "shopsphere";
  private static final List<String> TABLES =
      List.of(
          "users",
          "categories",
          "products",
          "addresses",
          "cart",
          "wishlist",
          "coupons",
          "orders",
          "order_items",
          "payments",
          "reviews",
          "coupon_usage");

  public static void main(String[] args) throws Exception {
    Console console = System.console();
    if (console == null) {
      throw new IllegalStateException("Run this migration from an interactive Terminal.");
    }

    String sourceUrl =
        Database.env(
            "SHOP_DB_URL",
            "jdbc:h2:./data/shopsphere;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
    if (!sourceUrl.startsWith("jdbc:h2:")) {
      throw new IllegalArgumentException("SHOP_DB_URL must point to the existing H2 database.");
    }

    String adminUser = console.readLine("MySQL administrator username [root]: ").trim();
    if (adminUser.isEmpty()) adminUser = "root";
    char[] adminPassword = console.readPassword("MySQL administrator password: ");
    char[] appPassword = console.readPassword("New password for MySQL user shopsphere: ");
    char[] appPasswordAgain = console.readPassword("Re-enter the new shopsphere password: ");
    if (appPassword.length < 1 || !java.util.Arrays.equals(appPassword, appPasswordAgain)) {
      throw new IllegalArgumentException("The new MySQL password is empty or the entries differ.");
    }

    Properties adminProperties = new Properties();
    adminProperties.setProperty("user", adminUser);
    adminProperties.setProperty("password", new String(adminPassword));
    String serverUrl =
        "jdbc:mysql://127.0.0.1:3306/?serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";
    try (Connection source =
            DriverManager.getConnection(sourceUrl, Database.env("SHOP_DB_USER", "sa"),
                Database.env("SHOP_DB_PASSWORD", ""));
        Connection admin = DriverManager.getConnection(serverUrl, adminProperties)) {
      requireSourceSchema(source);
      ensureTargetIsSafe(admin);
      ensureAppUserAvailable(admin);
      try (Statement statement = admin.createStatement()) {
        statement.execute(
            "CREATE DATABASE IF NOT EXISTS shopsphere"
                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
      }
      String targetUrl =
          "jdbc:mysql://127.0.0.1:3306/shopsphere?serverTimezone=Asia/Kolkata"
              + "&allowPublicKeyRetrieval=true";
      try (Connection target = DriverManager.getConnection(targetUrl, adminProperties)) {
        createSchema(target);
        createAppUser(admin, appPassword);
        target.setAutoCommit(false);
        try {
          try (Statement statement = target.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
          }
          for (String table : TABLES) copyTable(source, target, table);
          try (Statement statement = target.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
          }
          target.commit();
        } catch (Exception e) {
          try {
            target.rollback();
          } catch (SQLException rollbackFailure) {
            e.addSuppressed(rollbackFailure);
          }
          try (Statement statement = admin.createStatement()) {
            statement.execute("DROP USER 'shopsphere'@'localhost'");
          } catch (SQLException cleanupFailure) {
            e.addSuppressed(cleanupFailure);
          }
          throw e;
        }
      }
    } finally {
      java.util.Arrays.fill(adminPassword, '\0');
      java.util.Arrays.fill(appPassword, '\0');
      java.util.Arrays.fill(appPasswordAgain, '\0');
      adminProperties.setProperty("password", "");
    }

    console.printf(
        "%nMigration completed. MySQL database: %s; app user: %s@localhost%n",
        DATABASE, APP_USER);
    console.printf("Use the same shopsphere password when starting ShopSphere:%n");
    console.printf(
        "export SHOP_DB_URL='jdbc:mysql://127.0.0.1:3306/shopsphere?serverTimezone=Asia/Kolkata'%n");
    console.printf("export SHOP_DB_USER=shopsphere%n");
    console.printf("read -s -p 'MySQL shopsphere password: ' SHOP_DB_PASSWORD; echo%n");
    console.printf("export SHOP_DB_PASSWORD%n");
    console.printf("./scripts/run.sh%n");
  }

  private static void requireSourceSchema(Connection source) throws SQLException {
    for (String table : TABLES) {
      try (PreparedStatement statement =
          source.prepareStatement(
              "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES"
                  + " WHERE TABLE_SCHEMA='public' AND TABLE_NAME=?")) {
        statement.setString(1, table);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next() || result.getInt(1) != 1) {
            throw new SQLException("Expected H2 table is missing: " + table);
          }
        }
      }
    }
  }

  private static void ensureTargetIsSafe(Connection admin) throws SQLException {
    try (PreparedStatement statement =
            admin.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?")) {
      statement.setString(1, DATABASE);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        if (result.getInt(1) == 0) return;
      }
    }

    try (PreparedStatement statement =
            admin.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=?"
                    + " AND table_type='BASE TABLE'")) {
      statement.setString(1, DATABASE);
      try (ResultSet tables = statement.executeQuery()) {
        while (tables.next()) {
          String table = tables.getString(1);
          try (Statement count = admin.createStatement();
              ResultSet rows =
                  count.executeQuery(
                      "SELECT COUNT(*) FROM `shopsphere`.`" + table.replace("`", "``") + "`")) {
            if (rows.next() && rows.getLong(1) > 0) {
              throw new SQLException(
                  "Refusing to overwrite existing MySQL data in "
                      + DATABASE
                      + "."
                      + table);
            }
          }
        }
      }
    }
  }

  private static void createSchema(Connection target) throws Exception {
    try (var input = MigrateH2ToMySql.class.getResourceAsStream("/schema.sql")) {
      if (input == null) throw new IllegalStateException("Could not load /schema.sql");
      String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      try (Statement statement = target.createStatement()) {
        for (String sql : schema.split(";")) {
          if (!sql.isBlank()) statement.execute(sql);
        }
      }
    }
  }

  private static void copyTable(Connection source, Connection target, String table)
      throws SQLException {
    String quotedTable = "`" + table + "`";
    String sql = "SELECT * FROM " + quotedTable;
    try (Statement select = source.createStatement();
        ResultSet rows = select.executeQuery(sql)) {
      ResultSetMetaData metadata = rows.getMetaData();
      int columnCount = metadata.getColumnCount();
      StringBuilder insert = new StringBuilder("INSERT INTO ").append(quotedTable).append(" (");
      for (int column = 1; column <= columnCount; column++) {
        if (column > 1) insert.append(',');
        insert.append('`').append(metadata.getColumnLabel(column)).append('`');
      }
      insert.append(") VALUES (");
      insert.append("?,".repeat(columnCount));
      insert.setLength(insert.length() - 1);
      insert.append(')');
      long copied = 0;
      try (PreparedStatement write = target.prepareStatement(insert.toString())) {
        while (rows.next()) {
          for (int column = 1; column <= columnCount; column++) {
            write.setObject(column, rows.getObject(column));
          }
          write.addBatch();
          if (++copied % 500 == 0) write.executeBatch();
        }
        write.executeBatch();
      }
      try (Statement count = target.createStatement();
          ResultSet result = count.executeQuery("SELECT COUNT(*) FROM " + quotedTable)) {
        result.next();
        if (result.getLong(1) != copied) {
          throw new SQLException("Row count mismatch while copying table " + table);
        }
      }
      System.out.printf("Copied %d rows from %s%n", copied, table);
    }
  }

  private static void ensureAppUserAvailable(Connection admin) throws SQLException {
    try (PreparedStatement statement =
            admin.prepareStatement(
                "SELECT COUNT(*) FROM mysql.user WHERE user=? AND host='localhost'")) {
      statement.setString(1, APP_USER);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        if (result.getInt(1) != 0) {
          throw new SQLException(
              "MySQL account shopsphere@localhost already exists; refusing to change its password.");
        }
      }
    }
  }

  private static void createAppUser(Connection admin, char[] appPassword) throws SQLException {
    String password = new String(appPassword).replace("'", "''");
    try (Statement statement = admin.createStatement()) {
      statement.execute(
          "CREATE USER 'shopsphere'@'localhost' IDENTIFIED BY '" + password + "'");
      statement.execute(
          "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES ON shopsphere.*"
              + " TO 'shopsphere'@'localhost'");
    }
  }
}
