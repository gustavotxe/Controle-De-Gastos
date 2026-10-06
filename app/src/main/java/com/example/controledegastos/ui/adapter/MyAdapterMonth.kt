package com.example.controledegastos.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.core.content.ContextCompat
import com.example.controledegastos.R
import com.example.controledegastos.databinding.AdapterlayoutmonthBinding
import com.example.controledegastos.ui.model.MonthSummaryUi
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class MyAdapterMonth(
    private val onMonthClicked: (MonthSummaryUi) -> Unit
) : ListAdapter<MonthSummaryUi, MyAdapterMonth.Mvh>(DIFF) {

    class Mvh(binding: AdapterlayoutmonthBinding) : RecyclerView.ViewHolder(binding.root) {
        val inflow = binding.textInflow
        val outflow = binding.textOutflow
        val balance = binding.textBalance
        val month = binding.textViewMonth
        val pieChart = binding.pieChart
        val details = binding.textViewDetails
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Mvh {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapterlayoutmonth, parent, false)
        return Mvh(AdapterlayoutmonthBinding.bind(view))
    }

    override fun onBindViewHolder(holder: Mvh, position: Int) {
        val monthSummary = getItem(position)
        val context = holder.itemView.context

        holder.inflow.text = monthSummary.inflowText
        holder.outflow.text = monthSummary.outflowText
        holder.balance.text = monthSummary.balanceText
        holder.month.text = monthSummary.monthName
        holder.details.setText(if (monthSummary.hasData) R.string.month_details else R.string.month_empty)

        val pieDataSet = if (monthSummary.hasData) {
            PieDataSet(
                arrayListOf(
                    PieEntry(monthSummary.inflowPie, context.getString(R.string.inflow)),
                    PieEntry(monthSummary.outflowPie, context.getString(R.string.outflow))
                ),
                ""
            ).apply {
                setColors(ContextCompat.getColor(context, R.color.chartGreen), ContextCompat.getColor(context, R.color.chartRed))
            }
        } else {
            PieDataSet(arrayListOf(PieEntry(1f, "Sem dados")), "").apply {
                setColor(Color.parseColor("#D1D5DB"))
            }
        }.apply {
            setDrawValues(false)
            sliceSpace = 3f
        }

        holder.pieChart.apply {
            data = PieData(pieDataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            transparentCircleRadius = 55f
            setDrawEntryLabels(monthSummary.hasData)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            setTouchEnabled(monthSummary.hasData)
            isRotationEnabled = monthSummary.hasData
            isHighlightPerTapEnabled = monthSummary.hasData
            isDragDecelerationEnabled = false
            rotationAngle = 270f
            highlightValues(null)
            centerText = if (monthSummary.hasData) "" else "Sem dados"
            setCenterTextSize(11f)
            setCenterTextColor(Color.parseColor("#6B7280"))
            legend.isEnabled = false
            invalidate()
        }

        holder.itemView.setOnClickListener { onMonthClicked(monthSummary) }
        holder.details.setOnClickListener { onMonthClicked(monthSummary) }
    }

    fun setItems(newItems: List<MonthSummaryUi>) {
        submitList(newItems)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MonthSummaryUi>() {
            override fun areItemsTheSame(oldItem: MonthSummaryUi, newItem: MonthSummaryUi) =
                oldItem.yearMonth == newItem.yearMonth
            override fun areContentsTheSame(oldItem: MonthSummaryUi, newItem: MonthSummaryUi) =
                oldItem == newItem
        }
    }
}
