package com.example.data.seed

import com.example.data.model.Coupon
import com.example.data.model.FoodCategory
import com.example.data.model.MenuAddonOption
import com.example.data.model.MenuItem
import com.example.data.model.MenuSizeOption
import com.example.data.model.PromoBanner
import com.example.data.model.Restaurant

object EfoodCatalogSeed {

    val bangladeshDistricts: Map<String, List<String>> = mapOf(
        "Dhaka" to listOf(
            "Dhanmondi", "Gulshan 1", "Gulshan 2", "Banani", "Uttara Sector 7",
            "Mirpur 10", "Bashundhara R/A", "Motijheel", "Baily Road", "Mohammadpur"
        ),
        "Chattogram" to listOf("GEC Circle", "Agrabad", "Nasirabad", "Panchlaish", "Halishahar"),
        "Sylhet" to listOf("Zindabazar", "Amberkhana", "Shahjalal Upashahar"),
        "Rajshahi" to listOf("Shaheb Bazar", "Uposhohor"),
        "Khulna" to listOf("Shib Bari", "Sonadanga")
    )

    val categories: List<FoodCategory> = listOf(
        FoodCategory(id = "cat_burger", name = "Burger", nameBn = "বার্গার", iconKey = "burger", sortOrder = 1L),
        FoodCategory(id = "cat_pizza", name = "Pizza", nameBn = "পিৎজা", iconKey = "pizza", sortOrder = 2L),
        FoodCategory(id = "cat_fried_chicken", name = "Fried Chicken", nameBn = "ফ্রাইড চিকেন", iconKey = "chicken", sortOrder = 3L),
        FoodCategory(id = "cat_chinese", name = "Chinese", nameBn = "চাইনিজ", iconKey = "chinese", sortOrder = 4L),
        FoodCategory(id = "cat_thai", name = "Thai", nameBn = "থাই", iconKey = "thai", sortOrder = 5L),
        FoodCategory(id = "cat_indian", name = "Indian", nameBn = "ইন্ডিয়ান", iconKey = "indian", sortOrder = 6L),
        FoodCategory(id = "cat_bangladeshi", name = "Bangladeshi", nameBn = "বাংলাদেশি", iconKey = "bangladeshi", sortOrder = 7L),
        FoodCategory(id = "cat_biryani", name = "Biryani", nameBn = "বিরিয়ানি", iconKey = "biryani", sortOrder = 8L),
        FoodCategory(id = "cat_fast_food", name = "Fast Food", nameBn = "ফাস্ট ফুড", iconKey = "fastfood", sortOrder = 9L),
        FoodCategory(id = "cat_desserts", name = "Desserts", nameBn = "ডেজার্ট", iconKey = "dessert", sortOrder = 10L),
        FoodCategory(id = "cat_drinks", name = "Drinks", nameBn = "ড্রিংকস", iconKey = "drink", sortOrder = 11L),
        FoodCategory(id = "cat_snacks", name = "Snacks", nameBn = "স্ন্যাকস", iconKey = "snack", sortOrder = 12L)
    )

    val banners: List<PromoBanner> = listOf(
        PromoBanner(
            id = "banner_kacchi",
            title = "Royal Dhaka Kacchi Feast",
            subtitle = "Use code BIRYANI15 for 15% off handi biryani orders above ৳400",
            badgeText = "15% OFF",
            promoCode = "BIRYANI15",
            imageKey = "biryani",
            sortOrder = 1L
        ),
        PromoBanner(
            id = "banner_efood50",
            title = "Midnight Smash & Fries",
            subtitle = "Flat ৳50 off on burgers & pizzas with code EFOOD50",
            badgeText = "৳50 OFF",
            promoCode = "EFOOD50",
            imageKey = "burger",
            sortOrder = 2L
        ),
        PromoBanner(
            id = "banner_dhaka20",
            title = "Wood-Fired Weekend",
            subtitle = "20% savings up to ৳150 across Dhanmondi & Banani kitchens",
            badgeText = "20% SAVE",
            promoCode = "DHAKA20",
            imageKey = "pizza",
            sortOrder = 3L
        )
    )

