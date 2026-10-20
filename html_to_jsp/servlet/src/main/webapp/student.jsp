<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Student Information</title>
</head>
<body>
    <h2>Student Information</h2>
    <form action="hello" method="post">
        Name:
        <input type="text" name="name" required>
        <br><br>

        Email:
        <input type="email" name="email" required>
        <br><br>

        Course:
        <input type="text" name="course" required>
        <br><br>

        Age:
        <input type="number" name="age" required>
        <br><br>

        <input type="submit" value="Register">
    </form>
</body>
</html>
