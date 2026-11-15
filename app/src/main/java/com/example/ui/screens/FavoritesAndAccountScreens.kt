package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.auth.signOutUser
import com.example.data.model.AppLanguage
import com.example.data.model.MenuItem
import com.example.data.model.Restaurant
import com.example.data.model.SavedAddress
import com.example.ui.components.FoodDishCard
import com.example.ui.components.RestaurantCard
import com.example.ui.theme.EfoodSuccess
import com.example.ui.viewmodel.EfoodUiState

@Composable
fun FavoritesScreen(
    state: EfoodUiState,
    onOpenRestaurant: (Restaurant) -> Unit,
    onOpenFoodDetail: (MenuItem) -> Unit,
    onToggleFavoriteRestaurant: (String) -> Unit,
    onToggleFavoriteFood: (String) -> Unit
) {
    val favRestaurants = state.restaurants.filter { it.id in state.favoriteRestaurantIds }
    val favFoods = state.menuItems.filter { it.id in state.favoriteFoodIds }
    val restaurantMap = state.restaurants.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (state.language == AppLanguage.BN) "পছন্দের তালিকা" else "Favorites",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (favRestaurants.isEmpty() && favFoods.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No favorites saved yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap the heart icon on any restaurant or dish to save it here for quick reordering.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (favRestaurants.isNotEmpty()) {
                item {
                    Text("Favorite Restaurants (${favRestaurants.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                items(favRestaurants, key = { "fav_r_${it.id}" }) { rest ->
                    RestaurantCard(
                        restaurant = rest,
                        isFavorite = true,
                        onClick = { onOpenRestaurant(rest) },
                        onFavoriteToggle = { onToggleFavoriteRestaurant(rest.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (favFoods.isNotEmpty()) {
                item {
                    Text("Favorite Dishes (${favFoods.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                items(favFoods, key = { "fav_f_${it.id}" }) { item ->
                    FoodDishCard(
                        item = item,
                        restaurantName = restaurantMap[item.restaurantId]?.name ?: "eFood Kitchen",
                        language = state.language,
                        isFavorite = true,
                        onClick = { onOpenFoodDetail(item) },
                        onFavoriteToggle = { onToggleFavoriteFood(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AccountScreen(
    state: EfoodUiState,
    onOpenAuthSheet: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSaveAddress: (SavedAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSubmitSupportTicket: (subject: String, message: String) -> Unit,
    onOpenRiderPortal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSupportDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    if (showSupportDialog) {
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Customer Support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("Order delay / Refund / Address help") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitSupportTicket(subject, message)
                        showSupportDialog = false
                    },
                    enabled = subject.trim().length >= 2 && message.trim().length >= 5
                ) {
                    Text("Send Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var area by remember { mutableStateOf(state.selectedArea) }
        var street by remember { mutableStateOf("") }
        var landmark by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Bangladesh Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home/Office)") })
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area (e.g., Dhanmondi)") })
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("House, Road, Flat") })
                    OutlinedTextField(value = landmark, onValueChange = { landmark = it }, label = { Text("Landmark (Optional)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAddress(
                            SavedAddress(
                                label = label,
                                division = state.selectedDistrict,
                                district = state.selectedDistrict,
                                area = area,
                                landmark = landmark,
                                streetAddress = street,
                                isDefault = state.savedAddresses.isEmpty()
                            )
                        )
                        showAddAddressDialog = false
                    },
                    enabled = street.isNotBlank() && area.isNotBlank()
                ) {
                    Text("Save Address")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (state.language == AppLanguage.BN) "প্রোফাইল ও সেটিংস" else "Account & Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.userProfile?.displayName ?: state.currentUser?.displayName ?: "Guest Diner",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = state.currentUser?.email ?: "Browse freely • Sign in for orders & sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.currentUser == null) {
                        Button(
                            onClick = onOpenAuthSheet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_sign_in_button")
                        ) {
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (state.isPhoneVerifiedForCheckout) EfoodSuccess else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.userProfile?.phone?.takeIf { it.isNotBlank() } ?: "Phone not verified yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isPhoneVerifiedForCheckout) EfoodSuccess else MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = onOpenAuthSheet) {
                                Text("Edit / Verify")
                            }
                        }
                    }
                }
            }
        }

        // Separate Delivery Partner Portal Entry
        item {
            Card(
                onClick = onOpenRiderPortal,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_rider_portal_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("eFood Delivery Partner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Separate Rider Login, Dispatch Queue & Earnings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Saved Addresses
        if (state.currentUser != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saved Addresses (${state.savedAddresses.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            }
                            TextButton(onClick = { showAddAddressDialog = true }) { Text("+ Add") }
                        }
                        state.savedAddresses.forEach { addr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${addr.label} • ${addr.area}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                                    Text(addr.formattedSummary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete address", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language, Notifications, Support
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleLanguage)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Language / ভাষা", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Text(state.language.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenNotifications)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Notifications", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Text("${state.unreadNotificationsCount} unread", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (state.currentUser == null) onOpenAuthSheet() else showSupportDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Help & Customer Support", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Open Support Tickets
        if (state.supportTickets.isNotEmpty()) {
            item {
                Text("Recent Support Tickets", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.supportTickets, key = { it.id }) { ticket ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ticket.subject, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            Text(ticket.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (state.currentUser != null) {
            item {
                OutlinedButton(
                    onClick = { signOutUser(credentialManager, scope) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    state: EfoodUiState,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Notifications", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        if (state.notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("You're all caught up", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Order updates, rider alerts, and promo campaigns appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notifications, key = { it.id }) { notif ->
                    Card(
                        onClick = { if (!notif.isRead) onMarkRead(notif.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (notif.isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
