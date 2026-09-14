package com.example.controledegastos.ui.model

data class MonthSummaryUi(
    val yearMonth: Int,
    val monthName: String,
    val inflowText: String,
    val outflowText: String,
    val balanceText: String,
    val inflowPie: Float,
    val outflowPie: Float,
    val hasData: Boolean
)
