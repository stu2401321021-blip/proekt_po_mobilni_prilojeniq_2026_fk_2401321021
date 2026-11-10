package com.example.proekt.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("bg", "BG"))

    fun formatDate(epochMs: Long): String {
        return dateFormat.format(Date(epochMs))
    }

    fun formatDateTime(epochMs: Long): String {
        return dateTimeFormat.format(Date(epochMs))
    }

    fun formatFullDate(epochMs: Long): String {
        return fullDateFormat.format(Date(epochMs))
    }

    fun getRemainingDaysText(endDateEpochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = endDateEpochMs - now
        if (diff <= 0) return "Изтекъл"
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        return if (days > 0) {
            "Остават $days дни"
        } else {
            "Остават $hours ч."
        }
    }
}
