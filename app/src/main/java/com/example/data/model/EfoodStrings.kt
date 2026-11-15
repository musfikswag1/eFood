package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

fun formatBdt(amount: Double): String {
    val rounded = kotlin.math.round(amount).toLong()
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(rounded)
    return "৳$formatted"
}

fun isValidBangladeshPhone(input: String): Boolean {
    val cleaned = input.trim().replace(" ", "").replace("-", "")
    return Regex("^(\\+880|880|0)1[3-9]\\d{8}$").matches(cleaned)
}

fun normalizeBangladeshPhone(input: String): String {
    val cleaned = input.trim().replace(" ", "").replace("-", "")
    return when {
        cleaned.startsWith("+880") -> cleaned
        cleaned.startsWith("880") -> "+$cleaned"
        cleaned.startsWith("01") -> "+88$cleaned"
        else -> cleaned
    }
}

data class EfoodLocalizedStrings(
    val appTitle: String,
    val deliverTo: String,
    val searchPlaceholder: String,
    val promoTitle: String,
    val categoriesTitle: String,
    val specialOffers: String,
    val featuredRestaurants: String,
    val popularRestaurants: String,
    val bestSellers: String,
    val recommendedForYou: String,
    val nearbyRestaurants: String,
    val recentlyViewed: String,
    val navHome: String,
    val navSearch: String,
    val navOrders: String,
    val navFavorites: String,
    val navAccount: String,
    val addToCart: String,
    val buyNow: String,
    val viewCart: String,
    val checkout: String,
    val placeOrder: String,
    val subtotal: String,
    val deliveryFee: String,
    val discount: String,
    val vatTax: String,
    val totalPayable: String,
    val riderPortal: String,
    val signInGoogle: String,
    val verifyPhone: String
)

fun localizedStrings(lang: AppLanguage): EfoodLocalizedStrings {
    return if (lang == AppLanguage.BN) {
        EfoodLocalizedStrings(
            appTitle = "eFood",
            deliverTo = "ডেলিভারি ঠিকানা",
            searchPlaceholder = "রেস্টুরেন্ট বা খাবার খুঁজুন...",
            promoTitle = "স্পেশাল অফার",
            categoriesTitle = "খাবারের ক্যাটাগরি",
            specialOffers = "ডিসকাউন্ট ডিল",
            featuredRestaurants = "ফিচার্ড রেস্টুরেন্ট",
            popularRestaurants = "জনপ্রিয় রেস্টুরেন্ট",
            bestSellers = "সর্বাধিক বিক্রিত",
            recommendedForYou = "আপনার জন্য প্রস্তাবিত",
            nearbyRestaurants = "কাছাকাছি রেস্টুরেন্ট",
            recentlyViewed = "সম্প্রতি দেখা",
            navHome = "হোম",
            navSearch = "খুঁজুন",
            navOrders = "অর্ডার",
            navFavorites = "পছন্দ",
            navAccount = "অ্যাকাউন্ট",
            addToCart = "কার্টে যোগ করুন",
            buyNow = "এখনই কিনুন",
            viewCart = "কার্ট দেখুন",
            checkout = "চেকআউট",
            placeOrder = "অর্ডার নিশ্চিত করুন",
            subtotal = "সাবটোটাল",
            deliveryFee = "ডেলিভারি চার্জ",
            discount = "ডিসকাউন্ট",
            vatTax = "ভ্যাট (৫%)",
            totalPayable = "সর্বমোট বিল",
            riderPortal = "ডেলিভারি পার্টনার পোর্টাল",
            signInGoogle = "Google দিয়ে সাইন ইন করুন",
            verifyPhone = "ফোন নম্বর ভেরিফাই করুন (+880)"
        )
    } else {
        EfoodLocalizedStrings(
            appTitle = "eFood",
            deliverTo = "Deliver to",
            searchPlaceholder = "Search biryani, burgers, restaurants...",
            promoTitle = "Hot Deals",
            categoriesTitle = "Food Categories",
            specialOffers = "Special Offers",
            featuredRestaurants = "Featured Restaurants",
            popularRestaurants = "Popular Kitchens",
            bestSellers = "Best Sellers",
            recommendedForYou = "Recommended For You",
            nearbyRestaurants = "Nearby Restaurants",
            recentlyViewed = "Recently Viewed",
            navHome = "Home",
            navSearch = "Search",
            navOrders = "Orders",
            navFavorites = "Favorites",
            navAccount = "Account",
            addToCart = "Add to Cart",
            buyNow = "Buy Now",
            viewCart = "View Cart",
            checkout = "Proceed to Checkout",
            placeOrder = "Confirm Order",
            subtotal = "Subtotal",
            deliveryFee = "Delivery Charge",
            discount = "Discount",
            vatTax = "VAT (5%)",
            totalPayable = "Total Payable",
            riderPortal = "Delivery Partner Portal",
            signInGoogle = "Sign in with Google",
            verifyPhone = "Verify +880 Phone"
        )
    }
}
