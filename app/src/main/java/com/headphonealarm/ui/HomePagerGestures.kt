package com.headphonealarm.ui

import android.os.SystemClock
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private class VerticalFling : NestedScrollConnection {
    var lastFrame = 0L

    override suspend fun onPreFling(available: Velocity): Velocity {
        lastFrame = if (available.y != 0f) SystemClock.uptimeMillis() else 0L
        return Velocity.Zero
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.SideEffect && available.y != 0f) lastFrame = SystemClock.uptimeMillis()
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        lastFrame = 0L
        return Velocity.Zero
    }
}

@Composable
internal fun Modifier.homePagerGestures(mode: Int, pager: PagerState): Modifier {
    val direction = LocalLayoutDirection.current
    if (mode == 0) return this
    if (mode == 2) {
        val fling = remember { VerticalFling() }
        return nestedScroll(fling).pointerInput(fling) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.uptimeMillis - fling.lastFrame !in 0L..120L || fling.lastFrame == 0L) return@awaitEachGesture
                // Let the child receive the down to stop its vertical fling; absorb this horizontal drag.
                var horizontal = false
                do {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.first()
                    val delta = change.position - down.position
                    if (!horizontal && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) >= abs(delta.x)) break
                    if (abs(delta.x) > viewConfiguration.touchSlop) horizontal = true
                    if (horizontal) change.consume()
                } while (change.pressed)
            }
        }
    }
    return pointerInput(pager, direction) {
        val scrollSign = if (direction == LayoutDirection.Rtl) 1f else -1f
        val minVelocity = 400.dp.toPx()
        coroutineScope {
            val motionScope = this
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val velocity = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                var deltas: Channel<Float>? = null
                var releaseVelocity = 0f
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        velocity.addPosition(change.uptimeMillis, change.position)
                        if (!change.pressed || event.changes.size > 1) break
                        var delta = change.position.x - change.previousPosition.x
                        if (deltas == null) {
                            val total = change.position - down.position
                            if (abs(total.y) > viewConfiguration.touchSlop && abs(total.y) >= abs(total.x)) break
                            if (abs(total.x) <= viewConfiguration.touchSlop) continue
                            delta = total.x - sign(total.x) * viewConfiguration.touchSlop
                            val channel = Channel<Float>(Channel.UNLIMITED)
                            deltas = channel
                            motionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                try {
                                    pager.scroll(MutatePriority.UserInput) {
                                        for (distance in channel) scrollBy(distance)
                                        val position = pager.currentPage + pager.currentPageOffsetFraction
                                        val target = when {
                                            releaseVelocity > minVelocity -> ceil(position).toInt()
                                            releaseVelocity < -minVelocity -> floor(position).toInt()
                                            else -> position.roundToInt()
                                        }.coerceIn(0, pager.pageCount - 1)
                                        val distance = (target - position) * pager.layoutInfo.pageSize
                                        var traveled = 0f
                                        animate(0f, distance, initialVelocity = releaseVelocity,
                                            animationSpec = spring(stiffness = 410f)) { value, _ ->
                                            traveled += scrollBy(value - traveled)
                                        }
                                    }
                                } finally {
                                    channel.cancel()
                                }
                            }
                        }
                        change.consume()
                        deltas.trySend(delta * scrollSign)
                    } while (true)
                    releaseVelocity = velocity.calculateVelocity().x * scrollSign
                } finally {
                    deltas?.close()
                }
            }
        }
    }
}
