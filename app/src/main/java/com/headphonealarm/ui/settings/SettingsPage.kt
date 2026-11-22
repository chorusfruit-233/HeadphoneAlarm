package com.headphonealarm.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.components.GlassCard
import com.headphonealarm.ui.components.SectionTitle
import com.headphonealarm.ui.theme.LocalInterfaceStyle
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPage(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val miuix = LocalInterfaceStyle.current == InterfaceStyle.MIUIX
    val materialScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val miuixScroll = MiuixScrollBehavior()
    val colors = MaterialTheme.colorScheme
    val back: @Composable () -> Unit = {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = colors.onSurface)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(
            if (miuix) miuixScroll.nestedScrollConnection else materialScroll.nestedScrollConnection
        ),
        containerColor = Color.Transparent,
        topBar = {
            if (miuix) {
                MiuixTopAppBar(
                    title = title,
                    color = colors.background,
                    navigationIcon = back,
                    scrollBehavior = miuixScroll
                )
            } else {
                LargeTopAppBar(
                    title = { Text(title) },
                    navigationIcon = back,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        scrolledContainerColor = colors.surfaceContainer
                    ),
                    scrollBehavior = materialScroll
                )
            }
        }
    ) { insets ->
        Box(
            modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 620.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionTitle(title)
        GlassCard(content = content)
    }
}

@Composable
internal fun SettingsLink(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val leading: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        SuperArrow(title = title, summary = summary, leftAction = leading, onClick = onClick)
    } else {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(summary) },
            leadingContent = leading,
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
        )
    }
}
