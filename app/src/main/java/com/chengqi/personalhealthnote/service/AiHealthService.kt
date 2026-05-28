package com.chengqi.personalhealthnote.service

import android.util.Log
import com.chengqi.personalhealthnote.BuildConfig
import com.chengqi.personalhealthnote.entity.HealthRecord
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody // 导入扩展函数
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI健康评估服务
 * 封装云侧模型调用，返回健康评估和建议
 */
class AiHealthService {

    // 配置你的AI模型API信息，从BuildConfig读取，避免硬编码敏感信息
    private val apiKey = BuildConfig.DOUBAO_API_KEY
    private val apiUrl = BuildConfig.DOUBAO_API_URL
    private val model = BuildConfig.DOUBAO_MODEL

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时30秒
        .readTimeout(30, TimeUnit.SECONDS)     // 读取超时30秒，和加载动画同步
        .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时30秒
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * 生成健康评估
     * @param record 当前健康记录
     * @param historyRecords 历史健康记录（可选，用于更准确的评估）
     * @param callback 结果回调
     */
    fun generateHealthAssessment(
        record: HealthRecord,
        historyRecords: List<HealthRecord> = emptyList(),
        callback: (Boolean, String, HealthAssessmentResult?) -> Unit
    ) {
        // 先检查API Key是否配置
        if (apiKey == "这里替换成你的API Key" || apiKey.isEmpty()) {
            callback(false, "请先配置API Key", null)
            return
        }

        // 构建提示词
        val prompt = buildPrompt(record, historyRecords)

        // 构建请求体
        val requestBody = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("max_tokens", 2000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        你是一个专业的健康管理助手，根据用户提供的健康记录数据，给出专业的健康评估和生活建议。
                        请严格按照以下JSON格式返回结果，不要有其他多余内容：
                        {
                            "overallAssessment": "整体健康状态评估，一句话总结",
                            "healthScore": 0-100的健康评分,
                            "riskWarnings": ["风险点1", "风险点2", ...],
                            "dietSuggestions": ["饮食建议1", "饮食建议2", ...],
                            "exerciseSuggestions": ["运动建议1", "运动建议2", ...],
                            "sleepSuggestions": ["睡眠建议1", "睡眠建议2", ...],
                            "lifestyleSuggestions": ["生活习惯建议1", "生活习惯建议2", ...],
                            "medicalSuggestions": ["就医建议1", "就医建议2", ...]
                        }
                        所有建议要具体、可落地，符合用户实际数据情况。如果某项没有数据，可以给出通用建议。
                    """.trimIndent())
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toString().toRequestBody(JSON)) // 修复过时API
            .build()

        // 异步发送请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, "请求失败", null) // 统一错误提示
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: "未知错误"
                            Log.e("AiHealthService", "接口异常(${response.code})：${errorBody.take(200)}")
                            callback(false, "接口异常(${response.code})", null) // 统一错误提示
                            return
                        }

                        val responseBody = response.body?.string()

                        // 空值判断
                        if (responseBody.isNullOrEmpty()) {
                            callback(false, "请求失败", null)
                            return@use
                        }

                        val jsonObject = JSONObject(responseBody)
                        val choices = jsonObject.getJSONArray("choices")
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content")

                        // 解析返回的JSON结果
                        val resultJson = JSONObject(content)
                        val assessmentResult = HealthAssessmentResult(
                            overallAssessment = resultJson.getString("overallAssessment"),
                            healthScore = resultJson.getInt("healthScore"),
                            riskWarnings = parseJsonArray(resultJson.getJSONArray("riskWarnings")),
                            dietSuggestions = parseJsonArray(resultJson.getJSONArray("dietSuggestions")),
                            exerciseSuggestions = parseJsonArray(resultJson.getJSONArray("exerciseSuggestions")),
                            sleepSuggestions = parseJsonArray(resultJson.getJSONArray("sleepSuggestions")),
                            lifestyleSuggestions = parseJsonArray(resultJson.getJSONArray("lifestyleSuggestions")),
                            medicalSuggestions = parseJsonArray(resultJson.getJSONArray("medicalSuggestions"))
                        )

                        callback(true, "评估完成", assessmentResult)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(false, "评估结果解析失败，请重试", null) // 统一错误提示
                    }
                }
            }
        })
    }

    /**
     * 构建提示词
     */
    private fun buildPrompt(record: HealthRecord, historyRecords: List<HealthRecord>): String {
        val sb = StringBuilder()

        sb.append("当前健康记录数据：\n")
        sb.append("记录日期：${record.recordDate}\n")
        if (record.weight > 0) sb.append("体重：${record.weight} kg\n")
        if (record.systolicPressure > 0 && record.diastolicPressure > 0) {
            sb.append("血压：${record.systolicPressure}/${record.diastolicPressure} mmHg\n")
        }
        if (record.heartRate > 0) sb.append("心率：${record.heartRate} 次/分钟\n")
        if (record.bloodSugar > 0) sb.append("血糖：${record.bloodSugar} mmol/L\n")
        if (record.sleepDuration > 0) sb.append("睡眠时长：${record.sleepDuration} 小时\n")
        if (record.waterIntake > 0) sb.append("饮水量：${record.waterIntake} ml\n")
        if (record.steps > 0) sb.append("步数：${record.steps} 步\n")
        if (record.notes.isNotEmpty()) sb.append("备注：${record.notes}\n")

        if (historyRecords.isNotEmpty()) {
            sb.append("\n最近30天历史记录共${historyRecords.size}条，仅供参考：\n")
            // 只取最近5条避免太长
            historyRecords.take(5).forEachIndexed { index, r ->
                sb.append("${index + 1}. ${r.recordDate}: ")
                if (r.systolicPressure > 0 && r.diastolicPressure > 0) sb.append("血压${r.systolicPressure}/${r.diastolicPressure} ")
                if (r.bloodSugar > 0) sb.append("血糖${r.bloodSugar} ")
                sb.append("\n")
            }
        }

        sb.append("\n请根据以上数据给出专业的健康评估和生活建议。")
        return sb.toString()
    }

    /**
     * 解析JSON数组为字符串列表
     */
    private fun parseJsonArray(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    /**
     * 健康评估结果实体
     */
    data class HealthAssessmentResult(
        val overallAssessment: String,  // 整体评估
        val healthScore: Int,           // 健康评分 0-100
        val riskWarnings: List<String>, // 风险警告
        val dietSuggestions: List<String>, // 饮食建议
        val exerciseSuggestions: List<String>, // 运动建议
        val sleepSuggestions: List<String>,   // 睡眠建议
        val lifestyleSuggestions: List<String>, // 生活习惯建议
        val medicalSuggestions: List<String>    // 就医建议
    )
}
