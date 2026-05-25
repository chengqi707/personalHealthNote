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
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
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

        // 输入字数限制提示
        setupMaxLengthWatcher(binding.etSymptoms, 500)
        setupMaxLengthWatcher(binding.etDiagnosisResult, 500)
        setupMaxLengthWatcher(binding.etCheckItems, 300)
        setupMaxLengthWatcher(binding.etMedicines, 300)
    }

    private fun setupMaxLengthWatcher(editText: android.widget.EditText, maxLength: Int) {
        var lastLength = 0
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastLength = s?.length ?: 0
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s != null && s.length == maxLength && lastLength < maxLength) {
                    ToastUtils.show(this@MedicalRecordEditActivity, "输入内容已达上限")
                }
            }
        })
    }

    private fun loadRecordData() {
        val record = dbHelper.getMedicalRecordById(recordId)
        if (record == null) {
            ToastUtils.show(this, "记录不存在")
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
            ToastUtils.show(this, "请填写$firstEmptyField")
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
                DialogUtils.showConfirm(
                    this,
                    "提示",
                    "是否放弃修改？",
                    "放弃"
                ) { finish() }
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
