package com.chengqi.personalhealthnote.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMedicineReminderDetailBinding
import com.chengqi.personalhealthnote.utils.AlarmScheduler
import com.chengqi.personalhealthnote.utils.CalendarHelper
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils

class MedicineReminderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicineReminderDetailBinding
    private lateinit var dbHelper: DatabaseHelper
    private var reminderId: Long = 0

    companion object {
        const val REQUEST_EDIT_REMINDER = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineReminderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        reminderId = intent.getLongExtra("reminder_id", 0)
        if (reminderId == 0L) {
            ToastUtils.show(this, "提醒不存在")
            finish()
            return
        }

        initViews()
        setupListeners()
        loadData()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "用药提醒详情"
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, MedicineReminderEditActivity::class.java)
            intent.putExtra("reminder_id", reminderId)
            startActivityForResult(intent, REQUEST_EDIT_REMINDER)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun loadData() {
        val reminder = dbHelper.getMedicineReminderById(reminderId)
        if (reminder == null) {
            ToastUtils.show(this, "提醒不存在")
            finish()
            return
        }

        binding.tvMedicineName.text = reminder.medicineName
        binding.tvDosage.text = reminder.getFullDosage()
        binding.tvFrequency.text = reminder.getFrequencyText()
        binding.tvRemindTime.text = reminder.getAllRemindTimes().joinToString("、")
        binding.tvMealTime.text = reminder.getMealTimeText()
        binding.tvDateRange.text = "${reminder.startDate} 至 ${reminder.endDate ?: "长期"}"
        binding.tvEnabledStatus.text = if (reminder.isEnabled) "已启用" else "已停用"
        binding.tvEnabledStatus.setTextColor(if (reminder.isEnabled) {
            getColor(R.color.primary)
        } else {
            getColor(R.color.textHint)
        })
        binding.tvNotes.text = reminder.notes.ifEmpty { "暂无备注" }
    }

    private fun showDeleteConfirmDialog() {
        val reminder = dbHelper.getMedicineReminderById(reminderId) ?: return
        DialogUtils.showConfirm(
            this,
            "删除确认",
            "确定要删除 \"${reminder.medicineName}\" 的用药提醒吗？此操作不可恢复。",
            "删除"
        ) {
            val deletedRows = dbHelper.deleteMedicineReminder(reminderId)
            if (deletedRows > 0) {
                try {
                    AlarmScheduler.cancelReminder(this, reminder)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    if (reminder.calendarEventIds.isNotEmpty()) {
                        CalendarHelper.deleteCalendarEvents(this, reminder.calendarEventIds)
                    } else {
                        CalendarHelper.deleteCalendarEventsByMedicineName(this, reminder.medicineName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                ToastUtils.show(this, "删除成功")
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                ToastUtils.show(this, "删除失败")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_REMINDER && resultCode == Activity.RESULT_OK) {
            loadData()
            setResult(Activity.RESULT_OK)
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
