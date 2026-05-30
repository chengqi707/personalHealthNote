package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.entity.PhysicalExamReport
import org.json.JSONArray

class PhysicalExamReportAdapter(
    private var reports: MutableList<PhysicalExamReport> = mutableListOf(),
    private val onItemClick: ((PhysicalExamReport) -> Unit)? = null
) : RecyclerView.Adapter<PhysicalExamReportAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReportTitle: TextView = itemView.findViewById(R.id.tvReportTitle)
        val tvExamDate: TextView = itemView.findViewById(R.id.tvExamDate)
        val tvHospital: TextView = itemView.findViewById(R.id.tvHospital)
        val tvAbnormalCount: TextView = itemView.findViewById(R.id.tvAbnormalCount)
        val tvAbnormalSummary: TextView = itemView.findViewById(R.id.tvAbnormalSummary)
        val cardView: CardView = itemView.findViewById(R.id.cardView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(reports[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_physical_exam_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]

        holder.tvReportTitle.text = report.reportTitle.ifEmpty { "体检报告" }
        holder.tvExamDate.text = report.examDate
        holder.tvHospital.text = report.hospital.ifEmpty { "未填写机构" }

        // 异常指标计数
        if (report.parsedIndicators.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(report.parsedIndicators)
                var abnormalCount = 0
                for (i in 0 until jsonArray.length()) {
                    if (jsonArray.getJSONObject(i).optBoolean("isAbnormal", false)) {
                        abnormalCount++
                    }
                }
                if (abnormalCount > 0) {
                    holder.tvAbnormalCount.text = "${abnormalCount}项异常"
                    holder.tvAbnormalCount.visibility = View.VISIBLE
                } else {
                    holder.tvAbnormalCount.text = "指标正常"
                    holder.tvAbnormalCount.setTextColor(holder.itemView.context.getColor(R.color.textHint))
                    holder.tvAbnormalCount.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                holder.tvAbnormalCount.visibility = View.GONE
            }
        } else {
            holder.tvAbnormalCount.visibility = View.GONE
        }

        // 异常摘要
        if (report.abnormalSummary.isNotEmpty()) {
            holder.tvAbnormalSummary.text = report.abnormalSummary
            holder.tvAbnormalSummary.visibility = View.VISIBLE
        } else {
            holder.tvAbnormalSummary.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = reports.size

    fun setData(newReports: List<PhysicalExamReport>) {
        reports.clear()
        reports.addAll(newReports)
        notifyDataSetChanged()
    }
}
