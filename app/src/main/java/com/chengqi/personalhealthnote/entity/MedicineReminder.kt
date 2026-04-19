package com.chengqi.personalhealthnote.entity

/**
 * 用药提醒实体类
 * 存储用户的用药计划和提醒设置
 */
data class MedicineReminder(
    val id: Long = 0,
    val medicineName: String,        // 药品名称
    val dosage: String,              // 剂量，如 "1片", "2粒"
    val unit: String,                // 单位，如 "mg", "g", "ml"
    val frequency: Int,              // 每日次数，1-5次
    val remindTime1: String,         // 第一次提醒时间，格式：HH:mm
    val remindTime2: String?,        // 第二次提醒时间
    val remindTime3: String?,        // 第三次提醒时间
    val remindTime4: String?,        // 第四次提醒时间
    val remindTime5: String?,        // 第五次提醒时间
    val isEnabled: Boolean = true,   // 是否启用提醒
    val startDate: String,           // 开始日期，格式：yyyy-MM-dd
    val endDate: String?,            // 结束日期，null表示长期用药
    val beforeAfterMeal: Int,        // 饭前饭后：0-不限，1-饭前，2-饭后，3-饭中
    val notes: String,               // 备注
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取服用时间描述
     */
    fun getMealTimeText(): String {
        return when (beforeAfterMeal) {
            1 -> "饭前服用"
            2 -> "饭后服用"
            3 -> "饭中服用"
            else -> "任意时间"
        }
    }

    /**
     * 获取完整剂量描述
     */
    fun getFullDosage(): String {
        return "$dosage$unit"
    }

    /**
     * 获取所有提醒时间列表
     */
    fun getAllRemindTimes(): List<String> {
        val times = mutableListOf<String>()
        times.add(remindTime1)
        remindTime2?.let { times.add(it) }
        remindTime3?.let { times.add(it) }
        remindTime4?.let { times.add(it) }
        remindTime5?.let { times.add(it) }
        return times
    }

    /**
     * 获取频率描述
     */
    fun getFrequencyText(): String {
        return when (frequency) {
            1 -> "每日一次"
            2 -> "每日两次"
            3 -> "每日三次"
            4 -> "每日四次"
            5 -> "每日五次"
            else -> "每日$frequency 次"
        }
    }
}
