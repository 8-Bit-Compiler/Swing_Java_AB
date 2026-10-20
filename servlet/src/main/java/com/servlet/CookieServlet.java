package com.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/cookie")
public class CookieServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        
        Cookie[] cookies = request.getCookies();
        String username = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("username".equals(cookie.getName())) {
                    username = cookie.getValue();
                    break;
                }
            }
        }
        
        if (username == null) {
            username = "Student";
            Cookie cookie = new Cookie("username", username);
            cookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(cookie);
        }
        
        username = escapeHtml(username);
        
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Cookie Example</title></head><body>");
        out.println("<h1>Cookie Example</h1>");
        out.println("<p>Username stored in cookie: " + username + "</p>");
        out.println("<p>Refreshing the page allows the browser to send the cookie back.</p>");
        out.println("<p><a href='" + request.getContextPath() + "/index.html'>Back to Home</a></p>");
        out.println("</body></html>");
    }
    
    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
