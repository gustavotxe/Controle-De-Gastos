package com.example.controledegastos.ui.features.months

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
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
import com.example.controledegastos.ui.configureListMotion
import com.example.controledegastos.ui.setupFilterBar
import com.example.controledegastos.ui.render
import com.example.controledegastos.ui.drawerItemId
import com.example.controledegastos.viewmodel.ItemsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Locale

@AndroidEntryPoint
class FilterMonthActivity : AppCompatActivity(), OnClickInterface {
    private lateinit var binding: ActivityFilterMonthBinding
    private lateinit var adapter: MyAdapter
    private lateinit var drawer: DrawerLayout
    private val viewModel: ItemsViewModel by viewModels()
    private val yearMonth by lazy { intent.getIntExtra(EXTRA_YEAR_MONTH, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (yearMonth / 100 <= 0 || yearMonth % 100 !in 1..12) { finish(); return }
        binding = ActivityFilterMonthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MyAdapter(this)
        binding.recyclerViewF.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewF.adapter = adapter
        binding.recyclerViewF.configureListMotion(this)
        binding.floatingActionButtonBack.setOnClickListener { finish() }

        setupDrawer()
        setupMonthHeader()
        viewModel.initializeMonth(yearMonth)
        setupFilterBar(binding.transactionFilterBar, { viewModel.monthUiState.value.filter }, viewModel::selectMonthFilter)
        observeState()
    }

    private fun setupMonthHeader() {
        val year = yearMonth / 100
        val monthName = DateFormatSymbols(Locale("pt", "BR")).months[yearMonth % 100 - 1]
        binding.monthTitle.text = getString(R.string.monthly_transactions_title, monthName, year.toString())
        viewModel.selectYear(year)
    }

    private fun observeState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.monthUiState.collect { state ->
                binding.transactionFilterBar.render(state.filter)
                binding.navViewFilter.setCheckedItem(state.filter.drawerItemId(month = true))
                adapter.setItems(state.items)
                binding.emptyState.isVisible = state.items.isEmpty()
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
        toggle.drawerArrowDrawable.color = getColor(R.color.white)
        binding.navViewFilter.getHeaderView(0).findViewById<View>(R.id.closeDrawerButton)
            .setOnClickListener { drawer.closeDrawer(GravityCompat.START) }
        binding.navViewFilter.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_backup -> startActivity(Intent(this, com.example.controledegastos.ui.features.backup.BackupActivity::class.java))
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Deletar lançamentos")
            .setMessage("Deseja deletar todos os lançamentos deste mês?")
            .setPositiveButton("Sim") { _, _ -> viewModel.deleteItemMonth(yearMonth); finish() }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onClickDelete(id: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Deletar Item")
            .setMessage("Deseja deletar este item?")
            .setPositiveButton("Sim") { _, _ -> viewModel.deleteItem(id) }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onClickEdit(items: Items, value: String) { startActivity(AddItem.newIntent(this, items)) }

    companion object { const val EXTRA_YEAR_MONTH = "year_month" }
}
