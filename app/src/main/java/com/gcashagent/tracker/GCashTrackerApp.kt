package com.gcashagent.tracker

import android.app.Application
import com.gcashagent.tracker.di.AppContainer

class GCashTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
