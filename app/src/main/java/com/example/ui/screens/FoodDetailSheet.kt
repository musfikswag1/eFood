package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.MenuAddonOption
import com.example.data.model.MenuItem
import com.example.data.model.MenuSizeOption
import com.example.data.model.Restaurant
import com.example.data.model.formatBdt
import com.example.data.model.localizedStrings
import com.example.ui.components.EfoodMediaImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodDetailSheet(
    item: MenuItem,
    restaurant: Restaurant,
    language: AppLanguage,
    onAddToCart: (
        size: MenuSizeOption,
        addons: List<MenuAddonOption>,
        customization: String,
        instructions: String,
        quantity: Int,
        buyNow: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = localizedStrings(language)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sizes = item.sizes.ifEmpty { listOf(MenuSizeOption("Regular", 0.0)) }
    val gallery = item.galleryKeys.ifEmpty { listOf(item.imageKey) }
    val pagerState = rememberPagerState(pageCount = { gallery.size })

    var selectedSize by remember(item.id) { mutableStateOf(sizes.first()) }
    var selectedAddons by remember(item.id) { mutableStateOf(emptySet<String>()) }
    var selectedCustomization by remember(item.id) {
        mutableStateOf(item.customizationOptions.firstOrNull() ?: "Standard")
    }
    var instructions by remember(item.id) { mutableStateOf("") }
    var quantity by remember(item.id) { mutableIntStateOf(1) }

    val chosenAddons = item.addons.filter { it.id in selectedAddons }
    val unitPrice = item.effectivePriceBdt + selectedSize.extraPriceBdt + chosenAddons.sumOf { it.priceBdt }
    val totalPrice = unitPrice * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Multiple Images Gallery Pager
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    EfoodMediaImage(
                        imageKey = gallery[page],
                        imageUrl = item.imageUrl,
                        contentDescription = "${item.name} photo ${page + 1}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (gallery.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(gallery.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (language == AppLanguage.BN && item.nameBn.isNotBlank()) item.nameBn else item.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatBdt(item.effectivePriceBdt),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                if (item.hasDiscount) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatBdt(item.priceBdt),
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = "${item.discountPercent}% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Ingredients Chips
            if (item.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Ingredients", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.ingredients.forEach { ing ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = ing,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Sizes Selector
            Spacer(modifier = Modifier.height(16.dp))
            Text("Choose Size", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
            sizes.forEach { sizeOption ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSize = sizeOption }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedSize == sizeOption, onClick = { selectedSize = sizeOption })
                    Text(
                        text = sizeOption.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (sizeOption.extraPriceBdt > 0) "+${formatBdt(sizeOption.extraPriceBdt)}" else "Base",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Add-ons
            if (item.addons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Add-ons (Optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
                item.addons.forEach { addon ->
                    val checked = addon.id in selectedAddons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAddons = if (checked) selectedAddons - addon.id else selectedAddons + addon.id
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedAddons = if (checked) selectedAddons - addon.id else selectedAddons + addon.id
                            }
                        )
                        Text(
                            text = addon.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${formatBdt(addon.priceBdt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Customization options
            if (item.customizationOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Preparation Preference", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.customizationOptions.forEach { opt ->
                        val active = opt == selectedCustomization
                        Surface(
                            onClick = { selectedCustomization = opt },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Special Instructions
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Special instructions for kitchen (optional)") },
                placeholder = { Text("e.g., Less oil, extra crispy onions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Quantity & Action Buttons
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.testTag("qty_minus_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.testTag("qty_plus_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                        }
                    }
                }

                Text(
                    text = formatBdt(totalPrice),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onAddToCart(selectedSize, chosenAddons, selectedCustomization, instructions, quantity, false)
                        onDismiss()
                    },
                    enabled = item.isAvailable,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_to_cart_button")
                ) {
                    Text(strings.addToCart, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = {
                        onAddToCart(selectedSize, chosenAddons, selectedCustomization, instructions, quantity, true)
                        onDismiss()
                    },
                    enabled = item.isAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_now_button")
                ) {
                    Text(strings.buyNow, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
