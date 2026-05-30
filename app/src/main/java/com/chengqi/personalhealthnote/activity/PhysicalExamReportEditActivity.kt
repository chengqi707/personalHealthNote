package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityPhysicalExamReportAddBinding
import com.chengqi.personalhealthnote.entity.PhysicalExamReport
import com.chengqi.personalhealthnote.service.AiExamReportParseService
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ImageCompressUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhysicalExamReportEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhysicalExamReportAddBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var aiParseService: AiExamReportParseService
    private var reportId: Long = 0
    private var selectedExamDate: String = ""
    private val selectedImages = mutableListOf<String>()
    private var parsedResult: AiExamReportParseService.ParseResult? = null

    companion object {
        private const val REQUEST_PERMISSION = 4001
        private const val REQUEST_PICK_IMAGE = 4002
        private const val MAX_IMAGE_COUNT = 9
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhysicalExamReportAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        aiParseService = AiExamReportParseService()

        reportId = intent.getLongExtra("report_id", 0)
        if (reportId == 0L) {
            ToastUtils.show(this, "报告不存在")
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑体检报告"

        setupListeners()
        initImageRecyclerView()
        loadReportData()
    }

    private fun initImageRecyclerView() {
        imageAdapter = ImageAdapter(
            imagePaths = selectedImages,
            onImageClick = { position ->
                val intent = Intent(this, ImagePreviewActivity::class.java)
                intent.putStringArrayListExtra("image_paths", ArrayList(selectedImages))
                intent.putExtra("current_position", position)
                startActivity(intent)
            },
            onImageDelete = { position ->
                DialogUtils.showConfirm(this, "删除图片", "确定要删除这张图片吗？", "删除") {
                    File(selectedImages[position]).delete()
                    imageAdapter.removeImage(position)
                }
            }
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupListeners() {
        binding.tvExamDate.setOnClickListener { showExamDatePicker() }
        binding.btnAddImage.setOnClickListener { checkPermissionAndPickImage() }
        binding.btnAiParse.setOnClickListener { parseWithAi() }
        binding.btnSubmit.text = "保存修改"
        binding.btnSubmit.setOnClickListener { saveReport() }
    }

    private fun showExamDatePicker() {
        val cal = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedExamDate = sdf.format(cal.time)
                binding.tvExamDate.text = selectedExamDate
                binding.tvExamDate.setTextColor(getColor(R.color.textPrimary))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun loadReportData() {
        val report = dbHelper.getPhysicalExamReportById(reportId) ?: run {
            ToastUtils.show(this, "报告不存在")
            finish()
            return
        }

        selectedExamDate = report.examDate
        binding.tvExamDate.text = report.examDate
        binding.tvExamDate.setTextColor(getColor(R.color.textPrimary))
        binding.etHospital.setText(report.hospital)
        binding.etReportTitle.setText(report.reportTitle)

        // 加载已有图片
        if (report.imagePaths.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(report.imagePaths)
                for (i in 0 until jsonArray.length()) {
                    selectedImages.add(jsonArray.getString(i))
                }
                imageAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 回显已保存的AI解析结果
        if (report.parsedIndicators.isNotEmpty() || report.abnormalSummary.isNotEmpty() || report.aiSuggestion.isNotEmpty()) {
            parsedResult = AiExamReportParseService.ParseResult(
                indicators = if (report.parsedIndicators.isNotEmpty()) {
                    try {
                        val jsonArray = JSONArray(report.parsedIndicators)
                        (0 until jsonArray.length()).map { i ->
                            val item = jsonArray.getJSONObject(i)
                            AiExamReportParseService.Indicator(
                                name = item.getString("name"),
                                value = item.getString("value"),
                                unit = item.optString("unit", ""),
                                refRange = item.optString("refRange", ""),
                                isAbnormal = item.optBoolean("isAbnormal", false)
                            )
                        }
                    } catch (e: Exception) { emptyList() }
                } else emptyList(),
                abnormalSummary = report.abnormalSummary,
                suggestion = report.aiSuggestion
            )
            if (parsedResult != null) {
                showParseResult(parsedResult!!)
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery()
        } else {
            ToastUtils.show(this, "需要存储权限才能选择图片")
        }
    }

    private fun pickImageFromGallery() {
        if (selectedImages.size >= MAX_IMAGE_COUNT) {
            ToastUtils.show(this, "最多只能选择${MAX_IMAGE_COUNT}张图片")
            return
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.let { intent ->
                val selectedUris = mutableListOf<Uri>()
                intent.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        if (selectedImages.size + selectedUris.size < MAX_IMAGE_COUNT) {
                            selectedUris.add(clipData.getItemAt(i).uri)
                        } else break
                    }
                } ?: intent.data?.let { selectedUris.add(it) }
                selectedUris.forEach { uri ->
                    val fileName = "exam_report_${System.currentTimeMillis()}.jpg"
                    val path = ImageCompressUtils.compressToPrivateDir(this, uri, "exam_reports", fileName)
                    if (path != null) {
                        imageAdapter.addImage(path)
                    } else {
                        ToastUtils.show(this, "图片处理失败")
                    }
                }
            }
        }
    }

    private fun parseWithAi() {
        if (selectedImages.isEmpty()) {
            ToastUtils.show(this, "请先添加体检报告图片")
            return
        }

        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnAiParse.isEnabled = false

        val userProfile = dbHelper.getUserProfile()
        aiParseService.parseExamReport(selectedImages, userProfile) { success, message, result ->
            runOnUiThread {
                binding.layoutLoading.visibility = View.GONE
                binding.btnAiParse.isEnabled = true

                if (success && result != null) {
                    parsedResult = result
                    showParseResult(result)
                    ToastUtils.show(this, "AI解析完成")
                } else {
                    ToastUtils.show(this, message)
                }
            }
        }
    }

    private fun showParseResult(result: AiExamReportParseService.ParseResult) {
        binding.layoutAiResult.visibility = View.VISIBLE

        if (result.abnormalSummary.isNotEmpty()) {
            binding.tvAbnormalSummary.text = "异常指标：${result.abnormalSummary}"
            binding.tvAbnormalSummary.visibility = View.VISIBLE
        } else {
            binding.tvAbnormalSummary.visibility = View.GONE
        }

        val indicatorsText = buildString {
            result.indicators.forEach { indicator ->
                val flag = if (indicator.isAbnormal) " ↑异常" else ""
                append("${indicator.name}: ${indicator.value}${indicator.unit}")
                if (indicator.refRange.isNotEmpty()) append(" (参考: ${indicator.refRange})")
                append("$flag\n")
            }
        }
        binding.tvIndicators.text = indicatorsText.trimEnd()

        if (result.suggestion.isNotEmpty()) {
            binding.tvAiSuggestion.text = "健康建议：${result.suggestion}"
            binding.tvAiSuggestion.visibility = View.VISIBLE
        } else {
            binding.tvAiSuggestion.visibility = View.GONE
        }
    }

    private fun saveReport() {
        if (selectedExamDate.isEmpty()) {
            ToastUtils.show(this, "请选择体检日期")
            return
        }

        val imageJson = if (selectedImages.isNotEmpty()) {
            val jsonArray = JSONArray()
            selectedImages.forEach { jsonArray.put(it) }
            jsonArray.toString()
        } else ""

        val indicatorsJson = if (parsedResult != null) {
            val jsonArray = JSONArray()
            parsedResult!!.indicators.forEach { ind ->
                jsonArray.put(JSONObject().apply {
                    put("name", ind.name)
                    put("value", ind.value)
                    put("unit", ind.unit)
                    put("refRange", ind.refRange)
                    put("isAbnormal", ind.isAbnormal)
                })
            }
            jsonArray.toString()
        } else ""

        val currentTime = System.currentTimeMillis()
        val report = PhysicalExamReport(
            id = reportId,
            examDate = selectedExamDate,
            hospital = binding.etHospital.text.toString().trim(),
            reportTitle = binding.etReportTitle.text.toString().trim(),
            imagePaths = imageJson,
            parsedIndicators = indicatorsJson,
            abnormalSummary = parsedResult?.abnormalSummary ?: "",
            aiSuggestion = parsedResult?.suggestion ?: "",
            updateTime = currentTime
        )

        val result = dbHelper.updatePhysicalExamReport(report)
        if (result > 0) {
            ToastUtils.show(this, "修改成功")
            setResult(RESULT_OK)
            finish()
        } else {
            ToastUtils.show(this, "保存失败，请重试")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                DialogUtils.showConfirm(this, "提示", "是否放弃修改？", "放弃") { finish() }
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
