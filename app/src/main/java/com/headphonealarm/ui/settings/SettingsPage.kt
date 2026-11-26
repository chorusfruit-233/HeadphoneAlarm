package com.headphonealarm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.BlurredBarBackground
import com.headphonealarm.ui.LocalHomeBottomPadding
import com.headphonealarm.ui.recordBackdrop
import com.headphonealarm.ui.rememberHomeBackdrop
import com.headphonealarm.ui.theme.LocalEnableBlur
import com.headphonealarm.ui.theme.LocalInterfaceStyle
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsPage(
    title: String,
    onBack: (() -> Unit)? = null,
    palette: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    val bottom = LocalHomeBottomPadding.current + 12.dp
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        val scroll = MiuixScrollBehavior()
        val backdrop = rememberHomeBackdrop(LocalEnableBlur.current)
        val colors = MiuixTheme.colorScheme
        val direction = LocalLayoutDirection.current
        MiuixScaffold(
            popupHost = {},
            topBar = {
                Box {
                    if (backdrop != null) {
                        BlurredBarBackground(backdrop, colors.background, Modifier.matchParentSize())
                    }
                    MiuixTopAppBar(
                        title = title,
                        color = if (backdrop == null) colors.background else Color.Transparent,
                        scrollBehavior = scroll,
                        navigationIcon = {
                            if (onBack != null) {
                                MiuixIconButton(onClick = onBack, modifier = Modifier.padding(start = 16.dp)) {
                                    Icon(
                                        imageVector = MiuixIcons.Useful.Back,
                                        contentDescription = "返回",
                                        tint = colors.onBackground,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = if (direction == LayoutDirection.Rtl) -1f else 1f
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().recordBackdrop(backdrop)
                    .scrollEndHaptic().overScrollVertical().nestedScroll(scroll.nestedScrollConnection),
                overscrollEffect = null,
                contentPadding = PaddingValues(
                    start = insets.calculateStartPadding(direction) + 12.dp,
                    end = insets.calculateEndPadding(direction) + 12.dp,
                    top = insets.calculateTopPadding() + if (palette) 0.dp else 12.dp,
                    bottom = insets.calculateBottomPadding() + bottom
                ),
                verticalArrangement = Arrangement.spacedBy(if (palette) 0.dp else 12.dp),
                content = content
            )
        }
    } else {
        val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val colors = MaterialTheme.colorScheme
        Scaffold(
            containerColor = colors.surfaceContainer,
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surfaceContainer, scrolledContainerColor = colors.surfaceContainer
                    ),
                    scrollBehavior = scroll,
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(horizontal = 10.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = colors.surfaceContainerHighest,
                                    contentColor = colors.onSurfaceVariant
                                )
                            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
                        }
                    }
                )
            }
        ) { insets ->
            val direction = LocalLayoutDirection.current
            val sides = if (palette) 0.dp else 16.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = insets.calculateTopPadding())
                    .consumeWindowInsets(insets).nestedScroll(scroll.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = insets.calculateStartPadding(direction) + sides,
                    end = insets.calculateEndPadding(direction) + sides,
                    bottom = insets.calculateBottomPadding() + bottom
                ),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                content = content
            )
        }
    }
}
