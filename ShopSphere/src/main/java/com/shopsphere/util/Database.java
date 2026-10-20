package com.shopsphere.util;

import com.shopsphere.dao.Jdbc;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class Database {
  public static String env(String k, String fallback) {
    return System.getProperty(k, System.getenv().getOrDefault(k, fallback));
  }

  public static Connection open() throws SQLException {
    return DriverManager.getConnection(
        env(
            "SHOP_DB_URL",
            "jdbc:h2:./data/shopsphere;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE"),
        env("SHOP_DB_USER", "sa"),
        env("SHOP_DB_PASSWORD", ""));
  }

  public static synchronized void initialize() {
    try (var c = open()) {
      String schema =
          new String(
              Database.class.getResourceAsStream("/schema.sql").readAllBytes(),
              StandardCharsets.UTF_8);
      for (String statement : schema.split(";"))
        if (!statement.isBlank()) Jdbc.update(c, statement);
      if (((Number) Jdbc.one(c, "SELECT COUNT(*) AS n FROM users").get("n")).intValue() == 0) {
        String admin = env("SHOP_ADMIN_PASSWORD", "Admin@12345");
        Jdbc.insert(
            c,
            "INSERT INTO users(name,email,password,role) VALUES(?,?,?,?)",
            "ShopSphere Admin",
            "admin@shopsphere.local",
            Passwords.hash(admin),
            "ADMIN");
        Jdbc.insert(
            c,
            "INSERT INTO users(name,email,password,role) VALUES(?,?,?,?)",
            "Alex Morgan",
            "alex@shopsphere.local",
            Passwords.hash("Shop@12345"),
            "CUSTOMER");
      }
      if (((Number) Jdbc.one(c, "SELECT COUNT(*) AS n FROM categories").get("n")).intValue() == 0) {
        String[] names = {"Audio", "Workspace", "Everyday", "Lifestyle"};
        for (String n : names)
          Jdbc.insert(
              c,
              "INSERT INTO categories(name,description) VALUES(?,?)",
              n,
              "Thoughtfully selected " + n.toLowerCase() + " essentials");
        Object[][] products = {
          {
            1,
            "Studio Wireless Headphones",
            "SONORA",
            "Your own quiet corner. Rich, balanced sound, active noise cancellation and 40 hours of"
                + " listening, wrapped in soft vegan leather.",
            7999,
            20,
            24,
            "headphones"
          },
          {
            2,
            "Arc Desk Lamp",
            "FORM & FIELD",
            "A softer light for brighter ideas. A sculptural adjustable desk lamp with three warm"
                + " light settings and a weighted aluminium base.",
            3499,
            10,
            18,
            "lamp"
          },
          {
            3,
            "Everyday Carry Backpack",
            "NOMAD",
            "Go light, carry everything. Weather-resistant recycled fabric, a padded laptop sleeve"
                + " and thoughtful pockets for your daily essentials.",
            4299,
            15,
            32,
            "backpack"
          },
          {
            1,
            "Pocket Bluetooth Speaker",
            "SONORA",
            "Big sound. Small footprint. A water-resistant portable speaker with 12-hour battery"
                + " life and a beautifully tactile finish.",
            2499,
            0,
            40,
            "speaker"
          },
          {
            2,
            "Mechanical Keyboard",
            "KEYFORM",
            "Make every keystroke count. Compact wireless keyboard with quiet tactile switches,"
                + " warm backlighting and a rechargeable battery.",
            5999,
            10,
            15,
            "keyboard"
          },
          {
            4,
            "Daily Ceramic Cup",
            "EARTHEN",
            "Slow mornings start here. A hand-finished 350 ml ceramic cup with a comfortable handle"
                + " and a smooth matte glaze.",
            899,
            0,
            50,
            "cup"
          },
          {
            3,
            "Transit Watch",
            "HOUR STUDIO",
            "Time, considered. A clean analogue dial, Japanese quartz movement and a soft"
                + " interchangeable silicone strap.",
            3999,
            20,
            8,
            "watch"
          },
          {
            4,
            "Insulated Water Bottle",
            "NOMAD",
            "A little better, every day. Double-wall stainless steel keeps your drink cold for 24"
                + " hours. 750 ml, leakproof and made to last.",
            1499,
            10,
            45,
            "bottle"
          }
        };
        for (Object[] p : products)
          Jdbc.insert(
              c,
              "INSERT INTO"
                  + " products(category_id,name,brand,description,price,discount,stock,image_url)"
                  + " VALUES(?,?,?,?,?,?,?,?)",
              p[0],
              p[1],
              p[2],
              p[3],
              p[4],
              p[5],
              p[6],
              "/assets/" + p[7] + ".svg");
        Jdbc.insert(
            c,
            "INSERT INTO"
                + " coupons(code,discount_type,discount_value,minimum_order,maximum_discount,expiry_date)"
                + " VALUES(?,?,?,?,?,?)",
            "WELCOME10",
            "PERCENT",
            10,
            1000,
            1000,
            java.sql.Date.valueOf("2030-12-31"));
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "Database initialization failed. Check SHOP_DB_* settings.", e);
    }
  }
}
