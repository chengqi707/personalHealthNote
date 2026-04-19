package com.chengqi.personalhealthnote.network
import com.chengqi.personalhealthnote.BuildConfig
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.chengqi.personalhealthnote.utils.TokenManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
/**
 * 统一网络请求服务
 * 封装所有服务端接口调用
 */
object ApiService {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    /**
     * 用户注册
     */
    fun register(username: String, password: String, callback: (Boolean, String, Long?, String?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/user/register")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null, null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val userId = data.getLong("userId")
                            val token = data.getString("token")
                            callback(true, message, userId, token)
                        } else {
                            callback(false, message, null, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null, null)
                    }
                }
            }
        })
    }
    /**
     * 用户登录
     */
    fun login(username: String, password: String, callback: (Boolean, String, Long?, String?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/user/login")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null, null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val userId = data.getLong("userId")
                            val token = data.getString("token")
                            callback(true, message, userId, token)
                        } else {
                            callback(false, message, null, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null, null)
                    }
                }
            }
        })
    }
    /**
     * 拉取健康记录增量数据
     */
    fun pullHealthRecords(context: android.content.Context, lastSyncTime: Long, callback: (Boolean, String, List<HealthRecord>?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("lastSyncTime", lastSyncTime)
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/health/pull")
            .addHeader("Authorization", "Bearer ${TokenManager.getToken(context)}")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val records = parseHealthRecords(data.getJSONArray("records"))
                            callback(true, message, records)
                        } else {
                            callback(false, message, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null)
                    }
                }
            }
        })
    }
    /**
     * 上传健康记录增量数据
     */
    fun pushHealthRecords(context: android.content.Context, records: List<HealthRecord>, callback: (Boolean, String, Int?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("records", JSONArray().apply {
                records.forEach { record ->
                    put(JSONObject().apply {
                        put("id", record.id)
                        put("recordDate", record.recordDate)
                        put("weight", record.weight)
                        put("systolicPressure", record.systolicPressure)
                        put("diastolicPressure", record.diastolicPressure)
                        put("heartRate", record.heartRate)
                        put("bloodSugar", record.bloodSugar)
                        put("sleepDuration", record.sleepDuration)
                        put("waterIntake", record.waterIntake)
                        put("steps", record.steps)
                        put("notes", record.notes)
                        put("imagePaths", record.imagePaths)
                        put("updateTime", record.updateTime)
                        put("deleteFlag", record.deleteFlag)
                    })
                }
            })
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/health/push")
            .addHeader("Authorization", "Bearer ${TokenManager.getToken(context)}")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val successCount = data.getInt("successCount")
                            callback(true, message, successCount)
                        } else {
                            callback(false, message, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null)
                    }
                }
            }
        })
    }
    /**
     * 拉取用药提醒增量数据
     */
    fun pullMedicineReminders(context: android.content.Context, lastSyncTime: Long, callback: (Boolean, String, List<MedicineReminder>?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("lastSyncTime", lastSyncTime)
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/medicine/pull")
            .addHeader("Authorization", "Bearer ${TokenManager.getToken(context)}")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val reminders = parseMedicineReminders(data.getJSONArray("reminders"))
                            callback(true, message, reminders)
                        } else {
                            callback(false, message, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null)
                    }
                }
            }
        })
    }
    /**
     * 上传用药提醒增量数据
     */
    fun pushMedicineReminders(context: android.content.Context, reminders: List<MedicineReminder>, callback: (Boolean, String, Int?) -> Unit) {
        val requestBody = JSONObject().apply {
            put("reminders", JSONArray().apply {
                reminders.forEach { reminder ->
                    put(JSONObject().apply {
                        put("id", reminder.id)
                        put("medicineName", reminder.medicineName)
                        put("dosage", reminder.dosage)
                        put("unit", reminder.unit)
                        put("frequency", reminder.frequency)
                        put("remindTime1", reminder.remindTime1)
                        put("remindTime2", reminder.remindTime2)
                        put("remindTime3", reminder.remindTime3)
                        put("remindTime4", reminder.remindTime4)
                        put("remindTime5", reminder.remindTime5)
                        put("isEnabled", reminder.isEnabled)
                        put("startDate", reminder.startDate)
                        put("endDate", reminder.endDate)
                        put("beforeAfterMeal", reminder.beforeAfterMeal)
                        put("notes", reminder.notes)
                        put("updateTime", reminder.updateTime)
                        put("deleteFlag", reminder.deleteFlag)
                    })
                }
            })
        }.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_BASE_URL}/medicine/push")
            .addHeader("Authorization", "Bearer ${TokenManager.getToken(context)}")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络请求失败", null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val code = json.getInt("code")
                        val message = json.getString("message")
                        if (code == 200) {
                            val data = json.getJSONObject("data")
                            val successCount = data.getInt("successCount")
                            callback(true, message, successCount)
                        } else {
                            callback(false, message, null)
                        }
                    } catch (e: Exception) {
                        callback(false, "数据解析失败", null)
                    }
                }
            }
        })
    }
    /**
     * 解析健康记录JSON数组
     */
    private fun parseHealthRecords(jsonArray: JSONArray): List<HealthRecord> {
        val list = mutableListOf<HealthRecord>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(
                HealthRecord(
                    id = json.getLong("id"),
                    recordDate = json.getString("recordDate"),
                    weight = json.getDouble("weight").toFloat(),
                    systolicPressure = json.getInt("systolicPressure"),
                    diastolicPressure = json.getInt("diastolicPressure"),
                    heartRate = json.getInt("heartRate"),
                    bloodSugar = json.getDouble("bloodSugar").toFloat(),
                    sleepDuration = json.getDouble("sleepDuration").toFloat(),
                    waterIntake = json.getInt("waterIntake"),
                    steps = json.getInt("steps"),
                    notes = json.getString("notes"),
                    imagePaths = json.getString("imagePaths"),
                    updateTime = json.getLong("updateTime"),
                    isSync = 1,
                    deleteFlag = json.getInt("deleteFlag")
                )
            )
        }
        return list
    }
    /**
     * 解析用药提醒JSON数组
     */
    private fun parseMedicineReminders(jsonArray: JSONArray): List<MedicineReminder> {
        val list = mutableListOf<MedicineReminder>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(
                MedicineReminder(
                    id = json.getLong("id"),
                    medicineName = json.getString("medicineName"),
                    dosage = json.getString("dosage"),
                    unit = json.getString("unit"),
                    frequency = json.getInt("frequency"),
                    remindTime1 = json.getString("remindTime1"),
                    remindTime2 = json.optString("remindTime2", null),
                    remindTime3 = json.optString("remindTime3", null),
                    remindTime4 = json.optString("remindTime4", null),
                    remindTime5 = json.optString("remindTime5", null),
                    isEnabled = json.getBoolean("isEnabled"),
                    startDate = json.getString("startDate"),
                    endDate = json.optString("endDate", null),
                    beforeAfterMeal = json.getInt("beforeAfterMeal"),
                    notes = json.getString("notes"),
                    updateTime = json.getLong("updateTime"),
                    isSync = 1,
                    deleteFlag = json.getInt("deleteFlag")
                )
            )
        }
        return list
    }
}