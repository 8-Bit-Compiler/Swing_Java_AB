# Setup and deployment

## Local run

Install a JDK of version 17 or later. Confirm `java -version` or set `JAVA_HOME` to the JDK directory. Tomcat 11 requires Java 17+. The project compiles with `--release 17`.

On this machine:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
./scripts/run.sh
```

On another system, set `JAVA_HOME` to its installed JDK path. The POSIX launcher works on macOS/Linux; Windows users can use WSL or standard Maven 3.9+ with the same POM. First run needs internet access to Maven Central.

The launcher builds the WAR and Java classpath, then starts embedded Tomcat on `127.0.0.1:8080`. Source files remain directly available to Jasper; no frontend build or CDN is required. The local vector product illustrations and all CSS/JS are included.

To exercise the packaged WAR with a non-root context using the embedded launcher:

```sh
PORT=8082 SHOP_WAR=target/shopsphere.war ./scripts/run.sh
# Open http://localhost:8082/shopsphere/
```

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `JAVA_HOME` | Homebrew JDK 17 when available, otherwise PATH | JDK directory |
| `PORT` | `8080` | Embedded web server port |
| `SHOP_WAR` | unset | Optional WAR path; serves at `/shopsphere` |
| `SHOP_BIND` | `127.0.0.1` | Embedded web server interface |
| `SHOP_DB_URL` | `jdbc:h2:./data/shopsphere;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE` | JDBC URL |
| `SHOP_DB_USER` | `sa` | JDBC username |
| `SHOP_DB_PASSWORD` | empty | JDBC password |
| `SHOP_ADMIN_PASSWORD` | `Admin@12345` | Seed administrator password, only on fresh database |
| `INVENTORY_PORT` | `9090` | Socket client target port; server accepts port as first argument |
| `RMI_PORT` | `1099` | RMI client target port; registry server accepts port as first argument |
| `SHOP_BASE_URL` | `http://localhost:8080` | HTTP smoke test target |

Export variables in each terminal. `scripts/run.sh` does not load `.env`; Docker Compose loads `.env` automatically. JDBC configuration also accepts JVM system properties for integration tests.

## Native MySQL 8.4

Create the database and a dedicated local user with schema privileges:

```sql
CREATE DATABASE shopsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'shopsphere'@'localhost' IDENTIFIED BY 'choose-a-local-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES ON shopsphere.* TO 'shopsphere'@'localhost';
```

Then run:

```sh
export SHOP_DB_URL='jdbc:mysql://localhost:3306/shopsphere?serverTimezone=Asia/Kolkata'
export SHOP_DB_USER=shopsphere
export SHOP_DB_PASSWORD='choose-a-local-password'
./scripts/run.sh
```

Java initializes the tables and inserts demo accounts/catalog/coupon on a fresh database. Alternatively, import `database/shopsphere.sql` first with an administrator account. The SQL file defines tables; Java supplies password hashes and demo records. H2 and MySQL have separate data; switching URLs does not migrate records.

Use TLS settings appropriate to your database server outside local development. The Compose URL disables TLS only within its local educational environment.

### Migrate the local H2 database

Stop the running ShopSphere app, ensure MySQL is running on `127.0.0.1:3306`, then run this from the project directory:

```sh
sh scripts/migrate-to-mysql.sh
```

The helper prompts locally for a MySQL administrator login and a new `shopsphere` app-user password; do not put passwords in chat or command arguments. It creates the `shopsphere` schema, copies all application tables from the default `data/shopsphere.mv.db`, checks copied row counts, and refuses to overwrite existing MySQL table data or an existing `shopsphere@localhost` account. The H2 file is left unchanged as a backup. If the app uses a non-default H2 URL, set `SHOP_DB_URL`, `SHOP_DB_USER`, and `SHOP_DB_PASSWORD` in the same terminal before running the helper.

After migration, use the helper's printed commands in that same terminal to launch the app against MySQL. In MySQL Workbench, connect to `127.0.0.1:3306` as `shopsphere` with the app-user password and select the `shopsphere` schema.

