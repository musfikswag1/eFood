import React, { useEffect, useMemo, useState } from "react";
import {
  LayoutDashboard,
  Store,
  UtensilsCrossed,
  ClipboardList,
  Bike,
  Users,
  TicketPercent,
  Wallet,
  Settings,
  LogOut,
  ShieldCheck,
  RefreshCw,
} from "lucide-react";
import {
  collection,
  doc,
  onSnapshot,
  setDoc,
  updateDoc,
  serverTimestamp,
  Firestore,
} from "firebase/firestore";
import { onAuthStateChanged, User } from "firebase/auth";
import {
  getEnvFirebaseConfig,
  initAdminFirebase,
  signInAdminWithGoogle,
  signOutAdmin,
  FirebaseRuntimeConfig,
} from "./firebaseClient";

type AdminSection =
  | "overview"
  | "orders"
  | "restaurants"
  | "menu"
  | "riders"
  | "customers"
  | "marketing"
  | "finance"
  | "settings";

interface AdminOrder {
  id: string;
  customerName?: string;
  customerPhone?: string;
  restaurantName?: string;
  deliveryArea?: string;
  totalBdt?: number;
  paymentMethod?: string;
  paymentStatus?: string;
  status?: string;
  riderId?: string;
  riderName?: string;
}

interface AdminRestaurant {
  id: string;
  name: string;
  cuisine: string;
  area?: string;
  deliveryFeeBdt: number;
  minOrderBdt: number;
  deliveryTimeMinutes: number;
  isOpen: boolean;
  isFeatured: boolean;
  isActive: boolean;
}

interface AdminCategory {
  id: string;
  name: string;
  nameBn: string;
  sortOrder: number;
  isActive: boolean;
}

interface AdminMenuItem {
  id: string;
  restaurantId: string;
  categoryId: string;
  name: string;
  priceBdt: number;
  discountPriceBdt: number;
  isAvailable: boolean;
  isActive: boolean;
}

interface AdminRider {
  id: string;
  fullName: string;
  phone: string;
  nidNumber: string;
  vehicleType: string;
  approvalStatus: "PENDING" | "APPROVED" | "SUSPENDED";
  isOnline: boolean;
  totalDeliveries: number;
  totalEarningsBdt: number;
}

interface AdminCoupon {
  id: string;
  code: string;
  title: string;
  discountType: "PERCENT" | "FLAT";
  discountValue: number;
  minOrderBdt: number;
  maxDiscountBdt: number;
  isActive: boolean;
}

interface AdminBanner {
  id: string;
  title: string;
  subtitle: string;
  badgeText: string;
  promoCode: string;
  isActive: boolean;
}

interface SupportTicket {
  id: string;
  userId: string;
  subject: string;
  message: string;
  status: string;
}

const ADMIN_EMAIL = "musfikswag@gmail.com";
const ORDER_STAGES = [
  "ORDER_PLACED",
  "ORDER_CONFIRMED",
  "RESTAURANT_PREPARING",
  "FOOD_READY",
  "RIDER_ASSIGNED",
  "RIDER_PICKED_UP",
  "ON_THE_WAY",
  "DELIVERED",
  "CANCELLED",
];

function formatBdt(v: number | undefined): string {
  return `৳${Math.round(v || 0).toLocaleString("en-US")}`;
}

