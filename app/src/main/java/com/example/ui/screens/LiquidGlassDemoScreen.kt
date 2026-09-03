package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassBackdropSource
import com.example.ui.components.LiquidGlassNavBar
import com.example.ui.components.LiquidGlassTab
import com.example.ui.components.rememberLiquidGlassState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Vazirmatn
import com.example.ui.theme.isDark

/**
 * ============================================================================
 *  LIQUID GLASS DEMO SCREEN
 * ============================================================================
 *  A complete, self-contained reference page showing how to compose the
 *  liquid-glass navigation bar in ANY screen:
 *
 *      1. Scaffold with containerColor = Transparent
 *      2. Box with colorful content BEHIND the bar (a gradient "image" plus
 *         colored cards — scrollable so you can see the glass blur live)
 *      3. GlassBackdropSource wrapping that content (records it for blur)
 *      4. LiquidGlassNavBar floating at the bottom
 *
 *  Tab state is plain `remember { mutableStateOf(...) }`, exactly as asked.
 *  Dark/light adaptation is automatic (colors derive from MaterialTheme).
 *  RTL is automatic too (offset is RTL-aware).
 *
 *  To apply this pattern to an existing screen: swap the plain Scaffold
 *  bottomBar for LiquidGlassNavBar, wrap the scaffold content in
 *  GlassBackdropSource with a shared rememberLiquidGlassState(), and give
 *  the content Box padding(bottom = 0.dp) so it scrolls under the glass.
 * ============================================================================
 */
@Composable
fun LiquidGlassDemoScreen(modifier: Modifier = Modifier) {
    // ── Tab state: remember + mutableStateOf (as requested) ──
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = remember {
        listOf(
            LiquidGlassTab("خانه", Icons.Default.Terrain, "خانه"),
            LiquidGlassTab("جستجو", Icons.Default.Search, "جستجو"),
            LiquidGlassTab("تنظیمات", Icons.Default.Settings, "تنظیمات"),
        )
    }

    // One shared state links the backdrop recorder to the glass bar.
    val glassState = rememberLiquidGlassState()

    // A colorful gradient "image" behind everything, so the frosted blur has
    // something vivid to diffuse (best proof the effect is really working).
    val bgBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF4338CA),
            Color(0xFF7C3AED),
            Color(0xFFDB2777),
            Color(0xFFF59E0B),
        ),
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            // Floating capsule — never touches the screen edges.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                LiquidGlassNavBar(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    state = glassState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { innerPadding ->
        // The backdrop recorder must wrap exactly what sits behind the bar.
        GlassBackdropSource(
            state = glassState,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush),
            ) {
                // Foreground "list" — scrolls under the floating glass bar.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = 120.dp, // content slides beneath the capsule
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "نمونه صفحه شیشه‌ای",
                            color = Color.White,
                            fontFamily = Vazirmatn,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        Text(
                            text = "کارت‌ها را اسکرول کنید؛ محتوای زیر نوار شیشه‌ای به‌صورت زنده بلور می‌شود.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = Vazirmatn,
                            fontSize = 13.sp,
                        )
                    }
                    itemsIndexed(List(14) { it }) { index, _ ->
                        DemoCard(
                            title = "کارت ${index + 1}",
                            color = cardColors[index % cardColors.size],
                            dark = MaterialTheme.colorScheme.background.isDark,
                        )
                    }
                }

                // Small caption of the active tab near the top.
                Text(
                    text = "تب فعال: ${tabs[selectedTab].label}",
                    color = Color.White,
                    fontFamily = Vazirmatn,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 20.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private val cardColors = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF43F5E),
    Color(0xFF8B5CF6),
    Color(0xFFF59E0B),
)

@Composable
private fun DemoCard(title: String, color: Color, dark: Boolean) {
    val surface = if (dark) Color(0xFF1A2332) else Color(0xFFFFFFFF)
    val accentAlpha = if (dark) 0.90f else 1f
    val cardColor by animateColorAsState(
        targetValue = surface,
        animationSpec = tween(250),
        label = "demoCardSurface",
    )
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (dark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = accentAlpha)),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontFamily = Vazirmatn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else Color(0xFF111827),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "این کارت زیر نوار شیشه‌ای شناور اسکرول می‌شود و از پشت شیشه مات دیده می‌شود — دقیقاً مثل تلگرام جدید.",
                fontFamily = Vazirmatn,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = if (dark) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280),
            )
        }
    }
}

@Preview(name = "Liquid Glass Demo — Light", showBackground = true)
@Composable
private fun LiquidGlassDemoLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        LiquidGlassDemoScreen()
    }
}

@Preview(name = "Liquid Glass Demo — Dark", showBackground = true, backgroundColor = 0xFF101418)
@Composable
private fun LiquidGlassDemoDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        LiquidGlassDemoScreen()
    }
}