    val coupons: List<Coupon> = listOf(
        Coupon(
            id = "coupon_efood50",
            code = "EFOOD50",
            title = "Flat ৳50 off orders above ৳300",
            discountType = "FLAT",
            discountValue = 50.0,
            minOrderBdt = 300.0,
            maxDiscountBdt = 50.0,
            isActive = true
        ),
        Coupon(
            id = "coupon_dhaka20",
            code = "DHAKA20",
            title = "20% off (max ৳150) on orders ৳500+",
            discountType = "PERCENT",
            discountValue = 20.0,
            minOrderBdt = 500.0,
            maxDiscountBdt = 150.0,
            isActive = true
        ),
        Coupon(
            id = "coupon_biryani15",
            code = "BIRYANI15",
            title = "15% off Biryani & Bangla platters ৳400+",
            discountType = "PERCENT",
            discountValue = 15.0,
            minOrderBdt = 400.0,
            maxDiscountBdt = 120.0,
            isActive = true
        )
    )

    val restaurants: List<Restaurant> = listOf(
        Restaurant(
            id = "rest_sultans_handi",
            name = "Sultan's Handi House",
            description = "Traditional Old Dhaka style basmati Kacchi Biryani slow-cooked in sealed copper pots with saffron and ghee.",
            cuisine = "Biryani • Bangladeshi • Mughlai",
            categoryIds = listOf("cat_biryani", "cat_bangladeshi", "cat_indian"),
            area = "Dhanmondi",
            district = "Dhaka",
            deliveryTimeMinutes = 28L,
            minOrderBdt = 250.0,
            deliveryFeeBdt = 40.0,
            rating = 4.9,
            reviewCount = 1420L,
            openHours = "11:00 AM - 11:30 PM",
            isOpen = true,
            isFeatured = true,
            isPopular = true,
            imageKey = "biryani",
            latitude = 23.7461,
            longitude = 90.3742
        ),
        Restaurant(
            id = "rest_emerald_smash",
            name = "Emerald Smash Kitchen",
            description = "Double-seared dry-aged beef smash burgers, crispy buttermilk fried chicken, and truffle-salted fries.",
            cuisine = "Burger • Fried Chicken • Fast Food",
            categoryIds = listOf("cat_burger", "cat_fried_chicken", "cat_fast_food", "cat_snacks"),
            area = "Banani",
            district = "Dhaka",
            deliveryTimeMinutes = 24L,
            minOrderBdt = 200.0,
            deliveryFeeBdt = 35.0,
            rating = 4.8,
            reviewCount = 980L,
            openHours = "12:00 PM - 02:00 AM",
            isOpen = true,
            isFeatured = true,
            isPopular = true,
            imageKey = "burger",
            latitude = 23.7937,
            longitude = 90.4066
        ),
        Restaurant(
            id = "rest_firestone_pizzeria",
            name = "Firestone Wood-Fired Pizzeria",
            description = "48-hour fermented Neapolitan sourdough pizzas baked at 450°C with fresh mozzarella and spicy beef pepperoni.",
            cuisine = "Pizza • Fast Food • Desserts",
            categoryIds = listOf("cat_pizza", "cat_fast_food", "cat_desserts"),
            area = "Gulshan 2",
            district = "Dhaka",
            deliveryTimeMinutes = 32L,
            minOrderBdt = 350.0,
            deliveryFeeBdt = 45.0,
            rating = 4.7,
            reviewCount = 650L,
            openHours = "11:30 AM - 11:45 PM",
            isOpen = true,
            isFeatured = true,
            isPopular = false,
            imageKey = "pizza",
            latitude = 23.7925,
            longitude = 90.4078
        ),
        Restaurant(
            id = "rest_wok_bangkok",
            name = "Wok & Bangkok Express",
            description = "Aromatic Thai Tom Yum, Pad Kra Pao, and sizzling Schezwan Chinese bowls crafted fresh to order.",
            cuisine = "Thai • Chinese • Snacks",
            categoryIds = listOf("cat_thai", "cat_chinese", "cat_snacks"),
            area = "Uttara Sector 7",
            district = "Dhaka",
            deliveryTimeMinutes = 30L,
            minOrderBdt = 220.0,
            deliveryFeeBdt = 40.0,
            rating = 4.6,
            reviewCount = 540L,
            openHours = "12:00 PM - 11:00 PM",
            isOpen = true,
            isFeatured = false,
            isPopular = true,
            imageKey = "burger",
            latitude = 23.8682,
            longitude = 90.3982
        ),
        Restaurant(
            id = "rest_padma_ghar",
            name = "Padma Ghar Heritage",
            description = "Authentic Bengali bhuna khichuri, smoked ilish curry, morog polao, and traditional Bangladeshi sweets.",
            cuisine = "Bangladeshi • Biryani • Indian",
            categoryIds = listOf("cat_bangladeshi", "cat_biryani", "cat_indian"),
            area = "Dhanmondi",
            district = "Dhaka",
            deliveryTimeMinutes = 35L,
            minOrderBdt = 200.0,
            deliveryFeeBdt = 30.0,
            rating = 4.8,
            reviewCount = 875L,
            openHours = "10:00 AM - 10:30 PM",
            isOpen = true,
            isFeatured = false,
            isPopular = true,
            imageKey = "biryani",
            latitude = 23.7412,
            longitude = 90.3711
        ),
        Restaurant(
            id = "rest_cocoa_mango",
            name = "Cocoa & Mango Lounge",
            description = "Warm molten chocolate lava cakes, Rajshahi mango lassi, cold brew tonics, and late-night desserts.",
            cuisine = "Desserts • Drinks • Snacks",
            categoryIds = listOf("cat_desserts", "cat_drinks", "cat_snacks"),
            area = "Bashundhara R/A",
            district = "Dhaka",
            deliveryTimeMinutes = 22L,
            minOrderBdt = 150.0,
            deliveryFeeBdt = 30.0,
            rating = 4.9,
            reviewCount = 720L,
            openHours = "11:00 AM - 01:00 AM",
            isOpen = true,
            isFeatured = true,
            isPopular = true,
            imageKey = "dessert",
            latitude = 23.8191,
            longitude = 90.4526
        )
    )

