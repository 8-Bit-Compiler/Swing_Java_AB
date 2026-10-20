package com.shopsphere.rmi;

import java.io.*;
import java.net.*;
import java.rmi.server.*;

public class LoopbackSocketFactory
    implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable {
  private static final long serialVersionUID = 1L;

  public Socket createSocket(String host, int port) throws IOException {
    return new Socket(InetAddress.getLoopbackAddress(), port);
  }

  public ServerSocket createServerSocket(int port) throws IOException {
    return new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
  }

  public boolean equals(Object o) {
    return o instanceof LoopbackSocketFactory;
  }

  public int hashCode() {
    return 311;
  }
}
