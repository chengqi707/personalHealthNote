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
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
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
        Log.d(TAG, "onCreate start")
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        aiAssessmentService = AiMedicalAssessmentService()

        recordId = intent.getLongExtra("record_id", 0)
        Log.d(TAG, "recordId=$recordId")
        if (recordId == 0L) {
            Log.w(TAG, "recordId为0，finish")
            ToastUtils.show(this, "记录不存在")
            finish()
            return
        }

        initViews()
        setupListeners()
        loadRecordData()
        Log.d(TAG, "onCreate end")
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
            Log.d(TAG, "点击删除按钮")
            showDeleteConfirmDialog()
        }

        binding.btnEdit.setOnClickListener {
            Log.d(TAG, "点击编辑按钮")
            val intent = Intent(this, MedicalRecordEditActivity::class.java)
            intent.putExtra("record_id", recordId)
            startActivityForResult(intent, REQUEST_EDIT_RECORD)
        }

        binding.btnHealthAssessment.setOnClickListener {
            Log.d(TAG, "点击生成健康评估按钮")
            if (!this::currentRecord.isInitialized) {
                Log.w(TAG, "currentRecord未初始化")
                ToastUtils.show(this, "记录加载中，请稍后")
                return@setOnClickListener
            }
            if (!currentRecord.hasValidInfo()) {
                Log.w(TAG, "记录无有效信息")
                ToastUtils.show(this, "记录无有效信息，无法生成评估")
                return@setOnClickListener
            }
            generateHealthAssessment()
        }
    }

    private fun loadRecordData() {
        val record = dbHelper.getMedicalRecordById(recordId)
        if (record == null) {
            Log.w(TAG, "loadRecordData: record为null, finish")
            ToastUtils.show(this, "记录不存在")
            finish()
            return
        }

        currentRecord = record
        Log.d(TAG, "loadRecordData: 记录加载成功, id=${record.id}, hasValidInfo=${record.hasValidInfo()}, " +
                "hasEvaluation=${!record.healthEvaluation.isNullOrEmpty()}")

        binding.tvMedicalTime.text = record.medicalTime
        binding.tvHospital.text = record.hospital
        binding.tvDoctor.text = record.doctor.ifEmpty { "未填写" }
        binding.tvSymptoms.text = record.symptoms
        binding.tvDiagnosisResult.text = record.diagnosisResult
        binding.tvCheckItems.text = record.checkItems.ifEmpty { "未填写" }
        binding.tvMedicines.text = record.medicines.ifEmpty { "未填写" }

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
        Log.d(TAG, "generateHealthAssessment: 开始")

        if (!isNetworkAvailable()) {
            Log.w(TAG, "generateHealthAssessment: 无网络")
            ToastUtils.show(this, "网络未连接，无法生成健康评估")
            return
        }
        Log.d(TAG, "generateHealthAssessment: 网络可用")

        if (!currentRecord.healthEvaluation.isNullOrEmpty()) {
            Log.d(TAG, "generateHealthAssessment: 有缓存，直接弹窗")
            showAssessmentDialog(currentRecord.healthEvaluation!!, currentRecord.lifeSuggestion ?: "")
            return
        }
        Log.d(TAG, "generateHealthAssessment: 无缓存，调用API")

        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnHealthAssessment.isEnabled = false
        binding.btnHealthAssessment.text = "评估中..."

        val standardText = currentRecord.toStandardFormatText()
        Log.d(TAG, "generateHealthAssessment: standardText长度=${standardText.length}")
        val context = this
        aiAssessmentService.assess(standardText) { success, message, healthEvaluation, lifeSuggestion ->
            Log.d(TAG, "assess回调: success=$success, message=$message, " +
                    "healthEvaluation=${healthEvaluation?.take(50)}, lifeSuggestion=${lifeSuggestion?.take(50)}")
            try {
                runOnUiThread {
                    Log.d(TAG, "assess回调 runOnUiThread: isFinishing=$isFinishing, isDestroyed=$isDestroyed")
                    if (isFinishing || isDestroyed) {
                        Log.w(TAG, "Activity已finishing/destroyed，跳过UI更新")
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
                        Log.d(TAG, "assess回调: 准备弹窗")
                        showAssessmentDialog(healthEvaluation, lifeSuggestion ?: "")
                    } else {
                        Log.w(TAG, "assess回调: 失败, message=$message")
                        ToastUtils.show(context, message ?: "评估生成失败")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "assess回调异常: ${e.message}", e)
            }
        }
        Log.d(TAG, "generateHealthAssessment: assess已发起，等待回调")
    }

    private fun showAssessmentDialog(healthEvaluation: String, lifeSuggestion: String) {
        Log.d(TAG, "showAssessmentDialog: 弹窗显示")
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
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_RECORD && resultCode == Activity.RESULT_OK) {
            loadRecordData()
            setResult(Activity.RESULT_OK)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: itemId=${item.itemId}")
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d(TAG, "点击返回键，finish")
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        super.onBackPressed()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        dbHelper.close()
    }
}
