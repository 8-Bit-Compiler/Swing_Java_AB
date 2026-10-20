# ShopSphere design diagrams

## System architecture

```mermaid
flowchart TD
 Browser[Customer or admin browser] -->|HTTP forms and session cookie| Tomcat[Tomcat 11]
 Tomcat --> Filter[SecurityFilter]
 Filter --> Servlet[ShopServlet controller]
 Servlet -->|request attributes| JSP[JSP EL JSTL and tag files]
 JSP --> Browser
 Servlet --> Service[ShopService business rules and transactions]
 Swing[AdminApp Swing MVC] --> Service
 SocketClient[InventoryClient] -->|UTF command / serialized Product| SocketServer[InventoryServer]
 SocketServer --> DAO[StoreDao and Jdbc]
 RMIClient[InventoryRmiClient] -->|registry lookup / remote method| RMI[InventoryRemote]
 RMI --> Service
 Service --> DAO
 DAO -->|prepared statements| DB[(MySQL or persistent H2)]
 Listener[AppListener] -->|initialize schema and demo records| DB
```

## Database ER diagram

```mermaid
erDiagram
 users ||--o{ addresses : saves
 users ||--o{ cart : fills
 users ||--o{ wishlist : saves
 users ||--o{ orders : places
 users ||--o{ reviews : writes
 users ||--o{ coupon_usage : redeems
 categories ||--o{ products : groups
 products ||--o{ cart : contains
 products ||--o{ wishlist : contains
 products ||--o{ order_items : purchased
 products ||--o{ reviews : receives
 addresses ||--o{ orders : ships
 orders ||--|{ order_items : contains
 orders ||--|| payments : records
 orders ||--o| coupon_usage : uses
 coupons ||--o{ coupon_usage : tracks
 users { bigint id PK
 varchar name
 varchar email UK
 varchar password
 varchar role
 boolean status }
 addresses { bigint id PK
 bigint user_id FK
 varchar address_line
 varchar city
 varchar state
 varchar pincode }
 categories { bigint id PK
 varchar name UK
 boolean status }
 products { bigint id PK
 bigint category_id FK
 varchar name
 decimal price
 decimal discount
 int stock
 boolean status }
 cart { bigint id PK
 bigint user_id FK
 bigint product_id FK
 int quantity }
 wishlist { bigint id PK
 bigint user_id FK
 bigint product_id FK }
 orders { bigint id PK
 bigint user_id FK
 bigint address_id FK
 varchar shipping_address
 decimal subtotal
 decimal discount
 decimal total_amount
 varchar checkout_token UK
 varchar order_status }
 order_items { bigint id PK
 bigint order_id FK
 bigint product_id FK
 varchar product_name
 int quantity
 decimal price }
 payments { bigint id PK
 bigint order_id FK
 varchar transaction_reference
 decimal amount
 varchar payment_status }
 coupons { bigint id PK
 varchar code UK
 varchar discount_type
 decimal discount_value
 decimal minimum_order
 decimal maximum_discount
 date expiry_date }
 reviews { bigint id PK
 bigint user_id FK
 bigint product_id FK
 int rating
 varchar review_text }
 coupon_usage { bigint id PK
 bigint coupon_id FK
 bigint user_id FK
 bigint order_id FK }
```

Cart, wishlist and reviews each have a unique `(user_id, product_id)` key. Coupon usage has a unique `(coupon_id, user_id)` key. Order records store shipping-address, item-name and price snapshots so subsequent catalog/profile changes do not rewrite history.

## Use cases

```mermaid
flowchart LR
 Guest([Visitor]) --> Browse[Browse search filter and sort]
 Guest --> Register[Register]
 Guest --> Login[Sign in]
 Customer([Authenticated customer]) --> Cart[Manage cart and wishlist]
 Customer --> Profile[Manage profile and addresses]
 Customer --> Checkout[Checkout with coupon and simulated payment]
 Customer --> Orders[View own orders and tracking]
 Customer --> Review[Review delivered purchases]
 Admin([Administrator]) --> Dashboard[View sales and low stock]
 Admin --> Catalog[Manage products and categories]
 Admin --> Fulfil[Advance or cancel orders]
 Admin --> Accounts[Enable or disable customers]
 Admin --> Coupons[Manage coupons]
 Admin --> Swing[Swing management]
 Admin --> Lab[Java syllabus demos]
```

## Class relationships

```mermaid
classDiagram
 class ShopServlet {
  +init()
  +doGet(request,response)
  +doPost(request,response)
  +destroy()
 }
 class ShopService {
  +login(email,password)
  +register(name,email,password)
  +cart(user,product,quantity,add)
  +checkout(user,address,code,method,token,declined)
  +orderStatus(actor,order,next)
  +saveProduct(...)
 }
 class StoreDao {
  +products(search,category,sort)
  +product(id)
  +cart(user)
  +orders(user)
  +stats()
 }
 class Jdbc {
  +query(connection,sql,args)
  +one(connection,sql,args)
  +update(connection,sql,args)
  +insert(connection,sql,args)
 }
 class Database {
  +open()
  +initialize()
 }
 class Product {
  -long id
  -String name
  -BigDecimal price
  -int stock
 }
 class InventoryService {
  <<interface>>
  +getProduct(id)
  +getStock(id)
  +updateStock(email,password,id,quantity)
 }
 ShopServlet --> ShopService
 ShopService --> StoreDao
 ShopService --> Jdbc
 StoreDao --> Jdbc
 StoreDao --> Database
 InventoryRemote ..|> InventoryService
 InventoryRemote --> ShopService
 InventoryService --> Product
 InventoryServer --> StoreDao
 InventoryServer --> Product
 AdminApp --> AdminController
 AdminController --> AdminView
 AdminController --> ShopService
```

The network Product is a typed serializable DTO. JDBC page records are maps keyed by column names and passed as request attributes for JSP EL. Queries execute through DAO helpers; services coordinate business rules and transactional writes.

## Login sequence

```mermaid
sequenceDiagram
 actor User
 participant Browser
 participant Filter as SecurityFilter
 participant Controller as ShopServlet
 participant Service as ShopService
 participant DB as JDBC database
 User->>Browser: Submit email and password
 Browser->>Filter: POST /login + CSRF token
 Filter->>Filter: Validate session token
 Filter->>Controller: Accepted form
 Controller->>Service: login(email,password)
 Service->>DB: Prepared email lookup
 DB-->>Service: Salted PBKDF2 hash and role
 Service->>Service: Constant-time hash comparison
 Service-->>Controller: User without password hash
 Controller->>Controller: Rotate session ID and CSRF token
 Controller-->>Browser: Redirect to store or admin dashboard
```

## Order sequence

```mermaid
sequenceDiagram
 actor Customer
 participant Web as ShopServlet
 participant Service as ShopService
 participant DB as JDBC database
 Customer->>Web: Address coupon method checkout token
 Web->>Service: checkout(...)
 Service->>DB: Begin transaction and lock user
 Service->>DB: Check prior checkout token
 Service->>DB: Verify address belongs to customer
 Service->>DB: Lock cart products in ID order
 Service->>DB: Read current prices stock and coupon usage
 Service->>Service: Calculate subtotal discount and total
 alt Mock decline or invalid stock/address/coupon
  Service->>DB: Roll back
  Service-->>Web: Validation message
  Web-->>Customer: Checkout with bag unchanged
 else Approved mock payment or COD
  Service->>DB: Insert order and item snapshots
  Service->>DB: Decrement stock and insert payment/coupon usage
  Service->>DB: Clear cart and commit
  Service-->>Web: Order ID
  Web-->>Customer: Order confirmation
 end
```
