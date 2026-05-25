package com.chengqi.personalhealthnote.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.chengqi.personalhealthnote.R
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chengqi.personalhealthnote.adapter.HealthRecordAdapter
import com.chengqi.personalhealthnote.adapter.MedicalRecordAdapter
import com.chengqi.personalhealthnote.adapter.MedicineReminderAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMainBinding
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicalRecord
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.chengqi.personalhealthnote.network.ApiService
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
import com.chengqi.personalhealthnote.utils.TokenManager
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 主界面Activity
 * Tab1: 就医记录（主Tab），Tab2: 健康记录，Tab3: 用药提醒
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var medicalRecordAdapter: MedicalRecordAdapter
    private lateinit var healthRecordAdapter: HealthRecordAdapter
    private lateinit var medicineReminderAdapter: MedicineReminderAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 就医记录筛选条件：0=全部，1=近1个月，2=近3个月，3=近1年
    private var medicalRecordFilter = 0

    companion object {
        const val REQUEST_ADD_MEDICAL_RECORD = 1001
        const val REQUEST_EDIT_MEDICAL_RECORD = 1002
        const val REQUEST_ADD_HEALTH_RECORD = 1003
        const val REQUEST_EDIT_HEALTH_RECORD = 1004
        const val REQUEST_ADD_MEDICINE = 1005
        const val REQUEST_EDIT_MEDICINE = 1006
        const val REQUEST_LOGIN = 1007
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerViews()
        setupTabLayout()
        setupListeners()
        loadData()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "我的就医记录"
    }

    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("就医记录"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("健康记录"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("用药提醒"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showMedicalRecordTab()
                    1 -> showHealthRecordTab()
                    2 -> showMedicineReminderTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 默认显示就医记录Tab
        showMedicalRecordTab()
    }

    private fun showMedicalRecordTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadMedicalRecords()
    }

    private fun showHealthRecordTab() {
        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadHealthRecords()
    }

    private fun showMedicineReminderTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_menu_add)
        loadMedicineReminders()
    }

    private fun setupRecyclerViews() {
        // 就医记录RecyclerView（复用健康记录的RecyclerView，按Tab切换adapter）
        medicalRecordAdapter = MedicalRecordAdapter(
            onItemClick = { record ->
                val intent = Intent(this, MedicalRecordDetailActivity::class.java).apply {
                    putExtra("record_id", record.id)
                }
                startActivityForResult(intent, REQUEST_EDIT_MEDICAL_RECORD)
            },
            onItemLongClick = { record ->
                showMedicalRecordLongClickDialog(record)
                true
            }
        )

        // 健康记录RecyclerView
        healthRecordAdapter = HealthRecordAdapter(
            onItemClick = { record ->
                val intent = Intent(this, HealthRecordDetailActivity::class.java).apply {
                    putExtra("record_id", record.id)
                }
                startActivityForResult(intent, REQUEST_EDIT_HEALTH_RECORD)
            },
            onItemLongClick = { record ->
                showDeleteConfirmDialog(record)
                true
            }
        )

        binding.recyclerViewHealthRecords.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // 用药提醒RecyclerView
        medicineReminderAdapter = MedicineReminderAdapter(
            onItemClick = { reminder ->
                val intent = Intent(this, MedicineReminderEditActivity::class.java).apply {
                    putExtra("reminder_id", reminder.id)
                }
                startActivityForResult(intent, REQUEST_EDIT_MEDICINE)
            },
            onItemLongClick = { reminder ->
                showDeleteMedicineConfirmDialog(reminder)
                true
            },
            onSwitchChanged = { reminder, isEnabled ->
                dbHelper.toggleMedicineReminder(reminder.id, isEnabled)
                medicineReminderAdapter.updateReminderStatus(reminder.id, isEnabled)
            }
        )

        binding.recyclerViewMedicineReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = medicineReminderAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            when (binding.tabLayout.selectedTabPosition) {
                0 -> {
                    val intent = Intent(this, MedicalRecordAddActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_MEDICAL_RECORD)
                }
                1 -> {
                    val intent = Intent(this, HealthRecordEditActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_HEALTH_RECORD)
                }
                2 -> {
                    val intent = Intent(this, MedicineReminderEditActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_MEDICINE)
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun loadData() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> loadMedicalRecords()
            1 -> loadHealthRecords()
            2 -> loadMedicineReminders()
        }
    }

    private fun loadMedicalRecords() {
        val records = when (medicalRecordFilter) {
            0 -> dbHelper.getAllMedicalRecords()
            else -> {
                val cal = Calendar.getInstance()
                when (medicalRecordFilter) {
                    1 -> cal.add(Calendar.MONTH, -1)
                    2 -> cal.add(Calendar.MONTH, -3)
                    3 -> cal.add(Calendar.YEAR, -1)
                }
                val startTime = dateTimeFormat.format(cal.time)
                val endTime = dateTimeFormat.format(Calendar.getInstance().time)
                dbHelper.getMedicalRecordsByTimeRange(startTime, endTime)
            }
        }
        medicalRecordAdapter.setData(records)

        binding.recyclerViewHealthRecords.adapter = medicalRecordAdapter
        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE

        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无就医记录，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun loadHealthRecords() {
        val records = dbHelper.getAllHealthRecords()
        binding.recyclerViewHealthRecords.adapter = healthRecordAdapter
        healthRecordAdapter.setData(records)

        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE

        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无健康记录，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun loadMedicineReminders() {
        val reminders = dbHelper.getAllMedicineReminders()
        medicineReminderAdapter.setData(reminders)

        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.VISIBLE

        if (reminders.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无用药提醒，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    /**
     * 就医记录长按弹窗：删除、编辑
     */
    private fun showMedicalRecordLongClickDialog(record: MedicalRecord) {
        DialogUtils.showOptions(this, "操作", arrayOf("删除", "编辑")) { which ->
            when (which) {
                0 -> showDeleteMedicalRecordDialog(record)
                1 -> {
                    val intent = Intent(this, MedicalRecordEditActivity::class.java)
                    intent.putExtra("record_id", record.id)
                    startActivityForResult(intent, REQUEST_EDIT_MEDICAL_RECORD)
                }
            }
        }
    }

    private fun showDeleteMedicalRecordDialog(record: MedicalRecord) {
        DialogUtils.showConfirm(
            this,
            "删除确认",
            "确定要删除这条记录吗？删除后不可恢复",
            "确定"
        ) {
            val deletedRows = dbHelper.deleteMedicalRecord(record.id.toInt())
            if (deletedRows > 0) {
                medicalRecordAdapter.removeRecord(record.id)
                if (medicalRecordAdapter.getData().isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "暂无就医记录，点击右下角添加"
                }
                ToastUtils.show(this, "删除成功")
            }
        }
    }

    private fun showDeleteConfirmDialog(record: HealthRecord) {
        DialogUtils.showConfirm(
            this,
            "删除确认",
            "确定要删除 ${record.recordDate} 的健康记录吗？此操作不可恢复。",
            "删除"
        ) {
            val deletedRows = dbHelper.deleteHealthRecord(record.id)
            if (deletedRows > 0) {
                healthRecordAdapter.removeRecord(record.id)
                if (healthRecordAdapter.getData().isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "暂无健康记录，点击右下角添加"
                }
            }
        }
    }

    private fun showDeleteMedicineConfirmDialog(reminder: MedicineReminder) {
        DialogUtils.showConfirm(
            this,
            "删除确认",
            "确定要删除 \"${reminder.medicineName}\" 的用药提醒吗？此操作不可恢复。",
            "删除"
        ) {
            val deletedRows = dbHelper.deleteMedicineReminder(reminder.id)
            if (deletedRows > 0) {
                medicineReminderAdapter.removeReminder(reminder.id)
                if (medicineReminderAdapter.getData().isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "暂无用药提醒，点击右下角添加"
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_MEDICAL_RECORD, REQUEST_EDIT_MEDICAL_RECORD -> {
                    loadMedicalRecords()
                }
                REQUEST_ADD_HEALTH_RECORD, REQUEST_EDIT_HEALTH_RECORD -> {
                    loadHealthRecords()
                }
                REQUEST_ADD_MEDICINE, REQUEST_EDIT_MEDICINE -> {
                    loadMedicineReminders()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_sync -> {
                syncData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        if (binding.tabLayout.selectedTabPosition != 0) {
            ToastUtils.show(this, "筛选仅支持就医记录")
            return
        }
        val options = arrayOf("全部", "近1个月", "近3个月", "近1年")
        DialogUtils.showOptions(this, "按时间筛选", options) { which ->
            medicalRecordFilter = which
            loadMedicalRecords()
        }
    }

    private fun syncData() {
        if (!TokenManager.isLogin(this)) {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_LOGIN)
            return
        }
        ToastUtils.show(this, "开始同步...")
        val context = this
        Thread {
            try {
                val lastSyncTime = TokenManager.getLastSyncTime(context)
                var syncSuccess = true
                ApiService.pullHealthRecords(context, lastSyncTime) { success: Boolean, message: String?, records: List<HealthRecord>? ->
                    if (success && records != null) {
                        dbHelper.syncHealthRecordsFromServer(records)
                    } else {
                        syncSuccess = false
                        runOnUiThread {
                            ToastUtils.show(context, "拉取健康记录失败：$message")
                        }
                    }
                }
                ApiService.pullMedicineReminders(context, lastSyncTime) { success: Boolean, message: String?, reminders: List<MedicineReminder>? ->
                    if (success && reminders != null) {
                        dbHelper.syncMedicineRemindersFromServer(reminders)
                    } else {
                        syncSuccess = false
                        runOnUiThread {
                            ToastUtils.show(context, "拉取用药提醒失败：$message")
                        }
                    }
                }
                val unsyncedHealth = dbHelper.getUnsyncedHealthRecords()
                if (unsyncedHealth.isNotEmpty()) {
                    ApiService.pushHealthRecords(context, unsyncedHealth) { success: Boolean, message: String?, count: Int? ->
                        if (success && count != null) {
                            val ids = unsyncedHealth.map { it.id }
                            dbHelper.markHealthRecordsSynced(ids)
                        } else {
                            syncSuccess = false
                            runOnUiThread {
                                ToastUtils.show(context, "上传健康记录失败：$message")
                            }
                        }
                    }
                }
                val unsyncedMedicine = dbHelper.getUnsyncedMedicineReminders()
                if (unsyncedMedicine.isNotEmpty()) {
                    ApiService.pushMedicineReminders(context, unsyncedMedicine) { success: Boolean, message: String?, count: Int? ->
                        if (success && count != null) {
                            val ids = unsyncedMedicine.map { it.id }
                            dbHelper.markMedicineRemindersSynced(ids)
                        } else {
                            syncSuccess = false
                            runOnUiThread {
                                ToastUtils.show(context, "上传用药提醒失败：$message")
                            }
                        }
                    }
                }
                if (syncSuccess) {
                    TokenManager.updateLastSyncTime(context)
                    runOnUiThread {
                        loadData()
                        ToastUtils.show(context, "同步成功")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    ToastUtils.show(context, "同步失败：${e.message}")
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
