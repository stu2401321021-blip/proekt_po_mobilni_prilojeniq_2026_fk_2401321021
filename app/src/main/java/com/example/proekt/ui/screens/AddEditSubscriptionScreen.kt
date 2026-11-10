package com.example.proekt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proekt.data.model.ParkingZone
import com.example.proekt.data.model.Subscription
import com.example.proekt.data.model.SubscriptionStatus
import com.example.proekt.data.model.SubscriptionType
import com.example.proekt.ui.viewmodel.ParkingViewModel
import com.example.proekt.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionScreen(
    viewModel: ParkingViewModel,
    existingSubscription: Subscription? = null,
    onNavigateBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()

    var clientName by remember { mutableStateOf(existingSubscription?.clientName ?: profile.fullName) }
    var phone by remember { mutableStateOf(existingSubscription?.phone ?: profile.phone) }
    var vehiclePlate by remember { mutableStateOf(existingSubscription?.vehiclePlate ?: profile.defaultPlate) }
    var vehicleModel by remember { mutableStateOf(existingSubscription?.vehicleModel ?: profile.defaultVehicleModel) }
    var spotNumber by remember { mutableStateOf(existingSubscription?.spotNumber ?: "") }
    var selectedZone by remember { mutableStateOf(existingSubscription?.zone ?: ParkingZone.ZONE_A) }
    var selectedType by remember { mutableStateOf(existingSubscription?.type ?: SubscriptionType.MONTHLY) }
    var priceText by remember { mutableStateOf(existingSubscription?.price?.toString() ?: selectedType.defaultPrice.toString()) }
    var notes by remember { mutableStateOf(existingSubscription?.notes ?: "") }
    var status by remember { mutableStateOf(existingSubscription?.status ?: SubscriptionStatus.ACTIVE) }

    var zoneExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 24L * 60 * 60 * 1000
    val calculatedEndDate = remember(selectedType, existingSubscription) {
        if (existingSubscription != null) {
            existingSubscription.endDateEpochMs
        } else {
            now + (selectedType.durationDays * oneDayMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingSubscription != null) "Редакция на абонамент" else "Нов абонамент",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            // Данни за клиента
            Text("Данни за клиента", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Име и фамилия *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Телефон за контакт") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            // Данни за автомобила
            Text("Автомобил и паркинг", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = vehiclePlate,
                onValueChange = { vehiclePlate = it.uppercase() },
                label = { Text("Регистрационен номер *") },
                placeholder = { Text("напр. PB 1234 AB") },
                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = vehicleModel,
                onValueChange = { vehicleModel = it },
                label = { Text("Марка и модел (по избор)") },
                placeholder = { Text("напр. Audi A4, VW Golf") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = spotNumber,
                onValueChange = { spotNumber = it },
                label = { Text("Паркомясто (по избор)") },
                placeholder = { Text("напр. A-12, B-04") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Избор на зона (Dropdown)
            ExposedDropdownMenuBox(
                expanded = zoneExpanded,
                onExpandedChange = { zoneExpanded = !zoneExpanded }
            ) {
                OutlinedTextField(
                    value = selectedZone.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Паркинг зона") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = zoneExpanded,
                    onDismissRequest = { zoneExpanded = false }
                ) {
                    ParkingZone.entries.forEach { zone ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(zone.displayName, fontWeight = FontWeight.Bold)
                                    Text(zone.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedZone = zone
                                zoneExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // План и валидност
            Text("Абонаментен план", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Избор на тип абонамент
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Тип на абонамента") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    SubscriptionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(type.displayName)
                                    Text("${type.defaultPrice} лв.", fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                selectedType = type
                                priceText = type.defaultPrice.toString()
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Карта с валидност
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Период на валидност", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${DateUtils.formatDate(now)} - ${DateUtils.formatDate(calculatedEndDate)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "${selectedType.durationDays} дни",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Цена
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Цена (лв.)") },
                leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            // Бележки
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Допълнителни бележки") },
                placeholder = { Text("напр. плащане по банков път, специални инструкции") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Бутон за запазване
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        errorMessage = "Моля, въведете име на клиента."
                        return@Button
                    }
                    if (vehiclePlate.isBlank()) {
                        errorMessage = "Моля, въведете регистрационен номер на автомобила."
                        return@Button
                    }
                    val priceVal = priceText.toDoubleOrNull()
                    if (priceVal == null || priceVal < 0) {
                        errorMessage = "Моля, въведете валидна цена."
                        return@Button
                    }

                    errorMessage = null

                    val startDate = existingSubscription?.startDateEpochMs ?: now
                    val endDate = if (existingSubscription != null) {
                        existingSubscription.endDateEpochMs
                    } else {
                        now + (selectedType.durationDays * oneDayMs)
                    }

                    val sub = Subscription(
                        id = existingSubscription?.id ?: 0,
                        clientName = clientName.trim(),
                        phone = phone.trim(),
                        vehiclePlate = vehiclePlate.trim().uppercase(),
                        vehicleModel = vehicleModel.trim(),
                        spotNumber = spotNumber.trim(),
                        zone = selectedZone,
                        type = selectedType,
                        startDateEpochMs = startDate,
                        endDateEpochMs = endDate,
                        price = priceVal,
                        status = status,
                        qrPayload = existingSubscription?.qrPayload ?: "",
                        notes = notes.trim()
                    )

                    if (existingSubscription == null) {
                        viewModel.addSubscription(sub) {
                            onNavigateBack()
                        }
                    } else {
                        viewModel.updateSubscription(sub) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (existingSubscription != null) "Обнови абонамент" else "Запази абонамент",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
