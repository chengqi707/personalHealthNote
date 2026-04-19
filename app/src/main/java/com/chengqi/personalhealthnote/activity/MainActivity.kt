package com.chengqi.personalhealthnote.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chengqi.personalhealthnote.adapter.HealthRecordAdapter
import com.chengqi.personalhealthnote.adapter.MedicineReminderAdapter
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityMainBinding
import com.chengqi.personalhealthnote.entity.HealthRecord
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 主界面Activity
 * 包含健康记录列表和用药提醒列表两个Tab页面
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var healthRecordAdapter: HealthRecordAdapter
    private lateinit var medicineReminderAdapter: MedicineReminderAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        const val REQUEST_ADD_HEALTH_RECORD = 1001
        const val REQUEST_EDIT_HEALTH_RECORD = 1002
        const val REQUEST_ADD_MEDICINE = 1003
        const val REQUEST_EDIT_MEDICINE = 1004
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerViews()  // 先初始化Adapter，再初始化TabLayout
        setupTabLayout()
        setupListeners()
        loadData()
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "健康笔记"
    }

    /**
     * 设置TabLayout
     */
    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("健康记录"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("用药提醒"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showHealthRecordTab()
                    1 -> showMedicineReminderTab()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 默认显示健康记录Tab
        showHealthRecordTab()
    }

    /**
     * 显示健康记录Tab
     */
    private fun showHealthRecordTab() {
        binding.recyclerViewHealthRecords.visibility = View.VISIBLE
        binding.recyclerViewMedicineReminders.visibility = View.GONE
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        loadHealthRecords()
    }

    /**
     * 显示用药提醒Tab
     */
    private fun showMedicineReminderTab() {
        binding.recyclerViewHealthRecords.visibility = View.GONE
        binding.recyclerViewMedicineReminders.visibility = View.VISIBLE
        binding.fabAdd.setImageResource(android.R.drawable.ic_menu_add)
        loadMedicineReminders()
    }

    /**
     * 设置RecyclerViews
     */
    private fun setupRecyclerViews() {
        // 健康记录RecyclerView
        healthRecordAdapter = HealthRecordAdapter(
            onItemClick = { record ->
                // 点击查看详情
                val intent = Intent(this, HealthRecordDetailActivity::class.java).apply {
                    putExtra("record_id", record.id)
                }
                startActivityForResult(intent, REQUEST_EDIT_HEALTH_RECORD)
            },
            onItemLongClick = { record ->
                // 长按删除
                showDeleteConfirmDialog(record)
                true
            }
        )

        binding.recyclerViewHealthRecords.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = healthRecordAdapter
        }

        // 用药提醒RecyclerView
        medicineReminderAdapter = MedicineReminderAdapter(
            onItemClick = { reminder ->
                // 点击编辑
                val intent = Intent(this, MedicineReminderEditActivity::class.java).apply {
                    putExtra("reminder_id", reminder.id)
                }
                startActivityForResult(intent, REQUEST_EDIT_MEDICINE)
            },
            onItemLongClick = { reminder ->
                // 长按删除
                showDeleteMedicineConfirmDialog(reminder)
                true
            },
            onSwitchChanged = { reminder, isEnabled ->
                // 切换启用状态
                dbHelper.toggleMedicineReminder(reminder.id, isEnabled)
                medicineReminderAdapter.updateReminderStatus(reminder.id, isEnabled)
            }
        )

        binding.recyclerViewMedicineReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = medicineReminderAdapter
        }
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            when (binding.tabLayout.selectedTabPosition) {
                0 -> {
                    // 添加健康记录
                    val intent = Intent(this, HealthRecordEditActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_HEALTH_RECORD)
                }
                1 -> {
                    // 添加用药提醒
                    val intent = Intent(this, MedicineReminderEditActivity::class.java)
                    startActivityForResult(intent, REQUEST_ADD_MEDICINE)
                }
            }
        }

        // 下拉刷新（这里用SwipeRefreshLayout模拟）
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> loadHealthRecords()
            1 -> loadMedicineReminders()
        }
    }

    /**
     * 加载健康记录
     */
    private fun loadHealthRecords() {
        val records = dbHelper.getAllHealthRecords()
        healthRecordAdapter.setData(records)

        // 显示或隐藏空数据提示
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无健康记录，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    /**
     * 加载用药提醒
     */
    private fun loadMedicineReminders() {
        val reminders = dbHelper.getAllMedicineReminders()
        medicineReminderAdapter.setData(reminders)

        // 显示或隐藏空数据提示
        if (reminders.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "暂无用药提醒，点击右下角添加"
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    /**
     * 显示删除健康记录确认对话框
     */
    private fun showDeleteConfirmDialog(record: HealthRecord) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除 ${record.recordDate} 的健康记录吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                val deletedRows = dbHelper.deleteHealthRecord(record.id)
                if (deletedRows > 0) {
                    healthRecordAdapter.removeRecord(record.id)
                    // 检查是否需要显示空数据提示
                    if (healthRecordAdapter.getData().isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "暂无健康记录，点击右下角添加"
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除用药提醒确认对话框
     */
    private fun showDeleteMedicineConfirmDialog(reminder: MedicineReminder) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除 \"${reminder.medicineName}\" 的用药提醒吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                val deletedRows = dbHelper.deleteMedicineReminder(reminder.id)
                if (deletedRows > 0) {
                    medicineReminderAdapter.removeReminder(reminder.id)
                    // 检查是否需要显示空数据提示
                    if (medicineReminderAdapter.getData().isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "暂无用药提醒，点击右下角添加"
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_HEALTH_RECORD, REQUEST_EDIT_HEALTH_RECORD -> {
                    // 刷新健康记录列表
                    loadHealthRecords()
                }
                REQUEST_ADD_MEDICINE, REQUEST_EDIT_MEDICINE -> {
                    // 刷新用药提醒列表
                    loadMedicineReminders()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次回到页面时刷新数据
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}