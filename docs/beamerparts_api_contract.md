## BeamerParts API Contract

This is the source of truth for all API endpoints to implement across services. It supports an iterative, step-by-step delivery: implement each microservice’s basic (M0) endpoints first (external + key internal), then layer messaging (M1), caching/optimizations (M2), and admin/advanced (M3).

Legend (phase tags)
- M0 Basic: minimal endpoints for service viability (external + core internal)
- M1 Messaging: add inter-service sync endpoints/contracts and message-driven flows (RabbitMQ)
- M2 Caching/Perf: enable Redis caching, pagination normalization, response tuning (no new endpoints unless needed)
- M3 Admin/Advanced: admin features and extended functionality
- Exists: implemented in code today

Implementation order (recommendation)
1) Vehicle Service → 2) Product Service → 3) User Service → 4) Order Service → 5) Gateway refinements

---

### API Gateway (external routing)
- External routes (maps `/api/*` to service-local paths without the `/api` prefix). No business endpoints beyond actuator.
- Health via gateway: `/health/{service}/actuator/health` (Exists)

---

### User Service (Port 8081)

External (via gateway `/api/...`)
- [M0] POST `/auth/register` – Register user
- [M0] POST `/auth/login` – Login and receive token pair
- [M0] POST `/auth/refresh` – Refresh access token
- [M0] POST `/auth/logout` – Revoke refresh token
- [M0] GET  `/auth/me` – Current user profile
- [M0] GET  `/cart` – Get cart
- [M0] POST `/cart/items` – Add item
- [M0] PUT  `/cart/items/{id}` – Update item
- [M0] DELETE `/cart/items/{id}` – Remove item
- [M0] DELETE `/cart/clear` – Clear cart
- [M1] GET  `/profile` – Get profile
- [M1] PUT  `/profile` – Update profile
- [M1] GET  `/profile/vehicles` – List user vehicles
- [M1] POST `/profile/vehicles` – Add vehicle

Internal (direct service-to-service `/internal/...`)
- [M0] POST `/internal/auth/validate-token` – Validate JWT
- [M0] GET  `/internal/users/{userId}/cart` – Get cart (checkout)
- [M1] POST `/internal/users/{userId}/cart/validate` – Validate cart across services
- [M1] POST `/internal/cart/bulk-validate` – Bulk cart validation
- [M1] GET  `/internal/users/{userId}/profile` – Internal profile
- [M1] GET  `/internal/users/{userId}/vehicles` – User vehicles

---

### Vehicle Service (Port 8082)

External (via gateway `/api/...`)
- [M0] GET `/vehicles/series` – List BMW series
- [M0] GET `/vehicles/series/{seriesCode}/generations` – Series generations
- [M0] GET `/vehicles/generations/{generationCode}` – Generation details
- [M1] GET `/vehicles/generations/{generationCode}/products` – Compatible products (delegates to Product)

Admin (via gateway `/api/admin/...`)
- [M3] POST `/admin/vehicles/series` – Create series
- [M3] PUT  `/admin/vehicles/series/{seriesCode}` – Update series
- [M3] POST `/admin/vehicles/generations` – Create generation
- [M3] PUT  `/admin/vehicles/generations/{generationCode}` – Update generation

Internal (direct `/internal/...`)
- [M0] GET  `/internal/series/{seriesCode}` – Series detail
- [M0] GET  `/internal/generations/{generationCode}` – Generation detail
- [M1] POST `/internal/series/bulk` – Bulk series lookup
- [M1] POST `/internal/generations/bulk` – Bulk generation lookup
- [M1] POST `/internal/compatibility/validate` – Validate compatibility
- [M1] GET  `/internal/compatibility/{generationCode}/products` – Products for generation

---

### Product Service (Port 8083)

External (via gateway `/api/...`)
- [M0, Exists] GET  `/products` – Paginated products
- [M0, Exists] GET  `/products/{sku}` – Product by SKU
- [M0, Exists] GET  `/products/search` – Search products
- [M1, Exists] GET  `/products/featured` – Featured products
- [M1, Exists] GET  `/products/by-generation/{generationCode}` – Products compatible with generation
- [M0, Exists] GET  `/categories` – Category tree
- [M0, Exists] GET  `/categories/{id}` – Category detail
- [M0, Exists] GET  `/categories/{id}/products` – Products by category (paginated)