export default function App() {
  const [section, setSection] = useState<AdminSection>("overview");
  const [fbConfig, setFbConfig] = useState<FirebaseRuntimeConfig>(getEnvFirebaseConfig);
  const [apiBaseUrl, setApiBaseUrl] = useState<string>(
    import.meta.env.VITE_EFOOD_API_BASE_URL || ""
  );
  const [jwtToken, setJwtToken] = useState<string>("");
  const [adminEmailInput, setAdminEmailInput] = useState<string>(ADMIN_EMAIL);
  const [adminPasswordInput, setAdminPasswordInput] = useState<string>("");
  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [authError, setAuthError] = useState<string>("");
  const [statusBanner, setStatusBanner] = useState<string>("");

  const firebaseBundle = useMemo(() => initAdminFirebase(fbConfig), [fbConfig]);
  const db: Firestore | null = firebaseBundle?.db || null;

  // Live collections
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [restaurants, setRestaurants] = useState<AdminRestaurant[]>([]);
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [menuItems, setMenuItems] = useState<AdminMenuItem[]>([]);
  const [riders, setRiders] = useState<AdminRider[]>([]);
  const [coupons, setCoupons] = useState<AdminCoupon[]>([]);
  const [banners, setBanners] = useState<AdminBanner[]>([]);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);

  // Forms
  const [newRestName, setNewRestName] = useState("");
  const [newRestCuisine, setNewRestCuisine] = useState("Bangladeshi • Biryani");
  const [newRestFee, setNewRestFee] = useState("40");
  const [newCatName, setNewCatName] = useState("");
  const [newCatNameBn, setNewCatNameBn] = useState("");
  const [newCouponCode, setNewCouponCode] = useState("");
  const [newCouponValue, setNewCouponValue] = useState("15");
  const [newItemName, setNewItemName] = useState("");
  const [newItemPrice, setNewItemPrice] = useState("280");
  const [newItemRestId, setNewItemRestId] = useState("");
  const [newItemCatId, setNewItemCatId] = useState("");

  useEffect(() => {
    if (!firebaseBundle) return;
    return onAuthStateChanged(firebaseBundle.auth, (user) => {
      setFirebaseUser(user);
    });
  }, [firebaseBundle]);

  const isFirebaseAdmin =
    Boolean(firebaseUser?.emailVerified) &&
    firebaseUser?.email?.toLowerCase() === ADMIN_EMAIL;
  const isAuthenticated = isFirebaseAdmin || Boolean(jwtToken);

  useEffect(() => {
    if (!db || !isFirebaseAdmin) return;
    const unsubs = [
      onSnapshot(collection(db, "orders"), (snap) =>
        setOrders(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminOrder, "id">) })))
      ),
      onSnapshot(collection(db, "restaurants"), (snap) =>
        setRestaurants(
          snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminRestaurant, "id">) }))
        )
      ),
      onSnapshot(collection(db, "categories"), (snap) =>
        setCategories(
          snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminCategory, "id">) }))
        )
      ),
      onSnapshot(collection(db, "menu_items"), (snap) =>
        setMenuItems(
          snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminMenuItem, "id">) }))
        )
      ),
      onSnapshot(collection(db, "riders"), (snap) =>
        setRiders(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminRider, "id">) })))
      ),
      onSnapshot(collection(db, "coupons"), (snap) =>
        setCoupons(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminCoupon, "id">) })))
      ),
      onSnapshot(collection(db, "banners"), (snap) =>
        setBanners(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AdminBanner, "id">) })))
      ),
      onSnapshot(collection(db, "support_tickets"), (snap) =>
        setTickets(
          snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<SupportTicket, "id">) }))
        )
      ),
    ];
    return () => unsubs.forEach((u) => u());
  }, [db, isFirebaseAdmin]);

  async function handleGoogleLogin() {
    setAuthError("");
    if (!firebaseBundle) {
      setAuthError("Enter your Firebase Web API Key below or configure VITE_FIREBASE_API_KEY.");
      return;
    }
    try {
      const cred = await signInAdminWithGoogle(firebaseBundle.auth);
      if (cred.user.email?.toLowerCase() !== ADMIN_EMAIL) {
        await signOutAdmin(firebaseBundle.auth);
        setAuthError(`Access denied. Only authorized admin (${ADMIN_EMAIL}) can enter.`);
      }
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Google Sign-In failed.");
    }
  }

  async function handleRestLogin(e: React.FormEvent) {
    e.preventDefault();
    setAuthError("");
    if (!apiBaseUrl) {
      setAuthError("Enter your Backend API Base URL to use REST admin login.");
      return;
    }
    try {
      const res = await fetch(`${apiBaseUrl.replace(/\/$/, "")}/api/admin/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: adminEmailInput, password: adminPasswordInput }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Admin login failed.");
      setJwtToken(data.token);
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Login error.");
    }
  }

  async function updateOrderStage(order: AdminOrder, nextStatus: string) {
    if (!db) return;
    try {
      await updateDoc(doc(db, "orders", order.id), {
        status: nextStatus,
        updatedAt: serverTimestamp(),
      });
      if (nextStatus === "FOOD_READY") {
        await setDoc(doc(db, "delivery_requests", `req_${order.id}`), {
          orderId: order.id,
          restaurantName: order.restaurantName || "eFood Kitchen",
          pickupArea: order.deliveryArea || "Dhaka",
          dropoffArea: order.deliveryArea || "Dhaka",
          estimatedFeeBdt: 55,
          distanceKm: 2.5,
          status: "OPEN",
          claimedByRiderId: "",
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
      setStatusBanner(`Order #${order.id.slice(-6)} updated to ${nextStatus}`);
    } catch (err) {
      setStatusBanner(err instanceof Error ? err.message : "Update failed");
    }
  }

  async function updateRiderApproval(riderId: string, approvalStatus: "APPROVED" | "SUSPENDED" | "PENDING") {
    if (!db) return;
    await updateDoc(doc(db, "riders", riderId), {
      approvalStatus,
      updatedAt: serverTimestamp(),
    });
    setStatusBanner(`Rider ${riderId.slice(-6)} set to ${approvalStatus}`);
  }

  async function handleCreateRestaurant(e: React.FormEvent) {
    e.preventDefault();
    if (!db || !newRestName.trim()) return;
    const id = `rest_${Date.now()}`;
    await setDoc(doc(db, "restaurants", id), {
      name: newRestName.trim(),
      description: `${newRestCuisine} kitchen serving fresh meals across Dhaka.`,
      cuisine: newRestCuisine.trim(),
      area: "Dhanmondi",
      district: "Dhaka",
      deliveryTimeMinutes: 30,
      minOrderBdt: 150,
      deliveryFeeBdt: Number(newRestFee) || 40,
      rating: 4.8,
      reviewCount: 1,
      openHours: "10:00 AM - 11:30 PM",
      isOpen: true,
      isFeatured: true,
      isActive: true,
      imageKey: "biryani",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    setNewRestName("");
    setStatusBanner("Restaurant published to Firestore catalog.");
  }

  async function handleCreateCategory(e: React.FormEvent) {
    e.preventDefault();
    if (!db || !newCatName.trim()) return;
    const id = `cat_${newCatName.toLowerCase().replace(/[^a-z0-9]+/g, "_")}`;
    await setDoc(doc(db, "categories", id), {
      name: newCatName.trim(),
      nameBn: newCatNameBn.trim() || newCatName.trim(),
      iconKey: "fastfood",
      sortOrder: categories.length + 1,
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    setNewCatName("");
    setNewCatNameBn("");
    setStatusBanner("Category saved.");
  }

  async function handleCreateMenuItem(e: React.FormEvent) {
    e.preventDefault();
    if (!db || !newItemName.trim() || !newItemRestId) return;
    const id = `item_${Date.now()}`;
    await setDoc(doc(db, "menu_items", id), {
      restaurantId: newItemRestId,
      categoryId: newItemCatId || categories[0]?.id || "cat_biryani",
      name: newItemName.trim(),
      nameBn: newItemName.trim(),
      description: "Freshly prepared by partner kitchen.",
      priceBdt: Number(newItemPrice) || 250,
      discountPriceBdt: 0,
      isAvailable: true,
      isPopular: true,
      isBestSeller: false,
      isRecommended: true,
      isActive: true,
      imageKey: "biryani",
      rating: 4.7,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    setNewItemName("");
    setStatusBanner("Menu item added.");
  }

  async function handleCreateCoupon(e: React.FormEvent) {
    e.preventDefault();
    if (!db || !newCouponCode.trim()) return;
    const code = newCouponCode.trim().toUpperCase();
    await setDoc(doc(db, "coupons", `cpn_${code}`), {
      code,
      title: `${newCouponValue}% Promo Campaign`,
      discountType: "PERCENT",
      discountValue: Number(newCouponValue) || 15,
      minOrderBdt: 300,
      maxDiscountBdt: 150,
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    setNewCouponCode("");
    setStatusBanner(`Coupon ${code} activated.`);
  }

  const grossRevenue = orders
    .filter((o) => o.status !== "CANCELLED")
    .reduce((acc, o) => acc + (o.totalBdt || 0), 0);
  const codTotal = orders
    .filter((o) => o.paymentMethod === "COD" && o.status !== "CANCELLED")
    .reduce((acc, o) => acc + (o.totalBdt || 0), 0);
  const onlineTotal = orders
    .filter((o) => o.paymentMethod !== "COD" && o.status !== "CANCELLED")
    .reduce((acc, o) => acc + (o.totalBdt || 0), 0);

  if (!isAuthenticated) {
    return (
      <div className="login-shell">
        <div className="login-card">
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 20 }}>
            <img
              src="/efood-logo.png"
              alt="eFood logo"
              style={{ width: 44, height: 44, borderRadius: 12, objectFit: "cover", border: "1.5px solid var(--accent-yellow)" }}
            />
            <div>
              <h2>eFood Admin Console</h2>
              <div style={{ color: "var(--text-muted)", fontSize: 13 }}>
                Separate Operations & Dispatch Portal
              </div>
            </div>
          </div>

          {authError && (
            <div
              className="pill pill-red"
              style={{ width: "100%", borderRadius: 10, padding: "10px 12px", marginBottom: 16 }}
            >
              {authError}
            </div>
          )}

          <div style={{ marginBottom: 18 }}>
            <label style={{ fontSize: 12, color: "var(--text-muted)" }}>
              Firebase Web API Key (for direct Firestore real-time sync)
            </label>
            <input
              className="input"
              style={{ marginTop: 6 }}
              placeholder="Paste Firebase Web API Key or set VITE_FIREBASE_API_KEY"
              value={fbConfig.apiKey}
              onChange={(e) => setFbConfig({ ...fbConfig, apiKey: e.target.value })}
            />
          </div>

          <button
            className="btn btn-primary"
            style={{ width: "100%", padding: "12px 16px", marginBottom: 18 }}
            onClick={handleGoogleLogin}
          >
            Sign in with Authorized Google Admin ({ADMIN_EMAIL})
          </button>

          <hr style={{ borderColor: "var(--border-subtle)", margin: "18px 0" }} />

          <form onSubmit={handleRestLogin} style={{ display: "grid", gap: 10 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>Or Backend JWT Admin Login</div>
            <input
              className="input"
              placeholder="Backend API Base URL (e.g. https://api.efood.com.bd)"
              value={apiBaseUrl}
              onChange={(e) => setApiBaseUrl(e.target.value)}
            />
            <input
              className="input"
              placeholder="Admin Email"
              value={adminEmailInput}
              onChange={(e) => setAdminEmailInput(e.target.value)}
            />
            <input
              className="input"
              type="password"
              placeholder="Admin Password"
              value={adminPasswordInput}
              onChange={(e) => setAdminPasswordInput(e.target.value)}
            />
            <button className="btn" type="submit">
              Sign in via Backend REST API
            </button>
          </form>
        </div>
      </div>
    );
  }

  const navItems: { key: AdminSection; label: string; icon: React.ReactNode }[] = [
    { key: "overview", label: "Overview", icon: <LayoutDashboard size={18} /> },
    { key: "orders", label: "Orders & Dispatch", icon: <ClipboardList size={18} /> },
    { key: "restaurants", label: "Restaurants & Categories", icon: <Store size={18} /> },
    { key: "menu", label: "Food & Inventory", icon: <UtensilsCrossed size={18} /> },
    { key: "riders", label: "Riders & Approvals", icon: <Bike size={18} /> },
    { key: "customers", label: "Support Tickets", icon: <Users size={18} /> },
    { key: "marketing", label: "Banners & Coupons", icon: <TicketPercent size={18} /> },
    { key: "finance", label: "Financial Reports", icon: <Wallet size={18} /> },
    { key: "settings", label: "System Settings", icon: <Settings size={18} /> },
  ];

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand-header">
          <img
            src="/efood-logo.png"
            alt="eFood logo"
            style={{ width: 40, height: 40, borderRadius: 12, objectFit: "cover", border: "1.5px solid var(--accent-yellow)" }}
          />
          <div>
            <div style={{ fontWeight: 800, fontSize: 16 }}>eFood Admin</div>
            <div style={{ fontSize: 11, color: "var(--accent-yellow)" }}>
              <ShieldCheck size={12} style={{ verticalAlign: "middle", marginRight: 4 }} />
              {firebaseUser?.email || adminEmailInput}
            </div>
          </div>
        </div>

        {navItems.map((item) => (
          <button
            key={item.key}
            className={`nav-btn ${section === item.key ? "active" : ""}`}
            onClick={() => setSection(item.key)}
          >
            {item.icon}
            {item.label}
          </button>
        ))}

        <div style={{ marginTop: "auto", paddingTop: 16 }}>
          <button
            className="nav-btn"
            onClick={() => {
              if (firebaseBundle) signOutAdmin(firebaseBundle.auth);
              setJwtToken("");
            }}
          >
            <LogOut size={18} /> Sign Out
          </button>
        </div>
      </aside>

      <main className="main-area">
        <div className="topbar">
          <div>
            <h1 style={{ fontSize: 24 }}>{navItems.find((n) => n.key === section)?.label}</h1>
            <div style={{ color: "var(--text-muted)", fontSize: 13 }}>
              Connected DB: {fbConfig.databaseId}
            </div>
          </div>
          {statusBanner && (
            <div className="pill pill-yellow">
              <RefreshCw size={13} style={{ marginRight: 6 }} />
              {statusBanner}
            </div>
          )}
        </div>

        {section === "overview" && (
          <>
            <div className="kpi-grid">
              <div className="card">
                <div className="kpi-label">Gross Revenue</div>
                <div className="kpi-value accent">{formatBdt(grossRevenue)}</div>
              </div>
              <div className="card">
                <div className="kpi-label">Total Orders</div>
                <div className="kpi-value">{orders.length}</div>
              </div>
              <div className="card">
                <div className="kpi-label">Approved Riders</div>
                <div className="kpi-value">
                  {riders.filter((r) => r.approvalStatus === "APPROVED").length}
                </div>
              </div>
              <div className="card">
                <div className="kpi-label">Active Restaurants</div>
                <div className="kpi-value">{restaurants.length}</div>
              </div>
            </div>
            <div className="card">
              <h3 style={{ marginBottom: 12 }}>Recent Live Orders</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>Customer</th>
                      <th>Restaurant</th>
                      <th>Total</th>
                      <th>Payment</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.slice(0, 10).map((ord) => (
                      <tr key={ord.id}>
                        <td>#{ord.id.slice(-6)}</td>
                        <td>{ord.customerName}</td>
                        <td>{ord.restaurantName}</td>
                        <td>{formatBdt(ord.totalBdt)}</td>
                        <td>
                          {ord.paymentMethod} ({ord.paymentStatus})
                        </td>
                        <td>
                          <span className="pill pill-yellow">{ord.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        {section === "orders" && (
          <div className="card">
            <h3 style={{ marginBottom: 14 }}>Order Status & Rider Dispatch Control</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Order</th>
                    <th>Area</th>
                    <th>Amount</th>
                    <th>Current Stage</th>
                    <th>Advance / Assign</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((ord) => (
                    <tr key={ord.id}>
                      <td>
                        <strong>#{ord.id.slice(-6)}</strong>
                        <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                          {ord.restaurantName} • {ord.customerPhone}
                        </div>
                      </td>
                      <td>{ord.deliveryArea}</td>
                      <td>{formatBdt(ord.totalBdt)}</td>
                      <td>
                        <span className="pill pill-yellow">{ord.status}</span>
                      </td>
                      <td>
                        <select
                          value={ord.status}
                          onChange={(e) => updateOrderStage(ord, e.target.value)}
                          style={{ maxWidth: 220 }}
                        >
                          {ORDER_STAGES.map((st) => (
                            <option key={st} value={st}>
                              {st}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {section === "restaurants" && (
          <div style={{ display: "grid", gap: 20 }}>
            <div className="card">
              <h3 style={{ marginBottom: 12 }}>Add Restaurant Partner</h3>
              <form onSubmit={handleCreateRestaurant} className="form-grid">
                <input
                  className="input"
                  placeholder="Restaurant Name"
                  value={newRestName}
                  onChange={(e) => setNewRestName(e.target.value)}
                />
                <input
                  className="input"
                  placeholder="Cuisine"
                  value={newRestCuisine}
                  onChange={(e) => setNewRestCuisine(e.target.value)}
                />
                <input
                  className="input"
                  placeholder="Delivery Fee (BDT)"
                  value={newRestFee}
                  onChange={(e) => setNewRestFee(e.target.value)}
                />
                <button className="btn btn-primary" type="submit">
                  Publish Restaurant
                </button>
              </form>
            </div>

            <div className="card">
              <h3 style={{ marginBottom: 12 }}>Add / Manage Food Categories</h3>
              <form onSubmit={handleCreateCategory} className="form-grid" style={{ marginBottom: 16 }}>
                <input
                  className="input"
                  placeholder="Category Name (English)"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                />
                <input
                  className="input"
                  placeholder="Category Name (বাংলা)"
                  value={newCatNameBn}
                  onChange={(e) => setNewCatNameBn(e.target.value)}
                />
                <button className="btn btn-primary" type="submit">
                  Save Category
                </button>
              </form>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {categories.map((cat) => (
                  <span key={cat.id} className="pill pill-green">
                    {cat.name} ({cat.nameBn})
                  </span>
                ))}
              </div>
            </div>
          </div>
        )}

        {section === "menu" && (
          <div style={{ display: "grid", gap: 20 }}>
            <div className="card">
              <h3 style={{ marginBottom: 12 }}>Add Menu Product</h3>
              <form onSubmit={handleCreateMenuItem} className="form-grid">
                <select
                  value={newItemRestId}
                  onChange={(e) => setNewItemRestId(e.target.value)}
                >
                  <option value="">Select Restaurant</option>
                  {restaurants.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))}
                </select>
                <select
                  value={newItemCatId}
                  onChange={(e) => setNewItemCatId(e.target.value)}
                >
                  <option value="">Select Category</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                <input
                  className="input"
                  placeholder="Dish Name"
                  value={newItemName}
                  onChange={(e) => setNewItemName(e.target.value)}
                />
                <input
                  className="input"
                  placeholder="Price (BDT)"
                  value={newItemPrice}
                  onChange={(e) => setNewItemPrice(e.target.value)}
                />
                <button className="btn btn-primary" type="submit">
                  Add Product
                </button>
              </form>
            </div>
            <div className="card">
              <h3>Catalog Menu Items ({menuItems.length})</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Dish</th>
                      <th>Price</th>
                      <th>Availability</th>
                    </tr>
                  </thead>
                  <tbody>
                    {menuItems.map((m) => (
                      <tr key={m.id}>
                        <td>{m.name}</td>
                        <td>{formatBdt(m.priceBdt)}</td>
                        <td>{m.isAvailable ? "In Stock" : "Out of Stock"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {section === "riders" && (
          <div className="card">
            <h3 style={{ marginBottom: 14 }}>Delivery Partner Onboarding & Approvals</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Rider</th>
                    <th>NID</th>
                    <th>Vehicle</th>
                    <th>Deliveries</th>
                    <th>Earnings</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {riders.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <strong>{r.fullName}</strong>
                        <div style={{ fontSize: 12, color: "var(--text-muted)" }}>{r.phone}</div>
                      </td>
                      <td>{r.nidNumber}</td>
                      <td>{r.vehicleType}</td>
                      <td>{r.totalDeliveries}</td>
                      <td>{formatBdt(r.totalEarningsBdt)}</td>
                      <td>
                        <span
                          className={`pill ${
                            r.approvalStatus === "APPROVED"
                              ? "pill-green"
                              : r.approvalStatus === "SUSPENDED"
                              ? "pill-red"
                              : "pill-yellow"
                          }`}
                        >
                          {r.approvalStatus}
                        </span>
                      </td>
                      <td style={{ display: "flex", gap: 8 }}>
                        <button
                          className="btn btn-primary"
                          onClick={() => updateRiderApproval(r.id, "APPROVED")}
                        >
                          Approve
                        </button>
                        <button
                          className="btn btn-danger"
                          onClick={() => updateRiderApproval(r.id, "SUSPENDED")}
                        >
                          Suspend
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {section === "customers" && (
          <div className="card">
            <h3 style={{ marginBottom: 12 }}>Customer Complaints & Support Tickets</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Subject</th>
                    <th>Message</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tickets.map((t) => (
                    <tr key={t.id}>
                      <td>{t.subject}</td>
                      <td>{t.message}</td>
                      <td>{t.status}</td>
                      <td>
                        {db && (
                          <button
                            className="btn btn-primary"
                            onClick={() =>
                              updateDoc(doc(db, "support_tickets", t.id), {
                                status: "RESOLVED",
                                updatedAt: serverTimestamp(),
                              })
                            }
                          >
                            Mark Resolved
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {section === "marketing" && (
          <div style={{ display: "grid", gap: 20 }}>
            <div className="card">
              <h3 style={{ marginBottom: 12 }}>Create Promo Coupon</h3>
              <form onSubmit={handleCreateCoupon} className="form-grid">
                <input
                  className="input"
                  placeholder="Code (e.g. EID30)"
                  value={newCouponCode}
                  onChange={(e) => setNewCouponCode(e.target.value)}
                />
                <input
                  className="input"
                  placeholder="Discount % (e.g. 20)"
                  value={newCouponValue}
                  onChange={(e) => setNewCouponValue(e.target.value)}
                />
                <button className="btn btn-primary" type="submit">
                  Activate Coupon
                </button>
              </form>
            </div>
            <div className="card">
              <h3>Active Coupons ({coupons.length}) & Banners ({banners.length})</h3>
              <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 12 }}>
                {coupons.map((c) => (
                  <span key={c.id} className="pill pill-yellow">
                    {c.code} — {c.discountValue}
                    {c.discountType === "PERCENT" ? "%" : " BDT"}
                  </span>
                ))}
              </div>
            </div>
          </div>
        )}

        {section === "finance" && (
          <div className="kpi-grid">
            <div className="card">
              <div className="kpi-label">Total Gross Volume</div>
              <div className="kpi-value accent">{formatBdt(grossRevenue)}</div>
            </div>
            <div className="card">
              <div className="kpi-label">COD Collections</div>
              <div className="kpi-value">{formatBdt(codTotal)}</div>
            </div>
            <div className="card">
              <div className="kpi-label">Online (bKash / Nagad / Card)</div>
              <div className="kpi-value">{formatBdt(onlineTotal)}</div>
            </div>
          </div>
        )}

        {section === "settings" && (
          <div className="card">
            <h3 style={{ marginBottom: 10 }}>Platform Integration Configuration</h3>
            <p style={{ color: "var(--text-muted)", lineHeight: 1.6 }}>
              Firebase Project: <strong>{fbConfig.projectId}</strong>
              <br />
              Firestore Named Database: <strong>{fbConfig.databaseId}</strong>
              <br />
              Backend API URL: <strong>{apiBaseUrl || "Not set (configure VITE_EFOOD_API_BASE_URL)"}</strong>
              <br />
              Payment Merchant Secrets (bKash, Nagad, SSLCommerz) are stored strictly in{" "}
              <code>backend/.env</code> on the server and never exposed to client bundles.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
