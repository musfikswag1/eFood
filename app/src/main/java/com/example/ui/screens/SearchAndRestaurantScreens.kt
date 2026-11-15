package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.MenuItem
import com.example.data.model.Restaurant
import com.example.data.model.formatBdt
import com.example.ui.components.CategoryChipItem
import com.example.ui.components.EfoodMediaImage
import com.example.ui.components.FoodDishCard
import com.example.ui.components.RestaurantCard
import com.example.ui.viewmodel.EfoodUiState
import java.util.Locale

@Composable
fun SearchScreen(
    state: EfoodUiState,
    initialCategoryId: String?,
    onOpenRestaurant: (Restaurant) -> Unit,
    onOpenFoodDetail: (MenuItem) -> Unit,
    onToggleFavoriteRestaurant: (String) -> Unit,
    onToggleFavoriteFood: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember(initialCategoryId) { mutableStateOf(initialCategoryId) }
    var discountsOnly by remember { mutableStateOf(false) }
    var fastDeliveryOnly by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("RATING") } // RATING, PRICE_LOW, TIME

    val restaurantMap = state.restaurants.associateBy { it.id }

    val filteredRestaurants = state.restaurants.filter { rest ->
        val matchesQuery = query.isBlank() ||
            rest.name.contains(query, ignoreCase = true) ||
            rest.cuisine.contains(query, ignoreCase = true) ||
            rest.area.contains(query, ignoreCase = true)
        val matchesCat = selectedCategoryId == null || selectedCategoryId in rest.categoryIds
        val matchesTime = !fastDeliveryOnly || rest.deliveryTimeMinutes <= 30L
        matchesQuery && matchesCat && matchesTime
    }.let { list ->
        when (sortBy) {
            "TIME" -> list.sortedBy { it.deliveryTimeMinutes }
            else -> list.sortedByDescending { it.rating }
        }
    }

    val filteredFoods = state.menuItems.filter { item ->
        val matchesQuery = query.isBlank() ||
            item.name.contains(query, ignoreCase = true) ||
            item.nameBn.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true)
        val matchesCat = selectedCategoryId == null || item.categoryId == selectedCategoryId
        val matchesDiscount = !discountsOnly || item.hasDiscount
        matchesQuery && matchesCat && matchesDiscount
    }.let { list ->
        when (sortBy) {
            "PRICE_LOW" -> list.sortedBy { it.effectivePriceBdt }
            else -> list.sortedByDescending { it.rating }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (state.language == AppLanguage.BN) "খাবার ও রেস্টুরেন্ট খুঁজুন" else "Search & Filter",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Biryani, Burger, Pizza, Dhanmondi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_query_input")
                )
            }
        }

        // Category Filter Row
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = { it.id }) { cat ->
                    val isSel = selectedCategoryId == cat.id
                    CategoryChipItem(
                        category = cat,
                        language = state.language,
                        selected = isSel,
                        onClick = { selectedCategoryId = if (isSel) null else cat.id }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter & Sort Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = discountsOnly,
                        onClick = { discountsOnly = !discountsOnly },
                        label = { Text("Offers Only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_discounts_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = fastDeliveryOnly,
                        onClick = { fastDeliveryOnly = !fastDeliveryOnly },
                        label = { Text("Under 30 min") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "RATING",
                        onClick = { sortBy = "RATING" },
                        label = { Text("Top Rated") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "PRICE_LOW",
                        onClick = { sortBy = "PRICE_LOW" },
                        label = { Text("Price: Low-High") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "TIME",
                        onClick = { sortBy = "TIME" },
                        label = { Text("Fastest") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Matching Dishes
        item {
            Text(
                text = "Dishes (${filteredFoods.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            if (filteredFoods.isEmpty()) {
                Text(
                    text = "No dishes matched your filter. Try clearing a filter chip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFoods, key = { it.id }) { item ->
                        FoodDishCard(
                            item = item,
                            restaurantName = restaurantMap[item.restaurantId]?.name ?: "eFood Kitchen",
                            language = state.language,
                            isFavorite = item.id in state.favoriteFoodIds,
                            onClick = { onOpenFoodDetail(item) },
                            onFavoriteToggle = { onToggleFavoriteFood(item.id) },
                            modifier = Modifier.width(190.dp)
                        )
                    }
                }
            }
        }

        // Matching Restaurants
        item {
            Text(
                text = "Restaurants (${filteredRestaurants.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
            )
        }
        items(filteredRestaurants, key = { it.id }) { rest ->
            RestaurantCard(
                restaurant = rest,
                isFavorite = rest.id in state.favoriteRestaurantIds,
                onClick = { onOpenRestaurant(rest) },
                onFavoriteToggle = { onToggleFavoriteRestaurant(rest.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    state: EfoodUiState,
    onBack: () -> Unit,
    onOpenFoodDetail: (MenuItem) -> Unit,
    onToggleFavorite: () -> Unit,
    onObserveReviews: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(restaurant.id, state.currentUser) {
        onObserveReviews(restaurant.id)
    }

    val menuForRestaurant = state.menuItems.filter { it.restaurantId == restaurant.id }
    val isFav = restaurant.id in state.favoriteRestaurantIds
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Hero Banner Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                EfoodMediaImage(
                    imageKey = restaurant.imageKey,
                    imageUrl = restaurant.coverUrl,
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f))
                            .testTag("restaurant_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f))
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Restaurant Info Card
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", restaurant.rating)} (${restaurant.reviewCount})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(restaurant.cuisine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(restaurant.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoMetric("Delivery", "${restaurant.deliveryTimeMinutes} min")
                        InfoMetric("Fee", formatBdt(restaurant.deliveryFeeBdt))
                        InfoMetric("Min Order", formatBdt(restaurant.minOrderBdt))
                        InfoMetric("Hours", if (restaurant.isOpen) "Open" else "Closed")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${restaurant.area}, ${restaurant.district} • ${restaurant.openHours}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Menu (${menuForRestaurant.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reviews (${state.selectedRestaurantReviews.size})") })
                }
            }
        }

        if (selectedTab == 0) {
            items(menuForRestaurant, key = { it.id }) { item ->
                MenuRowItem(
                    item = item,
                    language = state.language,
                    onClick = { onOpenFoodDetail(item) }
                )
            }
        } else {
            if (state.selectedRestaurantReviews.isEmpty()) {
                item {
                    Text(
                        text = if (state.currentUser == null) {
                            "Sign in with Google on Account or Checkout to view live verified reviews."
                        } else {
                            "No written reviews yet for ${restaurant.name}. Place an order to leave the first review!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                items(state.selectedRestaurantReviews, key = { it.id }) { rev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.authorName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                                Text("★ ${rev.rating}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(rev.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun MenuRowItem(
    item: MenuItem,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("menu_row_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN && item.nameBn.isNotBlank()) item.nameBn else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatBdt(item.effectivePriceBdt),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (item.hasDiscount) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatBdt(item.priceBdt),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!item.isAvailable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sold Out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(94.dp)) {
                EfoodMediaImage(
                    imageKey = item.imageKey,
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Customize and add",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
