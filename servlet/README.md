# Student Servlet Application

## Project Description
A complete Java Servlet practical project demonstrating core Servlet concepts including HTTP methods, Form processing, Session Management, Filters, Listeners, and Cookies.

## Requirements
- Java 17+
- Maven
- Apache Tomcat 11
- No Spring, JSP, Database, or frontend frameworks

## Project Structure
Standard Maven Web App (`war`) structure.
- `src/main/java/com/servlet` - Contains Java Servlets, Filters, and Listeners.
- `src/main/webapp` - Contains HTML resources.

## How to Build
Use Maven to build the project:
```sh
mvn clean package
```
This produces `target/servlet.war`.

## How to Deploy
1. Copy `target/servlet.war` to your Tomcat 11 `webapps/` directory.
2. The application will be extracted to `webapps/servlet/`.

## How to Start Tomcat on Windows
Run the following from your terminal:
```cmd
<TOMCAT_HOME>\bin\startup.bat
```
To stop the server:
```cmd
<TOMCAT_HOME>\bin\shutdown.bat
```

## Application URLs
- Base URL: http://localhost:8080/servlet/
- Home Page (index): http://localhost:8080/servlet/index.html
- HelloServlet: http://localhost:8080/servlet/hello?name=Student
- Student Form: http://localhost:8080/servlet/student.html
- Lifecycle: http://localhost:8080/servlet/lifecycle
- Config / Context: http://localhost:8080/servlet/config
- Cookie Demo: http://localhost:8080/servlet/cookie
- Login: http://localhost:8080/servlet/login
- Secure Home: http://localhost:8080/servlet/home
- Logout: http://localhost:8080/servlet/logout

## Login Credentials
- Username: `admin`
- Password: `1234`

## Common Errors
- `404 Not Found`: Ensure the WAR file is named `servlet.war` and successfully deployed to Tomcat's `webapps` folder. Check Tomcat console.
- `500 Internal Server Error`: Check Tomcat logs. This may occur if session is null and not handled properly, or standard exceptions occur.
- ClassNotFoundException for Servlet API: Ensure Jakarta Servlet API dependency is marked as `<scope>provided</scope>` in `pom.xml`.
