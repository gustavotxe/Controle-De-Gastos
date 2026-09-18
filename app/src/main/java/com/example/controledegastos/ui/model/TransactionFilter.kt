package com.example.controledegastos.ui.model

import com.example.controledegastos.data.model.CategoryType
import com.example.controledegastos.data.model.FlowType

/** One selection shared by the drawer, filter bar and transaction query. */
enum class TransactionFilter(val flow: String? = null, val category: String? = null) {
    ALL,
    INFLOW(flow = FlowType.INFLOW.value),
    OUTFLOW(flow = FlowType.OUTFLOW.value),
    SALARY(category = CategoryType.SALARY.value),
    INVESTMENTS(category = CategoryType.INVESTMENTS.value),
    EXTRA_INCOME(category = CategoryType.EXTRA_INCOME.value),
    DEBTS(category = CategoryType.DEBTS.value),
    FOOD(category = CategoryType.FOOD.value),
    HEALTH(category = CategoryType.HEALTH.value),
    TRANSPORT(category = CategoryType.TRANSPORT.value),
    EDUCATION(category = CategoryType.EDUCATION.value),
    LEISURE(category = CategoryType.LEISURE.value),
    OTHERS(category = CategoryType.OTHERS.value);

    companion object {
        fun from(flow: String? = null, category: String? = null): TransactionFilter =
            entries.firstOrNull { it.flow == flow && it.category == category } ?: ALL
    }
}
