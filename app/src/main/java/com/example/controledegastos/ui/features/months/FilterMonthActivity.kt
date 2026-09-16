package com.example.controledegastos.ui.features.months

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controledegastos.R
import com.example.controledegastos.data.model.CategoryType
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.databinding.ActivityFilterMonthBinding
import com.example.controledegastos.listeners.OnClickInterface
import com.example.controledegastos.ui.adapter.MyAdapter
import com.example.controledegastos.ui.features.additem.AddItem
import com.example.controledegastos.ui.features.help.HelpActivity
import com.example.controledegastos.ui.renderYears
import com.example.controledegastos.viewmodel.ItemsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class FilterMonthActivity : AppCompatActivity(), OnClickInterface {
    private lateinit var binding: ActivityFilterMonthBinding
    private lateinit var adapter: MyAdapter
    private lateinit var drawer: DrawerLayout
    private val viewModel: ItemsViewModel by viewModels()
    private val yearMonth by lazy { intent.getIntExtra(EXTRA_YEAR_MONTH, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (yearMonth == 0) { finish(); return }
        binding = ActivityFilterMonthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MyAdapter(this)
        binding.recyclerViewF.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewF.adapter = adapter
        setupDrawer()
        setupYearSelector()
        observeState()
        viewModel.applyMonthFilter(yearMonth)
        binding.floatingActionButtonBack.setOnClickListener { finish() }
    }

    private fun setupYearSelector() {
        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.yearSpinner.selectedItem?.toString()?.toIntOrNull()?.let(viewModel::selectYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.availableYears, viewModel.selectedYear) { years, selected -> years to selected }
                    .collect { (years, selected) -> binding.yearSpinner.renderYears(years, selected) }
            }
        }
    }

    private fun observeState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.monthUiState.collect { state ->
                adapter.setItems(state.items)
                binding.totalInflow.text = state.inflow
                binding.totalOutflow.text = state.outflow
                binding.totalBalance.text = state.balance
            }
        }
    }

    private fun setupDrawer() {
        drawer = binding.drawerLayoutFilter
        val toggle = ActionBarDrawerToggle(this, drawer, binding.toolbarF, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        binding.navViewFilter.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_homeF -> viewModel.applyMonthFilter(yearMonth)
                R.id.nav_inflowF -> viewModel.applyMonthFilter(yearMonth, flow = FlowType.INFLOW.value)
                R.id.nav_outflowF -> viewModel.applyMonthFilter(yearMonth, flow = FlowType.OUTFLOW.value)
                R.id.nav_salaryF -> category(CategoryType.SALARY.value)
                R.id.nav_investmentsF -> category(CategoryType.INVESTMENTS.value)
                R.id.nav_extraIncomeF -> category(CategoryType.EXTRA_INCOME.value)
                R.id.nav_debtsF -> category(CategoryType.DEBTS.value)
                R.id.nav_foodF -> category(CategoryType.FOOD.value)
                R.id.nav_healthF -> category(CategoryType.HEALTH.value)
                R.id.nav_transportF -> category(CategoryType.TRANSPORT.value)
                R.id.nav_leisureF -> category(CategoryType.LEISURE.value)
                R.id.nav_eduF -> category(CategoryType.EDUCATION.value)
                R.id.nav_othersF -> category(CategoryType.OTHERS.value)
                R.id.nav_deleteF -> confirmDeleteMonth()
                R.id.nav_helpF -> startActivity(Intent(this, HelpActivity::class.java))
                R.id.nav_exitF -> finish()
                else -> return@setNavigationItemSelectedListener false
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun category(value: String) { viewModel.applyMonthFilter(yearMonth, category = value) }

    private fun confirmDeleteMonth() {
        AlertDialog.Builder(this)
            .setTitle("Deletar lançamentos")
            .setMessage("Deseja deletar todos os lançamentos deste mês?")
            .setPositiveButton("Sim") { _, _ -> viewModel.deleteItemMonth(yearMonth); finish() }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onClickDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Deletar Item")
            .setMessage("Deseja deletar este item?")
            .setPositiveButton("Sim") { _, _ -> viewModel.deleteItem(id) }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onClickEdit(items: Items, value: String) { startActivity(AddItem.newIntent(this, items)) }

    companion object { const val EXTRA_YEAR_MONTH = "year_month" }
}
