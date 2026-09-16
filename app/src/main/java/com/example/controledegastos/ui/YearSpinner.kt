package com.example.controledegastos.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView

fun Spinner.renderYears(years: List<Int>, selectedYear: Int) {
    if (years.isEmpty()) return
    val values = years.map(Int::toString)
    val currentValues = (0 until count).map { getItemAtPosition(it).toString() }
    if (currentValues != values) {
        adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).styleYearText()

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getDropDownView(position, convertView, parent).styleYearDropdown()
        }
    }
    val position = values.indexOf(selectedYear.toString())
    if (position >= 0 && selectedItemPosition != position) setSelection(position, false)
}

private fun View.styleYearDropdown(): View {
    styleYearText()
    val horizontal = 20.dp
    val vertical = 14.dp
    setPadding(horizontal, vertical, horizontal, vertical)
    minimumHeight = 56.dp
    (this as? TextView)?.textSize = 18f
    return this
}

private fun View.styleYearText(): View {
    (this as? TextView)?.apply {
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        textSize = 15f
    }
    return this
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
