package com.example.proekt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proekt.ui.theme.ParkGreenSuccess
import com.example.proekt.ui.theme.ParkRedDanger
import com.example.proekt.ui.viewmodel.ParkingViewModel
import com.example.proekt.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarrierScannerScreen(
    viewModel: ParkingViewModel,
    initialPayload: String = ""
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputPayload by remember { mutableStateOf(initialPayload) }
    var selectedScanType by remember { mutableStateOf("Вход") } // "Вход" или "Изход"

    LaunchedEffect(initialPayload) {
        if (initialPayload.isNotBlank()) {
            inputPayload = initialPayload
            viewModel.verifyQrCode(initialPayload, selectedScanType)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Контролен терминал на бариерата",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Проверка на QR пропуск и автоматично регистриране на вход/изход",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Карта за сканиране / проверка
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Избор на посока: Вход / Изход
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedScanType == "Вход",
                            onClick = { selectedScanType = "Вход" },
                            label = { Text("🟢 ВХОД (Влизане)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = selectedScanType == "Изход",
                            onClick = { selectedScanType = "Изход" },
                            label = { Text("🔵 ИЗХОД (Напускане)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Поле за QR код данни или регистрационен номер
                    OutlinedTextField(
                        value = inputPayload,
                        onValueChange = { inputPayload = it },
                        label = { Text("QR код данни или Рег. номер") },
                        placeholder = { Text("напр. PB 1234 AB или сканиран QR низ") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        trailingIcon = {
                            if (inputPayload.isNotBlank()) {
                                IconButton(onClick = { inputPayload = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Изчисти")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Бърз избор от съществуващи автомобили за лесна демонстрация
                    if (subscriptions.isNotEmpty()) {
                        Text(
                            text = "Бърз избор на автомобил за тестване:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subscriptions.take(3).forEach { sub ->
                                SuggestionChip(
                                    onClick = {
                                        inputPayload = sub.qrPayload.ifBlank { sub.generateDefaultQrPayload() }
                                    },
                                    label = { Text(sub.vehiclePlate, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Бутон за проверка
                    Button(
                        onClick = {
                            if (inputPayload.isNotBlank()) {
                                viewModel.verifyQrCode(inputPayload, selectedScanType)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = inputPayload.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Провери и задействай бариерата", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Резултат от проверката (Визуален статус на бариерата)
        if (verificationResult != null) {
            val result = verificationResult!!
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isValid) ParkGreenSuccess.copy(alpha = 0.12f) else ParkRedDanger.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (result.isValid) ParkGreenSuccess else ParkRedDanger
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (result.isValid) ParkGreenSuccess else ParkRedDanger),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.isValid) Icons.Default.Check else Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (result.isValid) "БАРИЕРАТА СЕ ОТВАРЯ!" else "ДОСТЪПЪТ Е ОТКАЗАН!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (result.isValid) ParkGreenSuccess else ParkRedDanger
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = result.message,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (result.subscription != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Зона", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(result.subscription.zone.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Паркомясто", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(result.subscription.spotNumber.ifBlank { "Свободно" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Валидност", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(DateUtils.formatDate(result.subscription.endDateEpochMs), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.clearVerificationResult() }) {
                            Text("Затвори съобщението")
                        }
                    }
                }
            }
        }

        // История на последните преминавания
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дневник на преминалите автомобили",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Записани в базата",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Text(
                    text = "Няма записани преминавания.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentLogs) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = log.plateNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (log.scanType == "Вход") Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFF1565C0).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = log.scanType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.scanType == "Вход") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (log.details.isNotBlank()) {
                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = DateUtils.formatDateTime(log.timestampEpochMs),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = log.resultStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (log.resultStatus) {
                                "Успешен" -> ParkGreenSuccess
                                else -> ParkRedDanger
                            }
                        )
                    }
                }
            }
        }
    }
}
