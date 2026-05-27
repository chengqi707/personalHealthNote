package com.chengqi.personalhealthnote.service

import android.util.Log
import com.chengqi.personalhealthnote.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 就医记录健康评估服务
 * 按 PRD §6.4 规范：上传标准化文本，返回 healthEvaluation + lifeSuggestion
 */
class AiMedicalAssessmentService {

    private val apiKey = BuildConfig.DOUBAO_API_KEY
    private val apiUrl = BuildConfig.DOUBAO_API_URL
    private val model = BuildConfig.DOUBAO_MODEL

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * 调用云侧模型进行健康评估
     * @param standardText 端侧格式化后的就医记录文本
     * @param callback 回调 (success, message, healthEvaluation, lifeSuggestion)
     */
    fun assess(
        standardText: String,
        callback: (Boolean, String, String?, String?) -> Unit
    ) {
        try {
            if (apiKey.isEmpty() || apiKey == "这里替换成你的API Key") {
                callback(false, "请先配置API Key", null, null)
                return
            }

            if (apiUrl.isEmpty()) {
                callback(false, "请先配置API地址", null, null)
                return
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("temperature", 0.7)
                put("max_tokens", 2000)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", """
你是一个专业的健康管理助手，根据用户提供的就医记录信息，给出健康状态评估和生活建议。
请严格按照以下JSON格式返回结果，不要有其他多余内容：
{
    "healthEvaluation": "基于就医记录的健康状态评估，包含症状分析、诊断解读等",
    "lifeSuggestion": "生活各方面的建议，包含饮食、作息、运动、防护等"
}
所有建议要具体、可落地，符合用户实际就医情况。
                        """.trimIndent())
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", standardText)
                    })
                })
            }

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("AiMedicalAssessment", "请求失败: ${e.message}", e)
                    callback(false, "评估请求超时，请稍后重试", null, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                val errorBody = response.body?.string() ?: ""
                                Log.e("AiMedicalAssessment", "接口异常: code=${response.code}, body=$errorBody")
                                callback(false, "评估生成失败：接口返回异常", null, null)
                                return
                            }

                            val responseBody = response.body?.string()
                            if (responseBody.isNullOrEmpty()) {
                                callback(false, "未生成有效评估结果，请重试", null, null)
                                return
                            }

                            Log.d("AiMedicalAssessment", "响应: $responseBody")
                            val jsonObject = JSONObject(responseBody)
                            val choices = jsonObject.getJSONArray("choices")
                            val message = choices.getJSONObject(0).getJSONObject("message")
                            val content = message.getString("content")

                            // 尝试提取JSON，兼容markdown代码块包裹的情况
                            val jsonStr = extractJson(content)
                            val resultJson = JSONObject(jsonStr)
                            val healthEvaluation = resultJson.optString("healthEvaluation", "")
                            val lifeSuggestion = resultJson.optString("lifeSuggestion", "")

                            if (healthEvaluation.isEmpty() && lifeSuggestion.isEmpty()) {
                                callback(false, "评估结果解析失败，请重试", null, null)
                                return
                            }

                            callback(true, "评估完成", healthEvaluation, lifeSuggestion)
                        } catch (e: Exception) {
                            Log.e("AiMedicalAssessment", "结果解析失败: ${e.message}", e)
                            callback(false, "评估结果解析失败，请重试", null, null)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("AiMedicalAssessment", "请求构建失败: ${e.message}", e)
            callback(false, "评估请求发起失败，请重试", null, null)
        }
    }

    /**
     * 从AI返回的content中提取JSON字符串
     * 兼容直接返回JSON和markdown代码块包裹的情况
     */
    private fun extractJson(content: String): String {
        // 尝试直接解析
        try {
            JSONObject(content)
            return content
        } catch (_: Exception) {}

        // 尝试提取markdown代码块中的JSON
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```")
        val match = codeBlockRegex.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // 尝试找第一个 { 到最后一个 }
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')
        if (startIndex >= 0 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1)
        }

        return content
    }
}