    val menuItems: List<MenuItem> = listOf(
        MenuItem(
            id = "item_kacchi_basmati",
            restaurantId = "rest_sultans_handi",
            categoryId = "cat_biryani",
            name = "Royal Mutton Kacchi Biryani",
            nameBn = "রয়্যাল মাটন কাচ্চি বিরিয়ানি",
            description = "Slow-cooked tender goat meat layered with aromatic saffron basmati rice, fried potatoes, boiled egg, and borhani.",
            priceBdt = 490.0,
            discountPriceBdt = 430.0,
            ingredients = listOf("Basmati Rice", "Mutton", "Saffron", "Ghee", "Aloo Bukhara", "Mint"),
            sizes = listOf(
                MenuSizeOption("Half (1 Person)", 0.0),
                MenuSizeOption("Full Handi (2 Persons)", 360.0),
                MenuSizeOption("Family Pot (4 Persons)", 1100.0)
            ),
            addons = listOf(
                MenuAddonOption("add_borhani", "Chilled Borhani (250ml)", 60.0),
                MenuAddonOption("add_jali_kabab", "Beef Jali Kabab", 85.0),
                MenuAddonOption("add_firni", "Shahi Firni Cup", 70.0)
            ),
            customizationOptions = listOf("Mild Spice", "Dhaka Authentic", "Extra Ghee & Beresta"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "biryani",
            galleryKeys = listOf("biryani", "dessert"),
            rating = 4.9
        ),
        MenuItem(
            id = "item_morog_polao",
            restaurantId = "rest_sultans_handi",
            categoryId = "cat_bangladeshi",
            name = "Shahi Morog Polao",
            nameBn = "শাহী মোরগ পোলাও",
            description = "Fragrant chinigura rice simmered with golden roasted chicken quarter, kewra essence, and roasted cashews.",
            priceBdt = 360.0,
            discountPriceBdt = 320.0,
            ingredients = listOf("Chinigura Rice", "Roast Chicken", "Yogurt", "Cashews", "Kewra Water"),
            sizes = listOf(
                MenuSizeOption("Single Plate", 0.0),
                MenuSizeOption("Double Combo", 280.0)
            ),
            addons = listOf(
                MenuAddonOption("add_borhani", "Chilled Borhani (250ml)", 60.0),
                MenuAddonOption("add_egg", "Extra Boiled Egg", 25.0)
            ),
            customizationOptions = listOf("Standard", "Extra Crispy Onion"),
            isPopular = true,
            isBestSeller = false,
            isRecommended = true,
            imageKey = "biryani",
            galleryKeys = listOf("biryani"),
            rating = 4.8
        ),
        MenuItem(
            id = "item_double_smash",
            restaurantId = "rest_emerald_smash",
            categoryId = "cat_burger",
            name = "Double Emerald Smash Burger",
            nameBn = "ডাবল এমারেল্ড স্ম্যাশ বার্গার",
            description = "Two crispy-edged smashed beef patties, double aged cheddar, caramelized onions, and house smoky sauce on brioche.",
            priceBdt = 420.0,
            discountPriceBdt = 365.0,
            ingredients = listOf("Beef Patty", "Aged Cheddar", "Brioche Bun", "Pickles", "House Sauce"),
            sizes = listOf(
                MenuSizeOption("Double Patty", 0.0),
                MenuSizeOption("Triple Stack", 140.0),
                MenuSizeOption("Meal w/ Fries & Drink", 160.0)
            ),
            addons = listOf(
                MenuAddonOption("add_cheese", "Extra Cheddar Slice", 50.0),
                MenuAddonOption("add_bacon", "Smoked Beef Bacon", 90.0),
                MenuAddonOption("add_fries", "Cajun Skin-On Fries", 110.0)
            ),
            customizationOptions = listOf("No Pickles", "Extra Smoky Sauce", "Spicy Jalapeno"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "burger",
            galleryKeys = listOf("burger", "pizza"),
            rating = 4.9
        ),
        MenuItem(
            id = "item_crispy_chicken",
            restaurantId = "rest_emerald_smash",
            categoryId = "cat_fried_chicken",
            name = "Nashville Golden Fried Chicken",
            nameBn = "ন্যাশভিল গোল্ডেন ফ্রাইড চিকেন",
            description = "Buttermilk-brined crunchy fried chicken tenders tossed with honey-chili glaze and garlic dip.",
            priceBdt = 340.0,
            discountPriceBdt = 295.0,
            ingredients = listOf("Chicken Breast", "Buttermilk", "Chili Oil", "Garlic Mayo"),
            sizes = listOf(
                MenuSizeOption("4 Pcs Box", 0.0),
                MenuSizeOption("8 Pcs Bucket", 260.0)
            ),
            addons = listOf(
                MenuAddonOption("add_coleslaw", "Creamy Coleslaw", 55.0),
                MenuAddonOption("add_dip", "Extra Garlic Aioli", 35.0)
            ),
            customizationOptions = listOf("Original Crunch", "Medium Hot", "Blazing Honey Chili"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = false,
            imageKey = "burger",
            galleryKeys = listOf("burger"),
            rating = 4.7
        ),
        MenuItem(
            id = "item_pepperoni_pizza",
            restaurantId = "rest_firestone_pizzeria",
            categoryId = "cat_pizza",
            name = "Wood-Fired Beef Pepperoni",
            nameBn = "উড-ফায়ার্ড বিফ পেপারোনি পিৎজা",
            description = "Blistered sourdough crust topped with San Marzano tomato sauce, fior di latte mozzarella, spicy beef pepperoni, and basil.",
            priceBdt = 780.0,
            discountPriceBdt = 660.0,
            ingredients = listOf("Sourdough", "Mozzarella", "Beef Pepperoni", "Basil", "Olive Oil"),
            sizes = listOf(
                MenuSizeOption("9\" Personal", 0.0),
                MenuSizeOption("12\" Medium", 260.0),
                MenuSizeOption("16\" Party", 520.0)
            ),
            addons = listOf(
                MenuAddonOption("add_mozz", "Extra Burrata / Mozzarella", 140.0),
                MenuAddonOption("add_chili", "Chili Honey Drizzle", 50.0)
            ),
            customizationOptions = listOf("Thin Crispy", "Classic Neapolitan", "Extra Oregano"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "pizza",
            galleryKeys = listOf("pizza", "burger"),
            rating = 4.8
        ),
        MenuItem(
            id = "item_margherita_truffle",
            restaurantId = "rest_firestone_pizzeria",
            categoryId = "cat_fast_food",
            name = "Truffle Mushroom & Basil Pie",
            nameBn = "ট্রাফল মাশরুম পিৎজা",
            description = "Roasted shiitake & button mushrooms, white garlic cream, mozzarella, and aromatic basil oil.",
            priceBdt = 690.0,
            discountPriceBdt = 0.0,
            ingredients = listOf("Shiitake", "Garlic Cream", "Mozzarella", "Basil"),
            sizes = listOf(
                MenuSizeOption("9\" Personal", 0.0),
                MenuSizeOption("12\" Medium", 240.0)
            ),
            addons = listOf(MenuAddonOption("add_olives", "Kalamata Olives", 65.0)),
            customizationOptions = listOf("Regular", "Extra Garlic"),
            isPopular = false,
            isBestSeller = false,
            isRecommended = true,
            imageKey = "pizza",
            galleryKeys = listOf("pizza"),
            rating = 4.6
        ),
        MenuItem(
            id = "item_thai_basil_bowl",
            restaurantId = "rest_wok_bangkok",
            categoryId = "cat_thai",
            name = "Bangkok Pad Kra Pao Bowl",
            nameBn = "ব্যাংকক পাড ক্রা পাও বোল",
            description = "Wok-tossed minced chicken with holy basil, bird's eye chili, jasmine rice, and crispy fried egg.",
            priceBdt = 350.0,
            discountPriceBdt = 310.0,
            ingredients = listOf("Jasmine Rice", "Chicken", "Thai Holy Basil", "Fried Egg", "Chili"),
            sizes = listOf(
                MenuSizeOption("Regular Bowl", 0.0),
                MenuSizeOption("Jumbo Bowl", 110.0)
            ),
            addons = listOf(MenuAddonOption("add_wonton", "Crispy Chicken Wontons (4pc)", 120.0)),
            customizationOptions = listOf("Mild", "Thai Spicy", "Extra Basil"),
            isPopular = true,
            isBestSeller = false,
            isRecommended = true,
            imageKey = "biryani",
            galleryKeys = listOf("biryani"),
            rating = 4.7
        ),
        MenuItem(
            id = "item_schezwan_hakka",
            restaurantId = "rest_wok_bangkok",
            categoryId = "cat_chinese",
            name = "Schezwan Chicken & Egg Fried Rice",
            nameBn = "সেজুয়ান চিকেন ফ্রাইড রাইস",
            description = "Smoky wok-seared egg fried rice paired with spicy Schezwan glazed chicken and crisp bell peppers.",
            priceBdt = 380.0,
            discountPriceBdt = 340.0,
            ingredients = listOf("Egg Fried Rice", "Chicken Strips", "Bell Peppers", "Schezwan Sauce"),
            sizes = listOf(MenuSizeOption("1:1 Platter", 0.0), MenuSizeOption("1:2 Sharing", 240.0)),
            addons = listOf(MenuAddonOption("add_soup", "Thai Thick Soup Cup", 130.0)),
            customizationOptions = listOf("Medium Spice", "Extra Chili"),
            isPopular = false,
            isBestSeller = true,
            isRecommended = false,
            imageKey = "burger",
            galleryKeys = listOf("burger"),
            rating = 4.6
        ),
        MenuItem(
            id = "item_bhuna_khichuri",
            restaurantId = "rest_padma_ghar",
            categoryId = "cat_bangladeshi",
            name = "Beef Bhuna Khichuri Platter",
            nameBn = "গরুর মাংস ভুনা খিচুড়ি",
            description = "Comforting moong dal and chinigura rice khichuri served with slow-braised spicy Dhaka beef bhuna and begun bhaja.",
            priceBdt = 390.0,
            discountPriceBdt = 350.0,
            ingredients = listOf("Chinigura Rice", "Moong Dal", "Beef Bhuna", "Begun Bhaja", "Achar"),
            sizes = listOf(MenuSizeOption("Standard", 0.0), MenuSizeOption("Executive Box", 150.0)),
            addons = listOf(MenuAddonOption("add_egg_omelette", "Masala Omelette", 40.0)),
            customizationOptions = listOf("Less Oil", "Extra Beef Gravy"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "biryani",
            galleryKeys = listOf("biryani"),
            rating = 4.9
        ),
        MenuItem(
            id = "item_butter_naan_tikka",
            restaurantId = "rest_padma_ghar",
            categoryId = "cat_indian",
            name = "Tandoori Chicken Tikka & Butter Naan",
            nameBn = "তান্দুরি চিকেন টিক্কা ও বাটার নান",
            description = "Charcoal-grilled yogurt-spiced chicken tikka masala paired with two warm garlic butter naans.",
            priceBdt = 410.0,
            discountPriceBdt = 0.0,
            ingredients = listOf("Chicken Tikka", "Tomato Makhani", "Butter Naan", "Coriander"),
            sizes = listOf(MenuSizeOption("2 Naan Meal", 0.0), MenuSizeOption("4 Naan Family", 270.0)),
            addons = listOf(MenuAddonOption("add_naan", "Extra Garlic Naan", 55.0)),
            customizationOptions = listOf("Rich Creamy", "Spicy Tandoori"),
            isPopular = false,
            isBestSeller = false,
            isRecommended = true,
            imageKey = "biryani",
            galleryKeys = listOf("biryani"),
            rating = 4.7
        ),
        MenuItem(
            id = "item_lava_cake",
            restaurantId = "rest_cocoa_mango",
            categoryId = "cat_desserts",
            name = "Dark Belgian Chocolate Lava Cake",
            nameBn = "ডার্ক চকলেট লাভা কেক",
            description = "Warm baked dark cocoa cake with a molten bittersweet center, served with vanilla bean gelato.",
            priceBdt = 280.0,
            discountPriceBdt = 235.0,
            ingredients = listOf("70% Belgian Cocoa", "Butter", "Vanilla Gelato", "Mint"),
            sizes = listOf(MenuSizeOption("Single", 0.0), MenuSizeOption("Duo Box", 200.0)),
            addons = listOf(MenuAddonOption("add_gelato", "Extra Vanilla Scoop", 80.0)),
            customizationOptions = listOf("With Gelato", "Cake Only"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "dessert",
            galleryKeys = listOf("dessert"),
            rating = 4.9
        ),
        MenuItem(
            id = "item_mango_lassi",
            restaurantId = "rest_cocoa_mango",
            categoryId = "cat_drinks",
            name = "Rajshahi Himsagar Mango Lassi",
            nameBn = "রাজশাহীর হিমসাগর ম্যাঙ্গো লাচ্ছি",
            description = "Thick chilled sweet yogurt blended with ripe Rajshahi Himsagar mango pulp and toasted pistachio dust.",
            priceBdt = 190.0,
            discountPriceBdt = 160.0,
            ingredients = listOf("Himsagar Mango", "Mishti Doi", "Cardamom", "Pistachio"),
            sizes = listOf(MenuSizeOption("350ml Tall", 0.0), MenuSizeOption("500ml Large", 70.0)),
            addons = listOf(MenuAddonOption("add_nuts", "Extra Pistachio & Almond", 45.0)),
            customizationOptions = listOf("Normal Sweet", "Less Sugar"),
            isPopular = true,
            isBestSeller = true,
            isRecommended = true,
            imageKey = "dessert",
            galleryKeys = listOf("dessert"),
            rating = 4.9
        ),
        MenuItem(
            id = "item_peri_loaded_fries",
            restaurantId = "rest_cocoa_mango",
            categoryId = "cat_snacks",
            name = "Loaded Cheese & Jalapeno Nacho Box",
            nameBn = "লোডেড চিজ নাচোস বক্স",
            description = "Crispy tortilla chips topped with warm cheddar fondue, spiced chicken bits, salsa, and sour cream.",
            priceBdt = 260.0,
            discountPriceBdt = 220.0,
            ingredients = listOf("Corn Chips", "Cheddar Fondue", "Shredded Chicken", "Jalapeno", "Salsa"),
            sizes = listOf(MenuSizeOption("Regular Box", 0.0), MenuSizeOption("Party Tray", 180.0)),
            addons = listOf(MenuAddonOption("add_salsa", "Extra Roasted Salsa", 40.0)),
            customizationOptions = listOf("Classic", "Extra Jalapeno"),
            isPopular = false,
            isBestSeller = false,
            isRecommended = true,
            imageKey = "burger",
            galleryKeys = listOf("burger"),
            rating = 4.7
        )
    )
}
