# Conversion of HTML into JSP

## Objective
The practical converts an HTML form into JSP and demonstrates Servlet-JSP interaction using request attributes, `forward()`, and EL.

## Requirements
- JDK 17+
- Maven
- Apache Tomcat 11
- VS Code or another Java IDE

## Build
```bash
mvn clean package
```

## WAR
`target/servlet.war`

## Deployment
Copy:
`target/servlet.war`

to:
`Tomcat/webapps/`

Start:
`Tomcat/bin/startup.bat`

## Run
Open:
`http://localhost:8080/servlet/student.jsp`

## Flow
`student.jsp`
→ `HelloServlet`
→ `request.getParameter()`
→ `request.setAttribute()`
→ `forward()`
→ `result.jsp`
→ `EL`

## Example
Name: Bhavesh
Email: bhavesh@example.com
Course: B.Tech
Age: 20
