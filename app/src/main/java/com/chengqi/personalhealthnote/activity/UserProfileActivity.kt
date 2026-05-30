package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityUserProfileBinding
import com.chengqi.personalhealthnote.entity.UserProfile
import com.chengqi.personalhealthnote.utils.ToastUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var dbHelper: DatabaseHelper
    private var selectedBirthDate: String = ""
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "个人健康档案"

        setupGenderSpinner()
        setupBirthDatePicker()
        setupSaveButton()
        loadProfile()
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            com.chengqi.personalhealthnote.R.array.gender_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }

    private fun setupBirthDatePicker() {
        binding.tvBirthDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedBirthDate = dateFormat.format(calendar.time)
                    binding.tvBirthDate.text = selectedBirthDate
                    binding.tvBirthDate.setTextColor(getColor(com.chengqi.personalhealthnote.R.color.textPrimary))
                },
                calendar.get(Calendar.YEAR) - 30,
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfile() {
        val profile = dbHelper.getUserProfile()
        if (profile != null) {
            // 性别
            val genderIndex = when (profile.gender) {
                "male" -> 1
                "female" -> 2
                "other" -> 3
                else -> 0
            }
            binding.spinnerGender.setSelection(genderIndex)

            // 出生日期
            if (profile.birthDate.isNotEmpty()) {
                selectedBirthDate = profile.birthDate
                binding.tvBirthDate.text = selectedBirthDate
                binding.tvBirthDate.setTextColor(getColor(com.chengqi.personalhealthnote.R.color.textPrimary))
            }

            // 身高体重
            if (profile.height > 0) binding.etHeight.setText(profile.height.toString())
            if (profile.weight > 0) binding.etWeight.setText(profile.weight.toString())

            // 健康信息
            binding.etAllergies.setText(profile.allergies)
            binding.etChronicDiseases.setText(profile.chronicDiseases)
            binding.etFamilyHistory.setText(profile.familyHistory)
        }
    }

    private fun saveProfile() {
        val gender = when (binding.spinnerGender.selectedItemPosition) {
            1 -> "male"
            2 -> "female"
            3 -> "other"
            else -> ""
        }

        val heightStr = binding.etHeight.text.toString().trim()
        val weightStr = binding.etWeight.text.toString().trim()
        val height = if (heightStr.isNotEmpty()) heightStr.toFloat() else 0f
        val weight = if (weightStr.isNotEmpty()) weightStr.toFloat() else 0f

        val profile = UserProfile(
            gender = gender,
            birthDate = selectedBirthDate,
            height = height,
            weight = weight,
            allergies = binding.etAllergies.text.toString().trim(),
            chronicDiseases = binding.etChronicDiseases.text.toString().trim(),
            familyHistory = binding.etFamilyHistory.text.toString().trim(),
            updateTime = System.currentTimeMillis(),
            isSync = 0
        )

        val result = dbHelper.saveUserProfile(profile)
        if (result > 0) {
            ToastUtils.show(this, "保存成功")
            setResult(RESULT_OK)
            finish()
        } else {
            ToastUtils.show(this, "保存失败，请重试")
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
