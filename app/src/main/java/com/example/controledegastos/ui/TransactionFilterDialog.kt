package com.example.controledegastos.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.controledegastos.R
import com.example.controledegastos.databinding.TransactionFilterBarBinding
import com.example.controledegastos.ui.model.TransactionFilter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TransactionFilterDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val filters = TransactionFilter.entries
        val selected = filters.indexOfFirst { it.name == arguments?.getString(SELECTION) }.coerceAtLeast(0)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_choose)
            .setSingleChoiceItems(filters.map { it.label(requireContext()) }.toTypedArray(), selected) { _, position ->
                parentFragmentManager.setFragmentResult(RESULT, bundleOf(SELECTION to filters[position].name))
                dismiss()
            }
            .setNegativeButton(R.string.filter_cancel, null)
            .create()
    }

    companion object {
        const val RESULT = "transaction_filter_result"
        const val SELECTION = "transaction_filter_selection"
        const val TAG = "transaction_filter_dialog"
    }
}

fun AppCompatActivity.setupFilterBar(
    binding: TransactionFilterBarBinding,
    current: () -> TransactionFilter,
    select: (TransactionFilter) -> Unit
) {
    supportFragmentManager.setFragmentResultListener(TransactionFilterDialog.RESULT, this) { _, result ->
        TransactionFilter.entries.firstOrNull { it.name == result.getString(TransactionFilterDialog.SELECTION) }
            ?.let(select)
    }
    binding.root.setOnClickListener {
        if (!supportFragmentManager.isStateSaved &&
            supportFragmentManager.findFragmentByTag(TransactionFilterDialog.TAG) == null) {
            TransactionFilterDialog().apply {
                arguments = bundleOf(TransactionFilterDialog.SELECTION to current().name)
            }.showNow(supportFragmentManager, TransactionFilterDialog.TAG)
        }
    }
}

fun TransactionFilterBarBinding.render(filter: TransactionFilter) {
    val label = filter.label(root.context)
    activeFilterLabel.text = root.context.getString(R.string.filter_active_label, label)
    root.contentDescription = root.context.getString(R.string.filter_accessibility, label)
}

private fun TransactionFilter.label(context: Context): String = when (this) {
    TransactionFilter.ALL -> context.getString(R.string.filter_all)
    TransactionFilter.INFLOW -> context.getString(R.string.inflow)
    TransactionFilter.OUTFLOW -> context.getString(R.string.outflow)
    else -> requireNotNull(category)
}

fun TransactionFilter.drawerItemId(month: Boolean = false): Int = when (this) {
    TransactionFilter.ALL -> if (month) R.id.nav_homeF else R.id.nav_home
    TransactionFilter.INFLOW -> if (month) R.id.nav_inflowF else R.id.nav_inflow
    TransactionFilter.OUTFLOW -> if (month) R.id.nav_outflowF else R.id.nav_outflow
    TransactionFilter.SALARY -> if (month) R.id.nav_salaryF else R.id.nav_salary
    TransactionFilter.INVESTMENTS -> if (month) R.id.nav_investmentsF else R.id.nav_investments
    TransactionFilter.EXTRA_INCOME -> if (month) R.id.nav_extraIncomeF else R.id.nav_extraIncome
    TransactionFilter.DEBTS -> if (month) R.id.nav_debtsF else R.id.nav_debts
    TransactionFilter.FOOD -> if (month) R.id.nav_foodF else R.id.nav_food
    TransactionFilter.HEALTH -> if (month) R.id.nav_healthF else R.id.nav_health
    TransactionFilter.TRANSPORT -> if (month) R.id.nav_transportF else R.id.nav_transport
    TransactionFilter.EDUCATION -> if (month) R.id.nav_eduF else R.id.nav_edu
    TransactionFilter.LEISURE -> if (month) R.id.nav_leisureF else R.id.nav_leisure
    TransactionFilter.OTHERS -> if (month) R.id.nav_othersF else R.id.nav_others
}
