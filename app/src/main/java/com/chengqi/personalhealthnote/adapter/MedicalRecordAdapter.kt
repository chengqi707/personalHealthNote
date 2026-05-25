package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.MedicalRecord

/**
 * 就医记录适配器
 * 用于RecyclerView显示就医记录列表，按PRD §4.1卡片布局
 */
class MedicalRecordAdapter(
    private var records: MutableList<MedicalRecord> = mutableListOf(),
    private val onItemClick: ((MedicalRecord) -> Unit)? = null,
    private val onItemLongClick: ((MedicalRecord) -> Boolean)? = null
) : RecyclerView.Adapter<MedicalRecordAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicalTime: TextView = itemView.findViewById(R.id.tvMedicalTime)
        val tvHospital: TextView = itemView.findViewById(R.id.tvHospital)
        val tvSymptoms: TextView = itemView.findViewById(R.id.tvSymptoms)
        val tvDiagnosisResult: TextView = itemView.findViewById(R.id.tvDiagnosisResult)
        val tvDoctor: TextView = itemView.findViewById(R.id.tvDoctor)
        val ivHasImages: ImageView = itemView.findViewById(R.id.ivHasImages)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(records[position])
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(records[position]) ?: false
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

    fun getData(): List<MedicalRecord> = records.toList()
}
