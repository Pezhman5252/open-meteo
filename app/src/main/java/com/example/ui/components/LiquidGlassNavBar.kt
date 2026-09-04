package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Vazirmatn
import com.example.ui.theme.isDark

/**
 * ============================================================================
 *  LIQUID GLASS NAV BAR — native, auto-tiered, iOS-26 "Liquid Glass" style
 * ============================================================================
 *  Implements the same technique used inside the liquid-glass library
 *  (io.github.nadeemiqbal:liquid-glass 0.2.3) with official androidx APIs:
 *
 *  • The content BEHIND the bar is recorded into a [GraphicsLayer]
 *    (see [GlassBackdropSource]) and the glass surface re-draws it through
 *    a RenderEffect chain — blur(24dp) → saturation(1.4x) — clipped to the
 *    capsule shape. That chain is the library's "Full" tier recipe.
 *  • IMPORTANT: the blur is applied as a PERSISTENT RenderEffect on the glass
 *    surface's own layer via `Modifier.graphicsLayer { renderEffect = … }`.
 *    A previous attempt used the set→draw→reset dance on the backdrop layer;
 *    that loses the effect on every display-list REPLAY (RenderNode reads its
 *    properties at replay time), so the backdrop showed through SHARP and
 *    mixed with the tab labels. Keeping the effect as a permanent property of
 *    the glass layer makes the blur survive every replay.
 *  • On top of the blurred body: a sharp specular sheen, hairline border, and
 *    the animated tab slider pill (drawn in a separate, un-blurred layer).
 *  • Auto-tiering (mirrors LiquidGlassQuality.android.kt):
 *      – Full:     Android 12 (API 31)+ on non-low-RAM devices.
 *      – Fallback: older / low-RAM devices → no GraphicsLayer recording,
 *                  no blur; nearly-opaque frosted veil instead. Zero GPU
 *                  shader cost, zero extra allocation.
 *  • All colors derive from MaterialTheme → automatic dark/light support.
 *  • RTL-safe: Modifier.offset mirrors automatically in RTL.
 * ============================================================================
 */

enum class GlassTier { Full, Fallback }

/** Resolve the render tier exactly like the library's platform quality probe. */
fun resolveGlassTier(isLowRamDevice: Boolean): GlassTier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isLowRamDevice) {
        GlassTier.Full
    } else {
        GlassTier.Fallback
    }

/** Shared state linking the backdrop source to the glass surface(s). */
class LiquidGlassState internal constructor(val tier: GlassTier) {
    internal var backdrop: androidx.compose.ui.graphics.layer.GraphicsLayer? = null
    internal var backdropOriginX: Float = 0f
    internal var backdropOriginY: Float = 0f

    /** Number of glass surfaces currently composed. While it is 0 (bar scrolled
     *  away / removed) the full-screen backdrop recording is SKIPPED so scrolling
     *  pays zero glass cost until the bar comes back. */
    internal var consumerCount: Int = 0
}

/** Probe the tier once per device; cheap and stable for the app lifetime. */
@Composable
fun rememberLiquidGlassState(): LiquidGlassState {
    val context = LocalContext.current
    val tier = remember(context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        resolveGlassTier(isLowRamDevice = am?.isLowRamDevice == true)
    }
    return remember(tier) { LiquidGlassState(tier) }
}

/**
 * Wrap the content that sits BEHIND the glass (the app Scaffold content).
 * On the Full tier the content is recorded into a GraphicsLayer at draw time
 * (recording is per-draw, no per-frame allocation) so any glass surface
 * sharing this state can re-draw the exact pixels behind itself, blurred.
 * On the Fallback tier this is a pure pass-through — zero overhead.
 */
@Composable
fun GlassBackdropSource(
    state: LiquidGlassState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    SideEffect {
        state.backdrop = if (state.tier == GlassTier.Full) layer else null
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                state.backdropOriginX = pos.x
                state.backdropOriginY = pos.y
            }
            .drawWithContent {
                if (state.tier == GlassTier.Full && state.consumerCount > 0) {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                } else {
                    // No glass surface is on screen (bar hidden/removed) or the
                    // device is on the Fallback tier: draw straight to the
                    // display — identical pixels, zero recording/layer cost.
                    drawContent()
                }
            },
    ) {
        content()
    }
}

/** One tab of the glass navigation bar. */
data class LiquidGlassTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
    val testTag: String? = null,
)

