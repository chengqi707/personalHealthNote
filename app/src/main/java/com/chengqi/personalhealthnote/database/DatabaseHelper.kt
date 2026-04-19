package com.chengqi.personalhealthnote.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicineReminder

/**
 * SQLite数据库帮助类
 * 管理数据库创建、升级及所有数据操作
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "health_note.db"
        private const val DATABASE_VERSION = 2

        // 健康记录表
        private const val TABLE_HEALTH_RECORD = "health_record"
        private const val COLUMN_ID = "id"
        private const val COLUMN_RECORD_DATE = "record_date"
        private const val COLUMN_WEIGHT = "weight"
        private const val COLUMN_SYSTOLIC_PRESSURE = "systolic_pressure"
        private const val COLUMN_DIASTOLIC_PRESSURE = "diastolic_pressure"
        private const val COLUMN_HEART_RATE = "heart_rate"
        private const val COLUMN_BLOOD_SUGAR = "blood_sugar"
        private const val COLUMN_SLEEP_DURATION = "sleep_duration"
        private const val COLUMN_WATER_INTAKE = "water_intake"
        private const val COLUMN_STEPS = "steps"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_IMAGE_PATHS = "image_paths" // 就诊/检查单据图片路径，JSON数组格式
        private const val COLUMN_CREATE_TIME = "create_time"
        private const val COLUMN_UPDATE_TIME = "update_time"

        // 用药提醒表
        private const val TABLE_MEDICINE_REMINDER = "medicine_reminder"
        private const val COLUMN_MEDICINE_NAME = "medicine_name"
        private const val COLUMN_DOSAGE = "dosage"
        private const val COLUMN_UNIT = "unit"
        private const val COLUMN_FREQUENCY = "frequency"
        private const val COLUMN_REMIND_TIME1 = "remind_time1"
        private const val COLUMN_REMIND_TIME2 = "remind_time2"
        private const val COLUMN_REMIND_TIME3 = "remind_time3"
        private const val COLUMN_REMIND_TIME4 = "remind_time4"
        private const val COLUMN_REMIND_TIME5 = "remind_time5"
        private const val COLUMN_IS_ENABLED = "is_enabled"
        private const val COLUMN_START_DATE = "start_date"
        private const val COLUMN_END_DATE = "end_date"
        private const val COLUMN_BEFORE_AFTER_MEAL = "before_after_meal"
        private const val COLUMN_MEDICINE_NOTES = "medicine_notes"

        // 创建健康记录表的SQL
        private const val CREATE_HEALTH_RECORD_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_HEALTH_RECORD (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECORD_DATE TEXT NOT NULL UNIQUE,
                $COLUMN_WEIGHT REAL DEFAULT 0,
                $COLUMN_SYSTOLIC_PRESSURE INTEGER DEFAULT 0,
                $COLUMN_DIASTOLIC_PRESSURE INTEGER DEFAULT 0,
                $COLUMN_HEART_RATE INTEGER DEFAULT 0,
                $COLUMN_BLOOD_SUGAR REAL DEFAULT 0,
                $COLUMN_SLEEP_DURATION REAL DEFAULT 0,
                $COLUMN_WATER_INTAKE INTEGER DEFAULT 0,
                $COLUMN_STEPS INTEGER DEFAULT 0,
                $COLUMN_NOTES TEXT,
                $COLUMN_IMAGE_PATHS TEXT DEFAULT '',
                $COLUMN_CREATE_TIME INTEGER DEFAULT 0,
                $COLUMN_UPDATE_TIME INTEGER DEFAULT 0
            )
        """

        // 创建用药提醒表的SQL
        private const val CREATE_MEDICINE_REMINDER_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_MEDICINE_REMINDER (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MEDICINE_NAME TEXT NOT NULL,
                $COLUMN_DOSAGE TEXT,
                $COLUMN_UNIT TEXT,
                $COLUMN_FREQUENCY INTEGER DEFAULT 1,
                $COLUMN_REMIND_TIME1 TEXT NOT NULL,
                $COLUMN_REMIND_TIME2 TEXT,
                $COLUMN_REMIND_TIME3 TEXT,
                $COLUMN_REMIND_TIME4 TEXT,
                $COLUMN_REMIND_TIME5 TEXT,
                $COLUMN_IS_ENABLED INTEGER DEFAULT 1,
                $COLUMN_START_DATE TEXT NOT NULL,
                $COLUMN_END_DATE TEXT,
                $COLUMN_BEFORE_AFTER_MEAL INTEGER DEFAULT 0,
                $COLUMN_MEDICINE_NOTES TEXT,
                $COLUMN_CREATE_TIME INTEGER DEFAULT 0,
                $COLUMN_UPDATE_TIME INTEGER DEFAULT 0
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_HEALTH_RECORD_TABLE)
        db?.execSQL(CREATE_MEDICINE_REMINDER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 增量升级，保留用户数据
        if (oldVersion < 2) {
            // 版本1升级到版本2，添加图片路径字段
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_IMAGE_PATHS TEXT DEFAULT ''")
        }
    }

    // ==================== 健康记录 CRUD 操作 ====================

    /**
     * 插入健康记录
     * @param record 健康记录对象
     * @return 插入记录的ID，-1表示插入失败
     */
    fun insertHealthRecord(record: HealthRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_RECORD_DATE, record.recordDate)
            put(COLUMN_WEIGHT, record.weight)
            put(COLUMN_SYSTOLIC_PRESSURE, record.systolicPressure)
            put(COLUMN_DIASTOLIC_PRESSURE, record.diastolicPressure)
            put(COLUMN_HEART_RATE, record.heartRate)
            put(COLUMN_BLOOD_SUGAR, record.bloodSugar)
            put(COLUMN_SLEEP_DURATION, record.sleepDuration)
            put(COLUMN_WATER_INTAKE, record.waterIntake)
            put(COLUMN_STEPS, record.steps)
            put(COLUMN_NOTES, record.notes)
            put(COLUMN_IMAGE_PATHS, record.imagePaths)
            put(COLUMN_CREATE_TIME, record.createTime)
            put(COLUMN_UPDATE_TIME, record.updateTime)
        }
        return db.insert(TABLE_HEALTH_RECORD, null, values)
    }

    /**
     * 更新健康记录
     * @param record 健康记录对象
     * @return 更新的行数
     */
    fun updateHealthRecord(record: HealthRecord): Int {
        // 获取原有记录，对比图片，删除不再使用的图片
        val oldRecord = getHealthRecordById(record.id)
        oldRecord?.let { old ->
            if (old.imagePaths.isNotEmpty() && old.imagePaths != record.imagePaths) {
                try {
                    val oldPaths = mutableListOf<String>()
                    val oldJson = org.json.JSONArray(old.imagePaths)
                    for (i in 0 until oldJson.length()) {
                        oldPaths.add(oldJson.getString(i))
                    }

                    val newPaths = mutableListOf<String>()
                    if (record.imagePaths.isNotEmpty()) {
                        val newJson = org.json.JSONArray(record.imagePaths)
                        for (i in 0 until newJson.length()) {
                            newPaths.add(newJson.getString(i))
                        }
                    }

                    // 删除不再使用的图片
                    oldPaths.forEach { path ->
                        if (!newPaths.contains(path)) {
                            val imageFile = java.io.File(path)
                            if (imageFile.exists()) {
                                imageFile.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 更新数据库记录
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_RECORD_DATE, record.recordDate)
            put(COLUMN_WEIGHT, record.weight)
            put(COLUMN_SYSTOLIC_PRESSURE, record.systolicPressure)
            put(COLUMN_DIASTOLIC_PRESSURE, record.diastolicPressure)
            put(COLUMN_HEART_RATE, record.heartRate)
            put(COLUMN_BLOOD_SUGAR, record.bloodSugar)
            put(COLUMN_SLEEP_DURATION, record.sleepDuration)
            put(COLUMN_WATER_INTAKE, record.waterIntake)
            put(COLUMN_STEPS, record.steps)
            put(COLUMN_NOTES, record.notes)
            put(COLUMN_IMAGE_PATHS, record.imagePaths)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_HEALTH_RECORD,
            values,
            "$COLUMN_ID = ?",
            arrayOf(record.id.toString())
        )
    }

    /**
     * 删除健康记录
     * @param id 记录ID
     * @return 删除的行数
     */
    fun deleteHealthRecord(id: Long): Int {
        // 先获取记录，删除对应的图片文件
        val record = getHealthRecordById(id)
        record?.let {
            if (it.imagePaths.isNotEmpty()) {
                try {
                    val jsonArray = org.json.JSONArray(it.imagePaths)
                    for (i in 0 until jsonArray.length()) {
                        val imagePath = jsonArray.getString(i)
                        val imageFile = java.io.File(imagePath)
                        if (imageFile.exists()) {
                            imageFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 删除数据库记录
        val db = writableDatabase
        return db.delete(
            TABLE_HEALTH_RECORD,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * 根据ID查询健康记录
     * @param id 记录ID
     * @return 健康记录对象，未找到返回null
     */
    fun getHealthRecordById(id: Long): HealthRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            cursorToHealthRecord(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    /**
     * 根据日期查询健康记录
     * @param date 日期字符串，格式：yyyy-MM-dd
     * @return 健康记录对象，未找到返回null
     */
    fun getHealthRecordByDate(date: String): HealthRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_RECORD_DATE = ?",
            arrayOf(date),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            cursorToHealthRecord(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    /**
     * 查询所有健康记录（按日期降序）
     * @return 健康记录列表
     */
    fun getAllHealthRecords(): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_RECORD_DATE DESC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToHealthRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 查询最近days天的健康记录
     * @param days 最近多少天
     */
    fun getRecentHealthRecords(days: Int): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val cutoffTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_CREATE_TIME >= ?",
            arrayOf(cutoffTime.toString()),
            null,
            null,
            "$COLUMN_RECORD_DATE DESC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToHealthRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 查询日期范围内的健康记录
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @return 健康记录列表
     */
    fun getHealthRecordsByDateRange(startDate: String, endDate: String): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_RECORD_DATE BETWEEN ? AND ?",
            arrayOf(startDate, endDate),
            null,
            null,
            "$COLUMN_RECORD_DATE ASC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToHealthRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 获取最新的一条健康记录
     * @return 最新的健康记录，无记录返回null
     */
    fun getLatestHealthRecord(): HealthRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_RECORD_DATE DESC",
            "1"
        )
        return if (cursor.moveToFirst()) {
            cursorToHealthRecord(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    // ==================== 用药提醒 CRUD 操作 ====================

    /**
     * 插入用药提醒
     * @param reminder 用药提醒对象
     * @return 插入记录的ID，-1表示插入失败
     */
    fun insertMedicineReminder(reminder: MedicineReminder): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MEDICINE_NAME, reminder.medicineName)
            put(COLUMN_DOSAGE, reminder.dosage)
            put(COLUMN_UNIT, reminder.unit)
            put(COLUMN_FREQUENCY, reminder.frequency)
            put(COLUMN_REMIND_TIME1, reminder.remindTime1)
            put(COLUMN_REMIND_TIME2, reminder.remindTime2)
            put(COLUMN_REMIND_TIME3, reminder.remindTime3)
            put(COLUMN_REMIND_TIME4, reminder.remindTime4)
            put(COLUMN_REMIND_TIME5, reminder.remindTime5)
            put(COLUMN_IS_ENABLED, if (reminder.isEnabled) 1 else 0)
            put(COLUMN_START_DATE, reminder.startDate)
            put(COLUMN_END_DATE, reminder.endDate)
            put(COLUMN_BEFORE_AFTER_MEAL, reminder.beforeAfterMeal)
            put(COLUMN_MEDICINE_NOTES, reminder.notes)
            put(COLUMN_CREATE_TIME, reminder.createTime)
            put(COLUMN_UPDATE_TIME, reminder.updateTime)
        }
        return db.insert(TABLE_MEDICINE_REMINDER, null, values)
    }

    /**
     * 更新用药提醒
     * @param reminder 用药提醒对象
     * @return 更新的行数
     */
    fun updateMedicineReminder(reminder: MedicineReminder): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MEDICINE_NAME, reminder.medicineName)
            put(COLUMN_DOSAGE, reminder.dosage)
            put(COLUMN_UNIT, reminder.unit)
            put(COLUMN_FREQUENCY, reminder.frequency)
            put(COLUMN_REMIND_TIME1, reminder.remindTime1)
            put(COLUMN_REMIND_TIME2, reminder.remindTime2)
            put(COLUMN_REMIND_TIME3, reminder.remindTime3)
            put(COLUMN_REMIND_TIME4, reminder.remindTime4)
            put(COLUMN_REMIND_TIME5, reminder.remindTime5)
            put(COLUMN_IS_ENABLED, if (reminder.isEnabled) 1 else 0)
            put(COLUMN_START_DATE, reminder.startDate)
            put(COLUMN_END_DATE, reminder.endDate)
            put(COLUMN_BEFORE_AFTER_MEAL, reminder.beforeAfterMeal)
            put(COLUMN_MEDICINE_NOTES, reminder.notes)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_MEDICINE_REMINDER,
            values,
            "$COLUMN_ID = ?",
            arrayOf(reminder.id.toString())
        )
    }

    /**
     * 删除用药提醒
     * @param id 提醒ID
     * @return 删除的行数
     */
    fun deleteMedicineReminder(id: Long): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_MEDICINE_REMINDER,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * 根据ID查询用药提醒
     * @param id 提醒ID
     * @return 用药提醒对象，未找到返回null
     */
    fun getMedicineReminderById(id: Long): MedicineReminder? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            cursorToMedicineReminder(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    /**
     * 查询所有用药提醒（按创建时间降序）
     * @return 用药提醒列表
     */
    fun getAllMedicineReminders(): List<MedicineReminder> {
        val reminders = mutableListOf<MedicineReminder>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATE_TIME DESC"
        )
        while (cursor.moveToNext()) {
            reminders.add(cursorToMedicineReminder(cursor))
        }
        cursor.close()
        return reminders
    }

    /**
     * 查询启用的用药提醒
     * @return 启用的用药提醒列表
     */
    fun getEnabledMedicineReminders(): List<MedicineReminder> {
        val reminders = mutableListOf<MedicineReminder>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            "$COLUMN_IS_ENABLED = ?",
            arrayOf("1"),
            null,
            null,
            "$COLUMN_REMIND_TIME1 ASC"
        )
        while (cursor.moveToNext()) {
            reminders.add(cursorToMedicineReminder(cursor))
        }
        cursor.close()
        return reminders
    }

    /**
     * 切换用药提醒启用状态
     * @param id 提醒ID
     * @param isEnabled 是否启用
     * @return 更新的行数
     */
    fun toggleMedicineReminder(id: Long, isEnabled: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_ENABLED, if (isEnabled) 1 else 0)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_MEDICINE_REMINDER,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    // ==================== 工具方法 ====================

    /**
     * 将Cursor转换为HealthRecord对象
     */
    private fun cursorToHealthRecord(cursor: android.database.Cursor): HealthRecord {
        return HealthRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            recordDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE)),
            weight = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT)),
            systolicPressure = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYSTOLIC_PRESSURE)),
            diastolicPressure = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DIASTOLIC_PRESSURE)),
            heartRate = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HEART_RATE)),
            bloodSugar = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_BLOOD_SUGAR)),
            sleepDuration = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_SLEEP_DURATION)),
            waterIntake = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WATER_INTAKE)),
            steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
            imagePaths = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATHS)) ?: "",
            createTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)),
            updateTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME))
        )
    }

    /**
     * 将Cursor转换为MedicineReminder对象
     */
    private fun cursorToMedicineReminder(cursor: android.database.Cursor): MedicineReminder {
        return MedicineReminder(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            medicineName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME)),
            dosage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSAGE)) ?: "",
            unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT)) ?: "",
            frequency = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)),
            remindTime1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMIND_TIME1)),
            remindTime2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMIND_TIME2)),
            remindTime3 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMIND_TIME3)),
            remindTime4 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMIND_TIME4)),
            remindTime5 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMIND_TIME5)),
            isEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ENABLED)) == 1,
            startDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE)),
            endDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_DATE)),
            beforeAfterMeal = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BEFORE_AFTER_MEAL)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NOTES)) ?: "",
            createTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)),
            updateTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME))
        )
    }

    /**
     * 获取指定日期的统计信息
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据Map
     */
    fun getHealthStatistics(startDate: String, endDate: String): Map<String, Any> {
        val db = readableDatabase
        val statistics = mutableMapOf<String, Any>()

        // 查询平均体重
        val weightCursor = db.rawQuery(
            "SELECT AVG($COLUMN_WEIGHT) as avg_weight, MIN($COLUMN_WEIGHT) as min_weight, MAX($COLUMN_WEIGHT) as max_weight " +
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_WEIGHT > 0",
            arrayOf(startDate, endDate)
        )
        if (weightCursor.moveToFirst()) {
            statistics["avgWeight"] = weightCursor.getFloat(0)
            statistics["minWeight"] = weightCursor.getFloat(1)
            statistics["maxWeight"] = weightCursor.getFloat(2)
        }
        weightCursor.close()

        // 查询平均血压
        val pressureCursor = db.rawQuery(
            "SELECT AVG($COLUMN_SYSTOLIC_PRESSURE) as avg_sys, AVG($COLUMN_DIASTOLIC_PRESSURE) as avg_dia " +
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_SYSTOLIC_PRESSURE > 0",
            arrayOf(startDate, endDate)
        )
        if (pressureCursor.moveToFirst()) {
            statistics["avgSystolic"] = pressureCursor.getInt(0)
            statistics["avgDiastolic"] = pressureCursor.getInt(1)
        }
        pressureCursor.close()

        // 查询平均血糖
        val sugarCursor = db.rawQuery(
            "SELECT AVG($COLUMN_BLOOD_SUGAR) as avg_sugar " +
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_BLOOD_SUGAR > 0",
            arrayOf(startDate, endDate)
        )
        if (sugarCursor.moveToFirst()) {
            statistics["avgBloodSugar"] = sugarCursor.getFloat(0)
        }
        sugarCursor.close()

        return statistics
    }


    /**
     * 获取用药提醒数量
     * @return 用药提醒总数
     */
    fun getMedicineReminderCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_MEDICINE_REMINDER",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * 获取启用的用药提醒数量
     * @return 启用的用药提醒数量
     */
    fun getEnabledReminderCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_MEDICINE_REMINDER WHERE $COLUMN_IS_ENABLED = 1",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}