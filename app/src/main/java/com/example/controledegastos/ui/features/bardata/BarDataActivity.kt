package com.example.controledegastos.ui.features.bardata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.controledegastos.databinding.ActivityGratBinding
import com.example.controledegastos.ui.renderYears
import com.example.controledegastos.viewmodel.ItemsViewModel
import com.github.mikephil.charting.charts.BarChart
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

    private fun setupObservers() {

        val barChart: BarChart = binding.barGrat

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                itemsViewModel.annualChartState.collect { state ->
            val entries = arrayListOf(
                BarEntry(100f, state.inflowCents.toFloat() / 100, "Entrada Total"),
                BarEntry(101.5f, state.outflowCents.toFloat() / 100, "Saída Total"),
                BarEntry(103f, state.balanceCents.toFloat() / 100, "SaldoTotal")
            )

            val barDataSet = BarDataSet(entries, "Ganhos / Despesas Total (Em R$)").apply {
                setColors(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#DF4646"),
                    Color.parseColor("#3042A8")
                )
                valueTextSize = 1f
                valueTextColor = Color.BLACK
            }

            barChart.data = BarData(barDataSet)
            barChart.invalidate()

            binding.textTotalbalance.text = state.balanceText
            binding.textTotalinflow.text = state.inflowText
            binding.textTotaloutflow.text = state.outflowText
                }
            }
        }

        barChart.apply {
            setFitBars(true)
            description.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

}
