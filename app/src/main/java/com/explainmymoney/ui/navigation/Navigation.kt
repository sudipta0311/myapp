package com.explainmymoney.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.explainmymoney.domain.slm.SlmDownloadState
import com.explainmymoney.ui.screens.analytics.AnalyticsScreen
import com.explainmymoney.ui.screens.chat.ChatScreen
import com.explainmymoney.ui.screens.home.HomeScreen
import com.explainmymoney.ui.screens.investments.InvestmentsScreen
import com.explainmymoney.ui.screens.permissions.PermissionsScreen
import com.explainmymoney.ui.viewmodel.MainViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Investments : Screen("investments", "Invest", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Analytics,
    Screen.Investments,
    Screen.Chat,
    Screen.Settings
)

@Composable
fun MainNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    val transactions by viewModel.transactions.collectAsState()
    val investmentTransactions by viewModel.investmentTransactions.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val totalSpent by viewModel.totalSpentThisMonth.collectAsState()
    val totalIncome by viewModel.totalIncomeThisMonth.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val slmDownloadState by viewModel.slmDownloadState.collectAsState()
    val slmDownloadProgress by viewModel.slmDownloadProgress.collectAsState()
    val slmIsReady by viewModel.slmIsReady.collectAsState()
    val isGmailScanning by viewModel.isGmailScanning.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    transactions = transactions,
                    isLoading = isLoading,
                    scanResult = scanResult,
                    currencySymbol = viewModel.getCurrencySymbol(),
                    onScanSms = { context, hasPermission -> 
                        viewModel.scanSmsMessages(context, hasPermission) 
                    },
                    onImportFile = { uri -> viewModel.parseStatementFile(uri) },
                    onDeleteTransaction = { id -> viewModel.deleteTransaction(id) },
                    onClearScanResult = { viewModel.clearScanResult() },
                    isGmailConnected = viewModel.isGmailConnected(),
                    isGmailScanning = isGmailScanning,
                    onScanGmail = { viewModel.scanGmailEmails() }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    transactions = transactions,
                    totalSpent = totalSpent,
                    totalIncome = totalIncome,
                    categoryBreakdown = categoryBreakdown,
                    currencySymbol = viewModel.getCurrencySymbol(),
                    onRefresh = { viewModel.loadAnalytics() }
                )
            }
            composable(Screen.Investments.route) {
                InvestmentsScreen(
                    investmentTransactions = investmentTransactions,
                    currencySymbol = viewModel.getCurrencySymbol()
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen(
                    transactions = transactions,
                    currencySymbol = viewModel.getCurrencySymbol(),
                    isSlmEnabled = viewModel.isSlmEnabled(),
                    onSlmQuery = { query -> viewModel.generateSlmResponse(query) }
                )
            }
            composable(Screen.Settings.route) {
                PermissionsScreen(
                    userSettings = userSettings,
                    onLogin = { 
                        viewModel.login("Demo User", "demo@example.com", null) 
                    },
                    onLogout = { viewModel.logout() },
                    onCountryChange = { country -> viewModel.updateCountry(country) },
                    deviceCapability = viewModel.checkSlmCapability(),
                    slmDownloadState = slmDownloadState,
                    slmDownloadProgress = slmDownloadProgress,
                    slmIsReady = slmIsReady,
                    isSlmModelDownloaded = viewModel.isSlmModelDownloaded(),
                    onToggleSlm = { enabled -> viewModel.toggleSlmEnabled(enabled) },
                    onDownloadSlm = { viewModel.downloadSlmModel() },
                    onDeleteSlm = { viewModel.deleteSlmModel() },
                    isGmailConnected = viewModel.isGmailConnected(),
                    gmailEmail = userSettings?.gmailEmail,
                    onGetGmailSignInIntent = { viewModel.getGmailSignInIntent() },
                    onGmailSignInResult = { account -> viewModel.handleGmailSignInResult(account) },
                    onDisconnectGmail = { viewModel.disconnectGmail() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
