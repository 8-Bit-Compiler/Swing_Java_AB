package com.shopsphere.network;

import com.shopsphere.dao.StoreDao;
import com.shopsphere.model.Product;
import com.shopsphere.util.Database;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class InventoryServer implements AutoCloseable {
  private final ServerSocket server;
  private final ExecutorService pool = Executors.newFixedThreadPool(4);

  public InventoryServer(int port) throws IOException {
    server = new ServerSocket();
    server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
  }

  public int port() {
    return server.getLocalPort();
  }

  public void serve() throws IOException {
    System.out.println("Read-only inventory socket listening on 127.0.0.1:" + port());
    while (!server.isClosed()) {
      try {
        Socket socket = server.accept();
        pool.submit(() -> handle(socket));
      } catch (SocketException e) {
        if (!server.isClosed()) throw e;
      }
    }
  }

  private void handle(Socket socket) {
    try (socket) {
      socket.setSoTimeout(5000);
      var in = new DataInputStream(socket.getInputStream());
      String command = in.readUTF();
      Object result;
      var dao = new StoreDao();
      try {
        String[] pieces = command.split(" ", 2);
        String arg = pieces.length > 1 ? pieces[1] : "";
        result =
            switch (pieces[0]) {
              case "GET_PRODUCT" -> {
                var p = dao.product(Long.parseLong(arg));
                yield p == null ? "Product not found" : new Product(p);
              }
              case "GET_STOCK" -> {
                var p = dao.product(Long.parseLong(arg));
                yield p == null ? -1 : ((Number) p.get("stock")).intValue();
              }
              case "SEARCH_PRODUCT" ->
                  new ArrayList<>(
                      dao.products(arg, null, null).stream().map(Product::new).toList());
              default -> "Commands: GET_PRODUCT id, GET_STOCK id, SEARCH_PRODUCT text";
            };
      } catch (Exception e) {
        result = "Invalid request";
      }
      var out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(result);
      out.flush();
    } catch (IOException e) {
      System.err.println("Socket request failed: " + e.getMessage());
    }
  }

  public void close() throws IOException {
    server.close();
    pool.shutdownNow();
  }

  public static void main(String[] args) throws Exception {
    Database.initialize();
    try (var server = new InventoryServer(args.length > 0 ? Integer.parseInt(args[0]) : 9090)) {
      server.serve();
    }
  }
}
