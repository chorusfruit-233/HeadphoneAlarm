package com.headphonealarm.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.headphonealarm.alarmApp
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.ui.edit.AlarmEditScreen
import com.headphonealarm.ui.list.AlarmListScreen
import com.headphonealarm.ui.settings.AppSettingsScreen
import com.headphonealarm.ui.settings.AppearanceScreen
import com.headphonealarm.ui.theme.AppBackground
import com.headphonealarm.ui.theme.HeadphoneAlarmTheme
import java.io.IOException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by alarmApp.settingsRepository.appearance.collectAsStateWithLifecycle(AppearanceSettings())
            HeadphoneAlarmTheme(appearance = appearance) {
                AppBackground {
                    AlarmNavHost(appearance, ::updateAppearance)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        alarmApp.shizukuKeepAlive.setUiVisible(true)
    }

    override fun onPause() {
        alarmApp.shizukuKeepAlive.setUiVisible(false)
        super.onPause()
    }

    private fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        lifecycleScope.launch {
            try {
                alarmApp.settingsRepository.updateAppearance(update)
            } catch (_: IOException) {
                Toast.makeText(this@MainActivity, "无法保存外观设置，请重试。", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private object Routes {
    const val LIST = "list"
    const val EDIT = "edit"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val ARG_ID = "alarmId"
    const val NEW_ID = -1L
    fun edit(id: Long) = "$EDIT?$ARG_ID=$id"
}

@Composable
private fun AlarmNavHost(
    appearance: AppearanceSettings,
    updateAppearance: ((AppearanceSettings) -> AppearanceSettings) -> Unit
) {
    val app = LocalContext.current.alarmApp
    val currentAppearance by rememberUpdatedState(appearance)
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            AlarmListScreen(
                // launchSingleTop 避免连点造成多个编辑页堆叠
                onAddAlarm = {
                    navController.navigate(Routes.edit(Routes.NEW_ID)) { launchSingleTop = true }
                },
                onEditAlarm = { id ->
                    navController.navigate(Routes.edit(id)) { launchSingleTop = true }
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.SETTINGS) {
            AppSettingsScreen(
                appearance = currentAppearance,
                keepAlive = app.shizukuKeepAlive,
                onAppearance = { navController.navigate(Routes.APPEARANCE) { launchSingleTop = true } },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.APPEARANCE) {
            AppearanceScreen(
                appearance = currentAppearance,
                update = updateAppearance,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Routes.EDIT}?${Routes.ARG_ID}={${Routes.ARG_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_ID
                }
            )
        ) { entry ->
            val alarmId = entry.arguments?.getLong(Routes.ARG_ID) ?: Routes.NEW_ID
            AlarmEditScreen(
                alarmId = alarmId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
