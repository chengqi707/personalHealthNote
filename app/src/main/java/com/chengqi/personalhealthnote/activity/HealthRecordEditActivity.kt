package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
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
import com.chengqi.personalhealthnote.adapter.ImageAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityHealthRecordEditBinding
import com.chengqi.personalhealthnote.entity.HealthRecord
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 健康记录编辑Activity
 * 用于新增或编辑健康记录
 */
class HealthRecordEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthRecordEditBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter

    private var recordId: Long = 0
    private var isEditMode: Boolean = false
    private var selectedDate: String = ""
    private val selectedImages = mutableListOf<String>() // 已选择的图片路径

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    companion object {
        private const val REQUEST_PERMISSION = 1001
        private const val REQUEST_PICK_IMAGE = 1002
        private const val MAX_IMAGE_COUNT = 9 // 最多选择9张图片
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthRecordEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // 获取传入参数
        recordId = intent.getLongExtra("record_id", 0)
        selectedDate = intent.getStringExtra("record_date") ?: ""
        isEditMode = recordId > 0

        initViews()
        setupListeners()
        initImageRecyclerView()

        // 根据模式加载数据
        if (isEditMode) {
            loadRecordData()
        } else {
            // 新增模式，设置默认日期
            if (selectedDate.isEmpty()) {
                selectedDate = dateFormat.format(calendar.time)
            }
            binding.tvDate.text = selectedDate
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "编辑健康记录" else "添加健康记录"

        // 设置输入提示
        binding.etWeight.hint = "例如：65.5"
        binding.etSystolicPressure.hint = "例如：120"
        binding.etDiastolicPressure.hint = "例如：80"
        binding.etHeartRate.hint = "例如：72"
        binding.etBloodSugar.hint = "例如：5.6"
        binding.etSleepDuration.hint = "例如：7.5"
        binding.etWaterIntake.hint = "例如：2000"
        binding.etSteps.hint = "例如：8000"
    }

    /**
     * 初始化图片列表
     */
    private fun initImageRecyclerView() {
        imageAdapter = ImageAdapter(
            imagePaths = selectedImages,
            onImageClick = { position ->
                // 点击查看大图，这里可以跳转到图片查看页面
                Toast.makeText(this, "查看图片 ${position + 1}", Toast.LENGTH_SHORT).show()
            },
            onImageDelete = { position ->
                // 删除图片
                showDeleteImageConfirm(position)
            }
        )
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 日期选择
        binding.cardDate.setOnClickListener {
            showDatePicker()
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveRecord()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // 添加图片按钮
        binding.btnAddImage.setOnClickListener {
            checkPermissionAndPickImage()
        }
    }

    /**
     * 显示日期选择器
     */
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormat.format(calendar.time)
                binding.tvDate.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * 加载记录数据（编辑模式）
     */
    private fun loadRecordData() {
        val record = dbHelper.getHealthRecordById(recordId)
        if (record != null) {
            selectedDate = record.recordDate
            binding.tvDate.text = selectedDate

            // 设置各项数值
            if (record.weight > 0) {
                binding.etWeight.setText(record.weight.toString())
            }
            if (record.systolicPressure > 0) {
                binding.etSystolicPressure.setText(record.systolicPressure.toString())
            }
            if (record.diastolicPressure > 0) {
                binding.etDiastolicPressure.setText(record.diastolicPressure.toString())
            }
            if (record.heartRate > 0) {
                binding.etHeartRate.setText(record.heartRate.toString())
            }
            if (record.bloodSugar > 0) {
                binding.etBloodSugar.setText(record.bloodSugar.toString())
            }
            if (record.sleepDuration > 0) {
                binding.etSleepDuration.setText(record.sleepDuration.toString())
            }
            if (record.waterIntake > 0) {
                binding.etWaterIntake.setText(record.waterIntake.toString())
            }
            if (record.steps > 0) {
                binding.etSteps.setText(record.steps.toString())
            }
            if (record.notes.isNotEmpty()) {
                binding.etNotes.setText(record.notes)
            }

            // 加载已有图片
            if (record.imagePaths.isNotEmpty()) {
                val jsonArray = JSONArray(record.imagePaths)
                val imagePaths = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    imagePaths.add(jsonArray.getString(i))
                }
                imageAdapter.addImages(imagePaths)
            }
        } else {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 保存记录
     */
    private fun saveRecord() {
        // 获取输入值
        val weight = binding.etWeight.text.toString().toFloatOrNull() ?: 0f
        val systolicPressure = binding.etSystolicPressure.text.toString().toIntOrNull() ?: 0
        val diastolicPressure = binding.etDiastolicPressure.text.toString().toIntOrNull() ?: 0
        val heartRate = binding.etHeartRate.text.toString().toIntOrNull() ?: 0
        val bloodSugar = binding.etBloodSugar.text.toString().toFloatOrNull() ?: 0f
        val sleepDuration = binding.etSleepDuration.text.toString().toFloatOrNull() ?: 0f
        val waterIntake = binding.etWaterIntake.text.toString().toIntOrNull() ?: 0
        val steps = binding.etSteps.text.toString().toIntOrNull() ?: 0
        val notes = binding.etNotes.text.toString()

        // 验证日期
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "请选择日期", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查是否至少填写了一项数据
        if (weight == 0f && systolicPressure == 0 && diastolicPressure == 0 &&
            heartRate == 0 && bloodSugar == 0f && sleepDuration == 0f &&
            waterIntake == 0 && steps == 0 && notes.isEmpty()) {
            Toast.makeText(this, "请至少填写一项健康数据", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证血压逻辑
        if (systolicPressure > 0 && diastolicPressure > 0) {
            if (systolicPressure <= diastolicPressure) {
                Toast.makeText(this, "收缩压必须大于舒张压", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val currentTime = System.currentTimeMillis()

        // 把图片路径转为JSON字符串
        val imageJson = if (selectedImages.isNotEmpty()) {
            val jsonArray = JSONArray()
            selectedImages.forEach { jsonArray.put(it) }
            jsonArray.toString()
        } else {
            ""
        }

        // 创建记录对象
        val record = HealthRecord(
            id = if (isEditMode) recordId else 0,
            recordDate = selectedDate,
            weight = weight,
            systolicPressure = systolicPressure,
            diastolicPressure = diastolicPressure,
            heartRate = heartRate,
            bloodSugar = bloodSugar,
            sleepDuration = sleepDuration,
            waterIntake = waterIntake,
            steps = steps,
            notes = notes,
            imagePaths = imageJson,
            updateTime = currentTime
        )

        // 保存到数据库
        val result = if (isEditMode) {
            dbHelper.updateHealthRecord(record)
        } else {
            // 检查该日期是否已有记录
            val existingRecord = dbHelper.getHealthRecordByDate(selectedDate)
            if (existingRecord != null) {
                // 询问是否覆盖
                AlertDialog.Builder(this)
                    .setTitle("记录已存在")
                    .setMessage("该日期已有健康记录，是否覆盖？")
                    .setPositiveButton("覆盖") { _, _ ->
                        val updatedRecord = record.copy(id = existingRecord.id)
                        dbHelper.updateHealthRecord(updatedRecord)
                        Toast.makeText(this, "记录已更新", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return
            }
            val newId = dbHelper.insertHealthRecord(record)
            if (newId > 0) 1 else 0
        }

        if (result > 0) {
            Toast.makeText(
                this,
                if (isEditMode) "记录已更新" else "记录已添加",
                Toast.LENGTH_SHORT
            ).show()
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

    /**
     * 检查权限并选择图片
     */
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

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery()
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 从相册选择图片
     */
    private fun pickImageFromGallery() {
        if (selectedImages.size >= MAX_IMAGE_COUNT) {
            Toast.makeText(this, "最多只能选择${MAX_IMAGE_COUNT}张图片", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 允许多选
        }
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGE)
    }

    /**
     * 处理图片选择结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.let { intent ->
                val selectedUris = mutableListOf<Uri>()

                // 处理多选
                intent.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        if (selectedImages.size + selectedUris.size < MAX_IMAGE_COUNT) {
                            selectedUris.add(clipData.getItemAt(i).uri)
                        } else {
                            break
                        }
                    }
                } ?: intent.data?.let { uri ->
                    // 处理单选
                    selectedUris.add(uri)
                }

                // 复制图片到私有目录并添加到列表
                selectedUris.forEach { uri ->
                    val privatePath = copyImageToPrivateDir(uri)
                    if (privatePath != null) {
                        imageAdapter.addImage(privatePath)
                    }
                }
            }
        }
    }

    /**
     * 复制图片到APP私有目录
     * @param uri 图片的Uri
     * @return 私有目录中的路径，失败返回null
     */
    private fun copyImageToPrivateDir(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val fileName = "health_record_${System.currentTimeMillis()}.jpg"
            val privateDir = getDir("records", MODE_PRIVATE)
            val destFile = File(privateDir, fileName)

            val outputStream = FileOutputStream(destFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * 显示删除图片确认对话框
     */
    private fun showDeleteImageConfirm(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除图片")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                // 删除本地文件
                val imagePath = selectedImages[position]
                File(imagePath).delete()
                // 从列表中删除
                imageAdapter.removeImage(position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}