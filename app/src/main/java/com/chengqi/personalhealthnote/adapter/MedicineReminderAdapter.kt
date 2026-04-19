package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.MedicineReminder

/**
 * 用药提醒适配器
 * 用于RecyclerView显示用药提醒列表
 */
class MedicineReminderAdapter(
    private var reminders: MutableList<MedicineReminder> = mutableListOf(),
    private val onItemClick: ((MedicineReminder) -> Unit)? = null,
    private val onItemLongClick: ((MedicineReminder) -> Boolean)? = null,
    private val onSwitchChanged: ((MedicineReminder, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<MedicineReminderAdapter.ViewHolder>() {

    /**
     * ViewHolder内部类
     * 缓存Item视图引用
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvDosage: TextView = itemView.findViewById(R.id.tvDosage)
        val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        val tvRemindTime: TextView = itemView.findViewById(R.id.tvRemindTime)
        val tvMealTime: TextView = itemView.findViewById(R.id.tvMealTime)
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val switchEnable: Switch = itemView.findViewById(R.id.switchEnable)

        init {
            // 设置点击事件（不包括Switch）
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(reminders[position])
                }
            }

            // 设置长按事件
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(reminders[position]) ?: false
                } else {
                    false
                }
            }

            // 设置Switch切换监听
            switchEnable.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val reminder = reminders[position]
                    // 只有当状态真正改变时才回调
                    if (reminder.isEnabled != isChecked) {
                        onSwitchChanged?.invoke(reminder, isChecked)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = reminders[position]

        // 设置药品名称
        holder.tvMedicineName.text = reminder.medicineName

        // 设置剂量
        holder.tvDosage.text = reminder.getFullDosage()

        // 设置频率
        holder.tvFrequency.text = reminder.getFrequencyText()

        // 设置提醒时间（显示所有时间）
        val times = reminder.getAllRemindTimes()
        holder.tvRemindTime.text = times.joinToString(", ")

        // 设置服用时间
        holder.tvMealTime.text = reminder.getMealTimeText()

        // 设置日期范围
        val endDateText = reminder.endDate ?: "长期"
        holder.tvDateRange.text = "${reminder.startDate} 至 $endDateText"

        // 设置开关状态（先移除监听器避免触发回调）
        holder.switchEnable.setOnCheckedChangeListener(null)
        holder.switchEnable.isChecked = reminder.isEnabled

        // 重新设置监听器
        holder.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val rem = reminders[pos]
                if (rem.isEnabled != isChecked) {
                    onSwitchChanged?.invoke(rem, isChecked)
                }
            }
        }
    }

    override fun getItemCount(): Int = reminders.size

    /**
     * 设置数据列表（全新替换）
     * @param newReminders 新的数据列表
     */
    fun setData(newReminders: List<MedicineReminder>) {
        reminders.clear()
        reminders.addAll(newReminders)
        notifyDataSetChanged()
    }

    /**
     * 添加单条提醒
     * @param reminder 用药提醒
     */
    fun addReminder(reminder: MedicineReminder) {
        reminders.add(0, reminder)
        notifyItemInserted(0)
    }

    /**
     * 更新单条提醒
     * @param reminder 用药提醒
     */
    fun updateReminder(reminder: MedicineReminder) {
        val position = reminders.indexOfFirst { it.id == reminder.id }
        if (position != -1) {
            reminders[position] = reminder
            notifyItemChanged(position)
        }
    }

    /**
     * 删除单条提醒
     * @param id 提醒ID
     */
    fun removeReminder(id: Long) {
        val position = reminders.indexOfFirst { it.id == id }
        if (position != -1) {
            reminders.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * 更新提醒启用状态
     * @param id 提醒ID
     * @param isEnabled 是否启用
     */
    fun updateReminderStatus(id: Long, isEnabled: Boolean) {
        val position = reminders.indexOfFirst { it.id == id }
        if (position != -1) {
            val reminder = reminders[position]
            reminders[position] = reminder.copy(isEnabled = isEnabled)
            notifyItemChanged(position)
        }
    }

    /**
     * 获取所有数据
     * @return 用药提醒列表
     */
    fun getData(): List<MedicineReminder> = reminders.toList()

    /**
     * 清空所有数据
     */
    fun clearData() {
        val size = reminders.size
        reminders.clear()
        notifyItemRangeRemoved(0, size)
    }
}