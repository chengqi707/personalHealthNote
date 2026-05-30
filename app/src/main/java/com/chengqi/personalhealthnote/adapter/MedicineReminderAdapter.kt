package com.chengqi.personalhealthnote.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import android.widget.CheckBox
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.MedicineReminder

class MedicineReminderAdapter(
    private var reminders: MutableList<MedicineReminder> = mutableListOf(),
    private val onItemClick: ((MedicineReminder) -> Unit)? = null,
    private val onItemLongClick: ((MedicineReminder) -> Boolean)? = null,
    private val onSwitchChanged: ((MedicineReminder, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<MedicineReminderAdapter.ViewHolder>() {

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Long>()
    private var onSelectionChanged: ((Int) -> Unit)? = null

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChanged = listener
    }

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode == enabled) return
        isSelectionMode = enabled
        if (!enabled) selectedIds.clear()
        notifyDataSetChanged()
    }

    fun isInSelectionMode(): Boolean = isSelectionMode

    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        val pos = reminders.indexOfFirst { it.id == id }
        if (pos != -1) notifyItemChanged(pos)
        onSelectionChanged?.invoke(selectedIds.size)
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(reminders.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvDosage: TextView = itemView.findViewById(R.id.tvDosage)
        val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        val tvRemindTime: TextView = itemView.findViewById(R.id.tvRemindTime)
        val tvMealTime: TextView = itemView.findViewById(R.id.tvMealTime)
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val switchEnable: Switch = itemView.findViewById(R.id.switchEnable)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        val cardView: CardView = itemView.findViewById(R.id.cardView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(reminders[position].id)
                    } else {
                        onItemClick?.invoke(reminders[position])
                    }
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(reminders[position].id)
                        true
                    } else {
                        onItemLongClick?.invoke(reminders[position]) ?: false
                    }
                } else {
                    false
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

        holder.tvMedicineName.text = reminder.medicineName
        holder.tvDosage.text = reminder.getFullDosage()
        holder.tvFrequency.text = reminder.getFrequencyText()
        holder.tvRemindTime.text = reminder.getAllRemindTimes().joinToString(", ")
        holder.tvMealTime.text = reminder.getMealTimeText()
        val endDateText = reminder.endDate ?: "长期"
        holder.tvDateRange.text = "${reminder.startDate} 至 $endDateText"

        // 禁用状态视觉反馈
        val disabledColor = holder.itemView.context.getColor(R.color.textHint)
        val enabledColor = holder.itemView.context.getColor(R.color.textPrimary)
        val enabledSecondaryColor = holder.itemView.context.getColor(R.color.textSecondary)

        if (reminder.isEnabled) {
            holder.tvMedicineName.setTextColor(enabledColor)
            holder.tvMedicineName.paintFlags = holder.tvMedicineName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvDosage.setTextColor(enabledSecondaryColor)
            holder.tvFrequency.setTextColor(enabledSecondaryColor)
            holder.tvRemindTime.setTextColor(enabledSecondaryColor)
            holder.tvMealTime.setTextColor(enabledSecondaryColor)
            holder.tvDateRange.setTextColor(enabledSecondaryColor)
            holder.cardView.alpha = 1.0f
        } else {
            holder.tvMedicineName.setTextColor(disabledColor)
            holder.tvMedicineName.paintFlags = holder.tvMedicineName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvMedicineName.text = "${reminder.medicineName}（已停用）"
            holder.tvDosage.setTextColor(disabledColor)
            holder.tvFrequency.setTextColor(disabledColor)
            holder.tvRemindTime.setTextColor(disabledColor)
            holder.tvMealTime.setTextColor(disabledColor)
            holder.tvDateRange.setTextColor(disabledColor)
            holder.cardView.alpha = 0.6f
        }

        holder.switchEnable.setOnCheckedChangeListener(null)
        holder.switchEnable.isChecked = reminder.isEnabled
        holder.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val rem = reminders[pos]
                if (rem.isEnabled != isChecked) {
                    onSwitchChanged?.invoke(rem, isChecked)
                }
            }
        }

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedIds.contains(reminder.id)
        holder.cbSelect.setOnClickListener {
            toggleSelection(reminder.id)
        }

        // 选择模式下隐藏开关
        holder.switchEnable.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = reminders.size

    fun setData(newReminders: List<MedicineReminder>) {
        reminders.clear()
        reminders.addAll(newReminders)
        notifyDataSetChanged()
    }

    fun updateReminder(reminder: MedicineReminder) {
        val position = reminders.indexOfFirst { it.id == reminder.id }
        if (position != -1) {
            reminders[position] = reminder
            notifyItemChanged(position)
        }
    }

    fun updateReminderStatus(id: Long, isEnabled: Boolean) {
        val position = reminders.indexOfFirst { it.id == id }
        if (position != -1) {
            val reminder = reminders[position]
            reminders[position] = reminder.copy(isEnabled = isEnabled)
            notifyItemChanged(position)
        }
    }

    fun removeRecords(ids: Set<Long>) {
        reminders.removeAll { ids.contains(it.id) }
        notifyDataSetChanged()
    }

    fun getData(): List<MedicineReminder> = reminders.toList()
}
