package com.headphonealarm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val ScaleTitle = "界面缩放"
private const val ScaleSummary = "调整全局显示比例"
private val scaleRange = 0.8f..1.1f
private val Float.percent get() = (this * 100).roundToInt()

@Composable
internal fun MaterialScaleCard(value: Float, onCommit: (Float) -> Unit) {
    var scale by remember(value) { mutableFloatStateOf(value) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = colors.surfaceBright,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AspectRatio, null, tint = colors.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(ScaleTitle, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(ScaleSummary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Text("${scale.percent}%", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Slider(
                value = scale,
                valueRange = scaleRange,
                onValueChange = { scale = it },
                onValueChangeFinished = { onCommit(scale.percent / 100f) },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = ScaleTitle
                    stateDescription = "${scale.percent}%"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixScaleRow(value: Float, onCommit: (Float) -> Unit) {
    var scale by remember(value) { mutableFloatStateOf(value) }
    val dialog = rememberSaveable { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    Column {
        SuperArrow(
            title = ScaleTitle,
            summary = ScaleSummary,
            leftAction = { MiuixRowIcon(Icons.Rounded.AspectRatio) },
            rightText = "${scale.percent}%",
            onClick = { dialog.value = true },
            holdDownState = dialog.value
        )
        // The Material slider owns drag completion, keyboard input and the accessibility range.
        // A filled track and decade markers reproduce the Miuix control without changing density mid-drag.
        Slider(
            value = scale,
            valueRange = scaleRange,
            steps = 29,
            onValueChange = { scale = it },
            onValueChangeFinished = { onCommit(scale.percent / 100f) },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).semantics {
                contentDescription = ScaleTitle
                stateDescription = "${scale.percent}%"
            },
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = {
                Box(
                    Modifier.fillMaxWidth().height(30.dp).clip(CircleShape).background(colors.tertiaryContainerVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val fill = ((scale - scaleRange.start) / (scaleRange.endInclusive - scaleRange.start)).coerceIn(0f, 1f)
                    Spacer(Modifier.fillMaxHeight().fillMaxWidth(fill).background(colors.primary))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (mark in 80..110 step 10) {
                            val color = if (mark <= scale.percent) colors.onPrimary else colors.primary
                            Spacer(Modifier.size(3.dp).background(color, CircleShape))
                        }
                    }
                }
            }
        )
    }
    if (dialog.value) {
        var entry by rememberSaveable { mutableStateOf(value.percent.toString()) }
        val confirm = {
            val percent = entry.toIntOrNull()?.coerceIn(80, 110) ?: value.percent
            dialog.value = false
            onCommit(percent / 100f)
        }
        SuperDialog(show = dialog, title = ScaleTitle, summary = "80% - 110%", onDismissRequest = { dialog.value = false }) {
            MiuixTextField(
                value = entry,
                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) entry = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { MiuixText("%", Modifier.padding(horizontal = 16.dp), color = colors.onSurfaceVariantActions) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() })
            )
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MiuixButton("取消", { dialog.value = false }, Modifier.weight(1f))
                MiuixButton("确定", confirm, Modifier.weight(1f), colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}
