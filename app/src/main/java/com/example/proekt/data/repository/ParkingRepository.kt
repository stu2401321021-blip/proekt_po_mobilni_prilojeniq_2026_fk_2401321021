package com.example.proekt.data.repository

import com.example.proekt.data.db.ParkingDatabaseHelper
import com.example.proekt.data.model.ScanLog
import com.example.proekt.data.model.Subscription
import com.example.proekt.data.model.SubscriptionStatus
import com.example.proekt.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParkingStats(
    val totalSubscriptions: Int,
    val activeSubscriptions: Int,
    val expiredSubscriptions: Int,
    val totalRevenue: Double
)

data class QrVerificationResult(
    val isValid: Boolean,
    val status: String,
    val message: String,
    val subscription: Subscription?
)

interface ParkingRepository {
    suspend fun getAllSubscriptions(): List<Subscription>
    suspend fun searchSubscriptions(query: String): List<Subscription>
    suspend fun getSubscriptionById(id: Long): Subscription?
    suspend fun getSubscriptionByPlate(plate: String): Subscription?
    suspend fun addSubscription(subscription: Subscription): Long
    suspend fun updateSubscription(subscription: Subscription): Boolean
    suspend fun deleteSubscription(id: Long): Boolean

    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(profile: UserProfile): Boolean

    suspend fun verifyAndLogQr(payload: String, scanType: String = "Вход"): QrVerificationResult
    suspend fun getRecentScanLogs(): List<ScanLog>
    suspend fun getStats(): ParkingStats
    suspend fun resetToDemoData()
}

class ParkingRepositoryImpl(
    private val dbHelper: ParkingDatabaseHelper
) : ParkingRepository {

    override suspend fun getAllSubscriptions(): List<Subscription> = withContext(Dispatchers.IO) {
        dbHelper.getAllSubscriptions()
    }

    override suspend fun searchSubscriptions(query: String): List<Subscription> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            dbHelper.getAllSubscriptions()
        } else {
            dbHelper.searchSubscriptions(query)
        }
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? = withContext(Dispatchers.IO) {
        dbHelper.getSubscriptionById(id)
    }

    override suspend fun getSubscriptionByPlate(plate: String): Subscription? = withContext(Dispatchers.IO) {
        dbHelper.getSubscriptionByPlate(plate)
    }

    override suspend fun addSubscription(subscription: Subscription): Long = withContext(Dispatchers.IO) {
        dbHelper.insertSubscription(subscription)
    }

    override suspend fun updateSubscription(subscription: Subscription): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateSubscription(subscription)
    }

    override suspend fun deleteSubscription(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteSubscription(id)
    }

    override suspend fun getProfile(): UserProfile = withContext(Dispatchers.IO) {
        dbHelper.getProfile()
    }

    override suspend fun saveProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        dbHelper.saveProfile(profile)
    }

    override suspend fun verifyAndLogQr(payload: String, scanType: String): QrVerificationResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val trimmed = payload.trim()

        // 1. Опит за извличане по ID или регистрационен номер
        // Очакван формат: "PARK-PASS|ID:1|PLATE:PB 1234 AB|ZONE:ZONE_A|EXPIRES:..."
        var sub: Subscription? = null

        if (trimmed.startsWith("PARK-PASS|")) {
            val parts = trimmed.split("|")
            for (p in parts) {
                if (p.startsWith("ID:")) {
                    val id = p.removePrefix("ID:").toLongOrNull()
                    if (id != null) {
                        sub = dbHelper.getSubscriptionById(id)
                    }
                }
                if (sub == null && p.startsWith("PLATE:")) {
                    val plate = p.removePrefix("PLATE:")
                    sub = dbHelper.getSubscriptionByPlate(plate)
                }
            }
        } else {
            // Търсене директно по номер или по точно съвпадение на payload
            sub = dbHelper.getSubscriptionByPlate(trimmed)
            if (sub == null) {
                val all = dbHelper.getAllSubscriptions()
                sub = all.find { it.qrPayload.equals(trimmed, ignoreCase = true) }
            }
        }

        if (sub == null) {
            val log = ScanLog(
                subscriptionId = null,
                plateNumber = trimmed.take(15),
                scanType = scanType,
                timestampEpochMs = now,
                resultStatus = "Неразпознат",
                details = "QR кодът не съответства на съществуващ абонамент"
            )
            dbHelper.insertScanLog(log)
            return@withContext QrVerificationResult(
                isValid = false,
                status = "НЕВАЛИДЕН",
                message = "Няма намерен абонамент за тези данни. Бариерата остава затворена.",
                subscription = null
            )
        }

        val isExpired = now > sub.endDateEpochMs || sub.status == SubscriptionStatus.EXPIRED

        if (isExpired) {
            val log = ScanLog(
                subscriptionId = sub.id,
                plateNumber = sub.vehiclePlate,
                scanType = scanType,
                timestampEpochMs = now,
                resultStatus = "Изтекъл",
                details = "Абонаментът е изтекъл на ${sub.endDateEpochMs}"
            )
            dbHelper.insertScanLog(log)
            return@withContext QrVerificationResult(
                isValid = false,
                status = "ИЗТЕКЪЛ",
                message = "Абонаментът за ${sub.vehiclePlate} (${sub.clientName}) е изтекъл! Достъпът е отказан.",
                subscription = sub
            )
        }

        // Успешна верификация
        val log = ScanLog(
            subscriptionId = sub.id,
            plateNumber = sub.vehiclePlate,
            scanType = scanType,
            timestampEpochMs = now,
            resultStatus = "Успешен",
            details = "Място: ${sub.spotNumber.ifBlank { "Свободно" }}, Зона: ${sub.zone.displayName}"
        )
        dbHelper.insertScanLog(log)

        return@withContext QrVerificationResult(
            isValid = true,
            status = "УСПЕШЕН",
            message = "Валиден абонамент! Бариерата се отваря за ${sub.vehiclePlate} (${sub.clientName}). Зона: ${sub.zone.displayName}.",
            subscription = sub
        )
    }

    override suspend fun getRecentScanLogs(): List<ScanLog> = withContext(Dispatchers.IO) {
        dbHelper.getRecentScanLogs(20)
    }

    override suspend fun getStats(): ParkingStats = withContext(Dispatchers.IO) {
        val all = dbHelper.getAllSubscriptions()
        val now = System.currentTimeMillis()
        val active = all.count { it.status == SubscriptionStatus.ACTIVE && now in it.startDateEpochMs..it.endDateEpochMs }
        val expired = all.count { it.status == SubscriptionStatus.EXPIRED || now > it.endDateEpochMs }
        val revenue = all.sumOf { it.price }

        ParkingStats(
            totalSubscriptions = all.size,
            activeSubscriptions = active,
            expiredSubscriptions = expired,
            totalRevenue = revenue
        )
    }

    override suspend fun resetToDemoData() = withContext(Dispatchers.IO) {
        dbHelper.resetToDemoData()
    }
}
