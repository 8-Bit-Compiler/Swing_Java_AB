import java.rmi.Naming;

public class Client {
    public static void main(String[] args) {
        try {
            Calc obj = (Calc) Naming.lookup(
                    "rmi://localhost/Calculator");
            System.out.println("Addition = "
                    + obj.add(18, 20));
            System.out.println("Subtraction = "
                    + obj.sub(20, 10));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
