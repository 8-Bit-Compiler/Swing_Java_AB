# RTU syllabus mapping and viva preparation

## Implementation map

| RTU concept | Where to demonstrate |
| --- | --- |
| Swing components and MVC | `swing/AdminApp`, `AdminView`, `AdminController`; frames, panels, labels, text/password fields, buttons, checkbox, radio button, tables, scroll panes, dialogs and dropdown choices |
| Pluggable look and feel | `UIManager.setLookAndFeel` in AdminApp |
| MVC and layered architecture | ShopServlet → ShopService → StoreDao/Jdbc → Database, with JSP views |
| Applets | `docs/legacy/HistoricalShopApplet.java.txt`; historical lifecycle example excluded from Java 17 build |
| JDBC and java.sql | `dao/Jdbc`, prepared statements, connection/result-set resource management, transactional checkout |
| JDBC driver | MySQL Connector/J, optional local H2 driver |
| java.net client/server | `network/InventoryServer` and `InventoryClient`: ServerSocket, Socket, timeouts and UTF commands |
| Serialization | Serializable Product and ObjectOutputStream/ObjectInputStream; client object filter |
| Content/protocol handlers | `demo/UrlDemo`: URI→URL→URLConnection, response content type and protocol-specific implementation |
| RMI architecture | InventoryService remote interface, InventoryRemote exported object, remote client |
| RMI registry/naming | `LocateRegistry.createRegistry`, rebind and client lookup of ShopSphereInventory |
| JNDI | `demo/JndiDemo`, `database/tomcat-context.xml.example`, admin-only `/demo/jsp` |
| Internationalization | messages_en/messages_hi ResourceBundles; `/?lang=hi`, Locale and JSTL number formatting |
| J2EE / Jakarta EE | Servlet 6.1, JSP 4 on Tomcat 11; a focused web application rather than a full Jakarta EE application server |
| N-tier | Browser presentation, HTTP/controller, service, DAO and relational storage; separate local distributed-service examples |
| HTTP and web container | Tomcat deployment, GET display routes, POST mutations, redirects and response headers |
| Servlet lifecycle | ShopServlet init, doGet/doPost dispatch, destroy; container invokes inherited service |
| ServletConfig/ServletContext | Controller init logs servlet config name; AppListener sets application name and active session counter |
| Request/response | Prepared form inputs, request attributes, JSP forwards and POST/redirect/GET |
| Sessions | Logged-in user, CSRF token, checkout idempotency token, locale; session ID rotation on login |
| Cookies | HttpOnly session cookie, SameSite recentProduct convenience cookie; credentials excluded |
| Events/listeners | ServletContextListener initializes schema; HttpSessionListener tracks sessions |
| Authentication/admin filters | SecurityFilter checks live account status, role and CSRF before controllers |
| JSP lifecycle/elements | `/demo/jsp`: declaration, thread-safe counter, scriptlet, expression, JSP comment |
| JSP EL | Column-keyed request records, session user, dynamic conditions and values |
| Tag extensions/tag files | `/WEB-INF/tags/productCard.tag` and `rating.tag`, reused from views |
| JSP fragments | Header, footer, admin navigation and address form `.jspf` includes |
| JSTL core | forEach, if, choose, when, otherwise, set across normal views |
| JSTL functions | escapeXml, length, substring, toUpperCase in views and lab |
| JSTL SQL | `/demo/sql`, isolated read-only in-memory teaching DataSource |
| JSTL XML | `/demo/xml`, local XML feed parsed with x:parse and XPath iteration |
| EL number formatting | fmt:setLocale and fmt:formatNumber for INR values |

The project combines authentication/admin checks in one filter and customer/admin routes in one servlet controller. This keeps cross-cutting rules centralized; the guide's illustrative class-per-route names are not necessary to demonstrate dispatch or MVC.

## Suggested live demonstration

