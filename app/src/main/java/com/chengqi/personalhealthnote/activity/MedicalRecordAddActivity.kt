package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicalRecordAddBinding
import com.chengqi.personalhealthnote.entity.MedicalRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 新增就医记录Activity
 * 按 PRD §4.2 实现
 */
class MedicalRecordAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicalRecordAddBinding
    private lateinit var dbHelper: DatabaseHelper
    private var selectedMedicalTime: String = ""
    private val calendar = Calendar.getInstance()
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "新增就医记录"
    }

    private fun setupListeners() {
        // 就医时间选择
        binding.tvMedicalTime.setOnClickListener {
            showDateTimePicker()
        }

        // 提交按钮
        binding.btnSubmit.setOnClickListener {
            submitRecord()
        }
    }

    /**
     * 显示日期时间选择器
     */
    private fun showDateTimePicker() {
        // 先选日期
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // 再选时间
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                        selectedMedicalTime = dateTimeFormat.format(calendar.time)
                        binding.tvMedicalTime.text = selectedMedicalTime
                        binding.tvMedicalTime.setTextColor(getColor(R.color.textPrimary))
                        binding.tvMedicalTime.setBackgroundResource(R.drawable.bg_edit_text)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // 禁止选择未来时间
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    /**
     * 提交记录
     */
    private fun submitRecord() {
        // 校验必填项
        var hasError = false

        if (selectedMedicalTime.isEmpty()) {
            binding.tvMedicalTime.setBackgroundResource(R.drawable.bg_edit_text_error)
            hasError = true
        }

        val hospital = binding.etHospital.text.toString().trim()
        if (hospital.isEmpty()) {
            binding.etHospital.setBackgroundResource(R.drawable.bg_edit_text_error)
            hasError = true
        }

        val symptoms = binding.etSymptoms.text.toString().trim()
        if (symptoms.isEmpty()) {
            binding.etSymptoms.setBackgroundResource(R.drawable.bg_edit_text_error)
            hasError = true
        }

        val diagnosisResult = binding.etDiagnosisResult.text.toString().trim()
        if (diagnosisResult.isEmpty()) {
            binding.etDiagnosisResult.setBackgroundResource(R.drawable.bg_edit_text_error)
            hasError = true
        }

        if (hasError) {
            val firstEmptyField = when {
                selectedMedicalTime.isEmpty() -> "就医时间"
                hospital.isEmpty() -> "就诊医院"
                symptoms.isEmpty() -> "症状描述"
                diagnosisResult.isEmpty() -> "就诊结果"
                else -> ""
            }
            Toast.makeText(this, "请填写$firstEmptyField", Toast.LENGTH_SHORT).show()
            return
        }

        val doctor = binding.etDoctor.text.toString().trim()
        val checkItems = binding.etCheckItems.text.toString().trim()
        val medicines = binding.etMedicines.text.toString().trim()
        val currentTime = System.currentTimeMillis()

        val record = MedicalRecord(
            medicalTime = selectedMedicalTime,
            hospital = hospital,
            doctor = doctor,
            symptoms = symptoms,
            diagnosisResult = diagnosisResult,
            checkItems = checkItems,
            medicines = medicines,
            createTime = currentTime,
            updateTime = currentTime
        )

        val newId = dbHelper.insertMedicalRecord(record)
        if (newId > 0) {
            Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show()
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
