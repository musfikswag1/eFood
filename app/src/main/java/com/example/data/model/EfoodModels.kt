package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "English"),
    BN("bn", "বাংলা")
}

@IgnoreExtraProperties
data class FoodCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val iconKey: String = "fastfood",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Restaurant(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val cuisine: String = "",
    val categoryIds: List<String> = emptyList(),
    val area: String = "Dhanmondi",
    val district: String = "Dhaka",
    val deliveryTimeMinutes: Long = 30L,
    val minOrderBdt: Double = 150.0,
    val deliveryFeeBdt: Double = 40.0,
    val rating: Double = 4.6,
    val reviewCount: Long = 0L,
    val openHours: String = "10:00 AM - 11:30 PM",
    val isOpen: Boolean = true,
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val isActive: Boolean = true,
    val imageKey: String = "biryani",
    val logoUrl: String = "",
    val coverUrl: String = "",
    val latitude: Double = 23.7461,
    val longitude: Double = 90.3742,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MenuSizeOption(
    val name: String = "Regular",
    val extraPriceBdt: Double = 0.0
)

data class MenuAddonOption(
    val id: String = "",
    val name: String = "",
    val priceBdt: Double = 0.0
)

@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val nameBn: String = "",
    val description: String = "",
    val priceBdt: Double = 0.0,
    val discountPriceBdt: Double = 0.0,
    val ingredients: List<String> = emptyList(),
    val sizes: List<MenuSizeOption> = emptyList(),
    val addons: List<MenuAddonOption> = emptyList(),
    val customizationOptions: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    val isPopular: Boolean = false,
    val isBestSeller: Boolean = false,
    val isRecommended: Boolean = false,
    val isActive: Boolean = true,
    val imageKey: String = "biryani",
    val galleryKeys: List<String> = emptyList(),
    val imageUrl: String = "",
    val rating: Double = 4.7,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePriceBdt: Double
        get() = if (discountPriceBdt > 0.0 && discountPriceBdt < priceBdt) discountPriceBdt else priceBdt

    val hasDiscount: Boolean
        get() = discountPriceBdt > 0.0 && discountPriceBdt < priceBdt

    val discountPercent: Int
        get() = if (hasDiscount && priceBdt > 0.0) (((priceBdt - discountPriceBdt) / priceBdt) * 100).toInt() else 0
}

@IgnoreExtraProperties
data class PromoBanner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val promoCode: String = "",
    val imageKey: String = "biryani",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val discountType: String = "PERCENT", // PERCENT or FLAT
    val discountValue: Double = 0.0,
    val minOrderBdt: Double = 0.0,
    val maxDiscountBdt: Double = 0.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun calculateDiscount(subtotalBdt: Double): Double {
        if (!isActive || subtotalBdt < minOrderBdt) return 0.0
        val raw = if (discountType == "FLAT") discountValue else subtotalBdt * (discountValue / 100.0)
        return if (maxDiscountBdt > 0.0) minOf(raw, maxDiscountBdt) else raw
    }
}

