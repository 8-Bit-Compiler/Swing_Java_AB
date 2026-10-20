package com.shopsphere.rmi;

import java.rmi.registry.*;

public final class InventoryRmiClient {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getenv().getOrDefault("RMI_PORT", "1099"));
    var service =
        (InventoryService)
            LocateRegistry.getRegistry("127.0.0.1", port, new LoopbackSocketFactory())
                .lookup("ShopSphereInventory");
    System.out.println(service.getProduct(args.length > 0 ? Long.parseLong(args[0]) : 1));
  }
}
