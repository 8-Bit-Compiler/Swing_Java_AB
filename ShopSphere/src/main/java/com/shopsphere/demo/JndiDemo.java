package com.shopsphere.demo;

import javax.naming.InitialContext;
import javax.sql.DataSource;

public final class JndiDemo {
  /** Run inside a configured servlet container. See docs/SETUP.md. */
  public static String inspect() throws Exception {
    var context = new InitialContext();
    try {
      var ds = (DataSource) context.lookup("java:comp/env/jdbc/ShopSphere");
      try (var c = ds.getConnection()) {
        return "JNDI DataSource: " + c.getMetaData().getDatabaseProductName();
      }
    } finally {
      context.close();
    }
  }
}
