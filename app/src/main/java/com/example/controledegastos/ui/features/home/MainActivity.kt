package com.example.controledegastos.ui.features.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
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
import com.example.controledegastos.databinding.ActivityMainBinding
import com.example.controledegastos.listeners.OnClickInterface
import com.example.controledegastos.ui.adapter.MyAdapter
import com.example.controledegastos.ui.features.additem.AddItem
import com.example.controledegastos.ui.features.bardata.BarDataActivity
import com.example.controledegastos.ui.features.help.HelpActivity
import com.example.controledegastos.ui.features.months.MonthsActivity
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionDate
import com.example.controledegastos.viewmodel.ItemsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        setupDrawer()
        observeState()
        binding.floatingActionAddItem.setOnClickListener { startActivity(AddItem.newIntent(this)) }
    }

    private fun observeState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.mainUiState.collect { state ->
                adapter.setItems(state.items)
                binding.toolbarBalance.text = state.balance
                binding.toolbarBalance.setTextColor(
                    if (state.balance.startsWith("-")) Color.parseColor("#FFEB3B") else Color.parseColor("#1ff024")
                )
                binding.toolbarBalance.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
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
            true
        }
    }

    override fun onClickDelete(id: Int) {
        AlertDialog.Builder(this)
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
