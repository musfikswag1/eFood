const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const RIDER_UID = "rider_approved_1";
const ADMIN_UID = "admin_root_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read users or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("restaurants").where("isActive", "==", true).get());
});

test("Customer cannot self-assign admin or rider role on user profile creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      phoneVerified: false,
      preferredLanguage: "en",
      role: "admin",
      isSuspended: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Customer can create and read own profile, but cannot read Bob's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      phone: "+8801711000001",
      phoneVerified: true,
      preferredLanguage: "en",
      role: "customer",
      isSuspended: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Customer can place COD order and query own orders, but Bob cannot read Alice's order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderPayload = {
    customerId: ALICE_UID,
    customerName: "Alice Rahman",
    customerPhone: "+8801711000001",
    restaurantId: "rest_sultan",
    restaurantName: "Sultan's Dine",
    riderId: "",
    deliveryAddressText: "House 14, Road 6, Dhanmondi, Dhaka",
    deliveryArea: "Dhanmondi",
    deliveryDistrict: "Dhaka",
    itemsSummary: "1x Mutton Kacchi Biryani",
    subtotalBdt: 480,
    deliveryFeeBdt: 40,
    discountBdt: 0,
    taxBdt: 24,
    totalBdt: 544,
    paymentMethod: "COD",
    paymentStatus: "COD_DUE",
    status: "ORDER_PLACED",
    deliveryOtp: "4821",
    estimatedMinutes: 35,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };

  await assertSucceeds(aliceDb.collection("orders").doc("order_1").set(orderPayload));
  await assertSucceeds(aliceDb.collection("orders").where("customerId", "==", ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("orders").doc("order_1").get());
  await assertFails(bobDb.collection("orders").get());
});

test("Rider cannot mark order DELIVERED with wrong OTP, succeeds with matching OTP", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("riders").doc(RIDER_UID).set({
      userId: RIDER_UID,
      fullName: "Karim Rider",
      phone: "+8801811223344",
      nidNumber: "1995261234567",
      vehicleType: "MOTORBIKE",
      emergencyContact: "+8801711998877",
      approvalStatus: "APPROVED",
      isOnline: true,
      totalDeliveries: 12,
      totalEarningsBdt: 1400,
      commissionRate: 0.8,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await db.collection("orders").doc("order_2").set({
      customerId: ALICE_UID,
      customerName: "Alice Rahman",
      customerPhone: "+8801711000001",
      restaurantId: "rest_sultan",
      restaurantName: "Sultan's Dine",
      riderId: RIDER_UID,
      deliveryAddressText: "House 14, Road 6, Dhanmondi, Dhaka",
      deliveryArea: "Dhanmondi",
      deliveryDistrict: "Dhaka",
      itemsSummary: "1x Mutton Kacchi Biryani",
      subtotalBdt: 480,
      deliveryFeeBdt: 40,
      discountBdt: 0,
      taxBdt: 24,
      totalBdt: 544,
      paymentMethod: "COD",
      paymentStatus: "COD_DUE",
      status: "ON_THE_WAY",
      deliveryOtp: "4821",
      estimatedMinutes: 35,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const riderDb = testEnv.authenticatedContext(RIDER_UID).firestore();
  await assertFails(
    riderDb.collection("orders").doc("order_2").update({
      status: "DELIVERED",
      deliveryOtp: "0000",
      paymentStatus: "VERIFIED",
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    riderDb.collection("orders").doc("order_2").update({
      status: "DELIVERED",
      deliveryOtp: "4821",
      paymentStatus: "VERIFIED",
      updatedAt: nowTs(),
    })
  );
});

test("Shadow update with ghost field on user profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      phoneVerified: false,
      preferredLanguage: "en",
      role: "customer",
      isSuspended: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      isVerifiedAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("Verified bootstrap admin can list orders and approve riders, non-admin cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("riders").doc(RIDER_UID).set({
      userId: RIDER_UID,
      fullName: "Karim Rider",
      phone: "+8801811223344",
      nidNumber: "1995261234567",
      vehicleType: "MOTORBIKE",
      emergencyContact: "+8801711998877",
      approvalStatus: "PENDING",
      isOnline: false,
      totalDeliveries: 0,
      totalEarningsBdt: 0,
      commissionRate: 0.8,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertFails(aliceDb.collection("riders").get());

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "musfikswag@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(adminDb.collection("orders").get());
  await assertSucceeds(adminDb.collection("riders").get());
  await assertSucceeds(
    adminDb.collection("riders").doc(RIDER_UID).update({
      approvalStatus: "APPROVED",
      updatedAt: nowTs(),
    })
  );
});
