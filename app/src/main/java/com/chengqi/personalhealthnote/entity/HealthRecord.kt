package com.chengqi.personalhealthnote.entity

/**
 * 健康记录实体类
 * 存储用户每日的健康数据，包括体重、血压、血糖等
 */
data class HealthRecord(
    val id: Long = 0,
    val recordDate: String,      // 记录日期，格式：yyyy-MM-dd
    val weight: Float,           // 体重，单位：kg
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
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取血压显示文本
     */
    fun getBloodPressureText(): String {
        return "$systolicPressure/$diastolicPressure mmHg"
    }

    /**
     * 获取BMI指数（简单计算，仅供参考）
     * 需要身高数据，这里预留方法
     */
    fun calculateBMI(heightInMeters: Float): Float {
        return if (heightInMeters > 0) {
            weight / (heightInMeters * heightInMeters)
        } else {
            0f
        }
    }
}