/**
 * Floating liquid-glass capsule navigation bar.
 *
 * @param tabs          ordered tab descriptors
 * @param selectedIndex currently selected index (0-based)
 * @param onTabSelected invoked with the tapped index
 * @param state         shared [LiquidGlassState] whose backdrop gets blurred
 * @param barHeight     capsule height (default 64dp)
 */
@Composable
fun LiquidGlassNavBar(
    tabs: List<LiquidGlassTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    state: LiquidGlassState,
    modifier: Modifier = Modifier,
    barHeight: Dp = 64.dp,
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val capsuleShape = RoundedCornerShape(percent = 50)

    // Register this glass surface while it is composed. The backdrop source
    // records the content behind the bar ONLY while at least one consumer
    // exists — so a bar that is hidden (scrolled away / removed) never forces
    // a full-screen GraphicsLayer re-record during scrolling.
    DisposableEffect(state) {
        state.consumerCount++
        onDispose { state.consumerCount-- }
    }

    // ─── Theme-aware palette (mirrors LiquidGlassDefaults.tintFor) ───
    val veilColor = if (isDark) Color(0xFF0B1220) else Color(0xFFFFFFFF)
    val veilAlpha = when (state.tier) {
        GlassTier.Full -> if (isDark) 0.58f else 0.50f
        GlassTier.Fallback -> if (isDark) 0.92f else 0.90f
    }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.65f)
    val sheenBrush = if (isDark) {
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.20f),
            0.12f to Color.White.copy(alpha = 0.05f),
            0.50f to Color.White.copy(alpha = 0.01f),
            0.88f to Color.White.copy(alpha = 0.06f),
            1.00f to Color.White.copy(alpha = 0.13f),
        )
    } else {
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.85f),
            0.12f to Color.White.copy(alpha = 0.28f),
            0.50f to Color.White.copy(alpha = 0.06f),
            0.88f to Color.White.copy(alpha = 0.24f),
            1.00f to Color.White.copy(alpha = 0.50f),
        )
    }

    // ─── Blur chain: blur(24dp) → saturation(1.4x), built once (Full tier only) ───
    // Applied as a PERSISTENT RenderEffect on the glass layer below. Because it
    // is a property of that layer (not set/reset around each draw op), the blur
    // survives every display-list replay and keeps diffusing the scrolling
    // content behind the bar, so background text never collides with the labels.
    //
    // ─── Edge treatment (3rd arg of createBlurEffect) — Shader.TileMode ───
    // The blur kernel always reads a few pixels OUTSIDE the glass layer's
    // raster near the borders. This parameter decides what those out-of-bounds
    // samples return:
    //
    //   CLAMP  (default of the 2-arg createBlurEffect overload)
    //          Extends the outermost edge pixels infinitely outward. Output
    //          stays fully opaque at the borders, but high-contrast content at
    //          the edges smears into visible streaks — the wrong look for a
    //          floating glass capsule sitting over scrolling content.
    //
    //   DECAL  ← CURRENT CHOICE
    //          Treats outside samples as transparent. The blur fades naturally
    //          at the borders instead of streaking. This is the standard edge
    //          treatment for backdrop/behind-blur (frosted glass), because the
    //          real content continues beyond the blurred region anyway. Any
    //          slight transparency at the extreme borders is covered by the
    //          veil + capsule clip drawn on top.
    //
    //   REPEAT
    //          Tiles the whole source repeatedly outward — ghost copies of the
    //          content appear near the edges. Only useful for seamless
    //          textures/patterns, never for glass.
    //
    //   MIRROR
    //          Mirrors the content outward — avoids hard streaks but shows
    //          reflected ghosting of the content. Rarely desirable here.
    //
    // All four modes exist since API 31 — the same level this Full-tier path
    // already requires (see the SDK_INT guard below), so switching between
    // them is always safe here. If you ever switch back to CLAMP, re-check the
    // capsule's curved tips for edge streaks (e.g. with a screenshot probe).
    val density = LocalDensity.current
    val blurChain: androidx.compose.ui.graphics.RenderEffect? = remember(state.tier, density) {
        if (state.tier == GlassTier.Full && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurPx = with(density) { 24.dp.toPx() }
            val blurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL)
            // Android has no createColorMatrixEffect — the saturation boost goes
            // through ColorMatrixColorFilter instead.
            val saturationFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix(saturationMatrix(1.4f))
            )
            val saturationEffect = RenderEffect.createColorFilterEffect(saturationFilter)
            RenderEffect.createChainEffect(saturationEffect, blurEffect).asComposeRenderEffect()
        } else {
            null
        }
    }

    // ─── Slider animation (iOS spring-like glide) ───
    val sliderPosition = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, tabs.size) {
        sliderPosition.animateTo(
            targetValue = selectedIndex.coerceIn(0, tabs.size - 1).toFloat(),
            animationSpec = tween(durationMillis = 420, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)),
        )
    }

    // Per-item color cross-fade instead of a hard snap.
    @Composable
    fun tabColor(selected: Boolean): Color {
        val target = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.62f else 0.72f)
        }
        return animateColorAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 300),
            label = "glassTabColor",
        ).value
    }

    val pillColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.90f)

    BoxWithConstraints(
        modifier = modifier
            .height(barHeight)
            .shadow(elevation = 10.dp, shape = capsuleShape, clip = false),
    ) {
        // ── Glass BODY: blurred backdrop + frosted veil ──
        // The whole node is wrapped in a graphicsLayer carrying the blur
        // RenderEffect, so EVERYTHING this node draws — the translated backdrop
        // AND the veil — is composited blurred, on every replay. `.clip` sits
        // OUTSIDE the blur layer so the smeared output is cut to the capsule.
        val barOrigin = remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
        Box(
            Modifier
                .matchParentSize()
                .onGloballyPositioned { barOrigin.value = it.positionInRoot() }
                .clip(capsuleShape)
                .graphicsLayer { renderEffect = blurChain }
                .drawWithCache {
                    val radius = size.height / 2f
                    onDrawWithContent {
                        val backdrop = state.backdrop
                        if (backdrop != null) {
                            // Shift the recorded backdrop so the pixels that sit
                            // directly behind the bar land inside the capsule.
                            translate(
                                left = state.backdropOriginX - barOrigin.value.x,
                                top = state.backdropOriginY - barOrigin.value.y,
                            ) {
                                drawLayer(backdrop)
                            }
                        }
                        drawRoundRect(veilColor.copy(alpha = veilAlpha), cornerRadius = CornerRadius(radius))
                    }
                },
        )

        // ── Glass DETAILS: specular sheen + hairline border (drawn SHARP,
        //    above the blurred body, so the glass keeps its crisp edges) ──
        Box(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val radius = size.height / 2f
                    onDrawWithContent {
                        drawRoundRect(brush = sheenBrush, cornerRadius = CornerRadius(radius))
                        drawRoundRect(
                            color = borderColor,
                            cornerRadius = CornerRadius(radius),
                            style = Stroke(width = 1.5f),
                        )
                        drawContent()
                    }
                },
        )

        // ── Slider pill (above the glass, below the items) ──
        // Row below has padding(horizontal = 5.dp), so each item slot spans
        // [5.dp + i*slotW, 5.dp + (i+1)*slotW) in LTR. Modifier.offset { }
        // is RTL-aware (placeRelative) in Compose 1.7, so the same x formula
        // aligns the pill under item i in BOTH directions.
        val rowEdgePad = 5.dp
        val slotW = (maxWidth - rowEdgePad * 2) / tabs.size.coerceAtLeast(1)
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset { IntOffset((rowEdgePad + slotW * sliderPosition.value).roundToPx(), 0) }
                    .width(slotW)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(capsuleShape)
                    .background(pillColor)
                    .drawBehind {
                        drawRoundRect(
                            color = pillBorder,
                            cornerRadius = CornerRadius(size.height / 2f),
                            style = Stroke(width = 1.5f),
                        )
                    },
            )
        }

        // ── Items ──
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(capsuleShape)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) { onTabSelected(index) }
                        .then(if (tab.testTag != null) Modifier.testTag(tab.testTag) else Modifier),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.contentDescription,
                        tint = tabColor(selected),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        fontFamily = Vazirmatn,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tabColor(selected),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 4x5 color matrix scaling saturation around Rec.709 luminance — the exact
 * saturation boost the library applies in its Full tier (1.4x).
 */
internal fun saturationMatrix(saturation: Float): FloatArray {
    val lumR = 0.2126f
    val lumG = 0.7152f
    val lumB = 0.0722f
    val sr = (1f - saturation) * lumR
    val sg = (1f - saturation) * lumG
    val sb = (1f - saturation) * lumB
    return floatArrayOf(
        sr + saturation, sg, sb, 0f, 0f,
        sr, sg + saturation, sb, 0f, 0f,
        sr, sg, sb + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}
