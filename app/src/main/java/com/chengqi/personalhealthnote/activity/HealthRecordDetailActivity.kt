package com.chengqi.personalhealthnote.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityHealthRecordDetailBinding
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.service.AiHealthService
import com.chengqi.personalhealthnote.utils.ShareUtils
import org.json.JSONArray

/**
 * 健康记录详情页
 * 展示完整记录信息，支持AI健康评估、编辑、删除
 */
class HealthRecordDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthRecordDetailBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var aiHealthService: AiHealthService
    private var recordId: Long = 0
    private lateinit var currentRecord: HealthRecord
    private val imagePaths = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter

    companion object {
        const val REQUEST_EDIT_RECORD = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        aiHealthService = AiHealthService()

        // 获取传入的记录ID
        recordId = intent.getLongExtra("record_id", 0)
        if (recordId == 0L) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadRecordData()
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "健康记录详情"

        // 初始化图片列表
        imageAdapter = ImageAdapter(
            imagePaths = imagePaths,
            onImageClick = { position ->
                // 点击查看大图
                val imagePath = imagePaths[position]
                Toast.makeText(this, "查看图片", Toast.LENGTH_SHORT).show()
                // 这里可以扩展为全屏图片查看
            },
            onImageDelete = {
                // 详情页不允许删除图片，编辑页才能操作
                Toast.makeText(this, "请进入编辑模式修改图片", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 编辑按钮
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, HealthRecordEditActivity::class.java)
            intent.putExtra("record_id", recordId)
            startActivityForResult(intent, REQUEST_EDIT_RECORD)
        }

        // 删除按钮
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // 分享按钮
        binding.btnShare.setOnClickListener {
            if (this::currentRecord.isInitialized) shareRecord()
        }

        // AI评估按钮
        binding.btnAiAssessment.setOnClickListener {
            generateHealthAssessment()
        }
    }

    /**
     * 加载记录数据
     */
    private fun loadRecordData() {
        val record = dbHelper.getHealthRecordById(recordId)
        if (record == null) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentRecord = record

        // 填充基础数据
        binding.tvDate.text = record.recordDate
        binding.tvWeight.text = if (record.weight > 0) "${record.weight} kg" else "-"
        binding.tvHeight.text = if (record.height > 0) "${record.height} cm" else "-"
        val bmi = record.calculateBMI()
        binding.tvBMI.text = if (bmi > 0) String.format("%.1f", bmi) else "-"
        binding.tvBloodPressure.text = if (record.systolicPressure > 0 && record.diastolicPressure > 0) {
            "${record.systolicPressure}/${record.diastolicPressure} mmHg"
        } else "-"
        binding.tvHeartRate.text = if (record.heartRate > 0) "${record.heartRate} 次/分" else "-"
        binding.tvBloodSugar.text = if (record.bloodSugar > 0) "${record.bloodSugar} mmol/L" else "-"
        binding.tvSleepDuration.text = if (record.sleepDuration > 0) "${record.sleepDuration} 小时" else "-"
        binding.tvWaterIntake.text = if (record.waterIntake > 0) "${record.waterIntake} ml" else "-"
        binding.tvSteps.text = if (record.steps > 0) "${record.steps} 步" else "-"
        binding.tvNotes.text = record.notes.ifEmpty { "暂无备注" }

        // 加载图片
        imagePaths.clear()
        if (record.imagePaths.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(record.imagePaths)
                for (i in 0 until jsonArray.length()) {
                    imagePaths.add(jsonArray.getString(i))
                }
                imageAdapter.notifyDataSetChanged()
                binding.cardImages.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
                binding.cardImages.visibility = View.GONE
            }
        } else {
            binding.cardImages.visibility = View.GONE
        }

        // 隐藏评估结果
        binding.cardAssessmentResult.visibility = View.GONE

        // 如果有缓存的评估结果，直接展示
        val eval = record.healthEvaluation
        if (!eval.isNullOrEmpty()) {
            val result = AiHealthService.HealthAssessmentResult(
                overallAssessment = eval,
                healthScore = 0,
                riskWarnings = emptyList(),
                dietSuggestions = emptyList(),
                exerciseSuggestions = emptyList(),
                sleepSuggestions = emptyList(),
                lifestyleSuggestions = record.lifeSuggestion?.let { listOf(it) } ?: emptyList(),
                medicalSuggestions = emptyList()
            )
            showAssessmentResult(result)
        }
    }

    /**
     * 生成AI健康评估
     */
    private fun generateHealthAssessment() {
        // 优先读缓存
        val eval = currentRecord.healthEvaluation
        if (!eval.isNullOrEmpty()) {
            val result = AiHealthService.HealthAssessmentResult(
                overallAssessment = eval,
                healthScore = 0,
                riskWarnings = emptyList(),
                dietSuggestions = emptyList(),
                exerciseSuggestions = emptyList(),
                sleepSuggestions = emptyList(),
                lifestyleSuggestions = currentRecord.lifeSuggestion?.let { listOf(it) } ?: emptyList(),
                medicalSuggestions = emptyList()
            )
            showAssessmentResult(result)
            return
        }

        // 显示加载状态
        // 显示加载状态
        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnAiAssessment.isEnabled = false

        // 获取历史记录（最近30天）
        val historyRecords = dbHelper.getRecentHealthRecords(30)
        val userProfile = dbHelper.getUserProfile()

        // 调用AI服务
        aiHealthService.generateHealthAssessment(currentRecord, historyRecords, userProfile) { success, message, result ->
            runOnUiThread {
                // 隐藏加载状态
                binding.layoutLoading.visibility = View.GONE
                binding.btnAiAssessment.isEnabled = true

                if (success && result != null) {
                    // 缓存评估结果到数据库
                    val evalText = result.overallAssessment +
                        result.riskWarnings.joinToString("；", "风险：", "") +
                        result.dietSuggestions.joinToString("；", "饮食：", "") +
                        result.exerciseSuggestions.joinToString("；", "运动：", "") +
                        result.sleepSuggestions.joinToString("；", "睡眠：", "") +
                        result.medicalSuggestions.joinToString("；", "就医：", "")
                    val suggestionText = result.lifestyleSuggestions.joinToString("；")
                    dbHelper.updateHealthRecordEvaluation(currentRecord.id, evalText, suggestionText)
                    currentRecord = currentRecord.copy(
                        healthEvaluation = evalText,
                        lifeSuggestion = suggestionText
                    )
                    // 显示评估结果
                    showAssessmentResult(result)
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 展示评估结果
     */
    private fun showAssessmentResult(result: AiHealthService.HealthAssessmentResult) {
        binding.cardAssessmentResult.visibility = View.VISIBLE

        // 基础信息
        binding.tvHealthScore.text = result.healthScore.toString()
        binding.tvOverallAssessment.text = result.overallAssessment

        // 风险警告
        if (result.riskWarnings.isNotEmpty()) {
            (binding.layoutRiskWarnings.parent as View).visibility = View.VISIBLE
            binding.layoutRiskWarnings.removeAllViews()
            result.riskWarnings.forEach { warning ->
                val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutRiskWarnings, false) as TextView
                textView.text = "• $warning"
                binding.layoutRiskWarnings.addView(textView)
            }
        } else {
            (binding.layoutRiskWarnings.parent as View).visibility = View.GONE
        }

        // 饮食建议
        binding.layoutDietSuggestions.removeAllViews()
        result.dietSuggestions.forEach { suggestion ->
            val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutDietSuggestions, false) as TextView
            textView.text = "• $suggestion"
            binding.layoutDietSuggestions.addView(textView)
        }

        // 运动建议
        binding.layoutExerciseSuggestions.removeAllViews()
        result.exerciseSuggestions.forEach { suggestion ->
            val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutExerciseSuggestions, false) as TextView
            textView.text = "• $suggestion"
            binding.layoutExerciseSuggestions.addView(textView)
        }

        // 睡眠建议
        binding.layoutSleepSuggestions.removeAllViews()
        result.sleepSuggestions.forEach { suggestion ->
            val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutSleepSuggestions, false) as TextView
            textView.text = "• $suggestion"
            binding.layoutSleepSuggestions.addView(textView)
        }

        // 生活习惯建议
        binding.layoutLifestyleSuggestions.removeAllViews()
        result.lifestyleSuggestions.forEach { suggestion ->
            val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutLifestyleSuggestions, false) as TextView
            textView.text = "• $suggestion"
            binding.layoutLifestyleSuggestions.addView(textView)
        }

        // 就医建议
        if (result.medicalSuggestions.isNotEmpty()) {
            (binding.layoutMedicalSuggestions.parent as View).visibility = View.VISIBLE
            binding.layoutMedicalSuggestions.removeAllViews()
            result.medicalSuggestions.forEach { suggestion ->
                val textView = LayoutInflater.from(this).inflate(R.layout.item_suggestion, binding.layoutMedicalSuggestions, false) as TextView
                textView.text = "• $suggestion"
                binding.layoutMedicalSuggestions.addView(textView)
            }
        } else {
            (binding.layoutMedicalSuggestions.parent as View).visibility = View.GONE
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun shareRecord() {
        val lines = mutableListOf<String>()
        lines.add("记录日期：${currentRecord.recordDate}")
        if (currentRecord.weight > 0) lines.add("体重：${currentRecord.weight} kg")
        if (currentRecord.systolicPressure > 0 && currentRecord.diastolicPressure > 0) {
            lines.add("血压：${currentRecord.systolicPressure}/${currentRecord.diastolicPressure} mmHg")
        }
        if (currentRecord.heartRate > 0) lines.add("心率：${currentRecord.heartRate} 次/分")
        if (currentRecord.bloodSugar > 0) lines.add("血糖：${currentRecord.bloodSugar} mmol/L")
        if (currentRecord.sleepDuration > 0) lines.add("睡眠：${currentRecord.sleepDuration} 小时")
        if (currentRecord.waterIntake > 0) lines.add("饮水：${currentRecord.waterIntake} ml")
        if (currentRecord.steps > 0) lines.add("步数：${currentRecord.steps} 步")
        if (currentRecord.notes.isNotEmpty()) lines.add("备注：${currentRecord.notes}")
        ShareUtils.shareAsImage(this, "健康记录详情", lines)
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除这条健康记录吗？删除后无法恢复。")
            .setPositiveButton("删除") { _, _ ->
                val result = dbHelper.deleteHealthRecord(recordId)
                if (result > 0) {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_RECORD && resultCode == Activity.RESULT_OK) {
            // 编辑完成，重新加载数据
            loadRecordData()
            // 通知列表页更新
            setResult(Activity.RESULT_OK)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
