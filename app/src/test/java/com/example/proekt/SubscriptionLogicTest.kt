package com.example.proekt

import com.example.proekt.data.model.ParkingZone
import com.example.proekt.data.model.Subscription
import com.example.proekt.data.model.SubscriptionStatus
import com.example.proekt.data.model.SubscriptionType
import com.example.proekt.util.DateUtils
import org.junit.Assert.*
import org.junit.Test

class SubscriptionLogicTest {

    @Test
    fun subscription_isActive_returnsTrue_whenWithinDates() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        val sub = Subscription(
            id = 1,
            clientName = "Иван Иванов",
            phone = "0888123456",
            vehiclePlate = "PB 1234 AB",
            vehicleModel = "Audi A4",
            spotNumber = "A-12",
            zone = ParkingZone.ZONE_A,
            type = SubscriptionType.MONTHLY,
            startDateEpochMs = now - (5 * oneDay),
            endDateEpochMs = now + (25 * oneDay),
            price = 220.0,
            status = SubscriptionStatus.ACTIVE
        )

        assertTrue(sub.isCurrentlyActive)
        assertTrue(sub.remainingDays > 0)
    }

    @Test
    fun subscription_isActive_returnsFalse_whenExpired() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        val sub = Subscription(
            id = 2,
            clientName = "Георги Петров",
            phone = "0899123456",
            vehiclePlate = "CA 5555 TX",
            vehicleModel = "BMW 3",
            spotNumber = "B-01",
            zone = ParkingZone.ZONE_B,
            type = SubscriptionType.WEEKLY,
            startDateEpochMs = now - (14 * oneDay),
            endDateEpochMs = now - (7 * oneDay),
            price = 70.0,
            status = SubscriptionStatus.EXPIRED
        )

        assertFalse(sub.isCurrentlyActive)
        assertEquals(0L, sub.remainingDays)
    }

    @Test
    fun subscription_generateDefaultQrPayload_formatsCorrectly() {
        val now = System.currentTimeMillis()
        val sub = Subscription(
            id = 10,
            clientName = "Тест Клиент",
            phone = "0877112233",
            vehiclePlate = "CB 9999 AA",
            vehicleModel = "VW",
            spotNumber = "VIP-1",
            zone = ParkingZone.VIP,
            type = SubscriptionType.YEARLY,
            startDateEpochMs = now,
            endDateEpochMs = now + 100000,
            price = 2100.0
        )

        val payload = sub.generateDefaultQrPayload()

        assertTrue(payload.startsWith("PARK-PASS|ID:10|PLATE:CB 9999 AA|ZONE:VIP|EXPIRES:"))
    }

    @Test
    fun enumMapping_returnsFallbackOnUnknown() {
        val zone = ParkingZone.fromString("UNKNOWN_ZONE")
        assertEquals(ParkingZone.ZONE_A, zone)

        val type = SubscriptionType.fromString("UNKNOWN_TYPE")
        assertEquals(SubscriptionType.MONTHLY, type)

        val status = SubscriptionStatus.fromString("UNKNOWN_STATUS")
        assertEquals(SubscriptionStatus.ACTIVE, status)
    }

    @Test
    fun dateUtils_getRemainingDaysText_handlesExpired() {
        val past = System.currentTimeMillis() - 100000
        val text = DateUtils.getRemainingDaysText(past)
        assertEquals("Изтекъл", text)
    }
}
