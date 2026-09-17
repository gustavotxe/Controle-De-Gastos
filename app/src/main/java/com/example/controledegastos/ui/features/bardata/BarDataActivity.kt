package com.example.controledegastos.ui.features.bardata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.example.controledegastos.R
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.controledegastos.databinding.ActivityGratBinding
import com.example.controledegastos.ui.renderYears
import com.example.controledegastos.viewmodel.ItemsViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class BarDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGratBinding

    private val itemsViewModel: ItemsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGratBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backIconGratActivity.setOnClickListener { finish() }

        setupYearSelector()
        setupChart()
        setupObservers()
    }

    private fun setupYearSelector() {
        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.yearSpinner.selectedItem?.toString()?.toIntOrNull()?.let(itemsViewModel::selectYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(itemsViewModel.availableYears, itemsViewModel.selectedYear) { years, selected -> years to selected }
                    .collect { (years, selected) -> binding.yearSpinner.renderYears(years, selected) }
            }
        }
    }

    private fun setupObservers() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            itemsViewModel.annualChartState.collect { state ->
                val entries = arrayListOf(
                    BarEntry(0f, state.inflowCents.toFloat() / 100),
                    BarEntry(1f, state.outflowCents.toFloat() / 100),
                    BarEntry(2f, state.balanceCents.toFloat() / 100)
                )
                val dataSet = BarDataSet(entries, "").apply {
                    colors = listOf(
                        this@BarDataActivity.getColor(R.color.chartGreen),
                        this@BarDataActivity.getColor(R.color.chartRed),
                        this@BarDataActivity.getColor(R.color.brand)
                    )
                    setDrawValues(false)
                }
                binding.barGrat.data = BarData(dataSet).apply { barWidth = 0.55f }
                binding.barGrat.invalidate()
                binding.annualTotals.textTotalbalance.text = state.balanceText
                binding.annualTotals.textTotalinflow.text = state.inflowText
                binding.annualTotals.textTotaloutflow.text = state.outflowText
            }
        }
    }

    private fun setupChart() {
        binding.barGrat.apply {
            setFitBars(true)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.textColor = getColor(R.color.muted)
            axisLeft.gridColor = getColor(R.color.outline)
            axisLeft.setDrawAxisLine(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = getColor(R.color.muted)
                granularity = 1f
                setLabelCount(3, false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = when {
                        kotlin.math.abs(value) < 0.1f -> getString(R.string.inflow)
                        kotlin.math.abs(value - 1f) < 0.1f -> getString(R.string.outflow)
                        kotlin.math.abs(value - 2f) < 0.1f -> getString(R.string.balance)
                        else -> ""
                    }
                }
            }
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setExtraOffsets(8f, 16f, 8f, 8f)
            invalidate()
        }
    }

}
