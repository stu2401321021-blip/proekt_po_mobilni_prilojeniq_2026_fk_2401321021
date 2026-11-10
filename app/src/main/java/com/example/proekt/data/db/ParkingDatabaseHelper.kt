package com.example.proekt.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.proekt.data.model.ParkingZone
import com.example.proekt.data.model.ScanLog
import com.example.proekt.data.model.Subscription
import com.example.proekt.data.model.SubscriptionStatus
import com.example.proekt.data.model.SubscriptionType
import com.example.proekt.data.model.UserProfile

class ParkingDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "parking_manager.db"
        private const val DATABASE_VERSION = 1

        // Таблица: Subscriptions
        const val TABLE_SUBSCRIPTIONS = "subscriptions"
        const val COL_SUB_ID = "id"
        const val COL_SUB_CLIENT_NAME = "client_name"
        const val COL_SUB_PHONE = "phone"
        const val COL_SUB_VEHICLE_PLATE = "vehicle_plate"
        const val COL_SUB_VEHICLE_MODEL = "vehicle_model"
        const val COL_SUB_SPOT_NUMBER = "spot_number"
        const val COL_SUB_ZONE = "zone"
        const val COL_SUB_TYPE = "type"
        const val COL_SUB_START_DATE = "start_date"
        const val COL_SUB_END_DATE = "end_date"
        const val COL_SUB_PRICE = "price"
        const val COL_SUB_STATUS = "status"
        const val COL_SUB_QR_PAYLOAD = "qr_payload"
        const val COL_SUB_NOTES = "notes"

        // Таблица: Profiles
        const val TABLE_PROFILES = "profiles"
        const val COL_PROF_ID = "id"
        const val COL_PROF_NAME = "full_name"
        const val COL_PROF_PHONE = "phone"
        const val COL_PROF_EMAIL = "email"
        const val COL_PROF_PLATE = "default_plate"
        const val COL_PROF_MODEL = "default_model"

        // Таблица: Scan Logs (QR проверки на бариерата)
        const val TABLE_SCAN_LOGS = "scan_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_SUB_ID = "subscription_id"
        const val COL_LOG_PLATE = "plate_number"
        const val COL_LOG_TYPE = "scan_type"
        const val COL_LOG_TIMESTAMP = "timestamp"
        const val COL_LOG_STATUS = "result_status"
        const val COL_LOG_DETAILS = "details"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSubscriptionsTable = """
            CREATE TABLE $TABLE_SUBSCRIPTIONS (
                $COL_SUB_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SUB_CLIENT_NAME TEXT NOT NULL,
                $COL_SUB_PHONE TEXT,
                $COL_SUB_VEHICLE_PLATE TEXT NOT NULL,
                $COL_SUB_VEHICLE_MODEL TEXT,
                $COL_SUB_SPOT_NUMBER TEXT,
                $COL_SUB_ZONE TEXT NOT NULL,
                $COL_SUB_TYPE TEXT NOT NULL,
                $COL_SUB_START_DATE INTEGER NOT NULL,
                $COL_SUB_END_DATE INTEGER NOT NULL,
                $COL_SUB_PRICE REAL NOT NULL,
                $COL_SUB_STATUS TEXT NOT NULL,
                $COL_SUB_QR_PAYLOAD TEXT,
                $COL_SUB_NOTES TEXT
            );
        """.trimIndent()

        val createProfilesTable = """
            CREATE TABLE $TABLE_PROFILES (
                $COL_PROF_ID INTEGER PRIMARY KEY,
                $COL_PROF_NAME TEXT NOT NULL,
                $COL_PROF_PHONE TEXT,
                $COL_PROF_EMAIL TEXT,
                $COL_PROF_PLATE TEXT,
                $COL_PROF_MODEL TEXT
            );
        """.trimIndent()

        val createScanLogsTable = """
            CREATE TABLE $TABLE_SCAN_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_SUB_ID INTEGER,
                $COL_LOG_PLATE TEXT NOT NULL,
                $COL_LOG_TYPE TEXT NOT NULL,
                $COL_LOG_TIMESTAMP INTEGER NOT NULL,
                $COL_LOG_STATUS TEXT NOT NULL,
                $COL_LOG_DETAILS TEXT
            );
        """.trimIndent()

        db.execSQL(createSubscriptionsTable)
        db.execSQL(createProfilesTable)
        db.execSQL(createScanLogsTable)

        // Индекси за бързо търсене по регистрационен номер и дати
        db.execSQL("CREATE INDEX idx_sub_plate ON $TABLE_SUBSCRIPTIONS($COL_SUB_VEHICLE_PLATE);")
        db.execSQL("CREATE INDEX idx_sub_status ON $TABLE_SUBSCRIPTIONS($COL_SUB_STATUS);")

        // Зареждане на първоначални примерни данни (Демо)
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUBSCRIPTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCAN_LOGS")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Профил на клиента по подразбиране
        val profileValues = ContentValues().apply {
            put(COL_PROF_ID, 1)
            put(COL_PROF_NAME, "Иван Иванов")
            put(COL_PROF_PHONE, "+359 888 123 456")
            put(COL_PROF_EMAIL, "ivan.ivanov@mail.bg")
            put(COL_PROF_PLATE, "PB 1234 AB")
            put(COL_PROF_MODEL, "Audi A4 (Черен)")
        }
        db.insert(TABLE_PROFILES, null, profileValues)

        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        // Примерен абонамент 1: Активен месечен абонамент
        val sub1 = ContentValues().apply {
            put(COL_SUB_CLIENT_NAME, "Иван Иванов")
            put(COL_SUB_PHONE, "+359 888 123 456")
            put(COL_SUB_VEHICLE_PLATE, "PB 1234 AB")
            put(COL_SUB_VEHICLE_MODEL, "Audi A4 (Черен)")
            put(COL_SUB_SPOT_NUMBER, "A-12")
            put(COL_SUB_ZONE, ParkingZone.ZONE_A.name)
            put(COL_SUB_TYPE, SubscriptionType.MONTHLY.name)
            put(COL_SUB_START_DATE, now - (5 * oneDayMs))
            put(COL_SUB_END_DATE, now + (25 * oneDayMs))
            put(COL_SUB_PRICE, 220.0)
            put(COL_SUB_STATUS, SubscriptionStatus.ACTIVE.name)
            put(COL_SUB_QR_PAYLOAD, "PARK-PASS|ID:1|PLATE:PB 1234 AB|ZONE:ZONE_A|EXPIRES:${now + 25 * oneDayMs}")
            put(COL_SUB_NOTES, "Централен вход, запазено място до асансьора")
        }
        db.insert(TABLE_SUBSCRIPTIONS, null, sub1)

        // Примерен абонамент 2: VIP целогодишен
        val sub2 = ContentValues().apply {
            put(COL_SUB_CLIENT_NAME, "Мария Петрова")
            put(COL_SUB_PHONE, "+359 899 987 654")
            put(COL_SUB_VEHICLE_PLATE, "CA 5678 TX")
            put(COL_SUB_VEHICLE_MODEL, "BMW X5 (Бял)")
            put(COL_SUB_SPOT_NUMBER, "VIP-01")
            put(COL_SUB_ZONE, ParkingZone.VIP.name)
            put(COL_SUB_TYPE, SubscriptionType.YEARLY.name)
            put(COL_SUB_START_DATE, now - (30 * oneDayMs))
            put(COL_SUB_END_DATE, now + (335 * oneDayMs))
            put(COL_SUB_PRICE, 2100.0)
            put(COL_SUB_STATUS, SubscriptionStatus.ACTIVE.name)
            put(COL_SUB_QR_PAYLOAD, "PARK-PASS|ID:2|PLATE:CA 5678 TX|ZONE:VIP|EXPIRES:${now + 335 * oneDayMs}")
            put(COL_SUB_NOTES, "Денонощен персонален охраняем достъп")
        }
        db.insert(TABLE_SUBSCRIPTIONS, null, sub2)

        // Примерен абонамент 3: Изтекъл седмичен абонамент
        val sub3 = ContentValues().apply {
            put(COL_SUB_CLIENT_NAME, "Георги Димитров")
            put(COL_SUB_PHONE, "+359 878 555 444")
            put(COL_SUB_VEHICLE_PLATE, "B 4321 CH")
            put(COL_SUB_VEHICLE_MODEL, "VW Golf 7 (Сребрист)")
            put(COL_SUB_SPOT_NUMBER, "B-08")
            put(COL_SUB_ZONE, ParkingZone.ZONE_B.name)
            put(COL_SUB_TYPE, SubscriptionType.WEEKLY.name)
            put(COL_SUB_START_DATE, now - (14 * oneDayMs))
            put(COL_SUB_END_DATE, now - (7 * oneDayMs))
            put(COL_SUB_PRICE, 70.0)
            put(COL_SUB_STATUS, SubscriptionStatus.EXPIRED.name)
            put(COL_SUB_QR_PAYLOAD, "PARK-PASS|ID:3|PLATE:B 4321 CH|ZONE:ZONE_B|EXPIRES:${now - 7 * oneDayMs}")
            put(COL_SUB_NOTES, "Нужно е подновяване")
        }
        db.insert(TABLE_SUBSCRIPTIONS, null, sub3)

        // Примерен лог от бариера
        val log1 = ContentValues().apply {
            put(COL_LOG_SUB_ID, 1)
            put(COL_LOG_PLATE, "PB 1234 AB")
            put(COL_LOG_TYPE, "Вход")
            put(COL_LOG_TIMESTAMP, now - (2 * 60 * 60 * 1000))
            put(COL_LOG_STATUS, "Успешен")
            put(COL_LOG_DETAILS, "Бариерата се отвори автоматично чрез QR сканиране")
        }
        db.insert(TABLE_SCAN_LOGS, null, log1)
    }

    // ==========================================
    // CRUD ОПЕРАЦИИ ЗА АБОНАМЕНТИ (SUBSCRIPTIONS)
    // ==========================================

    // CREATE: Добавяне на нов абонамент
    fun insertSubscription(sub: Subscription): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SUB_CLIENT_NAME, sub.clientName)
            put(COL_SUB_PHONE, sub.phone)
            put(COL_SUB_VEHICLE_PLATE, sub.vehiclePlate.trim().uppercase())
            put(COL_SUB_VEHICLE_MODEL, sub.vehicleModel)
            put(COL_SUB_SPOT_NUMBER, sub.spotNumber)
            put(COL_SUB_ZONE, sub.zone.name)
            put(COL_SUB_TYPE, sub.type.name)
            put(COL_SUB_START_DATE, sub.startDateEpochMs)
            put(COL_SUB_END_DATE, sub.endDateEpochMs)
            put(COL_SUB_PRICE, sub.price)
            put(COL_SUB_STATUS, sub.status.name)
            put(COL_SUB_QR_PAYLOAD, sub.qrPayload.ifBlank { sub.generateDefaultQrPayload() })
            put(COL_SUB_NOTES, sub.notes)
        }
        val newId = db.insert(TABLE_SUBSCRIPTIONS, null, values)
        // Ако няма QR payload, обновяваме с новия ID
        if (sub.qrPayload.isBlank() && newId > 0) {
            val updatedPayload = "PARK-PASS|ID:$newId|PLATE:${sub.vehiclePlate.uppercase()}|ZONE:${sub.zone.name}|EXPIRES:${sub.endDateEpochMs}"
            val updateVal = ContentValues().apply { put(COL_SUB_QR_PAYLOAD, updatedPayload) }
            db.update(TABLE_SUBSCRIPTIONS, updateVal, "$COL_SUB_ID = ?", arrayOf(newId.toString()))
        }
        return newId
    }

    // READ: Всички абонаменти
    fun getAllSubscriptions(): List<Subscription> {
        val list = mutableListOf<Subscription>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SUBSCRIPTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_SUB_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSubscription(it))
            }
        }
        return list
    }

    // READ: Търсене по номер или име
    fun searchSubscriptions(query: String): List<Subscription> {
        val list = mutableListOf<Subscription>()
        val db = readableDatabase
        val q = "%$query%"
        val cursor = db.query(
            TABLE_SUBSCRIPTIONS,
            null,
            "$COL_SUB_VEHICLE_PLATE LIKE ? OR $COL_SUB_CLIENT_NAME LIKE ? OR $COL_SUB_SPOT_NUMBER LIKE ?",
            arrayOf(q, q, q),
            null,
            null,
            "$COL_SUB_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSubscription(it))
            }
        }
        return list
    }

    // READ: По ID
    fun getSubscriptionById(id: Long): Subscription? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SUBSCRIPTIONS,
            null,
            "$COL_SUB_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToSubscription(it)
            }
        }
        return null
    }

    // READ: По регистрационен номер
    fun getSubscriptionByPlate(plate: String): Subscription? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SUBSCRIPTIONS,
            null,
            "$COL_SUB_VEHICLE_PLATE = ?",
            arrayOf(plate.trim().uppercase()),
            null,
            null,
            "$COL_SUB_ID DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToSubscription(it)
            }
        }
        return null
    }

    // UPDATE: Редактиране на съществуващ абонамент
    fun updateSubscription(sub: Subscription): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SUB_CLIENT_NAME, sub.clientName)
            put(COL_SUB_PHONE, sub.phone)
            put(COL_SUB_VEHICLE_PLATE, sub.vehiclePlate.trim().uppercase())
            put(COL_SUB_VEHICLE_MODEL, sub.vehicleModel)
            put(COL_SUB_SPOT_NUMBER, sub.spotNumber)
            put(COL_SUB_ZONE, sub.zone.name)
            put(COL_SUB_TYPE, sub.type.name)
            put(COL_SUB_START_DATE, sub.startDateEpochMs)
            put(COL_SUB_END_DATE, sub.endDateEpochMs)
            put(COL_SUB_PRICE, sub.price)
            put(COL_SUB_STATUS, sub.status.name)
            put(COL_SUB_QR_PAYLOAD, sub.qrPayload.ifBlank { sub.generateDefaultQrPayload() })
            put(COL_SUB_NOTES, sub.notes)
        }
        val rows = db.update(TABLE_SUBSCRIPTIONS, values, "$COL_SUB_ID = ?", arrayOf(sub.id.toString()))
        return rows > 0
    }

    // DELETE: Изтриване на абонамент
    fun deleteSubscription(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_SUBSCRIPTIONS, "$COL_SUB_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    private fun cursorToSubscription(cursor: Cursor): Subscription {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SUB_ID))
        val clientName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_CLIENT_NAME))
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_PHONE)) ?: ""
        val plate = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_VEHICLE_PLATE))
        val model = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_VEHICLE_MODEL)) ?: ""
        val spot = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_SPOT_NUMBER)) ?: ""
        val zoneStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_ZONE))
        val typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_TYPE))
        val startDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SUB_START_DATE))
        val endDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SUB_END_DATE))
        val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SUB_PRICE))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_STATUS))
        val qrPayload = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_QR_PAYLOAD)) ?: ""
        val notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_NOTES)) ?: ""

        val zone = ParkingZone.fromString(zoneStr)
        val type = SubscriptionType.fromString(typeStr)
        var status = SubscriptionStatus.fromString(statusStr)

        // Автоматично отчитане на изтекъл срок
        val now = System.currentTimeMillis()
        if (status == SubscriptionStatus.ACTIVE && now > endDate) {
            status = SubscriptionStatus.EXPIRED
        }

        return Subscription(
            id = id,
            clientName = clientName,
            phone = phone,
            vehiclePlate = plate,
            vehicleModel = model,
            spotNumber = spot,
            zone = zone,
            type = type,
            startDateEpochMs = startDate,
            endDateEpochMs = endDate,
            price = price,
            status = status,
            qrPayload = qrPayload,
            notes = notes
        )
    }

    // ==========================================
    // ПРОФИЛ НА ПОТРЕБИТЕЛЯ
    // ==========================================

    fun getProfile(): UserProfile {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILES, null, "$COL_PROF_ID = 1", null, null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return UserProfile(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_PROF_ID)),
                    fullName = it.getString(it.getColumnIndexOrThrow(COL_PROF_NAME)),
                    phone = it.getString(it.getColumnIndexOrThrow(COL_PROF_PHONE)) ?: "",
                    email = it.getString(it.getColumnIndexOrThrow(COL_PROF_EMAIL)) ?: "",
                    defaultPlate = it.getString(it.getColumnIndexOrThrow(COL_PROF_PLATE)) ?: "",
                    defaultVehicleModel = it.getString(it.getColumnIndexOrThrow(COL_PROF_MODEL)) ?: ""
                )
            }
        }
        return UserProfile(1, "Клиент", "", "", "", "")
    }

    fun saveProfile(profile: UserProfile): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PROF_ID, 1)
            put(COL_PROF_NAME, profile.fullName)
            put(COL_PROF_PHONE, profile.phone)
            put(COL_PROF_EMAIL, profile.email)
            put(COL_PROF_PLATE, profile.defaultPlate.uppercase())
            put(COL_PROF_MODEL, profile.defaultVehicleModel)
        }
        val rows = db.insertWithOnConflict(TABLE_PROFILES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return rows > 0
    }

    // ==========================================
    // ЛОГОВЕ ОТ БАРИЕРА / ПРОВЕРКИ
    // ==========================================

    fun insertScanLog(log: ScanLog): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_LOG_SUB_ID, log.subscriptionId)
            put(COL_LOG_PLATE, log.plateNumber.uppercase())
            put(COL_LOG_TYPE, log.scanType)
            put(COL_LOG_TIMESTAMP, log.timestampEpochMs)
            put(COL_LOG_STATUS, log.resultStatus)
            put(COL_LOG_DETAILS, log.details)
        }
        return db.insert(TABLE_SCAN_LOGS, null, values)
    }

    fun getRecentScanLogs(limit: Int = 20): List<ScanLog> {
        val list = mutableListOf<ScanLog>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SCAN_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COL_LOG_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ScanLog(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_LOG_ID)),
                        subscriptionId = if (it.isNull(it.getColumnIndexOrThrow(COL_LOG_SUB_ID))) null else it.getLong(it.getColumnIndexOrThrow(COL_LOG_SUB_ID)),
                        plateNumber = it.getString(it.getColumnIndexOrThrow(COL_LOG_PLATE)),
                        scanType = it.getString(it.getColumnIndexOrThrow(COL_LOG_TYPE)),
                        timestampEpochMs = it.getLong(it.getColumnIndexOrThrow(COL_LOG_TIMESTAMP)),
                        resultStatus = it.getString(it.getColumnIndexOrThrow(COL_LOG_STATUS)),
                        details = it.getString(it.getColumnIndexOrThrow(COL_LOG_DETAILS)) ?: ""
                    )
                )
            }
        }
        return list
    }

    // Рестартиране на базата с първоначални примерни данни
    fun resetToDemoData() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_SUBSCRIPTIONS")
        db.execSQL("DELETE FROM $TABLE_PROFILES")
        db.execSQL("DELETE FROM $TABLE_SCAN_LOGS")
        seedInitialData(db)
    }
}
