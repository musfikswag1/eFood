-- eFood Production PostgreSQL Schema
-- Covers all 22 core platform tables with normalized foreign keys and operational indexes.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid VARCHAR(128) UNIQUE,
    email VARCHAR(120) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash TEXT,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('customer', 'rider', 'admin', 'restaurant_owner')),
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'en' CHECK (preferred_language IN ('en', 'bn')),
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. customers
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    loyalty_points INTEGER NOT NULL DEFAULT 0,
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_spent_bdt NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    default_address_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. admins
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    admin_level VARCHAR(30) NOT NULL DEFAULT 'SUPER_ADMIN' CHECK (admin_level IN ('SUPER_ADMIN', 'OPERATIONS', 'FINANCE', 'SUPPORT')),
    permissions_json JSONB NOT NULL DEFAULT '["*"]'::jsonb,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. riders
CREATE TABLE IF NOT EXISTS riders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(80) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    nid_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(20) NOT NULL CHECK (vehicle_type IN ('BICYCLE', 'MOTORBIKE', 'SCOOTER', 'WALKER')),
    emergency_contact VARCHAR(20) NOT NULL,
    photo_url TEXT,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'SUSPENDED')),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    commission_rate NUMERIC(4, 2) NOT NULL DEFAULT 0.80,
    total_deliveries INTEGER NOT NULL DEFAULT 0,
    total_earnings_bdt NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. restaurants
CREATE TABLE IF NOT EXISTS restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(128) UNIQUE,
    owner_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    cuisine VARCHAR(120) NOT NULL,
    logo_url TEXT,
    cover_url TEXT,
    delivery_time_minutes INTEGER NOT NULL DEFAULT 30,
    min_order_bdt NUMERIC(10, 2) NOT NULL DEFAULT 150.00,
    delivery_fee_bdt NUMERIC(10, 2) NOT NULL DEFAULT 40.00,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 4.50,
    review_count INTEGER NOT NULL DEFAULT 0,
    open_hours VARCHAR(60) NOT NULL DEFAULT '10:00 AM - 11:30 PM',
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. restaurant_branches
CREATE TABLE IF NOT EXISTS restaurant_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    branch_name VARCHAR(80) NOT NULL,
    division VARCHAR(40) NOT NULL DEFAULT 'Dhaka',
    district VARCHAR(40) NOT NULL DEFAULT 'Dhaka',
    area VARCHAR(60) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    contact_phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. food_categories
CREATE TABLE IF NOT EXISTS food_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(60) NOT NULL,
    name_bn VARCHAR(80) NOT NULL,
    icon_key VARCHAR(40) NOT NULL DEFAULT 'fastfood',
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. food_products
CREATE TABLE IF NOT EXISTS food_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(128) UNIQUE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES food_categories(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    name_bn VARCHAR(120),
    description TEXT NOT NULL,
    price_bdt NUMERIC(10, 2) NOT NULL CHECK (price_bdt >= 0),
    discount_price_bdt NUMERIC(10, 2) DEFAULT 0.00 CHECK (discount_price_bdt >= 0),
    ingredients JSONB NOT NULL DEFAULT '[]'::jsonb,
    image_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    is_best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    is_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 4.60,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. food_customizations
CREATE TABLE IF NOT EXISTS food_customizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES food_products(id) ON DELETE CASCADE,
    option_type VARCHAR(20) NOT NULL CHECK (option_type IN ('SIZE', 'ADDON', 'PREPARATION')),
    label VARCHAR(80) NOT NULL,
    extra_price_bdt NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

-- 10. addresses
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(40) NOT NULL,
    division VARCHAR(40) NOT NULL,
    district VARCHAR(40) NOT NULL,
    area VARCHAR(60) NOT NULL,
    landmark VARCHAR(120),
    street_address VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. carts
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    restaurant_id UUID REFERENCES restaurants(id) ON DELETE SET NULL,
    items_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    coupon_code VARCHAR(32),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. coupons
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    discount_type VARCHAR(15) NOT NULL CHECK (discount_type IN ('PERCENT', 'FLAT')),
    discount_value NUMERIC(10, 2) NOT NULL CHECK (discount_value >= 0),
    min_order_bdt NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    max_discount_bdt NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. orders
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(128) UNIQUE,
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE RESTRICT,
    rider_id UUID REFERENCES riders(id) ON DELETE SET NULL,
    delivery_address_text VARCHAR(300) NOT NULL,
    delivery_area VARCHAR(80) NOT NULL,
    delivery_district VARCHAR(80) NOT NULL,
    delivery_landmark VARCHAR(120),
    delivery_instructions VARCHAR(250),
    delivery_lat DOUBLE PRECISION NOT NULL,
    delivery_lng DOUBLE PRECISION NOT NULL,
    subtotal_bdt NUMERIC(10, 2) NOT NULL,
    delivery_fee_bdt NUMERIC(10, 2) NOT NULL,
    discount_bdt NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    tax_bdt NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_bdt NUMERIC(10, 2) NOT NULL,
    promo_code VARCHAR(32),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('COD', 'BKASH', 'NAGAD', 'CARD')),
    payment_status VARCHAR(20) NOT NULL CHECK (payment_status IN ('PENDING', 'COD_DUE', 'VERIFIED', 'FAILED', 'REFUNDED')),
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'ORDER_PLACED', 'ORDER_CONFIRMED', 'RESTAURANT_PREPARING', 'FOOD_READY',
        'RIDER_ASSIGNED', 'RIDER_PICKED_UP', 'ON_THE_WAY', 'DELIVERED', 'CANCELLED'
    )),
    delivery_otp VARCHAR(6) NOT NULL,
    estimated_minutes INTEGER NOT NULL DEFAULT 35,
    failure_reason VARCHAR(250),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. order_items
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES food_products(id) ON DELETE RESTRICT,
    product_name VARCHAR(120) NOT NULL,
    selected_size VARCHAR(40) NOT NULL DEFAULT 'Regular',
    selected_addons_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    special_instructions VARCHAR(250),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_bdt NUMERIC(10, 2) NOT NULL,
    line_total_bdt NUMERIC(10, 2) NOT NULL
);

