package com.example.controledegastos.di

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AppDispatchers(val io: CoroutineDispatcher, val computation: CoroutineDispatcher) {
    @Inject constructor() : this(Dispatchers.IO, Dispatchers.Default)
}
