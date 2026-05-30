package com.chengqi.personalhealthnote.activity

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicalRecordDetailBinding
import com.chengqi.personalhealthnote.entity.MedicalRecord
import com.chengqi.personalhealthnote.service.AiMedicalAssessmentService
import com.chengqi.personalhealthnote.utils.CalendarHelper
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.TemplateManager
import com.chengqi.personalhealthnote.utils.ToastUtils
import com.chengqi.personalhealthnote.utils.ShareUtils
import androidx.recyclerview.widget.GridLayoutManager
import org.json.JSONArray
import org.json.JSONObject

class MedicalRecordDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MedicalRecordDetail"
        const val REQUEST_EDIT_RECORD = 2001
    }

    private lateinit var binding: ActivityMedicalRecordDetailBinding
    private lateinit var dbHelper: DatabaseHelper
    private var recordId: Long = 0
    private lateinit var currentRecord: MedicalRecord
    private lateinit var aiAssessmentService: AiMedicalAssessmentService
    private val imagePaths = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        aiAssessmentService = AiMedicalAssessmentService()

        recordId = intent.getLongExtra("record_id", 0)
        if (recordId == 0L) {
            ToastUtils.show(this, "记录不存在")
            finish()
            return
        }

        initViews()
        setupListeners()
        loadRecordData()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "就医记录详情"

        imageAdapter = ImageAdapter(
            imagePaths = imagePaths,
            onImageClick = { position ->
                val intent = Intent(this, ImagePreviewActivity::class.java)
                intent.putStringArrayListExtra("image_paths", ArrayList(imagePaths))
                intent.putExtra("current_position", position)
                startActivity(intent)
            },
            onImageDelete = {
                ToastUtils.show(this, "请进入编辑模式修改图片")
            }
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupListeners() {
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, MedicalRecordEditActivity::class.java)
            intent.putExtra("record_id", recordId)
            startActivityForResult(intent, REQUEST_EDIT_RECORD)
        }

        binding.btnShare.setOnClickListener {
            if (!this::currentRecord.isInitialized) return@setOnClickListener
            shareRecord()
        }

        binding.btnSaveTemplate.setOnClickListener {
            if (!this::currentRecord.isInitialized) return@setOnClickListener
            saveAsTemplate()
        }

        binding.btnHealthAssessment.setOnClickListener {
            if (!this::currentRecord.isInitialized) {
                ToastUtils.show(this, "记录加载中，请稍后")
                return@setOnClickListener
            }
            if (!currentRecord.hasValidInfo()) {
                ToastUtils.show(this, "记录无有效信息，无法生成评估")
                return@setOnClickListener
            }
            generateHealthAssessment()
        }
    }

    private fun loadRecordData() {
        val record = dbHelper.getMedicalRecordById(recordId)
        if (record == null) {
            ToastUtils.show(this, "记录不存在")
            finish()
            return
        }

        currentRecord = record

        binding.tvMedicalTime.text = record.medicalTime
        binding.tvHospital.text = record.hospital
        binding.tvDoctor.text = record.doctor.ifEmpty { "未填写" }
        binding.tvSymptoms.text = record.symptoms
        binding.tvDiagnosisResult.text = record.diagnosisResult
        binding.tvCheckItems.text = record.checkItems.ifEmpty { "未填写" }
        binding.tvMedicines.text = record.medicines.ifEmpty { "未填写" }

        // 复诊日期
        if (record.followUpDate.isNotEmpty()) {
            binding.layoutFollowUp.visibility = View.VISIBLE
            binding.tvFollowUpDate.text = record.followUpDate
            // 计算倒计时
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val followUpDate = sdf.parse(record.followUpDate)
                if (followUpDate != null) {
                    val today = java.util.Calendar.getInstance()
                    val followUpCal = java.util.Calendar.getInstance().apply { time = followUpDate }
                    val diffMs = followUpCal.timeInMillis - today.timeInMillis
                    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                    if (diffDays > 0) {
                        binding.tvFollowUpCountdown.text = "还有${diffDays}天复诊"
                        binding.tvFollowUpCountdown.setTextColor(getColor(R.color.primary))
                    } else if (diffDays == 0) {
                        binding.tvFollowUpCountdown.text = "今天复诊"
                        binding.tvFollowUpCountdown.setTextColor(getColor(R.color.primary))
                    } else {
                        binding.tvFollowUpCountdown.text = "已过期${-diffDays}天"
                        binding.tvFollowUpCountdown.setTextColor(getColor(R.color.textHint))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // 加入日历按钮
            binding.btnAddFollowUpCalendar.setOnClickListener {
                try {
                    if (record.followUpCalendarEventId.isNotEmpty()) {
                        ToastUtils.show(this, "已添加过日历提醒")
                        return@setOnClickListener
                    }
                    val eventId = CalendarHelper.addFollowUpEvent(this, record)
                    if (eventId.isNotEmpty()) {
                        val updatedRecord = record.copy(followUpCalendarEventId = eventId)
                        dbHelper.updateMedicalRecord(updatedRecord)
                        currentRecord = updatedRecord
                        ToastUtils.show(this, "已添加到日历提醒")
                        binding.btnAddFollowUpCalendar.text = "已添加日历提醒"
                        binding.btnAddFollowUpCalendar.isEnabled = false
                    } else {
                        ToastUtils.show(this, "添加日历提醒失败")
                    }
                } catch (e: Exception) {
                    ToastUtils.show(this, "添加日历提醒失败")
                }
            }
            if (record.followUpCalendarEventId.isNotEmpty()) {
                binding.btnAddFollowUpCalendar.text = "已添加日历提醒"
                binding.btnAddFollowUpCalendar.isEnabled = false
            }
        } else {
            binding.layoutFollowUp.visibility = View.GONE
        }

        imagePaths.clear()
        if (record.imagePaths.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(record.imagePaths)
                for (i in 0 until jsonArray.length()) {
                    imagePaths.add(jsonArray.getString(i))
                }
                imageAdapter.notifyDataSetChanged()
                binding.layoutImages.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
                binding.layoutImages.visibility = View.GONE
            }
        } else {
            binding.layoutImages.visibility = View.GONE
        }

        binding.btnHealthAssessment.isEnabled = record.hasValidInfo()
        if (!record.hasValidInfo()) {
            binding.btnHealthAssessment.setBackgroundResource(R.drawable.bg_button_disabled)
            binding.btnHealthAssessment.setTextColor(getColor(R.color.textHint))
        }
    }

    private fun generateHealthAssessment() {
        if (!isNetworkAvailable()) {
            ToastUtils.show(this, "网络未连接，无法生成健康评估")
            return
        }

        if (!currentRecord.healthEvaluation.isNullOrEmpty()) {
            showAssessmentDialog(currentRecord.healthEvaluation!!, currentRecord.lifeSuggestion ?: "")
            return
        }

        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnHealthAssessment.isEnabled = false
        binding.btnHealthAssessment.text = "评估中..."

        val standardText = currentRecord.toStandardFormatText()
        val userProfile = dbHelper.getUserProfile()
        val context = this
        aiAssessmentService.assess(standardText, userProfile) { success, message, healthEvaluation, lifeSuggestion ->
            try {
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    binding.layoutLoading.visibility = View.GONE
                    binding.btnHealthAssessment.isEnabled = true
                    binding.btnHealthAssessment.text = "生成健康评估"

                    if (success && healthEvaluation != null) {
                        dbHelper.updateMedicalRecordEvaluation(
                            currentRecord.id,
                            healthEvaluation,
                            lifeSuggestion ?: ""
                        )
                        currentRecord = currentRecord.copy(
                            healthEvaluation = healthEvaluation,
                            lifeSuggestion = lifeSuggestion
                        )
                        showAssessmentDialog(healthEvaluation, lifeSuggestion ?: "")
                    } else {
                        ToastUtils.show(context, message ?: "评估生成失败")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "assess回调异常: ${e.message}", e)
            }
        }
    }

    private fun showAssessmentDialog(healthEvaluation: String, lifeSuggestion: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_health_assessment, null)
        val tvHealthEvaluation = dialogView.findViewById<TextView>(R.id.tvHealthEvaluation)
        val tvLifeSuggestion = dialogView.findViewById<TextView>(R.id.tvLifeSuggestion)

        tvHealthEvaluation.text = healthEvaluation
        tvLifeSuggestion.text = lifeSuggestion

        val dialog = AlertDialog.Builder(this)
            .setTitle("健康状态评估与生活建议")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun saveAsTemplate() {
        val templateName = "${currentRecord.hospital}-${currentRecord.diagnosisResult}"
        val template = TemplateManager.MedicalRecordTemplate(
            name = templateName,
            hospital = currentRecord.hospital,
            doctor = currentRecord.doctor,
            checkItems = currentRecord.checkItems,
            medicines = currentRecord.medicines
        )
        TemplateManager.saveTemplate(this, template)
        ToastUtils.show(this, "已保存为模板：$templateName")
    }

    private fun shareRecord() {
        val lines = mutableListOf<String>()
        lines.add("就医时间：${currentRecord.medicalTime}")
        lines.add("就诊医院：${currentRecord.hospital}")
        if (currentRecord.doctor.isNotEmpty()) lines.add("接诊医生：${currentRecord.doctor}")
        lines.add("症状描述：${currentRecord.symptoms}")
        lines.add("就诊结果：${currentRecord.diagnosisResult}")
        if (currentRecord.checkItems.isNotEmpty()) lines.add("检查项目：${currentRecord.checkItems}")
        if (currentRecord.medicines.isNotEmpty()) lines.add("药品清单：${currentRecord.medicines}")
        val eval = currentRecord.healthEvaluation
        val suggestion = currentRecord.lifeSuggestion
        if (!eval.isNullOrEmpty()) {
            lines.add("")
            lines.add("【健康评估】")
            lines.add(eval)
        }
        if (!suggestion.isNullOrEmpty()) {
            lines.add("")
            lines.add("【生活建议】")
            lines.add(suggestion)
        }
        ShareUtils.shareAsImage(this, "就医记录详情", lines)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showDeleteConfirmDialog() {
        DialogUtils.showConfirm(
            this,
            "删除确认",
            "确定要删除这条记录吗？删除后不可恢复",
            "确定"
        ) {
            // 删除关联的复诊日历事件
            try {
                if (currentRecord.followUpCalendarEventId.isNotEmpty()) {
                    CalendarHelper.deleteFollowUpEvent(this, currentRecord.followUpCalendarEventId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val result = dbHelper.deleteMedicalRecord(currentRecord.id.toInt())
            if (result > 0) {
                ToastUtils.show(this, "删除成功")
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                ToastUtils.show(this, "删除失败")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_RECORD && resultCode == Activity.RESULT_OK) {
            loadRecordData()
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
