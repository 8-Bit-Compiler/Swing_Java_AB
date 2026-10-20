package com.shopsphere;

import static org.junit.jupiter.api.Assertions.*;

import com.shopsphere.dao.*;
import com.shopsphere.model.Product;
import com.shopsphere.network.*;
import com.shopsphere.rmi.*;
import com.shopsphere.service.ShopService;
import com.shopsphere.util.*;
import java.math.BigDecimal;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class ShopIntegrationTest {
  ShopService shop;
  long user;
  long address;
  long admin;

  @BeforeEach
  void init() throws Exception {
    System.setProperty(
        "SHOP_DB_URL",
        "jdbc:h2:mem:"
            + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    Database.initialize();
    shop = new ShopService();
    user = Jdbc.id(shop.login("alex@shopsphere.local", "Shop@12345"));
    admin = Jdbc.id(shop.login("admin@shopsphere.local", "Admin@12345"));
    address = shop.address(user, "12 Green Park Road", "Jaipur", "Rajasthan", "302001", "Home");
  }

  String token() {
    return UUID.randomUUID().toString();
  }

  long order(String coupon, String method) throws Exception {
    shop.cart(user, 1, 1, true);
    return shop.checkout(user, address, coupon, method, token(), false);
  }

  @Test
  void passwordsUseSaltAndRejectWrongPassword() {
    String a = Passwords.hash("a password"), b = Passwords.hash("a password");
    assertNotEquals(a, b);
    assertTrue(Passwords.verify("a password", a));
    assertFalse(Passwords.verify("wrong", a));
  }

  @Test
  void registrationValidationAndDuplicate() throws Exception {
    shop.register("Test Person", "test@example.com", "Password1");
    assertNotNull(shop.login("TEST@example.com", "Password1"));
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.register("Test Person", "test@example.com", "Password1"));
    assertThrows(IllegalArgumentException.class, () -> shop.register("Test", "invalid", "short"));
    assertThrows(IllegalArgumentException.class, () -> shop.login("test@example.com", "wrong"));
  }

  @Test
  void searchAndSort() throws Exception {
    assertEquals(1, shop.dao.products("studio wireless", null, null).size());
    assertTrue(shop.dao.products("' OR 1=1 --", null, null).isEmpty());
    var rows = shop.dao.products(null, null, "price-low");
    assertEquals("Daily Ceramic Cup", rows.get(0).get("name"));
  }

  @Test
  void cartUpdateRemoveAndStockValidation() throws Exception {
    shop.cart(user, 1, 2, true);
    shop.cart(user, 1, 1, true);
    assertEquals(3, ((Number) shop.dao.cart(user).get(0).get("quantity")).intValue());
    assertThrows(IllegalArgumentException.class, () -> shop.cart(user, 1, 99, true));
    shop.cart(user, 1, 0, false);
    assertTrue(shop.dao.cart(user).isEmpty());
  }

  @Test
  void archivedProductsCanBeRemovedFromBag() throws Exception {
    shop.cart(user, 1, 1, true);
    try (var c = Database.open()) {
      Jdbc.update(c, "UPDATE products SET status=FALSE WHERE id=1");
    }
    shop.cart(user, 1, 0, false);
    assertTrue(shop.dao.cart(user).isEmpty());
  }

  @Test
  void wishlistToggle() throws Exception {
    shop.wishlist(user, 1);
    assertEquals(1, shop.dao.wishlist(user).size());
    shop.wishlist(user, 1);
    assertTrue(shop.dao.wishlist(user).isEmpty());
  }

  @Test
  void checkoutDiscountInventoryPaymentAndIdempotency() throws Exception {
    shop.cart(user, 1, 2, true);
    String token = token();
    long id = shop.checkout(user, address, "welcome10", "MOCK_CARD", token, false);
    var o = shop.dao.order(id, user, false);
    assertEquals(new BigDecimal("11798.40"), o.get("total_amount"));
    assertEquals(new BigDecimal("1000.00"), o.get("discount"));
    assertEquals(22, ((Number) shop.dao.product(1).get("stock")).intValue());
    assertEquals("PAID", o.get("payment_status"));
    assertTrue(shop.dao.cart(user).isEmpty());
    assertEquals(id, shop.checkout(user, address, "welcome10", "MOCK_CARD", token, false));
    assertEquals(1, shop.dao.orders(user).size());
    try (var c = Database.open()) {
      assertNotNull(Jdbc.one(c, "SELECT * FROM payments WHERE order_id=?", id));
    }
  }

  @Test
  void declinedPaymentRollsBackEverything() throws Exception {
    shop.cart(user, 1, 1, true);
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.checkout(user, address, "WELCOME10", "MOCK_CARD", token(), true));
    assertTrue(shop.dao.orders(user).isEmpty());
    assertEquals(24, ((Number) shop.dao.product(1).get("stock")).intValue());
    assertEquals(1, shop.dao.cart(user).size());
    try (var c = Database.open()) {
      assertEquals(
          0, ((Number) Jdbc.one(c, "SELECT COUNT(*) n FROM coupon_usage").get("n")).intValue());
    }
  }

  @Test
  void couponRules() throws Exception {
    order("WELCOME10", "COD");
    shop.cart(user, 1, 1, true);
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.checkout(user, address, "WELCOME10", "COD", token(), false));
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.checkout(user, address, "BOGUS", "COD", token(), false));
    shop.coupon(
        admin,
        0,
        "EXPIRED",
        "PERCENT",
        new BigDecimal("10"),
        BigDecimal.ZERO,
        new BigDecimal("500"),
        LocalDate.of(2020, 1, 1),
        true);
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.checkout(user, address, "EXPIRED", "COD", token(), false));
  }

  @Test
  void addressOwnershipAndOrderPrivacy() throws Exception {
    long id = order("", "COD");
    shop.register("Another User", "other@example.com", "Password1");
    long other = Jdbc.id(shop.login("other@example.com", "Password1"));
    assertNull(shop.dao.order(id, other, false));
    shop.cart(other, 1, 1, true);
    assertThrows(
        IllegalArgumentException.class,
        () -> shop.checkout(other, address, "", "COD", token(), false));
  }

  @Test
  void cancellationRestocksAndRefundsOnce() throws Exception {
    long id = order("WELCOME10", "MOCK_CARD");
    shop.orderStatus(admin, id, "CANCELLED");
    assertEquals(24, ((Number) shop.dao.product(1).get("stock")).intValue());
    assertEquals("REFUNDED", shop.dao.order(id, user, false).get("payment_status"));
    assertThrows(IllegalArgumentException.class, () -> shop.orderStatus(admin, id, "CANCELLED"));
    shop.cart(user, 1, 1, true);
    assertDoesNotThrow(() -> shop.checkout(user, address, "WELCOME10", "COD", token(), false));
  }

  @Test
  void trackingCodAndVerifiedReview() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> shop.review(user, 1, 5, "Wonderful item"));
    long id = order("", "COD");
    assertThrows(IllegalArgumentException.class, () -> shop.orderStatus(admin, id, "DELIVERED"));
    for (String next : List.of("PROCESSING", "SHIPPED", "DELIVERED"))
      shop.orderStatus(admin, id, next);
    assertEquals("PAID", shop.dao.order(id, user, false).get("payment_status"));
    shop.review(user, 1, 5, "Wonderful item");
    shop.review(user, 1, 4, "Still a great item");
    assertEquals(1, shop.dao.reviews(1).size());
  }

  @Test
  void authorizationAndDisabledAccounts() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> shop.stock(user, 1, 100));
    assertThrows(IllegalArgumentException.class, () -> shop.userStatus(admin, admin, false));
    shop.userStatus(admin, user, false);
    assertThrows(
        IllegalArgumentException.class, () -> shop.login("alex@shopsphere.local", "Shop@12345"));
  }

  @Test
  void productAndCategoryCrud() throws Exception {
    shop.category(admin, 0, "Travel", "Travel essentials", true);
    var cat =
        shop.dao.categories().stream()
            .filter(x -> "Travel".equals(x.get("name")))
            .findFirst()
            .orElseThrow();
    shop.saveProduct(
        admin,
        0,
        Jdbc.id(cat),
        "Travel cup",
        "NOMAD",
        "A practical travel cup",
        new BigDecimal("500"),
        BigDecimal.ZERO,
        10,
        "/assets/cup.svg",
        true);
    var product = shop.dao.products("Travel cup", null, null).get(0);
    shop.saveProduct(
        admin,
        Jdbc.id(product),
        Jdbc.id(cat),
        "Travel cup",
        "NOMAD",
        "A practical travel cup",
        new BigDecimal("600"),
        BigDecimal.ZERO,
        5,
        "/assets/cup.svg",
        false);
    assertTrue(shop.dao.products("Travel cup", null, null).isEmpty());
  }

  @Test
  void competingCheckoutsNeverOversell() throws Exception {
    shop.stock(admin, 1, 1);
    shop.register("Second Buyer", "second@example.com", "Password1");
    long second = Jdbc.id(shop.login("second@example.com", "Password1"));
    long secondAddress =
        shop.address(second, "99 Blue Road", "Jaipur", "Rajasthan", "302002", "Home");
    shop.cart(user, 1, 1, true);
    shop.cart(second, 1, 1, true);
    var pool = Executors.newFixedThreadPool(2);
    var latch = new CountDownLatch(1);
    try {
      var a = pool.submit(() -> buy(latch, user, address));
      var b = pool.submit(() -> buy(latch, second, secondAddress));
      latch.countDown();
      assertEquals(1, a.get(10, TimeUnit.SECONDS) + b.get(10, TimeUnit.SECONDS));
      assertEquals(0, ((Number) shop.dao.product(1).get("stock")).intValue());
    } finally {
      pool.shutdownNow();
    }
  }

  int buy(CountDownLatch start, long user, long address) throws Exception {
    start.await();
    try {
      shop.checkout(user, address, "", "COD", token(), false);
      return 1;
    } catch (IllegalArgumentException e) {
      return 0;
    }
  }

  @Test
  void socketSerialization() throws Exception {
    try (var server = new InventoryServer(0)) {
      var worker =
          new Thread(
              () -> {
                try {
                  server.serve();
                } catch (Exception ignored) {
                }
              });
      worker.start();
      assertInstanceOf(Product.class, InventoryClient.request(server.port(), "GET_PRODUCT 1"));
      assertEquals(24, InventoryClient.request(server.port(), "GET_STOCK 1"));
      assertEquals(
          1,
          ((List<?>) InventoryClient.request(server.port(), "SEARCH_PRODUCT headphones")).size());
      server.close();
      worker.join(2000);
    }
  }

  @Test
  void actualRmiRegistryAndRemoteCall() throws Exception {
    var factory = new LoopbackSocketFactory();
    var registry = LocateRegistry.createRegistry(0, factory, factory);
    var remote = new InventoryRemote();
    try {
      registry.rebind("ShopSphereInventory", remote);
      var stub = (InventoryService) registry.lookup("ShopSphereInventory");
      assertEquals(24, stub.getStock(1));
      assertTrue(stub.updateStock("admin@shopsphere.local", "Admin@12345", 1, 12));
      assertEquals(12, stub.getStock(1));
      assertThrows(
          java.rmi.RemoteException.class,
          () -> stub.updateStock("alex@shopsphere.local", "Shop@12345", 1, 100));
    } finally {
      UnicastRemoteObject.unexportObject(remote, true);
      UnicastRemoteObject.unexportObject(registry, true);
    }
  }
}
