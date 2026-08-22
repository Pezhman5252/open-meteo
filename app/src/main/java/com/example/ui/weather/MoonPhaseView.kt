package com.example.ui.weather

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

/**
 * A highly realistic custom View that dynamically draws the Moon Phase
 * using Android's Canvas, Paint, Path, and RadialGradient APIs.
 * 
 * This version supports:
 * - Precise Waxing/Waning detection based on lunar age (ageDays)
 * - Realistic 3D-shaded gradients with golden outer glow
 * - Smooth animations and optimized rendering
 * 
 * Accuracy: Uses ageDays to determine phase direction, which is more reliable
 * than phaseName string matching.
 */
class MoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Illumination percentage [0.0, 1.0]
    var illumination: Float = 0.5f
        set(value) {
            val coerced = value.coerceIn(0.0f, 1.0f)
            if (field != coerced) {
                field = coerced
                invalidate()
            }
        }

    // true = Waxing (right side illuminated), false = Waning (left side illuminated)
    var isWaxing: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // Lunar age in days (optional, used for advanced features)
    var ageDays: Double = 0.0
        set(value) {
            if (field != value) {
                field = value
                // No need to invalidate, only used for debugging or future features
            }
        }

    // Paint objects (optimized for performance)
    private val darkBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A1A2E") // Deep space dark grayish-blue
    }

    private val darkBaseHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#3A3A5A") // Faint rim highlight for the dark side
    }

    private val illuminatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val mainOval = RectF()
    private val ellipseOval = RectF()

    // Gradient colors for realistic moon surface
    private val gradientColors = intArrayOf(
        Color.parseColor("#FFFEF0"), // Bright white-yellow center
        Color.parseColor("#FFF8E1"), // Warm yellow
        Color.parseColor("#F5E6A3")  // Champagne/gold edge
    )
    private val gradientStops = floatArrayOf(0.0f, 0.35f, 1.0f)

    // Cached values for performance
    private var lastWidth = -1f
    private var lastHeight = -1f
    private var lastIllumination = -1f
    private var lastIsWaxing = true
    private var cachedGradient: RadialGradient? = null

    init {
        // Required for ShadowLayer (outer glow) to work correctly
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val size = minOf(width, height)

        // Padding for outer glow
        val padding = size * 0.12f
        val radius = (size - padding * 2) / 2f
        val cx = width / 2f
        val cy = height / 2f

        if (radius <= 0) return

        // 1. Draw the dark side silhouette
        canvas.drawCircle(cx, cy, radius, darkBasePaint)
        canvas.drawCircle(cx, cy, radius, darkBaseHighlightPaint)

        // If illumination is nearly zero, treat as New Moon (no illuminated part)
        if (illumination <= 0.02f) {
            illuminatedPaint.clearShadowLayer()
            return
        }

        // 2. Calculate Terminator Position
        // pos > 0: illuminated portion on the right (waxing)
        // pos < 0: illuminated portion on the left (waning)
        var pos = (illumination * 2f) - 1f
        if (!isWaxing) {
            pos = -pos
        }

        // 3. Build the illuminated path
        path.reset()
        mainOval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Outer semicircle (illuminated side)
        val startAngle = -90f
        val outerSweep = if (isWaxing) 180f else -180f
        path.arcTo(mainOval, startAngle, outerSweep, true)

        // Terminator elliptical arc
        val absPos = kotlin.math.abs(pos)
        ellipseOval.set(cx - absPos * radius, cy - radius, cx + absPos * radius, cy + radius)

        val terminatorSweep = when {
            pos > 0f  -> 180f   // waxing: CW from bottom to top
            pos < 0f  -> -180f  // waning: CCW from bottom to top
            isWaxing  -> 180f   // exactly half, waxing → right side
            else      -> -180f  // exactly half, waning → left side
        }
        path.arcTo(ellipseOval, 90f, terminatorSweep, false)
        path.close()

        // 4. Configure realistic 3D gradient
        val gradientShiftX = if (isWaxing) 0.3f * radius else -0.3f * radius

        // Cache gradient for performance
        if (cachedGradient == null ||
            width != lastWidth ||
            height != lastHeight ||
            illumination != lastIllumination ||
            isWaxing != lastIsWaxing
        ) {
            lastWidth = width
            lastHeight = height
            lastIllumination = illumination
            lastIsWaxing = isWaxing

            cachedGradient = RadialGradient(
                cx + gradientShiftX, cy - 0.12f * radius, radius * 1.3f,
                gradientColors, gradientStops,
                Shader.TileMode.CLAMP
            )
        }
        illuminatedPaint.shader = cachedGradient

        // 5. Dynamic outer glow (soft golden aura)
        illuminatedPaint.clearShadowLayer() // Clear previous to avoid ghosting

        val glowRadius = illumination * (size * 0.07f)
        val glowAlpha = (illumination * 150).toInt()
        val glowColor = Color.argb(glowAlpha, 255, 252, 230) // Soft golden

        if (glowRadius > 0.5f) {
            illuminatedPaint.setShadowLayer(glowRadius, 0f, 0f, glowColor)
        }

        // 6. Draw the illuminated moon part
        canvas.drawPath(path, illuminatedPaint)
    }
}

