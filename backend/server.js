require("dotenv").config();

const http = require("http");
const crypto = require("crypto");
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { Pool } = require("pg");
const { WebSocketServer } = require("ws");

const PORT = parseInt(process.env.PORT || "4000", 10);
const JWT_SECRET = process.env.JWT_SECRET || "";
const DATABASE_URL = process.env.DATABASE_URL || "";

if (!JWT_SECRET || JWT_SECRET.includes("replace_with")) {
  console.warn("[eFood API] WARNING: JWT_SECRET is not configured for production.");
}

const pool = DATABASE_URL
  ? new Pool({ connectionString: DATABASE_URL, max: 20 })
  : null;

// Optional Firebase Admin bridge for real-time Android Firestore synchronization
let firebaseAdmin = null;
let firestoreDb = null;
try {
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.FIREBASE_PROJECT_ID) {
    firebaseAdmin = require("firebase-admin");
    if (!firebaseAdmin.apps.length) {
      firebaseAdmin.initializeApp({
        projectId: process.env.FIREBASE_PROJECT_ID || "gen-lang-client-0193513787",
      });
    }
    const { getFirestore } = require("firebase-admin/firestore");
    const dbId =
      process.env.FIREBASE_DATABASE_ID ||
      "ai-studio-android-efood-d3a553e1-59f1-41a8-b2b2-11872c782cb9";
    firestoreDb = getFirestore(firebaseAdmin.app(), dbId);
  }
} catch (err) {
  console.warn("[eFood API] Firebase Admin bridge inactive:", err.message);
}

const app = express();
app.use(helmet());
app.use(
  cors({
    origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(",") : "*",
    credentials: true,
  })
);
app.use(express.json({ limit: "1mb" }));

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use("/api/", apiLimiter);

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: "/ws" });

const wsClients = new Set();
wss.on("connection", (ws) => {
  wsClients.add(ws);
  ws.on("close", () => wsClients.delete(ws));
});

function broadcastEvent(eventType, payload) {
  const message = JSON.stringify({ eventType, payload, timestamp: new Date().toISOString() });
  for (const client of wsClients) {
    if (client.readyState === 1) {
      client.send(message);
    }
  }
}

async function query(sql, params = []) {
  if (!pool) {
    throw Object.assign(new Error("DATABASE_URL is not configured."), { statusCode: 503 });
  }
  return pool.query(sql, params);
}

async function authenticateBearer(req, res, next) {
  const header = req.headers.authorization || "";
  if (!header.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Missing Bearer authorization token." });
  }
  const token = header.slice(7).trim();

  // 1. Try Firebase ID token first if Admin SDK initialized
  if (firebaseAdmin) {
    try {
      const decoded = await firebaseAdmin.auth().verifyIdToken(token);
      req.auth = {
        uid: decoded.uid,
        email: decoded.email || "",
        emailVerified: Boolean(decoded.email_verified),
        role: decoded.email === (process.env.ADMIN_BOOTSTRAP_EMAIL || "musfikswag@gmail.com")
          ? "admin"
          : "customer",
      };
      return next();
    } catch (_) {
      // Fallback to Backend Admin/Partner JWT check
    }
  }

  // 2. Verify backend JWT
  if (!JWT_SECRET) {
    return res.status(503).json({ error: "Server JWT_SECRET not configured." });
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.auth = payload;
    return next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token." });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.auth || !roles.includes(req.auth.role)) {
      return res.status(403).json({ error: `Requires role: ${roles.join(" or ")}` });
    }
    return next();
  };
}

