
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class Calc_Implementation
        extends UnicastRemoteObject
        implements Calc {
    Calc_Implementation() throws RemoteException {
        super();
    }

    public int add(int a, int b) {
        return a + b;
    }

    public int sub(int a, int b) {
        return a - b;
    }
}