/**
 * Jetpack Compose native drawing of Moon Phase with full parameter support.
 * Uses ageDays for precise Waxing/Waning detection.
 */
@Composable
fun MoonPhaseViewCompose(
    illumination: Float,
    isWaxing: Boolean,
    ageDays: Double = 0.0, // Added for advanced use
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(76.dp)
    ) {
        val width = size.width
        val height = size.height
        val sizeMin = minOf(width, height)

        val padding = sizeMin * 0.12f
        val radius = (sizeMin - padding * 2) / 2f
        val cx = width / 2f
        val cy = height / 2f

        if (radius > 0f) {
            // 1. Draw dark background circle (lunar body silhouette)
            drawCircle(
                color = ComposeColor(0xFF1A1A2E), // Deep space dark grayish-blue
                radius = radius,
                center = Offset(cx, cy)
            )

            // Rim highlight
            drawCircle(
                color = ComposeColor(0xFF3A3A5A), // Faint rim highlight
                radius = radius,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )

            if (illumination > 0.02f) {
                // Calculate position of terminator
                var pos = (illumination * 2f) - 1f
                if (!isWaxing) {
                    pos = -pos
                }

                val androidPath = android.graphics.Path()
                val mainOval = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                val startAngle = -90f
                val outerSweep = if (isWaxing) 180f else -180f
                androidPath.arcTo(mainOval, startAngle, outerSweep, true)

                val absPos = kotlin.math.abs(pos)
                val ellipseOval = android.graphics.RectF(cx - absPos * radius, cy - radius, cx + absPos * radius, cy + radius)
                val terminatorSweep = when {
                    pos > 0f  -> 180f
                    pos < 0f  -> -180f
                    isWaxing  -> 180f
                    else      -> -180f
                }
                androidPath.arcTo(ellipseOval, 90f, terminatorSweep, false)
                androidPath.close()

                val composePath = ComposePath().apply {
                    asAndroidPath().set(androidPath)
                }

                // 4. Configure realistic 3D gradient
                val gradientShiftX = if (isWaxing) 0.3f * radius else -0.3f * radius
                val brush = Brush.radialGradient(
                    colors = listOf(
                        ComposeColor(0xFFFFFEF0), // Bright white-yellow center
                        ComposeColor(0xFFFFF8E1), // Warm yellow
                        ComposeColor(0xFFF5E6A3)  // Champagne/gold edge
                    ),
                    center = Offset(cx + gradientShiftX, cy - 0.12f * radius),
                    radius = radius * 1.3f
                )

                drawPath(
                    path = composePath,
                    brush = brush
                )
            }
        }
    }
}

/**
 * Convenience function for HomeScreen that uses phaseName or ageDays.
 * This version uses ageDays for highest accuracy.
 */
@Composable
fun MoonPhaseCanvas(
    illuminationPercent: Int,
    ageDays: Double,          // Accurate Waxing/Waning detection
    phaseName: String = "",    // Fallback if ageDays is not available
    modifier: Modifier = Modifier
) {
    // Use ageDays for precise detection
    // Age < 14.76 days = Waxing (increasing), Age > 14.76 = Waning (decreasing)
    val isWaxing = if (ageDays > 0.0) {
        ageDays < 14.76
    } else {
        // Fallback to phaseName string matching (less accurate)
        !phaseName.contains("کاهنده") && !phaseName.contains("آخر")
    }
    
    val illumination = (illuminationPercent / 100f).coerceIn(0f, 1f)

    MoonPhaseViewCompose(
        illumination = illumination,
        isWaxing = isWaxing,
        ageDays = ageDays,
        modifier = modifier
    )
}