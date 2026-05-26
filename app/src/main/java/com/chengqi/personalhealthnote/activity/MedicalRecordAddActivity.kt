package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicalRecordAddBinding
import com.chengqi.personalhealthnote.entity.MedicalRecord
import com.chengqi.personalhealthnote.utils.ImageCompressUtils
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MedicalRecordAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicalRecordAddBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter
    private var selectedMedicalTime: String = ""
    private val calendar = Calendar.getInstance()
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val selectedImages = mutableListOf<String>()

    companion object {
        private const val REQUEST_PERMISSION = 2001
        private const val REQUEST_PICK_IMAGE = 2002
        private const val MAX_IMAGE_COUNT = 9
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupListeners()
        initImageRecyclerView()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "新增就医记录"
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
                showDeleteImageConfirm(position)
            }
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupListeners() {
        binding.tvMedicalTime.setOnClickListener { showDateTimePicker() }
        binding.btnSubmit.setOnClickListener { submitRecord() }
        binding.btnAddImage.setOnClickListener { checkPermissionAndPickImage() }

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
                    ToastUtils.show(this@MedicalRecordAddActivity, "输入内容已达上限")
                }
            }
        })
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

    private fun submitRecord() {
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
        val currentTime = System.currentTimeMillis()

        val imageJson = if (selectedImages.isNotEmpty()) {
            val jsonArray = JSONArray()
            selectedImages.forEach { jsonArray.put(it) }
            jsonArray.toString()
        } else {
            ""
        }

        val record = MedicalRecord(
            medicalTime = selectedMedicalTime,
            hospital = hospital,
            doctor = doctor,
            symptoms = symptoms,
            diagnosisResult = diagnosisResult,
            checkItems = checkItems,
            medicines = medicines,
            imagePaths = imageJson,
            createTime = currentTime,
            updateTime = currentTime
        )

        val newId = dbHelper.insertMedicalRecord(record)
        if (newId > 0) {
            ToastUtils.show(this, "新增成功")
            setResult(RESULT_OK)
            finish()
        } else {
            ToastUtils.show(this, "保存失败，请重试")
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
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery()
            } else {
                ToastUtils.show(this, "需要存储权限才能选择图片")
            }
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
                } ?: intent.data?.let { uri ->
                    selectedUris.add(uri)
                }
                selectedUris.forEach { uri ->
                    val privatePath = copyImageToPrivateDir(uri)
                    if (privatePath != null) {
                        imageAdapter.addImage(privatePath)
                    }
                }
            }
        }
    }

    private fun copyImageToPrivateDir(uri: Uri): String? {
        val fileName = "medical_record_${System.currentTimeMillis()}.jpg"
        val path = ImageCompressUtils.compressToPrivateDir(this, uri, "medical_records", fileName)
        if (path == null) {
            ToastUtils.show(this, "图片处理失败")
        }
        return path
    }

    private fun showDeleteImageConfirm(position: Int) {
        DialogUtils.showConfirm(this, "删除图片", "确定要删除这张图片吗？", "删除") {
            val imagePath = selectedImages[position]
            File(imagePath).delete()
            imageAdapter.removeImage(position)
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
