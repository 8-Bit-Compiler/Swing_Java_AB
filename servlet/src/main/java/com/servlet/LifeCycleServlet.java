package com.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/lifecycle")
public class LifeCycleServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("Servlet Initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Lifecycle Example</title></head><body>");
        out.println("<h1>Request Processed</h1>");
        out.println("<p>Check the Tomcat console for lifecycle messages.</p>");
        out.println("<p><a href='" + request.getContextPath() + "/index.html'>Back to Home</a></p>");
        out.println("</body></html>");
    }

    @Override
    public void destroy() {
        System.out.println("Servlet Destroyed");
        super.destroy();
    }
}
