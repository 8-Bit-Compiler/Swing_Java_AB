package com.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
    urlPatterns = {"/config"},
    initParams = {
        @WebInitParam(name = "course", value = "Java Servlet")
    }
)
public class ConfigContextServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        ServletConfig config = getServletConfig();
        String course = config.getInitParameter("course");
        
        ServletContext context = getServletContext();
        String appName = (String) context.getAttribute("appName");
        
        out.println("<html><head><title>Config and Context</title></head><body>");
        out.println("<h1>ServletConfig & ServletContext Example</h1>");
        out.println("<p>ServletConfig course: " + course + "</p>");
        out.println("<p>ServletContext appName: " + appName + "</p>");
        out.println("<p><a href='" + request.getContextPath() + "/index.html'>Back to Home</a></p>");
        out.println("</body></html>");
    }
}