Admin (via gateway `/api/admin/...`)
- [M3, Exists] POST   `/admin/products` – Create product
- [M3, Exists] PUT    `/admin/products/{sku}` – Update product
- [M3, Exists] DELETE `/admin/products/{sku}` – Soft-delete product
- [M3, Exists] PUT    `/admin/inventory/{sku}/stock` – Update stock level
- [M3, Exists] POST   `/admin/categories` – Create category
- [M3, Exists] PUT    `/admin/categories/{id}` – Update category
- [M3, Exists] DELETE `/admin/categories/{id}` – Delete category

Internal (direct `/internal/...`)
- [M0, Exists] GET  `/internal/products/{sku}` – Internal product detail (include flags)
- [M1, Exists] POST `/internal/products/bulk` – Bulk products by SKU
- [M1, Exists] POST `/internal/products/validate` – Validate products
- [M1, Exists] GET  `/internal/products/by-generation/{generationCode}` – Products by generation (internal dto)
- [M1, Exists] POST `/internal/inventory/reserve` – Reserve stock
- [M1, Exists] POST `/internal/inventory/release` – Release reservation
- [M1, Exists] POST `/internal/inventory/bulk-check` – Check availability
- [M1, Exists] PUT  `/internal/inventory/{sku}/stock` – Update stock
- [M1, Exists] POST `/internal/bmw-cache/sync` – Sync BMW cache (from Vehicle events)

Notes
- [M2] Apply Redis caching to: product by SKU, category tree, products by generation, featured products.
- Normalize pagination on list endpoints (M2).

---

### Order Service (Port 8084)

External (via gateway `/api/...`)
- [M3] GET  `/orders` – Current user orders
- [M3] GET  `/orders/{orderId}` – Order detail
- [M3] POST `/orders/checkout` – Create order from cart
- [M3] GET  `/orders/{orderId}/track` – Order status tracking
- [M3] POST `/orders/guest/checkout` – Guest checkout
- [M3] GET  `/orders/guest/track/{token}` – Guest order tracking
- [M3] POST `/orders/guest/send-tracking` – Send tracking email
- [M3] POST `/orders/{orderId}/payment` – Process payment
- [M3] GET  `/orders/{orderId}/payment/status` – Payment status
- [M3] GET  `/orders/{orderId}/invoice` – Invoice summary
- [M3] GET  `/orders/{orderId}/invoice/download` – Invoice PDF download

Internal (direct `/internal/...`)
- [M3] POST `/internal/orders` – Create order
- [M3] GET  `/internal/orders/{orderId}` – Get order
- [M3] PUT  `/internal/orders/{orderId}/status` – Update status
- [M3] GET  `/internal/orders/user/{userId}` – Orders by user
- [M3] GET  `/internal/orders/guest/{email}` – Orders by guest email
- [M3] POST `/internal/orders/{orderId}/payment/intent` – Create Stripe intent
- [M3] POST `/internal/orders/payment/webhook` – Stripe webhook
- [M3] GET  `/internal/orders/{orderId}/payment/status` – Payment status
- [M3] POST `/internal/orders/{orderId}/payment/refund` – Refund
- [M3] GET  `/internal/orders/{orderId}/invoice` – Invoice detail
- [M3] POST `/internal/orders/{orderId}/invoice/generate` – Generate PDF
- [M3] POST `/internal/orders/{orderId}/invoice/send` – Email invoice
- [M3] PUT  `/internal/orders/{orderId}/shipping` – Update shipping info
- [M3] POST `/internal/orders/{orderId}/ship` – Mark shipped
- [M3] PUT  `/internal/orders/{orderId}/tracking` – Update tracking
- [M3] GET  `/internal/orders/search` – Search orders

---

### Domain events (for reference; M1)
- Vehicle → Product: `BMWSeriesUpdated`, `BMWGenerationUpdated`, compatibility rule changes
- Product → Others: `ProductCreated`, `StockLevelChanged`
- Order → Product/User: `OrderCreated`, `PaymentCompleted`, `InventoryReduction`

---

Testing requirements (see `.cursorrules`)
- Every endpoint added/changed must have tests (controller/service/repository as applicable) and pass `scripts/dev/run-tests.sh`.
