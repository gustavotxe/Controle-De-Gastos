package com.example.controledegastos.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

fun AppCompatActivity.configureDrawerBack(drawer: DrawerLayout) {
    val callback = object : OnBackPressedCallback(drawer.isDrawerOpen(GravityCompat.START)) {
        override fun handleOnBackPressed() = drawer.closeDrawer(GravityCompat.START)
    }

    onBackPressedDispatcher.addCallback(this, callback)
    val listener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerOpened(drawerView: View) { callback.isEnabled = true }
        override fun onDrawerClosed(drawerView: View) { callback.isEnabled = false }
    }
    drawer.addDrawerListener(listener)

    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            drawer.removeDrawerListener(listener)
            owner.lifecycle.removeObserver(this)
        }
    })
}
