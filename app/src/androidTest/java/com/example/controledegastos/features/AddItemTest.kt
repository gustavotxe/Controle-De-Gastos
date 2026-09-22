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
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.controledegastos.R
import com.example.controledegastos.ui.features.additem.AddItem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddItemTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRuleAddItem = ActivityScenarioRule(AddItem::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun datePickerTest(){
        //Teste do DatePicker
        onView(withId(R.id.textDateSelected)).perform(scrollTo(), click())

        onView(withClassName(equalTo(DatePicker::class.java.name)))
            .perform(PickerActions.setDate(2025, 1, 15))

        onView(withText("OK")).perform(click())

        onView(withId(R.id.textDateSelected)).check(matches(withText("15/01/2025")))

    }

    @Test
    fun editTextLimit(){
        //Teste de limite de caracteres dos EditTexts
        val textLenghtLimit = "A".repeat(650)
        val textLenghtTest = "A".repeat(650)

        val textLenghtLimitDouble = "1".repeat(14)
        val textLenghtTestDouble = "1".repeat(14)

        val textLenghtLimitDesc = "A".repeat(120)
        val textLenghtTestDesc = "A".repeat(120)

        onView(withId(R.id.DescId)).perform(scrollTo(), replaceText(textLenghtTestDesc), closeSoftKeyboard())
        onView(withId(R.id.DescId)).check(matches(withText(textLenghtLimitDesc)))

        onView(withId(R.id.editValue)).perform(scrollTo(), replaceText(textLenghtTestDouble), closeSoftKeyboard())
        onView(withId(R.id.editValue)).check(matches(withText(textLenghtLimitDouble)))

        onView(withId(R.id.ObsId)).perform(scrollTo(), replaceText(textLenghtTest), closeSoftKeyboard())
        onView(withId(R.id.ObsId)).check(matches(withText(textLenghtLimit)))

    }

    @Test
    fun verifySpinner() {
        // Abre o Spinner, seleciona um item pelo texto e verifica se foi selecionado corretamente
        onView(withId(R.id.spinnerIO)).perform(click())

        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Saída"))).perform(click())
        onView(withId(R.id.spinnerIO)).check(matches(withSpinnerText(containsString("Saída"))))
    }

}
