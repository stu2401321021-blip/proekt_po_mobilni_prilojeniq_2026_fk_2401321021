package com.example.proekt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proekt.data.db.ParkingDatabaseHelper
import com.example.proekt.data.model.ParkingZone
import com.example.proekt.data.model.ScanLog
import com.example.proekt.data.model.Subscription
import com.example.proekt.data.model.SubscriptionStatus
import com.example.proekt.data.model.UserProfile
import com.example.proekt.data.repository.ParkingRepository
import com.example.proekt.data.repository.ParkingRepositoryImpl
import com.example.proekt.data.repository.ParkingStats
import com.example.proekt.data.repository.QrVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SubscriptionFilter(val title: String) {
    ALL("Всички"),
    ACTIVE("Активни"),
    EXPIRED("Изтекли"),
    ZONE_A("Зона А"),
    VIP("VIP")
}

class ParkingViewModel(
    application: Application,
    private val repository: ParkingRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        ParkingRepositoryImpl(ParkingDatabaseHelper(application))
    )

    private val _rawSubscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SubscriptionFilter.ALL)
    val selectedFilter: StateFlow<SubscriptionFilter> = _selectedFilter.asStateFlow()

    // Комбиниран поток от абонаменти след филтриране и търсене
    val filteredSubscriptions: StateFlow<List<Subscription>> = combine(
        _rawSubscriptions,
        _searchQuery,
        _selectedFilter
    ) { subs, query, filter ->
        subs.filter { sub ->
            val matchesQuery = query.isBlank() ||
                    sub.vehiclePlate.contains(query, ignoreCase = true) ||
                    sub.clientName.contains(query, ignoreCase = true) ||
                    sub.spotNumber.contains(query, ignoreCase = true) ||
                    sub.vehicleModel.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                SubscriptionFilter.ALL -> true
                SubscriptionFilter.ACTIVE -> sub.isCurrentlyActive
                SubscriptionFilter.EXPIRED -> !sub.isCurrentlyActive || sub.status == SubscriptionStatus.EXPIRED
                SubscriptionFilter.ZONE_A -> sub.zone == ParkingZone.ZONE_A
                SubscriptionFilter.VIP -> sub.zone == ParkingZone.VIP
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow(UserProfile(1, "", "", "", "", ""))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _stats = MutableStateFlow(ParkingStats(0, 0, 0, 0.0))
    val stats: StateFlow<ParkingStats> = _stats.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<ScanLog>>(emptyList())
    val recentLogs: StateFlow<List<ScanLog>> = _recentLogs.asStateFlow()

    private val _selectedSubscription = MutableStateFlow<Subscription?>(null)
    val selectedSubscription: StateFlow<Subscription?> = _selectedSubscription.asStateFlow()

    private val _verificationResult = MutableStateFlow<QrVerificationResult?>(null)
    val verificationResult: StateFlow<QrVerificationResult?> = _verificationResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val subs = repository.getAllSubscriptions()
                _rawSubscriptions.value = subs
                _userProfile.value = repository.getProfile()
                _stats.value = repository.getStats()
                _recentLogs.value = repository.getRecentScanLogs()

                // Ако няма избран абонамент, избираме първия активен
                if (_selectedSubscription.value == null && subs.isNotEmpty()) {
                    _selectedSubscription.value = subs.firstOrNull { it.isCurrentlyActive } ?: subs.first()
                }
            } catch (e: Exception) {
                _userMessage.value = "Грешка при зареждане: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: SubscriptionFilter) {
        _selectedFilter.value = filter
    }

    fun selectSubscription(sub: Subscription?) {
        _selectedSubscription.value = sub
    }

    fun addSubscription(sub: Subscription, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.addSubscription(sub)
                if (id > 0) {
                    _userMessage.value = "Успешно добавен абонамент за ${sub.vehiclePlate}!"
                    loadAllData()
                    // Избираме новия абонамент
                    val added = repository.getSubscriptionById(id)
                    if (added != null) {
                        _selectedSubscription.value = added
                    }
                    onComplete()
                } else {
                    _userMessage.value = "Неуспешен запис в базата данни."
                }
            } catch (e: Exception) {
                _userMessage.value = "Грешка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSubscription(sub: Subscription, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ok = repository.updateSubscription(sub)
                if (ok) {
                    _userMessage.value = "Абонаментът за ${sub.vehiclePlate} е обновен!"
                    loadAllData()
                    _selectedSubscription.value = sub
                    onComplete()
                } else {
                    _userMessage.value = "Грешка при обновяване."
                }
            } catch (e: Exception) {
                _userMessage.value = "Грешка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSubscription(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ok = repository.deleteSubscription(id)
                if (ok) {
                    _userMessage.value = "Абонаментът беше изтрит."
                    if (_selectedSubscription.value?.id == id) {
                        _selectedSubscription.value = null
                    }
                    loadAllData()
                    onComplete()
                } else {
                    _userMessage.value = "Неуспешно изтриване."
                }
            } catch (e: Exception) {
                _userMessage.value = "Грешка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyQrCode(payload: String, scanType: String = "Вход") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.verifyAndLogQr(payload, scanType)
                _verificationResult.value = result
                _recentLogs.value = repository.getRecentScanLogs()
                _stats.value = repository.getStats()
            } catch (e: Exception) {
                _userMessage.value = "Грешка при верификация: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearVerificationResult() {
        _verificationResult.value = null
    }

    fun saveProfile(profile: UserProfile, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveProfile(profile)
                _userProfile.value = profile
                _userMessage.value = "Профилът е запазен успешно!"
                onComplete()
            } catch (e: Exception) {
                _userMessage.value = "Грешка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.resetToDemoData()
                _searchQuery.value = ""
                _selectedFilter.value = SubscriptionFilter.ALL
                loadAllData()
                _userMessage.value = "Базата данни е възстановена с демо данни!"
            } catch (e: Exception) {
                _userMessage.value = "Грешка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
