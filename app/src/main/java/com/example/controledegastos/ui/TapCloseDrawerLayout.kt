package com.example.controledegastos.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.drawerlayout.widget.DrawerLayout

/**
 * Keeps an open drawer out of the horizontal drag recognizer so diagonal menu
 * scrolling cannot close it. Outside taps, buttons and system back still work.
 * Opening gestures continue to use DrawerLayout's normal behavior.
 */
class TapCloseDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {
    private val drawerBounds = Rect()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var openDrawerAtDown: View? = null
    private var outsideTap = false
    private var downX = 0f
    private var downY = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            openDrawerAtDown = findVisibleDrawer()
            downX = event.x
            downY = event.y
            openDrawerAtDown?.getHitRect(drawerBounds)
            outsideTap = openDrawerAtDown != null &&
                !drawerBounds.contains(event.x.toInt(), event.y.toInt())
        }
        if (openDrawerAtDown == null) return super.onInterceptTouchEvent(event)
        // Cancel a horizontal swipe rather than letting a menu row treat its UP
        // as a click. Vertical/mostly vertical movement stays with the menu.
        val dx = kotlin.math.abs(event.x - downX)
        val dy = kotlin.math.abs(event.y - downY)
        val horizontalSwipe = event.actionMasked == MotionEvent.ACTION_MOVE &&
            dx > touchSlop && dx > dy * 1.5f
        return outsideTap || horizontalSwipe
    }

    private fun findVisibleDrawer(): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if ((child.layoutParams as LayoutParams).gravity != 0 && isDrawerVisible(child)) {
                return child
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val drawer = openDrawerAtDown ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (kotlin.math.abs(event.x - downX) > touchSlop ||
                    kotlin.math.abs(event.y - downY) > touchSlop) {
                    outsideTap = false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> outsideTap = false
            MotionEvent.ACTION_UP -> {
                if (outsideTap && kotlin.math.abs(event.x - downX) <= touchSlop &&
                    kotlin.math.abs(event.y - downY) <= touchSlop) {
                    closeDrawer(drawer)
                    performClick()
                }
                outsideTap = false
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
