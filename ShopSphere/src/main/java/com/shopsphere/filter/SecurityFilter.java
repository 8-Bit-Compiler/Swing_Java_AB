package com.shopsphere.filter;

import com.shopsphere.service.ShopService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class SecurityFilter implements Filter {
  private final ShopService service = new ShopService();

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var req = (HttpServletRequest) request;
    var res = (HttpServletResponse) response;
    req.setCharacterEncoding("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    res.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; style-src 'self'; img-src 'self' data:; script-src 'self'; base-uri"
            + " 'self'; form-action 'self'; frame-ancestors 'none'");
    String path = req.getServletPath();
    if (path.startsWith("/assets/")) {
      chain.doFilter(req, res);
      return;
    }
    res.setHeader("Cache-Control", "no-store");
    var session = req.getSession();
    if (session.getAttribute("csrf") == null)
      session.setAttribute("csrf", UUID.randomUUID().toString());
    try {
      var user = (Map<?, ?>) session.getAttribute("user");
      if (user != null) {
        var fresh = service.user(((Number) user.get("id")).longValue());
        if (fresh == null || !Boolean.TRUE.equals(fresh.get("status"))) {
          session.removeAttribute("user");
          user = null;
        } else {
          session.setAttribute("user", fresh);
          user = fresh;
        }
      }
      boolean protectedPath =
          List.of("/cart", "/wishlist", "/checkout", "/orders", "/order", "/profile", "/review")
                  .contains(path)
              || path.startsWith("/admin")
              || path.startsWith("/demo");
      if (protectedPath && user == null) {
        session.setAttribute("flash", "Please sign in to continue.");
        res.sendRedirect(req.getContextPath() + "/login");
        return;
      }
      if ((path.startsWith("/admin") || path.startsWith("/demo"))
          && !"ADMIN".equals(user.get("role"))) {
        res.sendError(403, "Administrator access required.");
        return;
      }
      if ("POST".equals(req.getMethod())
          && !Objects.equals(session.getAttribute("csrf"), req.getParameter("csrf"))) {
        res.sendError(403, "Form expired. Reload the page and try again.");
        return;
      }
      chain.doFilter(req, res);
    } catch (java.sql.SQLException e) {
      throw new ServletException(e);
    }
  }
}
