package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object TemplateManager {

    private const val PREF_NAME = "medical_record_templates"
    private const val KEY_TEMPLATES = "templates"

    data class MedicalRecordTemplate(
        val id: Long = System.currentTimeMillis(),
        val name: String,
        val hospital: String,
        val doctor: String,
        val checkItems: String,
        val medicines: String
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveTemplate(context: Context, template: MedicalRecordTemplate) {
        val templates = getTemplates(context).toMutableList()
        // 检查同名模板是否存在，存在则覆盖
        val existingIndex = templates.indexOfFirst { it.name == template.name }
        if (existingIndex >= 0) {
            templates[existingIndex] = template
        } else {
            templates.add(template)
        }
        saveTemplatesToPrefs(context, templates)
    }

    fun getTemplates(context: Context): List<MedicalRecordTemplate> {
        val json = getPrefs(context).getString(KEY_TEMPLATES, null) ?: return emptyList()
        val jsonArray: JSONArray
        try {
            jsonArray = JSONArray(json)
        } catch (e: Exception) {
            return emptyList()
        }
        val list = mutableListOf<MedicalRecordTemplate>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                list.add(MedicalRecordTemplate(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    name = obj.getString("name"),
                    hospital = obj.getString("hospital"),
                    doctor = obj.optString("doctor", ""),
                    checkItems = obj.optString("checkItems", ""),
                    medicines = obj.optString("medicines", "")
                ))
            } catch (_: Exception) {}
        }
        return list
    }

    fun deleteTemplate(context: Context, templateId: Long) {
        val templates = getTemplates(context).filter { it.id != templateId }
        saveTemplatesToPrefs(context, templates)
    }

    private fun saveTemplatesToPrefs(context: Context, templates: List<MedicalRecordTemplate>) {
        val jsonArray = JSONArray()
        templates.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("hospital", t.hospital)
            obj.put("doctor", t.doctor)
            obj.put("checkItems", t.checkItems)
            obj.put("medicines", t.medicines)
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TEMPLATES, jsonArray.toString()).apply()
    }
}
