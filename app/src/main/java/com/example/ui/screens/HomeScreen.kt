package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MenuItem
import com.example.data.model.PromoBanner
import com.example.data.model.Restaurant
import com.example.data.model.localizedStrings
import com.example.ui.components.CategoryChipItem
import com.example.ui.components.EfoodMediaImage
import com.example.ui.components.EfoodTopBar
import com.example.ui.components.FoodDishCard
import com.example.ui.components.RestaurantCard
import com.example.ui.viewmodel.EfoodUiState

@Composable
fun HomeScreen(
    state: EfoodUiState,
    onOpenLocationPicker: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSearch: (String?) -> Unit,
    onOpenRestaurant: (Restaurant) -> Unit,
    onOpenFoodDetail: (MenuItem) -> Unit,
    onToggleFavoriteRestaurant: (String) -> Unit,
    onToggleFavoriteFood: (String) -> Unit,
    onApplyBannerPromo: (String) -> Unit
) {
    val strings = localizedStrings(state.language)
    val restaurantById = state.restaurants.associateBy { it.id }

    val specialOfferItems = state.menuItems.filter { it.hasDiscount && it.isAvailable }
    val bestSellers = state.menuItems.filter { it.isBestSeller && it.isAvailable }
    val recommended = state.menuItems.filter { it.isRecommended && it.isAvailable }
    val recentlyViewed = state.recentlyViewedIds.mapNotNull { id ->
        state.menuItems.firstOrNull { it.id == id }
    }
    val featuredRestaurants = state.restaurants.filter { it.isFeatured }
    val popularRestaurants = state.restaurants.filter { it.isPopular }
    val nearbyRestaurants = state.restaurants.sortedBy {
        if (it.area.equals(state.selectedArea, ignoreCase = true)) 0 else 1
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            EfoodTopBar(
                district = state.selectedDistrict,
                area = state.selectedArea,
                language = state.language,
                unreadNotifications = state.unreadNotificationsCount,
                onLocationClick = onOpenLocationPicker,
                onLanguageToggle = onToggleLanguage,
                onNotificationsClick = onOpenNotifications
            )
        }

        // Search Shortcut Bar
        item {
            Surface(
                onClick = { onOpenSearch(null) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("home_search_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.searchPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Promotional Banners Carousel
        item {
            SectionHeader(title = strings.promoTitle, modifier = Modifier.padding(top = 12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.banners, key = { it.id }) { banner ->
                    PromoBannerCard(
                        banner = banner,
                        onClick = {
                            if (banner.promoCode.isNotBlank()) {
                                onApplyBannerPromo(banner.promoCode)
                            } else {
                                onOpenSearch(null)
                            }
                        }
                    )
                }
            }
        }

        // Manageable Food Categories (12 Categories)
        item {
            SectionHeader(title = strings.categoriesTitle, modifier = Modifier.padding(top = 18.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.categories, key = { it.id }) { cat ->
                    CategoryChipItem(
                        category = cat,
                        language = state.language,
                        selected = false,
                        onClick = { onOpenSearch(cat.id) }
                    )
                }
            }
        }

        // Special Offers & Discounts
        if (specialOfferItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = strings.specialOffers,
                    badge = "UP TO 20% OFF",
                    modifier = Modifier.padding(top = 20.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(specialOfferItems, key = { it.id }) { item ->
                        FoodDishCard(
                            item = item,
                            restaurantName = restaurantById[item.restaurantId]?.name ?: "eFood Kitchen",
                            language = state.language,
                            isFavorite = item.id in state.favoriteFoodIds,
                            onClick = { onOpenFoodDetail(item) },
                            onFavoriteToggle = { onToggleFavoriteFood(item.id) },
                            modifier = Modifier.width(195.dp)
                        )
                    }
                }
            }
        }

        // Featured Restaurants
        item {
            SectionHeader(title = strings.featuredRestaurants, modifier = Modifier.padding(top = 22.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(featuredRestaurants, key = { it.id }) { rest ->
                    RestaurantCard(
                        restaurant = rest,
                        isFavorite = rest.id in state.favoriteRestaurantIds,
                        onClick = { onOpenRestaurant(rest) },
                        onFavoriteToggle = { onToggleFavoriteRestaurant(rest.id) },
                        modifier = Modifier.width(286.dp)
                    )
                }
            }
        }

        // Best Sellers
        if (bestSellers.isNotEmpty()) {
            item {
                SectionHeader(title = strings.bestSellers, modifier = Modifier.padding(top = 22.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bestSellers, key = { it.id }) { item ->
                        FoodDishCard(
                            item = item,
                            restaurantName = restaurantById[item.restaurantId]?.name ?: "eFood Kitchen",
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

        // Recently Viewed Food
        if (recentlyViewed.isNotEmpty()) {
            item {
                SectionHeader(title = strings.recentlyViewed, modifier = Modifier.padding(top = 22.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyViewed, key = { "recent_${it.id}" }) { item ->
                        FoodDishCard(
                            item = item,
                            restaurantName = restaurantById[item.restaurantId]?.name ?: "eFood Kitchen",
                            language = state.language,
                            isFavorite = item.id in state.favoriteFoodIds,
                            onClick = { onOpenFoodDetail(item) },
                            onFavoriteToggle = { onToggleFavoriteFood(item.id) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        // Popular Restaurants
        item {
            SectionHeader(title = strings.popularRestaurants, modifier = Modifier.padding(top = 22.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(popularRestaurants, key = { "pop_${it.id}" }) { rest ->
                    RestaurantCard(
                        restaurant = rest,
                        isFavorite = rest.id in state.favoriteRestaurantIds,
                        onClick = { onOpenRestaurant(rest) },
                        onFavoriteToggle = { onToggleFavoriteRestaurant(rest.id) },
                        modifier = Modifier.width(268.dp)
                    )
                }
            }
        }

        // Recommended Food
        if (recommended.isNotEmpty()) {
            item {
                SectionHeader(title = strings.recommendedForYou, modifier = Modifier.padding(top = 22.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommended, key = { "rec_${it.id}" }) { item ->
                        FoodDishCard(
                            item = item,
                            restaurantName = restaurantById[item.restaurantId]?.name ?: "eFood Kitchen",
                            language = state.language,
                            isFavorite = item.id in state.favoriteFoodIds,
                            onClick = { onOpenFoodDetail(item) },
                            onFavoriteToggle = { onToggleFavoriteFood(item.id) },
                            modifier = Modifier.width(188.dp)
                        )
                    }
                }
            }
        }

        // Nearby Restaurants Vertical Feed
        item {
            SectionHeader(
                title = "${strings.nearbyRestaurants} (${state.selectedArea})",
                modifier = Modifier.padding(top = 22.dp)
            )
        }
        items(nearbyRestaurants, key = { "nearby_${it.id}" }) { rest ->
            RestaurantCard(
                restaurant = rest,
                isFavorite = rest.id in state.favoriteRestaurantIds,
                onClick = { onOpenRestaurant(rest) },
                onFavoriteToggle = { onToggleFavoriteRestaurant(rest.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    banner: PromoBanner,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(305.dp)
            .height(152.dp)
            .testTag("promo_banner_${banner.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            EfoodMediaImage(
                imageKey = banner.imageKey,
                contentDescription = banner.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = banner.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = banner.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
