package com.phoneintel.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phoneintel.app.ui.battery.BatteryScreen
import com.phoneintel.app.ui.bluetooth.BluetoothScreen
import com.phoneintel.app.ui.dashboard.DashboardScreen
import com.phoneintel.app.ui.focus.FocusScreen
import com.phoneintel.app.ui.health.PhoneHealthScreen
import com.phoneintel.app.ui.network.NetworkScreen
import com.phoneintel.app.ui.notifications.NotificationsScreen
import com.phoneintel.app.ui.recap.RecapScreen

sealed class Screen(val route: String) {
    object Dashboard   : Screen("dashboard")
    object PhoneHealth : Screen("phone_health")
    object Focus       : Screen("focus")
    object Notifications : Screen("notifications")
    object Network     : Screen("network")
    object Bluetooth   : Screen("bluetooth")
    object Battery     : Screen("battery")
    object Recap       : Screen("recap")
}

@Composable
fun PhoneIntelNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route)    { DashboardScreen(navController) }
        composable(Screen.PhoneHealth.route)  { PhoneHealthScreen() }
        composable(Screen.Focus.route)        { FocusScreen() }
        composable(Screen.Notifications.route){ NotificationsScreen() }
        composable(Screen.Network.route)      { NetworkScreen() }
        composable(Screen.Bluetooth.route)    { BluetoothScreen() }
        composable(Screen.Battery.route)      { BatteryScreen() }
        composable(Screen.Recap.route)        { RecapScreen() }
    }
}
