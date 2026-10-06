package com.example.controledegastos.features

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.controledegastos.R
import com.example.controledegastos.data.local.dao.ItemsDao
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.ui.adapter.MyAdapter
import com.example.controledegastos.ui.features.home.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var itemsDao: ItemsDao

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.clearAllTables()
    }

    @Test
    fun testButtonAddItemActivity(){

        onView(withId(R.id.floatingActionAddItem)).perform(click())
        onView(withId(R.id.DescId)).check(matches(isDisplayed()))
    }

    @Test
    fun testRecyclerViewDisplaysItems() {
        //Teste recyclerView com itens no banco de dados

        createDatabaseItems()

        onView(withId(R.id.recyclerView))
            .perform(scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText("descrição"))))
            .check(matches(hasDescendant(withText("descrição"))))

        onView(withId(R.id.recyclerView))
            .perform(scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText("observação 2"))))
            .check(matches(hasDescendant(withText("observação 2"))))

        onView(withId(R.id.recyclerView))
            .perform(scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText("Saúde"))))
            .check(matches(hasDescendant(withText("Saúde"))))
    }

    @Test
    fun testRecyclerViewClick(){
        //Teste para clicar no ícone de editar de um item do RecyclerView

        createDatabaseItems()

        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions
                .actionOnItemAtPosition<MyAdapter.Mvh>(2, clickWithId(R.id.editIcon)))

        onView(withText("Deseja editar este item?"))
            .check(matches(isDisplayed()))

        onView(withText("Sim")).perform(click())

        Thread.sleep(2000L)

        onView(withId(R.id.textViewFlowEdit)).check(matches(isDisplayed()))
    }


    fun clickWithId(id: Int): ViewAction {
        //Clicar em um ícone dentro de um dos itens do RecyclerView

        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed())
            }

            override fun getDescription(): String {
                return "Clica em um filho específico de um item do RecyclerView"
            }

            override fun perform(uiController: UiController?, view: View) {
                val childView = view.findViewById<View>(id)
                childView?.performClick()
            }
        }
    }

    fun createDatabaseItems(){
        //criação de itens no banco de dados para teste

        val items = listOf(
            Items(1, "descrição", "observação", "Entrada",
                "Pagamento à vista", 15_000, 1_739_145_600_000, 202502, "Transporte"),

            Items(2, "descrição 2", "observação 2", "Saída",
                "Pagamento à vista", -30_000, 1_740_009_600_000, 202502, "Outros"),

            Items(3, "descrição 3", "observação 3", "Entrada",
                "Pagamento à vista", 2_500, 1_742_083_200_000, 202503, "Saúde")
        )

        runBlocking {
            itemsDao.insertAllItems(items)
        }

    }


}