## Docker Compose

Copy `.env.example` to `.env` and choose local passwords before first initialization. With the Docker engine running, stop the local Java app to free port 8080 and execute `docker compose up --build`. The Maven build stage runs tests and creates the WAR. MySQL uses a named persistent volume; web starts after the database health check passes. Browser: `http://localhost:8080`.

`docker compose down` stops containers and retains database data. Keep the named volume if you want to preserve accounts and orders. Changing `.env` seed values does not update an already initialized database.

This deployment configuration has not been runtime verified on the development host because its Docker daemon was stopped.

## Standalone Tomcat 11

1. Build with `./mvnw clean verify`.
2. Configure the `SHOP_DB_*` environment in the Tomcat process. On Windows use the service environment settings; on Unix export before `catalina.sh run`.
3. Copy `target/shopsphere.war` to Tomcat's `webapps/` directory.
4. Start Tomcat and visit `http://localhost:8080/shopsphere/`.

For root deployment, name the file `ROOT.war`. URLs in JSPs use the context path, so both deployments are supported. Servlet/JSP container libraries are Maven `provided` dependencies and are excluded from the WAR. JDBC drivers and JSTL implementations are packaged. Use a writable working directory if you keep the default file-based H2 URL, or use an absolute H2 path.

## Optional JNDI

The store uses configured JDBC connections. The admin-only `/demo/jsp` page separately demonstrates `InitialContext.lookup("java:comp/env/jdbc/ShopSphere")` and obtains a connection from a container-managed DataSource.

Copy/adapt `database/tomcat-context.xml.example` to `conf/Catalina/localhost/shopsphere.xml` for a `/shopsphere` WAR, or `ROOT.xml` for a root deployment. Install the MySQL driver JAR in Tomcat's `lib` so its pool can load it. Set the resource's JDBC URL and credentials and restart Tomcat. The page reports the database product when configured and explains missing configuration otherwise. JNDI uses `javax.naming` and `javax.sql`, which remain Java SE packages; servlet imports use `jakarta.servlet`.

## Swing, sockets and RMI

The commands in README launch the modules. A graphical desktop is needed for Swing. Admin login uses the same hashed user records and checks the administrator role. Tabs support products, categories, orders, customers and coupons. Select a row, use **Edit selected** or **Update status / stock**, and refresh to see shared changes. Product/category removal means archiving, preserving order history. Database work runs in SwingWorker tasks outside the event dispatch thread.

The socket server accepts one UTF command per connection and sends a serialized Product, stock Integer, product ArrayList or error String. The client applies a deserialization filter. Four request workers handle accepted clients and requests time out after five seconds.

The RMI server creates a registry and binds `ShopSphereInventory`. Read methods return Product/stock. `updateStock(email,password,id,quantity)` authenticates an administrator before changing inventory. The client and server socket factories restrict connections to loopback. The sample client reads products only; integration tests exercise remote stock updates and denied customer updates. RMI runs only as a local teaching service.

## Troubleshooting

- **UnsupportedClassVersionError**: use JDK 17+, not the system Java 8.
- **Port already in use**: choose `PORT=8082`, or stop the earlier process. For socket/RMI pass a different server port and matching client environment variable.
- **Database locked**: launch every process from the project directory with the exact same H2 URL, including `AUTO_SERVER=TRUE`. Do not disable H2 file locking.
- **MySQL connection denied**: verify credentials, database existence, grants, host and server port.
- **First load is slower**: Jasper compiles each JSP on first request; Maven may also need to download dependencies initially.
- **403 on form submit**: reload the form to get a current CSRF token. Signing in rotates the session ID and form token.
- **Coupon rejected**: check spend, expiry, activation and prior use. WELCOME10 requires ₹1,000 and caps savings at ₹1,000.
- **Order has no review button permission**: reviews require a delivered order for that product.
- **Swing updates disappear**: confirm all processes point to the same database and refresh the table.
