package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.HealthRecord

/**
 * 健康记录适配器
 * 用于RecyclerView显示健康记录列表
 */
class HealthRecordAdapter(
    private var records: MutableList<HealthRecord> = mutableListOf(),
    private val onItemClick: ((HealthRecord) -> Unit)? = null,
    private val onItemLongClick: ((HealthRecord) -> Boolean)? = null
) : RecyclerView.Adapter<HealthRecordAdapter.ViewHolder>() {

    /**
     * ViewHolder内部类
     * 缓存Item视图引用
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvWeight: TextView = itemView.findViewById(R.id.tvWeight)
        val tvBloodPressure: TextView = itemView.findViewById(R.id.tvBloodPressure)
        val tvHeartRate: TextView = itemView.findViewById(R.id.tvHeartRate)
        val tvBloodSugar: TextView = itemView.findViewById(R.id.tvBloodSugar)
        val ivHasImages: ImageView = itemView.findViewById(R.id.ivHasImages)

        init {
            // 设置点击事件
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(records[position])
                }
            }

            // 设置长按事件
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
            .inflate(R.layout.item_health_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        // 设置日期显示
        holder.tvDate.text = record.recordDate

        // 设置体重显示
        holder.tvWeight.text = if (record.weight > 0) {
            "${record.weight} kg"
        } else {
            "-"
        }

        // 设置血压显示
        holder.tvBloodPressure.text = if (record.systolicPressure > 0 && record.diastolicPressure > 0) {
            "${record.systolicPressure}/${record.diastolicPressure}"
        } else {
            "-"
        }

        // 设置心率显示
        holder.tvHeartRate.text = if (record.heartRate > 0) {
            "${record.heartRate} 次/分"
        } else {
            "-"
        }

        // 设置血糖显示
        holder.tvBloodSugar.text = if (record.bloodSugar > 0) {
            "${record.bloodSugar} mmol/L"
        } else {
            "-"
        }

        // 显示或隐藏图片标识
        holder.ivHasImages.visibility = if (record.imagePaths.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun getItemCount(): Int = records.size

    /**
     * 设置数据列表（全新替换）
     * @param newRecords 新的数据列表
     */
    fun setData(newRecords: List<HealthRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    /**
     * 添加单条记录
     * @param record 健康记录
     */
    fun addRecord(record: HealthRecord) {
        records.add(0, record)
        notifyItemInserted(0)
    }

    /**
     * 更新单条记录
     * @param record 健康记录
     */
    fun updateRecord(record: HealthRecord) {
        val position = records.indexOfFirst { it.id == record.id }
        if (position != -1) {
            records[position] = record
            notifyItemChanged(position)
        }
    }

    /**
     * 删除单条记录
     * @param id 记录ID
     */
    fun removeRecord(id: Long) {
        val position = records.indexOfFirst { it.id == id }
        if (position != -1) {
            records.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * 获取所有数据
     * @return 健康记录列表
     */
    fun getData(): List<HealthRecord> = records.toList()

    /**
     * 清空所有数据
     */
    fun clearData() {
        val size = records.size
        records.clear()
        notifyItemRangeRemoved(0, size)
    }
}