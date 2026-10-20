package com.shopsphere;

import java.io.File;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.*;

public class Launcher {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    var tomcat = new Tomcat();
    tomcat.setPort(port);
    tomcat.setHostname("localhost");
    tomcat.setBaseDir("target/tomcat-" + port);
    new File("target/tomcat-" + port + "/webapps").mkdirs();
    tomcat
        .getConnector()
        .setProperty("address", System.getenv().getOrDefault("SHOP_BIND", "127.0.0.1"));
    String war = System.getenv("SHOP_WAR");
    var ctx =
        tomcat.addWebapp(
            war == null ? "" : "/shopsphere",
            new File(war == null ? "src/main/webapp" : war).getAbsolutePath());
    ctx.setParentClassLoader(Launcher.class.getClassLoader());
    if (war == null) {
      WebResourceRoot resources = new StandardRoot(ctx);
      resources.addPreResources(
          new DirResourceSet(
              resources, "/WEB-INF/classes", new File("target/classes").getAbsolutePath(), "/"));
      ctx.setResources(resources);
    }
    tomcat.start();
    if (!ctx.getState().isAvailable())
      throw new IllegalStateException(
          "ShopSphere failed to start; check the database configuration and Tomcat log.");
    System.out.println(
        "\nShopSphere is ready at http://localhost:"
            + port
            + (war == null ? "" : "/shopsphere")
            + "\n");
    tomcat.getServer().await();
  }
}
