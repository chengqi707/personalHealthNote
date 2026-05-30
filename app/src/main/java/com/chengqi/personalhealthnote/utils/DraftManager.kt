package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.SharedPreferences

object DraftManager {

    private const val PREF_HEALTH = "draft_health_record"
    private const val PREF_MEDICAL = "draft_medical_record"

    // 健康记录草稿keys
    private const val H_DATE = "date"
    private const val H_WEIGHT = "weight"
    private const val H_HEIGHT = "height"
    private const val H_SYSTOLIC = "systolic"
    private const val H_DIASTOLIC = "diastolic"
    private const val H_HEART_RATE = "heart_rate"
    private const val H_BLOOD_SUGAR = "blood_sugar"
    private const val H_SLEEP = "sleep"
    private const val H_WATER = "water"
    private const val H_STEPS = "steps"
    private const val H_NOTES = "notes"
    private const val H_EXISTS = "draft_exists"

    // 就医记录草稿keys
    private const val M_TIME = "time"
    private const val M_HOSPITAL = "hospital"
    private const val M_DOCTOR = "doctor"
    private const val M_SYMPTOMS = "symptoms"
    private const val M_DIAGNOSIS = "diagnosis"
    private const val M_CHECK_ITEMS = "check_items"
    private const val M_MEDICINES = "medicines"
    private const val M_FOLLOW_UP_DATE = "follow_up_date"
    private const val M_EXISTS = "draft_exists"

    private fun getHealthPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_HEALTH, Context.MODE_PRIVATE)
    }

    private fun getMedicalPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_MEDICAL, Context.MODE_PRIVATE)
    }

    // ==================== 健康记录草稿 ====================

    data class HealthRecordDraft(
        val date: String,
        val weight: String,
        val height: String,
        val systolicPressure: String,
        val diastolicPressure: String,
        val heartRate: String,
        val bloodSugar: String,
        val sleepDuration: String,
        val waterIntake: String,
        val steps: String,
        val notes: String
    )

    fun saveHealthRecordDraft(context: Context, draft: HealthRecordDraft) {
        if (draft.weight.isEmpty() && draft.height.isEmpty() && draft.systolicPressure.isEmpty() &&
            draft.diastolicPressure.isEmpty() && draft.heartRate.isEmpty() && draft.bloodSugar.isEmpty() &&
            draft.sleepDuration.isEmpty() && draft.waterIntake.isEmpty() && draft.steps.isEmpty() &&
            draft.notes.isEmpty()
        ) {
            return
        }
        getHealthPrefs(context).edit()
            .putString(H_DATE, draft.date)
            .putString(H_WEIGHT, draft.weight)
            .putString(H_HEIGHT, draft.height)
            .putString(H_SYSTOLIC, draft.systolicPressure)
            .putString(H_DIASTOLIC, draft.diastolicPressure)
            .putString(H_HEART_RATE, draft.heartRate)
            .putString(H_BLOOD_SUGAR, draft.bloodSugar)
            .putString(H_SLEEP, draft.sleepDuration)
            .putString(H_WATER, draft.waterIntake)
            .putString(H_STEPS, draft.steps)
            .putString(H_NOTES, draft.notes)
            .putBoolean(H_EXISTS, true)
            .apply()
    }

    fun getHealthRecordDraft(context: Context): HealthRecordDraft? {
        if (!getHealthPrefs(context).getBoolean(H_EXISTS, false)) return null
        val prefs = getHealthPrefs(context)
        return HealthRecordDraft(
            date = prefs.getString(H_DATE, "") ?: "",
            weight = prefs.getString(H_WEIGHT, "") ?: "",
            height = prefs.getString(H_HEIGHT, "") ?: "",
            systolicPressure = prefs.getString(H_SYSTOLIC, "") ?: "",
            diastolicPressure = prefs.getString(H_DIASTOLIC, "") ?: "",
            heartRate = prefs.getString(H_HEART_RATE, "") ?: "",
            bloodSugar = prefs.getString(H_BLOOD_SUGAR, "") ?: "",
            sleepDuration = prefs.getString(H_SLEEP, "") ?: "",
            waterIntake = prefs.getString(H_WATER, "") ?: "",
            steps = prefs.getString(H_STEPS, "") ?: "",
            notes = prefs.getString(H_NOTES, "") ?: ""
        )
    }

    fun clearHealthRecordDraft(context: Context) {
        getHealthPrefs(context).edit().clear().apply()
    }

    // ==================== 就医记录草稿 ====================

    data class MedicalRecordDraft(
        val medicalTime: String,
        val hospital: String,
        val doctor: String,
        val symptoms: String,
        val diagnosisResult: String,
        val checkItems: String,
        val medicines: String,
        val followUpDate: String = ""
    )

    fun saveMedicalRecordDraft(context: Context, draft: MedicalRecordDraft) {
        if (draft.hospital.isEmpty() && draft.symptoms.isEmpty() && draft.diagnosisResult.isEmpty() &&
            draft.doctor.isEmpty() && draft.checkItems.isEmpty() && draft.medicines.isEmpty() &&
            draft.followUpDate.isEmpty()
        ) {
            return
        }
        getMedicalPrefs(context).edit()
            .putString(M_TIME, draft.medicalTime)
            .putString(M_HOSPITAL, draft.hospital)
            .putString(M_DOCTOR, draft.doctor)
            .putString(M_SYMPTOMS, draft.symptoms)
            .putString(M_DIAGNOSIS, draft.diagnosisResult)
            .putString(M_CHECK_ITEMS, draft.checkItems)
            .putString(M_MEDICINES, draft.medicines)
            .putString(M_FOLLOW_UP_DATE, draft.followUpDate)
            .putBoolean(M_EXISTS, true)
            .apply()
    }

    fun getMedicalRecordDraft(context: Context): MedicalRecordDraft? {
        if (!getMedicalPrefs(context).getBoolean(M_EXISTS, false)) return null
        val prefs = getMedicalPrefs(context)
        return MedicalRecordDraft(
            medicalTime = prefs.getString(M_TIME, "") ?: "",
            hospital = prefs.getString(M_HOSPITAL, "") ?: "",
            doctor = prefs.getString(M_DOCTOR, "") ?: "",
            symptoms = prefs.getString(M_SYMPTOMS, "") ?: "",
            diagnosisResult = prefs.getString(M_DIAGNOSIS, "") ?: "",
            checkItems = prefs.getString(M_CHECK_ITEMS, "") ?: "",
            medicines = prefs.getString(M_MEDICINES, "") ?: "",
            followUpDate = prefs.getString(M_FOLLOW_UP_DATE, "") ?: ""
        )
    }

    fun clearMedicalRecordDraft(context: Context) {
        getMedicalPrefs(context).edit().clear().apply()
    }
}
