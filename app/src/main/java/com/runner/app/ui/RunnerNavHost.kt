package com.runner.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runner.app.data.RunnerDatabase
import com.runner.app.ui.screens.AlarmsScreen
import com.runner.app.ui.screens.CreateAlarmScreen
import com.runner.app.ui.screens.EditScriptScreen
import com.runner.app.ui.screens.LogDetailScreen
import com.runner.app.ui.screens.LogsScreen
import com.runner.app.ui.screens.ScriptsScreen
import com.runner.app.ui.screens.UploadScriptScreen
import com.runner.app.ui.viewmodel.RunnerViewModel
import com.runner.app.ui.viewmodel.RunnerViewModelFactory

sealed class Screen(val route: String, val label: String) {
    data object Alarms : Screen("alarms", "Alarmas")
    data object Scripts : Screen("scripts", "Scripts")
    data object Logs : Screen("logs", "Historial")
    data object CreateAlarm : Screen("create_alarm", "Nueva alarma")
    data object UploadScript : Screen("upload_script", "Subir script")
    data object EditScript : Screen("edit_script/{scriptId}", "Editar script") {
        fun createRoute(scriptId: Long) = "edit_script/$scriptId"
    }
    data object LogDetail : Screen("log_detail/{logId}", "Detalle log") {
        fun createRoute(logId: Long) = "log_detail/$logId"
    }
}

@Composable
fun RunnerNavHost(database: RunnerDatabase) {
    val navController = rememberNavController()
    val factory = RunnerViewModelFactory(database)
    val viewModel: RunnerViewModel = viewModel(factory = factory)

    val bottomItems = listOf(Screen.Alarms, Screen.Scripts, Screen.Logs)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    when (screen) {
                                        Screen.Alarms -> Icons.Default.Alarm
                                        Screen.Scripts -> Icons.Default.Code
                                        else -> Icons.Default.History
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Alarms.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Alarms.route) {
                AlarmsScreen(
                    viewModel = viewModel,
                    onCreateAlarm = { navController.navigate(Screen.CreateAlarm.route) }
                )
            }
            composable(Screen.Scripts.route) {
                ScriptsScreen(
                    viewModel = viewModel,
                    onUpload = { navController.navigate(Screen.UploadScript.route) },
                    onEdit = { id -> navController.navigate(Screen.EditScript.createRoute(id)) }
                )
            }
            composable(Screen.Logs.route) {
                LogsScreen(
                    viewModel = viewModel,
                    onOpenLog = { id -> navController.navigate(Screen.LogDetail.createRoute(id)) }
                )
            }
            composable(
                route = Screen.LogDetail.route,
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) { backStackEntry ->
                val logId = backStackEntry.arguments?.getLong("logId") ?: 0L
                LogDetailScreen(
                    viewModel = viewModel,
                    logId = logId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CreateAlarm.route) {
                CreateAlarmScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Screen.UploadScript.route) {
                UploadScriptScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditScript.route,
                arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
            ) { backStackEntry ->
                val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: 0L
                EditScriptScreen(
                    viewModel = viewModel,
                    scriptId = scriptId,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
