package com.headphonealarm.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.headphonealarm.data.AppearanceSettings
import kotlin.math.abs

@Composable
internal fun SubpageNavigation(
    appearance: AppearanceSettings,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    // NavHost animates the previous page in step with Android's predictive back gesture.
    // A normal callback consumes the finished gesture when the animation is switched off.
    BackHandler(enabled = !appearance.enablePredictiveBack, onBack = onBack)
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val edgeSwipe = if (!appearance.enableSwipeDismiss) Modifier else Modifier.pointerInput(rtl, dispatcher) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val edge = if (rtl) size.width - down.position.x else down.position.x
            if (edge > 32.dp.toPx()) return@awaitEachGesture

            val direction = if (rtl) -1f else 1f
            var dragging = false
            do {
                val event = awaitPointerEvent()
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                val distance = pointer.position - down.position
                val forward = direction * distance.x
                if (event.changes.size > 1 || pointer.isConsumed) break
                if (!dragging) {
                    if (abs(distance.y) > viewConfiguration.touchSlop || forward < -viewConfiguration.touchSlop) break
                    dragging = forward > viewConfiguration.touchSlop
                }
                if (dragging) {
                    pointer.consume()
                    if (!pointer.pressed && forward >= minOf(80.dp.toPx(), size.width * 0.25f)) {
                        // Honor the editor's busy guard and any other active back handler.
                        dispatcher?.onBackPressed()
                    }
                }
            } while (pointer.pressed)
        }
    }
    Box(Modifier.fillMaxSize().then(edgeSwipe)) { content() }
}
