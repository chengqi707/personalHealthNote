package com.chengqi.personalhealthnote.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.chengqi.personalhealthnote.R
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chengqi.personalhealthnote.adapter.HealthRecordAdapter
import com.chengqi.personalhealthnote.adapter.MedicalRecordAdapter
import com.chengqi.personalhealthnote.adapter.MedicineReminderAdapter
import com.chengqi.personalhealthnote.adapter.PhysicalExamReportAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMainBinding
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicalRecord
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.chengqi.personalhealthnote.entity.PhysicalExamReport
import com.chengqi.personalhealthnote.network.ApiService
import com.chengqi.personalhealthnote.utils.AlarmScheduler
import com.chengqi.personalhealthnote.utils.CalendarHelper
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.ToastUtils
import com.chengqi.personalhealthnote.utils.TokenManager
import com.chengqi.personalhealthnote.utils.AppLockManager
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var medicalRecordAdapter: MedicalRecordAdapter
    private lateinit var healthRecordAdapter: HealthRecordAdapter
    private lateinit var medicineReminderAdapter: MedicineReminderAdapter
    private lateinit var examReportAdapter: PhysicalExamReportAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 就医记录筛选条件：0=全部，1=近1个月，2=近3个月，3=近1年
    private var medicalRecordFilter = 0
    private var currentSearchQuery = ""

    // 排序方向：0=由近到远(DESC)，1=由远到近(ASC)
    private var medicalRecordSortOrder = 0
    private var healthRecordSortOrder = 0
    private var medicineReminderSortOrder = 0

    companion object {
        const val REQUEST_ADD_MEDICAL_RECORD = 1001
        const val REQUEST_EDIT_MEDICAL_RECORD = 1002
        const val REQUEST_ADD_HEALTH_RECORD = 1003
        const val REQUEST_EDIT_HEALTH_RECORD = 1004
        const val REQUEST_ADD_MEDICINE = 1005
        const val REQUEST_EDIT_MEDICINE = 1006
        const val REQUEST_LOGIN = 1007
        const val REQUEST_APP_LOCK = 1008
        const val REQUEST_ADD_EXAM_REPORT = 1009
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查应用锁
        if (AppLockManager.isLockEnabled(this) && AppLockManager.isPinSet(this)) {
            startActivityForResult(Intent(this, AppLockActivity::class.java), REQUEST_APP_LOCK)
        }

        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerViews()
        setupTabLayout()
        setupListeners()
        setupBackPressedHandler()
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
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("体检报告"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("统计"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                exitSelectionMode()
                currentSearchQuery = ""
                medicalRecordSortOrder = 0
                healthRecordSortOrder = 0
                medicineReminderSortOrder = 0
                searchView?.queryHint = getSearchHint()
                when (tab?.position) {
                    0 -> showMedicalRecordTab()
                    1 -> showHealthRecordTab()
                    2 -> showMedicineReminderTab()
                    3 -> showExamReportTab()
                    4 -> showStatisticsTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        showMedicalRecordTab()
    }

    private fun showMedicalRecordTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.recyclerViewExamReports.visibility = View.GONE
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadMedicalRecords()
    }

    private fun showHealthRecordTab() {
        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.recyclerViewExamReports.visibility = View.GONE
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadHealthRecords()
    }

    private fun showMedicineReminderTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.VISIBLE
        binding.recyclerViewExamReports.visibility = View.GONE
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_menu_add)
        loadMedicineReminders()
    }

    private fun showExamReportTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.recyclerViewExamReports.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadExamReports()
    }

    private fun showStatisticsTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.recyclerViewExamReports.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        startActivity(Intent(this, StatisticsActivity::class.java))
        binding.tabLayout.getTabAt(0)?.select()
    }

    private fun setupRecyclerViews() {
        medicalRecordAdapter = MedicalRecordAdapter(
            onItemClick = { record ->
                if (medicalRecordAdapter.isInSelectionMode()) {
                    medicalRecordAdapter.toggleSelection(record.id)
                } else {
                    val intent = Intent(this, MedicalRecordDetailActivity::class.java).apply {
                        putExtra("record_id", record.id)
                    }
                    startActivityForResult(intent, REQUEST_EDIT_MEDICAL_RECORD)
                }
            },
            onItemLongClick = { record ->
                if (medicalRecordAdapter.isInSelectionMode()) {
                    medicalRecordAdapter.toggleSelection(record.id)
                } else {
                    showMedicalRecordLongClickDialog(record)
                }
                true
            }
        )
        medicalRecordAdapter.setOnSelectionChangedListener { count ->
            updateSelectionTitle(count)
        }

        healthRecordAdapter = HealthRecordAdapter(
            onItemClick = { record ->
                if (healthRecordAdapter.isInSelectionMode()) {
                    healthRecordAdapter.toggleSelection(record.id)
                } else {
                    val intent = Intent(this, HealthRecordDetailActivity::class.java).apply {
                        putExtra("record_id", record.id)
                    }
                    startActivityForResult(intent, REQUEST_EDIT_HEALTH_RECORD)
                }
            },
            onItemLongClick = { record ->
                if (healthRecordAdapter.isInSelectionMode()) {
                    healthRecordAdapter.toggleSelection(record.id)
                } else {
                    showDeleteConfirmDialog(record)
                }
                true
            }
        )
        healthRecordAdapter.setOnSelectionChangedListener { count ->
            updateSelectionTitle(count)
        }

        binding.recyclerViewHealthRecords.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        medicineReminderAdapter = MedicineReminderAdapter(
            onItemClick = { reminder ->
                if (medicineReminderAdapter.isInSelectionMode()) {
                    medicineReminderAdapter.toggleSelection(reminder.id)
                } else {
                    val intent = Intent(this, MedicineReminderDetailActivity::class.java).apply {
                        putExtra("reminder_id", reminder.id)
                    }
                    startActivityForResult(intent, REQUEST_EDIT_MEDICINE)
                }
            },
            onItemLongClick = { reminder ->
                if (medicineReminderAdapter.isInSelectionMode()) {
                    medicineReminderAdapter.toggleSelection(reminder.id)
                } else {
                    showDeleteMedicineConfirmDialog(reminder)
                }
                true
            },
            onSwitchChanged = { reminder, isEnabled ->
                dbHelper.toggleMedicineReminder(reminder.id, isEnabled)
                medicineReminderAdapter.updateReminderStatus(reminder.id, isEnabled)
                if (isEnabled) {
                    AlarmScheduler.scheduleReminder(this, reminder)
                } else {
                    AlarmScheduler.cancelReminder(this, reminder)
                }
            }
        )
        medicineReminderAdapter.setOnSelectionChangedListener { count ->
            updateSelectionTitle(count)
        }

        binding.recyclerViewMedicineReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = medicineReminderAdapter
        }

        examReportAdapter = PhysicalExamReportAdapter(
            onItemClick = { report ->
                val intent = Intent(this, PhysicalExamReportDetailActivity::class.java)
                intent.putExtra("report_id", report.id)
                startActivity(intent)
            }
        )
        binding.recyclerViewExamReports.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = examReportAdapter
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
                3 -> {
                    val intent = Intent(this, PhysicalExamReportAddActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_EXAM_REPORT)
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.tvSelectAll.setOnClickListener {
            when (binding.tabLayout.selectedTabPosition) {
                0 -> medicalRecordAdapter.selectAll()
                1 -> healthRecordAdapter.selectAll()
                2 -> medicineReminderAdapter.selectAll()
                3 -> examReportAdapter.selectAll()
            }
        }

        binding.tvBatchDelete.setOnClickListener {
            performBatchDelete()
        }
    }

    private fun loadData() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> loadMedicalRecords()
            1 -> loadHealthRecords()
            2 -> loadMedicineReminders()
            3 -> loadExamReports()
            4 -> { /* 统计Tab跳转独立Activity，此处无需操作 */ }
        }
    }

    private fun getSearchHint(): String {
        return when (binding.tabLayout.selectedTabPosition) {
            0 -> "搜索医院、症状、诊断..."
            1 -> "搜索日期、备注..."
            2 -> "搜索药品名、备注..."
            3 -> "搜索体检报告..."
            else -> "搜索..."
        }
    }

    private fun toggleSortOrder() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> {
                medicalRecordSortOrder = if (medicalRecordSortOrder == 0) 1 else 0
                loadMedicalRecords()
            }
            1 -> {
                healthRecordSortOrder = if (healthRecordSortOrder == 0) 1 else 0
                loadHealthRecords()
            }
            2 -> {
                medicineReminderSortOrder = if (medicineReminderSortOrder == 0) 1 else 0
                loadMedicineReminders()
            }
        }
        invalidateOptionsMenu()
        val label = when (binding.tabLayout.selectedTabPosition) {
            0 -> if (medicalRecordSortOrder == 0) "由近到远" else "由远到近"
            1 -> if (healthRecordSortOrder == 0) "由近到远" else "由远到近"
            else -> if (medicineReminderSortOrder == 0) "由近到远" else "由远到近"
        }
        ToastUtils.show(this, "排序：$label")
    }

    private fun loadMedicalRecords() {
        val order = if (medicalRecordSortOrder == 0) "DESC" else "ASC"
        val records = if (currentSearchQuery.isNotEmpty()) {
            dbHelper.searchMedicalRecords(currentSearchQuery, order)
        } else {
            when (medicalRecordFilter) {
                0 -> dbHelper.getAllMedicalRecords(order)
                else -> {
                    val cal = Calendar.getInstance()
                    when (medicalRecordFilter) {
                        1 -> cal.add(Calendar.MONTH, -1)
                        2 -> cal.add(Calendar.MONTH, -3)
                        3 -> cal.add(Calendar.YEAR, -1)
                    }
                    val startTime = dateTimeFormat.format(cal.time)
                    val endTime = dateTimeFormat.format(Calendar.getInstance().time)
                    dbHelper.getMedicalRecordsByTimeRange(startTime, endTime, order)
                }
            }
        }
        medicalRecordAdapter.setData(records)

        binding.recyclerViewHealthRecords.adapter = medicalRecordAdapter
        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE

        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = if (currentSearchQuery.isNotEmpty()) {
                "未找到匹配的就医记录"
            } else {
                "暂无就医记录，点击右下角添加"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun loadHealthRecords() {
        val order = if (healthRecordSortOrder == 0) "DESC" else "ASC"
        val records = if (currentSearchQuery.isNotEmpty()) {
            dbHelper.searchHealthRecords(currentSearchQuery, order)
        } else {
            dbHelper.getAllHealthRecords(order)
        }
        binding.recyclerViewHealthRecords.adapter = healthRecordAdapter
        healthRecordAdapter.setData(records)

        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE

        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = if (currentSearchQuery.isNotEmpty()) {
                "未找到匹配的健康记录"
            } else {
                "暂无健康记录，点击右下角添加"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun loadMedicineReminders() {
        val order = if (medicineReminderSortOrder == 0) "DESC" else "ASC"
        val reminders = if (currentSearchQuery.isNotEmpty()) {
            dbHelper.searchMedicineReminders(currentSearchQuery, order)
        } else {
            dbHelper.getAllMedicineReminders(order)
        }
        medicineReminderAdapter.setData(reminders)

        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.VISIBLE

        if (reminders.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = if (currentSearchQuery.isNotEmpty()) {
                "未找到匹配的用药提醒"
            } else {
                "暂无用药提醒，点击右下角添加"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun loadExamReports() {
        val reports = dbHelper.getAllPhysicalExamReports()
        examReportAdapter.setData(reports)

        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.recyclerViewExamReports.visibility = View.VISIBLE

        if (reports.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无体检报告，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    // ==================== 搜索功能 ====================

    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null

    // ==================== 批量删除 ====================

    private fun isInSelectionMode(): Boolean {
        return when (binding.tabLayout.selectedTabPosition) {
            0 -> medicalRecordAdapter.isInSelectionMode()
            1 -> healthRecordAdapter.isInSelectionMode()
            2 -> medicineReminderAdapter.isInSelectionMode()
            3 -> examReportAdapter.isInSelectionMode()
            else -> false
        }
    }

    private fun enterSelectionMode() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> medicalRecordAdapter.setSelectionMode(true)
            1 -> healthRecordAdapter.setSelectionMode(true)
            2 -> medicineReminderAdapter.setSelectionMode(true)
            3 -> examReportAdapter.setSelectionMode(true)
        }
        binding.fabAdd.visibility = View.GONE
        binding.layoutBatchDelete.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    private fun exitSelectionMode() {
        medicalRecordAdapter.setSelectionMode(false)
        healthRecordAdapter.setSelectionMode(false)
        medicineReminderAdapter.setSelectionMode(false)
        examReportAdapter.setSelectionMode(false)
        binding.fabAdd.visibility = View.VISIBLE
        binding.layoutBatchDelete.visibility = View.GONE
        updateActionBarTitle()
        invalidateOptionsMenu()
    }

    private fun updateActionBarTitle() {
        supportActionBar?.title = when (binding.tabLayout.selectedTabPosition) {
            0 -> "我的就医记录"
            1 -> "健康记录"
            2 -> "用药提醒"
            3 -> "体检报告"
            else -> "我的就医记录"
        }
    }

    private fun updateSelectionTitle(count: Int) {
        supportActionBar?.title = if (count > 0) "已选择 $count 项" else "批量删除"
    }

    private fun performBatchDelete() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> performMedicalRecordBatchDelete()
            1 -> performHealthRecordBatchDelete()
            2 -> performMedicineReminderBatchDelete()
            3 -> performExamReportBatchDelete()
        }
    }

    private fun performMedicalRecordBatchDelete() {
        val selectedIds = medicalRecordAdapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            ToastUtils.show(this, "请选择要删除的记录")
            return
        }
        DialogUtils.showConfirm(
            this,
            "批量删除确认",
            "确定要删除选中的 ${selectedIds.size} 条记录吗？删除后不可恢复",
            "确定"
        ) {
            var deletedCount = 0
            selectedIds.forEach { id ->
                val result = dbHelper.deleteMedicalRecord(id.toInt())
                if (result > 0) deletedCount++
            }
            medicalRecordAdapter.removeRecords(selectedIds)
            exitSelectionMode()
            if (medicalRecordAdapter.getData().isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "暂无就医记录，点击右下角添加"
            }
            ToastUtils.show(this, "已删除 $deletedCount 条记录")
        }
    }

    private fun performHealthRecordBatchDelete() {
        val selectedIds = healthRecordAdapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            ToastUtils.show(this, "请选择要删除的记录")
            return
        }
        DialogUtils.showConfirm(
            this,
            "批量删除确认",
            "确定要删除选中的 ${selectedIds.size} 条健康记录吗？删除后不可恢复",
            "确定"
        ) {
            var deletedCount = 0
            selectedIds.forEach { id ->
                val result = dbHelper.deleteHealthRecord(id)
                if (result > 0) deletedCount++
            }
            healthRecordAdapter.removeRecords(selectedIds)
            exitSelectionMode()
            if (healthRecordAdapter.getData().isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "暂无健康记录，点击右下角添加"
            }
            ToastUtils.show(this, "已删除 $deletedCount 条记录")
        }
    }

    private fun performMedicineReminderBatchDelete() {
        val selectedIds = medicineReminderAdapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            ToastUtils.show(this, "请选择要删除的提醒")
            return
        }
        DialogUtils.showConfirm(
            this,
            "批量删除确认",
            "确定要删除选中的 ${selectedIds.size} 条用药提醒吗？删除后不可恢复",
            "确定"
        ) {
            var deletedCount = 0
            val reminders = medicineReminderAdapter.getData()
            selectedIds.forEach { id ->
                val result = dbHelper.deleteMedicineReminder(id)
                if (result > 0) {
                    deletedCount++
                    val reminder = reminders.find { it.id == id }
                    reminder?.let {
                        try { AlarmScheduler.cancelReminder(this, it) } catch (_: Exception) {}
                        try {
                            if (it.calendarEventIds.isNotEmpty()) {
                                CalendarHelper.deleteCalendarEvents(this, it.calendarEventIds)
                            } else {
                                CalendarHelper.deleteCalendarEventsByMedicineName(this, it.medicineName)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            medicineReminderAdapter.removeRecords(selectedIds)
            exitSelectionMode()
            if (medicineReminderAdapter.getData().isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "暂无用药提醒，点击右下角添加"
            }
            ToastUtils.show(this, "已删除 $deletedCount 条提醒")
        }
    }

    private fun performExamReportBatchDelete() {
        val selectedIds = examReportAdapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            ToastUtils.show(this, "请选择要删除的报告")
            return
        }
        DialogUtils.showConfirm(
            this,
            "批量删除确认",
            "确定要删除选中的 ${selectedIds.size} 份体检报告吗？删除后不可恢复",
            "确定"
        ) {
            var deletedCount = 0
            selectedIds.forEach { id ->
                val result = dbHelper.deletePhysicalExamReport(id)
                if (result > 0) deletedCount++
            }
            examReportAdapter.removeRecords(selectedIds)
            exitSelectionMode()
            loadExamReports()
            ToastUtils.show(this, "已删除 $deletedCount 份报告")
        }
    }

    // ==================== 就医记录操作 ====================

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
                loadMedicineReminders()
                ToastUtils.show(this, "删除成功")
            }
        }
    }

    // ==================== Menu ====================

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        searchMenuItem = menu?.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView

        searchView?.apply {
            queryHint = getSearchHint()
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    currentSearchQuery = query?.trim() ?: ""
                    loadData()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        currentSearchQuery = ""
                        loadData()
                    }
                    return true
                }
            })

            searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    currentSearchQuery = ""
                    loadData()
                    return true
                }
            })
        }

        val sortItem = menu?.findItem(R.id.action_sort)
        val filterItem = menu?.findItem(R.id.action_filter)
        val batchDeleteItem = menu?.findItem(R.id.action_batch_delete)
        val syncItem = menu?.findItem(R.id.action_sync)

        val currentSortOrder = when (binding.tabLayout.selectedTabPosition) {
            0 -> medicalRecordSortOrder
            1 -> healthRecordSortOrder
            else -> medicineReminderSortOrder
        }
        sortItem?.setIcon(if (currentSortOrder == 0) R.drawable.ic_sort_desc else R.drawable.ic_sort_asc)
        sortItem?.title = if (currentSortOrder == 0) "排序：由近到远" else "排序：由远到近"

        if (isInSelectionMode()) {
            searchMenuItem?.isVisible = false
            sortItem?.isVisible = false
            filterItem?.isVisible = false
            batchDeleteItem?.isVisible = false
            syncItem?.isVisible = false
        } else {
            searchMenuItem?.isVisible = true
            sortItem?.isVisible = binding.tabLayout.selectedTabPosition < 4
            filterItem?.isVisible = binding.tabLayout.selectedTabPosition == 0
            batchDeleteItem?.isVisible = binding.tabLayout.selectedTabPosition < 4
            syncItem?.isVisible = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                toggleSortOrder()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_batch_delete -> {
                enterSelectionMode()
                true
            }
            R.id.action_sync -> {
                syncData()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (isInSelectionMode()) {
            exitSelectionMode()
        } else if (currentSearchQuery.isNotEmpty()) {
            currentSearchQuery = ""
            searchMenuItem?.collapseActionView()
            loadData()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isInSelectionMode()) {
                    exitSelectionMode()
                } else if (currentSearchQuery.isNotEmpty()) {
                    currentSearchQuery = ""
                    searchMenuItem?.collapseActionView()
                    loadData()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showFilterDialog() {
        if (binding.tabLayout.selectedTabPosition != 0) {
            ToastUtils.show(this, "筛选仅支持就医记录")
            return
        }
        val options = arrayOf("全部", "近1个月", "近3个月", "近1年")
        DialogUtils.showOptions(this, "按时间筛选", options) { which ->
            medicalRecordFilter = which
            currentSearchQuery = ""
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_APP_LOCK -> { /* 解锁成功，继续正常使用 */ }
                REQUEST_ADD_MEDICAL_RECORD, REQUEST_EDIT_MEDICAL_RECORD -> {
                    loadMedicalRecords()
                }
                REQUEST_ADD_HEALTH_RECORD, REQUEST_EDIT_HEALTH_RECORD -> {
                    loadHealthRecords()
                }
                REQUEST_ADD_MEDICINE, REQUEST_EDIT_MEDICINE -> {
                    loadMedicineReminders()
                }
                REQUEST_ADD_EXAM_REPORT -> {
                    loadExamReports()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
