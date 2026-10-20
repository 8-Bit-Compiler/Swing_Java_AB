# Test cases and verification

## Automated Java tests

Command: `./mvnw clean verify`.

`src/test/java/com/shopsphere/ShopIntegrationTest.java` creates a new isolated H2 database for every test. It exercises real JDBC operations and live local socket/RMI services rather than mocks.

| Test | Expected behavior |
| --- | --- |
| Password hashing | Random salts produce different hashes; correct password verifies and wrong password fails |
| Registration | Valid user can sign in, duplicate email and invalid inputs are rejected |
| Search/sort | Product search works, SQL injection text is treated as data, price sorting is correct |
| Cart | Add, accumulate, update, remove and reject excessive quantities |
| Archived cart removal | A product archived after adding can still be removed from the bag |
| Wishlist | Toggle saved item on and off |
| Checkout | Recalculate price and coupon cap, record payment, decrement stock, clear bag |
| Idempotency | Repeating a checkout token returns the original order without duplicating stock changes |
| Declined payment | No order/coupon usage persists, stock and cart remain unchanged |
| Coupon rules | Reject invalid, expired and already-used coupons |
| Ownership | Another user's address cannot be used; another user's order cannot be read |
| Cancellation | Restore stock, mark mock refund, release coupon usage; reject duplicate cancellation |
| Fulfilment/review | Enforce transitions, mark COD paid on delivery, allow only delivered-purchase reviews |
| Authorization | Customer cannot change stock; disabled user cannot sign in; admin cannot disable self |
| Product/category management | Create and update records, archive product from storefront |
| Concurrency | Two buyers compete for one unit: exactly one order succeeds and stock stays nonnegative |
| Socket | Real socket calls return Product, stock and search results through serialization |
| RMI | Real registry lookup and remote reads/updates; customer update denied |

The Java suite contains **17 test methods**; checkout and idempotency share one method. See `target/surefire-reports` after running for machine-generated results.

## HTTP journey

Run a separate disposable instance using the commands in README, then `SHOP_BASE_URL=http://localhost:8081 python3 -u scripts/smoke.py`.

| Journey | Expected result |
| --- | --- |
| Homepage | HTTP 200, rendered JSP and correct UTF-8 currency/navigation/footer |
| Search | Product result and clear empty state |
| Authentication | Anonymous cart request redirects to login |
| Register/login | New customer created and session established |
| Access controls | Customer admin request and invalid CSRF POST return 403 |
| Product/wishlist | Detail view and saved product render |
| Cart quantities | Server recalculates subtotal after add/update |
| Address | Saved address visible in customer profile |
| Failed payment | Checkout reports decline and retains bag |
| Coupon/order | Approved mock order uses server total and coupon maximum cap |
| Order history | Newly placed order visible to its customer |
| Admin login | Dashboard renders for administrator |
| Admin/JSP lab | All admin pages, edit form, SQL/XML/traditional JSP pages return valid responses |
| Fulfilment | Admin moves order through processing, shipped and delivered; customer sees status |
| Reviews | Delivered purchase can publish a review |
| Logout | Session loses access to protected pages |
| Missing route | HTTP 404 with custom error page |

All **17 HTTP journeys** passed on the local H2/Tomcat runtime and again against the built WAR deployed at `/shopsphere`. The script mutates only the database of the selected test instance. Re-runs create a distinct test customer.

## Visual verification

Browser checks cover storefront, login, dashboard, product management, product details and checkout. Screenshots are in `docs/screenshots/`. The responsive layout switches to two-column product cards, wrapping admin navigation and stacked checkout/forms on narrow screens.

## Deployment limits

MySQL/Docker Compose is configured but not executed on this host because the Docker daemon is stopped. H2 compatibility does not prove every MySQL behavior. Run the HTTP suite against a disposable MySQL deployment to verify the target database before a MySQL demonstration. JNDI succeeds only after a container DataSource is configured. Swing launched on this host; desktop interaction was not automated. Shared admin operations are covered by the service tests. Swing needs a desktop session. Payments/refunds are simulations and no money moves.
