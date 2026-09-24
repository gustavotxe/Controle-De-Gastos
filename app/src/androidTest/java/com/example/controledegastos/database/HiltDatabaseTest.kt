package com.example.controledegastos.database

import com.example.controledegastos.data.local.database.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class HiltDatabaseTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @Inject lateinit var database: AppDatabase

    @Before fun injectDatabase() = hiltRule.inject()

    @After fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }
}
