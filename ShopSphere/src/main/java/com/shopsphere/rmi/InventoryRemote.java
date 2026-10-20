package com.shopsphere.rmi;

import com.shopsphere.dao.Jdbc;
import com.shopsphere.model.Product;
import com.shopsphere.service.ShopService;
import java.rmi.*;
import java.rmi.server.*;

public class InventoryRemote extends UnicastRemoteObject implements InventoryService {
  private final ShopService service = new ShopService();

  public InventoryRemote() throws RemoteException {
    super(0, new LoopbackSocketFactory(), new LoopbackSocketFactory());
  }

  public Product getProduct(long id) throws RemoteException {
    try {
      var p = service.dao.product(id);
      return p == null ? null : new Product(p);
    } catch (Exception e) {
      throw new RemoteException("Product query failed", e);
    }
  }

  public int getStock(long id) throws RemoteException {
    var p = getProduct(id);
    return p == null ? -1 : p.getStock();
  }

  public boolean updateStock(String email, String password, long id, int quantity)
      throws RemoteException {
    try {
      return service.stock(Jdbc.id(service.login(email, password)), id, quantity);
    } catch (Exception e) {
      throw new RemoteException("Stock update denied", e);
    }
  }
}
