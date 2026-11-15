package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BuildConfig
import com.example.R
import com.example.auth.authStateFlow
import com.example.data.local.LocalPreferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppNotification
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.DeliveryRequest
import com.example.data.model.FoodCategory
import com.example.data.model.MenuAddonOption
import com.example.data.model.MenuItem
import com.example.data.model.MenuSizeOption
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStage
import com.example.data.model.PaymentMethod
import com.example.data.model.PromoBanner
import com.example.data.model.Restaurant
import com.example.data.model.ReviewRecord
import com.example.data.model.RiderProfile
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.normalizeBangladeshPhone
import com.example.data.remote.EfoodBackendApi
import com.example.data.remote.EfoodRepository
import com.example.data.remote.PaymentSessionRequest
import com.example.data.seed.EfoodCatalogSeed
import com.example.notifications.EfoodNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class PendingCartConflict(
    val currentRestaurantName: String,
    val newRestaurant: Restaurant,
    val newItem: CartItem,
    val goToCheckoutAfter: Boolean
)

data class EfoodUiState(
    val language: AppLanguage = AppLanguage.EN,
    val selectedDistrict: String = "Dhaka",
    val selectedArea: String = "Dhanmondi",
    val categories: List<FoodCategory> = EfoodCatalogSeed.categories,
    val banners: List<PromoBanner> = EfoodCatalogSeed.banners,
    val coupons: List<Coupon> = EfoodCatalogSeed.coupons,
    val restaurants: List<Restaurant> = EfoodCatalogSeed.restaurants,
    val menuItems: List<MenuItem> = EfoodCatalogSeed.menuItems,
    val recentlyViewedIds: List<String> = emptyList(),
    val favoriteRestaurantIds: Set<String> = emptySet(),
    val favoriteFoodIds: Set<String> = emptySet(),
    val cartItems: List<CartItem> = emptyList(),
    val cartRestaurantId: String? = null,
    val cartRestaurantName: String? = null,
    val appliedCoupon: Coupon? = null,
    val pendingCartConflict: PendingCartConflict? = null,
    val currentUser: FirebaseUser? = null,
    val userProfile: UserProfile? = null,
    val savedAddresses: List<SavedAddress> = emptyList(),
    val selectedAddressId: String? = null,
    val customerOrders: List<OrderRecord> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val supportTickets: List<SupportTicket> = emptyList(),
    val selectedRestaurantReviews: List<ReviewRecord> = emptyList(),
    val riderProfile: RiderProfile? = null,
    val openDeliveryRequests: List<DeliveryRequest> = emptyList(),
    val riderAssignedOrders: List<OrderRecord> = emptyList(),
    val riderRequestCountdownSec: Int = 30,
    val isBusy: Boolean = false,
    val snackbarMessage: String? = null,
    val activeGatewaySessionUrl: String? = null,
    val pendingGatewayNotice: String? = null,
    val navigateToCheckoutTrigger: Long = 0L,
    val navigateToOrderTrackingId: String? = null
) {
    val cartItemCount: Int get() = cartItems.sumOf { it.quantity }
    val cartSubtotalBdt: Double get() = cartItems.sumOf { it.lineTotalBdt }
    val cartDeliveryFeeBdt: Double
        get() {
            if (cartItems.isEmpty()) return 0.0
            val rest = restaurants.firstOrNull { it.id == cartRestaurantId }
            return rest?.deliveryFeeBdt ?: 40.0
        }
    val cartDiscountBdt: Double
        get() = appliedCoupon?.calculateDiscount(cartSubtotalBdt) ?: 0.0
    val cartTaxBdt: Double
        get() = kotlin.math.round((cartSubtotalBdt - cartDiscountBdt).coerceAtLeast(0.0) * 0.05)
    val cartTotalBdt: Double
        get() = (cartSubtotalBdt - cartDiscountBdt + cartDeliveryFeeBdt + cartTaxBdt).coerceAtLeast(0.0)
    val unreadNotificationsCount: Int
        get() = notifications.count { !it.isRead }
    val isPhoneVerifiedForCheckout: Boolean
        get() = userProfile?.phoneVerified == true && !userProfile.phone.isNullOrBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class EfoodViewModel(
    private val app: Application,
    private val repository: EfoodRepository,
    private val localPrefs: LocalPreferencesDataStore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val backendApi: EfoodBackendApi? = EfoodBackendApi.createOrNull()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EfoodUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<EfoodUiState> = _uiState.asStateFlow()

    private var authSessionJob: Job? = null
    private var reviewsJob: Job? = null
    private var countdownJob: Job? = null
    private var lastKnownOrderStatuses: Map<String, String> = emptyMap()

    init {
        EfoodNotificationHelper.ensureChannel(app)
        observeLocalPreferences()
        observeAuthAndCloudStreams()
    }

    private fun observeLocalPreferences() {
        viewModelScope.launch {
            localPrefs.languageFlow.collectLatest { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            localPrefs.selectedLocationFlow.collectLatest { (district, area) ->
                _uiState.update { it.copy(selectedDistrict = district, selectedArea = area) }
            }
        }
        viewModelScope.launch {
            localPrefs.recentlyViewedIdsFlow.collectLatest { ids ->
                _uiState.update { it.copy(recentlyViewedIds = ids) }
            }
        }
        viewModelScope.launch {
            localPrefs.localFavRestaurantsFlow.collectLatest { favs ->
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(favoriteRestaurantIds = favs) }
                }
            }
        }
        viewModelScope.launch {
            localPrefs.localFavFoodsFlow.collectLatest { favs ->
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(favoriteFoodIds = favs) }
                }
            }
        }
    }

    private fun observeAuthAndCloudStreams() {
        viewModelScope.launch {
            auth.authStateFlow().collectLatest { user ->
                authSessionJob?.cancel()
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    attachAuthenticatedStreams(user)
                } else {
                    lastKnownOrderStatuses = emptyMap()
                    _uiState.update {
                        it.copy(
                            userProfile = null,
                            savedAddresses = emptyList(),
                            customerOrders = emptyList(),
                            notifications = emptyList(),
                            supportTickets = emptyList(),
                            riderProfile = null,
                            openDeliveryRequests = emptyList(),
                            riderAssignedOrders = emptyList()
                        )
                    }
                }
            }
        }
    }

    private fun attachAuthenticatedStreams(user: FirebaseUser) {
        val uid = user.uid
        authSessionJob = viewModelScope.launch {
            // Ensure customer profile document exists
            repository.ensureUserProfile(
                displayName = user.displayName ?: "eFood Customer",
                email = user.email ?: "",
                preferredLanguage = _uiState.value.language.code
            )

            launchStream(repository.observeUserProfile(uid)) { profile ->
                if (profile != null) {
                    val lang = if (profile.preferredLanguage == "bn") AppLanguage.BN else AppLanguage.EN
                    _uiState.update { it.copy(userProfile = profile, language = lang) }
                }
            }

            launchStream(repository.observeAddresses(uid)) { addresses ->
                val defaultId = addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id
                _uiState.update { state ->
                    state.copy(
                        savedAddresses = addresses,
                        selectedAddressId = state.selectedAddressId
                            ?.takeIf { id -> addresses.any { it.id == id } } ?: defaultId
                    )
                }
            }

            launchStream(repository.observeFavorites(uid)) { entries ->
                val restFavs = entries.filter { it.targetType == "RESTAURANT" }.map { it.targetId }.toSet()
                val foodFavs = entries.filter { it.targetType == "FOOD" }.map { it.targetId }.toSet()
                _uiState.update {
                    it.copy(favoriteRestaurantIds = restFavs, favoriteFoodIds = foodFavs)
                }
            }

            launchStream(repository.observeCustomerOrders(uid)) { orders ->
                orders.forEach { order ->
                    val prev = lastKnownOrderStatuses[order.id]
                    if (prev != null && prev != order.status) {
                        EfoodNotificationHelper.showOrderUpdate(
                            app,
                            order.id,
                            "${order.restaurantName} • ${order.stage.labelEn}",
                            "Order #${order.id.takeLast(6)} is now ${order.stage.labelEn}"
                        )
                    }
                }
                lastKnownOrderStatuses = orders.associate { it.id to it.status }
                _uiState.update { it.copy(customerOrders = orders) }
            }

            launchStream(repository.observeNotifications(uid)) { list ->
                _uiState.update { it.copy(notifications = list) }
            }

            launchStream(repository.observeSupportTickets(uid)) { list ->
                _uiState.update { it.copy(supportTickets = list) }
            }

            // Admin-managed Firestore catalog overrides (merged on top of baseline catalog)
            launchStream(repository.observeActiveCategories()) { remoteCats ->
                if (remoteCats.isNotEmpty()) {
                    _uiState.update { it.copy(categories = remoteCats) }
                }
            }
            launchStream(repository.observeActiveRestaurants()) { remoteRests ->
                if (remoteRests.isNotEmpty()) {
                    val merged = (remoteRests + EfoodCatalogSeed.restaurants).distinctBy { it.id }
                    _uiState.update { it.copy(restaurants = merged) }
                }
            }
            launchStream(repository.observeActiveMenuItems()) { remoteItems ->
                if (remoteItems.isNotEmpty()) {
                    val merged = (remoteItems + EfoodCatalogSeed.menuItems).distinctBy { it.id }
                    _uiState.update { it.copy(menuItems = merged) }
                }
            }
            launchStream(repository.observeActiveBanners()) { remoteBanners ->
                if (remoteBanners.isNotEmpty()) {
                    _uiState.update { it.copy(banners = remoteBanners) }
                }
            }
            launchStream(repository.observeActiveCoupons()) { remoteCoupons ->
                if (remoteCoupons.isNotEmpty()) {
                    val merged = (remoteCoupons + EfoodCatalogSeed.coupons).distinctBy { it.code }
                    _uiState.update { it.copy(coupons = merged) }
                }
            }

            // Rider Partner streams
            launchStream(repository.observeRiderProfile(uid)) { rider ->
                _uiState.update { it.copy(riderProfile = rider) }
                if (rider?.approvalStatus == "APPROVED" && rider.isOnline) {
                    startRiderRequestCountdown()
                }
            }
            launchStream(repository.observeRiderAssignedOrders(uid)) { assigned ->
                _uiState.update { it.copy(riderAssignedOrders = assigned) }
            }
            launchStream(repository.observeOpenDeliveryRequests()) { openReqs ->
                _uiState.update { it.copy(openDeliveryRequests = openReqs) }
            }
        }
    }

    private fun <T> kotlinx.coroutines.CoroutineScope.launchStream(
        flow: Flow<T>,
        onValue: (T) -> Unit
    ) {
        launch {
            flow.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error: ${root.message}", root)
            }.collectLatest { value ->
                onValue(value)
            }
        }
    }

    private fun startRiderRequestCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = 30
            while (remaining >= 0) {
                _uiState.update { it.copy(riderRequestCountdownSec = remaining) }
                delay(1000L)
                remaining = if (remaining == 0) 30 else remaining - 1
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun consumeCheckoutTrigger() {
        _uiState.update { it.copy(navigateToCheckoutTrigger = 0L) }
    }

    fun consumeOrderTrackingTrigger() {
        _uiState.update { it.copy(navigateToOrderTrackingId = null) }
    }

    fun dismissGatewayNotice() {
        _uiState.update { it.copy(pendingGatewayNotice = null, activeGatewaySessionUrl = null) }
    }

    // --- Language & Location ---

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
        viewModelScope.launch {
            localPrefs.setLanguage(next)
            if (_uiState.value.currentUser != null) {
                repository.updatePreferredLanguage(next.code)
            }
        }
    }

    fun setDeliveryLocation(district: String, area: String) {
        viewModelScope.launch {
            localPrefs.setDeliveryLocation(district, area)
            showMessage("Delivery location set to $area, $district")
        }
    }

    fun recordFoodViewed(itemId: String) {
        viewModelScope.launch {
            localPrefs.addRecentlyViewed(itemId)
        }
    }

    // --- Favorites ---

    fun toggleFavoriteRestaurant(restaurantId: String) {
        val currentlyFav = restaurantId in _uiState.value.favoriteRestaurantIds
        viewModelScope.launch {
            if (_uiState.value.currentUser != null) {
                repository.setFavorite("RESTAURANT", restaurantId, !currentlyFav)
            } else {
                localPrefs.toggleLocalFavoriteRestaurant(restaurantId)
            }
        }
    }

    fun toggleFavoriteFood(foodId: String) {
        val currentlyFav = foodId in _uiState.value.favoriteFoodIds
        viewModelScope.launch {
            if (_uiState.value.currentUser != null) {
                repository.setFavorite("FOOD", foodId, !currentlyFav)
            } else {
                localPrefs.toggleLocalFavoriteFood(foodId)
            }
        }
    }

    // --- Cart Management (Single Restaurant Enforcement) ---

    fun addToCart(
        restaurant: Restaurant,
        item: MenuItem,
        size: MenuSizeOption,
        addons: List<MenuAddonOption>,
        customization: String,
        instructions: String,
        quantity: Int,
        buyNow: Boolean
    ) {
        val addonKey = addons.sortedBy { it.id }.joinToString(",") { it.id }
        val key = "${item.id}|${size.name}|$addonKey|$customization"
        val newCartItem = CartItem(
            cartKey = key,
            restaurantId = restaurant.id,
            restaurantName = restaurant.name,
            menuItem = item,
            selectedSize = size,
            selectedAddons = addons,
            selectedCustomization = customization,
            specialInstructions = instructions,
            quantity = quantity.coerceAtLeast(1)
        )

        val currentRestId = _uiState.value.cartRestaurantId
        if (currentRestId != null && currentRestId != restaurant.id && _uiState.value.cartItems.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    pendingCartConflict = PendingCartConflict(
                        currentRestaurantName = it.cartRestaurantName ?: "another restaurant",
                        newRestaurant = restaurant,
                        newItem = newCartItem,
                        goToCheckoutAfter = buyNow
                    )
                )
            }
            return
        }

        commitAddToCart(restaurant, newCartItem, buyNow)
    }

    fun confirmReplaceCart() {
        val conflict = _uiState.value.pendingCartConflict ?: return
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                cartRestaurantId = null,
                cartRestaurantName = null,
                appliedCoupon = null,
                pendingCartConflict = null
            )
        }
        commitAddToCart(conflict.newRestaurant, conflict.newItem, conflict.goToCheckoutAfter)
    }

    fun dismissReplaceCart() {
        _uiState.update { it.copy(pendingCartConflict = null) }
    }

    private fun commitAddToCart(restaurant: Restaurant, newItem: CartItem, buyNow: Boolean) {
        _uiState.update { state ->
            val existingIdx = state.cartItems.indexOfFirst { it.cartKey == newItem.cartKey }
            val updatedList = if (existingIdx >= 0) {
                state.cartItems.toMutableList().apply {
                    val prev = this[existingIdx]
                    this[existingIdx] = prev.copy(
                        quantity = prev.quantity + newItem.quantity,
                        specialInstructions = newItem.specialInstructions.ifBlank { prev.specialInstructions }
                    )
                }
            } else {
                state.cartItems + newItem
            }
            state.copy(
                cartItems = updatedList,
                cartRestaurantId = restaurant.id,
                cartRestaurantName = restaurant.name,
                snackbarMessage = if (buyNow) null else "Added ${newItem.menuItem.name} to cart",
                navigateToCheckoutTrigger = if (buyNow) System.currentTimeMillis() else state.navigateToCheckoutTrigger
            )
        }
    }

    fun updateCartItemQuantity(cartKey: String, delta: Int) {
        _uiState.update { state ->
            val updated = state.cartItems.mapNotNull { item ->
                if (item.cartKey == cartKey) {
                    val nextQty = item.quantity + delta
                    if (nextQty <= 0) null else item.copy(quantity = nextQty)
                } else item
            }
            state.copy(
                cartItems = updated,
                cartRestaurantId = if (updated.isEmpty()) null else state.cartRestaurantId,
                cartRestaurantName = if (updated.isEmpty()) null else state.cartRestaurantName,
                appliedCoupon = if (updated.isEmpty()) null else state.appliedCoupon
            )
        }
    }

    fun applyPromoCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) {
            _uiState.update { it.copy(appliedCoupon = null, snackbarMessage = "Promo code cleared") }
            return
        }
        val coupon = _uiState.value.coupons.firstOrNull { it.code.equals(code, ignoreCase = true) && it.isActive }
        if (coupon == null) {
            showMessage("Promo code \"$code\" is invalid or expired")
            return
        }
        val subtotal = _uiState.value.cartSubtotalBdt
        if (subtotal < coupon.minOrderBdt) {
            showMessage("Minimum order ৳${coupon.minOrderBdt.toInt()} required for $code")
            return
        }
        _uiState.update {
            it.copy(
                appliedCoupon = coupon,
                snackbarMessage = "Applied ${coupon.code}: -৳${coupon.calculateDiscount(subtotal).toInt()}"
            )
        }
    }

    // --- Profile, Phone Verification & Addresses ---

    fun saveVerifiedPhoneAndName(name: String, rawPhone: String) {
        val normalized = normalizeBangladeshPhone(rawPhone)
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.updateCustomerContact(
                displayName = name,
                phone = normalized,
                phoneVerified = true
            )
            _uiState.update { it.copy(isBusy = false) }
            result.onSuccess {
                showMessage("Contact verified ($normalized)")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to update contact")
            }
        }
    }

    fun saveAddress(address: SavedAddress) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.saveAddress(address)
            _uiState.update { it.copy(isBusy = false) }
            result.onSuccess { newId ->
                _uiState.update { it.copy(selectedAddressId = newId) }
                localPrefs.setDeliveryLocation(address.district, address.area)
                showMessage("Saved ${address.label} address")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to save address")
            }
        }
    }

    fun selectAddress(addressId: String) {
        val addr = _uiState.value.savedAddresses.firstOrNull { it.id == addressId } ?: return
        _uiState.update { it.copy(selectedAddressId = addressId) }
        viewModelScope.launch {
            localPrefs.setDeliveryLocation(addr.district, addr.area)
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(addressId)
            showMessage("Address deleted")
        }
    }

    // --- Checkout & Order Placement ---

    fun placeOrder(
        customerName: String,
        customerPhone: String,
        addressText: String,
        area: String,
        district: String,
        landmark: String,
        instructions: String,
        lat: Double,
        lng: Double,
        paymentMethod: PaymentMethod
    ) {
        val state = _uiState.value
        val user = state.currentUser
        if (user == null) {
            showMessage("Please sign in with Google before placing an order")
            return
        }
        val restaurantId = state.cartRestaurantId ?: return
        val restaurantName = state.cartRestaurantName ?: "eFood Partner"
        if (state.cartItems.isEmpty()) return

        val restaurant = state.restaurants.firstOrNull { it.id == restaurantId }
        if (restaurant != null && state.cartSubtotalBdt < restaurant.minOrderBdt) {
            showMessage("Minimum order for ${restaurant.name} is ৳${restaurant.minOrderBdt.toInt()}")
            return
        }

        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val summary = state.cartItems.joinToString(" • ") { item ->
            val addonsLabel = if (item.selectedAddons.isNotEmpty()) {
                " (${item.selectedAddons.joinToString { it.name }})"
            } else ""
            "${item.quantity}x ${item.menuItem.name} [${item.selectedSize.name}]$addonsLabel"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            // Online gateway check (bKash / Nagad / Card) — never simulate payment confirmations!
            if (paymentMethod != PaymentMethod.COD) {
                if (backendApi == null || BuildConfig.EFOOD_API_BASE_URL.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingGatewayNotice = "${paymentMethod.labelEn} requires a live merchant gateway backend (`EFOOD_API_BASE_URL` with bKash/Nagad/SSLCommerz server credentials). Configure your backend server or choose Cash on Delivery to place this order right now."
                        )
                    }
                    return@launch
                } else {
                    try {
                        val token = user.getIdToken(false).await().token.orEmpty()
                        val session = backendApi.initiatePayment(
                            bearerToken = "Bearer $token",
                            request = PaymentSessionRequest(
                                orderId = orderId,
                                amountBdt = state.cartTotalBdt,
                                paymentMethod = paymentMethod.code,
                                customerPhone = normalizeBangladeshPhone(customerPhone),
                                customerName = customerName
                            )
                        )
                        _uiState.update {
                            it.copy(isBusy = false, activeGatewaySessionUrl = session.gatewayUrl)
                        }
                        return@launch
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                pendingGatewayNotice = "Payment gateway error: ${e.localizedMessage ?: "Unable to connect to merchant server"}. Please try again or use Cash on Delivery."
                            )
                        }
                        return@launch
                    }
                }
            }

            val order = OrderRecord(
                id = orderId,
                customerId = user.uid,
                customerName = customerName,
                customerPhone = normalizeBangladeshPhone(customerPhone),
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                deliveryAddressText = addressText,
                deliveryArea = area,
                deliveryDistrict = district,
                deliveryLandmark = landmark,
                deliveryInstructions = instructions,
                deliveryLat = lat,
                deliveryLng = lng,
                itemsSummary = summary,
                subtotalBdt = state.cartSubtotalBdt,
                deliveryFeeBdt = state.cartDeliveryFeeBdt,
                discountBdt = state.cartDiscountBdt,
                taxBdt = state.cartTaxBdt,
                totalBdt = state.cartTotalBdt,
                promoCode = state.appliedCoupon?.code.orEmpty(),
                paymentMethod = paymentMethod.code,
                paymentStatus = "COD_DUE",
                status = OrderStage.ORDER_PLACED.code,
                deliveryOtp = (1000..9999).random().toString(),
                estimatedMinutes = restaurant?.deliveryTimeMinutes ?: 35L
            )

            val result = repository.placeOrder(order)
            _uiState.update { it.copy(isBusy = false) }
            result.onSuccess { createdId ->
                _uiState.update {
                    it.copy(
                        cartItems = emptyList(),
                        cartRestaurantId = null,
                        cartRestaurantName = null,
                        appliedCoupon = null,
                        snackbarMessage = "Order #$createdId placed successfully!",
                        navigateToOrderTrackingId = createdId
                    )
                }
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not place order")
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.cancelOrder(orderId, reason)
            res.onSuccess { showMessage("Order cancelled") }
                .onFailure { showMessage(it.localizedMessage ?: "Cannot cancel order") }
        }
    }

    // --- Reviews, Notifications & Support ---

    fun observeReviewsForRestaurant(restaurantId: String) {
        reviewsJob?.cancel()
        if (_uiState.value.currentUser == null) {
            _uiState.update { it.copy(selectedRestaurantReviews = emptyList()) }
            return
        }
        reviewsJob = viewModelScope.launch {
            repository.observeRestaurantReviews(restaurantId)
                .catch { _ -> }
                .collectLatest { reviews ->
                    _uiState.update { it.copy(selectedRestaurantReviews = reviews) }
                }
        }
    }

    fun submitReview(restaurantId: String, orderId: String, rating: Double, comment: String) {
        val author = _uiState.value.userProfile?.displayName
            ?: _uiState.value.currentUser?.displayName ?: "eFood Customer"
        viewModelScope.launch {
            val res = repository.submitReview(author, restaurantId, orderId, rating, comment)
            res.onSuccess { showMessage("Thank you for your review!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to submit review") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun submitSupportTicket(subject: String, message: String, orderId: String = "") {
        viewModelScope.launch {
            val res = repository.submitSupportTicket(subject, message, orderId)
            res.onSuccess { showMessage("Support ticket submitted. Our Dhaka team will respond shortly.") }
                .onFailure { showMessage(it.localizedMessage ?: "Could not submit ticket") }
        }
    }

    // --- Rider Partner Workflows ---

    fun registerOrUpdateRider(
        fullName: String,
        phone: String,
        nidNumber: String,
        vehicleType: String,
        emergencyContact: String,
        photoUri: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.registerOrUpdateRider(
                RiderProfile(
                    fullName = fullName,
                    phone = normalizeBangladeshPhone(phone),
                    nidNumber = nidNumber,
                    vehicleType = vehicleType,
                    emergencyContact = normalizeBangladeshPhone(emergencyContact),
                    photoUri = photoUri
                )
            )
            _uiState.update { it.copy(isBusy = false) }
            res.onSuccess {
                showMessage("Rider profile submitted. Admin approval status synced.")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to save rider registration")
            }
        }
    }

    fun toggleRiderOnline(isOnline: Boolean, lat: Double?, lng: Double?) {
        viewModelScope.launch {
            val res = repository.setRiderOnline(isOnline, lat, lng)
            res.onSuccess {
                showMessage(if (isOnline) "You are now ONLINE for deliveries" else "You are now OFFLINE")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not update status")
            }
        }
    }

    fun acceptDeliveryRequest(request: DeliveryRequest) {
        val rider = _uiState.value.riderProfile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.claimDeliveryRequest(request, rider.fullName, rider.phone)
            _uiState.update { it.copy(isBusy = false) }
            res.onSuccess {
                showMessage("Accepted delivery for ${request.restaurantName}")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not claim delivery")
            }
        }
    }

    fun advanceRiderStage(
        order: OrderRecord,
        nextStage: OrderStage,
        otp: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        shareLocation: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.advanceRiderDeliveryStage(
                order = order,
                nextStatus = nextStage.code,
                enteredOtp = otp,
                riderLat = lat,
                riderLng = lng,
                shareLocation = shareLocation
            )
            _uiState.update { it.copy(isBusy = false) }
            res.onSuccess {
                showMessage("Order updated to ${nextStage.labelEn}")
            }.onFailure { err ->
                showMessage(
                    if (nextStage == OrderStage.DELIVERED) {
                        "Invalid delivery OTP or permission denied. Verify the 4-digit OTP from the customer."
                    } else {
                        err.localizedMessage ?: "Failed to update delivery status"
                    }
                )
            }
        }
    }

    fun reportDeliveryFailure(orderId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.reportDeliveryFailure(orderId, reason)
            res.onSuccess { showMessage("Failure report logged for admin review") }
                .onFailure { showMessage(it.localizedMessage ?: "Could not log report") }
        }
    }

    companion object {
        private const val TAG = "EfoodViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = EfoodRepository(db, Firebase.auth)
                val prefs = LocalPreferencesDataStore(app.applicationContext)
                EfoodViewModel(app, repo, prefs)
            }
        }
    }
}
