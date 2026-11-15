import { initializeApp, getApps, FirebaseApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, Auth } from "firebase/auth";
import { getFirestore, Firestore } from "firebase/firestore";

export const DEFAULT_PROJECT_ID =
  import.meta.env.VITE_FIREBASE_PROJECT_ID || "gen-lang-client-0193513787";
export const DEFAULT_DATABASE_ID =
  import.meta.env.VITE_FIRESTORE_DATABASE_ID ||
  "ai-studio-android-efood-d3a553e1-59f1-41a8-b2b2-11872c782cb9";

export interface FirebaseRuntimeConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  appId: string;
  databaseId: string;
}

export function getEnvFirebaseConfig(): FirebaseRuntimeConfig {
  return {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "",
    authDomain:
      import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || `${DEFAULT_PROJECT_ID}.firebaseapp.com`,
    projectId: DEFAULT_PROJECT_ID,
    appId: import.meta.env.VITE_FIREBASE_APP_ID || "",
    databaseId: DEFAULT_DATABASE_ID,
  };
}

export function initAdminFirebase(cfg: FirebaseRuntimeConfig): {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
} | null {
  if (!cfg.apiKey || cfg.apiKey === "your_firebase_web_api_key") {
    return null;
  }
  const existing = getApps()[0];
  const app =
    existing ||
    initializeApp({
      apiKey: cfg.apiKey,
      authDomain: cfg.authDomain,
      projectId: cfg.projectId,
      appId: cfg.appId || undefined,
    });
  const auth = getAuth(app);
  const db = getFirestore(app, cfg.databaseId);
  return { app, auth, db };
}

export async function signInAdminWithGoogle(auth: Auth) {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  return signInWithPopup(auth, provider);
}

export async function signOutAdmin(auth: Auth) {
  return signOut(auth);
}
