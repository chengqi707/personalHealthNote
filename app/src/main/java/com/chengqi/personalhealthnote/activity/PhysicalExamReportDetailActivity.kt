package com.chengqi.personalhealthnote.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityPhysicalExamReportDetailBinding
import com.chengqi.personalhealthnote.entity.PhysicalExamReport
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
import org.json.JSONArray

class PhysicalExamReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhysicalExamReportDetailBinding
    private lateinit var dbHelper: DatabaseHelper
    private var reportId: Long = 0
    private lateinit var currentReport: PhysicalExamReport
    private val imagePaths = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter

    companion object {
        private const val REQUEST_EDIT_REPORT = 5001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhysicalExamReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        reportId = intent.getLongExtra("report_id", 0)
        if (reportId == 0L) {
            ToastUtils.show(this, "报告不存在")
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "体检报告详情"

        imageAdapter = ImageAdapter(
            imagePaths = imagePaths,
            onImageClick = { position ->
                val intent = Intent(this, ImagePreviewActivity::class.java)
                intent.putStringArrayListExtra("image_paths", ArrayList(imagePaths))
                intent.putExtra("current_position", position)
                startActivity(intent)
            },
            onImageDelete = {}
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)

        binding.btnEdit.setOnClickListener { showEdit() }
        binding.btnDelete.setOnClickListener { showDeleteConfirm() }
        loadReportData()
    }

    private fun loadReportData() {
        val report = dbHelper.getPhysicalExamReportById(reportId)
        if (report == null) {
            ToastUtils.show(this, "报告不存在")
            finish()
            return
        }
        currentReport = report

        binding.tvReportTitle.text = report.reportTitle.ifEmpty { "体检报告" }
        binding.tvExamDate.text = report.examDate
        binding.tvHospital.text = report.hospital.ifEmpty { "" }

        // 异常指标
        if (report.abnormalSummary.isNotEmpty()) {
            binding.layoutAbnormal.visibility = View.VISIBLE
            binding.tvAbnormalSummary.text = report.abnormalSummary
        } else {
            binding.layoutAbnormal.visibility = View.GONE
        }

        // 解析指标
        if (report.parsedIndicators.isNotEmpty()) {
            binding.layoutIndicators.visibility = View.VISIBLE
            try {
                val jsonArray = JSONArray(report.parsedIndicators)
                val indicatorsText = buildString {
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val name = item.getString("name")
                        val value = item.getString("value")
                        val unit = item.optString("unit", "")
                        val refRange = item.optString("refRange", "")
                        val isAbnormal = item.optBoolean("isAbnormal", false)
                        val flag = if (isAbnormal) " ↑异常" else ""
                        append("$name: $value$unit")
                        if (refRange.isNotEmpty()) append(" (参考: $refRange)")
                        append("$flag\n")
                    }
                }
                binding.tvIndicators.text = indicatorsText.trimEnd()
            } catch (e: Exception) {
                binding.tvIndicators.text = report.parsedIndicators
            }
        } else {
            binding.layoutIndicators.visibility = View.GONE
        }

        // AI建议
        if (report.aiSuggestion.isNotEmpty()) {
            binding.layoutSuggestion.visibility = View.VISIBLE
            binding.tvAiSuggestion.text = report.aiSuggestion
        } else {
            binding.layoutSuggestion.visibility = View.GONE
        }

        // 图片
        imagePaths.clear()
        if (report.imagePaths.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(report.imagePaths)
                for (i in 0 until jsonArray.length()) {
                    imagePaths.add(jsonArray.getString(i))
                }
                imageAdapter.notifyDataSetChanged()
                binding.layoutImages.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.layoutImages.visibility = View.GONE
            }
        } else {
            binding.layoutImages.visibility = View.GONE
        }
    }

    private fun showEdit() {
        val intent = Intent(this, PhysicalExamReportEditActivity::class.java)
        intent.putExtra("report_id", reportId)
        startActivityForResult(intent, REQUEST_EDIT_REPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_REPORT && resultCode == Activity.RESULT_OK) {
            loadReportData()
        }
    }

    private fun showDeleteConfirm() {
        DialogUtils.showConfirm(this, "删除确认", "确定要删除这份体检报告吗？删除后不可恢复。", "删除") {
            val result = dbHelper.deletePhysicalExamReport(reportId)
            if (result > 0) {
                ToastUtils.show(this, "删除成功")
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                ToastUtils.show(this, "删除失败")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
