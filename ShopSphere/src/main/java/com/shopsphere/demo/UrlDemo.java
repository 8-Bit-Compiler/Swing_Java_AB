package com.shopsphere.demo;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class UrlDemo {
  public static void main(String[] args) throws Exception {
    URI uri = URI.create(args.length > 0 ? args[0] : "http://localhost:8080/health");
    URLConnection connection = uri.toURL().openConnection();
    connection.setConnectTimeout(3000);
    connection.setReadTimeout(3000);
    System.out.println(
        "Protocol: "
            + uri.getScheme()
            + " / Handler: "
            + connection.getClass().getName()
            + " / Content type: "
            + connection.getContentType());
    try (var in = connection.getInputStream()) {
      System.out.println(new String(in.readNBytes(2048), StandardCharsets.UTF_8));
    }
  }
}
