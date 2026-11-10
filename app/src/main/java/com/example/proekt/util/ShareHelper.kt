package com.example.proekt.util

import android.content.Context
import android.content.Intent
import com.example.proekt.data.model.Subscription

object ShareHelper {

    fun shareSubscription(context: Context, subscription: Subscription) {
        val shareText = """
            🚗 ДИГИТАЛЕН ПАРКИНГ АБОНАМЕНТ #SMARTPARK
            =======================================
            👤 Клиент: ${subscription.clientName}
            📞 Телефон: ${subscription.phone.ifBlank { "Няма" }}
            🚘 Автомобил: ${subscription.vehiclePlate} (${subscription.vehicleModel.ifBlank { "Няма модел" }})
            🅿️ Паркомясто: ${subscription.spotNumber.ifBlank { "Свободно паркиране" }}
            📍 Зона: ${subscription.zone.displayName}
            📋 Тип абонамент: ${subscription.type.displayName}
            📅 Период: ${DateUtils.formatDate(subscription.startDateEpochMs)} - ${DateUtils.formatDate(subscription.endDateEpochMs)}
            ⏳ Статус: ${subscription.status.displayName} (${DateUtils.getRemainingDaysText(subscription.endDateEpochMs)})
            💳 Цена: ${String.format(java.util.Locale.US, "%.2f", subscription.price)} лв.
            
            🔑 Код за верификация:
            ${subscription.qrPayload.ifBlank { subscription.generateDefaultQrPayload() }}
            =======================================
            Представете този билет или QR кода на бариерата за достъп.
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Паркинг абонамент за ${subscription.vehiclePlate}")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Сподели паркинг абонамент")
        context.startActivity(shareIntent)
    }
}
