package com.example.proekt.data.model

enum class SubscriptionType(val displayName: String, val durationDays: Int, val defaultPrice: Double) {
    DAILY("Дневен (24ч)", 1, 15.0),
    WEEKLY("Седмичен (7 дни)", 7, 70.0),
    MONTHLY("Месечен (30 дни)", 30, 220.0),
    YEARLY("Годишен (365 дни)", 365, 2100.0),
    VIP("VIP Неограничен", 30, 350.0);

    companion object {
        fun fromString(value: String): SubscriptionType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class ParkingZone(val displayName: String, val description: String) {
    ZONE_A("Зона А (Централна)", "Партерно ниво до главния вход"),
    ZONE_B("Зона Б (Външна)", "Открит паркинг сектор Б"),
    UNDERGROUND("Подземен паркинг", "Ниво -1 с видеонаблюдение"),
    VIP("VIP Сектор", "Запазено закрито паркомясто");

    companion object {
        fun fromString(value: String): ParkingZone {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ZONE_A
        }
    }
}

enum class SubscriptionStatus(val displayName: String) {
    ACTIVE("Активен"),
    EXPIRED("Изтекъл"),
    PENDING("Чакащ"),
    CANCELLED("Анулиран");

    companion object {
        fun fromString(value: String): SubscriptionStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

data class Subscription(
    val id: Long = 0,
    val clientName: String,
    val phone: String,
    val vehiclePlate: String,
    val vehicleModel: String,
    val spotNumber: String,
    val zone: ParkingZone,
    val type: SubscriptionType,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val price: Double,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val qrPayload: String = "",
    val notes: String = ""
) {
    val isCurrentlyActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return status == SubscriptionStatus.ACTIVE && now in startDateEpochMs..endDateEpochMs
        }

    val remainingDays: Long
        get() {
            val now = System.currentTimeMillis()
            val diff = endDateEpochMs - now
            return if (diff > 0) diff / (1000 * 60 * 60 * 24) else 0
        }

    fun generateDefaultQrPayload(): String {
        return "PARK-PASS|ID:$id|PLATE:${vehiclePlate.uppercase()}|ZONE:${zone.name}|EXPIRES:$endDateEpochMs"
    }
}

data class UserProfile(
    val id: Long = 1,
    val fullName: String,
    val phone: String,
    val email: String,
    val defaultPlate: String,
    val defaultVehicleModel: String
)

data class ScanLog(
    val id: Long = 0,
    val subscriptionId: Long?,
    val plateNumber: String,
    val scanType: String, // "Вход" или "Изход"
    val timestampEpochMs: Long,
    val resultStatus: String, // "Успешен", "Изтекъл", "Неразпознат"
    val details: String = ""
)
