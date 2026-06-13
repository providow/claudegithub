package com.gcashagent.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gcashagent.tracker.ui.feature.day.DayCaptureScreen
import com.gcashagent.tracker.ui.feature.numbers.NumbersScreen
import com.gcashagent.tracker.ui.feature.report.ReportScreen
import com.gcashagent.tracker.ui.feature.transactions.TransactionEntryScreen
import com.gcashagent.tracker.ui.feature.transactions.TransactionsScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.NUMBERS) {

        composable(Routes.NUMBERS) {
            NumbersScreen(
                onOpenNumber = { numberId -> nav.navigate(Routes.day(numberId)) },
                onOpenCombinedReport = { nav.navigate(Routes.report(Routes.ALL_NUMBERS)) }
            )
        }

        composable(
            route = Routes.DAY,
            arguments = listOf(navArgument(Routes.ARG_NUMBER_ID) { type = NavType.LongType })
        ) { entry ->
            val numberId = entry.arguments?.getLong(Routes.ARG_NUMBER_ID) ?: return@composable
            DayCaptureScreen(
                numberId = numberId,
                onBack = { nav.popBackStack() },
                onOpenReport = { nav.navigate(Routes.report(numberId)) }
            )
        }

        composable(
            route = Routes.TRANSACTIONS,
            arguments = listOf(navArgument(Routes.ARG_NUMBER_ID) { type = NavType.LongType })
        ) { entry ->
            val numberId = entry.arguments?.getLong(Routes.ARG_NUMBER_ID) ?: return@composable
            TransactionsScreen(
                numberId = numberId,
                onBack = { nav.popBackStack() },
                onAddTransaction = { nav.navigate(Routes.transactionEntry(numberId)) },
                onEditTransaction = { txId -> nav.navigate(Routes.transactionEntry(numberId, txId)) },
                onOpenReport = { nav.navigate(Routes.report(numberId)) }
            )
        }

        composable(
            route = Routes.TRANSACTION_ENTRY,
            arguments = listOf(
                navArgument(Routes.ARG_NUMBER_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_TRANSACTION_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { entry ->
            val numberId = entry.arguments?.getLong(Routes.ARG_NUMBER_ID) ?: return@composable
            val txId = entry.arguments?.getLong(Routes.ARG_TRANSACTION_ID) ?: 0L
            TransactionEntryScreen(
                numberId = numberId,
                transactionId = txId.takeIf { it != 0L },
                onDone = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument(Routes.ARG_NUMBER_ID) { type = NavType.LongType })
        ) { entry ->
            val numberId = entry.arguments?.getLong(Routes.ARG_NUMBER_ID) ?: Routes.ALL_NUMBERS
            ReportScreen(
                numberId = numberId,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
