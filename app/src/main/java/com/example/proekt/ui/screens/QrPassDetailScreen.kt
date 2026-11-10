package com.example.proekt.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proekt.data.model.Subscription
import com.example.proekt.ui.theme.ParkGreenSuccess
import com.example.proekt.ui.theme.ParkRedDanger
import com.example.proekt.util.DateUtils
import com.example.proekt.util.QrCodeGenerator
import com.example.proekt.util.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPassDetailScreen(
    subscription: Subscription,
    onNavigateBack: () -> Unit,
    onTestAtBarrier: (String) -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val qrPayload = remember(subscription) {
        subscription.qrPayload.ifBlank { subscription.generateDefaultQrPayload() }
    }

    LaunchedEffect(qrPayload) {
        withContext(Dispatchers.Default) {
            val bmp = QrCodeGenerator.generateQrImageBitmap(qrPayload, size = 600)
            qrBitmap = bmp
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дигитален QR Паспорт", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { ShareHelper.shareSubscription(context, subscription) }) {
                        Icon(Icons.Default.Share, contentDescription = "Сподели")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Дигитален билет (Карта с бяла повърхност за четимост на QR)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Номер на автомобила
                    LicensePlateBadge(subscription.vehiclePlate)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Статус бадж
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (subscription.isCurrentlyActive) ParkGreenSuccess.copy(alpha = 0.15f) else ParkRedDanger.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (subscription.isCurrentlyActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (subscription.isCurrentlyActive) ParkGreenSuccess else ParkRedDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (subscription.isCurrentlyActive) "АКТИВЕН ДОСТЪП" else "ИЗТЕКЪЛ АБОНАМЕНТ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (subscription.isCurrentlyActive) ParkGreenSuccess else ParkRedDanger
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR код изображение
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!,
                                contentDescription = "QR Pass",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Покажете този QR код към камерата на бариерата",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Детайли за абонамента
                    DetailRow("Име на клиент", subscription.clientName)
                    if (subscription.phone.isNotBlank()) {
                        DetailRow("Телефон", subscription.phone)
                    }
                    if (subscription.vehicleModel.isNotBlank()) {
                        DetailRow("Автомобил", subscription.vehicleModel)
                    }
                    DetailRow("Паркинг зона", subscription.zone.displayName)
                    DetailRow("Паркомясто", subscription.spotNumber.ifBlank { "Свободно паркиране" })
                    DetailRow("Тип абонамент", subscription.type.displayName)
                    DetailRow("Период", "${DateUtils.formatDate(subscription.startDateEpochMs)} - ${DateUtils.formatDate(subscription.endDateEpochMs)}")
                    DetailRow("Оставащо време", DateUtils.getRemainingDaysText(subscription.endDateEpochMs))
                    DetailRow("Платена такса", "${String.format(java.util.Locale.US, "%.2f", subscription.price)} лв.")
                }
            }

            // Действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { ShareHelper.shareSubscription(context, subscription) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сподели")
                }

                FilledTonalButton(
                    onClick = { onTestAtBarrier(qrPayload) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Тествай бариера")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
