package com.chengqi.personalhealthnote.service

import android.util.Base64
import com.chengqi.personalhealthnote.BuildConfig
import com.chengqi.personalhealthnote.entity.UserProfile
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiExamReportParseService {

    private val apiKey = BuildConfig.DOUBAO_API_KEY
    private val apiUrl = BuildConfig.DOUBAO_API_URL
    private val model = BuildConfig.DOUBAO_MODEL

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    data class ParseResult(
        val indicators: List<Indicator>,
        val abnormalSummary: String,
        val suggestion: String
    )

    data class Indicator(
        val name: String,
        val value: String,
        val unit: String,
        val refRange: String,
        val isAbnormal: Boolean
    )

    fun parseExamReport(
        imagePaths: List<String>,
        userProfile: UserProfile?,
        callback: (Boolean, String, ParseResult?) -> Unit
    ) {
        try {
            if (apiKey.isEmpty() || apiKey == "这里替换成你的API Key") {
                callback(false, "请先配置API Key", null)
                return
            }

            val contentArray = JSONArray()

            // 文本提示
            val promptText = buildString {
                append("请解析以下体检报告图片，提取所有关键健康指标。")
                if (userProfile != null && !userProfile.isEmpty()) {
                    append("\n")
                    append(userProfile.toPromptText())
                }
                append("\n\n请严格按照以下JSON格式返回结果，不要有其他多余内容：\n")
                append("""{"indicators":[{"name":"指标名","value":"值","unit":"单位","refRange":"参考范围","isAbnormal":true/false}],"abnormalSummary":"异常指标汇总","suggestion":"健康建议"}""")
            }

            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", promptText)
            })

            // 图片（base64编码）
            for (path in imagePaths) {
                val file = File(path)
                if (!file.exists()) continue
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = when {
                    path.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "image/jpeg"
                }
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:$mimeType;base64,$base64")
                    })
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("temperature", 0.3)
                put("max_tokens", 3000)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
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
                    callback(false, "解析请求超时，请稍后重试", null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                val errorBody = response.body?.string() ?: ""
                                callback(false, "接口异常(${response.code})", null)
                                return
                            }

                            val responseBody = response.body?.string()
                            if (responseBody.isNullOrEmpty()) {
                                callback(false, "未生成有效结果，请重试", null)
                                return
                            }

                            val jsonObject = JSONObject(responseBody)
                            val choices = jsonObject.getJSONArray("choices")
                            val message = choices.getJSONObject(0).getJSONObject("message")
                            val content = message.getString("content")

                            val jsonStr = extractJson(content)
                            val resultJson = JSONObject(jsonStr)

                            val indicatorsArray = resultJson.getJSONArray("indicators")
                            val indicators = mutableListOf<Indicator>()
                            for (i in 0 until indicatorsArray.length()) {
                                val item = indicatorsArray.getJSONObject(i)
                                indicators.add(Indicator(
                                    name = item.getString("name"),
                                    value = item.getString("value"),
                                    unit = item.optString("unit", ""),
                                    refRange = item.optString("refRange", ""),
                                    isAbnormal = item.optBoolean("isAbnormal", false)
                                ))
                            }

                            val result = ParseResult(
                                indicators = indicators,
                                abnormalSummary = resultJson.optString("abnormalSummary", ""),
                                suggestion = resultJson.optString("suggestion", "")
                            )
                            callback(true, "解析完成", result)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            callback(false, "解析结果格式异常，请重试", null)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, "请求构建失败，请重试", null)
        }
    }

    private fun extractJson(content: String): String {
        try { JSONObject(content); return content } catch (_: Exception) {}
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```")
        val match = codeBlockRegex.find(content)
        if (match != null) return match.groupValues[1].trim()
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')
        if (startIndex >= 0 && endIndex > startIndex) return content.substring(startIndex, endIndex + 1)
        return content
    }
}
