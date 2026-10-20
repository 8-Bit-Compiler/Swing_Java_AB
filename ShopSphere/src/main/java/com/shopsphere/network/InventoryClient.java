package com.shopsphere.network;

import java.io.*;
import java.net.*;
import java.util.*;

public final class InventoryClient {
  public static Object request(int port, String command)
      throws IOException, ClassNotFoundException {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 3000);
      socket.setSoTimeout(5000);
      var out = new DataOutputStream(socket.getOutputStream());
      out.writeUTF(command);
      out.flush();
      try (var in = new ObjectInputStream(socket.getInputStream())) {
        in.setObjectInputFilter(
            ObjectInputFilter.Config.createFilter(
                "maxdepth=15;maxrefs=2000;maxbytes=500000;com.shopsphere.model.Product;java.base/*;!*"));
        return in.readObject();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getenv().getOrDefault("INVENTORY_PORT", "9090"));
    System.out.println(request(port, args.length == 0 ? "GET_PRODUCT 1" : String.join(" ", args)));
  }
}
