package com.example.controledegastos.features

import android.content.pm.ActivityInfo
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.Press
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.controledegastos.R
import com.example.controledegastos.ui.features.additem.AddItem
import com.example.controledegastos.ui.features.bardata.BarDataActivity
import com.example.controledegastos.ui.features.help.HelpActivity
import com.example.controledegastos.ui.features.home.MainActivity
import com.example.controledegastos.ui.features.months.MonthsActivity
import com.example.controledegastos.ui.features.months.FilterMonthActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals

/** Exercises the XML screens without modifying the user's transactions. */
@HiltAndroidTest
class UiRedesignTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun longTransactionCardsUseBoundedPreviewsAndScreensRecreate() {
        val item = com.example.controledegastos.data.model.Items(1, "d".repeat(100_000),
            "n".repeat(100_000), "Entrada", "p".repeat(100_000), 100, 1726444800000L,
            202409, "c".repeat(100_000))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val listener = object : com.example.controledegastos.listeners.OnClickInterface {
                    override fun onClickDelete(id: Int) = Unit
                    override fun onClickEdit(items: com.example.controledegastos.data.model.Items, value: String) = Unit
                }
                val adapter = com.example.controledegastos.ui.adapter.MyAdapter(listener)
                adapter.submitList(listOf(item))
                val holder = adapter.onCreateViewHolder(activity.findViewById<RecyclerView>(R.id.recyclerView), 0)
                adapter.onBindViewHolder(holder, 0)
                assertEquals(121, holder.desc.text.length)
                assertEquals(651, holder.obs.text.length)
                assertEquals(121, holder.payMethod.text.length)
                assertEquals(121, holder.ctg.text.length)
                assertEquals(2, holder.desc.maxLines)
                assertEquals(4, holder.obs.maxLines)
                assertEquals(100_000, item.observation.length)
            }
            scenario.recreate()
        }
        ActivityScenario.launch(MonthsActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<RecyclerView>(R.id.recyclerViewMonths).adapter != null)
            }
        }
    }

    @Test
    fun amountFieldAcceptsBrazilianGroupingAndRejectsInvalidDecimals() {
        ActivityScenario.launch(AddItem::class.java).use { scenario ->
            onView(withId(R.id.editValue)).perform(scrollTo(), replaceText("7.000"), closeSoftKeyboard())
                .check(matches(withText("7.000")))
            onView(withId(R.id.editValue)).perform(replaceText("7.000,00"), closeSoftKeyboard())
                .check(matches(withText("7.000,00")))
            scenario.recreate()
            onView(withId(R.id.editValue)).perform(scrollTo()).check(matches(withText("7.000,00")))
            scenario.onActivity { activity ->
                val text = activity.findViewById<android.widget.EditText>(R.id.editValue).text.toString()
                assertEquals(700_000L, com.example.controledegastos.ui.model.Money.parseToCents(text))
            }
            onView(withId(R.id.editValue)).perform(replaceText("7,000"), closeSoftKeyboard())
            scenario.onActivity { it.findViewById<View>(R.id.saveNote).performClick() }
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            onView(withId(R.id.editValue)).check(matches(hasErrorText(context.getString(R.string.amount_invalid_brl))))
        }
    }

    @Test
    fun homeFilterBarAndDrawerShareSelectionAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.transactionFilterBar)).perform(click())
            onView(withText(R.string.inflow)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.activeFilterLabel)).check(matches(withText("Filtro: Entradas")))
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_outflow))
            onView(withId(R.id.activeFilterLabel)).check(matches(withText("Filtro: Saídas")))
            scenario.recreate()
            onView(withId(R.id.activeFilterLabel)).check(matches(withText("Filtro: Saídas")))
            onView(withId(R.id.transactionFilterBar)).perform(click())
            onData(equalTo("Todos os lançamentos")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.activeFilterLabel)).check(matches(withText(R.string.filter_default_label)))
        }
    }

    @Test
    fun monthlyFilterAndOpenPickerSurviveRecreation() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FilterMonthActivity::class.java)
            .putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202409)
        ActivityScenario.launch<FilterMonthActivity>(intent).use { scenario ->
            onView(withId(R.id.transactionFilterBar)).perform(click())
            scenario.recreate()
            onView(withText("Salário")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.activeFilterLabel)).check(matches(withText("Filtro: Salário")))
            scenario.recreate()
            onView(withId(R.id.activeFilterLabel)).check(matches(withText("Filtro: Salário")))
            onView(withId(R.id.drawer_layout_filter)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view_filter)).perform(NavigationViewActions.navigateTo(R.id.nav_homeF))
            onView(withId(R.id.activeFilterLabel)).check(matches(withText(R.string.filter_default_label)))
            onView(withId(R.id.monthTitle)).check(matches(withText("Lançamentos de setembro de 2024")))
        }
    }

    @Test
    fun backupScreenIsAccessibleAfterRecreation() {
        ActivityScenario.launch(com.example.controledegastos.ui.features.backup.BackupActivity::class.java).use { scenario ->
            onView(withId(R.id.exportButton)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.importButton)).perform(scrollTo()).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withId(R.id.backupStatus)).perform(scrollTo()).check(matches(withText(R.string.backup_ready)))
        }
    }

    @Test
    fun homeActionOpensScrollableForm() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.floatingActionAddItem)).perform(click())
            onView(withId(R.id.DescId)).check(matches(isDisplayed()))
            onView(withId(R.id.saveNote)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.backIcon)).perform(click())
        }
    }

    @Test
    fun formKeepsDraftAndDateAfterRecreation() {
        ActivityScenario.launch(AddItem::class.java).use { scenario ->
            onView(withId(R.id.DescId)).perform(scrollTo(), replaceText("Mercado"), closeSoftKeyboard())
            onView(withId(R.id.editValue)).perform(scrollTo(), replaceText("123,45"), closeSoftKeyboard())
            onView(withId(R.id.textDateSelected)).perform(scrollTo(), click())
            onView(withClassName(equalTo("android.widget.DatePicker")))
                .perform(PickerActions.setDate(2026, 9, 16))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.ObsId)).perform(scrollTo(), replaceText("Compra da semana"), closeSoftKeyboard())
            scenario.recreate()
            onView(withId(R.id.DescId)).perform(scrollTo()).check(matches(withText("Mercado")))
            onView(withId(R.id.editValue)).perform(scrollTo()).check(matches(withText("123,45")))
            onView(withId(R.id.textDateSelected)).perform(scrollTo()).check(matches(withText("16/09/2026")))
            onView(withId(R.id.ObsId)).perform(scrollTo()).check(matches(withText("Compra da semana")))
        }
    }

    @Test
    fun annualChartFillsSpaceAboveFixedTotalsAfterRotation() {
        ActivityScenario.launch(BarDataActivity::class.java).use { scenario ->
            for (orientation in listOf(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )) {
                scenario.onActivity { it.requestedOrientation = orientation }
                onView(withId(R.id.textTotalbalance)).check(matches(isDisplayed()))
                onView(withId(R.id.textTotalinflow)).check(matches(isDisplayed()))
                onView(withId(R.id.textTotaloutflow)).check(matches(isDisplayed()))
                onView(withId(R.id.barGrat)).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    val chart = Rect()
                    val totals = Rect()
                    val card = Rect()
                    activity.findViewById<View>(R.id.barGrat).getGlobalVisibleRect(chart)
                    activity.findViewById<View>(R.id.annualTotals).getGlobalVisibleRect(totals)
                    activity.findViewById<View>(R.id.annualCard).getGlobalVisibleRect(card)
                    assertTrue("Chart must have space and not overlap the totals", chart.height() > 0 && chart.bottom <= totals.top)
                    val inset = (16 * activity.resources.displayMetrics.density).toInt()
                    assertTrue("Totals must stay at the bottom of the card", card.bottom - totals.bottom in 0..inset)
                }
            }
        }
    }

    @Test
    fun homeDrawerIgnoresSwipesButClosesWithButtonAndOutsideTap() {
        ActivityScenario.launch(MainActivity::class.java).use {
            checkManualDrawer(R.id.drawer_layout, R.id.nav_view)
        }
    }

    @Test
    fun monthlyDrawerIgnoresSwipesButClosesWithButtonAndOutsideTap() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FilterMonthActivity::class.java)
            .putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202609)
        ActivityScenario.launch<FilterMonthActivity>(intent).use {
            checkManualDrawer(R.id.drawer_layout_filter, R.id.nav_view_filter)
        }
    }

    private fun checkManualDrawer(drawerId: Int, menuId: Int) {
        onView(withId(drawerId)).perform(DrawerActions.open())
        onView(withId(menuId)).perform(swipeLeft())
        onView(withId(drawerId)).check(matches(DrawerMatchers.isOpen()))
        onView(withId(R.id.closeDrawerButton)).perform(click())
        waitForDrawerClosed(drawerId)
        onView(withId(drawerId)).check(matches(DrawerMatchers.isClosed()))
        onView(withId(drawerId)).perform(DrawerActions.open())
        onView(withId(menuId)).perform(swipeUp())
        onView(withId(drawerId)).check(matches(DrawerMatchers.isOpen()))
        onView(withId(drawerId)).perform(GeneralClickAction(Tap.SINGLE, { view ->
            val position = IntArray(2)
            view.getLocationOnScreen(position)
            floatArrayOf(position[0] + view.width - 8f, position[1] + view.height / 2f)
        }, Press.FINGER, InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.BUTTON_PRIMARY))
        waitForDrawerClosed(drawerId)
        onView(withId(drawerId)).check(matches(DrawerMatchers.isClosed()))
    }

    private fun waitForDrawerClosed(drawerId: Int) {
        onView(withId(drawerId)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isDisplayed()
            override fun getDescription() = "Wait for the drawer closing animation"
            override fun perform(controller: androidx.test.espresso.UiController, view: View) {
                val drawer = view as androidx.drawerlayout.widget.DrawerLayout
                val deadline = android.os.SystemClock.uptimeMillis() + 3_000
                while (drawer.isDrawerVisible(androidx.core.view.GravityCompat.START) &&
                    android.os.SystemClock.uptimeMillis() < deadline) {
                    controller.loopMainThreadForAtLeast(16)
                }
            }
        })
    }

    @Test
    fun monthlyOverviewAndHelpInflate() {
        ActivityScenario.launch(MonthsActivity::class.java).use {
            onView(withId(R.id.recyclerViewMonths)).check(matches(isDisplayed()))
            onView(withId(R.id.yearSpinner)).check(matches(isDisplayed()))
        }
        ActivityScenario.launch(HelpActivity::class.java).use {
            onView(withText(R.string.help_storage_question)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun monthlyDetailKeepsHeaderFixedWhileOnlyTransactionsScroll() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FilterMonthActivity::class.java)
            .putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202409)
        ActivityScenario.launch<FilterMonthActivity>(intent).use { scenario ->
            onView(withId(R.id.monthTitle)).check(matches(withText("Lançamentos de setembro de 2024")))
            onView(withId(R.id.yearSpinner)).check(doesNotExist())
            onView(withId(R.id.total_balance)).check(matches(isDisplayed()))
            val initialHeader = Rect()
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.fixedMonthHeader).getGlobalVisibleRect(initialHeader)
                // Populate only the view for this scrolling test; never write transactions.
                val list = activity.findViewById<RecyclerView>(R.id.recyclerViewF)
                list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun getItemCount() = 40
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val row = TextView(parent.context).apply {
                            layoutParams = RecyclerView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (72 * resources.displayMetrics.density).toInt()
                            )
                        }
                        return object : RecyclerView.ViewHolder(row) {}
                    }
                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        (holder.itemView as TextView).text = "Lançamento de teste $position"
                    }
                }
            }
            onView(withId(R.id.recyclerViewF)).perform(swipeUp())
            scenario.onActivity { activity ->
                val currentHeader = Rect()
                activity.findViewById<View>(R.id.fixedMonthHeader).getGlobalVisibleRect(currentHeader)
                assertEquals(initialHeader, currentHeader)
                assertTrue(activity.findViewById<RecyclerView>(R.id.recyclerViewF).canScrollVertically(-1))
            }
            onView(withId(R.id.sectionTitle)).check(matches(withText(R.string.transactions)))
            scenario.recreate()
            onView(withId(R.id.monthTitle)).check(matches(withText("Lançamentos de setembro de 2024")))
            onView(withId(R.id.floatingActionButtonBack)).perform(click())
        }
    }
}