data class CartItem(
    val cartKey: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val menuItem: MenuItem = MenuItem(),
    val selectedSize: MenuSizeOption = MenuSizeOption("Regular", 0.0),
    val selectedAddons: List<MenuAddonOption> = emptyList(),
    val selectedCustomization: String = "",
    val specialInstructions: String = "",
    val quantity: Int = 1
) {
    val unitPriceBdt: Double
        get() = menuItem.effectivePriceBdt + selectedSize.extraPriceBdt + selectedAddons.sumOf { it.priceBdt }

    val lineTotalBdt: Double
        get() = unitPriceBdt * quantity
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val phoneVerified: Boolean = false,
    val preferredLanguage: String = "en",
    val role: String = "customer",
    val isSuspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val division: String = "Dhaka",
    val district: String = "Dhaka",
    val area: String = "Dhanmondi",
    val landmark: String = "",
    val streetAddress: String = "",
    val latitude: Double = 23.7461,
    val longitude: Double = 90.3742,
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedSummary(): String =
        listOf(streetAddress, landmark.takeIf { it.isNotBlank() }, area, district)
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

@IgnoreExtraProperties
data class FavoriteEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "RESTAURANT",
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "ORDER",
    val orderId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class OrderStage(val code: String, val stepIndex: Int, val labelEn: String, val labelBn: String) {
    ORDER_PLACED("ORDER_PLACED", 0, "Order Placed", "অর্ডার সম্পন্ন হয়েছে"),
    ORDER_CONFIRMED("ORDER_CONFIRMED", 1, "Order Confirmed", "অর্ডার নিশ্চিত হয়েছে"),
    RESTAURANT_PREPARING("RESTAURANT_PREPARING", 2, "Restaurant Preparing", "রেস্টুরেন্ট খাবার তৈরি করছে"),
    FOOD_READY("FOOD_READY", 3, "Food Ready", "খাবার প্রস্তুত"),
    RIDER_ASSIGNED("RIDER_ASSIGNED", 4, "Rider Assigned", "রাইডার নির্ধারিত হয়েছে"),
    RIDER_PICKED_UP("RIDER_PICKED_UP", 5, "Rider Picked Up", "রাইডার খাবার সংগ্রহ করেছে"),
    ON_THE_WAY("ON_THE_WAY", 6, "On The Way", "ডেলিভারির পথে"),
    DELIVERED("DELIVERED", 7, "Delivered", "ডেলিভারি সম্পন্ন"),
    CANCELLED("CANCELLED", -1, "Cancelled", "বাতিল করা হয়েছে");

    companion object {
        fun fromCode(code: String): OrderStage = entries.firstOrNull { it.code == code } ?: ORDER_PLACED
        val trackingSteps: List<OrderStage> = listOf(
            ORDER_PLACED,
            ORDER_CONFIRMED,
            RESTAURANT_PREPARING,
            FOOD_READY,
            RIDER_ASSIGNED,
            RIDER_PICKED_UP,
            ON_THE_WAY,
            DELIVERED
        )
    }
}

enum class PaymentMethod(val code: String, val labelEn: String, val labelBn: String) {
    COD("COD", "Cash on Delivery", "ক্যাশ অন ডেলিভারি"),
    BKASH("BKASH", "bKash", "বিকাশ"),
    NAGAD("NAGAD", "Nagad", "নগদ"),
    CARD("CARD", "Visa / Mastercard", "কার্ড পেমেন্ট");

    companion object {
        fun fromCode(code: String): PaymentMethod = entries.firstOrNull { it.code == code } ?: COD
    }
}

@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val riderPhone: String = "",
    val riderLat: Double = 0.0,
    val riderLng: Double = 0.0,
    val riderLocationSharing: Boolean = false,
    val deliveryAddressText: String = "",
    val deliveryArea: String = "",
    val deliveryDistrict: String = "",
    val deliveryLandmark: String = "",
    val deliveryInstructions: String = "",
    val deliveryLat: Double = 23.7461,
    val deliveryLng: Double = 90.3742,
    val itemsSummary: String = "",
    val subtotalBdt: Double = 0.0,
    val deliveryFeeBdt: Double = 0.0,
    val discountBdt: Double = 0.0,
    val taxBdt: Double = 0.0,
    val totalBdt: Double = 0.0,
    val promoCode: String = "",
    val paymentMethod: String = "COD",
    val paymentStatus: String = "COD_DUE",
    val paymentReference: String = "",
    val status: String = "ORDER_PLACED",
    val deliveryOtp: String = "0000",
    val estimatedMinutes: Long = 35L,
    val failureReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val stage: OrderStage get() = OrderStage.fromCode(status)
    val payment: PaymentMethod get() = PaymentMethod.fromCode(paymentMethod)
}

@IgnoreExtraProperties
data class DeliveryRequest(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val restaurantName: String = "",
    val pickupArea: String = "",
    val dropoffArea: String = "",
    val estimatedFeeBdt: Double = 55.0,
    val distanceKm: Double = 2.5,
    val status: String = "OPEN",
    val claimedByRiderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RiderProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val nidNumber: String = "",
    val vehicleType: String = "MOTORBIKE",
    val emergencyContact: String = "",
    val photoUri: String = "",
    val approvalStatus: String = "PENDING", // PENDING, APPROVED, SUSPENDED
    val isOnline: Boolean = false,
    val currentLat: Double = 23.7461,
    val currentLng: Double = 90.3742,
    val totalDeliveries: Long = 0L,
    val totalEarningsBdt: Double = 0.0,
    val commissionRate: Double = 0.80,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReviewRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val restaurantId: String = "",
    val orderId: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val isVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
