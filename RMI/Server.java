import java.rmi.Naming;

public class Server {
    public static void main(String[] args) {
        try {
            Calc_Implementation obj = new Calc_Implementation();
            Naming.rebind("rmi://localhost/Calculator", obj);
            System.out.println("RMI Server Strated....");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
