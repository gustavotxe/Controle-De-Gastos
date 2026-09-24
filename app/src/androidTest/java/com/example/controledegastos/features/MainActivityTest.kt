package com.example.controledegastos.features

import android.view.View
import android.widget.DatePicker
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.controledegastos.R
import com.example.controledegastos.data.local.dao.ItemsDao
import com.example.controledegastos.database.HiltDatabaseTest
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.ui.features.home.MainActivity
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import javax.inject.Inject
import androidx.test.espresso.matcher.RootMatchers.isDialog
import kotlin.time.Duration.Companion.milliseconds

@HiltAndroidTest
class MainActivityTest : HiltDatabaseTest() {
    @Inject lateinit var itemsDao: ItemsDao
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val year = Calendar.getInstance().get(Calendar.YEAR)

    @Before fun setup() {
        runBlocking { itemsDao.insertItem(Items(1, "Registro de teste", "Observação", "Entrada",
            "Pagamento à vista", 15_000, Calendar.getInstance().timeInMillis, year * 100 + 9, "Salário")) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForItems(1)
    }

    @After fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test fun recyclerViewDisplaysPersistedAmount() {
        onView(withId(R.id.recyclerView)).check(matches(hasDescendant(withText("Registro de teste"))))
        onView(withId(R.id.textValue)).check(matches(withText(com.example.controledegastos.ui.model.Money.format(15_000))))
    }

    @Test fun editAndDeleteTransaction() {
        clickItemChild(R.id.editIcon)
        onView(withId(R.id.DescId)).check(matches(withText("Registro de teste")))
            .perform(replaceText("Registro editado"), closeSoftKeyboard())
        onView(withId(R.id.editValue)).perform(scrollTo(), replaceText("7.000,00"), closeSoftKeyboard())
        onView(withId(R.id.saveNote)).perform(scrollTo(), click())
        val edited = awaitItems { it.singleOrNull()?.description == "Registro editado" }.single()
        assertEquals(700_000L, edited.amountCents)
        waitForItems(1, "Registro editado")
        clickItemChild(R.id.deleteIcon)
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        awaitItems { it.isEmpty() }
        waitForItems(0)
    }

    @Test fun createTransactionWithBrazilianAmount() {
        onView(withId(R.id.floatingActionAddItem)).perform(click())
        onView(withId(R.id.DescId)).perform(replaceText("Novo registro"), closeSoftKeyboard())
        onView(withId(R.id.editValue)).perform(scrollTo(), replaceText("7.000"), closeSoftKeyboard())
        onView(withId(R.id.textDateSelected)).perform(scrollTo(), click())
        onView(withClassName(equalTo(DatePicker::class.java.name))).perform(PickerActions.setDate(year, 9, 16))
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.saveNote)).perform(scrollTo(), click())
        val created = awaitItems { it.size == 2 }.single { it.description == "Novo registro" }
        assertEquals(700_000L, created.amountCents)
        assertEquals(year * 100 + 9, created.yearMonth)
        waitForItems(2)
    }

    private fun awaitItems(condition: (List<Items>) -> Boolean): List<Items> = runBlocking {
        withTimeout(5000.milliseconds) { itemsDao.getAllItems().first(condition) }
    }

    private fun waitForItems(count: Int, description: String? = null) {
        onView(withId(R.id.recyclerView)).perform(object : ViewAction {
            override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
            override fun getDescription() = "Wait for the asynchronous list update"
            override fun perform(controller: UiController, view: View) {
                val list = view as RecyclerView
                val deadline = android.os.SystemClock.uptimeMillis() + 5_000
                fun ready() = list.adapter?.itemCount == count &&
                    (description == null || hasDescendant(withText(description)).matches(list))
                while (!ready() && android.os.SystemClock.uptimeMillis() < deadline) {
                    controller.loopMainThreadForAtLeast(16)
                }
                org.junit.Assert.assertTrue("List did not update", ready())
            }
        })
    }

    private fun clickItemChild(id: Int) {
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
            object : ViewAction {
                override fun getConstraints() = isDisplayed()
                override fun getDescription() = "Click a transaction action"
                override fun perform(controller: UiController, view: View) {
                    click().perform(controller, view.findViewById(id))
                    controller.loopMainThreadUntilIdle()
                }
            }))
    }

}
