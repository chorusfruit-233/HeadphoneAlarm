package com.headphonealarm.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.list.AlarmListScreen
import com.headphonealarm.ui.list.AlarmListViewModel
import com.headphonealarm.ui.list.AlarmLoadState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalHomeBottomPadding = staticCompositionLocalOf { 0.dp }

private enum class HomeTab(val label: String, val filled: ImageVector, val outlined: ImageVector, val rounded: ImageVector) {
    ALARMS("闹钟", Icons.Filled.Alarm, Icons.Outlined.Alarm, Icons.Rounded.Alarm),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings, Icons.Rounded.Settings)
}

@Composable
internal fun useHomeRail(floating: Boolean): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val deviceDensity = LocalResources.current.displayMetrics.density
    val widthDp = size.width / deviceDensity
    return !floating && (widthDp >= 840f || widthDp >= 600f && size.height < size.width * 1.2f)
}

@Composable
internal fun AlarmHome(
    appearance: AppearanceSettings,
    showSettings: Boolean,
    onTabChanged: (Boolean) -> Unit,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    settings: @Composable () -> Unit
) {
    val initialTab = if (showSettings) HomeTab.SETTINGS else HomeTab.ALARMS
    val pager = rememberPagerState(initialPage = initialTab.ordinal) { HomeTab.entries.size }
    val scope = rememberCoroutineScope()
    val onSettled by rememberUpdatedState(onTabChanged)
    val miuix = appearance.interfaceStyle == InterfaceStyle.MIUIX
    val floating = miuix && appearance.enableFloatingBottomBar
    val rail = useHomeRail(floating)
    val backdrop = rememberHomeBackdrop(miuix && !rail && if (floating) appearance.enableFloatingBottomBarBlur else appearance.enableBlur)

    LaunchedEffect(showSettings) {
        if (initialTab.ordinal != pager.settledPage) pager.animateScrollToPage(initialTab.ordinal)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { onSettled(it == HomeTab.SETTINGS.ordinal) }
    }
    val select: (Int) -> Unit = { page ->
        scope.launch { if (rail) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
    }

    val listViewModel: AlarmListViewModel = viewModel()
    val alarms by listViewModel.alarmState.collectAsStateWithLifecycle()
    val count = if (appearance.enableNavigationBadge) {
        (alarms as? AlarmLoadState.Ready)?.alarms?.count { it.enabled } ?: 0
    } else 0

    val density = LocalDensity.current
    val systemBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var barHeight by remember { mutableStateOf(64.dp + systemBottom) }
    val railInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
    Row(Modifier.fillMaxSize()) {
        if (rail) HomeRail(miuix, pager.currentPage, count, select)
        Box(Modifier.weight(1f).then(if (rail) Modifier.consumeWindowInsets(railInsets) else Modifier)) {
            CompositionLocalProvider(LocalHomeBottomPadding provides if (rail) 0.dp else (barHeight - systemBottom).coerceAtLeast(0.dp)) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().recordBackdrop(backdrop)
                        .homePagerGestures(appearance.pagerInterceptionMode, pager),
                    userScrollEnabled = appearance.pagerInterceptionMode != 1
                ) { page ->
                    if (page == HomeTab.ALARMS.ordinal) {
                        AlarmListScreen(onAddAlarm, onEditAlarm, pager.settledPage == page, listViewModel)
                    } else {
                        settings()
                    }
                }
            }
            if (!rail) {
                Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = with(density) { it.height.toDp() } }) {
                    if (miuix) {
                        MiuixHomeBar(floating, pager.currentPage, count, select, backdrop)
                    } else {
                        ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            HomeTab.entries.forEach { tab ->
                                val selected = tab.ordinal == pager.currentPage
                                ShortNavigationBarItem(
                                    selected = selected, onClick = { select(tab.ordinal) },
                                    icon = { TabIcon(tab, selected, count) }, label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixHomeBar(floating: Boolean, page: Int, count: Int, onSelect: (Int) -> Unit, backdrop: HomeBackdrop?) {
    val colors = MiuixTheme.colorScheme
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val shape = if (floating) {
        Modifier.windowInsetsPadding(insets).padding(bottom = if (bottom == 0.dp) 28.dp else 8.dp)
            .shadow(5.dp, CircleShape).clip(CircleShape)
            .then(if (backdrop != null) Modifier.border(1.dp, colors.outline.copy(alpha = 0.32f), CircleShape) else Modifier)
    } else Modifier.fillMaxWidth()
    Box(shape) {
        BlurredBarBackground(
            backdrop, if (floating) colors.surfaceContainer else colors.background,
            Modifier.matchParentSize(), glass = floating
        )
        val rowModifier = if (floating) Modifier.padding(4.dp) else Modifier.windowInsetsPadding(insets)
        Row(rowModifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            HomeTab.entries.forEach { tab ->
                val selected = page == tab.ordinal
                val cell = if (floating) {
                    Modifier.width(76.dp).height(56.dp).clip(CircleShape)
                        .background(if (selected) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                } else Modifier.weight(1f).height(64.dp)
                val tint = if (floating && selected) colors.primary else colors.onSurfaceContainer.copy(alpha = if (selected) 1f else 0.55f)
                Column(
                    cell.selectable(selected, role = Role.Tab, onClick = { onSelect(tab.ordinal) }),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabIcon(tab, selected, count, miuix = true, tint = tint)
                    Text(tab.label, color = tint, fontSize = if (floating) 11.sp else 12.sp, lineHeight = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun HomeRail(miuix: Boolean, page: Int, count: Int, onSelect: (Int) -> Unit) {
    val state = rememberWideNavigationRailState()
    val expanded = state.targetValue == WideNavigationRailValue.Expanded
    val scope = rememberCoroutineScope()
    val miuixColors = MiuixTheme.colorScheme
    val color = if (miuix) miuixColors.surface else MaterialTheme.colorScheme.surfaceContainer
    val itemColors = if (miuix) {
        WideNavigationRailItemDefaults.colors(
            selectedIconColor = miuixColors.onSurfaceContainer,
            selectedIndicatorColor = miuixColors.surfaceContainerHigh,
            selectedTextColor = miuixColors.onSurfaceContainer,
            unselectedIconColor = miuixColors.onSurfaceContainer,
            unselectedTextColor = miuixColors.onSurfaceContainer
        )
    } else WideNavigationRailItemDefaults.colors()
    WideNavigationRail(
        modifier = Modifier.fillMaxHeight(),
        state = state,
        colors = WideNavigationRailDefaults.colors(containerColor = color),
        header = {
            IconButton(modifier = Modifier.padding(start = 20.dp), onClick = {
                scope.launch { if (expanded) state.collapse() else state.expand() }
            }) {
                Icon(if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = if (expanded) "收起导航栏" else "展开导航栏")
            }
        }
    ) {
        HomeTab.entries.forEach { tab ->
            val selected = page == tab.ordinal
            WideNavigationRailItem(
                railExpanded = expanded, selected = selected, colors = itemColors, onClick = { onSelect(tab.ordinal) },
                icon = { TabIcon(tab, selected, count, miuix) }, label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun TabIcon(
    tab: HomeTab,
    selected: Boolean,
    count: Int,
    miuix: Boolean = false,
    tint: Color = LocalContentColor.current
) {
    val vector = if (miuix) tab.rounded else if (selected) tab.filled else tab.outlined
    val icon: @Composable () -> Unit = { Icon(vector, null, Modifier.size(if (miuix) 26.dp else 24.dp), tint = tint) }
    if (tab == HomeTab.ALARMS && count > 0) {
        BadgedBox(badge = {
            Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Text(count.toString())
            }
        }) { icon() }
    } else icon()
}
