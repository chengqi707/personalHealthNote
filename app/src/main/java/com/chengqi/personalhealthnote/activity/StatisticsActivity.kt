package com.chengqi.personalhealthnote.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivityStatisticsBinding
import com.chengqi.personalhealthnote.utils.ToastUtils

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var dbHelper: DatabaseHelper
    private var currentDays = 30
    private var currentMetric = METRIC_WEIGHT

    companion object {
        private const val METRIC_WEIGHT = 0
        private const val METRIC_BLOOD_PRESSURE = 1
        private const val METRIC_BLOOD_SUGAR = 2
        private const val METRIC_BMI = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "健康统计"

        setupTimeRangeSelector()
        setupMetricSelector()
        loadStatistics()
    }

    private fun setupTimeRangeSelector() {
        binding.chip7d.setOnClickListener { currentDays = 7; loadStatistics() }
        binding.chip30d.setOnClickListener { currentDays = 30; loadStatistics() }
        binding.chip90d.setOnClickListener { currentDays = 90; loadStatistics() }
        binding.chipAll.setOnClickListener { currentDays = 3650; loadStatistics() }

        binding.chip30d.isChecked = true
    }

    private fun setupMetricSelector() {
        binding.chipWeight.setOnClickListener { currentMetric = METRIC_WEIGHT; loadTrendChart() }
        binding.chipBp.setOnClickListener { currentMetric = METRIC_BLOOD_PRESSURE; loadTrendChart() }
        binding.chipSugar.setOnClickListener { currentMetric = METRIC_BLOOD_SUGAR; loadTrendChart() }
        binding.chipBmi.setOnClickListener { currentMetric = METRIC_BMI; loadTrendChart() }

        binding.chipWeight.isChecked = true
    }

    private fun loadStatistics() {
        loadSummaryCards()
        loadTrendChart()
        loadSymptomRanking()
    }

    private fun loadSummaryCards() {
        val medicalCount = dbHelper.getMedicalRecordCount()
        val healthCount = dbHelper.getHealthRecordCount()
        val medicineCount = dbHelper.getMedicineTypeCount()

        binding.tvMedicalCount.text = medicalCount.toString()
        binding.tvHealthCount.text = healthCount.toString()
        binding.tvMedicineCount.text = medicineCount.toString()
    }

    private fun loadTrendChart() {
        when (currentMetric) {
            METRIC_WEIGHT -> {
                val data = dbHelper.getWeightTrend(currentDays)
                if (data.isEmpty()) {
                    binding.trendChart.visibility = View.GONE
                    binding.tvChartEmpty.visibility = View.VISIBLE
                    binding.tvChartEmpty.text = "暂无体重数据"
                } else {
                    binding.trendChart.visibility = View.VISIBLE
                    binding.tvChartEmpty.visibility = View.GONE
                    binding.trendChart.setData(
                        data.map { it.first },
                        data.map { it.second }
                    )
                }
            }
            METRIC_BLOOD_PRESSURE -> {
                val data = dbHelper.getBloodPressureTrend(currentDays)
                if (data.isEmpty()) {
                    binding.trendChart.visibility = View.GONE
                    binding.tvChartEmpty.visibility = View.VISIBLE
                    binding.tvChartEmpty.text = "暂无血压数据"
                } else {
                    binding.trendChart.visibility = View.VISIBLE
                    binding.tvChartEmpty.visibility = View.GONE
                    binding.trendChart.setDualData(
                        data.map { it.first },
                        data.map { it.second.toFloat() },
                        data.map { it.third.toFloat() }
                    )
                }
            }
            METRIC_BLOOD_SUGAR -> {
                val data = dbHelper.getBloodSugarTrend(currentDays)
                if (data.isEmpty()) {
                    binding.trendChart.visibility = View.GONE
                    binding.tvChartEmpty.visibility = View.VISIBLE
                    binding.tvChartEmpty.text = "暂无血糖数据"
                } else {
                    binding.trendChart.visibility = View.VISIBLE
                    binding.tvChartEmpty.visibility = View.GONE
                    binding.trendChart.setData(
                        data.map { it.first },
                        data.map { it.second }
                    )
                }
            }
            METRIC_BMI -> {
                val data = dbHelper.getBmiTrend(currentDays)
                if (data.isEmpty()) {
                    binding.trendChart.visibility = View.GONE
                    binding.tvChartEmpty.visibility = View.VISIBLE
                    binding.tvChartEmpty.text = "暂无BMI数据（需录入身高）"
                } else {
                    binding.trendChart.visibility = View.VISIBLE
                    binding.tvChartEmpty.visibility = View.GONE
                    binding.trendChart.setData(
                        data.map { it.first },
                        data.map { it.second }
                    )
                }
            }
        }
    }

    private fun loadSymptomRanking() {
        val topSymptoms = dbHelper.getTopSymptoms(5)
        val container = binding.layoutSymptomRanking
        container.removeAllViews()

        if (topSymptoms.isEmpty()) {
            binding.tvSymptomEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvSymptomEmpty.visibility = View.GONE
        val maxCount = topSymptoms.firstOrNull()?.second ?: 1

        topSymptoms.forEachIndexed { index, pair ->
            val itemView = layoutInflater.inflate(R.layout.item_symptom_rank, container, false)
            val tvRank = itemView.findViewById<TextView>(R.id.tvRank)
            val tvSymptom = itemView.findViewById<TextView>(R.id.tvSymptom)
            val tvCount = itemView.findViewById<TextView>(R.id.tvCount)
            val viewBar = itemView.findViewById<View>(R.id.viewBar)

            tvRank.text = "${index + 1}"
            tvSymptom.text = pair.first
            tvCount.text = "${pair.second}次"

            val maxWidth = resources.displayMetrics.widthPixels - 280
            val barWidth = (maxWidth * pair.second.toFloat() / maxCount).toInt().coerceAtLeast(20)
            viewBar.layoutParams.width = barWidth

            container.addView(itemView)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
