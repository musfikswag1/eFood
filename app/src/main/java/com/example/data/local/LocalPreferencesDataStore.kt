package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.efoodDataStore by preferencesDataStore(name = "efood_preferences")

class LocalPreferencesDataStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("language_code")
    private val keyDistrict = stringPreferencesKey("selected_district")
    private val keyArea = stringPreferencesKey("selected_area")
    private val keyRecentItems = stringSetPreferencesKey("recently_viewed_ids")
    private val keyLocalFavRestaurants = stringSetPreferencesKey("local_fav_restaurants")
    private val keyLocalFavFoods = stringSetPreferencesKey("local_fav_foods")

    val languageFlow: Flow<AppLanguage> = context.efoodDataStore.data.map { prefs ->
        if (prefs[keyLanguage] == "bn") AppLanguage.BN else AppLanguage.EN
    }

    val selectedLocationFlow: Flow<Pair<String, String>> = context.efoodDataStore.data.map { prefs ->
        val district = prefs[keyDistrict] ?: "Dhaka"
        val area = prefs[keyArea] ?: "Dhanmondi"
        district to area
    }

    val recentlyViewedIdsFlow: Flow<List<String>> = context.efoodDataStore.data.map { prefs ->
        prefs[keyRecentItems]?.toList() ?: emptyList()
    }

    val localFavRestaurantsFlow: Flow<Set<String>> = context.efoodDataStore.data.map { prefs ->
        prefs[keyLocalFavRestaurants] ?: emptySet()
    }

    val localFavFoodsFlow: Flow<Set<String>> = context.efoodDataStore.data.map { prefs ->
        prefs[keyLocalFavFoods] ?: emptySet()
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.efoodDataStore.edit { prefs ->
            prefs[keyLanguage] = language.code
        }
    }

    suspend fun setDeliveryLocation(district: String, area: String) {
        context.efoodDataStore.edit { prefs ->
            prefs[keyDistrict] = district
            prefs[keyArea] = area
        }
    }

    suspend fun addRecentlyViewed(itemId: String) {
        context.efoodDataStore.edit { prefs ->
            val current = (prefs[keyRecentItems] ?: emptySet()).toMutableList()
            current.remove(itemId)
            current.add(0, itemId)
            prefs[keyRecentItems] = current.take(10).toSet()
        }
    }

    suspend fun toggleLocalFavoriteRestaurant(restaurantId: String) {
        context.efoodDataStore.edit { prefs ->
            val set = (prefs[keyLocalFavRestaurants] ?: emptySet()).toMutableSet()
            if (!set.add(restaurantId)) set.remove(restaurantId)
            prefs[keyLocalFavRestaurants] = set
        }
    }

    suspend fun toggleLocalFavoriteFood(foodId: String) {
        context.efoodDataStore.edit { prefs ->
            val set = (prefs[keyLocalFavFoods] ?: emptySet()).toMutableSet()
            if (!set.add(foodId)) set.remove(foodId)
            prefs[keyLocalFavFoods] = set
        }
    }
}
