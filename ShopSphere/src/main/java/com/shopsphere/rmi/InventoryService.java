package com.shopsphere.rmi;

import com.shopsphere.model.Product;
import java.rmi.*;

public interface InventoryService extends Remote {
  Product getProduct(long id) throws RemoteException;

  int getStock(long id) throws RemoteException;

  boolean updateStock(String email, String password, long id, int quantity) throws RemoteException;
}
