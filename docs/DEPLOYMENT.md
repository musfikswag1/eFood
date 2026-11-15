# eFood Platform — Production Deployment Guide

## 1. Architecture Overview
1. **Android Customer & Rider App (`/app/`)**: Kotlin + Jetpack Compose + Material 3 (`applicationId = com.aistudio.efood.bdxqmp`), connected to Firebase Auth (Google Sign-In via Credential Manager), Cloud Firestore (`ai-studio-android-efood-d3a553e1-59f1-41a8-b2b2-11872c782cb9`), and optional Backend API (`BuildConfig.EFOOD_API_BASE_URL`) for live payment initiation.
2. **Backend API & WebSocket Server (`/backend/`)**: Node.js + Express + `ws` + PostgreSQL (`schema.sql`) + Firebase Admin bridge + authorized bKash/Nagad/SSLCommerz gateway routes.
3. **Separate Web Admin Dashboard (`/admin-web/`)**: React + TypeScript + Vite SPA restricted to authorized Admin accounts (`musfikswag@gmail.com` or Admin JWT), completely separated from customer Android builds.

---

## 2. Database & Backend Deployment (`/backend/`)
1. Provision PostgreSQL 15+ and run:
   ```bash
   psql "$DATABASE_URL" -f backend/schema.sql
   psql "$DATABASE_URL" -f backend/migrations/001_initial.sql
   ```
2. Copy `backend/.env.example` to `backend/.env` on your server and populate:
   - `DATABASE_URL`
   - `JWT_SECRET` (64+ random characters)
   - `ADMIN_BOOTSTRAP_EMAIL` & `ADMIN_BOOTSTRAP_PASSWORD_HASH` (bcrypt hash)
   - `GOOGLE_APPLICATION_CREDENTIALS` (Firebase Service Account JSON for `gen-lang-client-0193513787`)
   - Merchant secrets for live online payments: `BKASH_*`, `NAGAD_*`, `SSLCOMMERZ_*`, `CALLBACK_BASE_URL`
3. Install dependencies and start behind TLS (Nginx/Cloud Run/PM2):
   ```bash
   cd backend && npm ci --omit=dev && npm start
   ```

---

## 3. Separate Web Admin Dashboard (`/admin-web/`)
1. Copy `admin-web/.env.example` to `admin-web/.env` and set `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, and `VITE_EFOOD_API_BASE_URL`.
2. Build static assets and deploy to Firebase Hosting / Cloud Storage / Nginx on a dedicated origin (e.g. `https://admin.efood.com.bd`):
   ```bash
   cd admin-web && npm ci && npm run build
   ```
3. Ensure `https://admin.efood.com.bd` is added to Firebase Console → Authentication → Authorized Domains and `CORS_ORIGIN` in `backend/.env`.

---

## 4. Android App Release Build (`/app/`)
1. Configure `EFOOD_API_BASE_URL` in your production build environment if using the Node.js payment gateway server (`Cash on Delivery` works immediately out of the box with Firebase Firestore).
2. Ensure release signing keystore credentials (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) are set in your CI/CD secrets.
3. Build Android App Bundle (`.aab`) and APK:
   ```bash
   gradle :app:bundleRelease :app:assembleRelease
   ```
4. Deploy verified Firestore security rules:
   Already deployed to database `ai-studio-android-efood-d3a553e1-59f1-41a8-b2b2-11872c782cb9` via `DeployRules`.
