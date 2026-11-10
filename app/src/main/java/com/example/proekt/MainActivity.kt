package com.example.proekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proekt.data.model.Subscription
import com.example.proekt.ui.screens.*
import com.example.proekt.ui.theme.ProektTheme
import com.example.proekt.ui.viewmodel.ParkingViewModel

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Начало", Icons.Default.Dashboard),
    SUBSCRIPTIONS("Абонаменти", Icons.Default.DirectionsCar),
    QR_PASS("QR Паспорт", Icons.Default.QrCode2),
    SCANNER("Бариера", Icons.Default.QrCodeScanner),
    PROFILE("Профил", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            ProektTheme(darkTheme = isDarkTheme) {
                MainAppContent(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    viewModel: ParkingViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var scannerInitialPayload by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    val userMessage by viewModel.userMessage.collectAsState()
    val selectedSub by viewModel.selectedSubscription.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Показване на съобщения за потребителя
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Скриване на долната лента, когато сме в екран за добавяне/редакция
            if (!isAddingNew && editingSubscription == null) {
                NavigationBar {
                    AppScreen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                isAddingNew = false
                                editingSubscription = null
                                currentScreen = screen
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                // Екран за добавяне или редакция
                isAddingNew || editingSubscription != null -> {
                    AddEditSubscriptionScreen(
                        viewModel = viewModel,
                        existingSubscription = editingSubscription,
                        onNavigateBack = {
                            isAddingNew = false
                            editingSubscription = null
                        }
                    )
                }

                // Основни екрани от долната навигация
                currentScreen == AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToQr = { sub ->
                            viewModel.selectSubscription(sub)
                            currentScreen = AppScreen.QR_PASS
                        },
                        onNavigateToAdd = {
                            editingSubscription = null
                            isAddingNew = true
                        },
                        onNavigateToSubscriptions = {
                            currentScreen = AppScreen.SUBSCRIPTIONS
                        },
                        onNavigateToScanner = {
                            scannerInitialPayload = ""
                            currentScreen = AppScreen.SCANNER
                        }
                    )
                }

                currentScreen == AppScreen.SUBSCRIPTIONS -> {
                    SubscriptionsListScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = {
                            editingSubscription = null
                            isAddingNew = true
                        },
                        onNavigateToEdit = { sub ->
                            editingSubscription = sub
                        },
                        onNavigateToQr = { sub ->
                            viewModel.selectSubscription(sub)
                            currentScreen = AppScreen.QR_PASS
                        }
                    )
                }

                currentScreen == AppScreen.QR_PASS -> {
                    if (selectedSub != null) {
                        QrPassDetailScreen(
                            subscription = selectedSub!!,
                            onNavigateBack = {
                                currentScreen = AppScreen.DASHBOARD
                            },
                            onTestAtBarrier = { payload ->
                                scannerInitialPayload = payload
                                currentScreen = AppScreen.SCANNER
                            }
                        )
                    } else {
                        // Ако няма избран абонамент
                        EmptyQrScreen(
                            onAddSubscription = {
                                editingSubscription = null
                                isAddingNew = true
                            }
                        )
                    }
                }

                currentScreen == AppScreen.SCANNER -> {
                    BarrierScannerScreen(
                        viewModel = viewModel,
                        initialPayload = scannerInitialPayload
                    )
                }

                currentScreen == AppScreen.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyQrScreen(onAddSubscription: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.QrCode2,
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Няма избран абонамент",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Изберете карта от списъка или добавете нова, за да видите вашия QR пропуск.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = onAddSubscription) {
                Icon(Icons.Default.Add, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
                Text("Добави абонамент")
            }
        }
    }
}
