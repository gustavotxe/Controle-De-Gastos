package com.example.controledegastos.ui

import android.provider.Settings
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun RecyclerView.configureListMotion(owner: LifecycleOwner) {
    val animator = itemAnimator
    itemAnimator = null
    (animator as? SimpleItemAnimator)?.apply {
        supportsChangeAnimations = false
        addDuration = 160L
        removeDuration = 120L
        moveDuration = 180L
    }
    val resolver = context.applicationContext.contentResolver
    owner.lifecycleScope.launch {
        val enabled = withContext(Dispatchers.IO) {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }
        if (enabled) itemAnimator = animator
    }
}
