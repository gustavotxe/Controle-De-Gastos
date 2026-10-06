package com.example.controledegastos.ui.features.months

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.AdapterView
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controledegastos.databinding.ActivityAllMonthsBinding
import com.example.controledegastos.ui.adapter.MyAdapterMonth
import com.example.controledegastos.ui.renderYears
import com.example.controledegastos.viewmodel.MonthsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class MonthsActivity : AppCompatActivity() {

    lateinit var binding: ActivityAllMonthsBinding
    private lateinit var adapter: MyAdapterMonth
    private val monthsViewModel: MonthsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllMonthsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MyAdapterMonth { monthSummary ->
            if (!monthSummary.hasData) {
                Toast.makeText(this, "Nenhum dado encontrado.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(this, FilterMonthActivity::class.java)
                        .putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, monthSummary.yearMonth)
                )
                finish()
            }
        }

        binding.recyclerViewMonths.layoutManager = LinearLayoutManager(this@MonthsActivity)
        binding.recyclerViewMonths.adapter = adapter

        setupYearSelector()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                monthsViewModel.months.collect { adapter.setItems(it) }
            }
        }

        binding.backIconMonth.setOnClickListener {
            finish()
        }

    }

    private fun setupYearSelector() {
        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.yearSpinner.selectedItem?.toString()?.toIntOrNull()?.let(monthsViewModel::selectYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(monthsViewModel.availableYears, monthsViewModel.selectedYear) { years, selected -> years to selected }
                    .collect { (years, selected) -> binding.yearSpinner.renderYears(years, selected) }
            }
        }
    }

}
