package com.example.controledegastos.features

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.Espresso.pressBack
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.controledegastos.R
import com.example.controledegastos.ui.features.additem.AddItem
import com.example.controledegastos.ui.features.backup.BackupActivity
import com.example.controledegastos.ui.features.bardata.BarDataActivity
import com.example.controledegastos.ui.features.help.HelpActivity
import com.example.controledegastos.ui.features.home.MainActivity
import com.example.controledegastos.ui.features.months.FilterMonthActivity
import com.example.controledegastos.ui.features.months.MonthsActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SystemCompatibilityTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Before fun setup() = hilt.inject()

    @Test fun systemBackClosesDrawerWithoutLeavingScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(MainActivity::class.java to R.id.drawer_layout,
            FilterMonthActivity::class.java to R.id.drawer_layout_filter).forEach { (screen, drawerId) ->
            val intent = Intent(context, screen).putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202409)
            ActivityScenario.launch<AppCompatActivity>(intent).use { scenario ->
                onView(withId(drawerId)).perform(DrawerActions.open())
                pressBack()
                onView(withId(drawerId)).perform(object : androidx.test.espresso.ViewAction {
                    override fun getConstraints() = isDisplayed()
                    override fun getDescription() = "Wait for system back to close the drawer"
                    override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                        val drawer = view as DrawerLayout
                        val deadline = android.os.SystemClock.uptimeMillis() + 3_000
                        while (drawer.isDrawerVisible(GravityCompat.START) && android.os.SystemClock.uptimeMillis() < deadline) {
                            controller.loopMainThreadForAtLeast(16)
                        }
                        assertTrue(!drawer.isDrawerVisible(GravityCompat.START))
                    }
                })
                scenario.onActivity { assertTrue(!it.isFinishing) }
            }
        }
    }

    @Test fun allScreensKeepContentOutsideSystemBarsAndCutouts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val screens = listOf(MainActivity::class.java, AddItem::class.java, MonthsActivity::class.java,
            FilterMonthActivity::class.java, BarDataActivity::class.java, BackupActivity::class.java, HelpActivity::class.java)
        screens.forEach { screen ->
            val intent = Intent(context, screen).putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202409)
            ActivityScenario.launch<AppCompatActivity>(intent).use { scenario ->
                onView(isRoot()).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    val content = activity.findViewById<ViewGroup>(android.R.id.content)
                    val safe = requireNotNull(ViewCompat.getRootWindowInsets(content))
                        .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    val bounds = Rect()
                    content.getChildAt(0).getGlobalVisibleRect(bounds)
                    assertTrue("${screen.simpleName}: top", bounds.top >= safe.top)
                    assertTrue("${screen.simpleName}: bottom", bounds.bottom <= activity.window.decorView.height - safe.bottom)
                    assertTrue("${screen.simpleName}: usable height", bounds.height() > 0)
                }
            }
        }
    }
}
