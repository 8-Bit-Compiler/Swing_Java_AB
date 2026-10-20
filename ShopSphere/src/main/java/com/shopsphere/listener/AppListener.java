package com.shopsphere.listener;

import com.shopsphere.util.Database;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AppListener implements ServletContextListener, HttpSessionListener {
  private final AtomicInteger sessions = new AtomicInteger();

  public void contextInitialized(ServletContextEvent e) {
    Database.initialize();
    e.getServletContext().setAttribute("appName", "ShopSphere");
    e.getServletContext().setAttribute("activeSessions", sessions);
  }

  public void contextDestroyed(ServletContextEvent e) {
    e.getServletContext().log("ShopSphere stopped");
  }

  public void sessionCreated(HttpSessionEvent e) {
    sessions.incrementAndGet();
  }

  public void sessionDestroyed(HttpSessionEvent e) {
    sessions.decrementAndGet();
  }
}
