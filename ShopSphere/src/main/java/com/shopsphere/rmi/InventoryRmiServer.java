package com.shopsphere.rmi;

import com.shopsphere.util.Database;
import java.rmi.registry.*;
import java.util.concurrent.CountDownLatch;

public final class InventoryRmiServer {
  public static void main(String[] args) throws Exception {
    Database.initialize();
    System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
    var factory = new LoopbackSocketFactory();
    Registry registry = LocateRegistry.createRegistry(port, factory, factory);
    registry.rebind("ShopSphereInventory", new InventoryRemote());
    System.out.println("RMI registry ready on 127.0.0.1:" + port + " / ShopSphereInventory");
    new CountDownLatch(1).await();
  }
}
