package com.mameen.isomessage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mameen.isomessage.ui.screens.balance.BalanceInquiryScreen
import com.mameen.isomessage.ui.screens.developer.DeveloperToolsScreen
import com.mameen.isomessage.ui.screens.home.HomeScreen
import com.mameen.isomessage.ui.screens.isomessageviewer.IsoMessageViewerScreen
import com.mameen.isomessage.ui.screens.logs.LogsScreen
import com.mameen.isomessage.ui.screens.purchase.PurchaseScreen
import com.mameen.isomessage.ui.screens.receipt.ReceiptScreen
import com.mameen.isomessage.ui.screens.refund.RefundScreen
import com.mameen.isomessage.ui.screens.reversal.ReversalScreen
import com.mameen.isomessage.ui.screens.settlement.SettlementScreen
import com.mameen.isomessage.ui.screens.settings.SettingsScreen
import com.mameen.isomessage.ui.screens.transactiondetails.TransactionDetailsScreen

/**
 * Navigation routes defined as sealed class for type safety.
 * Using a sealed class prevents typos in route strings.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Purchase : Screen("purchase")
    object Refund : Screen("refund")
    object Reversal : Screen("reversal")
    object Settlement : Screen("settlement")
    object BalanceInquiry : Screen("balance_inquiry")
    object IsoMessageViewer : Screen("iso_message_viewer/{transactionId}") {
        fun createRoute(transactionId: String) = "iso_message_viewer/$transactionId"
    }
    object TransactionDetails : Screen("transaction_details/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_details/$transactionId"
    }
    object Logs : Screen("logs")
    object Receipt : Screen("receipt/{transactionId}") {
        fun createRoute(transactionId: String) = "receipt/$transactionId"
    }
    object Settings : Screen("settings")
    object DeveloperTools : Screen("developer_tools")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPurchase = { navController.navigate(Screen.Purchase.route) },
                onNavigateToRefund = { navController.navigate(Screen.Refund.route) },
                onNavigateToReversal = { navController.navigate(Screen.Reversal.route) },
                onNavigateToSettlement = { navController.navigate(Screen.Settlement.route) },
                onNavigateToBalance = { navController.navigate(Screen.BalanceInquiry.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDeveloperTools = { navController.navigate(Screen.DeveloperTools.route) },
                onNavigateToTransaction = { id ->
                    navController.navigate(Screen.TransactionDetails.createRoute(id))
                }
            )
        }

        composable(Screen.Purchase.route) {
            PurchaseScreen(
                onBack = { navController.popBackStack() },
                onTransactionComplete = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id)) {
                        popUpTo(Screen.Purchase.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Refund.route) {
            RefundScreen(
                onBack = { navController.popBackStack() },
                onTransactionComplete = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id)) {
                        popUpTo(Screen.Refund.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Reversal.route) {
            ReversalScreen(
                onBack = { navController.popBackStack() },
                onTransactionComplete = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id)) {
                        popUpTo(Screen.Reversal.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settlement.route) {
            SettlementScreen(
                onBack = { navController.popBackStack() },
                onTransactionComplete = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id)) {
                        popUpTo(Screen.Settlement.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BalanceInquiry.route) {
            BalanceInquiryScreen(
                onBack = { navController.popBackStack() },
                onTransactionComplete = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.IsoMessageViewer.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStack ->
            IsoMessageViewerScreen(
                transactionId = backStack.arguments?.getString("transactionId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TransactionDetails.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStack ->
            TransactionDetailsScreen(
                transactionId = backStack.arguments?.getString("transactionId") ?: "",
                onBack = { navController.popBackStack() },
                onViewIso = { id ->
                    navController.navigate(Screen.IsoMessageViewer.createRoute(id))
                },
                onViewReceipt = { id ->
                    navController.navigate(Screen.Receipt.createRoute(id))
                }
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Receipt.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStack ->
            ReceiptScreen(
                transactionId = backStack.arguments?.getString("transactionId") ?: "",
                onBack = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }},
                onViewDetails = { id ->
                    navController.navigate(Screen.TransactionDetails.createRoute(id))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DeveloperTools.route) {
            DeveloperToolsScreen(onBack = { navController.popBackStack() })
        }
    }
}
