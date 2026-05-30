package com.chengqi.personalhealthnote.entity

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

data class UserProfile(
    val id: Long = 1,
    val gender: String = "",           // 男/女/不愿透露
    val birthDate: String = "",        // yyyy-MM-dd
    val height: Float = 0f,            // cm
    val weight: Float = 0f,            // kg（基线体重）
    val allergies: String = "",        // 过敏史
    val familyHistory: String = "",    // 家族病史
    val chronicDiseases: String = "",  // 慢性病
    val updateTime: Long = System.currentTimeMillis(),
    val isSync: Int = 0
) {
    fun getAge(): Int {
        if (birthDate.isEmpty()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birth = sdf.parse(birthDate) ?: return 0
            val birthCal = Calendar.getInstance().apply { time = birth }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) { 0 }
    }

    fun getGenderText(): String = when (gender) {
        "male" -> "男"
        "female" -> "女"
        "other" -> "不愿透露"
        else -> ""
    }

    fun isEmpty(): Boolean {
        return gender.isEmpty() && birthDate.isEmpty() && height == 0f && weight == 0f
                && allergies.isEmpty() && familyHistory.isEmpty() && chronicDiseases.isEmpty()
    }

    fun toPromptText(): String {
        if (isEmpty()) return ""
        val sb = StringBuilder("[用户基础信息]\n")
        if (gender.isNotEmpty()) sb.append("性别: ${getGenderText()}")
        val age = getAge()
        if (age > 0) sb.append(" | 年龄: ${age}岁")
        if (height > 0) sb.append(" | 身高: ${height}cm")
        if (weight > 0) sb.append(" | 基线体重: ${weight}kg")
        sb.append("\n")
        if (allergies.isNotEmpty()) sb.append("过敏史: $allergies\n")
        if (chronicDiseases.isNotEmpty()) sb.append("慢性病: $chronicDiseases\n")
        if (familyHistory.isNotEmpty()) sb.append("家族病史: $familyHistory\n")
        return sb.toString()
    }
}
