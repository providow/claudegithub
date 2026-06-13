package com.gcashagent.tracker.di

import android.content.Context
import com.gcashagent.tracker.core.data.local.AppDatabase
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.data.repository.GCashRepositoryImpl
import com.gcashagent.tracker.core.util.ImageStore
import com.gcashagent.tracker.core.util.ReportExporter

/**
 * Manual dependency container for the MVP. Keeps the app dependency-light while
 * still wiring everything through interfaces, so Hilt (or a different storage
 * backend) can be introduced later without touching the feature layer.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.getInstance(context)

    val repository: GCashRepository = GCashRepositoryImpl(
        numberDao = database.gcashNumberDao(),
        transactionDao = database.transactionDao()
    )

    val imageStore: ImageStore = ImageStore(context)

    val reportExporter: ReportExporter = ReportExporter(context)
}
