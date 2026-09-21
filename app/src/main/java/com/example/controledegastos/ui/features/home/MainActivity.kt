package com.example.controledegastos.ui.features.home

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
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
import com.example.controledegastos.databinding.ActivityMainBinding
import com.example.controledegastos.listeners.OnClickInterface
import com.example.controledegastos.ui.adapter.MyAdapter
import com.example.controledegastos.ui.renderYears
import com.example.controledegastos.ui.configureListMotion
import com.example.controledegastos.ui.setupFilterBar
import com.example.controledegastos.ui.render
import com.example.controledegastos.ui.drawerItemId
import com.example.controledegastos.ui.features.additem.AddItem
import com.example.controledegastos.ui.features.bardata.BarDataActivity
import com.example.controledegastos.ui.features.help.HelpActivity
import com.example.controledegastos.ui.features.months.MonthsActivity
import com.example.controledegastos.viewmodel.ItemsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnClickInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MyAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val viewModel: ItemsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MyAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.configureListMotion(this)
        setupDrawer()
        setupYearSelector()
        setupFilterBar(binding.transactionFilterBar, { viewModel.mainUiState.value.filter }, viewModel::selectMainFilter)
        observeState()
        binding.floatingActionAddItem.setOnClickListener { startActivity(AddItem.newIntent(this)) }
    }

    private fun observeState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.mainUiState.collect { state ->
                binding.transactionFilterBar.render(state.filter)
                binding.navView.setCheckedItem(state.filter.drawerItemId())
                adapter.setItems(state.items)
                binding.totalBalanceHome.text = buildString {
                    append("Saldo total: ")
                    append(state.balance)
                }
                binding.emptyState.isVisible = state.items.isEmpty()
            }
        }
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

    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(R.color.white)
        binding.navView.getHeaderView(0).findViewById<View>(R.id.closeDrawerButton)
            .setOnClickListener { drawerLayout.closeDrawer(GravityCompat.START) }
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_backup -> startActivity(Intent(this, com.example.controledegastos.ui.features.backup.BackupActivity::class.java))
                R.id.nav_home -> viewModel.applyMainAllFilter()
                R.id.nav_calendar -> startActivity(Intent(this, MonthsActivity::class.java))
                R.id.nav_resumo -> startActivity(Intent(this, BarDataActivity::class.java))
                R.id.nav_inflow -> viewModel.applyMainFlowFilter(FlowType.INFLOW.value)
                R.id.nav_outflow -> viewModel.applyMainFlowFilter(FlowType.OUTFLOW.value)
                R.id.nav_debts -> viewModel.applyMainCategoryFilter(CategoryType.DEBTS.value)
                R.id.nav_salary -> viewModel.applyMainCategoryFilter(CategoryType.SALARY.value)
                R.id.nav_investments -> viewModel.applyMainCategoryFilter(CategoryType.INVESTMENTS.value)
                R.id.nav_extraIncome -> viewModel.applyMainCategoryFilter(CategoryType.EXTRA_INCOME.value)
                R.id.nav_food -> viewModel.applyMainCategoryFilter(CategoryType.FOOD.value)
                R.id.nav_health -> viewModel.applyMainCategoryFilter(CategoryType.HEALTH.value)
                R.id.nav_transport -> viewModel.applyMainCategoryFilter(CategoryType.TRANSPORT.value)
                R.id.nav_leisure -> viewModel.applyMainCategoryFilter(CategoryType.LEISURE.value)
                R.id.nav_edu -> viewModel.applyMainCategoryFilter(CategoryType.EDUCATION.value)
                R.id.nav_others -> viewModel.applyMainCategoryFilter(CategoryType.OTHERS.value)
                R.id.nav_help -> startActivity(Intent(this, HelpActivity::class.java))
                R.id.nav_exit -> finish()
                else -> return@setNavigationItemSelectedListener false
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            if (item.itemId != R.id.nav_calendar && item.itemId != R.id.nav_resumo &&
                item.itemId != R.id.nav_help && item.itemId != R.id.nav_exit) {
            }
            true
        }
    }

    override fun onClickDelete(id: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Deletar Item")
            .setMessage("Deseja deletar este item?")
            .setPositiveButton("Sim") { _, _ -> viewModel.deleteItem(id) }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onClickEdit(items: Items, value: String) {
        startActivity(AddItem.newIntent(this, items))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) { super.onPostCreate(savedInstanceState); toggle.syncState() }
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home) { drawerLayout.openDrawer(GravityCompat.START); true } else super.onOptionsItemSelected(item)
}
