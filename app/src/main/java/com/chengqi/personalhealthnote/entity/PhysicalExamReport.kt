package com.chengqi.personalhealthnote.entity

data class PhysicalExamReport(
    val id: Long = 0,
    val examDate: String,              // yyyy-MM-dd
    val hospital: String = "",         // 体检机构
    val reportTitle: String = "",      // 用户自定义标题
    val imagePaths: String = "",       // JSON数组，报告图片
    val parsedIndicators: String = "", // AI解析的结构化结果，JSON
    val abnormalSummary: String = "",  // AI总结的异常指标
    val aiSuggestion: String = "",     // AI建议
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val isSync: Int = 0,
    val deleteFlag: Int = 0
)
