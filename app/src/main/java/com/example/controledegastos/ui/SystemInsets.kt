package com.example.controledegastos.ui

import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.controledegastos.R

fun AppCompatActivity.configureSystemInsets() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )

    val content = findViewById<ViewGroup>(android.R.id.content)
    content.setBackgroundColor(getColor(R.color.brand))
    content.getChildAt(0)?.let { root ->
        if (root.background == null) root.setBackgroundColor(getColor(R.color.page))
    }

    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(left = safe.left, top = safe.top, right = safe.right, bottom = maxOf(safe.bottom, keyboard.bottom))
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)

}
