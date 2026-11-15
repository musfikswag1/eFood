package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.Coupon
import com.example.data.model.DeliveryRequest
import com.example.data.model.FavoriteEntry
import com.example.data.model.FoodCategory
import com.example.data.model.MenuItem
import com.example.data.model.OrderRecord
import com.example.data.model.PromoBanner
import com.example.data.model.Restaurant
import com.example.data.model.ReviewRecord
import com.example.data.model.RiderProfile
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EfoodRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore.")
    }

    private fun handleStreamError(error: Throwable, operation: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, operation, path)
        }
    }

    // --- Real-time Admin-Managed Catalog Streams (Only subscribed when authenticated) ---

    fun observeActiveCategories(): Flow<List<FoodCategory>> {
        val path = "categories"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FoodCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sortOrder }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveRestaurants(): Flow<List<Restaurant>> {
        val path = "restaurants"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Restaurant::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveMenuItems(): Flow<List<MenuItem>> {
        val path = "menu_items"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MenuItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveBanners(): Flow<List<PromoBanner>> {
        val path = "banners"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PromoBanner::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sortOrder }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveCoupons(): Flow<List<Coupon>> {
        val path = "coupons"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Coupon::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- User Profile, Addresses, Favorites, Notifications ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                handleStreamError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        preferredLanguage: String = "en"
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.ifBlank { "eFood Customer" }.take(80),
                    "email" to email.take(120),
                    "phone" to "",
                    "phoneVerified" to false,
                    "preferredLanguage" to if (preferredLanguage == "bn") "bn" else "en",
                    "role" to "customer",
                    "isSuspended" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun updateCustomerContact(
        displayName: String,
        phone: String,
        phoneVerified: Boolean
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val updateMap = mapOf(
                "displayName" to displayName.trim().ifBlank { "eFood Customer" }.take(80),
                "phone" to phone.trim().take(20),
                "phoneVerified" to phoneVerified,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLanguage(languageCode: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docRef.update(
                mapOf(
                    "preferredLanguage" to if (languageCode == "bn") "bn" else "en",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    fun observeAddresses(userId: String): Flow<List<SavedAddress>> {
        val path = "users/$userId/addresses"
        return db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavedAddress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveAddress(address: SavedAddress): Result<String> {
        return try {
            val uid = requireUserId()
            val id = address.id.ifBlank { "addr_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
            val docRef = db.collection("users").document(uid).collection("addresses").document(id)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "label" to address.label.trim().ifBlank { "Home" }.take(40),
                "division" to address.division.trim().ifBlank { "Dhaka" }.take(40),
                "district" to address.district.trim().ifBlank { "Dhaka" }.take(40),
                "area" to address.area.trim().ifBlank { "Dhanmondi" }.take(60),
                "streetAddress" to address.streetAddress.trim().ifBlank { address.area }.take(200),
                "latitude" to address.latitude,
                "longitude" to address.longitude,
                "isDefault" to address.isDefault,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (address.landmark.isNotBlank()) {
                payload["landmark"] = address.landmark.trim().take(120)
            }
            if (address.id.isBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/addresses")
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("addresses").document(addressId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/addresses/$addressId")
            Result.failure(e)
        }
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteEntry>> {
        val path = "users/$userId/favorites"
        return db.collection("users").document(userId).collection("favorites")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FavoriteEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun setFavorite(targetType: String, targetId: String, isFav: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docId = "${targetType.lowercase()}_$targetId".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val docRef = db.collection("users").document(uid).collection("favorites").document(docId)
            if (isFav) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "targetType" to targetType,
                        "targetId" to targetId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/favorites")
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val path = "users/$userId/notifications"
        return db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("notifications").document(notificationId)
            docRef.update(mapOf("isRead" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications/$notificationId")
            Result.failure(e)
        }
    }

    // --- Orders & Real-Time Tracking ---

    fun observeCustomerOrders(userId: String): Flow<List<OrderRecord>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("customerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    OrderRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun placeOrder(order: OrderRecord): Result<String> {
        return try {
            val uid = requireUserId()
            val orderId = order.id.ifBlank { "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
            val otp = order.deliveryOtp.ifBlank { (1000..9999).random().toString() }
            val paymentStatus = if (order.paymentMethod == "COD") "COD_DUE" else "PENDING"

            val payload = mutableMapOf<String, Any>(
                "customerId" to uid,
                "customerName" to order.customerName.trim().ifBlank { "Customer" }.take(80),
                "customerPhone" to order.customerPhone.trim().take(20),
                "restaurantId" to order.restaurantId,
                "restaurantName" to order.restaurantName.take(100),
                "riderId" to "",
                "deliveryAddressText" to order.deliveryAddressText.take(300),
                "deliveryArea" to order.deliveryArea.take(80),
                "deliveryDistrict" to order.deliveryDistrict.take(80),
                "deliveryLat" to order.deliveryLat,
                "deliveryLng" to order.deliveryLng,
                "itemsSummary" to order.itemsSummary.take(1000),
                "subtotalBdt" to order.subtotalBdt,
                "deliveryFeeBdt" to order.deliveryFeeBdt,
                "discountBdt" to order.discountBdt,
                "taxBdt" to order.taxBdt,
                "totalBdt" to order.totalBdt,
                "paymentMethod" to order.paymentMethod,
                "paymentStatus" to paymentStatus,
                "status" to "ORDER_PLACED",
                "deliveryOtp" to otp,
                "estimatedMinutes" to order.estimatedMinutes.coerceIn(10L, 120L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (order.deliveryLandmark.isNotBlank()) payload["deliveryLandmark"] = order.deliveryLandmark.take(120)
            if (order.deliveryInstructions.isNotBlank()) payload["deliveryInstructions"] = order.deliveryInstructions.take(250)
            if (order.promoCode.isNotBlank()) payload["promoCode"] = order.promoCode.take(32)
            if (order.paymentReference.isNotBlank()) payload["paymentReference"] = order.paymentReference.take(120)

            val orderRef = db.collection("orders").document(orderId)
            orderRef.set(payload).await()

            // Create confirmation notification for the customer
            val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            db.collection("users").document(uid).collection("notifications").document(notifId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "title" to "Order Placed • ${order.restaurantName}",
                        "body" to "Order #$orderId received. Delivery OTP: $otp",
                        "type" to "ORDER",
                        "orderId" to orderId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        return try {
            requireUserId()
            val docRef = db.collection("orders").document(orderId)
            docRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "failureReason" to reason.take(250),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    // --- Reviews & Support ---

    fun observeRestaurantReviews(restaurantId: String): Flow<List<ReviewRecord>> {
        val path = "reviews"
        return db.collection(path)
            .whereEqualTo("isVisible", true)
            .whereEqualTo("restaurantId", restaurantId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ReviewRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitReview(
        authorName: String,
        restaurantId: String,
        orderId: String,
        rating: Double,
        comment: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val id = "rev_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("reviews").document(id).set(
                mapOf(
                    "userId" to uid,
                    "authorName" to authorName.ifBlank { "eFood Diner" }.take(60),
                    "restaurantId" to restaurantId,
                    "orderId" to orderId,
                    "rating" to rating.coerceIn(1.0, 5.0),
                    "comment" to comment.trim().take(500),
                    "isVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reviews")
            Result.failure(e)
        }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> {
        val path = "support_tickets"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SupportTicket::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitSupportTicket(
        subject: String,
        message: String,
        orderId: String = ""
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val id = "tkt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "subject" to subject.trim().take(120),
                "message" to message.trim().take(1000),
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (orderId.isNotBlank()) payload["orderId"] = orderId.take(128)
            db.collection("support_tickets").document(id).set(payload).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "support_tickets")
            Result.failure(e)
        }
    }

    // --- Delivery Partner / Rider Operations ---

    fun observeRiderProfile(userId: String): Flow<RiderProfile?> {
        val path = "riders/$userId"
        return db.collection("riders").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        RiderProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                handleStreamError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun registerOrUpdateRider(profile: RiderProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("riders").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "fullName" to profile.fullName.trim().take(80),
                    "phone" to profile.phone.trim().take(20),
                    "nidNumber" to profile.nidNumber.trim().take(20),
                    "vehicleType" to profile.vehicleType,
                    "emergencyContact" to profile.emergencyContact.trim().take(20),
                    "approvalStatus" to "PENDING",
                    "isOnline" to false,
                    "currentLat" to profile.currentLat,
                    "currentLng" to profile.currentLng,
                    "totalDeliveries" to 0,
                    "totalEarningsBdt" to 0.0,
                    "commissionRate" to 0.80,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (profile.photoUri.isNotBlank()) {
                    payload["photoUri"] = profile.photoUri.take(500)
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to profile.fullName.trim().take(80),
                    "phone" to profile.phone.trim().take(20),
                    "vehicleType" to profile.vehicleType,
                    "emergencyContact" to profile.emergencyContact.trim().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (profile.photoUri.isNotBlank()) {
                    updates["photoUri"] = profile.photoUri.take(500)
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "riders/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun setRiderOnline(isOnline: Boolean, lat: Double?, lng: Double?): Result<Unit> {
        return try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>(
                "isOnline" to isOnline,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (lat != null && lng != null) {
                updates["currentLat"] = lat
                updates["currentLng"] = lng
            }
            db.collection("riders").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "riders/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    fun observeOpenDeliveryRequests(): Flow<List<DeliveryRequest>> {
        val path = "delivery_requests"
        return db.collection(path)
            .whereEqualTo("status", "OPEN")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DeliveryRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeRiderAssignedOrders(riderId: String): Flow<List<OrderRecord>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("riderId", riderId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    OrderRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun claimDeliveryRequest(
        request: DeliveryRequest,
        riderName: String,
        riderPhone: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("delivery_requests").document(request.id).update(
                mapOf(
                    "status" to "CLAIMED",
                    "claimedByRiderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("orders").document(request.orderId).update(
                mapOf(
                    "riderId" to uid,
                    "riderName" to riderName.take(80),
                    "riderPhone" to riderPhone.take(20),
                    "status" to "RIDER_ASSIGNED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "delivery_requests/${request.id}")
            Result.failure(e)
        }
    }

    suspend fun advanceRiderDeliveryStage(
        order: OrderRecord,
        nextStatus: String,
        enteredOtp: String? = null,
        riderLat: Double? = null,
        riderLng: Double? = null,
        shareLocation: Boolean = true
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to nextStatus,
                "riderLocationSharing" to shareLocation,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (riderLat != null && riderLng != null) {
                updates["riderLat"] = riderLat
                updates["riderLng"] = riderLng
            }
            if (nextStatus == "DELIVERED") {
                val otp = enteredOtp?.trim().orEmpty()
                updates["deliveryOtp"] = otp
                updates["paymentStatus"] = "VERIFIED"
            }

            db.collection("orders").document(order.id).update(updates).await()

            if (nextStatus == "DELIVERED") {
                val earned = kotlin.math.max(40.0, order.deliveryFeeBdt * 0.80)
                db.collection("riders").document(uid).update(
                    mapOf(
                        "totalDeliveries" to FieldValue.increment(1L),
                        "totalEarningsBdt" to FieldValue.increment(earned),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun reportDeliveryFailure(orderId: String, reason: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "failureReason" to reason.trim().take(250),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Result.failure(e)
        }
    }
}
