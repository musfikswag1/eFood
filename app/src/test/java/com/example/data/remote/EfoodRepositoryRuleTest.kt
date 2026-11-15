package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.OrderRecord
import com.example.data.model.RiderProfile
import com.example.data.model.SavedAddress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EfoodRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun customerProfileAndAddress_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice_efood@test.com")
        val repo = EfoodRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice Rahman", "alice_efood@test.com", "en")
        }
        assertTrue(profileRes.isSuccess)

        val addrId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAddress(
                SavedAddress(
                    label = "Home",
                    division = "Dhaka",
                    district = "Dhaka",
                    area = "Dhanmondi",
                    landmark = "Near Star Kabab",
                    streetAddress = "House 14, Road 6",
                    latitude = 23.7461,
                    longitude = 90.3742,
                    isDefault = true
                )
            ).getOrThrow()
        }

        val addresses = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAddresses(uid).first { list -> list.any { it.id == addrId } }
        }
        assertTrue(addresses.any { it.area == "Dhanmondi" })
    }

    @Test
    fun placeOrder_authenticatedCustomer_persistsAndStreams() = runBlocking {
        val uid = signInTestUser("alice_order@test.com")
        val repo = EfoodRepository(firestore, auth)
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.placeOrder(
                OrderRecord(
                    id = orderId,
                    customerName = "Alice Rahman",
                    customerPhone = "+8801711000001",
                    restaurantId = "rest_sultans_handi",
                    restaurantName = "Sultan's Handi House",
                    deliveryAddressText = "House 14, Road 6, Dhanmondi, Dhaka",
                    deliveryArea = "Dhanmondi",
                    deliveryDistrict = "Dhaka",
                    itemsSummary = "1x Royal Mutton Kacchi Biryani",
                    subtotalBdt = 430.0,
                    deliveryFeeBdt = 40.0,
                    discountBdt = 0.0,
                    taxBdt = 21.5,
                    totalBdt = 491.5,
                    paymentMethod = "COD",
                    deliveryOtp = "5921",
                    estimatedMinutes = 30L
                )
            )
        }
        assertTrue(result.isSuccess)

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerOrders(uid).first { list -> list.any { it.id == orderId } }
        }
        assertEquals("ORDER_PLACED", orders.first { it.id == orderId }.status)
    }

    @Test
    fun crossUserOrderRead_deniedBySecurityRules() = runBlocking {
        signInTestUser("alice_iso@test.com")
        val aliceRepo = EfoodRepository(firestore, auth)
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.placeOrder(
                OrderRecord(
                    id = orderId,
                    customerName = "Alice Rahman",
                    customerPhone = "+8801711000001",
                    restaurantId = "rest_sultans_handi",
                    restaurantName = "Sultan's Handi House",
                    deliveryAddressText = "Dhanmondi 27, Dhaka",
                    deliveryArea = "Dhanmondi",
                    deliveryDistrict = "Dhaka",
                    itemsSummary = "1x Shahi Morog Polao",
                    subtotalBdt = 320.0,
                    deliveryFeeBdt = 40.0,
                    discountBdt = 0.0,
                    taxBdt = 16.0,
                    totalBdt = 376.0,
                    paymentMethod = "COD",
                    deliveryOtp = "3194",
                    estimatedMinutes = 30L
                )
            ).getOrThrow()
        }

        signInTestUser("bob_iso@test.com")
        var thrown: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("orders").document(orderId).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            thrown = e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown?.code)
    }

    @Test
    fun observeCustomerOrders_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = EfoodRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCustomerOrders("any_uid").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun registerRiderProfile_createsPendingPartnerRecord() = runBlocking {
        val uid = signInTestUser("rider_partner@test.com")
        val repo = EfoodRepository(firestore, auth)
        val res = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerOrUpdateRider(
                RiderProfile(
                    fullName = "Tanvir Ahmed",
                    phone = "+8801811223344",
                    nidNumber = "1996261234567",
                    vehicleType = "MOTORBIKE",
                    emergencyContact = "+8801711998877"
                )
            )
        }
        assertTrue(res.isSuccess)
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRiderProfile(uid).first { it != null }
        }
        assertEquals("PENDING", profile?.approvalStatus)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
