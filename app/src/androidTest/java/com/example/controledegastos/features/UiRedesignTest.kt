package com.example.controledegastos.features

import android.content.pm.ActivityInfo
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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

/** Exercises the XML screens without modifying the user's transactions. */
@HiltAndroidTest
class UiRedesignTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun inject() = hiltRule.inject()

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
    fun annualSummaryRemainsReachableInLandscape() {
        ActivityScenario.launch(BarDataActivity::class.java).use { scenario ->
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            onView(withId(R.id.textTotaloutflow)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.barGrat)).perform(scrollTo()).check(matches(isDisplayed()))
        }
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
    fun monthlyDetailInflatesWithScrollableSummary() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FilterMonthActivity::class.java)
            .putExtra(FilterMonthActivity.EXTRA_YEAR_MONTH, 202609)
        ActivityScenario.launch<FilterMonthActivity>(intent).use {
            onView(withId(R.id.total_balance)).check(matches(isDisplayed()))
            onView(withId(R.id.floatingActionButtonBack)).perform(click())
        }
    }
}
