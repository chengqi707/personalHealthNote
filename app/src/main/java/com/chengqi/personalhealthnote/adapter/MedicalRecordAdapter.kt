package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.MedicalRecord

class MedicalRecordAdapter(
    private var records: MutableList<MedicalRecord> = mutableListOf(),
    private val onItemClick: ((MedicalRecord) -> Unit)? = null,
    private val onItemLongClick: ((MedicalRecord) -> Boolean)? = null
) : RecyclerView.Adapter<MedicalRecordAdapter.ViewHolder>() {

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

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(0)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicalTime: TextView = itemView.findViewById(R.id.tvMedicalTime)
        val tvHospital: TextView = itemView.findViewById(R.id.tvHospital)
        val tvSymptoms: TextView = itemView.findViewById(R.id.tvSymptoms)
        val tvDiagnosisResult: TextView = itemView.findViewById(R.id.tvDiagnosisResult)
        val tvDoctor: TextView = itemView.findViewById(R.id.tvDoctor)
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
            .inflate(R.layout.item_medical_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.tvMedicalTime.text = record.medicalTime
        holder.tvHospital.text = record.hospital
        holder.tvSymptoms.text = record.symptoms
        holder.tvDiagnosisResult.text = record.diagnosisResult
        holder.tvDoctor.text = if (record.doctor.isNotEmpty()) record.doctor else ""
        holder.tvDoctor.visibility = if (record.doctor.isNotEmpty()) View.VISIBLE else View.GONE
        holder.ivHasImages.visibility = if (record.imagePaths.isNotEmpty()) View.VISIBLE else View.GONE

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedIds.contains(record.id)
        holder.cbSelect.setOnClickListener {
            toggleSelection(record.id)
        }
    }

    override fun getItemCount(): Int = records.size

    fun setData(newRecords: List<MedicalRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    fun addRecord(record: MedicalRecord) {
        records.add(0, record)
        notifyItemInserted(0)
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

    fun getData(): List<MedicalRecord> = records.toList()
}
