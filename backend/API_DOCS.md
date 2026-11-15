# eFood Platform REST & WebSocket API Documentation

Base URL (Production): `https://api.efood.com.bd`
WebSocket Endpoint: `wss://api.efood.com.bd/ws`

## Authentication
- **Customer & Rider Mobile Endpoints:** `Authorization: Bearer <Firebase_ID_Token>`
- **Admin Dashboard Endpoints:** `Authorization: Bearer <Admin_JWT_or_Firebase_Admin_ID_Token>`

---

## 1. Public & Catalog Endpoints (No Login Required)
- `GET /api/health` — Returns service health, PostgreSQL status, and Firestore bridge status.
- `GET /api/catalog/home` — Returns active `categories`, `restaurants`, `products`, and `coupons`.

## 2. Payment Gateway Endpoints (Authenticated)
- `POST /api/payments/initiate`
  - Body: `{ orderId, amountBdt, paymentMethod: "BKASH" | "NAGAD" | "CARD", customerPhone, customerName }`
  - Returns `{ sessionId, gatewayUrl, status: "INITIATED" }` when merchant credentials are configured, or HTTP `503` if merchant secrets are unset (never simulated).
- `POST /api/payments/verify`
  - Body: `{ orderId, sessionId, paymentMethod }`
  - Executes live merchant verification and returns `{ verified, transactionRef, status }`.

## 3. Admin Dashboard Endpoints (Requires `admin` Role)
- `POST /api/admin/auth/login` — Authenticates admin via bcrypt email/password or verified Firebase Admin ID token (`musfikswag@gmail.com`).
- `GET /api/admin/overview` — Aggregated gross revenue (BDT), COD vs Online split, live orders count, approved riders, active restaurants.
- `GET /api/admin/orders?status=ORDER_PLACED` — Lists up to 200 recent orders with optional status filter.
- `PATCH /api/admin/orders/:orderId/status` — Advances order lifecycle (`ORDER_CONFIRMED`, `RESTAURANT_PREPARING`, `FOOD_READY`, `RIDER_ASSIGNED`, `DELIVERED`, `CANCELLED`), publishes `delivery_requests` when `FOOD_READY`, syncs Firestore, and emits `ORDER_UPDATED` over `/ws`.
- `POST /api/admin/restaurants` — Creates a restaurant partner record and writes an entry to `admin_activity_logs`.
- `POST /api/admin/categories` — Creates or updates bilingual (`name`, `nameBn`) food categories.
- `POST /api/admin/products` — Creates menu products with BDT pricing and discount pricing.
- `PATCH /api/admin/riders/:riderId/approval` — Approves (`APPROVED`), sets `PENDING`, or suspends (`SUSPENDED`) a delivery partner.
- `POST /api/admin/coupons` — Creates percentage or flat BDT promo codes.
- `GET /api/admin/audit-logs` — Lists the latest 100 admin activity audit entries.

## 4. Real-Time WebSocket Events (`/ws`)
- `ORDER_UPDATED` — `{ orderId, status, riderId }`
- `CATALOG_UPDATED` — `{ type, id }`
- `RIDER_STATUS_UPDATED` — `{ riderId, approvalStatus }`
