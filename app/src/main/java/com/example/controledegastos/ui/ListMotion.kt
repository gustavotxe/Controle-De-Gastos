package com.example.controledegastos.ui

import android.provider.Settings
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/** Short, native list transitions; respect the device's reduced-motion setting. */
fun RecyclerView.configureListMotion() {
    if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
        itemAnimator = null
        return
    }
    (itemAnimator as? SimpleItemAnimator)?.apply {
        supportsChangeAnimations = false
        addDuration = 160L
        removeDuration = 120L
        moveDuration = 180L
    }
}
