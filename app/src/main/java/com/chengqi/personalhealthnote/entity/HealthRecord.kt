package com.chengqi.personalhealthnote.entity

data class HealthRecord(
    val id: Long = 0,
    val recordDate: String,      // 记录日期，格式：yyyy-MM-dd
    val weight: Float,           // 体重，单位：kg
    val height: Float = 0f,      // 身高，单位：cm
    val systolicPressure: Int,   // 收缩压（高压）
    val diastolicPressure: Int,  // 舒张压（低压）
    val heartRate: Int,          // 心率，单位：次/分钟
    val bloodSugar: Float,       // 血糖，单位：mmol/L
    val sleepDuration: Float,    // 睡眠时长，单位：小时
    val waterIntake: Int,        // 饮水量，单位：ml
    val steps: Int,              // 步数
    val notes: String,           // 备注
    val imagePaths: String = "", // 就诊/检查单据图片路径，JSON数组格式
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val isSync: Int = 0,         // 是否已同步：0未同步 1已同步
    val deleteFlag: Int = 0,     // 是否已删除：0未删除 1已删除
    val healthEvaluation: String? = null,  // AI健康评估结果（缓存）
    val lifeSuggestion: String? = null     // AI生活建议结果（缓存）
) {
    fun calculateBMI(): Float {
        return if (weight > 0 && height > 0) {
            val heightM = height / 100f
            weight / (heightM * heightM)
        } else {
            0f
        }
    }
}
