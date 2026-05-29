package com.chengqi.personalhealthnote.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicalRecord
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
        const val DATABASE_NAME = "health_note.db"
        private const val DATABASE_VERSION = 7

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
        private const val COLUMN_IS_SYNC = "is_sync" // 是否已同步：0未同步 1已同步
        private const val COLUMN_DELETE_FLAG = "delete_flag" // 是否已删除：0未删除 1已删除
        private const val COLUMN_HEIGHT = "height" // 身高，单位：cm
        private const val COLUMN_HEALTH_EVALUATION = "health_evaluation" // AI健康评估结果
        private const val COLUMN_LIFE_SUGGESTION = "life_suggestion" // AI生活建议结果

        // 用药提醒表
        private const val TABLE_MEDICINE_REMINDER = "medicine_reminder"

        // 就医记录表
        private const val TABLE_MEDICAL_RECORD = "medical_record"
        private const val COLUMN_MR_MEDICAL_TIME = "medical_time"
        private const val COLUMN_MR_HOSPITAL = "hospital"
        private const val COLUMN_MR_DOCTOR = "doctor"
        private const val COLUMN_MR_SYMPTOMS = "symptoms"
        private const val COLUMN_MR_DIAGNOSIS_RESULT = "diagnosis_result"
        private const val COLUMN_MR_CHECK_ITEMS = "check_items"
        private const val COLUMN_MR_MEDICINES = "medicines"
        private const val COLUMN_MR_HEALTH_EVALUATION = "health_evaluation"
        private const val COLUMN_MR_LIFE_SUGGESTION = "life_suggestion"
        private const val COLUMN_MR_IMAGE_PATHS = "image_paths"
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
        // 同步相关字段，和健康记录表共用
        // private const val COLUMN_IS_SYNC = "is_sync"
        // private const val COLUMN_DELETE_FLAG = "delete_flag"
        private const val COLUMN_CALENDAR_EVENT_IDS = "calendar_event_ids"

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
                $COLUMN_UPDATE_TIME INTEGER DEFAULT 0,
                $COLUMN_IS_SYNC INTEGER DEFAULT 0,
                $COLUMN_DELETE_FLAG INTEGER DEFAULT 0
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
                $COLUMN_UPDATE_TIME INTEGER DEFAULT 0,
                $COLUMN_IS_SYNC INTEGER DEFAULT 0,
                $COLUMN_DELETE_FLAG INTEGER DEFAULT 0,
                $COLUMN_CALENDAR_EVENT_IDS TEXT DEFAULT ''
            )
        """

        // 创建就医记录表的SQL
        private const val CREATE_MEDICAL_RECORD_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_MEDICAL_RECORD (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MR_MEDICAL_TIME TEXT NOT NULL,
                $COLUMN_MR_HOSPITAL TEXT NOT NULL,
                $COLUMN_MR_DOCTOR TEXT DEFAULT '',
                $COLUMN_MR_SYMPTOMS TEXT NOT NULL,
                $COLUMN_MR_DIAGNOSIS_RESULT TEXT NOT NULL,
                $COLUMN_MR_CHECK_ITEMS TEXT DEFAULT '',
                $COLUMN_MR_MEDICINES TEXT DEFAULT '',
                $COLUMN_CREATE_TIME INTEGER DEFAULT 0,
                $COLUMN_UPDATE_TIME INTEGER DEFAULT 0,
                $COLUMN_MR_HEALTH_EVALUATION TEXT,
                $COLUMN_MR_LIFE_SUGGESTION TEXT,
                $COLUMN_MR_IMAGE_PATHS TEXT DEFAULT ''
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_HEALTH_RECORD_TABLE)
        db?.execSQL(CREATE_MEDICINE_REMINDER_TABLE)
        db?.execSQL(CREATE_MEDICAL_RECORD_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 增量升级，保留用户数据
        if (oldVersion < 2) {
            // 版本1升级到版本2，添加图片路径字段
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_IMAGE_PATHS TEXT DEFAULT ''")
        }
        if (oldVersion < 3) {
            // 版本2升级到版本3，添加同步相关字段
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_IS_SYNC INTEGER DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_DELETE_FLAG INTEGER DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_MEDICINE_REMINDER ADD COLUMN $COLUMN_IS_SYNC INTEGER DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_MEDICINE_REMINDER ADD COLUMN $COLUMN_DELETE_FLAG INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) {
            // 版本3升级到版本4，新增就医记录表
            db?.execSQL(CREATE_MEDICAL_RECORD_TABLE)
        }
        if (oldVersion < 5) {
            // 版本4升级到版本5，就医记录表新增图片路径字段
            db?.execSQL("ALTER TABLE $TABLE_MEDICAL_RECORD ADD COLUMN $COLUMN_MR_IMAGE_PATHS TEXT DEFAULT ''")
        }
        if (oldVersion < 6) {
            // 版本5升级到版本6，用药提醒表新增日历事件ID字段
            db?.execSQL("ALTER TABLE $TABLE_MEDICINE_REMINDER ADD COLUMN $COLUMN_CALENDAR_EVENT_IDS TEXT DEFAULT ''")
        }
        if (oldVersion < 7) {
            // 版本6升级到版本7，健康记录表新增身高、AI评估缓存字段
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_HEIGHT REAL DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_HEALTH_EVALUATION TEXT")
            db?.execSQL("ALTER TABLE $TABLE_HEALTH_RECORD ADD COLUMN $COLUMN_LIFE_SUGGESTION TEXT")
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
            put(COLUMN_HEIGHT, record.height)
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
            put(COLUMN_IS_SYNC, record.isSync)
            put(COLUMN_DELETE_FLAG, record.deleteFlag)
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
            put(COLUMN_HEIGHT, record.height)
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
            put(COLUMN_IS_SYNC, 0) // 修改后标记为未同步
        }
        return db.update(
            TABLE_HEALTH_RECORD,
            values,
            "$COLUMN_ID = ?",
            arrayOf(record.id.toString())
        )
    }

    /**
     * 删除健康记录（逻辑删除）
     * @param id 记录ID
     * @return 更新的行数
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

        // 逻辑删除，标记为已删除，未同步
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DELETE_FLAG, 1)
            put(COLUMN_IS_SYNC, 0)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_HEALTH_RECORD,
            values,
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
    fun getAllHealthRecords(sortOrder: String = "DESC"): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_DELETE_FLAG = 0",
            null,
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
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
            "$COLUMN_CREATE_TIME >= ? AND $COLUMN_DELETE_FLAG = 0",
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
            "$COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_DELETE_FLAG = 0",
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
            "$COLUMN_DELETE_FLAG = 0",
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

    /**
     * 更新健康记录的AI评估缓存
     */
    fun updateHealthRecordEvaluation(id: Long, healthEvaluation: String, lifeSuggestion: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HEALTH_EVALUATION, healthEvaluation)
            put(COLUMN_LIFE_SUGGESTION, lifeSuggestion)
        }
        return db.update(TABLE_HEALTH_RECORD, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * 搜索健康记录
     * @param keyword 搜索关键词
     * @param sortOrder 排序方式，DESC或ASC
     * @return 匹配的健康记录列表
     */
    fun searchHealthRecords(keyword: String, sortOrder: String = "DESC"): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val likeKeyword = "%$keyword%"
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_DELETE_FLAG = 0 AND ($COLUMN_RECORD_DATE LIKE ? OR $COLUMN_NOTES LIKE ?)",
            arrayOf(likeKeyword, likeKeyword),
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToHealthRecord(cursor))
        }
        cursor.close()
        return records
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
            put(COLUMN_IS_SYNC, reminder.isSync)
            put(COLUMN_DELETE_FLAG, reminder.deleteFlag)
            put(COLUMN_CALENDAR_EVENT_IDS, reminder.calendarEventIds)
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
            put(COLUMN_IS_SYNC, 0) // 修改后标记为未同步
            put(COLUMN_CALENDAR_EVENT_IDS, reminder.calendarEventIds)
        }
        return db.update(
            TABLE_MEDICINE_REMINDER,
            values,
            "$COLUMN_ID = ?",
            arrayOf(reminder.id.toString())
        )
    }

    /**
     * 删除用药提醒（逻辑删除）
     * @param id 提醒ID
     * @return 更新的行数
     */
    fun deleteMedicineReminder(id: Long): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DELETE_FLAG, 1)
            put(COLUMN_IS_SYNC, 0)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_MEDICINE_REMINDER,
            values,
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
    fun getAllMedicineReminders(sortOrder: String = "DESC"): List<MedicineReminder> {
        val reminders = mutableListOf<MedicineReminder>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            "$COLUMN_DELETE_FLAG = 0",
            null,
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
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
            "$COLUMN_IS_ENABLED = ? AND $COLUMN_DELETE_FLAG = 0",
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
            put(COLUMN_IS_SYNC, 0) // 修改后标记为未同步
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
            height = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT)),
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
            updateTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME)),
            isSync = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNC)),
            deleteFlag = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DELETE_FLAG)),
            healthEvaluation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HEALTH_EVALUATION)),
            lifeSuggestion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIFE_SUGGESTION))
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
            updateTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME)),
            isSync = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNC)),
            deleteFlag = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DELETE_FLAG)),
            calendarEventIds = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CALENDAR_EVENT_IDS)) ?: ""
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
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_WEIGHT > 0 AND $COLUMN_DELETE_FLAG = 0",
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
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_SYSTOLIC_PRESSURE > 0 AND $COLUMN_DELETE_FLAG = 0",
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
            "FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_RECORD_DATE BETWEEN ? AND ? AND $COLUMN_BLOOD_SUGAR > 0 AND $COLUMN_DELETE_FLAG = 0",
            arrayOf(startDate, endDate)
        )
        if (sugarCursor.moveToFirst()) {
            statistics["avgBloodSugar"] = sugarCursor.getFloat(0)
        }
        sugarCursor.close()

        return statistics
    }

    /**
     * 获取体重趋势数据
     * @param days 最近天数
     * @return 日期→体重列表
     */
    fun getWeightTrend(days: Int): List<Pair<String, Float>> {
        val result = mutableListOf<Pair<String, Float>>()
        val db = readableDatabase
        val cutoffDate = getCutoffDate(days)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_RECORD_DATE, $COLUMN_WEIGHT FROM $TABLE_HEALTH_RECORD " +
            "WHERE $COLUMN_RECORD_DATE >= ? AND $COLUMN_WEIGHT > 0 AND $COLUMN_DELETE_FLAG = 0 " +
            "ORDER BY $COLUMN_RECORD_DATE ASC",
            arrayOf(cutoffDate)
        )
        while (cursor.moveToNext()) {
            result.add(Pair(cursor.getString(0), cursor.getFloat(1)))
        }
        cursor.close()
        return result
    }

    /**
     * 获取血压趋势数据
     * @param days 最近天数
     * @return 日期→(收缩压, 舒张压)列表
     */
    fun getBloodPressureTrend(days: Int): List<Triple<String, Int, Int>> {
        val result = mutableListOf<Triple<String, Int, Int>>()
        val db = readableDatabase
        val cutoffDate = getCutoffDate(days)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_RECORD_DATE, $COLUMN_SYSTOLIC_PRESSURE, $COLUMN_DIASTOLIC_PRESSURE FROM $TABLE_HEALTH_RECORD " +
            "WHERE $COLUMN_RECORD_DATE >= ? AND $COLUMN_SYSTOLIC_PRESSURE > 0 AND $COLUMN_DELETE_FLAG = 0 " +
            "ORDER BY $COLUMN_RECORD_DATE ASC",
            arrayOf(cutoffDate)
        )
        while (cursor.moveToNext()) {
            result.add(Triple(cursor.getString(0), cursor.getInt(1), cursor.getInt(2)))
        }
        cursor.close()
        return result
    }

    /**
     * 获取血糖趋势数据
     * @param days 最近天数
     * @return 日期→血糖列表
     */
    fun getBloodSugarTrend(days: Int): List<Pair<String, Float>> {
        val result = mutableListOf<Pair<String, Float>>()
        val db = readableDatabase
        val cutoffDate = getCutoffDate(days)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_RECORD_DATE, $COLUMN_BLOOD_SUGAR FROM $TABLE_HEALTH_RECORD " +
            "WHERE $COLUMN_RECORD_DATE >= ? AND $COLUMN_BLOOD_SUGAR > 0 AND $COLUMN_DELETE_FLAG = 0 " +
            "ORDER BY $COLUMN_RECORD_DATE ASC",
            arrayOf(cutoffDate)
        )
        while (cursor.moveToNext()) {
            result.add(Pair(cursor.getString(0), cursor.getFloat(1)))
        }
        cursor.close()
        return result
    }

    /**
     * 获取BMI趋势数据
     * @param days 最近天数
     * @return 日期→BMI列表
     */
    fun getBmiTrend(days: Int): List<Pair<String, Float>> {
        val result = mutableListOf<Pair<String, Float>>()
        val db = readableDatabase
        val cutoffDate = getCutoffDate(days)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_RECORD_DATE, $COLUMN_WEIGHT, $COLUMN_HEIGHT FROM $TABLE_HEALTH_RECORD " +
            "WHERE $COLUMN_RECORD_DATE >= ? AND $COLUMN_WEIGHT > 0 AND $COLUMN_HEIGHT > 0 AND $COLUMN_DELETE_FLAG = 0 " +
            "ORDER BY $COLUMN_RECORD_DATE ASC",
            arrayOf(cutoffDate)
        )
        while (cursor.moveToNext()) {
            val date = cursor.getString(0)
            val weight = cursor.getFloat(1)
            val height = cursor.getFloat(2)
            val heightM = height / 100f
            if (heightM > 0) {
                result.add(Pair(date, weight / (heightM * heightM)))
            }
        }
        cursor.close()
        return result
    }

    /**
     * 获取常见症状Top N
     * @param limit 取前N个
     * @return 症状→次数列表
     */
    fun getTopSymptoms(limit: Int = 5): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_MR_SYMPTOMS, COUNT(*) as cnt FROM $TABLE_MEDICAL_RECORD " +
            "GROUP BY $COLUMN_MR_SYMPTOMS ORDER BY cnt DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        while (cursor.moveToNext()) {
            result.add(Pair(cursor.getString(0), cursor.getInt(1)))
        }
        cursor.close()
        return result
    }

    /**
     * 获取就医记录总数
     */
    fun getMedicalRecordCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_MEDICAL_RECORD",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * 获取健康记录总数
     */
    fun getHealthRecordCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_HEALTH_RECORD WHERE $COLUMN_DELETE_FLAG = 0",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * 获取不同药品数
     */
    fun getMedicineTypeCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT $COLUMN_MEDICINE_NAME) FROM $TABLE_MEDICINE_REMINDER WHERE $COLUMN_DELETE_FLAG = 0",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * 计算截止日期字符串
     */
    private fun getCutoffDate(days: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(cal.time)
    }


    /**
     * 获取用药提醒数量
     * @return 用药提醒总数
     */
    fun getMedicineReminderCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_MEDICINE_REMINDER WHERE $COLUMN_DELETE_FLAG = 0",
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
            "SELECT COUNT(*) FROM $TABLE_MEDICINE_REMINDER WHERE $COLUMN_IS_ENABLED = 1 AND $COLUMN_DELETE_FLAG = 0",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * 搜索用药提醒
     * @param keyword 搜索关键词
     * @param sortOrder 排序方式，DESC或ASC
     * @return 匹配的用药提醒列表
     */
    fun searchMedicineReminders(keyword: String, sortOrder: String = "DESC"): List<MedicineReminder> {
        val reminders = mutableListOf<MedicineReminder>()
        val db = readableDatabase
        val likeKeyword = "%$keyword%"
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            "$COLUMN_DELETE_FLAG = 0 AND ($COLUMN_MEDICINE_NAME LIKE ? OR $COLUMN_DOSAGE LIKE ? OR $COLUMN_MEDICINE_NOTES LIKE ?)",
            arrayOf(likeKeyword, likeKeyword, likeKeyword),
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
        )
        while (cursor.moveToNext()) {
            reminders.add(cursorToMedicineReminder(cursor))
        }
        cursor.close()
        return reminders
    }

    // ==================== 同步相关方法 ====================

    // ==================== 就医记录 CRUD 操作 ====================

    /**
     * 插入就医记录
     * @param record 就医记录对象
     * @return 插入记录的ID，-1表示插入失败
     */
    fun insertMedicalRecord(record: MedicalRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MR_MEDICAL_TIME, record.medicalTime)
            put(COLUMN_MR_HOSPITAL, record.hospital)
            put(COLUMN_MR_DOCTOR, record.doctor)
            put(COLUMN_MR_SYMPTOMS, record.symptoms)
            put(COLUMN_MR_DIAGNOSIS_RESULT, record.diagnosisResult)
            put(COLUMN_MR_CHECK_ITEMS, record.checkItems)
            put(COLUMN_MR_MEDICINES, record.medicines)
            put(COLUMN_MR_IMAGE_PATHS, record.imagePaths)
            put(COLUMN_CREATE_TIME, record.createTime)
            put(COLUMN_UPDATE_TIME, record.updateTime)
            put(COLUMN_MR_HEALTH_EVALUATION, record.healthEvaluation)
            put(COLUMN_MR_LIFE_SUGGESTION, record.lifeSuggestion)
        }
        return db.insert(TABLE_MEDICAL_RECORD, null, values)
    }

    /**
     * 更新就医记录
     * @param record 就医记录对象
     * @return 更新的行数
     */
    fun updateMedicalRecord(record: MedicalRecord): Int {
        // 获取原有记录，对比图片，删除不再使用的图片
        val oldRecord = getMedicalRecordById(record.id)
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

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MR_MEDICAL_TIME, record.medicalTime)
            put(COLUMN_MR_HOSPITAL, record.hospital)
            put(COLUMN_MR_DOCTOR, record.doctor)
            put(COLUMN_MR_SYMPTOMS, record.symptoms)
            put(COLUMN_MR_DIAGNOSIS_RESULT, record.diagnosisResult)
            put(COLUMN_MR_CHECK_ITEMS, record.checkItems)
            put(COLUMN_MR_MEDICINES, record.medicines)
            put(COLUMN_MR_IMAGE_PATHS, record.imagePaths)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
            put(COLUMN_MR_HEALTH_EVALUATION, null as String?)
            put(COLUMN_MR_LIFE_SUGGESTION, null as String?)
        }
        return db.update(
            TABLE_MEDICAL_RECORD,
            values,
            "$COLUMN_ID = ?",
            arrayOf(record.id.toString())
        )
    }

    /**
     * 删除就医记录
     * @param id 记录ID
     * @return 删除的行数
     */
    fun deleteMedicalRecord(id: Int): Int {
        // 先获取记录，删除关联的图片文件
        val record = getMedicalRecordById(id.toLong())
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

        val db = writableDatabase
        return db.delete(
            TABLE_MEDICAL_RECORD,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * 根据ID查询就医记录
     * @param id 记录ID
     * @return 就医记录对象，未找到返回null
     */
    fun getMedicalRecordById(id: Long): MedicalRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICAL_RECORD,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            cursorToMedicalRecord(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    /**
     * 查询所有就医记录（按创建时间倒序）
     * @return 就医记录列表
     */
    fun getAllMedicalRecords(sortOrder: String = "DESC"): List<MedicalRecord> {
        val records = mutableListOf<MedicalRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICAL_RECORD,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToMedicalRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 按关键词搜索就医记录（匹配医院、症状、诊断结果、医生、检查项目、药品）
     * @param keyword 搜索关键词
     * @return 匹配的就医记录列表
     */
    fun searchMedicalRecords(keyword: String, sortOrder: String = "DESC"): List<MedicalRecord> {
        val records = mutableListOf<MedicalRecord>()
        val db = readableDatabase
        val likeKeyword = "%$keyword%"
        val cursor = db.query(
            TABLE_MEDICAL_RECORD,
            null,
            "$COLUMN_MR_HOSPITAL LIKE ? OR $COLUMN_MR_SYMPTOMS LIKE ? OR $COLUMN_MR_DIAGNOSIS_RESULT LIKE ? OR $COLUMN_MR_DOCTOR LIKE ? OR $COLUMN_MR_CHECK_ITEMS LIKE ? OR $COLUMN_MR_MEDICINES LIKE ?",
            arrayOf(likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword),
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToMedicalRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 按时间范围查询就医记录
     * @param startTime 开始时间，格式：yyyy-MM-dd HH:mm
     * @param endTime 结束时间，格式：yyyy-MM-dd HH:mm
     * @return 就医记录列表
     */
    fun getMedicalRecordsByTimeRange(startTime: String, endTime: String, sortOrder: String = "DESC"): List<MedicalRecord> {
        val records = mutableListOf<MedicalRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICAL_RECORD,
            null,
            "$COLUMN_MR_MEDICAL_TIME >= ? AND $COLUMN_MR_MEDICAL_TIME <= ?",
            arrayOf(startTime, endTime),
            null,
            null,
            "$COLUMN_CREATE_TIME $sortOrder"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToMedicalRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 更新就医记录的评估缓存
     * @param id 记录ID
     * @param healthEvaluation 健康评估结果
     * @param lifeSuggestion 生活建议
     * @return 更新的行数
     */
    fun updateMedicalRecordEvaluation(id: Long, healthEvaluation: String, lifeSuggestion: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MR_HEALTH_EVALUATION, healthEvaluation)
            put(COLUMN_MR_LIFE_SUGGESTION, lifeSuggestion)
            put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
        }
        return db.update(
            TABLE_MEDICAL_RECORD,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * 将Cursor转换为MedicalRecord对象
     */
    private fun cursorToMedicalRecord(cursor: android.database.Cursor): MedicalRecord {
        return MedicalRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            medicalTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_MEDICAL_TIME)),
            hospital = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_HOSPITAL)),
            doctor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_DOCTOR)) ?: "",
            symptoms = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_SYMPTOMS)),
            diagnosisResult = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_DIAGNOSIS_RESULT)),
            checkItems = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_CHECK_ITEMS)) ?: "",
            medicines = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_MEDICINES)) ?: "",
            imagePaths = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_IMAGE_PATHS)) ?: "",
            createTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)),
            updateTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME)),
            healthEvaluation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_HEALTH_EVALUATION)),
            lifeSuggestion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MR_LIFE_SUGGESTION))
        )
    }

    /**
     * 获取所有未同步的健康记录（包括已删除的）
     * @return 未同步的健康记录列表
     */
    fun getUnsyncedHealthRecords(): List<HealthRecord> {
        val records = mutableListOf<HealthRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HEALTH_RECORD,
            null,
            "$COLUMN_IS_SYNC = 0",
            null,
            null,
            null,
            "$COLUMN_UPDATE_TIME ASC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToHealthRecord(cursor))
        }
        cursor.close()
        return records
    }

    /**
     * 获取所有未同步的用药提醒（包括已删除的）
     * @return 未同步的用药提醒列表
     */
    fun getUnsyncedMedicineReminders(): List<MedicineReminder> {
        val reminders = mutableListOf<MedicineReminder>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDICINE_REMINDER,
            null,
            "$COLUMN_IS_SYNC = 0",
            null,
            null,
            null,
            "$COLUMN_UPDATE_TIME ASC"
        )
        while (cursor.moveToNext()) {
            reminders.add(cursorToMedicineReminder(cursor))
        }
        cursor.close()
        return reminders
    }

    /**
     * 批量标记健康记录为已同步
     * @param recordIds 记录ID列表
     * @return 更新的行数
     */
    fun markHealthRecordsSynced(recordIds: List<Long>): Int {
        if (recordIds.isEmpty()) return 0
        val db = writableDatabase
        val placeholders = recordIds.joinToString(",") { "?" }
        val args = recordIds.map { it.toString() }.toTypedArray()
        val values = ContentValues().apply {
            put(COLUMN_IS_SYNC, 1)
        }
        return db.update(
            TABLE_HEALTH_RECORD,
            values,
            "$COLUMN_ID IN ($placeholders)",
            args
        )
    }

    /**
     * 批量标记用药提醒为已同步
     * @param reminderIds 提醒ID列表
     * @return 更新的行数
     */
    fun markMedicineRemindersSynced(reminderIds: List<Long>): Int {
        if (reminderIds.isEmpty()) return 0
        val db = writableDatabase
        val placeholders = reminderIds.joinToString(",") { "?" }
        val args = reminderIds.map { it.toString() }.toTypedArray()
        val values = ContentValues().apply {
            put(COLUMN_IS_SYNC, 1)
        }
        return db.update(
            TABLE_MEDICINE_REMINDER,
            values,
            "$COLUMN_ID IN ($placeholders)",
            args
        )
    }

    /**
     * 同步服务器拉取的健康记录到本地
     * @param records 服务器返回的健康记录列表
     * @return 同步成功的数量
     */
    fun syncHealthRecordsFromServer(records: List<HealthRecord>): Int {
        if (records.isEmpty()) return 0
        val db = writableDatabase
        var successCount = 0
        db.beginTransaction()
        try {
            for (record in records) {
                // 检查本地是否已有该记录（服务器返回的ID是服务器的ID？或者需要用record_date作为唯一键？
                // 这里假设用record_date作为唯一键，因为同一日期只能有一条健康记录
                val existingRecord = getHealthRecordByDate(record.recordDate)
                if (existingRecord != null) {
                    // 比较更新时间，服务器的更新时间晚才更新
                    if (record.updateTime > existingRecord.updateTime) {
                        // 如果服务器标记为删除，本地也删除
                        if (record.deleteFlag == 1) {
                            deleteHealthRecord(existingRecord.id)
                        } else {
                            // 更新记录，保留本地ID，其他字段用服务器的
                            val values = ContentValues().apply {
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
                                put(COLUMN_UPDATE_TIME, record.updateTime)
                                put(COLUMN_IS_SYNC, 1) // 从服务器同步的，标记为已同步
                                put(COLUMN_DELETE_FLAG, record.deleteFlag)
                            }
                            db.update(
                                TABLE_HEALTH_RECORD,
                                values,
                                "$COLUMN_ID = ?",
                                arrayOf(existingRecord.id.toString())
                            )
                        }
                        successCount++
                    }
                } else {
                    // 本地没有，插入新记录
                    if (record.deleteFlag != 1) { // 未删除的才插入
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
                            put(COLUMN_IS_SYNC, 1) // 从服务器同步的，标记为已同步
                            put(COLUMN_DELETE_FLAG, record.deleteFlag)
                        }
                        db.insert(TABLE_HEALTH_RECORD, null, values)
                        successCount++
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return successCount
    }

    /**
     * 同步服务器拉取的用药提醒到本地
     * @param reminders 服务器返回的用药提醒列表
     * @return 同步成功的数量
     */
    fun syncMedicineRemindersFromServer(reminders: List<MedicineReminder>): Int {
        if (reminders.isEmpty()) return 0
        val db = writableDatabase
        var successCount = 0
        db.beginTransaction()
        try {
            for (reminder in reminders) {
                // 用药提醒没有唯一的业务键，所以用ID匹配？或者用medicineName + remindTime1？
                // 这里简化处理，直接比较更新时间，如果服务器的更新时间晚，就更新或者插入
                val existingReminder = getMedicineReminderById(reminder.id)
                if (existingReminder != null) {
                    if (reminder.updateTime > existingReminder.updateTime) {
                        if (reminder.deleteFlag == 1) {
                            deleteMedicineReminder(existingReminder.id)
                        } else {
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
                                put(COLUMN_UPDATE_TIME, reminder.updateTime)
                                put(COLUMN_IS_SYNC, 1)
                                put(COLUMN_DELETE_FLAG, reminder.deleteFlag)
                            }
                            db.update(
                                TABLE_MEDICINE_REMINDER,
                                values,
                                "$COLUMN_ID = ?",
                                arrayOf(existingReminder.id.toString())
                            )
                        }
                        successCount++
                    }
                } else {
                    if (reminder.deleteFlag != 1) {
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
                            put(COLUMN_IS_SYNC, 1)
                            put(COLUMN_DELETE_FLAG, reminder.deleteFlag)
                        }
                        db.insert(TABLE_MEDICINE_REMINDER, null, values)
                        successCount++
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return successCount
    }
}