package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.HealthRecord

class HealthRecordAdapter(
    private var records: MutableList<HealthRecord> = mutableListOf(),
    private val onItemClick: ((HealthRecord) -> Unit)? = null,
    private val onItemLongClick: ((HealthRecord) -> Boolean)? = null
) : RecyclerView.Adapter<HealthRecordAdapter.ViewHolder>() {

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
        val pos = records.indexOfFirst { it.id == id }
        if (pos != -1) notifyItemChanged(pos)
        onSelectionChanged?.invoke(selectedIds.size)
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(records.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvWeight: TextView = itemView.findViewById(R.id.tvWeight)
        val tvBloodPressure: TextView = itemView.findViewById(R.id.tvBloodPressure)
        val tvHeartRate: TextView = itemView.findViewById(R.id.tvHeartRate)
        val tvBloodSugar: TextView = itemView.findViewById(R.id.tvBloodSugar)
        val ivHasImages: ImageView = itemView.findViewById(R.id.ivHasImages)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(records[position].id)
                    } else {
                        onItemClick?.invoke(records[position])
                    }
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(records[position].id)
                        true
                    } else {
                        onItemLongClick?.invoke(records[position]) ?: false
                    }
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        holder.tvDate.text = record.recordDate
        holder.tvWeight.text = if (record.weight > 0) "${record.weight} kg" else "-"
        holder.tvBloodPressure.text = if (record.systolicPressure > 0 && record.diastolicPressure > 0) {
            "${record.systolicPressure}/${record.diastolicPressure}"
        } else "-"
        holder.tvHeartRate.text = if (record.heartRate > 0) "${record.heartRate} 次/分" else "-"
        holder.tvBloodSugar.text = if (record.bloodSugar > 0) "${record.bloodSugar} mmol/L" else "-"
        holder.ivHasImages.visibility = if (record.imagePaths.isNotEmpty()) View.VISIBLE else View.GONE

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedIds.contains(record.id)
        holder.cbSelect.setOnClickListener {
            toggleSelection(record.id)
        }
    }

    override fun getItemCount(): Int = records.size

    fun setData(newRecords: List<HealthRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    fun removeRecord(id: Long) {
        val position = records.indexOfFirst { it.id == id }
        if (position != -1) {
            records.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun removeRecords(ids: Set<Long>) {
        records.removeAll { ids.contains(it.id) }
        notifyDataSetChanged()
    }

    fun getData(): List<HealthRecord> = records.toList()
}
