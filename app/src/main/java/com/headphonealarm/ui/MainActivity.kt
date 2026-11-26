package com.headphonealarm.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.headphonealarm.alarmApp
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.ui.edit.AlarmEditScreen
import com.headphonealarm.ui.settings.AppSettingsScreen
import com.headphonealarm.ui.settings.ColorPaletteScreen
import com.headphonealarm.ui.settings.KeepAliveSettingsScreen
import com.headphonealarm.ui.theme.AppBackground
import com.headphonealarm.ui.theme.HeadphoneAlarmTheme
import java.io.IOException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by alarmApp.settingsRepository.appearance.collectAsStateWithLifecycle(AppearanceSettings())
            HeadphoneAlarmTheme(appearance = appearance) {
                AppBackground {
                    AlarmNavHost(appearance, ::updateAppearance)
                    MiuixPopupHost()
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
    const val APPEARANCE = "appearance"
    const val KEEP_ALIVE = "keepAlive"
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

    var showSettings by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / 4 } },
        popEnterTransition = { slideInHorizontally { -it / 4 } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        composable(Routes.LIST) {
            BackHandler(enabled = showSettings) { showSettings = false }
            AlarmHome(
                appearance = currentAppearance,
                showSettings = showSettings,
                onTabChanged = { showSettings = it },
                onAddAlarm = {
                    navController.navigate(Routes.edit(Routes.NEW_ID)) { launchSingleTop = true }
                },
                onEditAlarm = { id ->
                    navController.navigate(Routes.edit(id)) { launchSingleTop = true }
                },
                settings = {
                    AppSettingsScreen(
                        appearance = currentAppearance,
                        update = updateAppearance,
                        keepAlive = app.shizukuKeepAlive,
                        onPalette = { navController.navigate(Routes.APPEARANCE) { launchSingleTop = true } },
                        onKeepAlive = { navController.navigate(Routes.KEEP_ALIVE) { launchSingleTop = true } }
                    )
                }
            )
        }
        composable(Routes.KEEP_ALIVE) {
            val onBack = dropUnlessResumed { navController.popBackStack() }
            SubpageNavigation(currentAppearance, onBack) {
                KeepAliveSettingsScreen(keepAlive = app.shizukuKeepAlive, onBack = onBack)
            }
        }
        composable(Routes.APPEARANCE) {
            val onBack = dropUnlessResumed { navController.popBackStack() }
            SubpageNavigation(currentAppearance, onBack) {
                ColorPaletteScreen(appearance = currentAppearance, update = updateAppearance, onBack = onBack)
            }
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
            val onBack = dropUnlessResumed { navController.popBackStack() }
            SubpageNavigation(currentAppearance, onBack) {
                AlarmEditScreen(alarmId = alarmId, onBack = onBack)
            }
        }
    }
}
