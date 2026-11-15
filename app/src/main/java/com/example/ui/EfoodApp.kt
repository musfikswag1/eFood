package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.attemptAutoSignIn
import com.example.data.model.MenuItem
import com.example.data.model.Restaurant
import com.example.data.model.formatBdt
import com.example.data.model.localizedStrings
import com.example.ui.components.GoogleAuthAndPhoneSheet
import com.example.ui.components.LocationPickerSheet
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FoodDetailSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.RestaurantDetailScreen
import com.example.ui.screens.RiderPortalScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.viewmodel.EfoodViewModel

sealed interface EfoodScreen {
    data object Home : EfoodScreen
    data class Search(val categoryId: String? = null) : EfoodScreen
    data object Orders : EfoodScreen
    data object Favorites : EfoodScreen
    data object Account : EfoodScreen
    data class RestaurantDetail(val restaurant: Restaurant) : EfoodScreen
    data object Cart : EfoodScreen
    data object Checkout : EfoodScreen
    data class Tracking(val orderId: String) : EfoodScreen
    data object Notifications : EfoodScreen
    data object RiderPortal : EfoodScreen
}

@Composable
fun EfoodApp(
    viewModel: EfoodViewModel = viewModel(factory = EfoodViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = localizedStrings(state.language)

    var currentScreen by remember { mutableStateOf<EfoodScreen>(EfoodScreen.Home) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var activeFoodDetail by remember { mutableStateOf<MenuItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Silent Credential Manager auto-login check on startup (never blocks browsing!)
    LaunchedEffect(Unit) {
        val cm = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = cm,
            onAuthSuccess = { _ -> },
            onUnauthenticated = { },
            scope = scope
        )
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(state.navigateToCheckoutTrigger) {
        if (state.navigateToCheckoutTrigger > 0L) {
            currentScreen = EfoodScreen.Checkout
            viewModel.consumeCheckoutTrigger()
        }
    }

    LaunchedEffect(state.navigateToOrderTrackingId) {
        val id = state.navigateToOrderTrackingId
        if (id != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            currentScreen = EfoodScreen.Tracking(id)
            viewModel.consumeOrderTrackingTrigger()
        }
    }

    // Single-restaurant cart enforcement dialog
    if (state.pendingCartConflict != null) {
        val conflict = state.pendingCartConflict!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissReplaceCart() },
            title = { Text("Start a new cart?") },
            text = {
                Text(
                    "Your cart currently has items from ${conflict.currentRestaurantName}. Replace it with ${conflict.newItem.menuItem.name} from ${conflict.newRestaurant.name}?"
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmReplaceCart() }) {
                    Text("Replace Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReplaceCart() }) {
                    Text("Keep Current")
                }
            }
        )
    }

    if (showLocationSheet) {
        LocationPickerSheet(
            currentDistrict = state.selectedDistrict,
            currentArea = state.selectedArea,
            onSelectLocation = { dist, area -> viewModel.setDeliveryLocation(dist, area) },
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showAuthSheet) {
        GoogleAuthAndPhoneSheet(
            currentUser = state.currentUser,
            initialName = state.userProfile?.displayName ?: "",
            initialPhone = state.userProfile?.phone ?: "",
            phoneVerified = state.isPhoneVerifiedForCheckout,
            onVerifyContact = { name, phone -> viewModel.saveVerifiedPhoneAndName(name, phone) },
            onAuthSuccess = {
                if (currentScreen is EfoodScreen.RiderPortal) {
                    showAuthSheet = false
                }
            },
            onDismiss = { showAuthSheet = false }
        )
    }

    if (activeFoodDetail != null) {
        val dish = activeFoodDetail!!
        val rest = state.restaurants.firstOrNull { it.id == dish.restaurantId } ?: state.restaurants.first()
        FoodDetailSheet(
            item = dish,
            restaurant = rest,
            language = state.language,
            onAddToCart = { size, addons, custom, instr, qty, buyNow ->
                viewModel.addToCart(rest, dish, size, addons, custom, instr, qty, buyNow)
            },
            onDismiss = { activeFoodDetail = null }
        )
    }

    val showCustomerNav = currentScreen is EfoodScreen.Home ||
        currentScreen is EfoodScreen.Search ||
        currentScreen is EfoodScreen.Orders ||
        currentScreen is EfoodScreen.Favorites ||
        currentScreen is EfoodScreen.Account

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showCustomerNav && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        CustomerNavItems(
                            currentScreen = currentScreen,
                            strings = strings,
                            onSelect = { currentScreen = it }
                        ).forEach { navItem ->
                            NavigationBarItem(
                                selected = navItem.selected,
                                onClick = navItem.onClick,
                                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                                label = { Text(navItem.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(navItem.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showCustomerNav && useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        CustomerNavItems(
                            currentScreen = currentScreen,
                            strings = strings,
                            onSelect = { currentScreen = it }
                        ).forEach { navItem ->
                            NavigationRailItem(
                                selected = navItem.selected,
                                onClick = navItem.onClick,
                                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                                label = { Text(navItem.label) }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                    ) {
                        when (val screen = currentScreen) {
                            is EfoodScreen.Home -> HomeScreen(
                                state = state,
                                onOpenLocationPicker = { showLocationSheet = true },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onOpenNotifications = { currentScreen = EfoodScreen.Notifications },
                                onOpenSearch = { catId -> currentScreen = EfoodScreen.Search(catId) },
                                onOpenRestaurant = { rest -> currentScreen = EfoodScreen.RestaurantDetail(rest) },
                                onOpenFoodDetail = { item ->
                                    viewModel.recordFoodViewed(item.id)
                                    activeFoodDetail = item
                                },
                                onToggleFavoriteRestaurant = { viewModel.toggleFavoriteRestaurant(it) },
                                onToggleFavoriteFood = { viewModel.toggleFavoriteFood(it) },
                                onApplyBannerPromo = { code ->
                                    viewModel.applyPromoCode(code)
                                    currentScreen = EfoodScreen.Search(null)
                                }
                            )

                            is EfoodScreen.Search -> SearchScreen(
                                state = state,
                                initialCategoryId = screen.categoryId,
                                onOpenRestaurant = { rest -> currentScreen = EfoodScreen.RestaurantDetail(rest) },
                                onOpenFoodDetail = { item ->
                                    viewModel.recordFoodViewed(item.id)
                                    activeFoodDetail = item
                                },
                                onToggleFavoriteRestaurant = { viewModel.toggleFavoriteRestaurant(it) },
                                onToggleFavoriteFood = { viewModel.toggleFavoriteFood(it) }
                            )

                            is EfoodScreen.RestaurantDetail -> RestaurantDetailScreen(
                                restaurant = screen.restaurant,
                                state = state,
                                onBack = { currentScreen = EfoodScreen.Home },
                                onOpenFoodDetail = { item ->
                                    viewModel.recordFoodViewed(item.id)
                                    activeFoodDetail = item
                                },
                                onToggleFavorite = { viewModel.toggleFavoriteRestaurant(screen.restaurant.id) },
                                onObserveReviews = { viewModel.observeReviewsForRestaurant(it) }
                            )

                            is EfoodScreen.Cart -> CartScreen(
                                state = state,
                                onBack = { currentScreen = EfoodScreen.Home },
                                onUpdateQuantity = { key, delta -> viewModel.updateCartItemQuantity(key, delta) },
                                onApplyPromo = { viewModel.applyPromoCode(it) },
                                onProceedToCheckout = { currentScreen = EfoodScreen.Checkout }
                            )

                            is EfoodScreen.Checkout -> CheckoutScreen(
                                state = state,
                                onBack = { currentScreen = EfoodScreen.Cart },
                                onOpenAuthAndPhoneSheet = { showAuthSheet = true },
                                onOpenLocationPicker = { showLocationSheet = true },
                                onSelectSavedAddress = { viewModel.selectAddress(it) },
                                onSaveNewAddress = { viewModel.saveAddress(it) },
                                onPlaceOrder = { name, phone, addr, area, dist, landmark, instr, lat, lng, method ->
                                    viewModel.placeOrder(name, phone, addr, area, dist, landmark, instr, lat, lng, method)
                                },
                                onDismissGatewayNotice = { viewModel.dismissGatewayNotice() }
                            )

                            is EfoodScreen.Orders -> OrdersScreen(
                                state = state,
                                onOpenAuthSheet = { showAuthSheet = true },
                                onOpenOrderTracking = { ord -> currentScreen = EfoodScreen.Tracking(ord.id) },
                                onSubmitReview = { restId, ordId, rating, comment ->
                                    viewModel.submitReview(restId, ordId, rating, comment)
                                }
                            )

                            is EfoodScreen.Tracking -> {
                                val order = state.customerOrders.firstOrNull { it.id == screen.orderId }
                                if (order != null) {
                                    OrderTrackingScreen(
                                        order = order,
                                        language = state.language,
                                        onBack = { currentScreen = EfoodScreen.Orders },
                                        onCancelOrder = { id, reason -> viewModel.cancelOrder(id, reason) }
                                    )
                                } else {
                                    OrdersScreen(
                                        state = state,
                                        onOpenAuthSheet = { showAuthSheet = true },
                                        onOpenOrderTracking = { ord -> currentScreen = EfoodScreen.Tracking(ord.id) },
                                        onSubmitReview = { restId, ordId, rating, comment ->
                                            viewModel.submitReview(restId, ordId, rating, comment)
                                        }
                                    )
                                }
                            }

                            is EfoodScreen.Favorites -> FavoritesScreen(
                                state = state,
                                onOpenRestaurant = { rest -> currentScreen = EfoodScreen.RestaurantDetail(rest) },
                                onOpenFoodDetail = { item ->
                                    viewModel.recordFoodViewed(item.id)
                                    activeFoodDetail = item
                                },
                                onToggleFavoriteRestaurant = { viewModel.toggleFavoriteRestaurant(it) },
                                onToggleFavoriteFood = { viewModel.toggleFavoriteFood(it) }
                            )

                            is EfoodScreen.Account -> AccountScreen(
                                state = state,
                                onOpenAuthSheet = { showAuthSheet = true },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onOpenNotifications = { currentScreen = EfoodScreen.Notifications },
                                onSaveAddress = { viewModel.saveAddress(it) },
                                onDeleteAddress = { viewModel.deleteAddress(it) },
                                onSubmitSupportTicket = { subj, msg -> viewModel.submitSupportTicket(subj, msg) },
                                onOpenRiderPortal = { currentScreen = EfoodScreen.RiderPortal }
                            )

                            is EfoodScreen.Notifications -> NotificationsScreen(
                                state = state,
                                onBack = { currentScreen = EfoodScreen.Home },
                                onMarkRead = { viewModel.markNotificationRead(it) }
                            )

                            is EfoodScreen.RiderPortal -> RiderPortalScreen(
                                state = state,
                                onBackToCustomerApp = { currentScreen = EfoodScreen.Account },
                                onOpenGoogleSignIn = { showAuthSheet = true },
                                onSubmitRegistration = { name, phone, nid, vehicle, emergency, photo ->
                                    viewModel.registerOrUpdateRider(name, phone, nid, vehicle, emergency, photo)
                                },
                                onToggleOnline = { online, lat, lng -> viewModel.toggleRiderOnline(online, lat, lng) },
                                onAcceptRequest = { req -> viewModel.acceptDeliveryRequest(req) },
                                onAdvanceStage = { ord, stage, otp -> viewModel.advanceRiderStage(ord, stage, otp) },
                                onReportFailure = { ordId, reason -> viewModel.reportDeliveryFailure(ordId, reason) }
                            )
                        }

                        // Floating View Cart Pill across browsing screens
                        val showFloatingCart = state.cartItemCount > 0 &&
                            (currentScreen is EfoodScreen.Home ||
                                currentScreen is EfoodScreen.Search ||
                                currentScreen is EfoodScreen.RestaurantDetail ||
                                currentScreen is EfoodScreen.Favorites)

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFloatingCart,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Surface(
                                onClick = { currentScreen = EfoodScreen.Cart },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("floating_cart_bar")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${state.cartItemCount} items • ${strings.viewCart}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = formatBdt(state.cartTotalBdt),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavItemSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

private fun CustomerNavItems(
    currentScreen: EfoodScreen,
    strings: com.example.data.model.EfoodLocalizedStrings,
    onSelect: (EfoodScreen) -> Unit
): List<NavItemSpec> = listOf(
    NavItemSpec(strings.navHome, Icons.Default.Home, currentScreen is EfoodScreen.Home, "nav_home") {
        onSelect(EfoodScreen.Home)
    },
    NavItemSpec(strings.navSearch, Icons.Default.Search, currentScreen is EfoodScreen.Search, "nav_search") {
        onSelect(EfoodScreen.Search(null))
    },
    NavItemSpec(strings.navOrders, Icons.AutoMirrored.Filled.ReceiptLong, currentScreen is EfoodScreen.Orders, "nav_orders") {
        onSelect(EfoodScreen.Orders)
    },
    NavItemSpec(strings.navFavorites, Icons.Default.Favorite, currentScreen is EfoodScreen.Favorites, "nav_favorites") {
        onSelect(EfoodScreen.Favorites)
    },
    NavItemSpec(strings.navAccount, Icons.Default.Person, currentScreen is EfoodScreen.Account, "nav_account") {
        onSelect(EfoodScreen.Account)
    }
)
