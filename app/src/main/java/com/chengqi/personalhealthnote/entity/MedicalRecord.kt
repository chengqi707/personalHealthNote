package com.chengqi.personalhealthnote.entity

/**
 * 就医记录实体类
 * 存储个人就医全流程关键信息
 */
data class MedicalRecord(
    val id: Long = 0,
    val medicalTime: String,          // 就医时间，格式：yyyy-MM-dd HH:mm
    val hospital: String,             // 就诊医院（必填）
    val doctor: String = "",          // 接诊医生（非必填）
    val symptoms: String,             // 症状描述（必填）
    val diagnosisResult: String,      // 就诊结果（必填）
    val checkItems: String = "",      // 检查项目（非必填）
    val medicines: String = "",       // 药品清单（非必填）
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val healthEvaluation: String? = null,  // 健康状态评估结果（本地缓存）
    val lifeSuggestion: String? = null     // 生活建议结果（本地缓存）
) {
    /**
     * 将就医记录转换为固定格式文本，作为云侧模型输入
     */
    fun toStandardFormatText(): String {
        return """
以下是我的一条就医记录信息，请基于这些信息评估我的健康状态，并给出生活各方面的建议（饮食、作息、运动、防护等）：
1. 就医时间：${medicalTime.ifEmpty { "未填写" }}
2. 就诊医院：${hospital.ifEmpty { "未填写" }}
3. 接诊医生：${doctor.ifEmpty { "未填写" }}
4. 症状描述：${symptoms.ifEmpty { "未填写" }}
5. 就诊结果：${diagnosisResult.ifEmpty { "未填写" }}
6. 检查项目：${checkItems.ifEmpty { "未填写" }}
7. 药品清单：${medicines.ifEmpty { "未填写" }}
        """.trimIndent()
    }

    /**
     * 检查记录是否有有效信息（至少有症状描述或就诊结果）
     */
    fun hasValidInfo(): Boolean {
        return symptoms.isNotBlank() || diagnosisResult.isNotBlank()
    }
}
