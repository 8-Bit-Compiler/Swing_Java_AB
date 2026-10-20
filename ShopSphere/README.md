# ShopSphere

A complete Advanced Java coursework project: a Servlet/JSP shop, JDBC persistence, browser and Swing administration, socket inventory protocol, RMI service, and isolated syllabus demonstrations.

## Start here

Requires **JDK 17 or newer**, `curl`, and `tar`. Maven is downloaded automatically by the included launcher. On this Mac, the launcher selects the installed Homebrew JDK 17 because the system `java` is Java 8.

```sh
./scripts/run.sh
```

Open **http://localhost:8080**. Stop with Ctrl+C. The first run downloads dependencies and creates a persistent H2 database under `data/`; later runs retain your data.

| Account | Email | Password |
| --- | --- | --- |
| Customer | alex@shopsphere.local | Shop@12345 |
| Administrator | admin@shopsphere.local | Admin@12345 |

These are local academic demo credentials. `SHOP_ADMIN_PASSWORD` overrides the initial administrator password on a fresh database. It does not reset an existing account.

## What works

- Registration, login/logout, profile and saved delivery addresses.
- Product search, category filtering, price/name sorting, product details and recently viewed cookie.
- Persistent cart and wishlist, stock checks, quantity changes and removal.
- Coupon checkout, simulated approval/decline or cash on delivery, atomic order placement and duplicate-submission protection.
- Order history, tracking, delivered-purchase reviews and ratings.
- Admin dashboard, product/category creation and updates, archiving, inventory, customers, coupons and valid order transitions. Cancelling before shipment restores stock and records a simulated refund.
- Swing desktop administration sharing the same service and DAO layer.
- Socket client/server returning serialized products and a real RMI inventory registry/service/client.
- English/Hindi heading demonstration, JSP elements, fragments, custom tag files, EL, JSTL core/functions/SQL/XML, lifecycle events, URLConnection and optional JNDI.

## Other modules

Run each server in a separate terminal, from this project directory. Keep the same database environment settings for every process.

```sh
./scripts/run.sh com.shopsphere.swing.AdminApp
./scripts/run.sh com.shopsphere.network.InventoryServer
./scripts/run.sh com.shopsphere.network.InventoryClient GET_PRODUCT 1
./scripts/run.sh com.shopsphere.network.InventoryClient GET_STOCK 1
./scripts/run.sh com.shopsphere.network.InventoryClient SEARCH_PRODUCT headphones
./scripts/run.sh com.shopsphere.rmi.InventoryRmiServer
./scripts/run.sh com.shopsphere.rmi.InventoryRmiClient 1
./scripts/run.sh com.shopsphere.demo.UrlDemo
```

Web: 8080; socket: 9090; RMI registry: 1099. Services bind to loopback. The default H2 `AUTO_SERVER` mode supports sharing data between local processes. Stock changes require administrator credentials in Swing and RMI; socket commands are read-only.

## Build and test

```sh
./mvnw clean verify
```

Produces `target/shopsphere.war`, deployable on Tomcat 11 with Java 17+. Embedded Tomcat is a convenience launcher; the WAR runs on a standalone container as well.

The HTTP test creates a disposable test customer, order and review. Run it against a separate test instance:

```sh
# Terminal 1
PORT=8081 SHOP_DB_URL='jdbc:h2:./target/qa-store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE' ./scripts/run.sh
# Terminal 2
SHOP_BASE_URL=http://localhost:8081 python3 -u scripts/smoke.py
```

17 Java integration tests and 17 HTTP journeys passed during development. The tests include failed-payment rollback, order/address ownership, cancellation, authorization, overselling contention, socket serialization and actual RMI calls.

## MySQL and deployment

See [setup](docs/SETUP.md) for native MySQL, Docker Compose, Tomcat WAR deployment, environment variables, optional JNDI and troubleshooting. `database/shopsphere.sql` is a MySQL schema; initialization and demo seeding also run automatically in Java.

```sh
# Requires a running Docker engine; stop the local app first to free port 8080.
docker compose up --build
```

The Docker/MySQL configuration is supplied but was not executed on this host because the Docker daemon was unavailable. The verified runtime uses H2 in MySQL compatibility mode.

## Project handover

- [Setup and deployment](docs/SETUP.md)
- [Project report](docs/PROJECT_REPORT.md)
- [Architecture and database diagrams](docs/DIAGRAMS.md)
- [RTU syllabus mapping and viva](docs/SYLLABUS_AND_VIVA.md)
- [Test cases and verified results](docs/TESTING.md)
- [Screenshots](docs/screenshots)

Payments and refunds are simulations. Shipping is free in the project. English/Hindi support demonstrates ResourceBundle and localized headings rather than translating all content. Historical applet source is excluded from the modern build. JNDI requires an optional container resource. Public deployment would also need HTTPS, real payment integration, login throttling, password recovery, secret management and operational hardening.