1. Start the web app and browse/search/filter the collection.
2. Register or sign in as a customer. Add products, change quantity, toggle wishlist and save an address.
3. Use WELCOME10 at checkout. Select mock decline first and show the unchanged bag; then approve payment and show the persisted order.
4. Sign in as admin in a separate browser session. Update stock/product, create a category or coupon, and move the order through processing, shipped and delivered.
5. Return to the customer order history and submit a verified review after delivery.
6. Open the admin Java lab and demonstrate JSP syntax, JSTL SQL/XML, fragments, tag files and Hindi heading.
7. Open Swing admin, sign in with the same administrator credentials, select a row and edit product/inventory or order status.
8. Start the socket server and run GET_PRODUCT, GET_STOCK and SEARCH_PRODUCT client commands.
9. Start the RMI server/registry and run the RMI client. Explain remote method calls and serializable return objects.
10. Show schema, diagrams, security filter, checkout transaction and automated test results.

## Viva questions and concise answers

**Why MVC?** The controller handles HTTP orchestration, views render request data, and services coordinate business rules. Swing has a separate view/controller while reusing the same services.

**Why PreparedStatement?** Parameter binding treats input as data and avoids building SQL by concatenating customer input. Sorting uses a fixed whitelist because SQL column/order identifiers cannot be bound as values.

**Why JDBC transactions at checkout?** Stock, orders, items, payment, coupon usage and cart clearing must commit together or roll back together. Autocommit would allow partial orders.

**How is overselling prevented?** Checkout locks the user and product rows, checks current stock under that transaction, and decrements with an additional `stock >= quantity` condition. Products are locked in ascending ID order. A contention test proves exactly one of two buyers gets the last unit.

**Why BigDecimal?** Decimal monetary arithmetic avoids binary floating-point rounding errors. Each selling price is rounded to two places before quantity multiplication; percentage coupon discount is rounded to two places and capped.

**What is idempotency?** A unique checkout token identifies one logical placement. Repeating that token returns the original order instead of reducing stock twice. Changing the cart clears the web session's checkout token for a fresh checkout.

**How are passwords stored?** Salted PBKDF2-HMAC-SHA256, 210,000 iterations and a 256-bit derived value. The database never stores the original password; comparison uses MessageDigest.isEqual.

**Why sessions and CSRF tokens?** A session identifies the authenticated browser. A separate unpredictable token on POST forms prevents another site from silently submitting authenticated actions. Login rotates session ID and CSRF token.

**How is authorization enforced?** The filter reloads account status/role on requests, redirects anonymous users and rejects customers on admin routes. Administrative service methods check the actor again, which also protects Swing and RMI usage.

**What is a JSP internally?** The container translates JSP to Java servlet source and compiles it. EL/JSTL express rendering without moving the store's business logic into the page. Scriptlets appear only in an isolated historical demonstration.

**What is the difference between include and tag files?** A static JSP include shares a fragment at translation time. A tag file defines a reusable component with typed attributes, such as the product card and rating display.

**Socket versus RMI?** The socket module defines its own UTF command/serialized-response protocol. RMI exposes a typed remote interface and uses generated runtime stubs plus a registry for discovery.

**Why Serializable?** Product instances must cross process boundaries. Their stable serialVersionUID supports predictable Java object-stream identification. The read client filters permitted serialized classes and sizes.

**What does JNDI add?** A container can publish a DataSource under a logical name. Java code looks it up without hardcoding creation of the container-managed pool. The sample keeps this separate and documents the required Tomcat resource.

**Why javax.naming alongside jakarta.servlet?** JNDI and SQL are Java SE APIs and retain `javax` packages. Servlet APIs migrated to Jakarta packages; Tomcat 11 requires those Jakarta imports.

**What happens to old orders after editing products or addresses?** Orders preserve shipping text, product names and purchase prices. Catalog changes do not alter past totals. Related product/category records are archived instead of deleted.

**Are refunds real?** No. Payments and refunds are recorded simulations. A cancelled paid mock order is marked REFUNDED and stock is restored; no gateway is called.

**Why no runnable browser applet?** Modern browsers removed the Java plugin. A historical source example demonstrates its lifecycle without making obsolete tooling a dependency.

**What is not yet production scope?** Real gateways, real shipping, tax calculation, full translations, password recovery, rate limiting, connection pooling for the main DAO, audit logs and deployment monitoring.
