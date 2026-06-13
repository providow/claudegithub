package com.gcashagent.tracker.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gcashagent.tracker.GCashTrackerApp

/** Convenience accessor for the app's [AppContainer] from any composable. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as GCashTrackerApp).container
