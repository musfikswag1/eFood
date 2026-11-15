# eFood Platform — Verification & Testing Guide

## 1. Automated Security Rules Tests (Tier 1 & Tier 2)
### Tier 1 — JavaScript (`firestore.test.js`)
Validates zero-trust boundaries against the local Cloud Firestore Emulator (`127.0.0.1:8085`):
- Rejects unauthenticated reads on `orders`, `users`, `restaurants`.
- Blocks role escalation (`role: "admin"` on user profile creation).
- Enforces user isolation (`Alice` cannot read `Bob` profile or orders).
- Enforces shadow-update rejection (unknown keys rejected).
- Verifies Rider 4-digit OTP gate on `DELIVERED` transition.
- Verifies `musfikswag@gmail.com` (`email_verified: true`) admin list & rider approval permissions.

Run:
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

### Tier 2 — Android Robolectric Repository Integration (`EfoodRepositoryRuleTest.kt`)
Validates real Kotlin models (`UserProfile`, `SavedAddress`, `OrderRecord`, `RiderProfile`) against Firestore Emulator (`8085`) and Auth Emulator (`9099`):
```bash
gradle :app:testDebugUnitTest --tests '*Rule*'
```

## 2. Android Unit & Robolectric Business Logic Tests (`ExampleRobolectricTest.kt`)
- Verifies `app_name == "eFood"`.
- Verifies Bangladesh `+880` phone validation & normalization.
- Verifies BDT coupon percentage/flat math and formatting (`৳`).
```bash
gradle :app:testDebugUnitTest
```

## 3. End-to-End Critical User Journeys (Manual QA Matrix)
1. **Guest Browsing (Zero Login Barrier)**: Launch app -> verify immediate Home screen with mascot branding, Dhanmondi location selector, banners, 12 categories, featured/popular restaurants, best sellers, and recently viewed section.
2. **Food Customization & Single-Restaurant Cart Guard**: Open dish -> pick size + add-ons + special instructions -> add to cart -> attempt adding dish from a second restaurant -> confirm single-restaurant replacement prompt works cleanly.
3. **Google Sign-In + `+880` Verification + COD Checkout**: Proceed to checkout -> sign in with Google Credential Manager -> verify `+880` contact -> add Dhaka address -> apply `EFOOD50` -> confirm COD order -> track 8-stage progress & 4-digit OTP.
4. **Online Payments (bKash / Nagad / Card)**: Select bKash/Nagad/Card -> verify app invokes `/api/payments/initiate` when `EFOOD_API_BASE_URL` is configured, and refuses to fake-succeed when merchant credentials are not configured.
5. **Rider Partner Portal**: Account -> Delivery Partner Portal -> Google Sign-In -> register with NID & vehicle -> verify `PENDING` gate -> approve rider in Web Admin Console -> go online -> accept dispatch request -> navigate via Google Maps -> verify 4-digit customer OTP -> mark `DELIVERED`.
6. **Web Admin Console**: Sign in as `musfikswag@gmail.com` -> inspect live orders, advance order status to `FOOD_READY`, verify dispatch queue creation, approve riders, add restaurants/categories/coupons.
