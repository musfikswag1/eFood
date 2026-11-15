package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.AppLanguage
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStage
import com.example.data.model.formatBdt
import com.example.ui.theme.EfoodError
import com.example.ui.theme.EfoodSuccess
import com.example.ui.viewmodel.EfoodUiState

@Composable
fun OrdersScreen(
    state: EfoodUiState,
    onOpenAuthSheet: () -> Unit,
    onOpenOrderTracking: (OrderRecord) -> Unit,
    onSubmitReview: (restaurantId: String, orderId: String, rating: Double, comment: String) -> Unit
) {
    var reviewTargetOrder by remember { mutableStateOf<OrderRecord?>(null) }

    if (reviewTargetOrder != null) {
        val target = reviewTargetOrder!!
        ReviewOrderDialog(
            order = target,
            onSubmit = { rating, comment ->
                onSubmitReview(target.restaurantId, target.id, rating, comment)
                reviewTargetOrder = null
            },
            onDismiss = { reviewTargetOrder = null }
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
                text = if (state.language == AppLanguage.BN) "আপনার অর্ডারসমূহ" else "Orders & Tracking",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (state.currentUser == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sign in to track live orders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Your active 8-stage delivery tracker, rider GPS status, and order history sync automatically once signed in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onOpenAuthSheet, modifier = Modifier.testTag("orders_sign_in_button")) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        } else if (state.customerOrders.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No orders yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Place your first order from Dhaka's top kitchens to see live 8-stage tracking here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(state.customerOrders, key = { it.id }) { order ->
                OrderSummaryCard(
                    order = order,
                    language = state.language,
                    onTrackClick = { onOpenOrderTracking(order) },
                    onReviewClick = { reviewTargetOrder = order }
                )
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    order: OrderRecord,
    language: AppLanguage,
    onTrackClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    val isCancelled = order.stage == OrderStage.CANCELLED
    val isDelivered = order.stage == OrderStage.DELIVERED
    Card(
        onClick = onTrackClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (!isCancelled && !isDelivered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCancelled -> MaterialTheme.colorScheme.errorContainer
                        isDelivered -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) order.stage.labelBn else order.stage.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isCancelled -> EfoodError
                            isDelivered -> EfoodSuccess
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Order #${order.id.takeLast(8)} • ${order.payment.labelEn}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatBdt(order.totalBdt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDelivered) {
                        OutlinedButton(onClick = onReviewClick) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate")
                        }
                    }
                    Button(onClick = onTrackClick) {
                        Text(if (isDelivered || isCancelled) "Details" else "Track Live")
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTrackingScreen(
    order: OrderRecord,
    language: AppLanguage,
    onBack: () -> Unit,
    onCancelOrder: (orderId: String, reason: String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        var reason by remember { mutableStateOf("Changed delivery plan") }
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Order?") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Cancellation reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCancelOrder(order.id, reason)
                    showCancelDialog = false
                }) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Order") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("tracking_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Live Order Tracking", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("Order ID: ${order.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ETA & Delivery OTP Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated Arrival", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (order.stage == OrderStage.DELIVERED) "Delivered"
                            else if (order.stage == OrderStage.CANCELLED) "Cancelled"
                            else "${order.estimatedMinutes} mins",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(order.restaurantName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Delivery OTP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(order.deliveryOtp, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Rider Live Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delivery Partner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (order.riderId.isBlank()) {
                        Text("Waiting for restaurant preparation & rider assignment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(order.riderName.ifBlank { "Assigned Rider" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                                Text(order.riderPhone.ifBlank { "Contact via app" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (order.riderPhone.isNotBlank()) {
                                OutlinedButton(onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.riderPhone}")))
                                    }
                                }) {
                                    Icon(Icons.Default.Call, contentDescription = "Call Rider", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call")
                                }
                            }
                        }
                        if (order.riderLocationSharing && order.riderLat != 0.0 && order.riderLng != 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EfoodSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Live GPS: ${String.format("%.4f", order.riderLat)}, ${String.format("%.4f", order.riderLng)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EfoodSuccess
                                        )
                                    }
                                    TextButton(onClick = {
                                        val uri = Uri.parse("https://maps.google.com/?q=${order.riderLat},${order.riderLng}")
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    }) {
                                        Text("View Map")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8-Stage Real-Time Order Timeline
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Order Progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    if (order.stage == OrderStage.CANCELLED) {
                        Text("Order Cancelled: ${order.failureReason.ifBlank { "Cancelled by user/admin" }}", color = EfoodError)
                    } else {
                        OrderStage.trackingSteps.forEach { step ->
                            val reached = order.stage.stepIndex >= step.stepIndex
                            val isCurrent = order.stage == step
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (reached) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (language == AppLanguage.BN) step.labelBn else step.labelEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else if (reached) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cancel Option for Early Orders
        if (order.stage in listOf(OrderStage.ORDER_PLACED, OrderStage.ORDER_CONFIRMED)) {
            item {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    border = BorderStroke(1.dp, EfoodError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Order", color = EfoodError)
                }
            }
        }
    }
}

@Composable
private fun ReviewOrderDialog(
    order: OrderRecord,
    onSubmit: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate ${order.restaurantName}") },
        text = {
            Column {
                Text("Rating: ${rating.toInt()} / 5 ★", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..5f, steps = 3)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Share your feedback") },
                    placeholder = { Text("Hot, fresh, and delivered on time!") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating.toDouble(), comment.ifBlank { "Great food and fast delivery!" }) }
            ) {
                Text("Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
