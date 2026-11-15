package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRequest
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStage
import com.example.data.model.formatBdt
import com.example.data.model.isValidBangladeshPhone
import com.example.ui.theme.EfoodError
import com.example.ui.theme.EfoodSuccess
import com.example.ui.theme.EfoodWarning
import com.example.ui.viewmodel.EfoodUiState

@Composable
fun RiderPortalScreen(
    state: EfoodUiState,
    onBackToCustomerApp: () -> Unit,
    onOpenGoogleSignIn: () -> Unit,
    onSubmitRegistration: (
        fullName: String,
        phone: String,
        nidNumber: String,
        vehicleType: String,
        emergencyContact: String,
        photoUri: String
    ) -> Unit,
    onToggleOnline: (Boolean, Double?, Double?) -> Unit,
    onAcceptRequest: (DeliveryRequest) -> Unit,
    onAdvanceStage: (OrderRecord, OrderStage, String?) -> Unit,
    onReportFailure: (orderId: String, reason: String) -> Unit
) {
    BackHandler(onBack = onBackToCustomerApp)
    val context = LocalContext.current

    var otpDialogOrder by remember { mutableStateOf<OrderRecord?>(null) }
    var failDialogOrder by remember { mutableStateOf<OrderRecord?>(null) }

    if (otpDialogOrder != null) {
        val target = otpDialogOrder!!
        var otpInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { otpDialogOrder = null },
            title = { Text("Confirm Delivery OTP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ask the customer (${target.customerName}) for their 4-digit Delivery OTP to finalize delivery & release payout.")
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("4-Digit Delivery OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdvanceStage(target, OrderStage.DELIVERED, otpInput)
                        otpDialogOrder = null
                    },
                    enabled = otpInput.trim().length >= 4
                ) {
                    Text("Verify & Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { otpDialogOrder = null }) { Text("Cancel") }
            }
        )
    }

    if (failDialogOrder != null) {
        val target = failDialogOrder!!
        var reason by remember { mutableStateOf("Customer unreachable by phone") }
        AlertDialog(
            onDismissRequest = { failDialogOrder = null },
            title = { Text("Report Failed Delivery") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Failure Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onReportFailure(target.id, reason)
                    failDialogOrder = null
                }) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { failDialogOrder = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Distinct Rider Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToCustomerApp, modifier = Modifier.testTag("rider_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Customer App")
                    }
                    Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("eFood Rider Partner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Text("Dedicated Courier Console", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (state.riderProfile != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (state.riderProfile.approvalStatus) {
                            "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                            "SUSPENDED" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = state.riderProfile.approvalStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (state.riderProfile.approvalStatus) {
                                "APPROVED" -> EfoodSuccess
                                "SUSPENDED" -> EfoodError
                                else -> EfoodWarning
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (state.currentUser == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text("Rider Partner Sign In", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Authenticate with your Rider Google Account to access NID onboarding, live dispatch requests, and weekly payouts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onOpenGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("rider_google_sign_in_button")
                ) {
                    Text("Sign in as Delivery Partner", fontWeight = FontWeight.Bold)
                }
            }
        } else if (state.riderProfile == null) {
            RiderRegistrationForm(
                initialName = state.currentUser.displayName ?: "",
                onSubmit = onSubmitRegistration
            )
        } else {
            val rider = state.riderProfile
            val activeOrders = state.riderAssignedOrders.filter { it.stage != OrderStage.DELIVERED && it.stage != OrderStage.CANCELLED }
            val completedOrders = state.riderAssignedOrders.filter { it.stage == OrderStage.DELIVERED }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Online/Offline & Approval Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, if (rider.isOnline) EfoodSuccess else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(rider.fullName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                    Text("${rider.vehicleType} • NID ${rider.nidNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = rider.isOnline,
                                    enabled = rider.approvalStatus == "APPROVED",
                                    onCheckedChange = { checked ->
                                        onToggleOnline(checked, 23.7461, 90.3742)
                                    },
                                    modifier = Modifier.testTag("rider_online_switch")
                                )
                            }
                            if (rider.approvalStatus != "APPROVED") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (rider.approvalStatus == "PENDING")
                                        "Admin Approval Pending: Only approved riders can go online and receive delivery assignments. Approve this rider from the eFood Web Admin Dashboard."
                                    else
                                        "Account Suspended by Admin. Contact Rider Operations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EfoodWarning
                                )
                            }
                        }
                    }
                }

                // Earnings & Performance Metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RiderMetricCard("Completed", "${rider.totalDeliveries}", Modifier.weight(1f))
                        RiderMetricCard("Weekly Earnings", formatBdt(rider.totalEarningsBdt), Modifier.weight(1f))
                        RiderMetricCard("Commission", "${(rider.commissionRate * 100).toInt()}%", Modifier.weight(1f))
                    }
                }

                // Active Assigned Orders
                if (activeOrders.isNotEmpty()) {
                    item {
                        Text("Active Assigned Deliveries (${activeOrders.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    items(activeOrders, key = { it.id }) { order ->
                        RiderActiveOrderCard(
                            order = order,
                            onNavigateMaps = { lat, lng ->
                                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                runCatching { context.startActivity(intent) }.onFailure {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng")))
                                }
                            },
                            onCallCustomer = { phone ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                }
                            },
                            onAdvanceStage = { next ->
                                if (next == OrderStage.DELIVERED) {
                                    otpDialogOrder = order
                                } else {
                                    onAdvanceStage(order, next, null)
                                }
                            },
                            onReportFailure = { failDialogOrder = order }
                        )
                    }
                }

                // Available Delivery Requests Queue
                if (rider.approvalStatus == "APPROVED" && rider.isOnline) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available Dispatch Queue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Window: ${state.riderRequestCountdownSec}s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (state.openDeliveryRequests.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Listening for new ready-for-pickup orders in ${state.selectedArea}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(state.openDeliveryRequests, key = { it.id }) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(req.restaurantName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                                        Text(formatBdt(req.estimatedFeeBdt), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${req.pickupArea} -> ${req.dropoffArea} (${req.distanceKm} km)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onAcceptRequest(req) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Accept Delivery Request")
                                    }
                                }
                            }
                        }
                    }
                }

                // Completed Delivery History
                if (completedOrders.isNotEmpty()) {
                    item {
                        Text("Recent Completed Deliveries", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    items(completedOrders.take(10), key = { "hist_${it.id}" }) { ord ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ord.restaurantName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                                    Text("${ord.deliveryArea} • #${ord.id.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "+${formatBdt(kotlin.math.max(40.0, ord.deliveryFeeBdt * 0.80))}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EfoodSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun RiderActiveOrderCard(
    order: OrderRecord,
    onNavigateMaps: (Double, Double) -> Unit,
    onCallCustomer: (String) -> Unit,
    onAdvanceStage: (OrderStage) -> Unit,
    onReportFailure: () -> Unit
) {
    val nextAction: Pair<String, OrderStage>? = when (order.stage) {
        OrderStage.RIDER_ASSIGNED -> "Confirm Pickup at Restaurant" to OrderStage.RIDER_PICKED_UP
        OrderStage.RIDER_PICKED_UP -> "Start Trip (On The Way)" to OrderStage.ON_THE_WAY
        OrderStage.ON_THE_WAY -> "Confirm Delivery with OTP" to OrderStage.DELIVERED
        else -> null
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(order.stage.labelEn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            Text("Dropoff: ${order.deliveryAddressText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (order.paymentMethod == "COD") "Collect Cash: ${formatBdt(order.totalBdt)}" else "Paid Online (${order.payment.labelEn})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigateMaps(order.deliveryLat, order.deliveryLng) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Maps")
                }
                OutlinedButton(
                    onClick = { onCallCustomer(order.customerPhone) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call")
                }
            }
            if (nextAction != null) {
                Button(
                    onClick = { onAdvanceStage(nextAction.second) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(nextAction.first, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onReportFailure, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = EfoodError, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Report Issue", color = EfoodError)
            }
        }
    }
}

@Composable
private fun RiderRegistrationForm(
    initialName: String,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("MOTORBIKE") }
    var emergency by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rider Partner Onboarding", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Submit your Bangladesh NID & vehicle details. Admin approval is required before accepting deliveries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Legal Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+8801XXXXXXXXX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = nid, onValueChange = { nid = it }, label = { Text("Bangladesh NID Number (10-17 digits)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = emergency, onValueChange = { emergency = it }, label = { Text("Emergency Contact (+8801XXXXXXXXX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            Text("Vehicle Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MOTORBIKE", "BICYCLE", "SCOOTER").forEach { v ->
                    val sel = vehicleType == v
                    Surface(
                        onClick = { vehicleType = v },
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = v,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri.isBlank()) "Select Profile Photo (Optional)" else "Profile Photo Selected")
            }
        }
        item {
            val valid = fullName.trim().length >= 2 &&
                isValidBangladeshPhone(phone) &&
                nid.trim().length in 10..20 &&
                isValidBangladeshPhone(emergency)
            Button(
                onClick = { onSubmit(fullName, phone, nid, vehicleType, emergency, photoUri) },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_rider_registration_button")
            ) {
                Text("Submit for Admin Approval", fontWeight = FontWeight.Bold)
            }
        }
    }
}
