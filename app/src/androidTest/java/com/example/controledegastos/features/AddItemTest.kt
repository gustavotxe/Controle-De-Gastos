package com.example.controledegastos.features

import android.widget.DatePicker
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.RootMatchers.isDialog
import com.example.controledegastos.database.HiltDatabaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.controledegastos.R
import com.example.controledegastos.ui.features.additem.AddItem
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddItemTest : HiltDatabaseTest() {
    private lateinit var scenario: ActivityScenario<AddItem>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(AddItem::class.java)
    }

    @After fun closeActivity() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun datePickerTest(){
        onView(withId(R.id.textDateSelected)).perform(scrollTo(), click())

        onView(withClassName(equalTo(DatePicker::class.java.name)))
            .perform(PickerActions.setDate(2025, 1, 15))

        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

        onView(withId(R.id.textDateSelected)).check(matches(withText("15/01/2025")))

    }

    @Test
    fun fieldsAcceptTheirLimitAndTruncateExcess(){
        listOf(Triple(R.id.DescId, 120, "A"), Triple(R.id.editValue, 14, "1"),
            Triple(R.id.ObsId, 650, "A")).forEach { (id, limit, character) ->
            for (length in listOf(limit - 1, limit, limit + 1)) {
                onView(withId(id)).perform(scrollTo(), replaceText(character.repeat(length)), closeSoftKeyboard())
                    .check(matches(withText(character.repeat(minOf(length, limit)))))
            }
        }
    }

    @Test
    fun verifySpinner() {
        onView(withId(R.id.spinnerIO)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Saída"))).perform(click())
        onView(withId(R.id.spinnerIO)).check(matches(withSpinnerText(containsString("Saída"))))
    }

}
