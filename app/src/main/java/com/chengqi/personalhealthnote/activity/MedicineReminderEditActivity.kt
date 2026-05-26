package com.chengqi.personalhealthnote.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicineReminderEditBinding
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.chengqi.personalhealthnote.utils.AlarmScheduler
import com.chengqi.personalhealthnote.utils.CalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 用药提醒编辑Activity
 * 用于新增或编辑用药提醒
 */
class MedicineReminderEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicineReminderEditBinding
    private lateinit var dbHelper: DatabaseHelper

    private var reminderId: Long = 0
    private var isEditMode: Boolean = false
    private var existingCalendarEventIds: String = ""
    private var enableAppNotification: Boolean = true
    private var enableCalendarSync: Boolean = true

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 4001
        private const val REQUEST_CALENDAR_PERMISSION = 4002
    }

    private var selectedStartDate: String = ""
    private var selectedEndDate: String? = null
    private val remindTimes = mutableListOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    // 饭前饭后选项
    private val mealOptions = arrayOf("不限", "饭前", "饭后", "饭中")
    private val frequencyOptions = arrayOf("每日1次", "每日2次", "每日3次", "每日4次", "每日5次")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineReminderEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // 获取传入参数
        reminderId = intent.getLongExtra("reminder_id", 0)
        isEditMode = reminderId > 0

        initViews()
        setupSpinners()
        setupListeners()

        // 根据模式加载数据
        if (isEditMode) {
            loadReminderData()
        } else {
            // 新增模式，设置默认值
            selectedStartDate = dateFormat.format(calendar.time)
            binding.tvStartDate.text = selectedStartDate
            // 默认添加一个提醒时间
            addRemindTimeField("08:00")
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "编辑用药提醒" else "添加用药提醒"

        // 设置输入提示
        binding.etMedicineName.hint = "例如：阿莫西林"
        binding.etDosage.hint = "例如：1"
        binding.etUnit.hint = "例如：片、粒、ml"
        binding.etNotes.hint = "其他备注信息（选填）"

        // 提醒方式开关默认值
        binding.switchAppNotification.isChecked = true
        binding.switchCalendarSync.isChecked = true
    }

    /**
     * 设置下拉选择框
     */
    private fun setupSpinners() {
        // 饭前饭后选择
        val mealAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mealOptions)
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealTime.adapter = mealAdapter

        // 频率选择
        val frequencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencyOptions)
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = frequencyAdapter

        // 频率选择监听，动态添加/移除时间选择按钮
        binding.spinnerFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRemindTimeButtons(position + 1)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * 更新提醒时间按钮
     */
    private fun updateRemindTimeButtons(frequency: Int) {
        binding.layoutRemindTimes.removeAllViews()
        remindTimes.clear()

        // 根据频率添加时间选择按钮
        for (i in 0 until frequency) {
            val defaultTime = when (i) {
                0 -> "08:00"
                1 -> "12:00"
                2 -> "18:00"
                3 -> "21:00"
                else -> "08:00"
            }
            addRemindTimeButton(i, defaultTime)
        }
    }

    /**
     * 添加提醒时间按钮
     */
    private fun addRemindTimeButton(index: Int, defaultTime: String) {
        val timeButton = android.widget.Button(this).apply {
            text = defaultTime
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8
            }
            setOnClickListener {
                showTimePicker(this)
            }
        }

        // 保存时间到列表
        if (index < remindTimes.size) {
            remindTimes[index] = defaultTime
        } else {
            remindTimes.add(defaultTime)
        }

        binding.layoutRemindTimes.addView(timeButton)
    }

    /**
     * 添加提醒时间字段（用于编辑模式）
     */
    private fun addRemindTimeField(time: String) {
        val index = remindTimes.size
        addRemindTimeButton(index, time)
    }

    /**
     * 显示时间选择器
     */
    private fun showTimePicker(button: android.widget.Button) {
        val currentTime = button.text.toString().split(":")
        val hour = currentTime[0].toInt()
        val minute = currentTime[1].toInt()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                button.text = timeString

                // 更新remindTimes列表
                val index = binding.layoutRemindTimes.indexOfChild(button)
                if (index >= 0 && index < remindTimes.size) {
                    remindTimes[index] = timeString
                }
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 开始日期选择
        binding.cardStartDate.setOnClickListener {
            showDatePicker(true)
        }

        // 结束日期选择
        binding.cardEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // 长期用药开关
        binding.switchLongTerm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.cardEndDate.isEnabled = false
                binding.tvEndDate.text = "长期"
                binding.tvEndDate.alpha = 0.5f
                selectedEndDate = null
            } else {
                binding.cardEndDate.isEnabled = true
                binding.tvEndDate.alpha = 1.0f
                if (selectedEndDate == null) {
                    // 默认设置为开始日期后7天
                    val endCalendar = Calendar.getInstance()
                    endCalendar.time = dateFormat.parse(selectedStartDate) ?: Calendar.getInstance().time
                    endCalendar.add(Calendar.DAY_OF_MONTH, 7)
                    selectedEndDate = dateFormat.format(endCalendar.time)
                }
                binding.tvEndDate.text = selectedEndDate
            }
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            requestPermissionsAndSave()
        }

        // 系统日历开关：打开时请求日历权限
        binding.switchCalendarSync.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!hasCalendarPermission()) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR),
                        REQUEST_CALENDAR_PERMISSION
                    )
                }
            }
        }

        // APP通知开关：打开时请求通知权限
        binding.switchAppNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!hasNotificationPermission()) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    /**
     * 显示日期选择器
     * @param isStartDate 是否为开始日期
     */
    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) {
            selectedStartDate
        } else {
            selectedEndDate ?: selectedStartDate
        }

        try {
            val date = dateFormat.parse(currentDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            // 使用当前日期
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateString = dateFormat.format(calendar.time)

                if (isStartDate) {
                    selectedStartDate = dateString
                    binding.tvStartDate.text = dateString
                } else {
                    selectedEndDate = dateString
                    binding.tvEndDate.text = dateString
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // 如果是结束日期，限制不能早于开始日期
        if (!isStartDate) {
            try {
                val startDate = dateFormat.parse(selectedStartDate)
                if (startDate != null) {
                    datePickerDialog.datePicker.minDate = startDate.time
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }

        datePickerDialog.show()
    }

    /**
     * 加载提醒数据（编辑模式）
     */
    private fun loadReminderData() {
        val reminder = dbHelper.getMedicineReminderById(reminderId)
        if (reminder != null) {
            existingCalendarEventIds = reminder.calendarEventIds

            // 提醒方式开关：根据是否已有日历事件判断
            binding.switchCalendarSync.isChecked = reminder.calendarEventIds.isNotEmpty()

            // 设置药品名称
            binding.etMedicineName.setText(reminder.medicineName)

            // 设置剂量
            binding.etDosage.setText(reminder.dosage)
            binding.etUnit.setText(reminder.unit)

            // 设置频率
            val frequencyIndex = (reminder.frequency - 1).coerceIn(0, 4)
            binding.spinnerFrequency.setSelection(frequencyIndex)

            // 设置提醒时间
            remindTimes.clear()
            remindTimes.addAll(reminder.getAllRemindTimes())
            // 更新时间按钮显示
            updateRemindTimeButtonsFromList()

            // 设置饭前饭后
            val mealIndex = reminder.beforeAfterMeal.coerceIn(0, 3)
            binding.spinnerMealTime.setSelection(mealIndex)

            // 设置日期
            selectedStartDate = reminder.startDate
            binding.tvStartDate.text = selectedStartDate

            if (reminder.endDate == null) {
                // 长期用药
                binding.switchLongTerm.isChecked = true
            } else {
                selectedEndDate = reminder.endDate
                binding.tvEndDate.text = selectedEndDate
                binding.switchLongTerm.isChecked = false
            }

            // 设置备注
            if (reminder.notes.isNotEmpty()) {
                binding.etNotes.setText(reminder.notes)
            }
        } else {
            Toast.makeText(this, "提醒不存在", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 从remindTimes列表更新时间按钮
     */
    private fun updateRemindTimeButtonsFromList() {
        binding.layoutRemindTimes.removeAllViews()

        for (i in remindTimes.indices) {
            val timeButton = android.widget.Button(this).apply {
                text = remindTimes[i]
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginEnd = 8
                }
                setOnClickListener {
                    showTimePicker(this, i)
                }
            }
            binding.layoutRemindTimes.addView(timeButton)
        }
    }

    /**
     * 显示时间选择器（编辑模式）
     */
    private fun showTimePicker(button: android.widget.Button, index: Int) {
        val currentTime = button.text.toString().split(":")
        val hour = currentTime[0].toInt()
        val minute = currentTime[1].toInt()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                button.text = timeString
                remindTimes[index] = timeString
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun requestPermissionsAndSave() {
        enableAppNotification = binding.switchAppNotification.isChecked
        enableCalendarSync = binding.switchCalendarSync.isChecked

        if (enableCalendarSync && !hasCalendarPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR),
                REQUEST_CALENDAR_PERMISSION
            )
            return
        }

        if (enableAppNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
            return
        }

        saveReminder()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CALENDAR_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    saveReminder()
                } else {
                    binding.switchCalendarSync.isChecked = false
                    enableCalendarSync = false
                    Toast.makeText(this, "需要日历权限才能同步到系统日历", Toast.LENGTH_SHORT).show()
                    saveReminder()
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveReminder()
                } else {
                    binding.switchAppNotification.isChecked = false
                    enableAppNotification = false
                    Toast.makeText(this, "需要通知权限才能发送APP提醒", Toast.LENGTH_SHORT).show()
                    saveReminder()
                }
            }
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 保存提醒
     */
    private fun saveReminder() {
        // 获取输入值
        val medicineName = binding.etMedicineName.text.toString().trim()
        val dosage = binding.etDosage.text.toString().trim()
        val unit = binding.etUnit.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        // 验证必填项
        if (medicineName.isEmpty()) {
            Toast.makeText(this, "请输入药品名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (dosage.isEmpty()) {
            Toast.makeText(this, "请输入剂量", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取频率
        val frequency = binding.spinnerFrequency.selectedItemPosition + 1

        // 验证提醒时间
        if (remindTimes.size < frequency) {
            Toast.makeText(this, "请设置所有提醒时间", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取饭前饭后设置
        val beforeAfterMeal = binding.spinnerMealTime.selectedItemPosition

        // 验证日期
        if (selectedStartDate.isEmpty()) {
            Toast.makeText(this, "请选择开始日期", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查结束日期逻辑
        if (!binding.switchLongTerm.isChecked) {
            if (selectedEndDate == null) {
                Toast.makeText(this, "请选择结束日期或设为长期用药", Toast.LENGTH_SHORT).show()
                return
            }
            // 验证结束日期不能早于开始日期
            try {
                val start = dateFormat.parse(selectedStartDate)
                val end = dateFormat.parse(selectedEndDate!!)
                if (start != null && end != null && end.before(start)) {
                    Toast.makeText(this, "结束日期不能早于开始日期", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }

        val currentTime = System.currentTimeMillis()

        // 创建提醒对象
        val reminder = MedicineReminder(
            id = if (isEditMode) reminderId else 0,
            medicineName = medicineName,
            dosage = dosage,
            unit = unit,
            frequency = frequency,
            remindTime1 = remindTimes.getOrNull(0) ?: "08:00",
            remindTime2 = remindTimes.getOrNull(1),
            remindTime3 = remindTimes.getOrNull(2),
            remindTime4 = remindTimes.getOrNull(3),
            remindTime5 = remindTimes.getOrNull(4),
            isEnabled = true,
            startDate = selectedStartDate,
            endDate = if (binding.switchLongTerm.isChecked) null else selectedEndDate,
            beforeAfterMeal = beforeAfterMeal,
            notes = notes,
            updateTime = currentTime
        )

        // 保存到数据库
        val result = if (isEditMode) {
            dbHelper.updateMedicineReminder(reminder)
        } else {
            val newId = dbHelper.insertMedicineReminder(reminder)
            if (newId > 0) 1 else 0
        }

        if (result > 0) {
            val savedReminder = if (isEditMode) {
                reminder
            } else {
                dbHelper.getMedicineReminderById(reminder.id) ?: reminder
            }

            // 调度APP通知
            if (enableAppNotification) {
                if (isEditMode) {
                    val oldReminder = reminder.copy(calendarEventIds = existingCalendarEventIds)
                    AlarmScheduler.rescheduleReminder(this, oldReminder, savedReminder)
                } else {
                    AlarmScheduler.scheduleReminder(this, savedReminder)
                }
            } else {
                // 关闭APP通知时取消所有闹钟
                if (isEditMode) {
                    AlarmScheduler.cancelReminder(this, reminder.copy(calendarEventIds = existingCalendarEventIds))
                }
            }

            // 写入系统日历
            if (enableCalendarSync) {
                try {
                    val newEventIds = if (isEditMode) {
                        CalendarHelper.updateCalendarEvents(this, savedReminder, existingCalendarEventIds)
                    } else {
                        CalendarHelper.addCalendarEvents(this, savedReminder)
                    }
                    if (newEventIds.isNotEmpty()) {
                        dbHelper.updateMedicineReminder(savedReminder.copy(calendarEventIds = newEventIds))
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(this, "无日历权限，未同步到系统日历", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 关闭日历同步时删除已有日历事件
                if (isEditMode && existingCalendarEventIds.isNotEmpty()) {
                    try {
                        CalendarHelper.deleteCalendarEvents(this, existingCalendarEventIds)
                        dbHelper.updateMedicineReminder(savedReminder.copy(calendarEventIds = ""))
                    } catch (e: SecurityException) {
                        // 忽略
                    }
                }
            }

            Toast.makeText(
                this,
                if (isEditMode) "用药提醒已更新" else "用药提醒已添加",
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

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}