package com.example.controledegastos.ui.features.additem

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.controledegastos.ui.configureSystemInsets
import com.example.controledegastos.R
import com.example.controledegastos.data.model.CategoryType
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.databinding.ActivityAddItemBinding
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionDate
import com.example.controledegastos.viewmodel.ItemsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class AddItem : AppCompatActivity() {
    private lateinit var binding: ActivityAddItemBinding
    private val viewModel: ItemsViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private var editingId: Int? = null
    private var dateSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureSystemInsets()
        setupSpinners()
        restoreEditingItem(savedInstanceState)
        setupDatePicker()
        binding.saveNote.setOnClickListener { save() }
        binding.buttonCancel.setOnClickListener { finish() }
        binding.backIcon.setOnClickListener { finish() }
    }

    private fun setupSpinners() {
        binding.spinnerIO.adapter = fieldAdapter(FlowType.entries.map { it.value })
        binding.spinnerPayment.adapter = fieldAdapter(PAYMENT_METHODS)
        binding.spinnerCategory.adapter = fieldAdapter(CategoryType.valuesList)
    }

    private fun fieldAdapter(values: List<String>) =
        ArrayAdapter(this, R.layout.spinner_field_item, values).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

    private fun restoreEditingItem(savedState: Bundle?) {
        editingId = savedState?.getInt(STATE_ID)?.takeIf { it > 0 } ?: intent.getIntExtra(EXTRA_ID, -1).takeIf { it > 0 }
        val dateMillis = savedState?.getLong(STATE_DATE) ?: intent.getLongExtra(EXTRA_DATE, 0L)
        dateSelected = savedState?.getBoolean(STATE_DATE_SELECTED) ?: (dateMillis > 0)
        if (dateSelected) calendar.timeInMillis = dateMillis

        binding.DescId.setText(savedState?.getString(STATE_DESCRIPTION) ?: intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty())
        binding.ObsId.setText(savedState?.getString(STATE_OBSERVATION) ?: intent.getStringExtra(EXTRA_OBSERVATION).orEmpty())
        binding.editValue.setText(savedState?.getString(STATE_AMOUNT) ?: if (editingId != null) editingAmount() else "")

        select(binding.spinnerIO, savedState?.getString(STATE_FLOW) ?: intent.getStringExtra(EXTRA_FLOW).orEmpty())
        select(binding.spinnerPayment, savedState?.getString(STATE_PAYMENT) ?: intent.getStringExtra(EXTRA_PAYMENT).orEmpty())
        select(binding.spinnerCategory, savedState?.getString(STATE_CATEGORY) ?: intent.getStringExtra(EXTRA_CATEGORY).orEmpty())

        if (dateSelected) updateDate()
        if (editingId == null) return

        binding.toolbarTitle.setText(R.string.edit_transaction)
        binding.saveNote.setText(R.string.save_changes)
        binding.buttonCancel.visibility = View.VISIBLE
    }

    private fun editingAmount(): String =
        Money.formatInput(intent.getLongExtra(EXTRA_AMOUNT_CENTS, 0L))

    private fun select(spinner: android.widget.Spinner, value: String) {
        val position = (0 until spinner.count).firstOrNull { spinner.getItemAtPosition(it) == value } ?: -1
        if (position >= 0) spinner.setSelection(position)
    }

    private fun setupDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(year, month, day)
            dateSelected = true
            updateDate()
        }
        val showPicker = {
            DatePickerDialog(this, listener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.textViewDate.setOnClickListener { showPicker() }
        binding.textDateSelected.setOnClickListener { showPicker() }
    }

    private fun updateDate() { binding.textDateSelected.text = TransactionDate.format(TransactionDate.atStartOfSelectedDay(calendar)) }

    private fun save() {
        val amount = Money.parseToCents(binding.editValue.text.toString())
        if (amount == null || amount <= 0) {
            binding.editValue.error = getString(R.string.amount_invalid_brl)
            binding.editValue.requestFocus()
            return
        }
        if (binding.textDateSelected.text == "--/--/----") return showError("Insira uma data válida.")
        val flow = binding.spinnerIO.selectedItem.toString()
        val signedAmount = if (flow == FlowType.OUTFLOW.value) -amount else amount
        val dateMillis = TransactionDate.atStartOfSelectedDay(calendar)
        val item = Items(
            id = editingId ?: 0,
            description = binding.DescId.text.toString().trim(),
            observation = binding.ObsId.text.toString().trim(),
            io = flow,
            paymentMethod = binding.spinnerPayment.selectedItem.toString(),
            amountCents = signedAmount,
            occurredAtMillis = dateMillis,
            yearMonth = TransactionDate.yearMonth(dateMillis),
            category = binding.spinnerCategory.selectedItem.toString()
        )
        if (editingId == null) viewModel.insertItem(item) else viewModel.updateItem(item)
        finish()
    }

    private fun showError(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_ID, editingId ?: -1)
        outState.putLong(STATE_DATE, TransactionDate.atStartOfSelectedDay(calendar))
        outState.putBoolean(STATE_DATE_SELECTED, dateSelected)
        outState.putString(STATE_DESCRIPTION, binding.DescId.text.toString())
        outState.putString(STATE_OBSERVATION, binding.ObsId.text.toString())
        outState.putString(STATE_AMOUNT, binding.editValue.text.toString())
        outState.putString(STATE_FLOW, binding.spinnerIO.selectedItem.toString())
        outState.putString(STATE_PAYMENT, binding.spinnerPayment.selectedItem.toString())
        outState.putString(STATE_CATEGORY, binding.spinnerCategory.selectedItem.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val EXTRA_ID = "item_id"
        private const val EXTRA_DESCRIPTION = "description"
        private const val EXTRA_OBSERVATION = "observation"
        private const val EXTRA_FLOW = "flow"
        private const val EXTRA_PAYMENT = "payment"
        private const val EXTRA_AMOUNT_CENTS = "amount_cents"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_CATEGORY = "category"
        private const val STATE_ID = "state_id"
        private const val STATE_DATE = "state_date"
        private const val STATE_DATE_SELECTED = "state_date_selected"
        private const val STATE_DESCRIPTION = "state_description"
        private const val STATE_OBSERVATION = "state_observation"
        private const val STATE_AMOUNT = "state_amount"
        private const val STATE_FLOW = "state_flow"
        private const val STATE_PAYMENT = "state_payment"
        private const val STATE_CATEGORY = "state_category"
        private val PAYMENT_METHODS = listOf("Pagamento à vista", "Pagamento a prazo")

        fun newIntent(context: Context, item: Items? = null): Intent = Intent(context, AddItem::class.java).apply {
            item ?: return@apply
            putExtra(EXTRA_ID, item.id)
            putExtra(EXTRA_DESCRIPTION, item.description)
            putExtra(EXTRA_OBSERVATION, item.observation)
            putExtra(EXTRA_FLOW, item.io)
            putExtra(EXTRA_PAYMENT, item.paymentMethod)
            putExtra(EXTRA_AMOUNT_CENTS, item.amountCents)
            putExtra(EXTRA_DATE, item.occurredAtMillis)
            putExtra(EXTRA_CATEGORY, item.category)
        }
    }
}
