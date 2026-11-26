package com.headphonealarm.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal class HomeBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
internal fun rememberHomeBackdrop(enabled: Boolean): HomeBackdrop? {
    if (!enabled || Build.VERSION.SDK_INT < 33 || !LocalView.current.isHardwareAccelerated) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { HomeBackdrop(layer) }
}

internal fun Modifier.recordBackdrop(backdrop: HomeBackdrop?): Modifier {
    if (backdrop == null) return this
    return onGloballyPositioned { backdrop.origin = it.positionInRoot() }.drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }
}

/** Draw only the portion of the scrolling content directly underneath this bar. */
@Composable
internal fun BlurredBarBackground(backdrop: HomeBackdrop?, tint: Color, modifier: Modifier, glass: Boolean = false) {
    if (backdrop == null) {
        Box(modifier.background(tint))
        return
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val refraction = if (glass && Build.VERSION.SDK_INT >= 33) Modifier.glassRefraction() else Modifier
    Box(modifier.clipToBounds().onGloballyPositioned { origin = it.positionInRoot() }) {
        Box(Modifier.matchParentSize().then(refraction).blur(if (glass) 4.dp else 20.dp).drawBehind {
            val offset = backdrop.origin - origin
            translate(offset.x, offset.y) { drawLayer(backdrop.layer) }
        })
        Box(Modifier.matchParentSize().background(tint.copy(alpha = if (glass) 0.45f else 0.82f)))
    }
}

/** Refract the already recorded backdrop at the curved edge of a floating navigation bar. */
@RequiresApi(33)
@Composable
private fun Modifier.glassRefraction(): Modifier {
    val shader = remember { RuntimeShader("""
        uniform shader content;
        uniform float2 bounds;
        uniform float depth;
        half4 main(float2 pos) {
            float2 local = pos - bounds * 0.5;
            float radius = bounds.y * 0.5;
            float stem = max(bounds.x * 0.5 - radius, 0.0);
            float2 delta = local - float2(clamp(local.x, -stem, stem), 0.0);
            float len = length(delta);
            float rim = smoothstep(radius - depth, radius, len);
            float2 normal = delta / max(len, 1.0);
            return content.eval(clamp(pos - normal * depth * rim, float2(0.5), bounds - 0.5));
        }
    """.trimIndent()) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val depth = with(LocalDensity.current) { 9.dp.toPx() }
    val effect = remember(shader, bounds, depth) {
        if (bounds.width == 0 || bounds.height == 0) null else {
            shader.setFloatUniform("bounds", bounds.width.toFloat(), bounds.height.toFloat())
            shader.setFloatUniform("depth", depth)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    }
    return onSizeChanged { bounds = it }.graphicsLayer { renderEffect = effect }
}
