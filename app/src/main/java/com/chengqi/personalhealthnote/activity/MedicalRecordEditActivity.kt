package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicalRecordEditBinding
import com.chengqi.personalhealthnote.entity.MedicalRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 编辑就医记录Activity
 * 按 PRD §4.4 实现
 */
class MedicalRecordEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicalRecordEditBinding
    private lateinit var dbHelper: DatabaseHelper
    private var recordId: Long = 0
    private var selectedMedicalTime: String = ""
    private val calendar = Calendar.getInstance()
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

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

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑就医记录"
    }

    private fun setupListeners() {
        // 就医时间选择
        binding.tvMedicalTime.setOnClickListener {
            showDateTimePicker()
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveRecord()
        }
    }

    private fun loadRecordData() {
        val record = dbHelper.getMedicalRecordById(recordId)
        if (record == null) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 回显数据
        selectedMedicalTime = record.medicalTime
        binding.tvMedicalTime.text = selectedMedicalTime
        binding.etHospital.setText(record.hospital)
        binding.etDoctor.setText(record.doctor)
        binding.etSymptoms.setText(record.symptoms)
        binding.etDiagnosisResult.setText(record.diagnosisResult)
        binding.etCheckItems.setText(record.checkItems)
        binding.etMedicines.setText(record.medicines)
    }

    private fun showDateTimePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
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
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveRecord() {
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

        val record = MedicalRecord(
            id = recordId,
            medicalTime = selectedMedicalTime,
            hospital = hospital,
            doctor = doctor,
            symptoms = symptoms,
            diagnosisResult = diagnosisResult,
            checkItems = checkItems,
            medicines = medicines,
            updateTime = System.currentTimeMillis()
        )

        // updateMedicalRecord 会自动清空评估缓存
        val result = dbHelper.updateMedicalRecord(record)
        if (result > 0) {
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 返回键弹出确认弹窗
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否放弃修改？")
                    .setPositiveButton("放弃") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("取消", null)
                    .show()
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
