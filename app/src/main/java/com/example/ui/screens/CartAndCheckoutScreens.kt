package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.SavedAddress
import com.example.data.model.formatBdt
import com.example.data.model.isValidBangladeshPhone
import com.example.data.model.localizedStrings
import com.example.ui.components.EfoodMediaImage
import com.example.ui.theme.BkashPink
import com.example.ui.theme.EfoodSuccess
import com.example.ui.theme.EfoodWarning
import com.example.ui.theme.NagadOrange
import com.example.ui.viewmodel.EfoodUiState

@Composable
fun CartScreen(
    state: EfoodUiState,
    onBack: () -> Unit,
    onUpdateQuantity: (cartKey: String, delta: Int) -> Unit,
    onApplyPromo: (String) -> Unit,
    onProceedToCheckout: () -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = localizedStrings(state.language)
    var promoInput by remember { mutableStateOf(state.appliedCoupon?.code ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("cart_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Your Cart",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (state.cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Your eFood cart is empty", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Explore Dhaka's top biryani, smash burgers, and wood-fired pizzas to start an order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onBack) {
                    Text("Browse Restaurants")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Ordering From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.cartRestaurantName ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                items(state.cartItems, key = { it.cartKey }) { item ->
                    CartItemRow(item = item, onUpdateQuantity = onUpdateQuantity)
                }

                // Promo Code Box + Suggested Coupons
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Promo Code", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = promoInput,
                                    onValueChange = { promoInput = it.uppercase() },
                                    placeholder = { Text("EFOOD50 / DHAKA20") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("promo_code_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onApplyPromo(promoInput) },
                                    modifier = Modifier.testTag("apply_promo_button")
                                ) {
                                    Text("Apply")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            state.coupons.filter { it.isActive }.take(3).forEach { coupon ->
                                Text(
                                    text = "• ${coupon.code}: ${coupon.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            promoInput = coupon.code
                                            onApplyPromo(coupon.code)
                                        }
                                        .padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Bill Summary
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BillRow(strings.subtotal, formatBdt(state.cartSubtotalBdt))
                            BillRow(strings.deliveryFee, formatBdt(state.cartDeliveryFeeBdt))
                            if (state.cartDiscountBdt > 0.0) {
                                BillRow(strings.discount, "-${formatBdt(state.cartDiscountBdt)}", highlight = true)
                            }
                            BillRow(strings.vatTax, formatBdt(state.cartTaxBdt))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.totalPayable, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    text = formatBdt(state.cartTotalBdt),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onProceedToCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp)
                        .testTag("proceed_checkout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("${strings.checkout} • ${formatBdt(state.cartTotalBdt)}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onUpdateQuantity: (String, Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EfoodMediaImage(
                imageKey = item.menuItem.imageKey,
                contentDescription = item.menuItem.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.menuItem.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = buildString {
                        append(item.selectedSize.name)
                        if (item.selectedAddons.isNotEmpty()) {
                            append(" + ")
                            append(item.selectedAddons.joinToString { it.name })
                        }
                        if (item.selectedCustomization.isNotBlank()) {
                            append(" • ${item.selectedCustomization}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.specialInstructions.isNotBlank()) {
                    Text(
                        text = "Note: ${item.specialInstructions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatBdt(item.lineTotalBdt), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdateQuantity(item.cartKey, -1) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text("${item.quantity}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = { onUpdateQuantity(item.cartKey, 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) EfoodSuccess else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CheckoutScreen(
    state: EfoodUiState,
    onBack: () -> Unit,
    onOpenAuthAndPhoneSheet: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    onSelectSavedAddress: (String) -> Unit,
    onSaveNewAddress: (SavedAddress) -> Unit,
    onPlaceOrder: (
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
    ) -> Unit,
    onDismissGatewayNotice: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val strings = localizedStrings(state.language)
    val selectedSavedAddress = state.savedAddresses.firstOrNull { it.id == state.selectedAddressId }

    var customerName by remember(state.userProfile, state.currentUser) {
        mutableStateOf(state.userProfile?.displayName ?: state.currentUser?.displayName ?: "")
    }
    var customerPhone by remember(state.userProfile) {
        mutableStateOf(state.userProfile?.phone ?: "")
    }
    var streetAddress by remember(selectedSavedAddress) {
        mutableStateOf(selectedSavedAddress?.streetAddress ?: "")
    }
    var landmark by remember(selectedSavedAddress) {
        mutableStateOf(selectedSavedAddress?.landmark ?: "")
    }
    var deliveryInstructions by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.COD) }

    val contactReady = state.currentUser != null && customerName.isNotBlank() && isValidBangladeshPhone(customerPhone)
    val addressReady = streetAddress.trim().length >= 3

    if (state.pendingGatewayNotice != null) {
        AlertDialog(
            onDismissRequest = onDismissGatewayNotice,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = EfoodWarning) },
            title = { Text("Payment Gateway Notice") },
            text = { Text(state.pendingGatewayNotice) },
            confirmButton = {
                TextButton(onClick = {
                    paymentMethod = PaymentMethod.COD
                    onDismissGatewayNotice()
                }) {
                    Text("Switch to Cash on Delivery")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissGatewayNotice) { Text("Close") }
            }
        )
    }

    if (state.activeGatewaySessionUrl != null) {
        AlertDialog(
            onDismissRequest = onDismissGatewayNotice,
            title = { Text("Complete Online Payment") },
            text = { Text("Your verified ${paymentMethod.labelEn} gateway session is ready. Open the secure checkout page to complete payment.") },
            confirmButton = {
                Button(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.activeGatewaySessionUrl)))
                    }
                    onDismissGatewayNotice()
                }) {
                    Text("Open Gateway")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissGatewayNotice) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("checkout_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(strings.checkout, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Account & Verified +880 Contact Gate
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, if (contactReady) EfoodSuccess else MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Verified Contact", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            if (state.currentUser != null && state.isPhoneVerifiedForCheckout) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = EfoodSuccess)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (state.currentUser == null) {
                            Text(
                                "Sign in with Google & verify your +880 phone number before confirming an order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenAuthAndPhoneSheet,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_sign_in_button")
                            ) {
                                Text(strings.signInGoogle)
                            }
                        } else {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Receiver Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Phone (+8801XXXXXXXXX)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_phone_input")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onOpenAuthAndPhoneSheet) {
                                Text(if (state.isPhoneVerifiedForCheckout) "Verified with Google Account" else "Verify & Save Contact to Profile")
                            }
                        }
                    }
                }
            }

            // 2. Delivery Address (Bangladesh Division/District/Area/Landmark + Map Location)
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
                            Text("2. Delivery Address", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            TextButton(onClick = onOpenLocationPicker) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.selectedArea}, ${state.selectedDistrict}")
                            }
                        }
                        if (state.savedAddresses.isNotEmpty()) {
                            state.savedAddresses.forEach { addr ->
                                val selected = addr.id == state.selectedAddressId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectSavedAddress(addr.id)
                                            streetAddress = addr.streetAddress
                                            landmark = addr.landmark
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected, onClick = {
                                        onSelectSavedAddress(addr.id)
                                        streetAddress = addr.streetAddress
                                        landmark = addr.landmark
                                    })
                                    Column {
                                        Text("${addr.label} • ${addr.area}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                                        Text(addr.formattedSummary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        OutlinedTextField(
                            value = streetAddress,
                            onValueChange = { streetAddress = it },
                            label = { Text("House, Road, Block / Flat No.") },
                            placeholder = { Text("House 14, Road 6, Apt 4B") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_address_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = landmark,
                            onValueChange = { landmark = it },
                            label = { Text("Nearby Landmark (Optional)") },
                            placeholder = { Text("Opposite Eidgah Field") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deliveryInstructions,
                            onValueChange = { deliveryInstructions = it },
                            label = { Text("Delivery Instructions") },
                            placeholder = { Text("Call upon arrival at main gate") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.currentUser != null && addressReady) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    onSaveNewAddress(
                                        SavedAddress(
                                            label = "Saved",
                                            division = state.selectedDistrict,
                                            district = state.selectedDistrict,
                                            area = state.selectedArea,
                                            landmark = landmark,
                                            streetAddress = streetAddress,
                                            isDefault = state.savedAddresses.isEmpty()
                                        )
                                    )
                                }
                            ) {
                                Text("Save Address to Account")
                            }
                        }
                    }
                }
            }

            // 3. Payment Method (COD, bKash, Nagad, Card)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Payment Method", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        PaymentMethod.entries.forEach { method ->
                            val selected = paymentMethod == method
                            val badgeColor = when (method) {
                                PaymentMethod.BKASH -> BkashPink
                                PaymentMethod.NAGAD -> NagadOrange
                                PaymentMethod.COD -> EfoodSuccess
                                PaymentMethod.CARD -> MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                onClick = { paymentMethod = method },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_${method.code}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected, onClick = { paymentMethod = method })
                                    Icon(
                                        imageVector = if (method == PaymentMethod.COD) Icons.Default.Payments else Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = badgeColor
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(method.labelEn, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                                        Text(
                                            text = if (method == PaymentMethod.COD) "Pay cash to rider upon OTP delivery"
                                            else "Merchant gateway verification via server",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Order Summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Order Summary (${state.cartItemCount} items)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        BillRow(strings.subtotal, formatBdt(state.cartSubtotalBdt))
                        BillRow(strings.deliveryFee, formatBdt(state.cartDeliveryFeeBdt))
                        if (state.cartDiscountBdt > 0.0) BillRow(strings.discount, "-${formatBdt(state.cartDiscountBdt)}", highlight = true)
                        BillRow(strings.vatTax, formatBdt(state.cartTaxBdt))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        BillRow(strings.totalPayable, formatBdt(state.cartTotalBdt), highlight = true)
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val fullAddr = listOf(streetAddress, landmark.takeIf { it.isNotBlank() }, state.selectedArea, state.selectedDistrict)
                        .filterNotNull()
                        .joinToString(", ")
                    onPlaceOrder(
                        customerName,
                        customerPhone,
                        fullAddr,
                        state.selectedArea,
                        state.selectedDistrict,
                        landmark,
                        deliveryInstructions,
                        selectedSavedAddress?.latitude ?: 23.7461,
                        selectedSavedAddress?.longitude ?: 90.3742,
                        paymentMethod
                    )
                },
                enabled = contactReady && addressReady && !state.isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp)
                    .testTag("confirm_order_button")
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("${strings.placeOrder} • ${formatBdt(state.cartTotalBdt)}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