async function logAdminAction(req, action, entityType, entityId, metadata = {}) {
  try {
    if (!pool) return;
    await query(
      `INSERT INTO admin_activity_logs (admin_email, action, entity_type, entity_id, metadata_json, ip_address)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        req.auth?.email || "system",
        action,
        entityType,
        String(entityId || ""),
        JSON.stringify(metadata),
        req.ip || "",
      ]
    );
  } catch (err) {
    console.error("[eFood API] Audit log error:", err.message);
  }
}

// --- Health Check ---
app.get("/api/health", async (_req, res) => {
  res.json({
    status: "ok",
    service: "efood-api",
    postgresConfigured: Boolean(pool),
    firestoreBridgeConfigured: Boolean(firestoreDb),
    timestamp: new Date().toISOString(),
  });
});

// --- Admin Authentication (Separate from Customer App) ---
app.post("/api/admin/auth/login", async (req, res) => {
  const { email, password, firebaseIdToken } = req.body || {};

  if (firebaseIdToken && firebaseAdmin) {
    try {
      const decoded = await firebaseAdmin.auth().verifyIdToken(firebaseIdToken);
      const bootstrapEmail = process.env.ADMIN_BOOTSTRAP_EMAIL || "musfikswag@gmail.com";
      if (decoded.email !== bootstrapEmail || !decoded.email_verified) {
        return res.status(403).json({ error: "Not authorized as eFood Admin." });
      }
      const token = jwt.sign(
        { uid: decoded.uid, email: decoded.email, role: "admin" },
        JWT_SECRET,
        { expiresIn: "12h" }
      );
      return res.json({ token, admin: { email: decoded.email, role: "admin" } });
    } catch (err) {
      return res.status(401).json({ error: "Invalid Firebase ID token." });
    }
  }

  if (!email || !password) {
    return res.status(400).json({ error: "Email and password are required." });
  }

  const bootstrapEmail = process.env.ADMIN_BOOTSTRAP_EMAIL || "musfikswag@gmail.com";
  const bootstrapHash = process.env.ADMIN_BOOTSTRAP_PASSWORD_HASH || "";

  if (email.toLowerCase() === bootstrapEmail.toLowerCase() && bootstrapHash) {
    const valid = await bcrypt.compare(password, bootstrapHash);
    if (!valid) {
      return res.status(401).json({ error: "Invalid admin credentials." });
    }
    const token = jwt.sign(
      { uid: "admin_bootstrap", email: bootstrapEmail, role: "admin" },
      JWT_SECRET,
      { expiresIn: "12h" }
    );
    return res.json({ token, admin: { email: bootstrapEmail, role: "admin" } });
  }

  try {
    const result = await query(
      `SELECT u.id, u.email, u.password_hash, u.display_name, u.role
       FROM users u
       JOIN admins a ON a.user_id = u.id
       WHERE LOWER(u.email) = LOWER($1) AND u.is_suspended = FALSE`,
      [email]
    );
    if (!result.rows.length || !result.rows[0].password_hash) {
      return res.status(401).json({ error: "Invalid admin credentials." });
    }
    const adminRow = result.rows[0];
    const ok = await bcrypt.compare(password, adminRow.password_hash);
    if (!ok) {
      return res.status(401).json({ error: "Invalid admin credentials." });
    }
    const token = jwt.sign(
      { uid: adminRow.id, email: adminRow.email, role: "admin" },
      JWT_SECRET,
      { expiresIn: "12h" }
    );
    return res.json({
      token,
      admin: { id: adminRow.id, email: adminRow.email, name: adminRow.display_name, role: "admin" },
    });
  } catch (err) {
    return res.status(err.statusCode || 500).json({ error: err.message });
  }
});

// --- Public Customer Catalog (Zero Login Barrier) ---
app.get("/api/catalog/home", async (_req, res) => {
  try {
    const [categories, restaurants, products, coupons] = await Promise.all([
      query("SELECT * FROM food_categories WHERE is_active = TRUE ORDER BY sort_order ASC"),
      query("SELECT * FROM restaurants WHERE is_active = TRUE ORDER BY is_featured DESC, rating DESC"),
      query("SELECT * FROM food_products WHERE is_active = TRUE ORDER BY is_best_seller DESC, rating DESC"),
      query("SELECT * FROM coupons WHERE is_active = TRUE ORDER BY created_at DESC"),
    ]);
    res.json({
      categories: categories.rows,
      restaurants: restaurants.rows,
      products: products.rows,
      coupons: coupons.rows,
    });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

// --- Payment Gateway Integration (bKash, Nagad, SSLCommerz Card — NEVER SIMULATED) ---
app.post("/api/payments/initiate", authenticateBearer, async (req, res) => {
  const { orderId, amountBdt, paymentMethod, customerPhone, customerName } = req.body || {};
  if (!orderId || !amountBdt || !paymentMethod) {
    return res.status(400).json({ error: "orderId, amountBdt, and paymentMethod are required." });
  }

  try {
    if (paymentMethod === "BKASH") {
      if (!process.env.BKASH_APP_KEY || !process.env.BKASH_APP_SECRET || !process.env.BKASH_USERNAME) {
        return res.status(503).json({
          error: "bKash merchant credentials (BKASH_APP_KEY, BKASH_APP_SECRET, BKASH_USERNAME) are not configured on this server.",
        });
      }
      const base = process.env.BKASH_BASE_URL || "https://tokenized.pay.bka.sh/v1.2.0-beta";
      const grantRes = await fetch(`${base}/tokenized/checkout/token/grant`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          username: process.env.BKASH_USERNAME,
          password: process.env.BKASH_PASSWORD,
        },
        body: JSON.stringify({
          app_key: process.env.BKASH_APP_KEY,
          app_secret: process.env.BKASH_APP_SECRET,
        }),
      });
      const grantData = await grantRes.json();
      if (!grantData.id_token) {
        return res.status(502).json({ error: "Failed to obtain bKash grant token." });
      }
      const createRes = await fetch(`${base}/tokenized/checkout/create`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: grantData.id_token,
          "X-APP-Key": process.env.BKASH_APP_KEY,
        },
        body: JSON.stringify({
          mode: "0011",
          payerReference: customerPhone || orderId,
          callbackURL: `${process.env.CALLBACK_BASE_URL}/api/payments/bkash/callback`,
          amount: Number(amountBdt).toFixed(2),
          currency: "BDT",
          intent: "sale",
          merchantInvoiceNumber: orderId,
        }),
      });
      const createData = await createRes.json();
      if (!createData.bkashURL || !createData.paymentID) {
        return res.status(502).json({ error: createData.statusMessage || "bKash create payment failed." });
      }
      return res.json({
        sessionId: createData.paymentID,
        gatewayUrl: createData.bkashURL,
        status: "INITIATED",
      });
    }

    if (paymentMethod === "NAGAD") {
      if (!process.env.NAGAD_MERCHANT_ID || !process.env.NAGAD_PRIVATE_KEY) {
        return res.status(503).json({
          error: "Nagad merchant credentials (NAGAD_MERCHANT_ID, NAGAD_PRIVATE_KEY) are not configured on this server.",
        });
      }
      return res.status(501).json({
        error: "Configure Nagad RSA handshake endpoint in production Nagad merchant account.",
      });
    }

    if (paymentMethod === "CARD") {
      if (!process.env.SSLCOMMERZ_STORE_ID || !process.env.SSLCOMMERZ_STORE_PASSWD) {
        return res.status(503).json({
          error: "SSLCommerz merchant credentials (SSLCOMMERZ_STORE_ID, SSLCOMMERZ_STORE_PASSWD) are not configured on this server.",
        });
      }
      const sslBase =
        process.env.SSLCOMMERZ_IS_LIVE === "true"
          ? "https://securepay.sslcommerz.com/gwprocess/v4/api.php"
          : "https://sandbox.sslcommerz.com/gwprocess/v4/api.php";
      const form = new URLSearchParams({
        store_id: process.env.SSLCOMMERZ_STORE_ID,
        store_passwd: process.env.SSLCOMMERZ_STORE_PASSWD,
        total_amount: Number(amountBdt).toFixed(2),
        currency: "BDT",
        tran_id: orderId,
        success_url: `${process.env.CALLBACK_BASE_URL}/api/payments/ssl/success`,
        fail_url: `${process.env.CALLBACK_BASE_URL}/api/payments/ssl/fail`,
        cancel_url: `${process.env.CALLBACK_BASE_URL}/api/payments/ssl/cancel`,
        cus_name: customerName || "eFood Customer",
        cus_phone: customerPhone || "+8801700000000",
        cus_email: req.auth.email || "customer@efood.com.bd",
        cus_add1: "Dhaka",
        cus_city: "Dhaka",
        cus_country: "Bangladesh",
        shipping_method: "YES",
        product_name: "eFood Order",
        product_category: "Food Delivery",
        product_profile: "general",
      });
      const sslRes = await fetch(sslBase, { method: "POST", body: form });
      const sslData = await sslRes.json();
      if (!sslData.GatewayPageURL) {
        return res.status(502).json({ error: sslData.failedreason || "SSLCommerz initiation failed." });
      }
      return res.json({
        sessionId: sslData.sessionkey || orderId,
        gatewayUrl: sslData.GatewayPageURL,
        status: "INITIATED",
      });
    }

    return res.status(400).json({ error: `Unsupported gateway method: ${paymentMethod}` });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.post("/api/payments/verify", authenticateBearer, async (req, res) => {
  const { orderId, sessionId, paymentMethod } = req.body || {};
  if (!orderId || !sessionId || !paymentMethod) {
    return res.status(400).json({ error: "orderId, sessionId, and paymentMethod are required." });
  }
  try {
    if (paymentMethod === "BKASH" && process.env.BKASH_APP_KEY) {
      const base = process.env.BKASH_BASE_URL || "https://tokenized.pay.bka.sh/v1.2.0-beta";
      const grantRes = await fetch(`${base}/tokenized/checkout/token/grant`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          username: process.env.BKASH_USERNAME,
          password: process.env.BKASH_PASSWORD,
        },
        body: JSON.stringify({
          app_key: process.env.BKASH_APP_KEY,
          app_secret: process.env.BKASH_APP_SECRET,
        }),
      });
      const grantData = await grantRes.json();
      const execRes = await fetch(`${base}/tokenized/checkout/execute`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: grantData.id_token,
          "X-APP-Key": process.env.BKASH_APP_KEY,
        },
        body: JSON.stringify({ paymentID: sessionId }),
      });
      const execData = await execRes.json();
      const verified = execData.transactionStatus === "Completed";
      return res.json({
        verified,
        transactionRef: execData.trxID || "",
        status: verified ? "VERIFIED" : "FAILED",
      });
    }

    return res.status(503).json({
      error: "Live payment verification requires configured merchant gateway credentials.",
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// --- Admin Management Endpoints (RBAC: admin only) ---
app.get("/api/admin/overview", authenticateBearer, requireRole("admin"), async (_req, res) => {
  try {
    const [ordersAgg, activeRiders, restaurantsCount, customersCount] = await Promise.all([
      query(`SELECT
          COUNT(*)::int AS total_orders,
          COUNT(*) FILTER (WHERE status NOT IN ('DELIVERED','CANCELLED'))::int AS live_orders,
          COALESCE(SUM(total_bdt) FILTER (WHERE status != 'CANCELLED'), 0)::float AS gross_revenue_bdt,
          COALESCE(SUM(total_bdt) FILTER (WHERE payment_method = 'COD' AND status = 'DELIVERED'), 0)::float AS cod_revenue_bdt,
          COALESCE(SUM(total_bdt) FILTER (WHERE payment_method != 'COD' AND payment_status = 'VERIFIED'), 0)::float AS online_revenue_bdt
        FROM orders`),
      query("SELECT COUNT(*)::int AS count FROM riders WHERE approval_status = 'APPROVED'"),
      query("SELECT COUNT(*)::int AS count FROM restaurants WHERE is_active = TRUE"),
      query("SELECT COUNT(*)::int AS count FROM users WHERE role = 'customer'"),
    ]);
    res.json({
      metrics: ordersAgg.rows[0],
      approvedRiders: activeRiders.rows[0].count,
      activeRestaurants: restaurantsCount.rows[0].count,
      totalCustomers: customersCount.rows[0].count,
    });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.get("/api/admin/orders", authenticateBearer, requireRole("admin"), async (req, res) => {
  try {
    const statusFilter = req.query.status || null;
    const result = statusFilter
      ? await query("SELECT * FROM orders WHERE status = $1 ORDER BY created_at DESC LIMIT 200", [statusFilter])
      : await query("SELECT * FROM orders ORDER BY created_at DESC LIMIT 200");
    res.json({ orders: result.rows });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.patch("/api/admin/orders/:orderId/status", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { orderId } = req.params;
  const { status, riderId } = req.body || {};
  const validStatuses = [
    "ORDER_PLACED", "ORDER_CONFIRMED", "RESTAURANT_PREPARING", "FOOD_READY",
    "RIDER_ASSIGNED", "RIDER_PICKED_UP", "ON_THE_WAY", "DELIVERED", "CANCELLED",
  ];
  if (!validStatuses.includes(status)) {
    return res.status(400).json({ error: "Invalid order status." });
  }
  try {
    const updated = await query(
      `UPDATE orders
       SET status = $1, rider_id = COALESCE($2, rider_id), updated_at = NOW()
       WHERE id::text = $3 OR external_id = $3
       RETURNING *`,
      [status, riderId || null, orderId]
    );
    if (!updated.rows.length) {
      return res.status(404).json({ error: "Order not found." });
    }
    const order = updated.rows[0];
    if (firestoreDb) {
      await firestoreDb.collection("orders").doc(order.external_id || order.id).set(
        { status, updatedAt: new Date() },
        { merge: true }
      );
      if (status === "FOOD_READY") {
        await firestoreDb.collection("delivery_requests").doc(`req_${order.external_id || order.id}`).set({
          orderId: order.external_id || order.id,
          restaurantName: order.restaurant_name || "eFood Partner",
          pickupArea: order.delivery_area || "Dhaka",
          dropoffArea: order.delivery_area || "Dhaka",
          estimatedFeeBdt: Number(order.delivery_fee_bdt || 55),
          distanceKm: 2.8,
          status: "OPEN",
          claimedByRiderId: "",
          createdAt: new Date(),
          updatedAt: new Date(),
        });
      }
    }
    broadcastEvent("ORDER_UPDATED", { orderId, status, riderId: riderId || null });
    await logAdminAction(req, "UPDATE_ORDER_STATUS", "orders", orderId, { status, riderId });
    res.json({ order });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.post("/api/admin/restaurants", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { name, description, cuisine, deliveryTimeMinutes, minOrderBdt, deliveryFeeBdt, openHours, isFeatured } = req.body || {};
  if (!name || !cuisine) {
    return res.status(400).json({ error: "name and cuisine are required." });
  }
  try {
    const result = await query(
      `INSERT INTO restaurants (name, description, cuisine, delivery_time_minutes, min_order_bdt, delivery_fee_bdt, open_hours, is_featured)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        name,
        description || "",
        cuisine,
        deliveryTimeMinutes || 30,
        minOrderBdt || 150,
        deliveryFeeBdt || 40,
        openHours || "10:00 AM - 11:30 PM",
        Boolean(isFeatured),
      ]
    );
    await logAdminAction(req, "CREATE_RESTAURANT", "restaurants", result.rows[0].id, { name });
    broadcastEvent("CATALOG_UPDATED", { type: "restaurant", id: result.rows[0].id });
    res.status(201).json({ restaurant: result.rows[0] });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.post("/api/admin/categories", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { name, nameBn, iconKey, sortOrder } = req.body || {};
  if (!name || !nameBn) {
    return res.status(400).json({ error: "name and nameBn are required." });
  }
  const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, "_");
  try {
    const result = await query(
      `INSERT INTO food_categories (slug, name, name_bn, icon_key, sort_order)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, name_bn = EXCLUDED.name_bn, sort_order = EXCLUDED.sort_order, updated_at = NOW()
       RETURNING *`,
      [slug, name, nameBn, iconKey || "fastfood", sortOrder || 99]
    );
    await logAdminAction(req, "UPSERT_CATEGORY", "food_categories", result.rows[0].id, { name });
    res.status(201).json({ category: result.rows[0] });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.post("/api/admin/products", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { restaurantId, categoryId, name, nameBn, description, priceBdt, discountPriceBdt, isBestSeller } = req.body || {};
  if (!restaurantId || !categoryId || !name || priceBdt == null) {
    return res.status(400).json({ error: "restaurantId, categoryId, name, and priceBdt are required." });
  }
  try {
    const result = await query(
      `INSERT INTO food_products (restaurant_id, category_id, name, name_bn, description, price_bdt, discount_price_bdt, is_best_seller)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [restaurantId, categoryId, name, nameBn || name, description || "", priceBdt, discountPriceBdt || 0, Boolean(isBestSeller)]
    );
    await logAdminAction(req, "CREATE_PRODUCT", "food_products", result.rows[0].id, { name, priceBdt });
    res.status(201).json({ product: result.rows[0] });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.patch("/api/admin/riders/:riderId/approval", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { riderId } = req.params;
  const { approvalStatus } = req.body || {};
  if (!["PENDING", "APPROVED", "SUSPENDED"].includes(approvalStatus)) {
    return res.status(400).json({ error: "Invalid approvalStatus." });
  }
  try {
    const result = await query(
      "UPDATE riders SET approval_status = $1, updated_at = NOW() WHERE id::text = $2 OR user_id::text = $2 RETURNING *",
      [approvalStatus, riderId]
    );
    if (!result.rows.length) {
      return res.status(404).json({ error: "Rider not found." });
    }
    if (firestoreDb) {
      await firestoreDb.collection("riders").doc(riderId).set(
        { approvalStatus, updatedAt: new Date() },
        { merge: true }
      );
    }
    await logAdminAction(req, "UPDATE_RIDER_APPROVAL", "riders", riderId, { approvalStatus });
    broadcastEvent("RIDER_STATUS_UPDATED", { riderId, approvalStatus });
    res.json({ rider: result.rows[0] });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.post("/api/admin/coupons", authenticateBearer, requireRole("admin"), async (req, res) => {
  const { code, title, discountType, discountValue, minOrderBdt, maxDiscountBdt } = req.body || {};
  if (!code || !title || !discountType || discountValue == null) {
    return res.status(400).json({ error: "code, title, discountType, and discountValue are required." });
  }
  try {
    const result = await query(
      `INSERT INTO coupons (code, title, discount_type, discount_value, min_order_bdt, max_discount_bdt)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [code.toUpperCase(), title, discountType, discountValue, minOrderBdt || 0, maxDiscountBdt || 0]
    );
    await logAdminAction(req, "CREATE_COUPON", "coupons", result.rows[0].id, { code });
    res.status(201).json({ coupon: result.rows[0] });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

app.get("/api/admin/audit-logs", authenticateBearer, requireRole("admin"), async (_req, res) => {
  try {
    const result = await query("SELECT * FROM admin_activity_logs ORDER BY created_at DESC LIMIT 100");
    res.json({ logs: result.rows });
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

server.listen(PORT, () => {
  console.log(`[eFood API] Listening on port ${PORT} (REST + WebSocket /ws)`);
});