-- 15. payments
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    gateway VARCHAR(20) NOT NULL CHECK (gateway IN ('COD', 'BKASH', 'NAGAD', 'SSLCOMMERZ')),
    gateway_session_id VARCHAR(128),
    transaction_ref VARCHAR(128),
    amount_bdt NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'BDT',
    status VARCHAR(20) NOT NULL CHECK (status IN ('INITIATED', 'COD_DUE', 'VERIFIED', 'FAILED', 'REFUNDED')),
    raw_response_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 16. delivery_assignments
CREATE TABLE IF NOT EXISTS delivery_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    rider_id UUID REFERENCES riders(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLAIMED', 'PICKED_UP', 'COMPLETED', 'FAILED', 'CANCELLED')),
    estimated_fee_bdt NUMERIC(10, 2) NOT NULL,
    distance_km NUMERIC(6, 2) NOT NULL DEFAULT 2.5,
    assigned_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 17. rider_locations
CREATE TABLE IF NOT EXISTS rider_locations (
    id BIGSERIAL PRIMARY KEY,
    rider_id UUID NOT NULL REFERENCES riders(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 18. reviews
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rating NUMERIC(2, 1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    comment VARCHAR(500) NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 19. notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(400) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ORDER', 'PROMO', 'REFUND', 'SYSTEM')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 20. refunds
CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    amount_bdt NUMERIC(10, 2) NOT NULL,
    reason VARCHAR(250) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'REJECTED')),
    processed_by_admin UUID REFERENCES admins(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

-- 21. transactions
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    rider_id UUID REFERENCES riders(id) ON DELETE SET NULL,
    restaurant_id UUID REFERENCES restaurants(id) ON DELETE SET NULL,
    entry_type VARCHAR(30) NOT NULL CHECK (entry_type IN ('ORDER_PAYMENT', 'RIDER_PAYOUT', 'RESTAURANT_SETTLEMENT', 'REFUND_DEBIT', 'PLATFORM_COMMISSION')),
    amount_bdt NUMERIC(12, 2) NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('CREDIT', 'DEBIT')),
    notes VARCHAR(250),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 22. admin_activity_logs
CREATE TABLE IF NOT EXISTS admin_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    admin_email VARCHAR(120) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(128),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Operational Indexes
CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON orders(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_rider_status ON orders(rider_id, status);
CREATE INDEX IF NOT EXISTS idx_food_products_restaurant ON food_products(restaurant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_food_products_category ON food_products(category_id, is_active);
CREATE INDEX IF NOT EXISTS idx_rider_locations_rider_time ON rider_locations(rider_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_logs_time ON admin_activity_logs(created_at DESC);
