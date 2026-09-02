package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.isActive
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MountainEntity
import com.example.data.remote.WeatherResponse
import com.example.data.remote.HourlyData
import com.example.data.remote.CurrentWeather
import com.example.ui.util.MountaineeringHelper
import com.example.ui.util.SafetyStatus
import com.example.ui.util.PersianDateHelper
import com.example.ui.util.WeatherCodeHelper
import com.example.ui.weather.WeatherUiState
import com.example.ui.weather.WeatherViewModel
import com.example.ui.theme.*
import com.example.ui.theme.isDark

@Composable
fun getTextColor(alpha: Float = 1f): Color {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val base = if (isDark) Color.White else Color(0xFF0F172A)
    return if (alpha == 1f) base else base.copy(alpha = alpha)
}

@Composable
fun getAccentColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.isDark
    return if (isDark) Color(0xFF00FFE0) else Color(0xFF005F55)
}

@Composable
fun getGreenAccentColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.isDark
    return if (isDark) Color(0xFF00FF87) else Color(0xFF2E7D32)
}

@Composable
fun getCardBgColor(darkOverride: Color = Color(0xFF0C101B)): Color {
    val isDark = MaterialTheme.colorScheme.background.isDark
    return if (isDark) darkOverride else Color.White
}

@Composable
fun getCardBorderStroke(tagColor: Color? = null): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val color = when {
        tagColor != null -> {
            val alphaVal = if (isDark) 0.35f else 0.65f
            tagColor.copy(alpha = alphaVal)
        }
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color(0xFFE2E8F0)
    }
    return BorderStroke(1.dp, color)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val pinnedList by viewModel.pinnedMountains.collectAsStateWithLifecycle()
    val selectedMountain by viewModel.selectedMountain.collectAsStateWithLifecycle()
    val selectedAltitude by viewModel.selectedAltitude.collectAsStateWithLifecycle()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsStateWithLifecycle()

    val offlineErrorEvent by viewModel.offlineErrorEvent.collectAsStateWithLifecycle()
    val cachedMountainIds by viewModel.cachedMountainIds.collectAsStateWithLifecycle()
    val allMountains by viewModel.allMountains.collectAsStateWithLifecycle()

    val cachedMountains = remember(allMountains, cachedMountainIds) {
        allMountains.filter { it.id in cachedMountainIds }
    }

    if (offlineErrorEvent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearOfflineErrorEvent() },
            title = {
                Text(
                    text = "عدم دسترسی به پیش‌بینی آفلاین",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Vazirmatn
                )
            },
            text = {
                Text(
                    text = "اطلاعات هواشناسی قله «${offlineErrorEvent?.persianName}» قبلاً به صورت آنلاین بارگذاری نشده است و در حافظه گوشی شما موجود نیست. لطفا برای بارگیری اولیه به اینترنت متصل شوید یا قله دیگری را انتخاب کنید.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    fontFamily = Vazirmatn
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearOfflineErrorEvent() }) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = Vazirmatn)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("home_screen_root")
    ) {
        when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.testTag("loading_indicator"),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "درحال تحلیل جبهه‌های هوای قله...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    HomeScreenContent(
                        viewModel = viewModel,
                        mountain = state.mountain,
                        weather = state.weather,
                        pinnedCount = pinnedList.size,
                        selectedAltitude = selectedAltitude,
                        lastUpdatedTime = lastUpdatedTime,
                        onChangePeakClick = { showBottomSheet = true },
                        onSearchClick = onSearchClick,
                        isOffline = state.isOffline,
                        offlineTime = state.offlineTime
                    )
                }
                is WeatherUiState.Error -> {
                    val (errorIcon, errorColor, errorBg) = when (state.errorType) {
                        com.example.ui.weather.WeatherErrorType.NO_INTERNET -> Triple(
                            androidx.compose.material.icons.Icons.Default.WifiOff,
                            Color(0xFFE05A00),
                            Color(0xFFE05A00).copy(alpha = 0.12f)
                        )
                        com.example.ui.weather.WeatherErrorType.TIMEOUT -> Triple(
                            androidx.compose.material.icons.Icons.Default.HourglassEmpty,
                            Color(0xFFD97706),
                            Color(0xFFD97706).copy(alpha = 0.12f)
                        )
                        com.example.ui.weather.WeatherErrorType.SERVER_ERROR -> Triple(
                            androidx.compose.material.icons.Icons.Default.CloudOff,
                            Color(0xFFDC2626),
                            Color(0xFFDC2626).copy(alpha = 0.12f)
                        )
                        com.example.ui.weather.WeatherErrorType.UNKNOWN -> Triple(
                            androidx.compose.material.icons.Icons.Default.Warning,
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    }

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(errorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = errorIcon,
                                contentDescription = "Error Icon",
                                tint = errorColor,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = errorColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = Vazirmatn
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { selectedMountain?.let { viewModel.fetchWeather(it) } },
                            modifier = Modifier
                                .testTag("retry_button")
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = errorColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("سیگنال‌گیری مجدد", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = Vazirmatn)
                        }

                        if (cachedMountains.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(36.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قله‌های دارای پیش‌بینی آفلاین (ذخیره شده):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.clearAllCachedWeather() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "پاکسازی کش",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "حذف کل کش",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Vazirmatn
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                cachedMountains.forEach { mount ->
                                    val cachedTime = viewModel.getCachedTimeForMountain(mount.id) ?: "نامشخص"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectMountain(mount) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = getCardBorderStroke()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Terrain,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = mount.persianName,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontFamily = Vazirmatn
                                                    )
                                                    Text(
                                                        text = "${mount.persianProvince} • بروزرسانی: $cachedTime",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        fontFamily = Vazirmatn,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${PersianDateHelper.formatToPersianDigits(mount.altitude)} م",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontFamily = Vazirmatn
                                                )
                                                IconButton(
                                                    onClick = { viewModel.clearCachedWeatherForMountain(mount.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف کش قله",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom sheet for quickly choosing pinned mountains
            if (showBottomSheet) {
                val isDark = MaterialTheme.colorScheme.background.isDark
                val sheetBg = if (isDark) Color(0xFF080C14) else MaterialTheme.colorScheme.surface
                val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                val textMuted = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                val textUltraMuted = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                val accentColor = if (isDark) Color(0xFF00FFE0) else Color(0xFF00897B)
                val cardBgNormal = if (isDark) Color(0xFF0E121B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                val cardBgSelected = if (isDark) Color(0xFF00FFE0).copy(alpha = 0.04f) else Color(0xFF00897B).copy(alpha = 0.12f)
                val borderNormal = if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = sheetBg,
                    scrimColor = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(width = 44.dp, height = 4.dp)
                                .clip(RoundedCornerShape(100))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.4f),
                                            if (isDark) Color(0xFF4A00E0).copy(alpha = 0.4f) else accentColor.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )
                    },
                    modifier = Modifier.testTag("pinned_selection_sheet")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
                    ) {
                        // Header block with Tech Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "جابجایی سریع بین قله‌ها",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "انتقال رادار هواشناسی میان ارتفاعات نشانه‌گذاری‌شده",
                                    fontSize = 10.sp,
                                    color = textMuted,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.08f))
                                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        showBottomSheet = false
                                        onSearchClick()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Peak",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "جستجوی قله جدید",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        }

                        if (pinnedList.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, borderNormal),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgNormal
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFFFB300).copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = "Empty",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "هیچ قله‌ای اضافه نشده است",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "از بخش بانک قله‌ها می‌توانید صعودهای محبوب و پرطرفدار را نشانه‌گذاری و ذخیره کنید.",
                                        fontSize = 10.sp,
                                        color = textMuted,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            showBottomSheet = false
                                            onSearchClick()
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.12f),
                                            contentColor = accentColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "جستجوی قله جدید",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(pinnedList, key = { it.id }) { mount ->
                                    val isSelected = mount.id == selectedMountain?.id
                                    
                                    val borderStroke = if (isSelected) {
                                        BorderStroke(
                                            width = 1.2.dp,
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    accentColor,
                                                    if (isDark) Color(0xFF4A00E0) else accentColor
                                                )
                                            )
                                        )
                                    } else {
                                        BorderStroke(0.6.dp, borderNormal)
                                    }

                                    val cardBackground = if (isSelected) {
                                        cardBgSelected
                                    } else {
                                        cardBgNormal
                                    }

                                    Card(
                                        onClick = {
                                            viewModel.selectMountain(mount)
                                            showBottomSheet = false
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("mount_switch_item_${mount.id}"),
                                        shape = RoundedCornerShape(18.dp),
                                        border = borderStroke,
                                        colors = CardDefaults.cardColors(
                                            containerColor = cardBackground
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Left: Peak details
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val peakIcon = when {
                                                        mount.isCustom -> Icons.Default.Person
                                                        mount.type == "ski_resort" -> Icons.Default.AcUnit
                                                        mount.type == "international_peak" -> Icons.Default.Language
                                                        else -> Icons.Default.FilterHdr
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(
                                                                if (isSelected) accentColor.copy(alpha = 0.12f)
                                                                else if (isDark) Color.White.copy(alpha = 0.03f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = peakIcon,
                                                            contentDescription = "Peak Target",
                                                            tint = if (isSelected) accentColor else textColor.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = mount.persianName,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 14.sp,
                                                                color = textColor
                                                            )
                                                            if (isSelected) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(100),
                                                                    color = Color(0xFF00FF87).copy(alpha = 0.1f),
                                                                    border = BorderStroke(0.6.dp, Color(0xFF00FF87).copy(alpha = 0.25f))
                                                                ) {
                                                                    Text(
                                                                        text = "رادار فعال",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        color = Color(0xFF00FF87),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "${mount.persianProvince} • رشته کوه ${mount.range ?: "نامشخص"}",
                                                            fontSize = 9.sp,
                                                            color = textMuted,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // Right: Altitude + Quick Toggle love button to unpin
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${PersianDateHelper.formatToPersianDigits(mount.altitude)} متر",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = if (isSelected) accentColor else textColor
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.togglePin(mount)
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = "Unpin",
                                                            tint = Color(0xFFFF2A5F),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Sub-info: GPS indicator in tech layout
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(0.6.dp)
                                                    .background(borderNormal)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = run {
                                                        val latDir = if (mount.latitude >= 0) "N" else "S"
                                                        val lonDir = if (mount.longitude >= 0) "E" else "W"
                                                        "مختصات: $latDir ${PersianDateHelper.formatToPersianDigits(kotlin.math.abs(mount.latitude))}° / $lonDir ${PersianDateHelper.formatToPersianDigits(kotlin.math.abs(mount.longitude))}°"
                                                    },
                                                    fontSize = 8.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    color = textUltraMuted,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "شناسه موقعیت: ${PersianDateHelper.formatToPersianDigits(mount.id)}",
                                                    fontSize = 8.sp,
                                                    color = textUltraMuted,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun OfflineBanner(offlineTime: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF7ED), // Very soft warm/orange warning background
            contentColor = Color(0xFFC2410C)   // Warm orange/brown text color
        ),
        border = BorderStroke(1.dp, Color(0xFFFFEDD5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEA580C).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "ارتباط قطع است (مشاهده آفلاین قله)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اطلاعات زیر ذخیره شده از ساعت $offlineTime است. برای دیدن هواشناسی بروز، اتصال اینترنت خود را برقرار کنید.",
                    fontSize = 10.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF9A3412)
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    viewModel: WeatherViewModel,
    mountain: MountainEntity,
    weather: WeatherResponse,
    pinnedCount: Int,
    selectedAltitude: Int?,
    lastUpdatedTime: String,
    onChangePeakClick: () -> Unit,
    onSearchClick: () -> Unit,
    isOffline: Boolean = false,
    offlineTime: String? = null
) {
    val activeAltitude = selectedAltitude ?: mountain.altitude
    val selectedDaysCount by viewModel.selectedDaysCount.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val peakTemp = weather.current?.temperature2m ?: 0.0

    var isRefreshing by remember { mutableStateOf(false) }
    // Distinguishes WHICH trigger started the refresh: the drag gesture shows the pull
    // header; the manual button only spins its own icon — they never share a UI.
    var refreshSourceIsGesture by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Central transient status toast (replaces the bottom-anchored Snackbar for refresh feedback).
    var refreshToast by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val refreshToastAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(refreshToast) {
        val toast = refreshToast ?: return@LaunchedEffect
        refreshToastAlpha.snapTo(0f)
        refreshToastAlpha.animateTo(
            1f,
            androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
        kotlinx.coroutines.delay(3000)
        refreshToastAlpha.animateTo(
            0f,
            androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
        if (refreshToast?.second == toast.second) refreshToast = null
    }

    val handleRefresh = { fromGesture: Boolean ->
        if (!isRefreshing) {
            isRefreshing = true
            refreshSourceIsGesture = fromGesture
            viewModel.refreshCurrentMountainWeather { isSuccess ->
                isRefreshing = false
                refreshSourceIsGesture = false
                refreshToast = if (isSuccess) {
                    true to "داده‌ها بروزرسانی شد"
                } else {
                    false to "خطا در دریافت داده - بررسی اتصال"
                }
            }
        }
    }

    val handleTogglePin = { mountainToPin: MountainEntity ->
        viewModel.togglePin(mountainToPin) { newPinnedState ->
            coroutineScope.launch {
                val msg = if (newPinnedState) {
                    "«${mountainToPin.persianName}» به علاقه‌مندی‌ها اضافه شد"
                } else {
                    "«${mountainToPin.persianName}» از علاقه‌مندی‌ها حذف شد"
                }
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val lazyListState = rememberLazyListState()
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val refreshThresholdPx = remember(density) { with(density) { 90.dp.toPx() } }

    // ─────────────────────────────────────────────────────────────────────
    // DELIBERATE PULL-TO-REFRESH — final semantics (user-specified):
    //  1. A fast fling/drag that ARRIVES at the top never shows the pull label
    //     and never refreshes — regardless of leftover overscroll.
    //  2. The pull arms ONLY at the START of a fresh drag that begins while the
    //     list is ALREADY resting at the very top (DragInteraction.Start gate).
    //  3. Once armed and the drag passes the threshold, the refresh commits
    //     INSTANTLY mid-drag — no "release" step.
    // ─────────────────────────────────────────────────────────────────────
    val view = androidx.compose.ui.platform.LocalView.current
    val isRefreshingState = rememberUpdatedState(isRefreshing)
    val pullArmed = remember { mutableStateOf(false) }
    // Timestamp of the last scroll event that occurred while the list was AWAY from
    // the very top. A drag that begins within 300ms of it is a "traveling arrival"
    // (fast scroll from below / a still-moving fling) and must NEVER arm — this
    // closes the race where DragInteraction.Start is processed after the fling
    // has already reached the top.
    val lastAwayScrollMs = remember { mutableStateOf(0L) }

    // Arm the pull ONLY for a fresh drag that starts while the list has been
    // resting at the very top for at least 300ms.
    LaunchedEffect(lazyListState) {
        lazyListState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.DragInteraction.Start -> {
                    val top = lazyListState.firstVisibleItemIndex == 0 &&
                            lazyListState.firstVisibleItemScrollOffset == 0
                    val fresh = android.os.SystemClock.uptimeMillis() - lastAwayScrollMs.value > 300L
                    pullArmed.value = top && fresh && !isRefreshingState.value
                }
                is androidx.compose.foundation.interaction.DragInteraction.Stop,
                is androidx.compose.foundation.interaction.DragInteraction.Cancel -> {
                    pullArmed.value = false
                    // Safety net: if the drag ends with (near-)zero velocity and no fling
                    // is dispatched, the header must not hang open mid-pull.
                    if (pullDistance > 0f) pullDistance = 0f
                }
            }
        }
    }

    val pullScrollConnection = remember(isRefreshing, refreshThresholdPx) {
        object : NestedScrollConnection {
            private fun atVeryTop(): Boolean =
                lazyListState.firstVisibleItemIndex == 0 &&
                        lazyListState.firstVisibleItemScrollOffset == 0

            // Gentle progressive resistance — the real anti-fling protection is the
            // fresh-drag gate above, so the curve stays light enough that a normal
            // deliberate pull (≈ 400 px) reliably crosses the threshold.
            private fun dampedDelta(raw: Float): Float {
                val maxPull = refreshThresholdPx * 2.2f
                if (pullDistance >= maxPull) return 0f
                val t = pullDistance / maxPull
                return raw * 0.85f * (1f - 0.4f * t)
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Any scroll motion while away from the top marks the list as "moving".
                if (!atVeryTop()) {
                    lastAwayScrollMs.value = android.os.SystemClock.uptimeMillis()
                }
                // Drag in progress: consume upward motion to retract the pull indicator.
                if (available.y < 0f && pullDistance > 0f) {
                    pullDistance = maxOf(0f, pullDistance + available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && atVeryTop() && !isRefreshing) {
                    if (pullArmed.value) {
                        val before = pullDistance
                        pullDistance = (pullDistance + dampedDelta(available.y)).coerceAtLeast(0f)
                        // Refresh commits the INSTANT the live drag crosses the threshold.
                        if (before < refreshThresholdPx && pullDistance >= refreshThresholdPx) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            pullDistance = 0f
                            pullArmed.value = false
                            handleRefresh(true)
                        }
                    }
                    // ALWAYS consume the leftover at top: an unarmed arrival (fast scroll
                    // from below) shows NO label, and consuming prevents the Android-12
                    // stretch overscroll from activating and swallowing the drag stream
                    // (which would starve the armed path of its deltas).
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance > 0f) {
                    pullDistance = 0f
                }
                pullArmed.value = false
                return Velocity.Zero
            }
        }
    }

    // Compute altitude step meteorological metrics dynamically using physical and barometric lapse rates
    val adjustedCurrent = remember(weather.current, weather.hourly, mountain, activeAltitude) {
        val current = weather.current ?: return@remember null
        val diff = mountain.altitude - activeAltitude

        // 1. Temperature scales with the standard lapse rate (+0.65°C per 100 meters down)
        val adjTemp = current.temperature2m + (diff * 0.0065)

        // 2. Wind speeds using Power Law (instead of linear interpolation)
        val peakWind80m = current.windSpeed80m ?: current.windSpeed10m
        val baseWind10m = current.windSpeed10m

        // استفاده از Power Law برای باد (نسبت ارتفاع هدف به ارتفاع پایه)
        val adjWind80m = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = peakWind80m,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = activeAltitude.toDouble(),
            alpha = null
        )

        val adjWind10m = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = baseWind10m,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = activeAltitude.toDouble(),
            alpha = null
        )

        // Apparent Temperature: تصحیح بر اساس دمای جدید و باد جدید
        val adjApparent = MountaineeringHelper.calculateWindChill(adjTemp, adjWind80m)

        // فشار با دمای تصحیح‌شده و QNH محلی
        val qnhVal = current.pressureMsl ?: 1013.25
        val adjPressure = MountaineeringHelper.calculateBarometricPressure(
            basePressure = current.surfacePressure,
            baseTemp = current.temperature2m,
            baseAltitude = mountain.altitude,
            targetAltitude = activeAltitude,
            targetTemp = adjTemp,
            qnh = qnhVal
        )

        // رطوبت: تصحیح با ارتفاع
        val humidityVal = current.relativeHumidity2m ?: 60.0
        val adjHumidity = MountaineeringHelper.adjustHumidityWithAltitude(
            baseHumidity = humidityVal,
            baseTemp = current.temperature2m,
            basePressure = current.surfacePressure ?: 1013.25,
            targetTemp = adjTemp,
            targetPressure = adjPressure
        )

        val adjDewPoint = MountaineeringHelper.calculateDewPoint(adjTemp, adjHumidity)

        // Gusts
        val baseWindGusts = current.windGusts10m ?: (baseWind10m * MountaineeringHelper.calculateDynamicGustFactor(current.cape))
        val adjWindGusts = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = baseWindGusts,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = activeAltitude.toDouble(),
            alpha = null
        )

        // Visibility & Cloud Cover fallbacks
        val offsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
        val currentHourIdx = MountaineeringHelper.findHourlyIndexForCurrent(current, weather.hourly, offsetHours)
        val fallbackVisibility = weather.hourly?.visibility?.getOrNull(currentHourIdx)
        val fallbackCloudCover = weather.hourly?.cloudCover?.getOrNull(currentHourIdx)?.toDouble()

        val adjVisibility = current.visibility ?: fallbackVisibility
        val adjCloudCover = current.cloudCover ?: fallbackCloudCover
        
        // تفکیک فاز بارش بر اساس دمای مرطوب (Wet-Bulb) مطابق استاندارد WMO به‌جای دمای خشک سطحی:
        // - دمای مرطوب زیر ۱- درجه → بارش جامد (برف)
        // - دمای مرطوب بالای ۰.۵+ درجه → بارش مایع (باران)
        // - بازه میانی (باران یخ‌زده/بارش مخلوط) → کد وضعیت اصلی حفظ می‌شود
        val adjWetBulb = MountaineeringHelper.wetBulbTemp(adjTemp, adjHumidity)
        var finalWeatherCode = current.weatherCode
        when {
            adjWetBulb < -1.0 -> {
                finalWeatherCode = when (finalWeatherCode) {
                    61, 80 -> 71
                    63, 81 -> 73
                    65, 82 -> 75
                    else -> finalWeatherCode
                }
            }
            adjWetBulb > 0.5 -> {
                finalWeatherCode = when (finalWeatherCode) {
                    71, 85 -> 61
                    73, 86 -> 63
                    75 -> 65
                    else -> finalWeatherCode
                }
            }
        }

        current.copy(
            temperature2m = adjTemp,
            apparentTemperature = adjApparent,
            relativeHumidity2m = adjHumidity,
            windSpeed80m = adjWind80m,
            windSpeed10m = adjWind10m,
            surfacePressure = adjPressure,
            dewPoint2m = adjDewPoint,
            windGusts10m = adjWindGusts,
            visibility = adjVisibility,
            cloudCover = adjCloudCover,
            weatherCode = finalWeatherCode
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pull-to-refresh indicator — exclusive to the drag gesture. Shown only while
            // the finger holds the pull (or while a GESTURE-initiated refresh runs).
            // The manual button NEVER opens it — it spins its own icon in the hero card.
            androidx.compose.animation.AnimatedVisibility(
                visible = pullDistance > 4f || (isRefreshing && refreshSourceIsGesture),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = getCardBgColor(Color(0xFF0C1220)),
                    border = BorderStroke(1.dp, getAccentColor().copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = getAccentColor()
                                )
                            } else {
                                // Determinate progress ring mirroring the drag distance
                                val sweepFraction = (pullDistance / refreshThresholdPx).coerceIn(0f, 1f)
                                val trackColor = getTextColor(0.10f)
                                val progressColor = getAccentColor()
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                    drawCircle(
                                        color = trackColor,
                                        style = Stroke(width = 5f)
                                    )
                                    drawArc(
                                        color = progressColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * sweepFraction,
                                        useCenter = false,
                                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRefreshing) "درحال دریافت آخرین اطلاعات هواشناسی..." else "برای بروزرسانی ادامه دهید",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(),
                            fontFamily = Vazirmatn,
                            maxLines = 1
                        )
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen_content_scrollable"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isOffline) {
                    item(key = "offline_banner", contentType = "banner") {
                        OfflineBanner(offlineTime = offlineTime)
                    }
                }

                // Top Selected Mountain Title Card (Interactive Cockpit)
                item(key = "hero_card", contentType = "hero") {
                    MountainHeroCard(
                        mountain = mountain,
                        selectedAltitude = selectedAltitude,
                        lastUpdatedTime = lastUpdatedTime,
                        weather = weather,
                        pinnedCount = pinnedCount,
                        isPremium = isPremium,
                        isOffline = isOffline,
                        isRefreshing = isRefreshing,
                        onAltitudeSelected = { alt -> viewModel.setSelectedAltitude(alt) },
                        onChangeClick = onChangePeakClick,
                        onPinToggle = { handleTogglePin(mountain) },
                        onRefreshClick = { handleRefresh(false) },
                        onBillingTrigger = { viewModel.triggerBilling(true) }
                    )
                }

                // Climbing Safety Status Card (Reactively adjusts to selected altitude)
                item(key = "climbing_safety", contentType = "safety") {
                    weather.current?.let { rawCurrent ->
                        ClimbingSafetyCard(
                            viewModel = viewModel,
                            current = rawCurrent,
                            hourly = weather.hourly,
                            altitude = activeAltitude,
                            mountain = mountain,
                            minutely15 = weather.minutely15,
                            units = weather.hourlyUnits,
                            daily = weather.daily
                        )
                    }
                }

                // Current weather conditions card (detailed description, temperature)
                item(key = "current_weather", contentType = "weather") {
                    adjustedCurrent?.let { current ->
                        CurrentWeatherSection(
                            current = current,
                            hourly = weather.hourly,
                            daily = weather.daily,
                            altitude = activeAltitude,
                            mountain = mountain
                        )
                    }
                }

                // Mountaineering specialized statistics
                item(key = "mountaineering_stats", contentType = "stats") {
                    adjustedCurrent?.let { current ->
                        MountaineeringStatsSection(
                            current = current,
                            altitude = activeAltitude,
                            daily = weather.daily,
                            hourly = weather.hourly,
                            mountain = mountain,
                            apiUtcOffsetSeconds = weather.utcOffsetSeconds
                        )
                    }
                }

                // Horizontal Hourly Forecast (24 hrs)
                item(key = "hourly_forecast", contentType = "hourly") {
                    weather.hourly?.let { hourly ->
                        HourlyForecastSection(
                            hourly = hourly,
                            altitude = activeAltitude,
                            mountain = mountain,
                            daily = weather.daily
                        )
                    }
                }

                // Standing Golden Window Section (safe climbing windows for 3/7/16 days!)
                item(key = "golden_windows", contentType = "golden") {
                    GoldenWindowSection(
                        viewModel = viewModel,
                        hourly = weather.hourly,
                        daily = weather.daily,
                        altitude = activeAltitude,
                        mountain = mountain,
                        selectedDaysCount = selectedDaysCount,
                        onDaysCountChanged = { days -> viewModel.setSelectedDaysCount(days) }
                    )
                }

                // Interactive Multi-Range Daily Forecast
                item(key = "daily_forecast", contentType = "daily") {
                    weather.daily?.let { daily ->
                        DailyForecastSection(
                            viewModel = viewModel,
                            daily = daily,
                            hourly = weather.hourly,
                            altitude = activeAltitude,
                            mountain = mountain,
                            selectedDaysCount = selectedDaysCount,
                            onDaysCountChanged = { days -> viewModel.setSelectedDaysCount(days) },
                            units = weather.hourlyUnits
                        )
                    }
                }
            }
        }

        // Transient status toast — floats ABOVE the bottom navigation bar (never under it),
        // centered horizontally, with a soft fade/slide so it reads clearly without
        // covering any actionable content. Used for refresh + pin feedback.
        androidx.compose.animation.AnimatedVisibility(
            visible = refreshToast != null,
            enter = androidx.compose.animation.fadeIn() +
                    androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ),
            exit = androidx.compose.animation.fadeOut() +
                    androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 145.dp, start = 32.dp, end = 32.dp)
        ) {
            val toast = refreshToast
            val toastSuccess = toast?.first ?: true
            val toastColor = if (toastSuccess) {
                if (MaterialTheme.colorScheme.background.isDark) Color(0xFF00E676) else Color(0xFF2E7D32)
            } else {
                if (MaterialTheme.colorScheme.background.isDark) Color(0xFFFF5252) else Color(0xFFC62828)
            }
            Surface(
                shape = RoundedCornerShape(100),
                color = getCardBgColor(Color(0xFF101626)),
                border = BorderStroke(1.dp, toastColor.copy(alpha = 0.45f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (toastSuccess) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = toastColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = toast?.second ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTextColor(),
                        fontFamily = Vazirmatn,
                        maxLines = 1
                    )
                }
            }
        }

        // Snackbar retained ONLY for pin/unpin feedback (short messages, bottom position acceptable)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = if (MaterialTheme.colorScheme.background.isDark) Color(0xFF1E293B) else Color(0xFF0F172A),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ============================================================
// Golden Window (پنجره طلایی هوشمند)
// ============================================================

data class GoldenWindow(
    val startHour: Int,
    val endHour: Int,
    val startTime: String? = null,
    val endTime: String? = null,
    val durationHours: Int = 0,
    val durationMinutes: Int = 0,
    val startRawTime: String = "",   // برای تشخیص روز
    val endRawTime: String = ""      // برای تشخیص روز
) {
    val formattedDuration: String
        get() {
            val totalMins = if (durationMinutes > 0) durationMinutes else durationHours * 60
            return when {
                totalMins == 0 -> "۰ دقیقه"
                totalMins < 60 -> "${PersianDateHelper.formatToPersianDigits(totalMins)} دقیقه"
                totalMins % 60 == 0 -> "${PersianDateHelper.formatToPersianDigits(totalMins / 60)} ساعت"
                else -> "${PersianDateHelper.formatToPersianDigits(totalMins / 60)} ساعت و ${PersianDateHelper.formatToPersianDigits(totalMins % 60)} دقیقه"
            }
        }
}

fun findGoldenWindows(
    hourly: HourlyData?,
    altitude: Int,
    mountain: MountainEntity,
    startIdxParam: Int = 0,
    endIdxParam: Int = 24
): List<GoldenWindow> {
    if (hourly == null) return emptyList()

    val startIdx = startIdxParam.coerceAtLeast(0)
    val endIdx = endIdxParam.coerceAtMost(hourly.time.size)

    val diff = mountain.altitude - altitude

    val windows = mutableListOf<GoldenWindow>()
    var currentWindow: GoldenWindow? = null

    for (i in startIdx until endIdx) {
        val rawTemp = hourly.temperature2m.getOrNull(i) ?: 0.0
        val adjTemp = rawTemp + (diff * 0.0065)
        val rawWind = hourly.windSpeed10m?.getOrNull(i) ?: 0.0
        val rawWind80 = hourly.windSpeed80m?.getOrNull(i) ?: rawWind
        val adjWind = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = rawWind80,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = altitude.toDouble(),
            alpha = null
        )
        val isNight = hourly.isDay?.getOrNull(i) == 0
        val windChill = MountaineeringHelper.calculateWindChill(adjTemp, adjWind, isNight = isNight)
        val precipitation = hourly.precipitation?.getOrNull(i) ?: 0.0
        val visibility = hourly.visibility?.getOrNull(i) ?: -1.0

        val cape = hourly.cape?.getOrNull(i) ?: 0.0
        val cloudCover = hourly.cloudCover?.getOrNull(i) ?: 0.0
        val freezingLevel = hourly.freezingLevelHeight?.getOrNull(i) ?: 0.0
        val weatherCode = hourly.weatherCode.getOrNull(i) ?: 0

        val lightningRisk = MountaineeringHelper.calculateLightningRisk(
            cape = cape,
            precipitation = precipitation,
            cloudCover = cloudCover,
            freezingLevel = freezingLevel,
            summitElevation = altitude.toDouble(),
            weatherCode = weatherCode,
            lightningPotential = null
        )

        val precipProb = hourly.precipitationProbability?.getOrNull(i) ?: 0
        val windGusts = hourly.windGusts10m?.getOrNull(i) ?: (adjWind * MountaineeringHelper.calculateDynamicGustFactor(hourly.cape?.getOrNull(i)))
        val snowfall = MountaineeringHelper.normalizeSnowfallCm(hourly.snowfall?.getOrNull(i))

        val windRisk = MountaineeringHelper.calculateWindRisk(
            windSpeed = adjWind,
            windGusts = windGusts,
            altitude = altitude
        )
        
        // Dynamic safety thresholds strictly adjusted by summit/target altitude
        val maxWindLimit = if (altitude >= 3800) 28.0 else if (altitude >= 2800) 35.0 else 40.0
        val maxGustLimit = if (altitude >= 3800) 38.0 else if (altitude >= 2800) 45.0 else 50.0
        val minVisLimit = if (altitude >= 3800) 5000.0 else if (altitude >= 2800) 4000.0 else 3000.0
        val minWindChillLimit = if (altitude >= 3800) -25.0 else if (altitude >= 2800) -20.0 else -15.0

        // Zero-tolerance policy for lightning hazard in Golden Window
        val isLightningSafe = lightningRisk <= 5 && cape < 300.0

        // Professional criteria for Golden Window (ideal summit push) - Worst-case scan per hour
        val isSafe = isLightningSafe &&
                adjWind <= maxWindLimit &&
                windGusts <= maxGustLimit &&
                visibility >= minVisLimit &&
                windChill >= minWindChillLimit &&
                precipitation <= 0.05 &&   // Dry
                snowfall <= 0.05 &&        // No active snowfall
                precipProb <= 15 &&        // Low probability of precip
                (weatherCode in 0..3)      // Clear or cloudy only, no fog/rain/snow

        val timeString = hourly.time.getOrNull(i) ?: ""
        val hourText = try {
            val parts = timeString.split("T")
            if (parts.size > 1) {
                parts[1].substring(0, 5)
            } else {
                timeString
            }
        } catch (e: Exception) {
            ""
        }
        val formattedHourText = "\u200E" + PersianDateHelper.formatToPersianDigits(hourText)

        val nextTimeString = hourly.time.getOrNull(i + 1) ?: ""
        val nextHourText = try {
            val parts = nextTimeString.split("T")
            val currentHour = hourText.substringBefore(":").toIntOrNull() ?: 0
            if (currentHour == 23) {
                "24:00"
            } else if (parts.size > 1) {
                val nextHourStr = parts[1].substring(0, 5)
                if (nextHourStr == "00:00") "24:00" else nextHourStr
            } else {
                val nextHour = (currentHour + 1) % 24
                if (nextHour == 0) "24:00" else String.format(java.util.Locale.US, "%02d:00", nextHour)
            }
        } catch (e: Exception) {
            val currentHour = hourText.substringBefore(":").toIntOrNull() ?: 0
            val nextHour = (currentHour + 1) % 24
            if (nextHour == 0) "24:00" else String.format(java.util.Locale.US, "%02d:00", nextHour)
        }
        val formattedNextHourText = "\u200E" + PersianDateHelper.formatToPersianDigits(nextHourText)

        if (isSafe) {
            if (currentWindow == null) {
                currentWindow = GoldenWindow(
                    startHour = i,
                    endHour = i,
                    startTime = formattedHourText,
                    endTime = formattedNextHourText,
                    durationHours = 1,
                    durationMinutes = 60,
                    startRawTime = timeString,   // افزودن فیلد جدید برای تشخیص روز
                    endRawTime = timeString      // افزودن فیلد جدید برای تشخیص روز
                )
            } else {
                currentWindow = currentWindow.copy(
                    endHour = i,
                    endTime = formattedNextHourText,
                    durationHours = currentWindow.durationHours + 1,
                    durationMinutes = (currentWindow.durationHours + 1) * 60,
                    endRawTime = timeString
                )
            }
        } else {
            if (currentWindow != null) {
                windows.add(currentWindow)
                currentWindow = null
            }
        }
    }

    if (currentWindow != null) {
        windows.add(currentWindow)
    }

    return windows
}

fun getActiveHoursForDate(
    targetDate: String, // "yyyy-MM-dd"
    startRaw: String,   // "yyyy-MM-ddTHH:mm"
    endRaw: String      // "yyyy-MM-ddTHH:mm"
): Pair<Int, Int>? { // returns Pair(startHour, endHour) in 0..23 range, or null if no overlap
    try {
        val sDate = startRaw.substringBefore("T")
        val sHour = startRaw.substringAfter("T").substringBefore(":").toInt()
        val eDate = endRaw.substringBefore("T")
        val eHour = endRaw.substringAfter("T").substringBefore(":").toInt()
        
        if (targetDate < sDate || targetDate > eDate) {
            return null // No overlap with this calendar day
        }
        
        val hStart = if (targetDate == sDate) sHour else 0
        val hEnd = if (targetDate == eDate) eHour else 23
        
        return Pair(hStart, hEnd)
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun DayTimelineBar(
    windows: List<GoldenWindow>,
    statusColor: Color,
    currentHour: Float = -1f,
    targetDate: String = "",
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val trackBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
    val accentColor = getAccentColor()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    
    Canvas(modifier = modifier.height(10.dp)) {
        val width = size.width
        val height = size.height
        val strokeWidth = height
        
        // Draw background track
        drawLine(
            color = trackBg,
            start = Offset(strokeWidth / 2, height / 2),
            end = Offset(width - strokeWidth / 2, height / 2),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Draw each golden window segment
        windows.forEach { window ->
            val activeHours = if (targetDate.isNotEmpty() && window.startRawTime.isNotEmpty() && window.endRawTime.isNotEmpty()) {
                getActiveHoursForDate(targetDate, window.startRawTime, window.endRawTime)
            } else {
                val hStart = try {
                    window.startRawTime.substringAfter("T").substringBefore(":").toInt()
                } catch (e: Exception) {
                    0
                }
                val hEnd = try {
                    window.endRawTime.substringAfter("T").substringBefore(":").toInt()
                } catch (e: Exception) {
                    0
                }
                Pair(hStart, hEnd)
            }
            
            if (activeHours != null) {
                val (hStart, hEnd) = activeHours
                
                // Map 0..24 range to coordinates
                val startRatio = hStart / 24f
                val endRatio = (hEnd + 1).coerceAtMost(24) / 24f
                
                val rawStartX = if (isRtl) (1f - endRatio) * width else startRatio * width
                val rawEndX = if (isRtl) (1f - startRatio) * width else endRatio * width
                
                val startClamped = rawStartX.coerceIn(strokeWidth / 2, width - strokeWidth / 2)
                val endClamped = rawEndX.coerceIn(strokeWidth / 2, width - strokeWidth / 2)
                
                if (endClamped > startClamped) {
                    drawLine(
                        color = statusColor,
                        start = Offset(startClamped, height / 2),
                        end = Offset(endClamped, height / 2),
                        strokeWidth = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Draw current hour indicator (glowing circular pin indicator on the timeline)
        if (currentHour >= 0f) {
            val currentRatio = (currentHour / 24f).coerceIn(0f, 1f)
            val currentX = if (isRtl) (1f - currentRatio) * width else currentRatio * width
            val clampedX = currentX.coerceIn(strokeWidth / 2, width - strokeWidth / 2)
            
            // Glowing outer ring
            drawCircle(
                color = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f),
                radius = 6.dp.toPx(),
                center = Offset(clampedX, height / 2)
            )
            // Inner core background
            drawCircle(
                color = if (isDark) Color(0xFF0C1118) else Color.White,
                radius = 4.dp.toPx(),
                center = Offset(clampedX, height / 2)
            )
            // Center indicator dot
            drawCircle(
                color = accentColor,
                radius = 2.dp.toPx(),
                center = Offset(clampedX, height / 2)
            )
        }
    }
}

@Composable
fun DayGoldenSummary(
    dayLabel: String,
    windows: List<GoldenWindow>,
    isToday: Boolean = false,
    isBestDay: Boolean = false,
    currentHour: Float = -1f,
    dateSubtext: String? = null,
    dateStr: String = ""
) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalSafeHours = windows.sumOf { it.durationHours }
    val isDark = MaterialTheme.colorScheme.background.isDark
    
    val statusColor = when {
        totalSafeHours >= 8 -> if (isDark) Color(0xFF00FF87) else Color(0xFF2E7D32)
        totalSafeHours >= 4 -> if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17)
        else -> if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
    }

    val animatedBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isExpanded) {
            if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
        } else {
            Color.Transparent
        },
        label = "summary_card_bg"
    )

    val rotationAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBgColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { isExpanded = !isExpanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Day Label Column (Reduced from 64.dp to 54.dp to maximize timeline width)
            Column(
                modifier = Modifier.width(54.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayLabel,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = getTextColor()
                    )
                    if (isBestDay) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "بهترین روز",
                            tint = if (isDark) Color(0xFFFFD700) else Color(0xFFD97706),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                if (isToday) {
                    val timeSubtext = if (currentHour >= 0f) {
                        val hr = currentHour.toInt()
                        val mn = ((currentHour % 1f) * 60f).toInt().coerceIn(0, 59)
                        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", hr, mn)
                        "ساعت \u200E${PersianDateHelper.formatToPersianDigits(timeStr)}"
                    } else {
                        "امروز"
                    }
                    Text(
                        text = timeSubtext,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = getTextColor(0.5f)
                    )
                } else {
                    val subtext = dateSubtext ?: if (isBestDay) "بهترین روز" else null
                    if (subtext != null) {
                        Text(
                            text = subtext,
                            fontSize = 8.sp,
                            fontWeight = if (isBestDay) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBestDay) (if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)) else getTextColor(0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // 2. Timeline Bar in the center (Forced LTR for consistent progress visualization)
            // Weight(1f) automatically grabs all available extra horizontal space, making it much wider/longer
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    DayTimelineBar(
                        windows = windows,
                        statusColor = statusColor,
                        currentHour = if (isToday) currentHour else -1f,
                        targetDate = dateStr,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "\u200E۰۰:۰۰",
                            fontSize = 7.5.sp,
                            color = getTextColor(0.35f)
                        )
                        Text(text = "\u200E۱۲:۰۰", fontSize = 7.5.sp, color = getTextColor(0.35f))
                        Text(
                            text = "\u200E۲۴:۰۰",
                            fontSize = 7.5.sp,
                            color = getTextColor(0.35f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // 3. Info Column on the right (Reduced from 110.dp to 84.dp to maximize timeline width)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(84.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val safeHoursText = if (totalSafeHours > 0) {
                        "\u200E${PersianDateHelper.formatToPersianDigits(totalSafeHours)} ساعت"
                    } else {
                        "غیر ایمن"
                    }
                    Text(
                        text = safeHoursText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "نمایش جزئیات",
                        tint = statusColor,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
        }

        // Expanded details (interactive tooltip)
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "🔍 جزئیات بازه صعود ایمن:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = getAccentColor()
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                if (windows.isEmpty()) {
                    Text(
                        text = "⚠️ هیچ بازه ایمنی در این روز یافت نشد. شرایط جوی قله شامل سرعت باد، صاعقه یا دید افقی در وضعیت بحرانی قرار دارد. صعود به هیچ عنوان توصیه نمی‌شود.",
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                        color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB91C1C)
                    )
                } else {
                    windows.forEach { window ->
                        val startDay = window.startRawTime.substringBefore("T")
                        val endDay = window.endRawTime.substringBefore("T")
                        val isSameDay = startDay == endDay
                        val daySuffix = if (isSameDay) "" else " (روز بعد)"
                        
                        Text(
                            text = "🔹 از ساعت \u200E${window.startTime} تا \u200E${window.endTime}$daySuffix (\u200E${window.formattedDuration} مداوم)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = getTextColor()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val tipText = when {
                        totalSafeHours >= 8 -> "🟢 شرایط فوق‌العاده پایدار: پنجره طولانی‌مدت برای برنامه‌ریزی صعودهای سنگین و هم‌هوایی بدون عجله بسیار مناسب است."
                        totalSafeHours >= 4 -> "🟡 پنجره صعود محدود: شرایط جوی فقط برای چند ساعت پایدار است. پیشنهاد می‌شود به صعود سبکبار (Fast & Light) و کوتاه بسنده کنید."
                        else -> "🟠 شرایط بسیار حساس: بازه زمانی صعود به شدت کوتاه است. فقط برای جابه‌جایی‌های جزئی بین کمپ‌ها یا کوهپیمایی ارتفاع پایین مناسب است."
                    }
                    
                    Text(
                        text = tipText,
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                        color = getTextColor(0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 جهت بستن جزئیات، دوباره روی کارت ضربه بزنید.",
                    fontSize = 8.sp,
                    color = getTextColor(0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MountainHeroCard(
    mountain: MountainEntity,
    selectedAltitude: Int?,
    lastUpdatedTime: String,
    weather: WeatherResponse,
    pinnedCount: Int,
    isPremium: Boolean,
    isOffline: Boolean = false,
    isRefreshing: Boolean = false,
    onAltitudeSelected: (Int) -> Unit,
    onChangeClick: () -> Unit,
    onPinToggle: () -> Unit,
    onRefreshClick: () -> Unit,
    onBillingTrigger: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "hero_refresh_anim")
    val rotationAngle by if (isRefreshing) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val favoriteScale by animateFloatAsState(
        targetValue = if (mountain.isPinned) 1.25f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favorite_scale"
    )

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(mountain) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            tick++
        }
    }
    val peakLocalTimeText = remember(mountain, tick, weather.utcOffsetSeconds) {
        try {
            // Prefer the DST-aware offset from the Open-Meteo API response; fall back to standard-time estimate.
            val peakOffsetHours = com.example.ui.util.AstronomicalCalculator.resolvePeakOffset(
                apiUtcOffsetSeconds = weather.utcOffsetSeconds,
                name = mountain.name,
                latitude = mountain.latitude,
                longitude = mountain.longitude
            )
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            val currentUTC = cal.get(java.util.Calendar.HOUR_OF_DAY) + cal.get(java.util.Calendar.MINUTE) / 60.0
            val localHour = (currentUTC + peakOffsetHours + 24.0) % 24.0
            val formatted = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localHour)
            PersianDateHelper.formatToPersianDigits(formatted)
        } catch (e: Exception) {
            "خطا در محاسبه ساعت"
        }
    }
    val activeAltitude = selectedAltitude ?: mountain.altitude
    val current = weather.current
    val peakTemp = current?.temperature2m ?: 0.0
    val rawWind10m = current?.windSpeed10m ?: 0.0
    val rawWind80m = current?.windSpeed80m ?: rawWind10m
    // Open-Meteo hourly.time starts at 00:00 today (timezone=auto), so firstOrNull() would be
    // midnight's wind — NOT the current hour. Per the array-alignment rule (hourly.time[i] ↔
    // hourly.windSpeed10m[i]), resolve the index that matches current.time first.
    val hourlyWindFallback = remember(weather.current?.time, weather.hourly) {
        val hourly = weather.hourly
        val cur = weather.current
        val windArr = hourly?.windSpeed10m
        if (cur != null && windArr != null && hourly.time.isNotEmpty()) {
            val idx = MountaineeringHelper.findHourlyIndexForCurrent(current = cur, hourly = hourly)
            if (idx in windArr.indices) (windArr.getOrNull(idx) ?: 0.0) else 0.0
        } else {
            0.0
        }
    }
    val baseWind10m = if (rawWind10m > 0.0) rawWind10m else if (hourlyWindFallback > 0.0) hourlyWindFallback else 12.0
    val peakWind80m = maxOf(rawWind80m, baseWind10m)
    val baseGusts10m = current?.windGusts10m ?: (baseWind10m * MountaineeringHelper.calculateDynamicGustFactor(current?.cape))

    // 4. Nowcast vs Forecast Badge (Validation of observational timestamp vs current time)
    // current.time is in the PEAK'S LOCAL time (timezone=auto). Convert it to a UTC instant using
    // the response's utc_offset_seconds before comparing with the device's UTC wall clock.
    val isNowcastLive = remember(weather.current?.time, lastUpdatedTime, tick) {
        val curTimeStr = weather.current?.time
        if (curTimeStr.isNullOrEmpty()) {
            true
        } else {
            try {
                val localDateTime = java.time.LocalDateTime.parse(
                    curTimeStr,
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                )
                val utcOffset = java.time.ZoneOffset.ofTotalSeconds(weather.utcOffsetSeconds ?: 0)
                val instant = localDateTime.toInstant(utcOffset)
                val diffMinutes = Math.abs(System.currentTimeMillis() - instant.toEpochMilli()) / (1000 * 60)
                diffMinutes <= 35
            } catch (e: Exception) {
                true
            }
        }
    }

    val freezingLevelHeight: Double = remember(current, mountain.altitude, peakTemp) {
        if (current?.freezingLevelHeight != null) {
            current.freezingLevelHeight.toDouble()
        } else {
            MountaineeringHelper.estimateFreezingLevel(
                temp = peakTemp,
                baseAltitude = mountain.altitude
            ).toDouble()
        }
    }

    // 1 & 2. Linear Altitude Interpolation with Worst-Case Safety Locking
    // Environmental Lapse Rate (0.0065°C/m) + Conservative Hypothermia Protection Lock
    val activeTemp = remember(mountain.altitude, activeAltitude, peakTemp) {
        val activeDiff = mountain.altitude - activeAltitude
        val interpolatedTemp = peakTemp + (activeDiff * 0.0065)
        if (activeAltitude > mountain.altitude) {
            minOf(interpolatedTemp, peakTemp)
        } else {
            interpolatedTemp
        }
    }

    // Power Law Wind Scaling with Worst-Case Safety Lock (Always pick maximum wind for safety)
    val activeWind = remember(peakWind80m, baseWind10m, mountain.altitude, activeAltitude) {
        val calculatedWind = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = peakWind80m,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = activeAltitude.toDouble(),
            alpha = null
        )
        maxOf(calculatedWind, baseWind10m)
    }

    val activeGusts = remember(baseGusts10m, current?.cape, mountain.altitude, activeAltitude, activeWind) {
        // Power-law altitude scaling from the summit gust — same model as the wind,
        // so gusts DO decrease on the lower slopes instead of staying pinned to the summit value.
        val calculatedGusts = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = baseGusts10m,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = activeAltitude.toDouble(),
            alpha = null
        )
        // No summit-value floor here. Only enforce the physical invariant that a gust is
        // never weaker than the CAPE-scaled local wind (which itself already varies by altitude).
        maxOf(calculatedGusts, activeWind * MountaineeringHelper.calculateDynamicGustFactor(current?.cape))
    }

    val activePressure = remember(current, mountain.altitude, activeAltitude, activeTemp) {
        if (current != null) {
            val qnhVal = current.pressureMsl ?: 1013.25
            MountaineeringHelper.calculateBarometricPressure(
                basePressure = current.surfacePressure,
                baseTemp = current.temperature2m,
                baseAltitude = mountain.altitude,
                targetAltitude = activeAltitude,
                targetTemp = activeTemp,
                qnh = qnhVal
            )
        } else {
            1013.25 * Math.pow(1.0 - 0.0000225577 * activeAltitude, 5.25588)
        }
    }

    val activeHumidity = remember(current, mountain.altitude, activeAltitude, activeTemp, activePressure) {
        val baseHumidity = (current?.relativeHumidity2m ?: 50.0).toDouble()
        val qnhVal = current?.pressureMsl ?: 1013.25
        val basePressure = current?.surfacePressure ?: MountaineeringHelper.calculateBarometricPressure(null, peakTemp, mountain.altitude, mountain.altitude, qnh = qnhVal)
        MountaineeringHelper.adjustHumidityWithAltitude(
            baseHumidity = baseHumidity,
            baseTemp = peakTemp,
            basePressure = basePressure,
            targetTemp = activeTemp,
            targetPressure = activePressure
        )
    }

    // 6. RealFeel Composite (Wind Chill for cold, Heat Index for hot, Dry Bulb otherwise)
    val activeApparent = remember(activeTemp, activeWind, activeHumidity) {
        MountaineeringHelper.calculateApparentTemperature(
            temp = activeTemp,
            windSpeed = activeWind,
            humidity = activeHumidity,
            isNight = false
        )
    }

    val activeOxygenPct = remember(activePressure) {
        (activePressure / 1013.25) * 100.0
    }

    // 5. Rapid Change Safety Threshold Alert
    val isHazardousLevel = activeWind >= 38.0 || activeGusts >= 55.0 || activeApparent <= -15.0 || activeTemp <= -20.0

    // Dynamic Freezing Level Assessment
    val freezingSummary = remember(freezingLevelHeight, mountain.altitude) {
        val fLevel = freezingLevelHeight.toInt()
        when {
            fLevel >= mountain.altitude -> {
                "تراز صفر درجه بالاتر از قله صعود است (مسیر صعود کاملاً بالای صفر درجه و عاری از یخبندان سطحی اتمسفری)"
            }
            fLevel <= 1000 -> {
                "کل مخروط کوهستان زیر صفر درجه قرار دارد (یخبندان یکپارچه از کوهپایه تا قله)"
            }
            else -> {
                "خط صفر درجه (تراز انجماد): ارتفاع ${PersianDateHelper.formatToPersianDigits(fLevel)} متر (از این ارتفاع به بالا جبهه‌های صخره‌ای مستعد انجماد و برف منجمد هستند)"
            }
        }
    }

    // Generate altitudes in descending order: start from peak/summit altitude down to 1000m
    val altitudeSteps = remember(mountain.altitude) {
        val steps = mutableListOf<Int>()
        var currentStep = 1000
        while (currentStep < mountain.altitude) {
            steps.add(currentStep)
            currentStep += 500
        }
        if (mountain.altitude > 1000) {
            if (steps.isEmpty() || steps.last() != mountain.altitude) {
                steps.add(mountain.altitude)
            }
        } else {
            steps.add(mountain.altitude)
        }
        steps.reversed()
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mountain_hero_card"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    getAccentColor().copy(alpha = 0.50f),
                    Color(0xFF4A00E0).copy(alpha = 0.15f),
                    getAccentColor().copy(alpha = 0.50f)
                )
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor = getCardBgColor(Color(0xFF0A0E17))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color(0xFF141924),
                            Color(0xFF090C12)
                        ) else listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row: Tech Badge & GPS coordinates & Heart Favorite Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(100))
                                .background(getAccentColor())
                        )
                        Text(
                            text = "دیدبان دیجیتال صعود",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = getAccentColor(),
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 4. Nowcast vs Forecast Badge
                        if (isNowcastLive) {
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = (if (isDark) Color(0xFF00E676) else Color(0xFF15803D)).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, (if (isDark) Color(0xFF00E676) else Color(0xFF15803D)).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(100))
                                            .background(if (isDark) Color(0xFF00E676) else Color(0xFF15803D))
                                    )
                                    Text(
                                        text = "مشاهدات زنده",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF00E676) else Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            val timeString = weather.current?.time?.split("T")?.getOrNull(1)?.take(5) ?: ""
                            val pTime = if (timeString.isNotEmpty()) PersianDateHelper.formatToPersianDigits(timeString) else ""
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = (if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, (if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(100))
                                            .background(if (isDark) Color(0xFFFFB300) else Color(0xFFB45309))
                                    )
                                    Text(
                                        text = if (pTime.isNotEmpty()) "پیش‌بینی ($pTime)" else "پیش‌بینی",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFB300) else Color(0xFFB45309),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // ساعت محلی قله (پیل مینیمال و زنده)
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = getAccentColor().copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, getAccentColor().copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Peak Local Time",
                                    tint = getAccentColor(),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ساعت قله: $peakLocalTimeText",
                                    fontSize = 10.sp,
                                    fontFamily = Vazirmatn,
                                    color = getTextColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Prominent Mountain name and Header Action Row (Refresh + Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mountain.persianName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = getTextColor(),
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Manual Refresh Button (دکمه بروزرسانی دستی)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getAccentColor().copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, getAccentColor().copy(alpha = 0.28f)),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = !isRefreshing,
                                modifier = Modifier
                                    .testTag("refresh_button")
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "بروزرسانی دستی هواشناسی",
                                    tint = getAccentColor(),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }

                        // Add to Favorites Button (دکمه افزودن به علاقه‌مندی‌ها)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (mountain.isPinned) Color(0xFFFF5252).copy(alpha = 0.15f) else getTextColor(0.05f),
                            border = BorderStroke(1.dp, if (mountain.isPinned) Color(0xFFFF5252).copy(alpha = 0.40f) else getTextColor(0.12f)),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            IconButton(
                                onClick = onPinToggle,
                                modifier = Modifier
                                    .testTag("favorite_button")
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (mountain.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "افزودن به علاقه‌مندی‌ها",
                                    tint = if (mountain.isPinned) Color(0xFFFF5252) else getTextColor(0.70f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(favoriteScale)
                                )
                            }
                        }
                    }
                }
                
                // Dynamic Live Weather Subtitle (Replaces duplicate weather parameters with sleek height profile indicator)
                val activeZoneLabel = remember(activeAltitude, mountain.altitude) {
                    if (activeAltitude == mountain.altitude) "قله صعود"
                    else {
                        val zRatio = activeAltitude.toDouble() / mountain.altitude
                        when {
                            zRatio <= 0.35 -> "نزدیک به کوهپایه"
                            zRatio <= 0.62 -> "دامنه اصلی صعود"
                            zRatio <= 0.85 -> "یال منتهی به خط‌الراس"
                            else -> "شانه و چندقدمی قله"
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Active altitude radar monitoring",
                        tint = getAccentColor(),
                        modifier = Modifier.size(14.dp)
                    )
                    
                    Text(
                        text = "جبهه صعود تحت ارزیابی: ${PersianDateHelper.formatToPersianDigits(activeAltitude)} متر (${activeZoneLabel})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTextColor(0.85f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location and Range info inside custom cyberpunk minimalist chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Province Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = getTextColor(0.03f),
                            border = BorderStroke(0.6.dp, getTextColor(0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Province",
                                    modifier = Modifier.size(12.dp),
                                    tint = getAccentColor()
                                )
                                Text(
                                    text = mountain.persianProvince,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(0.85f)
                                )
                            }
                        }
                        
                        // Range Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = getTextColor(0.03f),
                            border = BorderStroke(0.6.dp, getTextColor(0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterHdr,
                                    contentDescription = "Range",
                                    modifier = Modifier.size(12.dp),
                                    tint = getAccentColor()
                                )
                                Text(
                                    text = mountain.range,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(0.85f)
                                )
                            }
                        }
                    }

                    // Luxury Cyan Altitude Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        getAccentColor().copy(alpha = 0.12f),
                                        getAccentColor().copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .border(1.dp, getAccentColor().copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFFFFD54F)) // golden summit dot
                            )
                            Text(
                                text = "${PersianDateHelper.formatToPersianDigits(mountain.altitude)} متر",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RealFeel Composite Box for Selected Active Altitude
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isDark) Color(0xFF0F1420) else Color(0xFFF1F5F9)
                        )
                        .border(
                            1.dp,
                            if (isHazardousLevel) {
                                if (isDark) Color(0xFFFF5252).copy(alpha = 0.6f) else Color(0xFFB91C1C).copy(alpha = 0.6f)
                            } else {
                                getAccentColor().copy(alpha = 0.25f)
                            },
                            RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    // 5. Rapid Hazard Alert Banner inside Hero Card if threshold crossed
                    if (isHazardousLevel) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rapidHazardPulse")
                        val alertAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.45f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 900, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alertAlpha"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)).copy(alpha = 0.15f * alertAlpha)
                                )
                                .border(
                                    1.dp,
                                    (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)).copy(alpha = alertAlpha),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Rapid Hazard Alert",
                                tint = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                                modifier = Modifier.size(16.dp)
                            )
                            val absAppAlert = kotlin.math.abs(activeApparent)
                            val pAppAlertFormatted = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absAppAlert))
                            val appAlertSign = if (activeApparent < -0.05) "−" else if (activeApparent > 0.05) "+" else ""
                            val pWindAlert = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", activeWind))
                            Text(
                                text = "⚠️ منطقه پرخطر در ${PersianDateHelper.formatToPersianDigits(activeAltitude)} متر (باد: $pWindAlert ک‌م/س | احساسی: \u200E$appAlertSign$pAppAlertFormatted°C)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB91C1C),
                                lineHeight = 15.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 6. Main Prominent RealFeel Temperature Readout (centered) + 3-column metric tiles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val hasWindChillEffect = activeTemp <= 10.0 && activeWind >= 4.8 && (activeTemp - activeApparent) >= 0.2
                        val hasHeatIndexEffect = activeTemp >= 27.0 && (activeApparent - activeTemp) >= 0.2

                        val mainDisplayTemp = if (hasWindChillEffect || hasHeatIndexEffect) activeApparent else activeTemp
                        val absMainTemp = kotlin.math.abs(mainDisplayTemp)
                        val pMainTempFormatted = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absMainTemp))
                        val mainSign = if (mainDisplayTemp < -0.05) "−" else if (mainDisplayTemp > 0.05) "+" else ""

                        val mainLabel = when {
                            hasWindChillEffect -> "دمای احساسی (شاخص سوزباد)"
                            hasHeatIndexEffect -> "شاخص گرما"
                            else -> "دمای واقعی هوا"
                        }

                        Text(
                            text = mainLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.6f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "\u200E$mainSign$pMainTempFormatted°C",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = if (mainDisplayTemp <= 0.0) (if (isDark) Color(0xFF00E5FF) else Color(0xFF00838F)) else (if (isDark) Color(0xFF00FF87) else Color(0xFF15803D))
                            )

                            if (hasWindChillEffect || hasHeatIndexEffect) {
                                val absDry = kotlin.math.abs(activeTemp)
                                val pDryFormatted = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absDry))
                                val drySign = if (activeTemp < -0.05) "−" else if (activeTemp > 0.05) "+" else ""
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = getTextColor(0.05f),
                                    border = BorderStroke(0.6.dp, getTextColor(0.10f))
                                ) {
                                    Text(
                                        text = "واقعی: \u200E$drySign$pDryFormatted°C",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getTextColor(0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            } else if (activeTemp <= 10.0 && activeWind < 4.8) {
                                Text(
                                    text = "(بدون اثر سوزباد)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = getTextColor(0.45f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Three equal-width metric tiles: Wind / Gusts / Oxygen
                        val pWind = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", activeWind))
                        val pGusts = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", activeGusts))
                        val pOxy = PersianDateHelper.formatToPersianDigits(activeOxygenPct.toInt())

                        val windTileColor = if (activeWind > 35.0) (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)) else getAccentColor()
                        val gustTileColor = if (activeGusts > 45.0) (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)) else getAccentColor()
                        val oxyTileColor = when {
                            activeOxygenPct < 60.0 -> if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                            activeOxygenPct < 75.0 -> if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)
                            else -> if (isDark) Color(0xFF00E676) else Color(0xFF2E7D32)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wind tile
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = windTileColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                                border = BorderStroke(0.8.dp, windTileColor.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Air,
                                            contentDescription = "Wind",
                                            tint = windTileColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "باد",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getTextColor(0.55f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$pWind ک‌م/س",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = windTileColor,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Gusts tile
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = gustTileColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                                border = BorderStroke(0.8.dp, gustTileColor.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Thunderstorm,
                                            contentDescription = "Gusts",
                                            tint = gustTileColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تندباد",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getTextColor(0.55f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$pGusts ک‌م/س",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = gustTileColor,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Oxygen tile
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = oxyTileColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                                border = BorderStroke(0.8.dp, oxyTileColor.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "🫁",
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اکسیژن",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getTextColor(0.55f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$pOxy٪",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = oxyTileColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                
                // Cyberpunk styled thin divider line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(getAccentColor())
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        getAccentColor().copy(alpha = 0.3f),
                                        getTextColor(0.04f)
                                    )
                                )
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Linear Elevation Profiler Panel
                val minGaugeElevation = if (mountain.altitude > 1000) 1000 else 500
                val maxGaugeElevation = mountain.altitude.coerceAtLeast(minGaugeElevation + 1)
                val rangeGauge = (maxGaugeElevation - minGaugeElevation).coerceAtLeast(1)
                val markerRatio = ((activeAltitude - minGaugeElevation).coerceAtLeast(0) / rangeGauge.toDouble()).coerceIn(0.0, 1.0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF111622).copy(alpha = 0.5f) else Color(0xFFF1F5F9).copy(alpha = 0.6f))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = getAccentColor(),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "نیم‌رخ ارتفاعی صعود:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.85f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(getAccentColor().copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "سنجش جاری: ${PersianDateHelper.formatToPersianDigits(activeAltitude)} متر",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = getAccentColor()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // The Sleek Custom Track Gauge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF090D14) else Color(0xFFE2E8F0))
                            .border(0.5.dp, getTextColor(0.08f), RoundedCornerShape(6.dp))
                    ) {
                        // Background gradient for freeze zone (from freezingLevelHeight upwards)
                        if (freezingLevelHeight < maxGaugeElevation) {
                            val freezeStartRatio = ((freezingLevelHeight - minGaugeElevation).coerceAtLeast(0.0) / rangeGauge.toDouble()).coerceIn(0.0, 1.0)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(1f - freezeStartRatio.toFloat())
                                    .align(Alignment.CenterEnd)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF).copy(alpha = if (isDark) 0.25f else 0.35f),
                                                Color(0xFF02AABD).copy(alpha = if (isDark) 0.05f else 0.1f)
                                            )
                                        )
                                    )
                            )
                        }
                        
                        // Active progress fill up to activeAltitude
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(markerRatio.toFloat())
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4A00E0).copy(alpha = 0.6f),
                                            getAccentColor().copy(alpha = 0.9f)
                                        )
                                    )
                                )
                        )
                        
                        // Freezing level tick line mark on the bar
                        if (freezingLevelHeight in (minGaugeElevation.toDouble()..maxGaugeElevation.toDouble())) {
                            val freezeRatio = ((freezingLevelHeight - minGaugeElevation) / rangeGauge.toDouble()).coerceIn(0.0, 1.0)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(freezeRatio.toFloat())
                                    .align(Alignment.CenterStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.5.dp)
                                        .background(if (isDark) Color(0xFF00FFE0) else Color(0xFF0891B2))
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Space-paired legend beneath the track bar to prevent overlap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کوهپایه (${PersianDateHelper.formatToPersianDigits(minGaugeElevation)} متر)",
                            fontSize = 8.5.sp,
                            color = getTextColor(0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "🏔️ قله (${PersianDateHelper.formatToPersianDigits(maxGaugeElevation)} متر)",
                            fontSize = 8.5.sp,
                            color = getTextColor(0.85f),
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Freezing summary text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = if (isDark) 0.15f else 0.02f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🚨",
                            fontSize = 11.sp
                        )
                        Text(
                            text = freezingSummary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.55f),
                            lineHeight = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Interactive Altitude climbing detailed radar list
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Radar Icon",
                        tint = getAccentColor(),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رادار پله‌های ارتفاعی:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = getTextColor(),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("altitude_chips_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(altitudeSteps, key = { it }) { step ->
                        val isSelected = step == activeAltitude
                        val isSummit = step == mountain.altitude

                        // Calculate temperature at this step
                        val diff = mountain.altitude - step
                        val tempAtStep = peakTemp + (diff * 0.0065)

                        // ✅ Wind with Power Law (same worst-case floor as the hero readout for consistency)
                        val estWindAtStep = maxOf(
                            MountaineeringHelper.adjustWindWithAltitude(
                                referenceWind = peakWind80m,
                                referenceElevation = mountain.altitude.toDouble(),
                                targetAltitude = step.toDouble(),
                                alpha = null
                            ),
                            baseWind10m
                        )

                        val cur = weather.current
                        val qnhVal = cur?.pressureMsl ?: 1013.25
                        val estPressureAtStep = if (cur != null) {
                            MountaineeringHelper.calculateBarometricPressure(
                                basePressure = cur.surfacePressure,
                                baseTemp = cur.temperature2m,
                                baseAltitude = mountain.altitude,
                                targetAltitude = step,
                                targetTemp = tempAtStep,
                                qnh = qnhVal
                            )
                        } else {
                            1013.25 * Math.pow(1.0 - 0.0000225577 * step, 5.25588)
                        }

                        val estHumidityAtStep = MountaineeringHelper.adjustHumidityWithAltitude(
                            baseHumidity = (cur?.relativeHumidity2m ?: 50.0).toDouble(),
                            baseTemp = peakTemp,
                            basePressure = cur?.surfacePressure ?: MountaineeringHelper.calculateBarometricPressure(null, peakTemp, mountain.altitude, mountain.altitude, qnh = qnhVal),
                            targetTemp = tempAtStep,
                            targetPressure = estPressureAtStep
                        )

                        // Apparent temperature (Windchill / Heat Index adjusted)
                        val estApparentAtStep = MountaineeringHelper.calculateApparentTemperature(
                            temp = tempAtStep,
                            windSpeed = estWindAtStep,
                            humidity = estHumidityAtStep,
                            isNight = false
                        )

                        // Subzero frozen status
                        val isStepFrozen = tempAtStep <= 0.0 || step >= freezingLevelHeight

                        // Dynamic local safety assessment of this altitude step
                        val stepSafety = remember(weather.current, weather.hourly, weather.daily, step, mountain, weather.minutely15) {
                            val cur = weather.current
                            if (cur != null) {
                                val stepCurrent = MountaineeringHelper.createAdjustedCurrentWeatherForAltitude(
                                    cur = cur,
                                    hourly = weather.hourly,
                                    mountainAltitude = mountain.altitude,
                                    targetAltitude = step,
                                    mountainName = mountain.name,
                                    lat = mountain.latitude,
                                    lon = mountain.longitude
                                )
                                val offsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
                                MountaineeringHelper.evaluateSafety(
                                    current = stepCurrent,
                                    hourly = weather.hourly,
                                    daily = weather.daily,
                                    altitudeOverride = step,
                                    slopeAngle = mountain.slopeAngle,
                                    aspect = mountain.aspect,
                                    offsetHours = offsetHours,
                                    minutely15 = weather.minutely15,
                                    summitElevation = mountain.altitude.toDouble(),
                                    baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
                                    units = weather.hourlyUnits,
                                    latitude = mountain.latitude,
                                    longitude = mountain.longitude
                                )
                            } else {
                                null
                            }
                        }

                        val stepSafetyColor = remember(stepSafety, isDark) {
                            when (stepSafety?.status) {
                                SafetyStatus.RED -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                                SafetyStatus.YELLOW -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
                                SafetyStatus.GREEN -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                                null -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                            }
                        }

                        val isStepLocked = !isPremium && !isSummit
                        val premiumGoldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)

                        val frozenColor = if (isDark) Color(0xFF00E5FF) else Color(0xFF006064)

                        // ✅ Card Border & Background ALWAYS reflect true weather safety color (stepSafetyColor)
                        val stepCardBorder = if (isSelected) {
                            BorderStroke(2.2.dp, stepSafetyColor)
                        } else {
                            BorderStroke(1.2.dp, stepSafetyColor.copy(alpha = if (isDark) 0.35f else 0.45f))
                        }

                        val stepCardBg = if (isSelected) {
                            stepSafetyColor.copy(alpha = if (isDark) 0.35f else 0.25f)
                        } else {
                            stepSafetyColor.copy(alpha = if (isDark) 0.10f else 0.05f)
                        }

                        // Oxygen density using actual barometric pressure
                        val oxygenPct = (estPressureAtStep / 1013.25) * 100

                        Card(
                            onClick = {
                                if (isStepLocked) {
                                    onBillingTrigger()
                                } else {
                                    onAltitudeSelected(step)
                                }
                            },
                            modifier = Modifier
                                .width(135.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = stepCardBorder,
                            colors = CardDefaults.cardColors(containerColor = stepCardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSummit) Icons.Default.FilterHdr else Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = if (isSummit) "Summit Peak" else "Climbing Stage",
                                        tint = stepSafetyColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val zoneLabel = when {
                                        isSummit -> "قله"
                                        else -> {
                                            val zRatio = step.toDouble() / mountain.altitude
                                            when {
                                                zRatio <= 0.35 -> "کوهپایه"
                                                zRatio <= 0.62 -> "دامنه صعود"
                                                zRatio <= 0.85 -> "یال صعود"
                                                else -> "شانه قله"
                                            }
                                        }
                                    }
                                    Text(
                                        text = zoneLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stepSafetyColor,
                                        maxLines = 1
                                    )
                                    if (isStepLocked) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked Level",
                                            tint = premiumGoldColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${PersianDateHelper.formatToPersianDigits(step)} متر",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = getTextColor()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Real-time precise temperature
                                val absTempStep = kotlin.math.abs(tempAtStep)
                                val formattedAbsTempStep = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absTempStep))
                                val tempStepSign = if (tempAtStep < -0.05) "−" else if (tempAtStep > 0.05) "+" else ""
                                val formattedTemp = "\u200E$tempStepSign$formattedAbsTempStep°C"
                                Text(
                                    text = formattedTemp,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isStepFrozen) frozenColor else (if (isDark) Color(0xEE00FF87) else Color(0xFF007A3E))
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Wind chill / Feels-like precise temperature
                                val absApparentStep = kotlin.math.abs(estApparentAtStep)
                                val formattedAbsApparentStep = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absApparentStep))
                                val apparentStepSign = if (estApparentAtStep < -0.05) "−" else if (estApparentAtStep > 0.05) "+" else ""
                                val formattedApparent = "\u200E$apparentStepSign$formattedAbsApparentStep°"
                                Text(
                                    text = "احساس: $formattedApparent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(0.55f),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val formattedWindStep = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", estWindAtStep))
                                val windColor = if (estWindAtStep > 35.0) (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)) else getTextColor(0.55f)
                                Text(
                                    text = "باد: $formattedWindStep ک‌م/س",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = windColor,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Oxygen Pills - visual representation
                                val oxyColor = when {
                                    oxygenPct < 60 -> if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                                    oxygenPct < 75 -> if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)
                                    else -> if (isDark) Color(0xFF00E676) else Color(0xFF2E7D32)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(oxyColor.copy(alpha = 0.12f))
                                        .border(0.6.dp, oxyColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "🫁",
                                            fontSize = 8.5.sp
                                        )
                                        Text(
                                            text = "${PersianDateHelper.formatToPersianDigits(oxygenPct.toInt())}٪",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = oxyColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                
                // Bottom Divider with Cyan Node
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        getTextColor(0.04f),
                                        getAccentColor().copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(getAccentColor())
                    )
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Bottom Action Bar (tactile luxury controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch Peak Cyber Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(getCardBgColor(Color(0xFF1C2230)))
                            .border(1.dp, getAccentColor().copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable { onChangeClick() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = "Change peak",
                                modifier = Modifier.size(14.dp),
                                tint = getAccentColor()
                            )
                            Text(
                                text = "سوییچ قله (${PersianDateHelper.formatToPersianDigits(pinnedCount)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor()
                            )
                        }
                    }



                    // Heartbeat synchronization metric
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        val indicatorColor = if (isOffline) {
                            if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
                        } else {
                            Color(0xFF00E676)
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(100))
                                .background(indicatorColor.copy(alpha = pulseAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(indicatorColor)
                            )
                        }
                        Text(
                            text = if (isOffline) {
                                if (lastUpdatedTime.isNotEmpty()) "سنجش آفلاین: $lastUpdatedTime" else "سنجش هوشمند آفلاین"
                            } else {
                                if (lastUpdatedTime.isNotEmpty()) "سنجش فعال: $lastUpdatedTime" else "سنجش هوشمند آنلاین"
                            },
                            fontSize = 9.sp,
                            color = getTextColor(0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
}

data class UpcomingHourRisk(
    val index: Int,
    val timeString: String,
    val hourText: String,
    val status: com.example.ui.util.SafetyStatus,
    val report: com.example.ui.util.SafetyReport,
    val temp: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val isDay: Int = 1
)

data class HazardDetailItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val name: String,
    val metricText: String,
    val riskVal: Int
)

data class HourlySafetyAlert(
    val status: com.example.ui.util.SafetyStatus,
    val maxRiskIndex: Int,
    val timeRanges: List<String>,
    val mainHazardName: String,
    val hazardItems: List<HazardDetailItem>,
    val isMultiHazardSynergy: Boolean,
    val actionRecommendation: String,
    val primaryIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun formatAlertTimeRange(startTimeString: String, endTimeString: String, baseTimeString: String): String {
    val startParts = startTimeString.split("T")
    val endParts = endTimeString.split("T")
    val baseParts = baseTimeString.split("T")

    val startDate = startParts.getOrNull(0) ?: ""
    val startTime = startParts.getOrNull(1)?.take(5) ?: ""
    val endDate = endParts.getOrNull(0) ?: ""
    val endTime = endParts.getOrNull(1)?.take(5) ?: ""
    val baseDate = baseParts.getOrNull(0) ?: ""

    fun getDayLabel(dateStr: String): String {
        if (dateStr.isEmpty() || baseDate.isEmpty()) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val d1 = sdf.parse(baseDate)
            val d2 = sdf.parse(dateStr)
            if (d1 != null && d2 != null) {
                val diffMs = d2.time - d1.time
                val diffDays = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
                when (diffDays) {
                    0 -> "امروز"
                    1 -> "فردا"
                    2 -> "پس‌فردا"
                    else -> {
                        val cal = java.util.Calendar.getInstance(java.util.Locale.US)
                        cal.time = d2
                        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                            java.util.Calendar.SATURDAY -> "شنبه"
                            java.util.Calendar.SUNDAY -> "یکشنبه"
                            java.util.Calendar.MONDAY -> "دوشنبه"
                            java.util.Calendar.TUESDAY -> "سه‌شنبه"
                            java.util.Calendar.WEDNESDAY -> "چهارشنبه"
                            java.util.Calendar.THURSDAY -> "پنج‌شنبه"
                            java.util.Calendar.FRIDAY -> "جمعه"
                            else -> dateStr
                        }
                    }
                }
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    // Calculate actual end time (+1 hour for the last hour slot)
    var calculatedEndHour = endTime
    var calculatedEndDate = endDate
    try {
        val hourVal = endTime.take(2).toIntOrNull()
        if (hourVal != null) {
            val nextHour = hourVal + 1
            if (nextHour < 24) {
                calculatedEndHour = String.format(java.util.Locale.US, "%02d:00", nextHour)
            } else {
                calculatedEndHour = "۰۰:۰۰"
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val d = sdf.parse(endDate)
                    if (d != null) {
                        val cal = java.util.Calendar.getInstance(java.util.Locale.US)
                        cal.time = d
                        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                        calculatedEndDate = sdf.format(cal.time)
                    }
                } catch (_: Exception) {}
            }
        }
    } catch (_: Exception) {}

    val startDayLabel = getDayLabel(startDate)
    val endDayLabel = getDayLabel(calculatedEndDate)

    val pStartTime = PersianDateHelper.formatToPersianDigits(startTime)
    val pEndTime = if (calculatedEndHour == "۰۰:۰۰") "۰۰:۰۰" else PersianDateHelper.formatToPersianDigits(calculatedEndHour)

    return if (startDate == calculatedEndDate || (startDate != calculatedEndDate && calculatedEndHour == "۰۰:۰۰" && startDayLabel == endDayLabel)) {
        if (startDayLabel.isNotEmpty()) "$startDayLabel $pStartTime تا $pEndTime" else "$pStartTime تا $pEndTime"
    } else {
        val startPrefix = if (startDayLabel.isNotEmpty()) "$startDayLabel $pStartTime" else "$pStartTime"
        val endPrefix = if (endDayLabel.isNotEmpty()) "$endDayLabel $pEndTime" else "$pEndTime"
        "$startPrefix تا $endPrefix"
    }
}

private fun buildHourlyAlerts(
    upcomingHoursRisks: List<UpcomingHourRisk>,
    targetStatus: com.example.ui.util.SafetyStatus,
    targetAltitude: Int = 2000
): List<HourlySafetyAlert> {
    if (upcomingHoursRisks.isEmpty()) return emptyList()

    val contiguousBlocks = mutableListOf<List<UpcomingHourRisk>>()
    var currentBlock = mutableListOf<UpcomingHourRisk>()

    for (hour in upcomingHoursRisks) {
        if (hour.status == targetStatus) {
            currentBlock.add(hour)
        } else {
            if (currentBlock.isNotEmpty()) {
                contiguousBlocks.add(currentBlock.toList())
                currentBlock = mutableListOf()
            }
        }
    }
    if (currentBlock.isNotEmpty()) {
        contiguousBlocks.add(currentBlock.toList())
    }

    val baseTimeString = upcomingHoursRisks.firstOrNull()?.timeString ?: ""

    // Dynamic Altitude-Dependent Safety Thresholds
    val windRedThresh = if (targetAltitude >= 3800) 28.0 else if (targetAltitude >= 2800) 35.0 else 40.0
    val windYellowThresh = windRedThresh * 0.70

    val coldRedThresh = if (targetAltitude >= 3800) -12.0 else if (targetAltitude >= 2800) -15.0 else -18.0
    val coldYellowThresh = coldRedThresh + 6.0

    val visRedThresh = if (targetAltitude >= 3500) 1500.0 else if (targetAltitude >= 2500) 1000.0 else 800.0
    val visYellowThresh = visRedThresh * 2.0

    val avRedThresh = if (targetAltitude >= 2500) 50 else 65

    data class RawAlert(
        val status: com.example.ui.util.SafetyStatus,
        val maxRiskIndex: Int,
        val timeRangeText: String,
        val mainHazardName: String,
        val hazardItems: List<HazardDetailItem>,
        val isMultiHazardSynergy: Boolean,
        val actionRecommendation: String,
        val primaryIcon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val rawList = contiguousBlocks.map { block ->
        val firstHour = block.first()
        val lastHour = block.last()
        val timeRangeText = formatAlertTimeRange(firstHour.timeString, lastHour.timeString, baseTimeString)

        val maxRiskIndex = block.maxOfOrNull { it.report.riskScore } ?: 0
        val maxLightning = block.maxOfOrNull { it.report.lightningRisk } ?: 0
        val maxCape = block.maxOfOrNull { it.report.capeJKg } ?: 0.0
        val maxWindRisk = block.maxOfOrNull { it.report.windRisk } ?: 0
        val maxWindSp = block.maxOfOrNull { it.report.windSpeedKmH } ?: 0.0
        val maxWhiteout = block.maxOfOrNull { it.report.whiteoutRisk } ?: 0
        val minVis = block.minOfOrNull { it.report.visibilityMeters } ?: 10000.0
        val maxFrostbite = block.maxOfOrNull { it.report.frostbiteRisk } ?: 0
        val minWindChill = block.minOfOrNull { it.report.windChillC } ?: 0.0
        val maxAvalanche = block.maxOfOrNull { it.report.avalancheRisk } ?: 0

        val hazardItems = mutableListOf<HazardDetailItem>()
        var primaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null

        // 1. Lightning / Storm
        if (maxLightning >= 40 || maxCape >= 250.0) {
            val capeVal = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", maxCape))
            hazardItems.add(
                HazardDetailItem(
                    icon = androidx.compose.material.icons.Icons.Default.Bolt,
                    name = "صاعقه و رعدوبرق",
                    metricText = "ناپایداری $capeVal ژول/کیلوگرم",
                    riskVal = maxLightning
                )
            )
            if (primaryIcon == null) primaryIcon = androidx.compose.material.icons.Icons.Default.Bolt
        }

        // 2. Wind
        val isWindTriggered = if (targetStatus == com.example.ui.util.SafetyStatus.RED) maxWindSp >= windRedThresh || maxWindRisk >= 50 else maxWindSp >= windYellowThresh || maxWindRisk >= 30
        if (isWindTriggered) {
            val windSpVal = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", maxWindSp))
            hazardItems.add(
                HazardDetailItem(
                    icon = androidx.compose.material.icons.Icons.Default.Air,
                    name = "طوفان و باد شدید",
                    metricText = "سرعت $windSpVal کیلومتر/ساعت",
                    riskVal = maxWindRisk
                )
            )
            if (primaryIcon == null) primaryIcon = androidx.compose.material.icons.Icons.Default.Air
        }

        // 3. Avalanche
        val isAvTriggered = if (targetStatus == com.example.ui.util.SafetyStatus.RED) maxAvalanche >= avRedThresh else maxAvalanche >= 30
        if (isAvTriggered) {
            val lvl = when (maxAvalanche) {
                in 0..20 -> "۱"
                in 21..40 -> "۲"
                in 41..60 -> "۳"
                in 61..80 -> "۴"
                else -> "۵"
            }
            hazardItems.add(
                HazardDetailItem(
                    icon = androidx.compose.material.icons.Icons.Default.Landscape,
                    name = "خطر بهمن",
                    metricText = "بهمن سطح $lvl",
                    riskVal = maxAvalanche
                )
            )
            if (primaryIcon == null) primaryIcon = androidx.compose.material.icons.Icons.Default.Landscape
        }

        // 4. Whiteout / Visibility
        val isVisTriggered = if (targetStatus == com.example.ui.util.SafetyStatus.RED) minVis <= visRedThresh || maxWhiteout >= 50 else minVis <= visYellowThresh || maxWhiteout >= 30
        if (isVisTriggered) {
            val visKm = minVis / 1000.0
            val visVal = if (minVis >= 1000.0) {
                PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", visKm)) + " کیلومتر"
            } else {
                PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", minVis)) + " متر"
            }
            hazardItems.add(
                HazardDetailItem(
                    icon = androidx.compose.material.icons.Icons.Default.VisibilityOff,
                    name = "وایت‌اوت و مه",
                    metricText = "دید $visVal",
                    riskVal = maxWhiteout
                )
            )
            if (primaryIcon == null) primaryIcon = androidx.compose.material.icons.Icons.Default.VisibilityOff
        }

        // 5. Frostbite / Severe Cold
        val isColdTriggered = if (targetStatus == com.example.ui.util.SafetyStatus.RED) minWindChill <= coldRedThresh || maxFrostbite >= 50 else minWindChill <= coldYellowThresh || maxFrostbite >= 30
        if (isColdTriggered) {
            val absChill = kotlin.math.abs(minWindChill)
            val chillVal = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", absChill))
            val chillSign = if (minWindChill < 0) "−" else ""
            hazardItems.add(
                HazardDetailItem(
                    icon = androidx.compose.material.icons.Icons.Default.AcUnit,
                    name = "سوزباد و افت دما",
                    metricText = "سوزباد $chillSign$chillVal درجه",
                    riskVal = maxFrostbite
                )
            )
            if (primaryIcon == null) primaryIcon = androidx.compose.material.icons.Icons.Default.AcUnit
        }

        // Fallbacks
        if (hazardItems.isEmpty()) {
            val fallbackName = if (targetStatus == com.example.ui.util.SafetyStatus.RED) "طوفان و ناپایداری حاد جوی" else "تغییرات دما و باد"
            val fallbackIcon = if (targetStatus == com.example.ui.util.SafetyStatus.RED) androidx.compose.material.icons.Icons.Default.GppBad else androidx.compose.material.icons.Icons.Default.Warning
            val pRiskVal = PersianDateHelper.formatToPersianDigits(maxRiskIndex)
            hazardItems.add(
                HazardDetailItem(
                    icon = fallbackIcon,
                    name = fallbackName,
                    metricText = "ریسک $pRiskVal٪",
                    riskVal = maxRiskIndex
                )
            )
            primaryIcon = fallbackIcon
        }

        if (primaryIcon == null) {
            primaryIcon = if (targetStatus == com.example.ui.util.SafetyStatus.RED) androidx.compose.material.icons.Icons.Default.GppBad else androidx.compose.material.icons.Icons.Default.Warning
        }

        val mainHazardName = when {
            hazardItems.isEmpty() -> "تغییرات ناپایدار جوی"
            hazardItems.size == 1 -> hazardItems.first().name
            hazardItems.size == 2 -> "${hazardItems[0].name} + ${hazardItems[1].name}"
            else -> {
                val pCount = PersianDateHelper.formatToPersianDigits(hazardItems.size)
                "مخاطرات چندگانه ($pCount عامل)"
            }
        }
        val isMultiHazardSynergy = hazardItems.size >= 2 || (maxRiskIndex >= 60 && hazardItems.any { it.name.contains("باد") } && hazardItems.any { it.name.contains("سوزباد") || it.name.contains("وایت‌اوت") })

        val actionRecommendation = if (targetStatus == com.example.ui.util.SafetyStatus.RED) {
            when {
                isMultiHazardSynergy -> "صعود را فوراً متوقف کرده و به سرعت به پناهگاه یا ترازهای پایین‌تر فرار کنید."
                hazardItems.any { it.name.contains("صاعقه") } -> "صعود را اکیداً متوقف کرده و به سرعت به پناهگاه یا ترازهای پایین‌تر فرار کنید."
                hazardItems.any { it.name.contains("بهمن") } -> "از شیب‌های بالای ۳۰ درجه و دیواره‌های برف‌گیر کاملاً فاصله بگیرید."
                hazardItems.any { it.name.contains("طوفان") || it.name.contains("باد") } -> "صعود را متوقف کنید؛ از خط‌الرأس‌های بادگیر خارج شده و فرار کنید."
                else -> "شرایط جوی بسیار خطرناک است. صعود را متوقف کرده و در مکان امن مستقر شوید."
            }
        } else {
            when {
                isMultiHazardSynergy -> "پوشش چندلایه گورتکس و لایه‌های گرمایشی کامل داشته باشید و زمان صعود را کوتاه کنید."
                hazardItems.any { it.name.contains("مه") || it.name.contains("وایت‌اوت") } -> "تجهیزات کامل مسیریابی (GPS) و چراغ پیشانی همراه داشته باشید و با احتیاط حرکت کنید."
                hazardItems.any { it.name.contains("سوزباد") || it.name.contains("افت دما") } -> "پوشش چندلایه گورتکس، دستکش پر و لایه‌های گرمایشی کامل همراه داشته باشید."
                else -> "با احتیاط کامل صعود کنید؛ شرایط را مداوم پایش کرده و تجهیزات کامل همراه داشته باشید."
            }
        }

        RawAlert(
            status = targetStatus,
            maxRiskIndex = maxRiskIndex,
            timeRangeText = timeRangeText,
            mainHazardName = mainHazardName,
            hazardItems = hazardItems,
            isMultiHazardSynergy = isMultiHazardSynergy,
            actionRecommendation = actionRecommendation,
            primaryIcon = primaryIcon
        )
    }

    return rawList.groupBy { Triple(it.status, it.mainHazardName, it.actionRecommendation) }
        .map { (key, group) ->
            val combinedItems = group.flatMap { it.hazardItems }.distinctBy { it.name }
            val highestRisk = group.maxOf { it.maxRiskIndex }
            val hasSynergy = group.any { it.isMultiHazardSynergy }
            HourlySafetyAlert(
                status = key.first,
                maxRiskIndex = highestRisk,
                timeRanges = group.map { it.timeRangeText }.distinct(),
                mainHazardName = key.second,
                hazardItems = combinedItems,
                isMultiHazardSynergy = hasSynergy,
                actionRecommendation = key.third,
                primaryIcon = group.first().primaryIcon
            )
        }
}

@Composable
fun ClimbingSafetyCard(
    viewModel: WeatherViewModel,
    current: com.example.data.remote.CurrentWeather,
    hourly: com.example.data.remote.HourlyData?,
    altitude: Int,
    mountain: com.example.data.local.MountainEntity,
    minutely15: com.example.data.remote.Minutely15Data? = null,
    units: com.example.data.remote.WeatherUnits? = null,
    daily: com.example.data.remote.DailyData? = null
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    var timeTick by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            timeTick = System.currentTimeMillis() / 60000L
        }
    }

    val mainReport = remember(current, hourly, daily, altitude, mountain, minutely15, units, timeTick) {
        val offsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
        
        val adjMainCurrent = MountaineeringHelper.createAdjustedCurrentWeatherForAltitude(
            cur = current,
            hourly = hourly,
            mountainAltitude = mountain.altitude,
            targetAltitude = altitude,
            mountainName = mountain.name,
            lat = mountain.latitude,
            lon = mountain.longitude
        )

        MountaineeringHelper.evaluateSafety(
            current = adjMainCurrent,
            hourly = hourly,
            daily = daily,
            altitudeOverride = altitude,
            slopeAngle = mountain.slopeAngle,
            aspect = mountain.aspect,
            offsetHours = offsetHours,
            minutely15 = minutely15,
            summitElevation = mountain.altitude.toDouble(),
            baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
            units = units,
            latitude = mountain.latitude,
            longitude = mountain.longitude
        )
    }

    val upcomingHoursRisks = remember(current, hourly, daily, altitude, mountain, minutely15, units, timeTick) {
        if (hourly == null) emptyList<UpcomingHourRisk>() else {
            val offsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
            val currentHourIdx = MountaineeringHelper.findHourlyIndexForCurrent(current, hourly, offsetHours)
            val startIdx = currentHourIdx.coerceAtLeast(0)
            val endIdx = (startIdx + 24).coerceAtMost(hourly.time.size)
            val diff = mountain.altitude - altitude
            
            (startIdx until endIdx).map { i ->
                
                val isCurrentHour = (i == currentHourIdx)
                val rawTempVal = hourly.temperature2m.getOrNull(i) ?: 0.0
                val adjTemp = if (isCurrentHour) current.temperature2m + (diff * 0.0065) else rawTempVal + (diff * 0.0065)
                val rawWindSp = hourly.windSpeed80m?.getOrNull(i) ?: hourly.windSpeed10m?.getOrNull(i) ?: 0.0
                val adjWindSp = MountaineeringHelper.adjustWindWithAltitude(
                    referenceWind = if (isCurrentHour) current.windSpeed80m ?: current.windSpeed10m ?: 0.0 else rawWindSp,
                    referenceElevation = mountain.altitude.toDouble(),
                    targetAltitude = altitude.toDouble(),
                    alpha = null
                )
                
                val rawGusts = if (isCurrentHour) current.windGusts10m ?: 0.0 else hourly.windGusts10m?.getOrNull(i) ?: (rawWindSp * MountaineeringHelper.calculateDynamicGustFactor(hourly.cape?.getOrNull(i)))
                val adjGusts = MountaineeringHelper.adjustWindWithAltitude(
                    referenceWind = rawGusts,
                    referenceElevation = mountain.altitude.toDouble(),
                    targetAltitude = altitude.toDouble(),
                    alpha = null
                )
                
                val basePress = if (isCurrentHour) current.surfacePressure else hourly.surfacePressure?.getOrNull(i)
                val baseTempP = if (isCurrentHour) current.temperature2m else rawTempVal
                val qnhVal = if (isCurrentHour) current.pressureMsl ?: 1013.25 else hourly.pressureMsl?.getOrNull(i) ?: 1013.25
                val adjPressure = MountaineeringHelper.calculateBarometricPressure(basePress, baseTempP, mountain.altitude, altitude, targetTemp = adjTemp, qnh = qnhVal)
                val humidityVal = if (isCurrentHour) current.relativeHumidity2m?.toInt() ?: 60 else hourly.relativeHumidity2m?.getOrNull(i)?.toInt() ?: 60
                val dewPointVal = if (isCurrentHour) current.dewPoint2m ?: com.example.ui.util.MountaineeringHelper.calculateDewPoint(adjTemp, humidityVal.toDouble()) else com.example.ui.util.MountaineeringHelper.calculateDewPoint(adjTemp, humidityVal.toDouble())
                val isNightHour = if (isCurrentHour) current.isDay == 0 else hourly.isDay?.getOrNull(i) == 0
                val adjApparent = if (isCurrentHour) current.apparentTemperature ?: MountaineeringHelper.calculateWindChill(adjTemp, adjWindSp, isNight = isNightHour) else MountaineeringHelper.calculateWindChill(adjTemp, adjWindSp, isNight = isNightHour)
                var finalWeatherCode = if (isCurrentHour) current.weatherCode else hourly.weatherCode.getOrNull(i) ?: 0
                if (!isCurrentHour) {
                    if (adjTemp <= 0.0) {
                        finalWeatherCode = when (finalWeatherCode) {
                            61, 80 -> 71
                            63, 81 -> 73
                            65, 82 -> 75
                            else -> finalWeatherCode
                        }
                    } else if (adjTemp > 2.0) {
                        finalWeatherCode = when (finalWeatherCode) {
                            71, 85 -> 61
                            73, 86 -> 63
                            75 -> 65
                            else -> finalWeatherCode
                        }
                    }
                }
                
                val timeString = hourly.time.getOrNull(i) ?: ""
                val hourText = try {
                    val parts = timeString.split("T")
                    if (parts.size > 1) {
                        parts[1].substring(0, 5)
                    } else {
                        timeString
                    }
                } catch (e: Exception) {
                    ""
                }
                val formattedHourText = PersianDateHelper.formatToPersianDigits(hourText)

                val simulatedCurrent = if (isCurrentHour) MountaineeringHelper.createAdjustedCurrentWeatherForAltitude(
                    cur = current,
                    hourly = hourly,
                    mountainAltitude = mountain.altitude,
                    targetAltitude = altitude,
                    mountainName = mountain.name,
                    lat = mountain.latitude,
                    lon = mountain.longitude
                ) else com.example.data.remote.CurrentWeather(
                    time = timeString,
                    temperature2m = adjTemp,
                    relativeHumidity2m = humidityVal.toDouble(),
                    apparentTemperature = adjApparent,
                    precipitation = hourly.precipitation?.getOrNull(i) ?: 0.0,
                    snowfall = if (adjTemp <= 0.5) maxOf(hourly.snowfall?.getOrNull(i) ?: 0.0, (hourly.precipitation?.getOrNull(i) ?: 0.0) * 0.8) else hourly.snowfall?.getOrNull(i) ?: 0.0,
                    weatherCode = finalWeatherCode,
                    windSpeed10m = adjWindSp,
                    windDirection10m = hourly.windDirection10m?.getOrNull(i) ?: 0.0,
                    windSpeed80m = adjWindSp,
                    windDirection80m = hourly.windDirection80m?.getOrNull(i) ?: 0.0,
                    surfacePressure = adjPressure,
                    pressureMsl = hourly.pressureMsl?.getOrNull(i) ?: 1013.25,
                    freezingLevelHeight = hourly.freezingLevelHeight?.getOrNull(i),
                    windGusts10m = adjGusts,
                    visibility = hourly.visibility?.getOrNull(i),
                    cloudCover = hourly.cloudCover?.getOrNull(i)?.toDouble(),
                    cloudCoverLow = hourly.cloudCoverLow?.getOrNull(i),
                    cloudCoverMid = hourly.cloudCoverMid?.getOrNull(i),
                    cloudCoverHigh = hourly.cloudCoverHigh?.getOrNull(i),
                    soilTemperature0cm = hourly.soilTemperature0cm?.getOrNull(i),
                    isDay = hourly.isDay?.getOrNull(i) ?: 1,
                    dewPoint2m = dewPointVal,
                    cape = hourly.cape?.getOrNull(i)
                )
                
                val hourlyReport = MountaineeringHelper.evaluateSafety(
                    current = simulatedCurrent,
                    hourly = hourly,
                    daily = daily,
                    altitudeOverride = altitude,
                    hourIndexOverride = i,
                    slopeAngle = mountain.slopeAngle,
                    aspect = mountain.aspect,
                    offsetHours = offsetHours,
                    minutely15 = minutely15,
                    summitElevation = mountain.altitude.toDouble(),
                    baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
                    units = units,
                    latitude = mountain.latitude,
                    longitude = mountain.longitude
                )
                UpcomingHourRisk(
                    index = i,
                    timeString = timeString,
                    hourText = formattedHourText,
                    status = hourlyReport.status,
                    report = hourlyReport,
                    temp = adjTemp,
                    windSpeed = adjWindSp,
                    weatherCode = finalWeatherCode,
                    isDay = hourly.isDay?.getOrNull(i) ?: 1
                )
            }
        }
    }

    var selectedHourTimeString by rememberSaveable(mountain.id, altitude) { mutableStateOf("") }

    val report = remember(mainReport, upcomingHoursRisks, selectedHourTimeString) {
        val firstTimeString = upcomingHoursRisks.firstOrNull()?.timeString ?: ""
        if (selectedHourTimeString.isEmpty() || selectedHourTimeString == firstTimeString) {
            upcomingHoursRisks.firstOrNull()?.report ?: mainReport
        } else {
            upcomingHoursRisks.find { it.timeString == selectedHourTimeString }?.report ?: mainReport
        }
    }

    val isCurrentSelected = remember(selectedHourTimeString, upcomingHoursRisks) {
        selectedHourTimeString.isEmpty() || selectedHourTimeString == upcomingHoursRisks.firstOrNull()?.timeString
    }

    val upcomingRedHoursExist = remember(upcomingHoursRisks) {
        upcomingHoursRisks.any { it.status == com.example.ui.util.SafetyStatus.RED }
    }

    val visibleHoursRisks = remember(upcomingHoursRisks, isPremium) {
        if (isPremium) upcomingHoursRisks else upcomingHoursRisks.take(6)
    }
    val lockedHoursRisks = remember(upcomingHoursRisks, isPremium) {
        if (isPremium) emptyList() else upcomingHoursRisks.drop(6)
    }

    val redAlertItems = remember(visibleHoursRisks, altitude) {
        buildHourlyAlerts(visibleHoursRisks, com.example.ui.util.SafetyStatus.RED, altitude)
    }
    val yellowAlertItems = remember(visibleHoursRisks, altitude) {
        buildHourlyAlerts(visibleHoursRisks, com.example.ui.util.SafetyStatus.YELLOW, altitude)
    }

    val lockedRedAlertItems = remember(lockedHoursRisks, altitude) {
        buildHourlyAlerts(lockedHoursRisks, com.example.ui.util.SafetyStatus.RED, altitude)
    }
    val lockedYellowAlertItems = remember(lockedHoursRisks, altitude) {
        buildHourlyAlerts(lockedHoursRisks, com.example.ui.util.SafetyStatus.YELLOW, altitude)
    }
    val totalLockedAlertCount = remember(lockedRedAlertItems, lockedYellowAlertItems) {
        lockedRedAlertItems.sumOf { it.timeRanges.size } + lockedYellowAlertItems.sumOf { it.timeRanges.size }
    }

    val scTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val scSubTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF576A80)
    val scDividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.08f)
    val scCyanAccent = if (isDark) Color(0xFF00FFE0) else Color(0xFF0F766E)

    val glowColor = when (report.status) {
        SafetyStatus.GREEN -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
        SafetyStatus.YELLOW -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
        SafetyStatus.RED -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
    }
    
    val bgGradient = when (report.status) {
        SafetyStatus.GREEN -> {
            if (isDark) Brush.verticalGradient(listOf(Color(0xFF0E1A14), Color(0xFF070B0A)))
            else Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)))
        }
        SafetyStatus.YELLOW -> {
            if (isDark) Brush.verticalGradient(listOf(Color(0xFF1F160C), Color(0xFF0C0907)))
            else Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
        }
        SafetyStatus.RED -> {
            if (isDark) Brush.verticalGradient(listOf(Color(0xFF240E11), Color(0xFF0D0607)))
            else Brush.verticalGradient(listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2)))
        }
    }

    val icon = when (report.status) {
        SafetyStatus.GREEN -> Icons.Default.CheckCircle
        SafetyStatus.YELLOW -> Icons.Default.Warning
        SafetyStatus.RED -> Icons.Default.GppBad
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("climbing_safety_status_card"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, glowColor.copy(alpha = if (isDark) 0.35f else 0.55f)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Main Status Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(glowColor.copy(alpha = 0.12f))
                            .border(1.dp, glowColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Safety Status Indicator",
                            tint = glowColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCurrentSelected) 
                                "ارزیابی هوشمند ایمنی صعود (زمان فعلی)" 
                            else {
                                val selHour = upcomingHoursRisks.find { it.timeString == selectedHourTimeString }
                                "ارزیابی هوشمند ایمنی صعود (ساعت ${selHour?.hourText ?: ""})"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scSubTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = glowColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated line for the status indicator badge
                Surface(
                    shape = RoundedCornerShape(100),
                    color = glowColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, glowColor.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = when (report.status) {
                            SafetyStatus.GREEN -> "وضعیت صعود: آزاد (سبز)"
                            SafetyStatus.YELLOW -> "وضعیت صعود: با احتیاط و هشدار (زرد)"
                            SafetyStatus.RED -> "وضعیت صعود: ممنوع و خطرناک (قرمز)"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = glowColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // 24h Future Horizon Risk Watch & Timeline (Partially available to free, fully to premium)
                if (upcomingHoursRisks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "رادار زمانی پایش ریسک صعود (۲۴ ساعت آینده):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = scCyanAccent,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(upcomingHoursRisks) { index, hourRisk ->
                            val isLocked = !isPremium && index >= 6
                            val isFirst = upcomingHoursRisks.firstOrNull()?.timeString == hourRisk.timeString
                            val isSelected = selectedHourTimeString == hourRisk.timeString || (selectedHourTimeString.isEmpty() && isFirst)
                            
                            val itemColor = when {
                                isLocked -> if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                                hourRisk.status == com.example.ui.util.SafetyStatus.GREEN -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                                hourRisk.status == com.example.ui.util.SafetyStatus.YELLOW -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
                                else -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                            }
                            
                            val bgAlpha = if (isSelected) {
                                if (isDark) 0.24f else 0.18f
                            } else {
                                if (isDark) 0.08f else 0.04f
                            }
                            val borderStrokeWidth = if (isSelected) 2.dp else 1.dp
                            val borderAlpha = if (isSelected) 0.85f else 0.25f
                            
                            Column(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(118.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(itemColor.copy(alpha = bgAlpha))
                                    .border(
                                        borderStrokeWidth,
                                        if (isSelected) itemColor else itemColor.copy(alpha = borderAlpha),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        if (isLocked) {
                                            viewModel.triggerBilling(true)
                                        } else {
                                            selectedHourTimeString = if (isSelected) "" else hourRisk.timeString
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Hour or "Now"
                                Text(
                                    text = if (isFirst) "الان" else hourRisk.hourText,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) itemColor else scTextColor
                                )
                                
                                // Weather Icon
                                Icon(
                                    imageVector = WeatherCodeHelper.getIcon(hourRisk.weatherCode, hourRisk.isDay),
                                    contentDescription = null,
                                    tint = if (isSelected) itemColor else scSubTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                
                                // Temperature (1 Decimal Precision)
                                val hTempAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(hourRisk.temp)))
                                val hTempSign = if (hourRisk.temp < 0) "-" else ""
                                Text(
                                    text = "\u200E$hTempSign$hTempAbs°",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = scTextColor
                                )
                                
                                // Wind
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = scSubTextColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", hourRisk.windSpeed)),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = scSubTextColor
                                    )
                                }
                                
                                // Bottom Indicator: Glowing Lock Badge for locked, Dot for unlocked
                                Box(
                                    modifier = Modifier.height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLocked) {
                                        val lockColor = if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(100))
                                                .background(lockColor.copy(alpha = 0.20f))
                                                .border(1.dp, lockColor.copy(alpha = 0.5f), RoundedCornerShape(100)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "PRO Locked",
                                                tint = lockColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(100))
                                                .background(itemColor)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val bottomGuideText = when {
                        !isPremium -> "✨ پایش ۶ ساعت آینده رایگان است. جهت دسترسی به پایش ۲۴ ساعته کامل، پایش زنده ۱۵ دقیقه‌ای و ۶ شاخص تخصصی ریسک، اشتراک پرو را فعال کنید."
                        isCurrentSelected -> "💡 برای پایش رادار و محاسبات هوشمندِ ریسک در هر ساعت، روی خانه آن ساعت کلیک کنید."
                        else -> {
                            val selHour = upcomingHoursRisks.find { it.timeString == selectedHourTimeString }
                            "🔗 در حال نمایش داده‌های پردازش‌شده برای ساعت ${selHour?.hourText ?: ""}. جهت بازگشت به زمان فعلی، دوباره روی آن کلیک کنید."
                        }
                    }
                    Text(
                        text = bottomGuideText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isPremium) (if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)) else scSubTextColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // High-fidelity Risk Index Visual progress/gauge bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.45f))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = scSubTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "شاخص همه‌جانبه ریسک اتمسفر (R.I):",
                                fontSize = 10.5.sp,
                                color = scSubTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "ضریب ${PersianDateHelper.formatToPersianDigits(report.riskScore)} از ۱۰۰",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = glowColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Segmented signal-bar level gauge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeSegments = when (report.status) {
                            com.example.ui.util.SafetyStatus.GREEN -> {
                                val score = report.riskScore.coerceIn(0, 29)
                                (1 + (score.toFloat() / 29f * 2f)).toInt().coerceIn(1, 3)
                            }
                            com.example.ui.util.SafetyStatus.YELLOW -> {
                                val score = (report.riskScore - 30).coerceIn(0, 29)
                                (4 + (score.toFloat() / 29f * 2f)).toInt().coerceIn(4, 6)
                            }
                            com.example.ui.util.SafetyStatus.RED -> {
                                val score = (report.riskScore - 60).coerceIn(0, 40)
                                (7 + (score.toFloat() / 40f * 3f)).toInt().coerceIn(7, 10)
                            }
                        }
                        for (i in 0 until 10) {
                            val isActive = i < activeSegments
                            val baseColor = when {
                                i < 3 -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                                i < 6 -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
                                else -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                            }
                            val segmentColor = if (isActive) baseColor else baseColor.copy(alpha = if (isDark) 0.12f else 0.18f)
                            val segmentBg = if (isActive) segmentColor else Color.Transparent
                            val segmentBorder = if (!isActive) BorderStroke(0.8.dp, segmentColor.copy(alpha = 0.35f)) else null
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(segmentBg)
                                    .let { 
                                        if (segmentBorder != null) it.border(segmentBorder, RoundedCornerShape(3.dp)) else it
                                    }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(glowColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                            .border(1.dp, glowColor.copy(alpha = if (isDark) 0.35f else 0.25f), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = report.riskCategory,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = glowColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Factor Breakdown (تجزیه شفاف و گرافیکی اجزای تشکیل‌دهنده ریسک R.I)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.04f))
                            .border(0.8.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📊 سهم اجزای تشکیل‌دهنده شاخص ریسک (Factor Breakdown):",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = scSubTextColor
                        )

                        val visKm = report.visibilityMeters / 1000.0
                        val visStr = if (report.visibilityMeters >= 1000.0) {
                            String.format(java.util.Locale.US, "%.1fkm", visKm)
                        } else {
                            String.format(java.util.Locale.US, "%.0fm", report.visibilityMeters)
                        }
                        val windStr = String.format(java.util.Locale.US, "%.0fkm/h", report.windSpeedKmH)
                        val chillStr = String.format(java.util.Locale.US, "%.0f°C", report.windChillC)
                        val avalancheLvl = when (report.avalancheRisk) {
                            in 0..20 -> "۱"
                            in 21..40 -> "۲"
                            in 41..60 -> "۳"
                            in 61..80 -> "۴"
                            else -> "۵"
                        }
                        val uvStr = String.format(java.util.Locale.US, "%.1f", report.uvIndexValue)

                        val hazardFactors = listOf(
                            Triple("⚡ رعدوبرق", report.lightningRisk, PersianDateHelper.formatToPersianDigits("${report.lightningRisk}٪")),
                            Triple("💨 باد و تندباد", report.windRisk, PersianDateHelper.formatToPersianDigits("$windStr (${report.windRisk}٪)")),
                            Triple("❄️ وایت‌اوت و دید", report.whiteoutRisk, PersianDateHelper.formatToPersianDigits("$visStr (${report.whiteoutRisk}٪)")),
                            Triple("🥶 سرمازدگی", report.frostbiteRisk, PersianDateHelper.formatToPersianDigits("$chillStr (${report.frostbiteRisk}٪)")),
                            Triple("🏔️ مخاطره بهمن", report.avalancheRisk, PersianDateHelper.formatToPersianDigits("سطح $avalancheLvl (${report.avalancheRisk}٪)")),
                            Triple("☀️ فرابنفش", report.uvRisk, PersianDateHelper.formatToPersianDigits("UV $uvStr (${report.uvRisk}٪)"))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            hazardFactors.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPair.forEach { (name, riskVal, displayVal) ->
                                        val factorColor = when {
                                            riskVal >= 60 -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                                            riskVal >= 30 -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
                                            else -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = name,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = scTextColor
                                                )
                                                Text(
                                                    text = displayVal,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = factorColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(factorColor.copy(alpha = 0.15f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction = (riskVal / 100f).coerceIn(0f, 1f))
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(factorColor)
                                                )
                                            }
                                        }
                                    }
                                    if (rowPair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = report.description,
                    fontSize = 12.5.sp,
                    lineHeight = 21.sp,
                    color = scTextColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Overall safety status badge (Visible to everyone)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(glowColor.copy(alpha = if (isDark) 0.08f else 0.05f))
                        .border(1.dp, glowColor.copy(alpha = if (isDark) 0.25f else 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(100))
                                .background(glowColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (report.status) {
                                com.example.ui.util.SafetyStatus.GREEN -> "وضعیت کلی صعود: شرایط کاملاً ایمن و مناسب"
                                com.example.ui.util.SafetyStatus.YELLOW -> "وضعیت کلی صعود: نیازمند احتیاط کامل و تجهیزات فنی"
                                com.example.ui.util.SafetyStatus.RED -> "وضعیت کلی صعود: شرایط بحرانی صعود اکیداً ممنوع!"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = glowColor
                        )
                    }
                }

                // 15-Minute Short-term Nowcasting Section (Under PRO/Premium subscription)
                if (isPremium) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.45f))
                            .border(1.dp, scCyanAccent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        if (!isCurrentSelected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(scCyanAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = scCyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "پیش‌بینی فوق‌کوتاه‌مدت ۱۵ دقیقه‌ای",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = scCyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💡 پایش فوق‌کوتاه‌مدت ۱۵ دقیقه‌ای رادار زنده مخصوص شرایط کنونی شماست. برای پایش مجدد زنده، روی کارت زمان فعلی (الان) کلیک کنید تا این پورتال فعال گردد.",
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = scTextColor
                            )
                        } else {
                            // Header Row with modern Live Tracking indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(scCyanAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = scCyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "پیش‌بینی فوق‌کوتاه‌مدت ۱۵ دقیقه‌ای",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = scCyanAccent
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                // Real-time active radar dot (Visible to Premium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(100))
                                            .background(Color(0xFF00FFE0))
                                    )
                                    Text(
                                        text = "پایش زنده فعال",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scCyanAccent.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // High-fidelity Metrics Grid (3 equal-size, perfectly aligned cards)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Metric 1: Lightning & CAPE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f))
                                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = if (report.minutelyLightningTrend.contains("افزایش")) Color(0xFFFF5252) else scCyanAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "صاعقه و CAPE",
                                            fontSize = 9.sp,
                                            color = scSubTextColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = report.minutelyLightningTrend,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (report.minutelyLightningTrend.contains("افزایش")) Color(0xFFFF5252) else scTextColor
                                    )
                                }
                            }

                            // Metric 2: Precipitation Intensity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f))
                                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = if (report.minutelyPrecipitationIntensity != "بدون بارش") Color(0xFF00FFE0) else scSubTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "شدت بارش",
                                            fontSize = 9.sp,
                                            color = scSubTextColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = report.minutelyPrecipitationIntensity,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (report.minutelyPrecipitationIntensity != "بدون بارش") Color(0xFF00FFE0) else scTextColor
                                    )
                                }
                            }

                            // Metric 3: Instantaneous Peak Wind
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f))
                                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Air,
                                            contentDescription = null,
                                            tint = if (report.minutelyInstantaneousPeakWind.contains("🚨") || report.minutelyInstantaneousPeakWind.contains("تندباد")) Color(0xFFFF5252) else scSubTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "تندباد لحظه‌ای",
                                            fontSize = 9.sp,
                                            color = scSubTextColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = report.minutelyInstantaneousPeakWind,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (report.minutelyInstantaneousPeakWind.contains("🚨") || report.minutelyInstantaneousPeakWind.contains("تندباد")) Color(0xFFFF5252) else scTextColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Confidence & Method Card (World-class UI/UX Banner)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF00FFE0).copy(alpha = 0.04f) else Color(0xFF0284C7).copy(alpha = 0.05f))
                                .border(1.dp, if (isDark) Color(0xFF00FFE0).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "ضریب اطمینان مدل پیش‌بینی:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = scTextColor
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isDark) Color(0xFF00FFE0).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF00FFE0).copy(alpha = 0.4f) else Color(0xFF0284C7).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${PersianDateHelper.formatToPersianDigits(report.minutelyConfidencePercent)}٪ (عالی)",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                
                                LinearProgressIndicator(
                                    progress = { (report.minutelyConfidencePercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7),
                                    trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📡 ${report.minutelyConfidenceLabel}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = scSubTextColor
                                    )
                                    Text(
                                        text = "الگوریتم: ${report.minutelyInterpolationMethod}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF00FFE0).copy(alpha = 0.8f) else Color(0xFF0284C7)
                                    )
                                }
                            }
                        }

                        // Explicit Interpolation Alert Badge for Non-Radar (e.g. Iran) regions
                        if (!MountaineeringHelper.isMinutely15NativeHighResolution(mountain.latitude, mountain.longitude)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF451A03).copy(alpha = 0.35f) else Color(0xFFFEF3C7))
                                    .border(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFFD97706).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Interpolation Info",
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "داده‌های ۱۵ دقیقه‌ای این منطقه به دلیل عدم پوشش راداری مستقیم، با الگوریتم درون‌یابی Monotone Spline از مدل ساعتی Open-Meteo تولید شده‌اند.",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // High-fidelity Immediate Risk Level Badge Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.01f) else Color.Black.copy(alpha = 0.02f))
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = scSubTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "سطح ریسک لحظه‌ای (۱۵ دقیقه آینده):",
                                        fontSize = 10.sp,
                                        color = scSubTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val rColor = when (report.immediateRiskLevel) {
                                    "بسیار بالا" -> Color(0xFFFF5252)
                                    "بالا" -> Color(0xFFFFA726)
                                    "متوسط" -> Color(0xFFFFD54F)
                                    else -> Color(0xFF00FF87)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = rColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, rColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = report.immediateRiskLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = rColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Dynamic immediate life-saving recommendation alert box
                        Spacer(modifier = Modifier.height(10.dp))
                        val minutelyRiskInfo = when (report.immediateRiskLevel) {
                            "بسیار بالا" -> Pair(
                                "🚨 هشدار جانی رعدوبرق و افت ناگهانی دما در ۱۵ دقیقه آینده! صعود را فوراً متوقف کرده و به سرعت به سمت جان‌پناه یا ترازهای ارتفاعی پایین‌تر فرار کنید.",
                                Color(0xFFFF5252)
                            )
                            "بالا" -> Pair(
                                "⚠️ شرایط جوی به شدت ناپایدار است! احتمال رعدوبرق و رگبار شدید در ۱۵ دقیقه آینده بسیار بالا است. صعود را موقتاً تعلیق کرده و در پناهگاهی ایمن مستقر شوید.",
                                Color(0xFFFFA726)
                            )
                            "متوسط" -> Pair(
                                "⚡ ناپایداری‌های خفیف جوی گزارش شده است. هوشیار باشید، پوشش ضدآب خود را بپوشید و بر تحرکات ابرهای بالا دست نظارت دقیق داشته باشید.",
                                Color(0xFFFFD54F)
                            )
                            else -> Pair(
                                "✅ شرایط جوی صعود برای ۱۵ دقیقه آینده کاملاً پایدار، مساعد و ایمن ارزیابی شده است. صعود خوشی داشته باشید!",
                                Color(0xFF00FF87)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(minutelyRiskInfo.second.copy(alpha = if (isDark) 0.08f else 0.04f))
                                .border(1.dp, minutelyRiskInfo.second.copy(alpha = if (isDark) 0.35f else 0.45f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = minutelyRiskInfo.first,
                                fontSize = 9.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
                            )
                        }

                        report.minutelySourceWarning?.let { warning ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = warning,
                                fontSize = 8.5.sp,
                                color = if (isDark) Color(0xFFFFA726).copy(alpha = 0.8f) else Color(0xFFD97706),
                                fontWeight = FontWeight.Medium,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            if (redAlertItems.isNotEmpty() || yellowAlertItems.isNotEmpty() || (!isPremium && totalLockedAlertCount > 0)) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF1E2028) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationImportant,
                                contentDescription = "Alerts Summary",
                                tint = if (redAlertItems.isNotEmpty()) (if (isDark) Color(0xFFFF5252) else Color(0xFFDC2626)) else (if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPremium) "بازه‌های مخاطره‌آمیز ۲۴ ساعت آینده:" else "بازه‌های مخاطره‌آمیز ۶ ساعت آینده:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = scTextColor
                            )
                        }
                        if (redAlertItems.isEmpty() && yellowAlertItems.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF00FF87).copy(alpha = 0.1f) else Color(0xFF15803D).copy(alpha = 0.08f))
                                    .border(1.dp, if (isDark) Color(0xFF00FF87).copy(alpha = 0.3f) else Color(0xFF15803D).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Safe",
                                    tint = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "در این بازه زمانی، مخاطره خاصی برای صعود پیش‌بینی نشده و شرایط جوی نسبتاً پایدار است.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D),
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            (redAlertItems + yellowAlertItems).forEach { alert ->
                                val maxR = alert.maxRiskIndex
                                val alertTextColor = when {
                                    maxR >= 75 -> if (isDark) Color(0xFFFF3366) else Color(0xFF991B1B)
                                    maxR >= 60 -> if (isDark) Color(0xFFFF5252) else Color(0xFFDC2626)
                                    maxR >= 45 -> if (isDark) Color(0xFFFF9800) else Color(0xFFD97706)
                                    else -> if (isDark) Color(0xFFFFB74D) else Color(0xFFCA8A04)
                                }

                                val badgeBg = alertTextColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                                val badgeBorder = alertTextColor.copy(alpha = if (isDark) 0.45f else 0.35f)

                                val pRiskStr = PersianDateHelper.formatToPersianDigits("${alert.maxRiskIndex}") + "٪"
                                val badgeLabel = when {
                                    maxR >= 75 -> "🚨 خطر حاد ($pRiskStr)"
                                    maxR >= 60 -> "🚨 خطر جانی ($pRiskStr)"
                                    maxR >= 45 -> "⚠️ احتیاط بالا ($pRiskStr)"
                                    else -> "⚠️ احتیاط ($pRiskStr)"
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badgeBg)
                                        .border(1.dp, badgeBorder, RoundedCornerShape(12.dp))
                                        .padding(11.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Title Row & Severity Badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = alert.primaryIcon,
                                                    contentDescription = "Hazard Icon",
                                                    tint = alertTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = alert.mainHazardName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = alertTextColor,
                                                    lineHeight = 17.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(100),
                                                color = alertTextColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.8.dp, alertTextColor.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = badgeLabel,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = alertTextColor,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Time Range Banner
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(alertTextColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = "Time Range Icon",
                                                    tint = alertTextColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                val timeStr = alert.timeRanges.joinToString(" • ")
                                                Text(
                                                    text = "بازه زمانی: $timeStr",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = alertTextColor,
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Hazard Detail Badges (Explicit risk types and physical metrics - FlowRow for multi-line wrapping)
                                        if (alert.hazardItems.isNotEmpty()) {
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                alert.hazardItems.forEach { hItem ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f),
                                                        border = BorderStroke(0.6.dp, alertTextColor.copy(alpha = 0.35f))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = hItem.icon,
                                                                contentDescription = null,
                                                                tint = alertTextColor,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(
                                                                text = "${hItem.name} • ${hItem.metricText}",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = scTextColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Multi-Hazard Synergy Banner
                                        if (alert.isMultiHazardSynergy) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isDark) Color(0xFF451A03).copy(alpha = 0.45f) else Color(0xFFFEF3C7))
                                                    .border(0.8.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "🌀",
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "اثر هم‌افزایی مخاطرات: ترکیب هم‌زمان چند عامل خطر (باد، سرما، رطوبت/دید) خطر هیپوترمی و سقوط را مضاعف می‌کند.",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                                        lineHeight = 14.5.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        // Safety Action Instruction
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                                            border = BorderStroke(0.6.dp, alertTextColor.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "💡",
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = alert.actionRecommendation,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = scTextColor,
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    
                    if (!isPremium && totalLockedAlertCount > 0) {
                        val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(goldColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                                .border(1.dp, goldColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.triggerBilling(true) }
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Pro Feature",
                                    tint = goldColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val pCount = PersianDateHelper.formatToPersianDigits(totalLockedAlertCount)
                                    Text(
                                        text = "🚀 $pCount بازه مخاطره‌آمیز دیگر در ساعات ۷ تا ۲۴ آینده شناسایی شد!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = goldColor
                                    )
                                    Text(
                                        text = "برای بازکردن رادار کامل ۲۴ ساعته مخاطرات و پروتکل‌های بقا کلیک کنید ⚡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = scTextColor,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



                if (isPremium) {

                    Spacer(modifier = Modifier.height(10.dp))

                    // ✅ نمایشگر پیوسته بقا و فرار از طوفان (مکمل فاز ۲)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF0D1527).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.45f))
                            .border(0.6.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🛡️ وضعیت پیشرفته بقا و استراتژی طوفان:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = scCyanAccent
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // دمای مرطوب
                            val wbVal = report.wetBulb
                            val (wbLabel, wbColor) = when {
                                wbVal == null -> "نامشخص" to scTextColor.copy(alpha = 0.5f)
                                wbVal < 0.0 -> "خطر انجماد" to (if (isDark) Color(0xFF00E5FF) else Color(0xFF0284C7))
                                wbVal <= 15.0 -> "ایده‌آل صعود" to (if (isDark) Color(0xFF00FF87) else Color(0xFF15803D))
                                wbVal <= 22.0 -> "مطبوع" to (if (isDark) Color(0xFFAEEA00) else Color(0xFF4D7C0F))
                                wbVal <= 28.0 -> "استرس گرمایی" to (if (isDark) Color(0xFFFFA726) else Color(0xFFC2410C))
                                else -> "خطر گرمازدگی" to (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f))
                                    .padding(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("🌡️ دمای مرطوب", fontSize = 8.sp, color = scTextColor.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val wbText = if (report.wetBulb != null) {
                                        val wbAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(report.wetBulb)))
                                        val wbSign = if (report.wetBulb < 0) "-" else ""
                                        "\u200E$wbSign$wbAbs°C"
                                    } else "نامشخص"
                                    Text(
                                        text = wbText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = wbColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = wbLabel,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = wbColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // هومیدکس / شاخص گرما
                            val hVal = report.humidex
                            val (hLabel, hColor) = when {
                                hVal == null -> "نامشخص" to scTextColor.copy(alpha = 0.5f)
                                hVal < 0.0 -> "سرد / هیپوترمی" to (if (isDark) Color(0xFF80D8FF) else Color(0xFF0284C7))
                                hVal <= 25.0 -> "مطبوع و ایمن" to (if (isDark) Color(0xFF00FF87) else Color(0xFF15803D))
                                hVal <= 35.0 -> "گرمای خفیف" to (if (isDark) Color(0xFFFFA726) else Color(0xFFC2410C))
                                else -> "خطر گرمازدگی" to (if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f))
                                    .padding(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("🥵 شاخص گرما", fontSize = 8.sp, color = scTextColor.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val hText = if (report.humidex != null) {
                                        val hAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(report.humidex)))
                                        val hSign = if (report.humidex < 0) "-" else ""
                                        "\u200E$hSign$hAbs°C"
                                    } else "نامشخص"
                                    Text(
                                        text = hText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = hColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = hLabel,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // زمان سرمازدگی
                            val fbMins = report.frostbiteTimeMinutes
                            val (fbText, fbSubText, fbColor) = when {
                                fbMins == null -> Triple("ایمن / نامحدود", "پوشش نرمال", if (isDark) Color(0xFF00FF87) else Color(0xFF15803D))
                                fbMins <= 5 -> Triple("${PersianDateHelper.formatToPersianDigits(fbMins)} دقیقه", "🚨 انجماد آنی", if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C))
                                fbMins <= 15 -> Triple("${PersianDateHelper.formatToPersianDigits(fbMins)} دقیقه", "⚠️ انجماد سریع", if (isDark) Color(0xFFFF7043) else Color(0xFFC2410C))
                                fbMins <= 30 -> Triple("${PersianDateHelper.formatToPersianDigits(fbMins)} دقیقه", "⚠️ خطر انجماد", if (isDark) Color(0xFFFFA726) else Color(0xFFD97706))
                                else -> Triple("${PersianDateHelper.formatToPersianDigits(fbMins)} دقیقه", "هشدار سوزباد", if (isDark) Color(0xFFFFD54F) else Color(0xFFA16207))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f))
                                    .padding(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("❄️ زمان سرمازدگی", fontSize = 8.sp, color = scTextColor.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = fbText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = fbColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = fbSubText,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fbColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // کادر استراتژی فرار از طوفان و پروتکل‌های تاکتیکی نجات جان
                        val escapeColor = when (report.escapeAction) {
                            "DESCEND_IMMEDIATELY" -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                            "HOLD_POSITION" -> if (isDark) Color(0xFFFFA726) else Color(0xFFC2410C)
                            "CAUTION" -> if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                            else -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                        }

                        val actionTagText = when (report.escapeAction) {
                            "DESCEND_IMMEDIATELY" -> "🚨 فرود فوری و بی‌درنگ"
                            "HOLD_POSITION" -> "⚠️ توقف و پناه‌گیری در موقعیت"
                            "CAUTION" -> "⚠️ صعود مشروط با احتیاط"
                            else -> "✅ شرایط صعود مساعد و پایدار"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(escapeColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                                .border(0.8.dp, escapeColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ۱. سطر عنوان اصلی و بج وضعیت اقدام
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = when (report.escapeAction) {
                                            "CONTINUE" -> Icons.Default.CheckCircle
                                            "CAUTION" -> Icons.Default.Warning
                                            else -> Icons.Default.Dangerous
                                        },
                                        contentDescription = "Escape Action Icon",
                                        tint = escapeColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = actionTagText,
                                        fontSize = 11.sp,
                                        color = escapeColor,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (report.escapeTargetElevation > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(escapeColor.copy(alpha = 0.20f))
                                            .border(0.5.dp, escapeColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        val badgeLabel = when (report.escapeAction) {
                                            "DESCEND_IMMEDIATELY" -> "تراز فرود"
                                            "HOLD_POSITION" -> "تراز پناه‌گیری"
                                            "CAUTION" -> "تراز احتیاط"
                                            else -> "تراز صعود"
                                        }
                                        Text(
                                            text = "$badgeLabel: ${PersianDateHelper.formatToPersianDigits(report.escapeTargetElevation.toInt())}م",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = escapeColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // ۲. سطر بج‌های اختصاصی پنجره زمانی فرار، سرعت فرود و جبهه پناه‌گیری (هم‌اندازه با وزن ۱)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (report.escapeTimeToImpactMinutes != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "⏱️ پنجره فرار",
                                                fontSize = 8.sp,
                                                color = scTextColor.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${PersianDateHelper.formatToPersianDigits(report.escapeTimeToImpactMinutes)} دقیقه",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = escapeColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                if (report.escapeRequiredDescentRateMh != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🏃 سرعت فرود",
                                                fontSize = 8.sp,
                                                color = scTextColor.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${PersianDateHelper.formatToPersianDigits(report.escapeRequiredDescentRateMh)} m/h",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = escapeColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                if (report.escapeShelterDirectionText.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🛡️ پناه‌گاه پشت باد",
                                                fontSize = 8.sp,
                                                color = scTextColor.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = report.escapeShelterDirectionText,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = scCyanAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // ۳. عنوان سناریو و علت اصلی خطر (راست‌چین کامل)
                            Text(
                                text = report.escapeReason,
                                fontSize = 9.5.sp,
                                color = scTextColor,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // ۴. دستورالعمل‌های گام‌به‌گام تاکتیکی نجات جان (Tactical Steps)
                            if (report.escapeTacticalSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "📌 دستورالعمل‌های حیاتی تاکتیکی (${report.escapeScenarioTitle}):",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = escapeColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    report.escapeTacticalSteps.forEach { step ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = step,
                                                fontSize = 9.sp,
                                                color = scTextColor,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 13.5.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // CompositionLocalProvider guarantees 100% RTL and Right-Alignment across all 3 Cards
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // Render Active Environmental Hazards (If any detected for Pro Users)
                    if (isPremium && report.environmentalHazards.isNotEmpty()) {
                        val hasActualHazards = report.environmentalHazards.any { !it.startsWith("✅") }
                        val sectionAccentColor = if (hasActualHazards) {
                            if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                        } else {
                            if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = scDividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(sectionAccentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ریسک‌های محیطی و بحران‌های تراز صعود:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = sectionAccentColor.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Right
                                )
                            }

                            // 🎯 نشانگر ضریب اطمینان مدلسازی محیطی
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF00E676).copy(alpha = 0.12f) else Color(0xFF15803D).copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF00E676).copy(alpha = 0.25f) else Color(0xFF15803D).copy(alpha = 0.25f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "🎯 اطمینان مدل: ${PersianDateHelper.formatToPersianDigits(report.environmentalConfidencePercent)}٪",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF00E676) else Color(0xFF15803D),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        Text(
                            text = "مدلسازی دینامیک open-meteo.com و ترازسنجی چندگانه اتمسفر mountain meteorology",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            color = scSubTextColor,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            report.environmentalHazards.forEach { hazard ->
                                val isPositive = hazard.startsWith("✅")
                                val isCritical = hazard.startsWith("🚨")
                                val isWarning = hazard.startsWith("⚠️")

                                val boxBg = when {
                                    isPositive -> if (isDark) Color(0xFF00FF87).copy(alpha = 0.05f) else Color(0xFF15803D).copy(alpha = 0.06f)
                                    isCritical -> if (isDark) Color(0xFFFF5252).copy(alpha = 0.08f) else Color(0xFFDC2626).copy(alpha = 0.07f)
                                    isWarning -> if (isDark) Color(0xFFFFB74D).copy(alpha = 0.08f) else Color(0xFFD97706).copy(alpha = 0.07f)
                                    else -> if (isDark) Color(0xFFFF5252).copy(alpha = 0.04f) else Color.White.copy(alpha = 0.5f)
                                }
                                val boxBorder = when {
                                    isPositive -> if (isDark) Color(0xFF00FF87).copy(alpha = 0.2f) else Color(0xFF15803D).copy(alpha = 0.25f)
                                    isCritical -> if (isDark) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFFDC2626).copy(alpha = 0.3f)
                                    isWarning -> if (isDark) Color(0xFFFFB74D).copy(alpha = 0.25f) else Color(0xFFD97706).copy(alpha = 0.3f)
                                    else -> if (isDark) Color(0xFFFF5252).copy(alpha = 0.12f) else Color(0xFFFF5252).copy(alpha = 0.25f)
                                }
                                val itemTint = when {
                                    isPositive -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                                    isCritical -> if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                                    isWarning -> if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706)
                                    else -> if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(boxBg)
                                        .border(1.dp, boxBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Hazard Alert",
                                        tint = itemTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = hazard,
                                        fontSize = 11.sp,
                                        color = scTextColor,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Render Meteorological Risk Evaluation Basics
                    if (isPremium && report.riskAssessmentBasics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = scDividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(glowColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مبانی ترازسنجی و سنسورهای سنجش ریسک:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = glowColor.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Right
                                )
                            }

                            // ⚖️ نشانگر کالیبراسیون و دقت بارومتریک
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(glowColor.copy(alpha = 0.12f))
                                    .border(1.dp, glowColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "⚖️ کالیبره ICAO/QNH",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = glowColor,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            report.riskAssessmentBasics.forEach { fact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.015f) else Color.White.copy(alpha = 0.45f))
                                        .border(0.6.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(RoundedCornerShape(100))
                                            .background(glowColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = fact,
                                        fontSize = 10.5.sp,
                                        color = scTextColor,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Render Actionable Climbing Recommendations & Protocols
                    if (isPremium && report.climbingRecommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = scDividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(scCyanAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پروتوکل‌های تاکتیکی صعود و بقای فنی:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = scCyanAccent,
                                textAlign = TextAlign.Right
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF090E17).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.55f))
                                .border(1.dp, scCyanAccent.copy(alpha = if (isDark) 0.15f else 0.28f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            report.climbingRecommendations.forEach { recommendation ->
                                val isCriticalEscape = recommendation.startsWith("🚨") || recommendation.contains("اقدام فرار")
                                val isCautionAction = recommendation.startsWith("⚠️") || recommendation.contains("احتیاط تاکتیکی")

                                val itemIcon = when {
                                    isCriticalEscape -> Icons.Default.Warning
                                    isCautionAction -> Icons.Default.Warning
                                    else -> Icons.Default.CheckCircle
                                }
                                val itemTint = when {
                                    isCriticalEscape -> if (isDark) Color(0xFFFF5252) else Color(0xFFC53030)
                                    isCautionAction -> if (isDark) Color(0xFFFFB300) else Color(0xFFD97706)
                                    else -> scCyanAccent
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = "Recommendation Check",
                                        tint = itemTint,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = recommendation,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                        color = if (isCriticalEscape) itemTint else scTextColor,
                                        fontWeight = if (isCriticalEscape || isCautionAction) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = scDividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.triggerBilling(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = goldColor.copy(alpha = if (isDark) 0.05f else 0.08f)
                        ),
                        border = BorderStroke(1.dp, goldColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Feature",
                                tint = goldColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "⚡ فعال‌سازی کامل «رادار ۲۴ ساعته ریسک صعود» و «پروتکل‌های پیشرفته بقا»",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = getTextColor(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "با ارتقا به اشتراک پرو، به پایش ۲۴ ساعته ساعتی ریسک صعود، شبیه‌ساز زنده ۱۵ دقیقه‌ای، ۶ شاخص تخصصی (رعدوبرق، باد، وایت‌اوت، سرمازدگی، بهمن و UV) و توصیه‌های هوشمند بقا و فرار از طوفان در تمام ساعت‌ها دسترسی پیدا کنید.",
                                fontSize = 10.5.sp,
                                color = getTextColor(0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "🚀 ارتقا به نسخه پرو و بازکردن تمام امکانات ⚡",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = goldColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Official Mountaineering Safety Disclaimer Banner
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Safety Disclaimer",
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "این اپلیکیشن ابزار کمکی تصمیم‌گیری است و جایگزین قضاوت حرفه‌ای سرپرست تیم کوهنوردی نمی‌شود. همیشه آخرین گزارش‌های محلی هواشناسی و شرایط میدانی را بررسی کنید.",
                            fontSize = 10.sp,
                            color = scSubTextColor,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
}

data class AtmosphereStats(
    val windSpeed80m: Double,
    val windDirection80m: Double,
    val humidity: Double,
    val surfacePressure: Double,
    val windChill: Double,
    val freezingLevel: Int,
    val uvIndex: Double,
    val windCategory: String,
    val oxygenRatio: Int,
    val hypoxiaWarning: String,
    val chillStatus: String,
    val pathIsFrozen: Boolean,
    val frozenWarning: String,
    val uvStatus: String,
    val uvHint: String,
    val uvColorHex: String,
    val cloudCeilingInFog: Boolean,
    val humidityWarning: String,
    val visibility: Double,
    val visibilityWarning: String,
    val windGusts: Double,
    val windGustsWarning: String,
    val cloudCover: Double,
    val cloudCoverWarning: String,
    val isDayText: String,
    val isDayColorHex: String,
    val dewPoint: Double,
    val dewPointWarning: String,
    val dewPointColorHex: String,
    val cape: Double,
    val capeWarning: String,
    val capeColorHex: String,
    val snowfall: Double,
    val snowfallWarning: String,
    val snowfallColorHex: String,
    val spread: Double,
    val spreadWarning: String,
    val spreadColorHex: String
)

@Composable
fun CurrentWeatherSection(
    current: com.example.data.remote.CurrentWeather,
    hourly: com.example.data.remote.HourlyData?,
    daily: com.example.data.remote.DailyData? = null,
    altitude: Int,
    mountain: MountainEntity
) {
    val stats = remember(current, hourly, daily, altitude, mountain) {
        val windSpeed80mVal = current.windSpeed80m ?: current.windSpeed10m
        val windDirection80mVal = current.windDirection80m ?: current.windDirection10m
        val humidityVal = current.relativeHumidity2m ?: 60.0
        val surfacePressureVal = current.surfacePressure ?: 850.0
        
        val isNightCurrent = current.isDay == 0
        val windChillVal = MountaineeringHelper.calculateWindChill(current.temperature2m, windSpeed80mVal, isNight = isNightCurrent)
        val freezingLevelVal = current.freezingLevelHeight?.toInt() ?: MountaineeringHelper.estimateFreezingLevel(current.temperature2m, altitude)
        
        val isSnowCoverPresent = (current.snowfall ?: 0.0) > 0.0 || 
            (current.snowDepth ?: 0.0) > 0.0 || 
            ((current.precipitation ?: 0.0) > 0.0 && current.temperature2m <= 0.5)
        val offsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
        val uvIndexVal = MountaineeringHelper.calculateResolvedUvIndex(
            current = current,
            hourly = hourly,
            daily = daily,
            altitude = altitude,
            mountainAltitude = mountain.altitude,
            snowCover = isSnowCoverPresent,
            snowfallRate = current.snowfall,
            offsetHours = offsetHours
        )
        
        val windCat = when {
            windSpeed80mVal > 65.0 -> "تندباد بحرانی و خطر سقوط"
            windSpeed80mVal > 45.0 -> "خطر تخلیه تعادل فیزیکی"
            windSpeed80mVal > 25.0 -> "تندباد شدید خط‌الراس"
            else -> "جریان باد معتدل و ایمن"
        }
        
        val oxRatio = ((surfacePressureVal / 1013.25) * 100).toInt()
        val hypoxiaWarn = when {
            oxRatio < 60 -> "خطر HAPE/HACE (بحرانی)"
            oxRatio < 68 -> "ریسک هیپوکسی حاد"
            oxRatio < 76 -> "کاهش چشمگیر بار اکسیژن"
            else -> "اکسیژن اتمسفر پایدار"
        }
        
        val chillStat = when {
            windChillVal < -27.0 -> "خطر انجماد سریع بافت‌ها"
            windChillVal < -15.0 -> "سرمازدگی مفرط (Frostbite)"
            windChillVal < -5.0 -> "سوزباد شدید فرساینده"
            windChillVal < 0.0 -> "احساس سرمای نفوذکننده"
            else -> "تطابق حرارتی ایمن"
        }
        
        val frozen = freezingLevelVal < altitude
        val frozenWarn = if (frozen) "معابر صعود یخ‌زده (کرامپون)" else "تراز انجماد بالاتر از صعود"
        
        val uvStat = when {
            isNightCurrent || uvIndexVal <= 0.0 -> "بدون پرتو (شب)"
            uvIndexVal >= 11.0 -> "فرابحرانی (خطر پوست و چشم)"
            uvIndexVal >= 8.0 -> "بسیار زیاد (برف‌کوری شدید)"
            uvIndexVal >= 6.0 -> "زیاد (عینک Cat 3/4 و کلاه)"
            uvIndexVal >= 3.0 -> "متوسط (عینک آفتابی کوه)"
            else -> "کم و ایمن (Low)"
        }
        val uvHintText = when {
            isNightCurrent || uvIndexVal <= 0.0 -> "بدون تابش خورشیدی در شب"
            isSnowCoverPresent -> "تشدید با بازتاب برف و ارتفاع"
            else -> "تعدیل‌شده برای ارتفاع تراز"
        }
        val uvColHex = when {
            isNightCurrent || uvIndexVal <= 0.0 -> "#00FF87"
            uvIndexVal >= 11.0 -> "#FF5252"
            uvIndexVal >= 8.0 -> "#FF7043"
            uvIndexVal >= 6.0 -> "#FFD54F"
            uvIndexVal >= 3.0 -> "#81D4FA"
            else -> "#00FF87"
        }
        
        val visibilityVal = current.visibility ?: -1.0
        val cloudCoverVal = current.cloudCover ?: -1.0
        val fog = (visibilityVal in 0.0..1000.0) || (humidityVal > 95.0 && cloudCoverVal > 80.0)
        val humWarn = when {
            fog -> "کور شدن دید (وایت‌اوت)"
            humidityVal > 85.0 -> "رطوبت بالا و اشباع ابر"
            else -> "جو نیمه‌اشباع و سبک"
        }
        
        val visWarn = when {
            visibilityVal < 0.0 -> "داده در دسترس نیست"
            visibilityVal < 1000.0 -> "دید بحرانی (خطر گم‌شدگی)"
            visibilityVal < 4000.0 -> "دید متوسط (مه سبک)"
            else -> "دید باز و ایده‌آل"
        }

        val windGustsVal = current.windGusts10m ?: (current.windSpeed10m * MountaineeringHelper.calculateDynamicGustFactor(current.cape))
        val gustWarn = when {
            windGustsVal > 65.0 -> "تندباد سهمگین لحظه‌ای"
            windGustsVal > 50.0 -> "تندباد شدید ناگهانی"
            windGustsVal > 30.0 -> "ریسک باد لحظه‌ای"
            else -> "باد لحظه‌ای ایمن"
        }

        val cloudWarn = when {
            visibilityVal in 0.0..1000.0 -> "دید بحرانی (وایت‌اوت)"
            cloudCoverVal < 0.0 -> "داده در دسترس نیست"
            cloudCoverVal > 85.0 -> "آسمان تمام ابری"
            cloudCoverVal > 40.0 -> "نیمه ابری تا ابری"
            else -> "آسمان کاملاً صاف"
        }

        val isDayVal = current.isDay ?: 1
        val isDayTxt = if (isDayVal == 1) "روز ☀️" else "شب 🌙"
        val isDayCol = if (isDayVal == 1) "#FFD54F" else "#90A4AE"

        // Dew Point calculations adjusted dynamically based on active status temperature
        val dewPointVal = current.dewPoint2m ?: MountaineeringHelper.calculateDewPoint(current.temperature2m, humidityVal)
        val tempDiff = current.temperature2m - dewPointVal
        val dewPointWarn = when {
            tempDiff < 2.0 && current.temperature2m <= 0.0 -> "خطر حاد شبنم یخ‌بند"
            tempDiff < 2.0 -> "خطر مه اشباع شدید"
            else -> "جو مطلوب بدون کندانسه"
        }
        val dewPointCol = when {
            tempDiff < 2.0 && current.temperature2m <= 0.0 -> "#FF5252"
            tempDiff < 2.0 -> "#FFD54F"
            else -> "#00FFE0"
        }

        // CAPE Convective Air Instability & Lightning potential calculations
        val hourlyIndex = MountaineeringHelper.findHourlyIndexForCurrent(current, hourly, offsetHours)
        val capeVal = current.cape ?: hourly?.cape?.getOrNull(hourlyIndex) ?: 0.0
        val capeWarn = when {
            capeVal > 1000.0 -> "خطر حاد صاعقه خط‌الراس"
            capeVal > 400.0 -> "ناپایداری شدید الکتریکی"
            capeVal > 100.0 -> "پتانسیل رعدوبرق موضعی"
            else -> "جو کاملاً ایمن و پایدار"
        }
        val capeCol = when {
            capeVal > 1000.0 -> "#FF5252"
            capeVal > 400.0 -> "#FF7043"
            capeVal > 100.0 -> "#FFD54F"
            else -> "#00FF87"
        }

        // Snowfall stats and accumulation tracking
        val snowfallVal = current.snowfall ?: 0.0
        val snowfallWarn = when {
            snowfallVal > 3.0 -> "بحران بهمن و کولاک"
            snowfallVal > 1.0 -> "انباشت سنگین برف تازه"
            snowfallVal > 0.0 -> "بارش تجمعی خفیف"
            else -> "بدون دگرگونی برف سطحی"
        }
        val snowfallCol = when {
            snowfallVal > 3.0 -> "#FF5252"
            snowfallVal > 1.0 -> "#FF7043"
            snowfallVal > 0.0 -> "#FFD54F"
            else -> "#00FF87"
        }

        // Spread representing difference between environmental temperature and dew point
        val spreadVal = current.temperature2m - dewPointVal
        val spreadWarn = when {
            spreadVal < 1.0 -> "مه و اشباع کامل سطحی"
            spreadVal < 3.0 -> "مه‌گیر و پتانسیل تراکم سریع"
            spreadVal < 10.0 -> "رطوبت معتدل غیرترشونده"
            else -> "هوای خشک و دید باز"
        }
        val spreadCol = when {
            spreadVal < 1.0 -> "#FF5252"
            spreadVal < 3.0 -> "#FFD54F"
            spreadVal < 10.0 -> "#00FFE0"
            else -> "#00FF87"
        }

        AtmosphereStats(
            windSpeed80m = windSpeed80mVal,
            windDirection80m = windDirection80mVal,
            humidity = humidityVal,
            surfacePressure = surfacePressureVal,
            windChill = windChillVal,
            freezingLevel = freezingLevelVal,
            windCategory = windCat,
            oxygenRatio = oxRatio,
            hypoxiaWarning = hypoxiaWarn,
            chillStatus = chillStat,
            pathIsFrozen = frozen,
            frozenWarning = frozenWarn,
            uvStatus = uvStat,
            uvHint = uvHintText,
            uvColorHex = uvColHex,
            uvIndex = uvIndexVal,
            cloudCeilingInFog = fog,
            humidityWarning = humWarn,
            visibility = visibilityVal,
            visibilityWarning = visWarn,
            windGusts = windGustsVal,
            windGustsWarning = gustWarn,
            cloudCover = cloudCoverVal,
            cloudCoverWarning = cloudWarn,
            isDayText = isDayTxt,
            isDayColorHex = isDayCol,
            dewPoint = dewPointVal,
            dewPointWarning = dewPointWarn,
            dewPointColorHex = dewPointCol,
            cape = capeVal,
            capeWarning = capeWarn,
            capeColorHex = capeCol,
            snowfall = snowfallVal,
            snowfallWarning = snowfallWarn,
            snowfallColorHex = snowfallCol,
            spread = spreadVal,
            spreadWarning = spreadWarn,
            spreadColorHex = spreadCol
        )
    }

    val isDark = MaterialTheme.colorScheme.background.isDark
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_weather_card"),
        shape = RoundedCornerShape(24.dp),
        border = getCardBorderStroke(getAccentColor()),
        colors = CardDefaults.cardColors(
            containerColor = getCardBgColor(Color(0xFF10141D))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Live Status Header with Pulsing Dot and Day/Night badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(100))
                            .background(getGreenAccentColor())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پایش آنلاین اتمسفر فوقانی قله",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getAccentColor(),
                        letterSpacing = 0.5.sp
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isDayBadgeColor = remember(stats.isDayColorHex, isDark) {
                        val rawColor = try {
                            Color(android.graphics.Color.parseColor(stats.isDayColorHex))
                        } catch (e: Exception) {
                            if (stats.isDayText.contains("روز")) Color(0xFFFFD54F) else Color(0xFF90A4AE)
                        }
                        if (isDark) {
                            rawColor
                        } else {
                            if (stats.isDayText.contains("روز")) Color(0xFFB45309) else Color(0xFF374151)
                        }
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = isDayBadgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, isDayBadgeColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = stats.isDayText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = isDayBadgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    
                    val activeElevPersian = remember(altitude) { com.example.ui.util.PersianDateHelper.formatToPersianDigits(altitude) }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1B2232) else Color(0xFFE3F2FD),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.35f) else Color(0xFF90CAF9))
                    ) {
                        Text(
                            text = "تراز صعود: $activeElevPersian متر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Primary Meteorological Panel (Temperature + Code Icon/Desc with dynamic alpine themes)
            val tempVal = current.temperature2m
            val dynamicGlowBrush = remember(tempVal) {
                when {
                    tempVal < 0.0 -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.12f),
                            Color(0xFF2979FF).copy(alpha = 0.02f)
                        )
                    )
                    tempVal <= 15.0 -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = 0.10f),
                            Color(0xFF00B0FF).copy(alpha = 0.01f)
                        )
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF9100).copy(alpha = 0.12f),
                            Color(0xFFFF3D00).copy(alpha = 0.02f)
                        )
                    )
                }
            }
            val dynamicBorderColor = remember(tempVal, isDark) {
                when {
                    tempVal < 0.0 -> if (isDark) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color(0xFF006064).copy(alpha = 0.35f)
                    tempVal <= 15.0 -> if (isDark) Color(0xFF00FF87).copy(alpha = 0.18f) else Color(0xFF15803D).copy(alpha = 0.25f)
                    else -> if (isDark) Color(0xFFFF9100).copy(alpha = 0.22f) else Color(0xFFB45309).copy(alpha = 0.3f)
                }
            }
            val dynamicAccentColor = remember(tempVal, isDark) {
                when {
                    tempVal < 0.0 -> if (isDark) Color(0xFF00E5FF) else Color(0xFF006064)
                    tempVal <= 15.0 -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                    else -> if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dynamicGlowBrush, RoundedCornerShape(24.dp))
                    .border(1.dp, dynamicBorderColor, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(dynamicAccentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جو حاکم بر ارتفاع جاری (${PersianDateHelper.formatToPersianDigits(altitude)} م)",
                                fontSize = 10.sp,
                                color = getTextColor(0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val mainTempAbs = PersianDateHelper.formatToPersianDigits(if (current.temperature2m % 1.0 == 0.0) {
                                String.format(java.util.Locale.US, "%.0f", kotlin.math.abs(current.temperature2m))
                            } else {
                                String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(current.temperature2m))
                            })
                            val mainTempSign = if (current.temperature2m < 0) "-" else ""
                            Text(
                                text = "\u200E$mainTempSign$mainTempAbs°",
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سلسیوس",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = dynamicAccentColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Apparent temperature / Wind chill pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(100))
                                .border(1.dp, getTextColor(0.06f), RoundedCornerShape(100))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SevereCold,
                                contentDescription = "Severe Cold",
                                modifier = Modifier.size(12.dp),
                                tint = dynamicAccentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val chillPillAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(stats.windChill)))
                            val chillPillSign = if (stats.windChill < 0) "-" else ""
                            Text(
                                text = "سوزباد: \u200E$chillPillSign$chillPillAbs°C \u200E(${stats.chillStatus})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.85f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        dynamicAccentColor.copy(alpha = 0.16f),
                                        Color(0x00000000)
                                    )
                                )
                            )
                            .border(1.5.dp, dynamicAccentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = WeatherCodeHelper.getIcon(current.weatherCode, current.isDay),
                            contentDescription = "Weather Icon",
                            modifier = Modifier.size(36.dp),
                            tint = dynamicAccentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Primary Weather Code Description
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(dynamicAccentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پدیده جوی غالب: ${WeatherCodeHelper.getDescription(current.weatherCode, current.isDay)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = getTextColor()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Advanced climate diagnostics grid for Current Atmosphere
                HorizontalDivider(color = getTextColor(0.06f), thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Diagnostic Column 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "افت دما / ارتفاع",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "۰.۶۵°C / ۱۰۰ متر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.85f)
                        )
                    }

                    // Diagnostic Column 2
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "وضعیت سطح مسیر",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (stats.pathIsFrozen) "یخبندان کامل" else "مرطوب / بدون یخ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.pathIsFrozen) (if (isDark) Color(0xFF00E5FF) else Color(0xFF006064)) else (if (isDark) Color(0xFFFF9100) else Color(0xFFB45309))
                        )
                    }

                    // Diagnostic Column 3
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "اشباع هوا (مه)",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (stats.spread < 1.5) "اشباع کامل (مه)" else if (stats.spread < 4.0) "نیمه‌اشباع" else "دید شفاف",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.spread < 1.5) Color(0xFFFF5252) else if (stats.spread < 4.0) (if (isDark) Color(0xFFFFB300) else Color(0xFFB45309)) else (if (isDark) Color(0xFF00FF87) else Color(0xFF15803D))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📈 نمودار ۱۲ ساعته فشار هوا و احتمال طوفان
            PressureStormChart(
                hourly = hourly,
                altitude = altitude,
                mountain = mountain,
                current = current
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "سنجشگرهای پیشرفته جوی تراز ${PersianDateHelper.formatToPersianDigits(altitude)} متر:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = getTextColor(0.9f),
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            // High-fidelity Grid Layout
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: High Ridge Wind & General Air Pressure
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val windValFormatted = String.format(java.util.Locale.US, "%.0f", stats.windSpeed80m)
                        AtmosphereGridTile(
                            icon = Icons.Default.Air,
                            title = "تندباد خط‌الراس",
                            value = "${PersianDateHelper.formatToPersianDigits(windValFormatted)} ک.م/س",
                            hint = "جهت: ${MountaineeringHelper.getWindDirectionPersian(stats.windDirection80m)}",
                            tagText = stats.windCategory,
                            tagColor = if (stats.windSpeed80m > 65.0) Color(0xFFFF5252) else if (stats.windSpeed80m > 45.0) Color(0xFFFF7043) else if (stats.windSpeed80m > 25.0) Color(0xFFFFD54F) else Color(0xFF00FFE0)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val pressureFormatted = String.format(java.util.Locale.US, "%.0f", stats.surfacePressure)
                        AtmosphereGridTile(
                            icon = Icons.Default.Speed,
                            title = "فشار و اکسیژن",
                            value = "${PersianDateHelper.formatToPersianDigits(pressureFormatted)} hPa",
                            hint = "غلظت اکسیژن: ${PersianDateHelper.formatToPersianDigits(stats.oxygenRatio)}٪",
                            tagText = stats.hypoxiaWarning,
                            tagColor = if (stats.oxygenRatio < 60) Color(0xFFFF5252) else if (stats.oxygenRatio < 68) Color(0xFFFF7043) else if (stats.oxygenRatio < 76) Color(0xFFFFD54F) else Color(0xFF00FFA3)
                        )
                    }
                }

                // Row 2: Wind Chill & Freezing Elevation Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val chillValAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(stats.windChill)))
                        val chillSign = if (stats.windChill < 0) "-" else ""
                        AtmosphereGridTile(
                            icon = Icons.Default.SevereCold,
                            title = "سوزباد (سرمای حسی)",
                            value = "\u200E$chillSign$chillValAbs°C",
                            hint = "برپایه تندباد خط‌الراس",
                            tagText = stats.chillStatus,
                            tagColor = if (stats.windChill < -27.0) Color(0xFFFF5252) else if (stats.windChill < -15.0) Color(0xFFFF7043) else if (stats.windChill < -5.0) Color(0xFFFFD54F) else Color(0xFF00FFE0)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AtmosphereGridTile(
                            icon = Icons.Default.AcUnit,
                            title = "تراز صفر درجه",
                            value = "${PersianDateHelper.formatToPersianDigits(stats.freezingLevel)} متر",
                            hint = "ارتفاع فرضی مرز ذوب",
                            tagText = stats.frozenWarning,
                            tagColor = if (stats.pathIsFrozen) Color(0xFFFF5252) else Color(0xFF00FF87)
                        )
                    }
                }

                // Row 3: UV Index & Humidity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val uvFormatted = if (stats.uvIndex <= 0.0) "۰.۰ UVI" else String.format(java.util.Locale.US, "%.1f UVI", stats.uvIndex)
                        AtmosphereGridTile(
                            icon = Icons.Default.WbSunny,
                            title = "شاخص فرابنفش (UV)",
                            value = PersianDateHelper.formatToPersianDigits(uvFormatted),
                            hint = stats.uvHint,
                            tagText = stats.uvStatus,
                            tagColor = Color(android.graphics.Color.parseColor(stats.uvColorHex))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val humFormatted = String.format(java.util.Locale.US, "%.0f", stats.humidity)
                        AtmosphereGridTile(
                            icon = Icons.Default.WaterDrop,
                            title = "رطوبت نسبی جو",
                            value = "${PersianDateHelper.formatToPersianDigits(humFormatted)}٪",
                            hint = "تراکم بخار آب پیرامون قله",
                            tagText = stats.humidityWarning,
                            tagColor = if (stats.cloudCeilingInFog) Color(0xFFFF5252) else if (stats.humidity > 85.0) Color(0xFFFFD54F) else Color(0xFF00FFE0)
                        )
                    }
                }

                // Row 4: Wind Gusts & Cloud/Visibility combination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AtmosphereGridTile(
                            icon = Icons.Default.Air,
                            title = "تندباد لحظه‌ای",
                            value = "${PersianDateHelper.formatToPersianDigits(stats.windGusts.toInt())} ک.م/س",
                            hint = "حداکثر رخداد در خط‌الراس",
                            tagText = stats.windGustsWarning,
                            tagColor = if (stats.windGusts > 65.0) Color(0xFFFF5252) else if (stats.windGusts > 50.0) Color(0xFFFF7043) else if (stats.windGusts > 30.0) Color(0xFFFFD54F) else Color(0xFF00FF87)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val visHintText = when {
                            stats.visibility < 0.0 -> "عمق دید: نامشخص"
                            stats.visibility >= 1000.0 -> {
                                val visKm = stats.visibility / 1000.0
                                val formatted = if (visKm % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", visKm) else String.format(java.util.Locale.US, "%.1f", visKm)
                                "عمق دید: ${PersianDateHelper.formatToPersianDigits(formatted)} کیلومتر"
                            }
                            else -> "عمق دید: ${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", stats.visibility))} متر"
                        }
                        AtmosphereGridTile(
                            icon = Icons.Default.Cloud,
                            title = "پوشش ابر و عمق دید",
                            value = if (stats.cloudCover < 0.0) "نامشخص" else "${PersianDateHelper.formatToPersianDigits(stats.cloudCover.toInt())}٪ پوشش",
                            hint = visHintText,
                            tagText = stats.cloudCoverWarning,
                            tagColor = if (stats.visibility in 0.0..1000.0) Color(0xFFFF5252) else if (stats.cloudCover < 0.0) Color(0xFF90A4AE) else if (stats.cloudCover > 65.0 || (stats.visibility in 1000.0..4000.0)) Color(0xFFFFD54F) else Color(0xFF00FF87)
                        )
                    }
                }

                // Row 5: Dew Point & CAPE Lightning potential
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val dewValAbs = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(stats.dewPoint)))
                        val dewSign = if (stats.dewPoint < 0) "-" else ""
                        AtmosphereGridTile(
                            icon = Icons.Default.Thermostat,
                            title = "نقطه شبنم تراز",
                            value = "\u200E$dewSign$dewValAbs°C",
                            hint = "تراز مایش سردترین نقطه",
                            tagText = stats.dewPointWarning,
                            tagColor = Color(android.graphics.Color.parseColor(stats.dewPointColorHex))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AtmosphereGridTile(
                            icon = Icons.Default.Bolt,
                            title = "پتانسیل صاعقه (CAPE)",
                            value = "${PersianDateHelper.formatToPersianDigits(stats.cape.toInt())} J/kg",
                            hint = "انرژی ناپایداری توفنده جو",
                            tagText = stats.capeWarning,
                            tagColor = Color(android.graphics.Color.parseColor(stats.capeColorHex))
                        )
                    }
                }

                // Row 6: Fresh Snowfall & Spread Spread indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val snowFormatted = if (stats.snowfall % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", stats.snowfall) else String.format(java.util.Locale.US, "%.1f", stats.snowfall)
                        AtmosphereGridTile(
                            icon = Icons.Default.AcUnit,
                            title = "بارش برف تازه",
                            value = "${PersianDateHelper.formatToPersianDigits(snowFormatted)} سانتی‌متر",
                            hint = "بارش برف تازه سطحی تراز",
                            tagText = stats.snowfallWarning,
                            tagColor = Color(android.graphics.Color.parseColor(stats.snowfallColorHex))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val spreadFormatted = String.format(java.util.Locale.US, "%.1f", stats.spread)
                        AtmosphereGridTile(
                            icon = Icons.Default.FilterDrama,
                            title = "فاصله اشباع مه",
                            value = "${PersianDateHelper.formatToPersianDigits(spreadFormatted)}°C",
                            hint = "اختلاف دما با نقطه شبنم تراز",
                            tagText = stats.spreadWarning,
                            tagColor = Color(android.graphics.Color.parseColor(stats.spreadColorHex))
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 📈 نمودار ۱۲ ساعته روند تغییرات فشار هوا و احتمال طوفان (Recharts style)
// ============================================================

data class HourlyPressureStormPoint(
    val hourLabel: String,
    val fullTime: String,
    val pressure: Double,
    val stormProbability: Int,
    val cape: Double,
    val windGusts: Double,
    val precipProb: Int,
    val weatherCode: Int,
    val p3hDrop: Double,
    val isDanger: Boolean,
    val isCaution: Boolean
)

@Composable
fun PressureStormChart(
    hourly: HourlyData?,
    altitude: Int,
    mountain: MountainEntity,
    current: com.example.data.remote.CurrentWeather,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vazirmatnTypeface = remember(context) {
        try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.vazirmatn_regular)
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
    }

    val isDark = MaterialTheme.colorScheme.background.isDark
    
    val offsetHours = remember(mountain.name, mountain.latitude, mountain.longitude) {
        com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
    }
    val currentHourIdx = remember(current, hourly, offsetHours) {
        MountaineeringHelper.findHourlyIndexForCurrent(current, hourly, offsetHours)
    }

    val points = remember(hourly, currentHourIdx, altitude, mountain) {
        if (hourly?.time == null || currentHourIdx < 0) emptyList()
        else {
            val list = mutableListOf<HourlyPressureStormPoint>()
            val total = hourly.time.size
            for (step in 0 until 12) {
                val idx = currentHourIdx + step
                if (idx >= total) break
                val rawTime = hourly.time[idx]
                val hourOnly = try {
                    val parts = rawTime.split("T")
                    val timeParts = parts.getOrNull(1)?.split(":")
                    val h = timeParts?.getOrNull(0)?.toIntOrNull() ?: 0
                    val formatted = String.format(java.util.Locale.US, "%02d:00", h)
                    PersianDateHelper.formatToPersianDigits(formatted)
                } catch (e: Exception) {
                    "${step + 1}h"
                }

                val rawTemp = hourly.temperature2m.getOrNull(idx) ?: current.temperature2m
                val diffAlt = mountain.altitude - altitude
                val adjTemp = rawTemp + (diffAlt * 0.0065)
                val baseP = hourly.surfacePressure?.getOrNull(idx)
                val qnhP = hourly.pressureMsl?.getOrNull(idx) ?: 1013.25
                val adjP = MountaineeringHelper.calculateBarometricPressure(
                    basePressure = baseP,
                    baseTemp = rawTemp,
                    baseAltitude = mountain.altitude,
                    targetAltitude = altitude,
                    targetTemp = adjTemp,
                    qnh = qnhP
                )

                val capeVal = hourly.cape?.getOrNull(idx) ?: 0.0
                val precipMm = hourly.precipitation?.getOrNull(idx) ?: 0.0
                val precipProb = hourly.precipitationProbability?.getOrNull(idx) ?: 0
                val cloudCoverVal = hourly.cloudCover?.getOrNull(idx) ?: 0.0
                val freezingLevelVal = hourly.freezingLevelHeight?.getOrNull(idx) ?: MountaineeringHelper.estimateFreezingLevel(adjTemp, altitude).toDouble()
                val code = hourly.weatherCode.getOrNull(idx) ?: 0
                val gusts = hourly.windGusts10m?.getOrNull(idx) ?: 0.0

                // Calculate 3-hour pressure drop (p3hDrop) using actual hourly index
                val p3Idx = (idx - 3).coerceAtLeast(0)
                val timeDiff = (idx - p3Idx).coerceAtLeast(1)
                val p3Temp = hourly.temperature2m.getOrNull(p3Idx) ?: current.temperature2m
                val p3BaseP = hourly.surfacePressure?.getOrNull(p3Idx)
                val p3QnhP = hourly.pressureMsl?.getOrNull(p3Idx) ?: 1013.25
                val p3AdjP = MountaineeringHelper.calculateBarometricPressure(
                    basePressure = p3BaseP,
                    baseTemp = p3Temp,
                    baseAltitude = mountain.altitude,
                    targetAltitude = altitude,
                    targetTemp = p3Temp + (diffAlt * 0.0065),
                    qnh = p3QnhP
                )
                val rawDrop = p3AdjP - adjP
                val actualP3hDrop = if (idx == p3Idx) 0.0 else rawDrop * (3.0 / timeDiff)
                val pointP3hDrop = actualP3hDrop.coerceAtLeast(0.0)

                // 100% Standardized Mountaineering Lightning/Storm Risk engine
                val baseLightningRisk = MountaineeringHelper.calculateLightningRisk(
                    cape = capeVal,
                    precipitation = precipMm,
                    cloudCover = cloudCoverVal,
                    freezingLevel = freezingLevelVal,
                    summitElevation = altitude.toDouble(),
                    weatherCode = code,
                    lightningPotential = null
                )

                val pressureDropBonus = when {
                    pointP3hDrop >= 3.0 -> 25
                    pointP3hDrop >= 2.0 -> 15
                    pointP3hDrop >= 1.0 -> 8
                    else -> 0
                }

                val stormPct = (baseLightningRisk + pressureDropBonus).coerceIn(0, 100)
                
                val isDanger = stormPct >= 60 || actualP3hDrop >= 2.0 || gusts >= 65.0 || capeVal >= 500.0
                val isCaution = stormPct >= 35 || actualP3hDrop >= 1.0 || gusts >= 45.0 || capeVal >= 150.0

                val finalRiskScore = when {
                    isDanger -> maxOf(stormPct, 65)
                    isCaution -> maxOf(stormPct, 40)
                    else -> stormPct
                }

                list.add(
                    HourlyPressureStormPoint(
                        hourLabel = hourOnly,
                        fullTime = hourOnly,
                        pressure = adjP,
                        stormProbability = finalRiskScore,
                        cape = capeVal,
                        windGusts = gusts,
                        precipProb = precipProb,
                        weatherCode = code,
                        p3hDrop = actualP3hDrop,
                        isDanger = isDanger,
                        isCaution = isCaution
                    )
                )
            }
            list
        }
    }

    if (points.isEmpty()) return

    val firstP = points.first().pressure
    val lastP = points.last().pressure
    val deltaP = lastP - firstP

    val isSevereStormRisk = points.any { it.isDanger } || deltaP <= -5.0
    val isModerateStormRisk = points.any { it.isCaution } || deltaP <= -2.5

    val alertMessage = when {
        deltaP <= -5.0 -> "⚠️ افت شدید فشار بارومتریک (هشدار ورود سیستم طوفان‌زا)"
        isSevereStormRisk -> "⚡ احتمال بالا برای وقوع صاعقه و ناپایداری‌های طوفنده در ۱۲ ساعت آینده"
        isModerateStormRisk -> "📉 ناپایداری احتمالی در ساعات آتی (احتیاط در صعود)"
        deltaP >= 2.0 -> "📈 افزایش فشار بارومتریک (ترسیم روند بهبود پایداری هوای قله)"
        else -> "⚖️ روند فشار جوی و پایداری هوا در ۱۲ ساعت آینده متعادل"
    }

    val alertColor = when {
        isSevereStormRisk -> Color(0xFFFF5252)
        isModerateStormRisk -> Color(0xFFFF9100)
        else -> Color(0xFF00FF87)
    }

    val headerBadgeText = when {
        isSevereStormRisk -> "هشدار طوفان (۱۲س) ⚡"
        isModerateStormRisk -> "احتیاط صعود (۱۲س) 🌩️"
        else -> "جو پایدار (۱۲س) ☀️"
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedPoint = points.getOrNull(selectedIndex) ?: points.first()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pressure_storm_chart"),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF00FFE0).copy(alpha = 0.2f) else Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Pressure Storm Chart",
                    tint = if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نمودار ۱۲ ساعته فشار هوا و طوفان",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTextColor(1f),
                    fontFamily = Vazirmatn,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge & Alert Message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100),
                    color = alertColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, alertColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = headerBadgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = alertColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontFamily = Vazirmatn
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alertMessage,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = alertColor,
                lineHeight = 15.sp,
                fontFamily = Vazirmatn
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Legends row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 3.dp)
                            .background(if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7), RoundedCornerShape(100))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "فشار بارومتریک (hPa)",
                        fontSize = 9.sp,
                        color = getTextColor(0.8f),
                        fontFamily = Vazirmatn
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 8.dp)
                            .background(Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "احتمال طوفان (٪)",
                        fontSize = 9.sp,
                        color = getTextColor(0.8f),
                        fontFamily = Vazirmatn
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Chart
            val pressureColor = if (isDark) Color(0xFF00FFE0) else Color(0xFF0284C7)
            val stormAreaTopColor = Color(0xFFFF5252).copy(alpha = 0.35f)
            val stormAreaBottomColor = Color(0xFFFF9100).copy(alpha = 0.05f)
            val gridLineColor = getTextColor(0.12f)
            val textColor = getTextColor(0.6f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val leftPadding = 35.dp.toPx()
                            val rightPadding = 35.dp.toPx()
                            val availableWidth = width - leftPadding - rightPadding
                            val stepWidth = availableWidth / (points.size - 1).coerceAtLeast(1)
                            val rawIndex = ((offset.x - leftPadding) / stepWidth).roundToInt()
                            selectedIndex = rawIndex.coerceIn(0, points.size - 1)
                        }
                    }
                    .pointerInput(points) {
                        detectDragGestures { change, _ ->
                            val width = size.width
                            val leftPadding = 35.dp.toPx()
                            val rightPadding = 35.dp.toPx()
                            val availableWidth = width - leftPadding - rightPadding
                            val stepWidth = availableWidth / (points.size - 1).coerceAtLeast(1)
                            val rawIndex = ((change.position.x - leftPadding) / stepWidth).roundToInt()
                            selectedIndex = rawIndex.coerceIn(0, points.size - 1)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 35.dp.toPx()
                    val rightPadding = 35.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    val minP = (points.minOf { it.pressure }) - 1.2
                    val maxP = (points.maxOf { it.pressure }) + 1.2
                    val pRange = (maxP - minP).coerceAtLeast(1.0)

                    // Draw Horizontal Grid Lines & Y-Axis values
                    val gridSteps = 3
                    for (g in 0..gridSteps) {
                        val fraction = g / gridSteps.toFloat()
                        val y = topPadding + chartHeight * (1 - fraction)
                        
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Left Y-Axis: Pressure (hPa)
                        val pVal = minP + pRange * fraction
                        val pText = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", pVal))
                        drawContext.canvas.nativeCanvas.drawText(
                            pText,
                            leftPadding - 6.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textColor.toArgb()
                                textSize = 8.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                typeface = vazirmatnTypeface
                            }
                        )

                        // Right Y-Axis: Storm Probability (%)
                        val stormVal = (fraction * 100).toInt()
                        val stormText = "${PersianDateHelper.formatToPersianDigits(stormVal)}٪"
                        drawContext.canvas.nativeCanvas.drawText(
                            stormText,
                            width - rightPadding + 6.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textColor.toArgb()
                                textSize = 8.sp.toPx()
                                textAlign = android.graphics.Paint.Align.LEFT
                                typeface = vazirmatnTypeface
                            }
                        )
                    }

                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                    // 1. Render Storm Probability Area (Right Axis: 0..100)
                    val stormPath = Path()
                    val stormFillPath = Path()

                    for (i in points.indices) {
                        val p = points[i]
                        val x = leftPadding + i * stepX
                        val stormFrac = p.stormProbability / 100f
                        val y = topPadding + chartHeight * (1 - stormFrac)

                        if (i == 0) {
                            stormPath.moveTo(x, y)
                            stormFillPath.moveTo(x, topPadding + chartHeight)
                            stormFillPath.lineTo(x, y)
                        } else {
                            val prevX = leftPadding + (i - 1) * stepX
                            val prevStormFrac = points[i - 1].stormProbability / 100f
                            val prevY = topPadding + chartHeight * (1 - prevStormFrac)

                            val controlX1 = prevX + (x - prevX) / 2f
                            val controlX2 = prevX + (x - prevX) / 2f
                            stormPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            stormFillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }

                        if (i == points.size - 1) {
                            stormFillPath.lineTo(x, topPadding + chartHeight)
                            stormFillPath.close()
                        }
                    }

                    drawPath(
                        path = stormFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(stormAreaTopColor, stormAreaBottomColor),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )

                    drawPath(
                        path = stormPath,
                        color = Color(0xFFFF5252).copy(alpha = 0.7f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 2. Render Pressure Line (Left Axis: minP..maxP)
                    val pressurePath = Path()
                    val pointOffsets = mutableListOf<Offset>()

                    for (i in points.indices) {
                        val p = points[i]
                        val x = leftPadding + i * stepX
                        val pFrac = ((p.pressure - minP) / pRange).toFloat()
                        val y = topPadding + chartHeight * (1 - pFrac)
                        val pos = Offset(x, y)
                        pointOffsets.add(pos)

                        if (i == 0) {
                            pressurePath.moveTo(x, y)
                        } else {
                            val prevX = pointOffsets[i - 1].x
                            val prevY = pointOffsets[i - 1].y
                            val controlX1 = prevX + (x - prevX) / 2f
                            val controlX2 = prevX + (x - prevX) / 2f
                            pressurePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }
                    }

                    drawPath(
                        path = pressurePath,
                        color = pressureColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 3. Draw Circles & Highlights
                    for (i in points.indices) {
                        val pos = pointOffsets[i]
                        val isSelected = i == selectedIndex

                        // Time label under X-axis
                        val hourStr = points[i].hourLabel
                        if (i % 2 == 0 || isSelected) {
                            drawContext.canvas.nativeCanvas.drawText(
                                hourStr,
                                pos.x,
                                height - 4.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = (if (isSelected) pressureColor else textColor).toArgb()
                                    textSize = 8.5.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = vazirmatnTypeface
                                    if (isSelected) isFakeBoldText = true
                                }
                            )
                        }

                        if (isSelected) {
                            // Vertical guide line for selected hour
                            drawLine(
                                color = pressureColor.copy(alpha = 0.5f),
                                start = Offset(pos.x, topPadding),
                                end = Offset(pos.x, topPadding + chartHeight),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Halo circle
                            drawCircle(
                                color = pressureColor.copy(alpha = 0.3f),
                                radius = 7.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = pressureColor,
                                radius = 4.dp.toPx(),
                                center = pos
                            )
                        } else {
                            drawCircle(
                                color = pressureColor,
                                radius = 2.5.dp.toPx(),
                                center = pos
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Hour Detail Tooltip Box (2x2 Grid + Advisory)
            val formatP = String.format(java.util.Locale.US, "%.1f", selectedPoint.pressure)
            val pDiffFromStart = selectedPoint.pressure - points.first().pressure
            val diffStr = if (pDiffFromStart >= 0) "+${String.format(java.util.Locale.US, "%.1f", pDiffFromStart)}" else String.format(java.util.Locale.US, "%.1f", pDiffFromStart)

            val p3hDrop = selectedPoint.p3hDrop
            val p3hTrendStr = when {
                p3hDrop > 0.1 -> "📉 -${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", p3hDrop))} hPa"
                p3hDrop < -0.1 -> "📈 +${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", -p3hDrop))} hPa"
                else -> "⚖️ بدون تغییر"
            }

            val isHourDanger = selectedPoint.isDanger
            val isHourCaution = selectedPoint.isCaution

            val stormRiskBadgeText = when {
                isHourDanger -> "خطر طوفان ⚡"
                isHourCaution -> "احتیاط صعود 🌩️"
                else -> "شرایط پایدار ☀️"
            }
            val stormRiskBadgeColor = when {
                isHourDanger -> Color(0xFFFF5252)
                isHourCaution -> Color(0xFFFF9100)
                else -> Color(0xFF00FF87)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF101726) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF00FFE0).copy(alpha = 0.25f) else Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Header (Time & Storm Status)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pressureColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ساعت ${selectedPoint.hourLabel}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pressureColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضعیت جوی",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = getTextColor(0.7f),
                                fontFamily = Vazirmatn,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(100),
                            color = stormRiskBadgeColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, stormRiskBadgeColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = stormRiskBadgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = stormRiskBadgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontFamily = Vazirmatn,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2x2 Grid Layout with IntrinsicSize.Max for identical cell heights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Pressure
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF182236) else Color.White,
                            border = BorderStroke(1.dp, getTextColor(0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "فشار بارومتریک",
                                    fontSize = 9.sp,
                                    color = getTextColor(0.6f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${PersianDateHelper.formatToPersianDigits(formatP)} hPa",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pressureColor,
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "تغییر ۳h: $p3hTrendStr",
                                    fontSize = 8.5.sp,
                                    color = getTextColor(0.75f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                            }
                        }

                        // Card 2: Storm & Rain
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF182236) else Color.White,
                            border = BorderStroke(1.dp, getTextColor(0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "طوفان و بارش",
                                    fontSize = 9.sp,
                                    color = getTextColor(0.6f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${PersianDateHelper.formatToPersianDigits(selectedPoint.stormProbability)}٪ طوفان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stormRiskBadgeColor,
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "احتمال بارش: ${PersianDateHelper.formatToPersianDigits(selectedPoint.precipProb)}٪",
                                    fontSize = 8.5.sp,
                                    color = getTextColor(0.75f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 3: Wind Gusts
                        val gustsVal = selectedPoint.windGusts.toInt()
                        val gustsColor = when {
                            gustsVal >= 65 -> Color(0xFFFF5252)
                            gustsVal >= 45 -> Color(0xFFFF9100)
                            else -> getTextColor(0.9f)
                        }
                        val gustsStatusStr = when {
                            gustsVal >= 65 -> "طوفانی و خطرناک"
                            gustsVal >= 45 -> "تندباد شدید"
                            else -> "سرعت عادی باد"
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF182236) else Color.White,
                            border = BorderStroke(1.dp, getTextColor(0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "سرعت تندباد (Gusts)",
                                    fontSize = 9.sp,
                                    color = getTextColor(0.6f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${PersianDateHelper.formatToPersianDigits(gustsVal)} km/h",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = gustsColor,
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "وضعیت: $gustsStatusStr",
                                    fontSize = 8.5.sp,
                                    color = getTextColor(0.75f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                            }
                        }

                        // Card 4: CAPE Index
                        val capeVal = selectedPoint.cape.toInt()
                        val capeColor = when {
                            capeVal >= 500 -> Color(0xFFFF5252)
                            capeVal >= 150 -> Color(0xFFFF9100)
                            else -> getTextColor(0.9f)
                        }
                        val capeStatusStr = when {
                            capeVal >= 500 -> "ریسک بالا صاعقه"
                            capeVal >= 150 -> "ناپایدار موضعی"
                            else -> "جو کاملاً پایدار"
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF182236) else Color.White,
                            border = BorderStroke(1.dp, getTextColor(0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "شاخص ناپایداری CAPE",
                                    fontSize = 9.sp,
                                    color = getTextColor(0.6f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${PersianDateHelper.formatToPersianDigits(capeVal)} J/kg",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = capeColor,
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                                Text(
                                    text = "وضعیت: $capeStatusStr",
                                    fontSize = 8.5.sp,
                                    color = getTextColor(0.75f),
                                    fontFamily = Vazirmatn,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Mountaineering Safety Advisory Banner
                    Spacer(modifier = Modifier.height(10.dp))
                    val hourAdvisory = when {
                        isHourDanger -> 
                            "⛔ هشدار صعود: جو بسیار ناپایدار است. صعود به خط‌الرأس در این ساعت خطر صاعقه و سقوط دارد."
                        isHourCaution -> 
                            "⚠️ احتیاط کوهنوردی: احتمال پدیده‌های ناگهانی جوی یا افزایش باد وجود دارد. پناهگاه‌ها را مدنظر داشته باشید."
                        else -> 
                            "✅ شرایط مساعد کوهنوردی: فشار جو متعادل و ریسک طوفان در این ساعت پایین است."
                    }

                    val advisoryBg = stormRiskBadgeColor.copy(alpha = 0.12f)
                    val advisoryTxtColor = when {
                        isHourDanger -> Color(0xFFFF5252)
                        isHourCaution -> Color(0xFFFF9100)
                        else -> Color(0xFF00E676)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = advisoryBg
                    ) {
                        Text(
                            text = hourAdvisory,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = advisoryTxtColor,
                            lineHeight = 14.sp,
                            fontFamily = Vazirmatn,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtmosphereGridTile(
    icon: ImageVector,
    title: String,
    value: String,
    hint: String,
    tagText: String,
    tagColor: Color

) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val finalTagColor = remember(tagColor, isDark) {
        if (isDark) {
            tagColor
        } else {
            val r = tagColor.red
            val g = tagColor.green
            val b = tagColor.blue
            when {
                // Neon Cyan / Ice Blue -> Deep Teal
                g > 0.75f && b > 0.85f && r < 0.2f -> Color(0xFF006D77)
                // Neon Green -> Forest Green
                g > 0.85f && r < 0.2f -> Color(0xFF15803D)
                // Yellow / Bright Amber -> Dark Gold/Orange
                r > 0.85f && g > 0.75f && b < 0.4f -> Color(0xFFB45309)
                // Orange -> Dark Amber/Red-Orange
                r > 0.85f && g > 0.5f && b < 0.2f -> Color(0xFFC2410C)
                // Red / Pink-Red -> Dark Crimson
                r > 0.85f && g < 0.4f && b < 0.4f -> Color(0xFFB91C1C)
                // Soft Silver Slate
                r > 0.5f && g > 0.6f && b > 0.61f && r < 0.8f -> Color(0xFF374151)
                else -> {
                    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    if (luminance > 0.6f) {
                        Color(r * 0.5f, g * 0.5f, b * 0.5f, tagColor.alpha)
                    } else {
                        tagColor
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 138.dp),
        shape = RoundedCornerShape(18.dp),
        border = getCardBorderStroke(),
        colors = CardDefaults.cardColors(
            containerColor = getCardBgColor(Color(0xFF0C101B))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // High-fidelity status visual accent strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.5.dp)
                    .background(finalTagColor.copy(alpha = 0.85f))
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = finalTagColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(0.55f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = getTextColor(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = hint,
                        fontSize = 9.sp,
                        color = getTextColor(0.4f),
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(100),
                    color = finalTagColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, finalTagColor.copy(alpha = 0.15f)),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = tagText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = finalTagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class DailySunStats(
    val sunrisePersian: String,
    val sunsetPersian: String,
    val totalDaylightPersian: String,
    val alpineStartPersian: String,
    val alpineEndPersian: String
)

data class MoonStats(
    val phaseName: String,
    val phaseSymbol: String,
    val illuminationPercent: Double,
    val moonrise: String,
    val moonset: String,
    val isInSky: Boolean,
    val stateText: String,
    val intensity: String,
    val intensityColor: Color,
    val isValid: Boolean = true,
    val ageDays: Double = 0.0,
    val effectiveIlluminationPercent: Double = 0.0,
    val currentAltitude: Double = 0.0
)

data class DaylightReport(
    val status: String,
    val message: String,
    val color: Color,
    val isDaylight: Boolean
)

data class AlpineWindowReport(
    val status: String,
    val message: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val currentProgress: Float,
    val totalDurationText: String,
    val isInside: Boolean
)

@Composable
fun MoonPhaseCanvas(
    illuminationPercent: Int,
    ageDays: Double,
    phaseName: String = "",
    modifier: Modifier = Modifier
) {
    val isWaxing = if (ageDays > 0.0) {
        ageDays < 14.76
    } else {
        !phaseName.contains("کاهنده") && !phaseName.contains("آخر")
    }
    val illumination = (illuminationPercent / 100f).coerceIn(0f, 1f)
    com.example.ui.weather.MoonPhaseViewCompose(
        illumination = illumination,
        isWaxing = isWaxing,
        ageDays = ageDays,
        modifier = modifier
    )
}

private fun isHourInInterval(h: Double, start: Double, end: Double): Boolean {
    return if (start <= end) {
        h >= start && h < end
    } else {
        h >= start || h < end
    }
}

@Composable
fun MountaineeringStatsSection(
    current: com.example.data.remote.CurrentWeather,
    altitude: Int,
    daily: com.example.data.remote.DailyData?,
    hourly: com.example.data.remote.HourlyData?,
    mountain: com.example.data.local.MountainEntity,
    apiUtcOffsetSeconds: Int? = null
) {
    val isDark = MaterialTheme.colorScheme.background.isDark

    // Central astronomical calculations based on selected peak's coordinates, elevation and calculated peak local offset.
    // Prefer the DST-aware offset from the Open-Meteo API response; fall back to the standard-time estimate.
    val peakOffsetHours = com.example.ui.util.AstronomicalCalculator.resolvePeakOffset(
        apiUtcOffsetSeconds = apiUtcOffsetSeconds,
        name = mountain.name,
        latitude = mountain.latitude,
        longitude = mountain.longitude
    )
    
    var statsTick by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            statsTick = System.currentTimeMillis() / 60000L
        }
    }

    val currentHourUTC = remember(statsTick) {
        val nowCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        nowCal.get(java.util.Calendar.HOUR_OF_DAY) + nowCal.get(java.util.Calendar.MINUTE) / 60.0
    }

    val statsPeakLocalTimeText = remember(mountain, currentHourUTC, apiUtcOffsetSeconds) {
        try {
            val peakOffset = com.example.ui.util.AstronomicalCalculator.resolvePeakOffset(
                apiUtcOffsetSeconds = apiUtcOffsetSeconds,
                name = mountain.name,
                latitude = mountain.latitude,
                longitude = mountain.longitude
            )
            val localHour = (currentHourUTC + peakOffset + 24.0) % 24.0
            val formatted = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localHour)
            val persianTime = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formatted)
            val offsetSign = if (peakOffset >= 0) "+" else ""
            val formattedOffset = String.format(java.util.Locale.US, "%.1f", peakOffset)
            val offsetPersian = "\u200E$offsetSign" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedOffset)
            "ساعت محلی قله: $persianTime (UTC$offsetPersian)"
        } catch (e: Exception) {
            "خطا در محاسبه ساعت"
        }
    }
    
    val mountainLocalCalendar = remember(peakOffsetHours, statsTick) {
        val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
        val localTimeMillis = System.currentTimeMillis() + offsetMillis
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localTimeMillis
        }
    }

    val todayNoonUTC = remember(mountainLocalCalendar) {
        try {
            val yr = mountainLocalCalendar.get(java.util.Calendar.YEAR)
            val mn = mountainLocalCalendar.get(java.util.Calendar.MONTH)
            val dy = mountainLocalCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            
            val noonCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            noonCal.set(yr, mn, dy, 12, 0, 0)
            noonCal.set(java.util.Calendar.MILLISECOND, 0)
            noonCal.time
        } catch (e: Exception) {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.time
        }
    }

    val sunTimesUTC = remember(mountain, altitude, todayNoonUTC) {
        try {
            com.example.ui.util.AstronomicalCalculator.calculateSunriseSunsetUTC(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = todayNoonUTC
            )
        } catch (e: Exception) {
            null
        }
    }

    val twilightTimesUTC = remember(mountain, altitude, todayNoonUTC) {
        try {
            com.example.ui.util.AstronomicalCalculator.calculateTwilightTimesUTC(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = todayNoonUTC
            )
        } catch (e: Exception) {
            null
        }
    }

    val nauticalDawnLocal = remember(twilightTimesUTC, peakOffsetHours) {
        twilightTimesUTC?.nauticalDawnUTC?.let { ((it + peakOffsetHours) % 24.0 + 24.0) % 24.0 }
    }
    val nauticalDawnPersian = remember(nauticalDawnLocal) {
        nauticalDawnLocal?.let {
            "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(
                com.example.ui.util.AstronomicalCalculator.formatFractionalHour(it)
            )
        } ?: "--:--"
    }

    val sunriseLocal = remember(sunTimesUTC, peakOffsetHours) {
        if (sunTimesUTC != null && !sunTimesUTC.isAlwaysBelow && !sunTimesUTC.isAlwaysAbove) {
            val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
            ((riseUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
        } else {
            6.0
        }
    }

    val sunsetLocal = remember(sunTimesUTC, peakOffsetHours) {
        if (sunTimesUTC != null && !sunTimesUTC.isAlwaysBelow && !sunTimesUTC.isAlwaysAbove) {
            val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
            ((setUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
        } else {
            18.0
        }
    }

    val alpineStartLocal = remember(sunriseLocal) {
        (sunriseLocal - 3.0 + 24.0) % 24.0
    }

    val alpineEndLocal = remember(sunsetLocal) {
        (sunsetLocal - 6.0 + 24.0) % 24.0
    }

    val currentHourLocal = remember(currentHourUTC, peakOffsetHours) {
        ((currentHourUTC + peakOffsetHours + 24.0) % 24.0).toFloat()
    }
    
    val calculationPrecisionText = if (sunTimesUTC != null) "دقیق (NOAA)" else "تخمینی (خطا)"
    val calculationPrecisionColor = if (sunTimesUTC != null) Color(0xFF00FF87) else Color(0xFFFFD54F)

    val isCurrentlyDaylight = remember(sunTimesUTC, currentHourUTC) {
        if (sunTimesUTC != null) {
            if (sunTimesUTC.isAlwaysAbove) {
                true
            } else if (sunTimesUTC.isAlwaysBelow) {
                false
            } else {
                val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
                val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
                if (riseUTC < setUTC) {
                    currentHourUTC >= riseUTC && currentHourUTC < setUTC
                } else {
                    currentHourUTC >= riseUTC || currentHourUTC < setUTC
                }
            }
        } else {
            val now = java.util.Calendar.getInstance()
            val localHour = now.get(java.util.Calendar.HOUR_OF_DAY) + now.get(java.util.Calendar.MINUTE) / 60.0
            localHour >= 6.0 && localHour < 18.0
        }
    }

    val sunStats = remember(sunTimesUTC, peakOffsetHours) {
        if (sunTimesUTC != null && !sunTimesUTC.isAlwaysBelow && !sunTimesUTC.isAlwaysAbove) {
            val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
            val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
            
            val peakSunriseHour = ((riseUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
            val peakSunsetHour = ((setUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
            
            val totalDaylightHours = if (setUTC > riseUTC) setUTC - riseUTC else (setUTC + 24.0) - riseUTC
            val daylightH = totalDaylightHours.toInt()
            val daylightM = Math.round((totalDaylightHours - daylightH) * 60.0).toInt() % 60
            
            val peakAlpineStartHour = (peakSunriseHour - 2.0 + 24.0) % 24.0
            val peakAlpineEndHour = (peakSunsetHour - 4.0 + 24.0) % 24.0
            
            val formattedSunrise = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakSunriseHour)
            val formattedSunset = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakSunsetHour)
            val formattedAlpineStart = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakAlpineStartHour)
            val formattedAlpineEnd = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakAlpineEndHour)
            
            val alpineStartP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedAlpineStart)
            val alpineEndP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedAlpineEnd)
            val sunriseP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedSunrise)
            val sunsetP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedSunset)
            val totalDaylightP = "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(daylightH)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(daylightM)} دقیقه"
            
            DailySunStats(
                sunrisePersian = sunriseP,
                sunsetPersian = sunsetP,
                totalDaylightPersian = totalDaylightP,
                alpineStartPersian = alpineStartP,
                alpineEndPersian = alpineEndP
            )
        } else {
            if (sunTimesUTC?.isAlwaysAbove == true) {
                DailySunStats(
                    sunrisePersian = "روشن دائم",
                    sunsetPersian = "روشن دائم",
                    totalDaylightPersian = "۲۴ ساعت روشنایی مداوم قطبی",
                    alpineStartPersian = "شروع دلخواه",
                    alpineEndPersian = "پایان دلخواه"
                )
            } else if (sunTimesUTC?.isAlwaysBelow == true) {
                DailySunStats(
                    sunrisePersian = "تاریک دائم",
                    sunsetPersian = "تاریک دائم",
                    totalDaylightPersian = "۰ ساعت (تاریکی مداوم قطبی)",
                    alpineStartPersian = "غیرقابل اجرا",
                    alpineEndPersian = "غیرقابل اجرا"
                )
            } else {
                DailySunStats(
                    sunrisePersian = "خطا",
                    sunsetPersian = "خطا",
                    totalDaylightPersian = "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید.",
                    alpineStartPersian = "خطا",
                    alpineEndPersian = "خطا"
                )
            }
        }
    }

    val sunrisePersian = sunStats.sunrisePersian
    val sunsetPersian = sunStats.sunsetPersian
    val totalDaylightPersian = sunStats.totalDaylightPersian
    val alpineStartPersian = sunStats.alpineStartPersian
    val alpineEndPersian = sunStats.alpineEndPersian

    val currentProgressPercent = remember(sunTimesUTC, currentHourUTC, isCurrentlyDaylight) {
        if (sunTimesUTC != null && !sunTimesUTC.isAlwaysBelow && !sunTimesUTC.isAlwaysAbove) {
            val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
            val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
            if (isCurrentlyDaylight) {
                val totalDayUTC = if (setUTC > riseUTC) setUTC - riseUTC else (setUTC + 24.0) - riseUTC
                val elapsedUTC = if (currentHourUTC >= riseUTC) currentHourUTC - riseUTC else (currentHourUTC + 24.0) - riseUTC
                (elapsedUTC / totalDayUTC).toFloat().coerceIn(0.0f, 1.0f)
            } else {
                0.0f
            }
        } else {
            0.5f
        }
    }

    val daylightReport = remember(sunTimesUTC, currentHourUTC, isCurrentlyDaylight, isDark) {
        if (sunTimesUTC == null) {
            return@remember DaylightReport(
                status = "خطا در پایش",
                message = "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید.",
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                isDaylight = false
            )
        }
        
        if (sunTimesUTC.isAlwaysAbove) {
            return@remember DaylightReport(
                status = "روز قطبی مداوم",
                message = "خورشید در این تاریخ ۲۴ ساعته بالای افق است. روشنایی دائم برقرار است.",
                color = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D),
                isDaylight = true
            )
        }
        if (sunTimesUTC.isAlwaysBelow) {
            return@remember DaylightReport(
                status = "شب قطبی مداوم",
                message = "خورشید در این تاریخ طلوع نمی‌کند. تاریکی دائم برقرار است.",
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                isDaylight = false
            )
        }

        val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
        val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
        
        if (isCurrentlyDaylight) {
            val remainingHours = if (currentHourUTC < setUTC) {
                setUTC - currentHourUTC
            } else {
                (setUTC + 24.0) - currentHourUTC
            }
            val h = remainingHours.toInt()
            val m = Math.round((remainingHours - h) * 60.0).toInt() % 60
            val timeStr = if (h > 0) "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(h)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(m)} دقیقه" else "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(m)} دقیقه"
            
            val shadowWarning = if (remainingHours <= 1.0) {
                "\n⚠️ سایه‌اندازی دره‌ای/ارتفاعات (Early Shadowing): نور مستقیم خورشید ۳۰ تا ۶۰ دقیقه زودتر از غروب نجومی به علت سایه دیواره‌ها و دره‌ها از دست می‌رود. سیستم روشنایی را آماده کنید."
            } else ""

            val advice = if (remainingHours < 3.0) {
                "⚠️ هشدار حساس: پنجره آلپاین رو به پایان است! کمتر از ۳ ساعت تا اتمام کامل روز وقت دارید، سریعاً به سمت پناهگاه ایمن حرکت کنید.$shadowWarning"
            } else {
                "✅ وضعیت مساعد: روشنایی اتمسفر فوقانی جهت پیمایش استاندارد کاملاً ایده آل است.$shadowWarning"
            }
            
            DaylightReport(
                status = "روشنایی روز برقرار است",
                message = "$timeStr تا غروب آفتاب و تاریک شدن جبهه صعود باقی مانده است.\n$advice",
                color = if (remainingHours < 3.0) {
                    if (isDark) Color(0xFFFF9100) else Color(0xFFC2410C)
                } else {
                    if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                },
                isDaylight = true
            )
        } else {
            val remainingHours = if (currentHourUTC < riseUTC) {
                riseUTC - currentHourUTC
            } else {
                (riseUTC + 24.0) - currentHourUTC
            }
            val h = remainingHours.toInt()
            val m = Math.round((remainingHours - h) * 60.0).toInt() % 60
            val timeStr = if (h > 0) "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(h)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(m)} دقیقه" else "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(m)} دقیقه"
            
            val isPostSunset = if (riseUTC < setUTC) currentHourUTC >= setUTC else (currentHourUTC >= setUTC && currentHourUTC < riseUTC)
            val nightStatus = if (isPostSunset) "تاریکی شب (پس از غروب آفتاب)" else "تاریکی شب (پیش از طلوع آفتاب)"

            DaylightReport(
                status = nightStatus,
                message = "آفتاب غروب کرده و تاریکی شب حاکم است. $timeStr تا شروع روشنایی روز و جلای جبهه صعود باقی مانده است.",
                color = if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309),
                isDaylight = false
            )
        }
    }

    val currentWindSpeed = current.windSpeed10m ?: 0.0
    val currentWindGusts = current.windGusts10m ?: 0.0
    val currentTemp = current.temperature2m ?: 0.0
    val isWeatherHazardInAlpine = currentWindSpeed >= 38.0 || currentWindGusts >= 55.0 || currentTemp <= -20.0

    val alpineReport = remember(sunTimesUTC, currentHourUTC, isDark, isWeatherHazardInAlpine, currentWindSpeed, currentTemp) {
        if (sunTimesUTC == null || sunTimesUTC.isAlwaysBelow || sunTimesUTC.isAlwaysAbove) {
            val errMsg = if (sunTimesUTC == null) "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید." else "محاسبات فنی به علت موقعیت قطبی در دسترس نیست."
            return@remember AlpineWindowReport(
                status = "خطا در پایش بازه آلپاین",
                message = errMsg,
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                icon = Icons.Default.Info,
                currentProgress = 0.0f,
                totalDurationText = "--",
                isInside = false
            )
        }
        
        val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
        val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
        
        val alpineStartUTC = (riseUTC - 2.0 + 24.0) % 24.0
        val alpineEndUTC = (setUTC - 4.0 + 24.0) % 24.0
        
        val totalAlpineHours = if (alpineEndUTC > alpineStartUTC) alpineEndUTC - alpineStartUTC else (alpineEndUTC + 24.0) - alpineStartUTC
        val durationH = totalAlpineHours.toInt()
        val durationM = Math.round((totalAlpineHours - durationH) * 60.0).toInt() % 60
        val totalAlpineDurationPersian = "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(durationH)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(durationM)} دقیقه"
        
        val isInside = if (alpineStartUTC < alpineEndUTC) {
            currentHourUTC >= alpineStartUTC && currentHourUTC < alpineEndUTC
        } else {
            currentHourUTC >= alpineStartUTC || currentHourUTC < alpineEndUTC
        }
        
        val progress = if (isInside) {
            val elapsed = if (currentHourUTC >= alpineStartUTC) currentHourUTC - alpineStartUTC else (currentHourUTC + 24.0) - alpineStartUTC
            (elapsed / totalAlpineHours).toFloat().coerceIn(0.0f, 1.0f)
        } else {
            val distToStart = if (currentHourUTC < alpineStartUTC) alpineStartUTC - currentHourUTC else (alpineStartUTC + 24.0) - currentHourUTC
            val distFromEnd = if (currentHourUTC >= alpineEndUTC) currentHourUTC - alpineEndUTC else (alpineEndUTC + 24.0) - alpineEndUTC
            if (distToStart < distFromEnd) 0.0f else 1.0f
        }
        
        var status: String
        var message: String
        var color: Color
        val icon = if (!isInside) {
            val distToStart = if (currentHourUTC < alpineStartUTC) alpineStartUTC - currentHourUTC else (alpineStartUTC + 24.0) - currentHourUTC
            if (distToStart < 12.0) Icons.Default.Timer else Icons.Default.Warning
        } else if (isWeatherHazardInAlpine) {
            Icons.Default.Warning
        } else {
            Icons.Default.CheckCircle
        }
        
        if (isInside) {
            val remainingHours = if (currentHourUTC < alpineEndUTC) alpineEndUTC - currentHourUTC else (alpineEndUTC + 24.0) - alpineStartUTC
            val rh = remainingHours.toInt()
            val rm = Math.round((remainingHours - rh) * 60.0).toInt() % 60
            val timeP = if (rh > 0) "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rh)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه" else "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه"
            
            if (isWeatherHazardInAlpine) {
                val pWind = com.example.ui.util.PersianDateHelper.formatToPersianDigits(currentWindSpeed.toInt())
                val pTemp = com.example.ui.util.PersianDateHelper.formatToPersianDigits(currentTemp.toInt())
                status = "پنجره نوری فعال اما مخاطره‌آمیز (طوفان/باد)"
                message = "⚠️ عدم تطابق شرایط جوی با پنجره آلپاین: اگرچه از نظر زمان نوری داخل پنجره ایمن هستید، اما به دلیل باد شدید ($pWind ک‌م/س) و سرمای هوا ($pTemp°C)، صعود پرخطر است. صعود پیشنهاد نمی‌شود!"
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
            } else {
                status = "داخل پنجره طلایی (ایمن)"
                message = "$timeP تا پایان پنجره طلایی و مهلت فرود ایمن باقی‌مانده است. بهترین و ایمن‌ترین بازه نوری اتمسفر جهت دستیابی به قله و بازگشت متعادل برقرار می‌باشد."
                color = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
            }
        } else {
            val distToStart = if (currentHourUTC < alpineStartUTC) alpineStartUTC - currentHourUTC else (alpineStartUTC + 24.0) - currentHourUTC
            if (distToStart < 12.0) {
                val rh = distToStart.toInt()
                val rm = Math.round((distToStart - rh) * 60.0).toInt() % 60
                val timeP = if (rh > 0) "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rh)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه" else "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه"
                
                status = "قبل از پنجره طلایی"
                message = "بازه صعود آلپاین هنوز شروع نشده است. $timeP تا آغاز پنجره طلایی آلپاین باقی مانده است. زمان باقی‌مانده را صرف آمادگی تجهیزات فنی، مسیریابی نهایی و هیدراتاسیون کنید."
                color = if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
            } else {
                val distFromEnd = if (currentHourUTC >= alpineEndUTC) currentHourUTC - alpineEndUTC else (alpineEndUTC + 24.0) - alpineEndUTC
                val rh = distFromEnd.toInt()
                val rm = Math.round((distFromEnd - rh) * 60.0).toInt() % 60
                val timeP = if (rh > 0) "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rh)} ساعت و \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه" else "\u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(rm)} دقیقه"
                
                status = "خروج اضطراری (بازه تاریک)"
                message = "هشدار جدی ایمنی: $timeP از زمان نهایی دوربرگردان آلپاین سپری شده است! جبهه قله رو به سرما، صعود شبانه دشوار، بادهای شدید جبهه‌ای و تاریکی اتمسفر می‌رود. همین حالا ارتفاع کم کنید."
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
            }
        }
        
        AlpineWindowReport(
            status = status,
            message = message,
            color = color,
            icon = icon,
            currentProgress = progress,
            totalDurationText = totalAlpineDurationPersian,
            isInside = isInside
        )
    }

    val moonDetails = remember(mountain, altitude, todayNoonUTC, statsTick) {
        try {
            val isTodaySelected = try {
                val nowUTC = java.util.Date()
                val mountainTimeMs = nowUTC.time + (peakOffsetHours * 3600000.0).toLong()
                
                val calMountainNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                calMountainNow.timeInMillis = mountainTimeMs
                
                val calTarget = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                calTarget.time = todayNoonUTC
                
                calMountainNow.get(java.util.Calendar.YEAR) == calTarget.get(java.util.Calendar.YEAR) &&
                calMountainNow.get(java.util.Calendar.DAY_OF_YEAR) == calTarget.get(java.util.Calendar.DAY_OF_YEAR)
            } catch (e: Exception) {
                true
            }
            
            val calculationDate = if (isTodaySelected) {
                java.util.Date()
            } else {
                val nowCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                val targetCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    time = todayNoonUTC
                    set(java.util.Calendar.HOUR_OF_DAY, nowCal.get(java.util.Calendar.HOUR_OF_DAY))
                    set(java.util.Calendar.MINUTE, nowCal.get(java.util.Calendar.MINUTE))
                    set(java.util.Calendar.SECOND, nowCal.get(java.util.Calendar.SECOND))
                    set(java.util.Calendar.MILLISECOND, nowCal.get(java.util.Calendar.MILLISECOND))
                }
                targetCal.time
            }

            val details = com.example.ui.util.AstronomicalCalculator.getLunarDetails(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = calculationDate,
                tzOffset = peakOffsetHours
            )
            
            val stableMoonTimes = com.example.ui.util.AstronomicalCalculator.calculateMoonriseSunsetUTC(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = todayNoonUTC,
                tzOffset = peakOffsetHours
            )
            
            val riseUTC = stableMoonTimes.moonriseUTC
            val setUTC = stableMoonTimes.moonsetUTC
            
            val localRise = riseUTC?.let { ((it + peakOffsetHours) % 24.0 + 24.0) % 24.0 }
            val localSet = setUTC?.let { ((it + peakOffsetHours) % 24.0 + 24.0) % 24.0 }
            
            val riseStr = if (stableMoonTimes.isAlwaysAbove) {
                "دائم"
            } else if (stableMoonTimes.isAlwaysBelow) {
                "--:--"
            } else {
                com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localRise)
            }
            val setStr = if (stableMoonTimes.isAlwaysAbove) {
                "دائم"
            } else if (stableMoonTimes.isAlwaysBelow) {
                "--:--"
            } else {
                com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localSet)
            }
            
            val riseStrPersian = if (riseStr == "دائم" || riseStr.contains("-")) riseStr else "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(riseStr)
            val setStrPersian = if (setStr == "دائم" || setStr.contains("-")) setStr else "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(setStr)
            
            // Mathematically precise real-time above-horizon calculation using spherical trigonometry
            val currentAltitude = com.example.ui.util.AstronomicalCalculator.getMoonAltitude(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = calculationDate
            )
            val isMoonInSky = com.example.ui.util.AstronomicalCalculator.isMoonAboveHorizon(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = calculationDate
            )
            
            val illuminationPercent = details.illuminationPercent
            
            // Effective summit atmospheric moon luminosity is modeled as phase illumination * sin(altitude) if night, else 0%
            val effectiveIlluminationPercent = if (isMoonInSky && !isCurrentlyDaylight) {
                val altRad = Math.toRadians(currentAltitude)
                val factor = Math.sin(altRad).coerceIn(0.0, 1.0)
                illuminationPercent * factor
            } else {
                0.0
            }

            val (intensityLabel, intensityColor) = when {
                !isMoonInSky -> Pair("فاقد بازتاب (ماه زیر خط افق کوهستان است)", Color(0xFF9E9E9E))
                isCurrentlyDaylight -> Pair("غیرقابل رویت (نور خورشید مانع بازتاب موثر مهتاب است)", Color(0xFF9E9E9E))
                effectiveIlluminationPercent >= 80.0 -> Pair("شدت بسیار قوی - روشنایی کامل جبهه‌ای جهت صعودهای گرید شبانه", Color(0xFF00FF87))
                effectiveIlluminationPercent >= 45.0 -> Pair("شدت متوسط - سایه‌اندازی مناسب یال‌ها و تسهیل ملموس هدگیری", Color(0xFFFFD54F))
                effectiveIlluminationPercent >= 15.0 -> Pair("شدت ضعیف - نفوذ نوری کم، استفاده مداوم سیستم روشنایی الزامیست", Color(0xFFFF9100))
                else -> Pair("شدت ناچیز و تاریک - جبهه بدون بازتاب فعال اتمسفری، صعود در ظلمت کامل", Color(0xFFFF5252))
            }
            
            val stateText = if (isMoonInSky) "بله، بالای افق جبهه مستقر است" else "خیر، در حال حاضر زیر افق کوهستان است"
            
            MoonStats(
                phaseName = details.phaseName,
                phaseSymbol = details.phaseSymbol,
                illuminationPercent = illuminationPercent,
                moonrise = riseStrPersian,
                moonset = setStrPersian,
                isInSky = isMoonInSky,
                stateText = stateText,
                intensity = intensityLabel,
                intensityColor = intensityColor,
                isValid = true,
                ageDays = details.ageDays,
                effectiveIlluminationPercent = effectiveIlluminationPercent,
                currentAltitude = currentAltitude
            )
        } catch (e: Exception) {
            MoonStats(
                phaseName = "خطا در محاسبه",
                phaseSymbol = "⚠️",
                illuminationPercent = 0.0,
                moonrise = "اطلاعات نجومی در دسترس نیست",
                moonset = "اطلاعات نجومی در دسترس نیست",
                isInSky = false,
                stateText = "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید.",
                intensity = "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید.",
                intensityColor = Color(0xFFFF5252),
                isValid = false,
                ageDays = 0.0,
                effectiveIlluminationPercent = 0.0,
                currentAltitude = -90.0
            )
        }
    }

    val isDaylightSafe = daylightReport.color == Color(0xFF00FF87) || daylightReport.color == Color(0xFF15803D)
    val daylightCardBg = if (isDaylightSafe) {
        if (isDark) Color(0xFF09140E) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF1E0A0C) else Color(0xFFFFEBEE)
    }
    val daylightCardBorderColor = if (isDaylightSafe) {
        if (isDark) Color(0xFF00FF87).copy(alpha = 0.25f) else Color(0xFF15803D).copy(alpha = 0.4f)
    } else {
        if (isDark) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFFB91C1C).copy(alpha = 0.4f)
    }
    val daylightCardTitleColor = if (isDaylightSafe) {
        if (isDark) Color(0xFFE2FFE9) else Color(0xFF15803D)
    } else {
        if (isDark) Color(0xFFFFE2E2) else Color(0xFFB91C1C)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, daylightCardBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = daylightCardBg
                )
            ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // ساعت محلی قله (زنده)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .background(getAccentColor().copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(1.dp, getAccentColor().copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Peak Local Time",
                    tint = getAccentColor(),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = statsPeakLocalTimeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTextColor()
                )
            }

            // Header: Completely responsive Title and separate Status Row to prevent wrap overlap
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(100))
                            .background(daylightReport.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پایش نور روز و محاسبات نجومی صعود",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = daylightCardTitleColor,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Status Badge sitting securely on its own line alongside the Precision indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = daylightReport.color.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, daylightReport.color.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "وضعیت: ${daylightReport.status}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = daylightReport.color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(100))
                                .background(calculationPrecisionColor)
                        )
                        Text(
                            text = "محاسبه: $calculationPrecisionText",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = calculationPrecisionColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Sunrise/Sunset Horizontal Panel (supports large font scales and perfect light contrast)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sunrise Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFFFFB300).copy(alpha = 0.12f) else Color(0xFFFFB300).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Sunrise",
                                tint = if (isDark) Color(0xFFFFB300) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "طلوع آفتاب",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.5f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sunrisePersian,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Sunset Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFFFF5252).copy(alpha = 0.12f) else Color(0xFFFF5252).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Sunset",
                                tint = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "غروب آفتاب",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.5f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sunsetPersian,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Visual Timeline / Progress Bar representing Day
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFF8FAFC))
                    .border(1.dp, if (isDark) getTextColor(0.04f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "کل زمان روشنایی اتمسفر:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTextColor(0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = totalDaylightPersian,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = getAccentColor()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 24h day-to-night timeline bar
                val dawnLocal = nauticalDawnLocal ?: ((sunriseLocal - 0.8 + 24.0) % 24.0)
                val duskLocal = (sunsetLocal + 0.8) % 24.0
                val formattedDawn = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(dawnLocal)
                val formattedDusk = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(duskLocal)
                val dawnPersian = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedDawn)
                val duskPersian = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedDusk)

                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100))
                    ) {
                        val w = size.width
                        val h = size.height
                        val segmentWidth = w / 240f
                        
                        for (i in 0 until 240) {
                            val hourVal = i * 0.1
                            val color = when {
                                isHourInInterval(hourVal, sunriseLocal, sunsetLocal) -> {
                                    if (isDark) Color(0xFFFCD34D) else Color(0xFFF59E0B) // Golden Daylight
                                }
                                isHourInInterval(hourVal, dawnLocal, sunriseLocal) -> {
                                    Color(0xFF6366F1) // Indigo Dawn
                                }
                                isHourInInterval(hourVal, sunsetLocal, duskLocal) -> {
                                    Color(0xFFEC4899) // Pink Dusk
                                }
                                else -> {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8) // Deep Night
                                }
                            }
                            val x = if (isRtl) {
                                w - (i + 1) * segmentWidth
                            } else {
                                i * segmentWidth
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(x, 0f),
                                size = Size(segmentWidth + 0.5f, h)
                            )
                        }
                    }

                    // Dynamic live hour pin indicator
                    val ratio = (currentHourLocal / 24f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ratio > 0.001f) {
                            Spacer(modifier = Modifier.weight(ratio))
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(100))
                                .background(Color.White)
                                .border(3.5.dp, getAccentColor(), RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(getAccentColor())
                            )
                        }
                        if (ratio < 0.999f) {
                            Spacer(modifier = Modifier.weight(1f - ratio))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Labels below the timeline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "گرگومیش ناوبری: $dawnPersian",
                        fontSize = 8.5.sp,
                        color = getTextColor(0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "طلوع: $sunrisePersian",
                        fontSize = 8.5.sp,
                        color = getTextColor(0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "غروب: $sunsetPersian",
                        fontSize = 8.5.sp,
                        color = getTextColor(0.55f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تاریکی: $duskPersian",
                        fontSize = 8.5.sp,
                        color = getTextColor(0.55f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Small legend keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8)))
                        Text("شب", fontSize = 7.5.sp, color = getTextColor(0.5f), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(Color(0xFF6366F1)))
                        Text("سپیده‌دم", fontSize = 7.5.sp, color = getTextColor(0.5f), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(if (isDark) Color(0xFFFCD34D) else Color(0xFFF59E0B)))
                        Text("روز", fontSize = 7.5.sp, color = getTextColor(0.5f), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(Color(0xFFEC4899)))
                        Text("غروب/شفق", fontSize = 7.5.sp, color = getTextColor(0.5f), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDark) getTextColor(0.06f) else Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Real-time live countdown / remaining daylight status report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(daylightReport.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "وضعیت نور زنده اتمسفر (${daylightReport.status}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = daylightReport.color,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = daylightReport.message,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = getTextColor(0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Stands alone: Card #2: "دیسپچ نجومی ماه و صعود شبانه قله"
    val isMoonSafe = moonDetails.isInSky && moonDetails.effectiveIlluminationPercent >= 15.0

    val moonCardBg = if (isCurrentlyDaylight) {
        if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
    } else if (isMoonSafe) {
        if (isDark) Color(0xFF09140E) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF1E0A0C) else Color(0xFFFFEBEE)
    }
    
    val moonCardBorderColor = if (isCurrentlyDaylight) {
        if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFCBD5E1)
    } else if (isMoonSafe) {
        if (isDark) Color(0xFF00FF87).copy(alpha = 0.25f) else Color(0xFF15803D).copy(alpha = 0.4f)
    } else {
        if (isDark) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFFB91C1C).copy(alpha = 0.4f)
    }
    
    val moonCardTitleColor = if (isCurrentlyDaylight) {
        if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    } else if (isMoonSafe) {
        if (isDark) Color(0xFFE2FFE9) else Color(0xFF15803D)
    } else {
        if (isDark) Color(0xFFFFE2E2) else Color(0xFFB91C1C)
    }

    val moonStatusText = if (isCurrentlyDaylight) {
        "وضعیت: روز فعال (روشنایی خورشید)"
    } else if (moonDetails.isInSky) {
        if (moonDetails.effectiveIlluminationPercent >= 45.0) "وضعیت: نور ماه مطلوب (ایمن/کمکی)" else "وضعیت: مهتاب ضعیف (نیاز به هدلامپ)"
    } else {
        "وضعیت: ماه زیر افق (تاریکی مطلق/توجه)"
    }
    val moonStatusColor = if (isCurrentlyDaylight) {
        if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    } else if (moonDetails.isInSky) {
        if (moonDetails.effectiveIlluminationPercent >= 45.0) {
            if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
        } else {
            if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
        }
    } else {
        if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, moonCardBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = moonCardBg
        )
    ) {
        val finalIntensityColor = remember(moonDetails.intensityColor, isDark) {
            if (isDark) {
                moonDetails.intensityColor
            } else {
                when (moonDetails.intensityColor) {
                    Color(0xFF00FF87) -> Color(0xFF15803D)
                    Color(0xFFFFD54F) -> Color(0xFFB45309)
                    Color(0xFFFF9100) -> Color(0xFFC2410C)
                    Color(0xFFFF5252) -> Color(0xFFB91C1C)
                    Color(0xFF9E9E9E) -> Color(0xFF4B5563)
                    else -> moonDetails.intensityColor
                }
            }
        }

        val horizonColor = remember(moonDetails.isInSky, isCurrentlyDaylight, isDark) {
            if (isCurrentlyDaylight) {
                if (isDark) Color(0xFF9E9E9E) else Color(0xFF616161)
            } else if (moonDetails.isInSky) {
                if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
            } else {
                if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
            }
        }

        val horizonMessage = remember(moonDetails.isInSky, isCurrentlyDaylight) {
            if (isCurrentlyDaylight) {
                "با توجه به حضور آفتاب در آسمان و روشنایی روز، درخشندگی ماه فاقد کارکرد نوری در ناوبری است."
            } else if (moonDetails.isInSky) {
                "ماه بالای خط افق جبهه مستقر است و با بازتاب نور اتمسفری، مسیریابی در صعود را تسهیل می‌کند."
            } else {
                "ماه در حال حاضر زیر افق کوهستان قرار دارد. صعود در تاریکی مطلق و ظلمت کامل کوهستان رقم خواهد خورد."
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Header: Completely responsive Title and separate Status Row to prevent wrap overlap
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Brightness3,
                        contentDescription = null,
                        tint = moonStatusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دیسپچ نجومی ماه و صعود شبانه قله",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = moonCardTitleColor,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Status Badge and precision indicator row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = moonStatusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, moonStatusColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = moonStatusText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = moonStatusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    val moonPrecisionText = if (moonDetails.isValid) "دقیق (Meeus)" else "تخمینی (خطا)"
                    val moonPrecisionColor = if (moonDetails.isValid) Color(0xFF00FF87) else Color(0xFFFFD54F)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(100))
                                .background(moonPrecisionColor)
                        )
                        Text(
                            text = "محاسبه: $moonPrecisionText",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = moonPrecisionColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Beautiful full-width Moon Phase Container (highly responsive and adaptive to colors)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF151A2E) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF283593).copy(alpha = 0.4f) else Color(0xFFCBD5E1), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Moon Phase Visual (perfect alignment)
                MoonPhaseCanvas(
                    illuminationPercent = moonDetails.illuminationPercent.toInt(),
                    ageDays = moonDetails.ageDays,
                    phaseName = moonDetails.phaseName,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Dynamic Moon Phase Badge on a complete full-width row / beautiful display style
                Surface(
                    shape = RoundedCornerShape(100),
                    color = if (isDark) Color(0xFF3F51B5).copy(alpha = 0.16f) else Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.35f) else Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "${moonDetails.phaseSymbol} ${moonDetails.phaseName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFC5CAE9) else Color(0xFF1A237E),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Atmosphere Illumination & Progress Board
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFF8FAFC))
                    .border(1.dp, if (isDark) getTextColor(0.04f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // 1. Lunar Disk Illumination (Phase)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness3,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF9FA8DA) else Color(0xFF3F51B5),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بخش روشن دیسک ماه (فاز):",
                        fontSize = 11.sp,
                        color = getTextColor(0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "\u200E" + PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", moonDetails.illuminationPercent)) + "%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF9FA8DA) else Color(0xFF3F51B5)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100))
                        .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((moonDetails.illuminationPercent / 100.0).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100))
                            .background(if (isDark) Color(0xFF3F51B5) else Color(0xFF4F46E5))
                    )
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // 2. Real-time Effective Mountain Atmosphere Brightness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFF4081) else Color(0xFFDB2777),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "درخشندگی مؤثر اتمسفر قله (واقعی):",
                        fontSize = 11.sp,
                        color = getTextColor(0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "\u200E" + PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", moonDetails.effectiveIlluminationPercent)) + "%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFFF4081) else Color(0xFFDB2777)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // High-fidelity dynamic progress bar for effective brightness
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((moonDetails.effectiveIlluminationPercent / 100.0).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        if (isDark) Color(0xFF3F51B5) else Color(0xFF4F46E5),
                                        if (isDark) Color(0xFFFF4081) else Color(0xFFEC4899)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Moonrise Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF3F51B5).copy(alpha = 0.12f) else Color(0xFF3F51B5).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 14.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "طلوع ماه",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.5f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = moonDetails.moonrise,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Moonset Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFFE91E63).copy(alpha = 0.12f) else Color(0xFFE91E63).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌌",
                                fontSize = 14.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "غروب ماه",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.5f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = moonDetails.moonset,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizon Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(horizonColor.copy(alpha = 0.06f))
                    .border(1.dp, horizonColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(horizonColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = horizonMessage,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTextColor(0.85f),
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Unified Smart Astronomical & Technical Climbing Dispatch Box
            val advisoryBoxColor = remember(moonDetails.isInSky, isCurrentlyDaylight, finalIntensityColor) {
                if (isCurrentlyDaylight) Color(0xFF9E9E9E) else if (moonDetails.isInSky) finalIntensityColor else Color(0xFFFF5252)
            }
            
            val advisoryThemeColor = remember(advisoryBoxColor, isDark) {
                if (isDark) {
                    advisoryBoxColor
                } else {
                    // Safe high-contrast colors suited perfectly for light mode
                    when (advisoryBoxColor) {
                        Color(0xFF00FF87) -> Color(0xFF15803D)
                        Color(0xFFFFD54F) -> Color(0xFF9E5C00)
                        Color(0xFFFF9100) -> Color(0xFFC2410C)
                        Color(0xFFFF5252), Color(0xFFB91C1C) -> Color(0xFFB91C1C)
                        Color(0xFF9E9E9E) -> Color(0xFF374151)
                        else -> advisoryBoxColor
                    }
                }
            }

            val advisoryMessage = remember(moonDetails.isInSky, moonDetails.illuminationPercent, moonDetails.effectiveIlluminationPercent, isCurrentlyDaylight, moonDetails.moonrise, currentTemp, altitude) {
                val effectiveIllumFormatted = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", moonDetails.effectiveIlluminationPercent))
                val rawIllumFormatted = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", moonDetails.illuminationPercent))
                val altFormatted = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(altitude)
                
                val snowAlbedoNote = if (currentTemp <= 0.0 || altitude >= 3000) {
                    "\n• شاخص بازتاب برف (Snow Albedo): به علت بازتاب ۸۰٪ نور ماه از سطح برف و رقت جو در ارتفاع $altFormatted متری، دید شبانه تقویت شده است (احتمال خیرگی برفی شبانه)."
                } else ""

                if (isCurrentlyDaylight) {
                    "• وضعیت نور: روز فعال (روشنایی کامل خورشید)\n• دید محیطی: دید افقی کامل و ایده‌آل جبهه‌ها و یال‌های صعود بدون نیاز به روشنایی مصنوعی.\n• توصیه فنی: نیازی به پایش شدت مهتاب در حضور روشنایی مستقیم خورشید نیست."
                } else if (!moonDetails.isInSky) {
                    val timingNote = if (moonDetails.illuminationPercent >= 40.0) {
                        "\n• ⚠️ عدم همزمانی حضور ماه: فاز ماه $rawIllumFormatted٪ است، اما طلوع آن ساعت ${moonDetails.moonrise} می‌باشد. تا قبل از طلوع، پیمایش در تاریکی مطلق (بدون مهتاب) است!"
                    } else ""
                    "• وضعیت نور: تاریکی مطلق (ماه زیر افق / بازتاب نوری فعال: ۰٪)$timingNote\n• تجهیزات لازم: هدلامپ بالای ۳۰۰ لومن با باتری پشتیبان + سیستم‌های ناوبری فعال آفلاین\n• هشدار صعود: ریسک شدید خطای دید، انحراف از یال‌های فنی و مواجهه با عوارض پنهان محیطی."
                } else if (moonDetails.effectiveIlluminationPercent >= 45.0) {
                    "• وضعیت نور: روشنایی مطلوب طبیعی (ماه بالای افق / درخشندگی مؤثر اتمسفر: $effectiveIllumFormatted٪ - فاز ماه: $rawIllumFormatted٪)$snowAlbedoNote\n• دید محیطی: پایش مناسب چشمی یخچال‌ها، دهلیزها و خط‌الرأس‌های سنگی در فواصل دور\n• توصیه فنی: صعود با حالت بهینه هدلامپ جهت ذخیره انرژی باتری و جلوگیری از خستگی چشم."
                } else {
                    "• وضعیت نور: تاریکی نسبی کوهستان (ماه بالای افق / هلال ضعیف یا درخشندگی ناچیز مؤثر: $effectiveIllumFormatted٪)$snowAlbedoNote\n• محدودیت دید: عدم امکان تشخیص عوارض سنگی بزرگ و دهلیزهای دوردست با چشم غیرمسلح\n• توصیه فنی: استفاده مداوم از هدلامپ با حالت استاندارد و تکیه بر ابزار ناوبری جهت ردیابی ایمن مسیر."
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(advisoryThemeColor.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .border(1.dp, advisoryThemeColor.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(100))
                            .background(advisoryThemeColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "راهنمای سناریوی صعود و دیسپچ نوری شبانه:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = advisoryThemeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = advisoryMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = getTextColor(),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Alpine Golden Safe Climbing Window Callout & Professional Mountaineering Dispatch
    val isInside = alpineReport.isInside
    val alpineCardBg = if (isInside) {
        if (isDark) Color(0xFF09140E) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF1E0A0C) else Color(0xFFFFEBEE)
    }
    val alpineCardBorderColor = if (isInside) {
        if (isDark) Color(0xFF00FF87).copy(alpha = 0.25f) else Color(0xFF15803D).copy(alpha = 0.4f)
    } else {
        if (isDark) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFFB91C1C).copy(alpha = 0.4f)
    }
    val alpineTitleColor = if (isInside) {
        if (isDark) Color(0xFFE2FFE9) else Color(0xFF15803D)
    } else {
        if (isDark) Color(0xFFFFE2E2) else Color(0xFFB91C1C)
    }
    
    val textPrimary = if (isDark) Color.White else Color(0xFF1B2A1E)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF4E6352)
    val sliderBg = if (isDark) Color.White.copy(alpha = 0.01f) else Color(0xFFE8F3EB)
    val sliderBorder = if (isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFD0E6D6)
    val alpineSubCardBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, alpineCardBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = alpineCardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Header: Completely responsive Title and separate Status Row to prevent wrap overlap
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe Window Icon",
                        tint = alpineReport.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پنجره طلایی صعود ایمن (بازه آلپاین)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = alpineTitleColor,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Status Badge sitting securely on its own line
                Surface(
                    shape = RoundedCornerShape(100),
                    color = alpineReport.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, alpineReport.color.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "وضعیت: ${alpineReport.status}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = alpineReport.color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-Parameter Grid Layout (Optimized Layout structure to prevent wrapping)
            // Row 1: Start and Deadline side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start of Window Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFFFFD54F).copy(alpha = 0.12f) else Color(0xFFFFD54F).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Start ascent",
                                tint = if (isDark) Color(0xFFFFD54F) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "شروع صعود ایمن",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(0.5f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alpineStartPersian,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor(),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Turnaround/Deadline Limit Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorderStroke(Color(0xFFFF5252)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFFFF5252).copy(alpha = 0.12f) else Color(0xFFFF5252).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Turnaround limit",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سررسید فرود",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.7f) else Color(0xFFB91C1C),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alpineEndPersian,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Total Active Safety Window Duration occupying its own horizontal line to handle long strings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.05f)),
                colors = CardDefaults.cardColors(
                    containerColor = alpineSubCardBg
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فرجه آزاد صعود ایمن (کل بازه فعال):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alpineReport.totalDurationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic Progress Slider specifically calibrating the Alpine Golden Window
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sliderBg, RoundedCornerShape(14.dp))
                    .border(1.dp, sliderBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شبیه‌ساز وضعیت لحظه‌ای در پنجره آلپاین:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Text(
                        text = if (alpineReport.currentProgress >= 1.0f) "پنجره سپری شده" else if (alpineReport.currentProgress <= 0.0f) "پنجره پیش‌رو" else "\u200E${PersianDateHelper.formatToPersianDigits((alpineReport.currentProgress * 100).toInt())}% طی شده",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = alpineReport.color
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 24h Alpine Window day timeline bar
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100))
                    ) {
                        val w = size.width
                        val h = size.height
                        val segmentWidth = w / 240f
                        
                        for (i in 0 until 240) {
                            val hourVal = i * 0.1
                            val color = when {
                                isHourInInterval(hourVal, alpineStartLocal, alpineEndLocal) -> {
                                    Color(0xFF10B981) // Emerald Green - Alpine Safe Window
                                }
                                isHourInInterval(hourVal, alpineEndLocal, (sunsetLocal + 1.0) % 24.0) -> {
                                    Color(0xFFEF4444) // Crimson Red - Critical Danger (after turnaround)
                                }
                                isHourInInterval(hourVal, (alpineStartLocal - 1.5 + 24.0) % 24.0, alpineStartLocal) -> {
                                    Color(0xFFF59E0B) // Amber - Pre-Alpine Prep
                                }
                                else -> {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8) // Deep Night / Rest
                                }
                            }
                            val x = if (isRtl) {
                                w - (i + 1) * segmentWidth
                            } else {
                                i * segmentWidth
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(x, 0f),
                                size = Size(segmentWidth + 0.5f, h)
                            )
                        }
                    }

                    // Dynamic live hour pin indicator
                    val ratio = (currentHourLocal / 24f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ratio > 0.001f) {
                            Spacer(modifier = Modifier.weight(ratio))
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(100))
                                .background(Color.White)
                                .border(3.5.dp, Color(0xFF10B981), RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (ratio < 0.999f) {
                            Spacer(modifier = Modifier.weight(1f - ratio))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Key Milestones below the timeline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "آماده‌سازی (قبل طلوع)",
                        fontSize = 8.5.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "آغاز بازه: $alpineStartPersian",
                        fontSize = 8.5.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "سررسید فرود: $alpineEndPersian",
                        fontSize = 8.5.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Small legend keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8)))
                        Text("استراحت/شب", fontSize = 7.5.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(Color(0xFFF59E0B)))
                        Text("آماده‌سازی", fontSize = 7.5.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(Color(0xFF10B981)))
                        Text("پنجره آلپاین (صعود ایمن)", fontSize = 7.5.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(100)).background(Color(0xFFEF4444)))
                        Text("خطر/فرود اضطراری", fontSize = 7.5.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic alert message box with situational coloring (Green/Amber/Red)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(alpineReport.color.copy(alpha = if (isDark) 0.05f else 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, alpineReport.color.copy(alpha = if (isDark) 0.16f else 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = alpineReport.icon,
                        contentDescription = null,
                        tint = alpineReport.color,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سیستم توصیه ایمنی آلپاین (تلفیق زنده):",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = alpineReport.color
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = alpineReport.message,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } 
    }
}
}
}







data class CompiledHourlyItem(
    val id: String,
    val index: Int,
    val hourPersian: String,
    val weatherCode: Int,
    val tempVal: Double,
    val tempPersian: String,
    val apparentTempVal: Double,
    val apparentTempPersian: String,
    val relativeHumidityVal: Int,
    val relativeHumidityPersian: String,
    val windSpeedVal: Double,
    val windSpeedPersian: String,
    val windGustsVal: Double,
    val windGustsPersian: String,
    val prob: Int,
    val probPersian: String,
    val precipitationMm: Double,
    val precipitationPersian: String,
    val cloudCoverVal: Int,
    val cloudCoverPersian: String,
    val visibilityMeters: Double,
    val visibilityPersian: String,
    val freezingLevelMeters: Double,
    val freezingLevelPersian: String,
    val uvIndexVal: Double,
    val uvIndexPersian: String,
    val windDirectionVal: Double = 0.0,
    val windDirectionPersian: String = "نامشخص",
    val pressureVal: Double = 1013.25,
    val pressurePersian: String = "۱۰۱۳",
    val isDay: Int = 1,
    val frostbiteWarning: String? = null,
    val suddenChangeWarning: String? = null
)

data class HourlySafetyReport(
    val status: String,
    val advice: String,
    val color: Color,
    val message: String,
    val statusEnum: com.example.ui.util.SafetyStatus
)

data class MetricStatus(
    val bg: Color,
    val border: BorderStroke,
    val tint: Color
)

fun getMetricStatus(
    level: String,
    isDark: Boolean
): MetricStatus {
    return when (level) {
        "CRITICAL" -> {
            val tint = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
            val bg = if (isDark) Color(0xFF261012) else Color(0xFFFFEBEE)
            val borderColor = if (isDark) Color(0xFFFF5252).copy(alpha = 0.35f) else Color(0xFFB91C1C).copy(alpha = 0.5f)
            MetricStatus(bg = bg, border = BorderStroke(1.5.dp, borderColor), tint = tint)
        }
        "WARNING" -> {
            val tint = if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
            val bg = if (isDark) Color(0xFF241F0A) else Color(0xFFFFFDE7)
            val borderColor = if (isDark) Color(0xFFFFD54F).copy(alpha = 0.35f) else Color(0xFFB45309).copy(alpha = 0.45f)
            MetricStatus(bg = bg, border = BorderStroke(1.2.dp, borderColor), tint = tint)
        }
        else -> { // "SAFE" / "NORMAL"
            val tint = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
            val bg = if (isDark) Color(0xFF0C1A12) else Color(0xFFE8F5E9)
            val borderColor = if (isDark) Color(0xFF00FF87).copy(alpha = 0.15f) else Color(0xFF15803D).copy(alpha = 0.25f)
            MetricStatus(bg = bg, border = BorderStroke(1.dp, borderColor), tint = tint)
        }
    }
}

fun getHourlySafetyReport(
    item: CompiledHourlyItem, 
    hourly: com.example.data.remote.HourlyData, 
    altitude: Int, 
    isDark: Boolean,
    mountain: com.example.data.local.MountainEntity,
    offsetHours: Double,
    minutely15: com.example.data.remote.Minutely15Data? = null,
    units: com.example.data.remote.WeatherUnits? = null,
    daily: com.example.data.remote.DailyData? = null
): HourlySafetyReport {
    val adjPressure = item.pressureVal
    val dewPointVal = com.example.ui.util.MountaineeringHelper.calculateDewPoint(item.tempVal, item.relativeHumidityVal.toDouble())
    val snowfallVal = hourly.snowfall?.getOrNull(item.index) ?: 0.0
    
    val simulatedCurrent = com.example.data.remote.CurrentWeather(
        time = item.id,
        temperature2m = item.tempVal,
        relativeHumidity2m = item.relativeHumidityVal.toDouble(),
        apparentTemperature = item.apparentTempVal,
        precipitation = item.precipitationMm,
        snowfall = snowfallVal,
        weatherCode = item.weatherCode,
        windSpeed10m = item.windSpeedVal,
        windDirection10m = 0.0,
        windSpeed80m = item.windSpeedVal,
        windDirection80m = 0.0,
        surfacePressure = adjPressure,
        pressureMsl = hourly.pressureMsl?.getOrNull(item.index) ?: 1013.25,
        freezingLevelHeight = item.freezingLevelMeters,
        windGusts10m = item.windGustsVal,
        visibility = item.visibilityMeters,
        cloudCover = item.cloudCoverVal.toDouble(),
        cloudCoverLow = hourly.cloudCoverLow?.getOrNull(item.index),
        cloudCoverMid = hourly.cloudCoverMid?.getOrNull(item.index),
        cloudCoverHigh = hourly.cloudCoverHigh?.getOrNull(item.index),
        soilTemperature0cm = hourly.soilTemperature0cm?.getOrNull(item.index),
        isDay = item.isDay,
        dewPoint2m = dewPointVal,
        cape = hourly.cape?.getOrNull(item.index)
    )

    val report = com.example.ui.util.MountaineeringHelper.evaluateSafety(
        current = simulatedCurrent,
        hourly = hourly,
        daily = daily,
        altitudeOverride = altitude,
        hourIndexOverride = item.index,
        slopeAngle = mountain.slopeAngle,
        aspect = mountain.aspect,
        offsetHours = offsetHours,
        minutely15 = minutely15,
        summitElevation = mountain.altitude.toDouble(),
        baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
        units = units,
        latitude = mountain.latitude,
        longitude = mountain.longitude
    )

    val reportColor = when (report.status) {
        com.example.ui.util.SafetyStatus.RED -> if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
        com.example.ui.util.SafetyStatus.YELLOW -> if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
        com.example.ui.util.SafetyStatus.GREEN -> if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
    }

    val statusStr = when (report.status) {
        com.example.ui.util.SafetyStatus.RED -> "بحرانی و پرخطر"
        com.example.ui.util.SafetyStatus.YELLOW -> "نیازمند احتیاط شدید"
        else -> "ایمن و مساعد"
    }

    val adviceStr = when (report.status) {
        com.example.ui.util.SafetyStatus.RED -> "کنسلی صعود و فرود فوری"
        com.example.ui.util.SafetyStatus.YELLOW -> "صعود فنی با تجهیزات کامل"
        else -> "پنجره طلایی صعود باز است"
    }

    val messageStr = report.description

    return HourlySafetyReport(
        status = statusStr,
        advice = adviceStr,
        color = reportColor,
        message = messageStr,
        statusEnum = report.status
    )
}

@Composable
fun GoldenWindowSection(
    viewModel: WeatherViewModel,
    hourly: com.example.data.remote.HourlyData?,
    daily: com.example.data.remote.DailyData?,
    altitude: Int,
    mountain: com.example.data.local.MountainEntity,
    selectedDaysCount: Int,
    onDaysCountChanged: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val peakOffsetHours = remember(mountain) {
        com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
    }

    var statsTick by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            statsTick = System.currentTimeMillis() / 60000L
        }
    }

    val mountainLocalCalendar = remember(peakOffsetHours, statsTick) {
        val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
        val localTimeMillis = System.currentTimeMillis() + offsetMillis
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localTimeMillis
        }
    }

    val todayDateStr = remember(mountainLocalCalendar) {
        val yr = mountainLocalCalendar.get(java.util.Calendar.YEAR)
        val mn = mountainLocalCalendar.get(java.util.Calendar.MONTH) + 1
        val dy = mountainLocalCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        String.format(java.util.Locale.US, "%04d-%02d-%02d", yr, mn, dy)
    }

    val currentHourIdx = remember(hourly, mountain, peakOffsetHours, statsTick) {
        if (hourly == null || hourly.time.isEmpty()) -1 else {
            val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
            val localTimeMillis = System.currentTimeMillis() + offsetMillis
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val nowStr = sdf.format(java.util.Date(localTimeMillis))
            val dummyCurrent = com.example.data.remote.CurrentWeather(
                time = nowStr,
                temperature2m = 0.0,
                relativeHumidity2m = 0.0,
                apparentTemperature = 0.0,
                precipitation = 0.0,
                snowfall = 0.0,
                weatherCode = 0,
                windSpeed10m = 0.0,
                windDirection10m = 0.0,
                windSpeed80m = 0.0,
                windDirection80m = 0.0,
                surfacePressure = 0.0,
                freezingLevelHeight = 0.0,
                windGusts10m = 0.0,
                visibility = 0.0,
                cloudCover = 0.0,
                isDay = 1,
                dewPoint2m = 0.0
            )
            com.example.ui.util.MountaineeringHelper.findHourlyIndexForCurrent(dummyCurrent, hourly, peakOffsetHours)
        }
    }

    val currentHourOfDay = remember(mountainLocalCalendar) {
        val hour = mountainLocalCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = mountainLocalCalendar.get(java.util.Calendar.MINUTE)
        hour + (minute / 60f)
    }

    data class DailyGoldenWindowItem(
        val index: Int,
        val dayLabel: String,
        val dateSubtext: String,
        val windows: List<GoldenWindow>,
        val dateStr: String
    )

    val dailyGoldenItems = remember(hourly, daily, altitude, mountain, currentHourIdx, selectedDaysCount, todayDateStr, peakOffsetHours, statsTick) {
        if (daily == null || hourly == null) emptyList() else {
            val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
            val localTimeMillis = System.currentTimeMillis() + offsetMillis
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = localTimeMillis
            }
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val tomorrowDateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dayAfterTomorrowDateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))

            val validIndices = java.util.ArrayList<Int>()
            for (i in daily.time.indices) {
                val dateStr = daily.time.getOrNull(i) ?: ""
                if (dateStr.isNotEmpty() && dateStr >= todayDateStr) {
                    validIndices.add(i)
                }
            }
            val count = validIndices.size.coerceAtMost(selectedDaysCount)
            val filteredIndices = validIndices.subList(0, count)
            filteredIndices.mapIndexed { idxInList, i ->
                val dateStr = daily.time.getOrNull(i) ?: ""
                val dName = PersianDateHelper.getPersianDayOfWeek(dateStr)
                val shamsi = PersianDateHelper.getJalaliNumericDateString(dateStr)
                
                val label = when (dateStr) {
                    todayDateStr -> "امروز"
                    tomorrowDateStr -> "فردا"
                    dayAfterTomorrowDateStr -> "پس‌فردا"
                    else -> dName
                }

                // Locate indices of this day inside hourly.time
                val dayStartIdx = hourly.time.indexOfFirst { it.startsWith(dateStr) }
                val dayEndIdx = if (dayStartIdx != -1) {
                    hourly.time.indexOfLast { it.startsWith(dateStr) } + 1
                } else {
                    -1
                }

                val (startI, endI) = if (dayStartIdx != -1 && dayEndIdx != -1) {
                    // For the actual current day, we start from currentHourIdx to only scan future windows
                    val startIdx = if (dateStr == todayDateStr && currentHourIdx >= 0) {
                        maxOf(dayStartIdx, currentHourIdx)
                    } else {
                        dayStartIdx
                    }
                    val endIdx = maxOf(startIdx, dayEndIdx)
                    Pair(startIdx, endIdx)
                } else {
                    // Fallback to index-based logic if index not found
                    val offset = if (idxInList == 0) 0 else {
                        if (currentHourIdx >= 0) 24 * idxInList - (currentHourIdx % 24) else 24 * idxInList
                    }
                    val duration = if (idxInList == 0 && currentHourIdx >= 0) 24 - (currentHourIdx % 24) else 24
                    Pair(maxOf(0, currentHourIdx + offset), currentHourIdx + offset + duration)
                }

                val windows = findGoldenWindows(hourly, altitude, mountain, startIdxParam = startI, endIdxParam = endI)

                DailyGoldenWindowItem(
                    index = idxInList,
                    dayLabel = label,
                    dateSubtext = shamsi,
                    windows = windows,
                    dateStr = dateStr
                )
            }
        }
    }

    val bestDayItem = remember(dailyGoldenItems) {
        dailyGoldenItems.maxByOrNull { it.windows.sumOf { w -> w.durationHours } }
    }

    val hasAnyWindows = remember(dailyGoldenItems) {
        dailyGoldenItems.any { it.windows.isNotEmpty() }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "پنجره طلایی صعود ایمن",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = getTextColor()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val activeElevPersian = remember(altitude) { "\u200E${PersianDateHelper.formatToPersianDigits(altitude)}" }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1B2232) else Color(0xFFE3F2FD),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.35f) else Color(0xFF90CAF9))
                    ) {
                        Text(
                            text = "تراز صعود: $activeElevPersian متر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

        if (hasAnyWindows) {
            val cardBg = if (isDark) Color(0xFF0A1118) else Color(0xFFF0F6FC)
            val borderColor = if (isDark) getAccentColor().copy(alpha = 0.2f) else getAccentColor().copy(alpha = 0.35f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = cardBg
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(getAccentColor().copy(alpha = if (isDark) 0.12f else 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Golden Window",
                                tint = getAccentColor(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val daysCountPersian = "\u200E" + PersianDateHelper.formatToPersianDigits(selectedDaysCount)
                            Text(
                                text = "پنجره طلایی صعود ایمن ($daysCountPersian روز آینده)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = getTextColor()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "پیش‌بینی تفکیکی و زمان‌بندی صعود بر اساس شرایط جوی",
                                fontSize = 8.5.sp,
                                color = getTextColor(0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Days count selection buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(3, 7, 16).forEach { days ->
                            val isLocked = !isPremium && days > 3
                            val isSelected = selectedDaysCount == days && !isLocked
                            val label = when (days) {
                                3 -> "۳ روزه"
                                7 -> if (isLocked) "۷ روزه 🔒" else "۷ روزه"
                                else -> if (isLocked) "۱۶ روزه 🔒" else "۱۶ روزه"
                            }
                            val chipBg = if (isSelected) {
                                if (isDark) Color(0xFF1B253D) else Color(0xFFE3F2FD)
                            } else {
                                Color.Transparent
                            }
                            val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)
                            val chipText = if (isSelected) {
                                if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                            } else if (isLocked) {
                                goldColor.copy(alpha = 0.9f)
                            } else {
                                getTextColor(0.55f)
                            }
                            val chipBorder = if (isSelected) {
                                BorderStroke(1.2.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.85f) else Color(0xFF2196F3))
                            } else if (isLocked) {
                                BorderStroke(1.dp, goldColor.copy(alpha = 0.35f))
                            } else {
                                BorderStroke(1.dp, getTextColor(0.08f))
                            }

                            Surface(
                                onClick = { 
                                    if (isLocked) {
                                        viewModel.triggerBilling(true)
                                    } else {
                                        onDaysCountChanged(days)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = chipBorder
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || isLocked) FontWeight.Black else FontWeight.Bold,
                                    color = chipText,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display summary for each day in our list
                    dailyGoldenItems.forEachIndexed { idx, item ->
                        val isItemToday = item.dateStr == todayDateStr
                        DayGoldenSummary(
                            dayLabel = item.dayLabel,
                            windows = item.windows,
                            isToday = isItemToday,
                            isBestDay = bestDayItem != null && bestDayItem.windows.isNotEmpty() && bestDayItem.index == item.index,
                            currentHour = if (isItemToday) currentHourOfDay else -1f,
                            dateSubtext = if (!isItemToday) item.dateSubtext else null,
                            dateStr = item.dateStr
                        )

                        if (idx < dailyGoldenItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = getTextColor(0.06f)
                            )
                        }
                    }
                }
            }
        } else {
            val noWindowCardBg = if (isDark) Color(0xFF1E0A0C) else Color(0xFFFFEBEE)
            val noWindowBorderColor = if (isDark) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFFB91C1C).copy(alpha = 0.4f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, noWindowBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = noWindowCardBg
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF5252).copy(alpha = if (isDark) 0.12f else 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "No Window",
                                tint = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "عدم وجود پنجره صعود ایمن",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFFFE2E2) else Color(0xFFB91C1C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val daysCountPersian = "\u200E" + PersianDateHelper.formatToPersianDigits(selectedDaysCount)
                            Text(
                                text = "هیچ پنجرهٔ ایمنی در $daysCountPersian روز آینده پیش‌بینی نمی‌شود.",
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.7f) else Color(0xFFB91C1C).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days count selection buttons (even in empty state so the user can switch/toggle or see lock state)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(3, 7, 16).forEach { days ->
                            val isLocked = !isPremium && days > 3
                            val isSelected = selectedDaysCount == days && !isLocked
                            val label = when (days) {
                                3 -> "۳ روزه"
                                7 -> if (isLocked) "۷ روزه 🔒" else "۷ روزه"
                                else -> if (isLocked) "۱۶ روزه 🔒" else "۱۶ روزه"
                            }
                            val chipBg = if (isSelected) {
                                if (isDark) Color(0xFF1B253D) else Color(0xFFE3F2FD)
                            } else {
                                Color.Transparent
                            }
                            val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)
                            val chipText = if (isSelected) {
                                if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                            } else if (isLocked) {
                                goldColor.copy(alpha = 0.9f)
                            } else {
                                getTextColor(0.55f)
                            }
                            val chipBorder = if (isSelected) {
                                BorderStroke(1.2.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.85f) else Color(0xFF2196F3))
                            } else if (isLocked) {
                                BorderStroke(1.dp, goldColor.copy(alpha = 0.35f))
                            } else {
                                BorderStroke(1.dp, getTextColor(0.08f))
                            }

                            Surface(
                                onClick = { 
                                    if (isLocked) {
                                        viewModel.triggerBilling(true)
                                    } else {
                                        onDaysCountChanged(days)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = chipBorder
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || isLocked) FontWeight.Black else FontWeight.Bold,
                                    color = chipText,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "با توجه به تحلیل لایه‌های جوی، سرعت باد، احتمال بارش و ریسک صاعقه، صعود در این بازه با خطرات جدی همراه است. لطفاً صعود خود را به تاخیر بیندازید.",
                        fontSize = 10.5.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A1515),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun HourlyForecastSection(
    hourly: com.example.data.remote.HourlyData,
    altitude: Int,
    mountain: com.example.data.local.MountainEntity,
    daily: com.example.data.remote.DailyData? = null
) {
    val peakOffsetHours = remember(mountain) {
        com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
    }

    var statsTick by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            statsTick = System.currentTimeMillis() / 60000L
        }
    }

    val mountainLocalCalendar = remember(peakOffsetHours, statsTick) {
        val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
        val localTimeMillis = System.currentTimeMillis() + offsetMillis
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localTimeMillis
        }
    }

    val todayDateStr = remember(mountainLocalCalendar) {
        val yr = mountainLocalCalendar.get(java.util.Calendar.YEAR)
        val mn = mountainLocalCalendar.get(java.util.Calendar.MONTH) + 1
        val dy = mountainLocalCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        String.format(java.util.Locale.US, "%04d-%02d-%02d", yr, mn, dy)
    }

    val currentHourIdx = remember(hourly, mountain, peakOffsetHours, statsTick) {
        if (hourly == null || hourly.time.isEmpty()) -1 else {
            val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
            val localTimeMillis = System.currentTimeMillis() + offsetMillis
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val nowStr = sdf.format(java.util.Date(localTimeMillis))
            val dummyCurrent = com.example.data.remote.CurrentWeather(
                time = nowStr,
                temperature2m = 0.0,
                relativeHumidity2m = 0.0,
                apparentTemperature = 0.0,
                precipitation = 0.0,
                snowfall = 0.0,
                weatherCode = 0,
                windSpeed10m = 0.0,
                windDirection10m = 0.0,
                windSpeed80m = 0.0,
                windDirection80m = 0.0,
                surfacePressure = 0.0,
                freezingLevelHeight = 0.0,
                windGusts10m = 0.0,
                visibility = 0.0,
                cloudCover = 0.0,
                isDay = 1,
                dewPoint2m = 0.0
            )
            com.example.ui.util.MountaineeringHelper.findHourlyIndexForCurrent(dummyCurrent, hourly, peakOffsetHours)
        }
    }

    val itemsFlow = remember(hourly, altitude, mountain, todayDateStr, peakOffsetHours, statsTick, daily) {
        if (hourly == null || hourly.time.isEmpty()) emptyList() else {
            val dayStartIdx = hourly.time.indexOfFirst { it.startsWith(todayDateStr) }
            val startIdx = if (dayStartIdx != -1) dayStartIdx else {
                if (currentHourIdx >= 0) (currentHourIdx / 24) * 24 else 0
            }
            val count = 24.coerceAtMost(hourly.time.size - startIdx)
            val diff = mountain.altitude - altitude

            val rawItems = List(count) { i ->
                val absoluteIdx = startIdx + i
                val fullTime = hourly.time.getOrNull(absoluteIdx) ?: ""
                val hourFormatted = try {
                    if (fullTime.contains("T")) {
                        val timePart = fullTime.split("T")[1]
                        if (timePart.length >= 5 && timePart[2] == ':') {
                            timePart.substring(0, 5)
                        } else {
                            val hInt = timePart.substringBefore(":").toIntOrNull()
                            if (hInt != null) String.format(java.util.Locale.US, "%02d:00", hInt) else "$timePart:00"
                        }
                    } else {
                        val hInt = fullTime.toIntOrNull()
                        if (hInt != null) String.format(java.util.Locale.US, "%02d:00", hInt) else "$fullTime:00"
                    }
                } catch (e: Exception) {
                    fullTime
                }
                val hourP = PersianDateHelper.formatToPersianDigits(hourFormatted)
                val probVal = hourly.precipitationProbability?.getOrNull(absoluteIdx) ?: 0
                
                // Raw values from hourly data
                val rawWindSp = hourly.windSpeed80m?.getOrNull(absoluteIdx) ?: hourly.windSpeed10m?.getOrNull(absoluteIdx) ?: 0.0
                val rawWind10m = hourly.windSpeed10m?.getOrNull(absoluteIdx) ?: rawWindSp
                val rawTempVal = hourly.temperature2m.getOrNull(absoluteIdx) ?: 0.0
                
                // Apply scale adjustments based on selected altitude using standard lapse rate (+0.65°C per 100 meters down / -0.65°C per 100 meters up)
                val adjTemp = rawTempVal + (diff * 0.0065)
                val absTemp = kotlin.math.abs(adjTemp)
                val tempSign = if (adjTemp < 0.0) "-" else ""
                val tempP = "\u200E$tempSign${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absTemp))}"
                
                // Wind speed scales dynamically from elevation ratios (Power Law)
                val adjWindSp = MountaineeringHelper.adjustWindWithAltitude(
                    referenceWind = rawWindSp,
                    referenceElevation = mountain.altitude.toDouble(),
                    targetAltitude = altitude.toDouble(),
                    alpha = null
                )
                val windSpeedPersian = PersianDateHelper.formatToPersianDigits(adjWindSp.toInt())

                // Apparent temp feels based on adjusted wind and adjusted temperature
                val isNightHour1 = hourly.isDay?.getOrNull(absoluteIdx) == 0
                val adjApparent = MountaineeringHelper.calculateWindChill(adjTemp, adjWindSp, isNight = isNightHour1)
                var finalWeatherCode = hourly.weatherCode.getOrNull(absoluteIdx) ?: 0
                if (adjTemp <= 0.0) {
                    finalWeatherCode = when (finalWeatherCode) {
                        61, 80 -> 71
                        63, 81 -> 73
                        65, 82 -> 75
                        else -> finalWeatherCode
                    }
                } else if (adjTemp > 2.0) {
                    finalWeatherCode = when (finalWeatherCode) {
                        71, 85 -> 61
                        73, 86 -> 63
                        75 -> 65
                        else -> finalWeatherCode
                    }
                }
                val absApparent = kotlin.math.abs(adjApparent)
                val apparentSign = if (adjApparent < 0.0) "-" else ""
                val apparentP = "\u200E$apparentSign${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absApparent))}"
                
                val humidityVal = hourly.relativeHumidity2m?.getOrNull(absoluteIdx)?.toInt() ?: 0
                val humidityP = PersianDateHelper.formatToPersianDigits(humidityVal)

                val rawWindGusts = hourly.windGusts10m?.getOrNull(absoluteIdx) ?: (rawWindSp * MountaineeringHelper.calculateDynamicGustFactor(hourly.cape?.getOrNull(absoluteIdx)))
                val adjWindGusts = MountaineeringHelper.adjustWindWithAltitude(
                    referenceWind = rawWindGusts,
                    referenceElevation = mountain.altitude.toDouble(),
                    targetAltitude = altitude.toDouble(),
                    alpha = null
                )
                val windGustsP = PersianDateHelper.formatToPersianDigits(adjWindGusts.toInt())
                
                val precMm = hourly.precipitation?.getOrNull(absoluteIdx) ?: 0.0
                val precP = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", precMm))

                val clouds = hourly.cloudCover?.getOrNull(absoluteIdx)?.toInt() ?: -1
                val cloudsP = if (clouds == -1) "نامشخص" else PersianDateHelper.formatToPersianDigits(clouds)

                val vis = hourly.visibility?.getOrNull(absoluteIdx) ?: -1.0
                val visKm = if (vis < 0.0) -1.0 else vis / 1000.0
                val visP = if (visKm < 0.0) "نامشخص" else PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", visKm))

                val freezing = hourly.freezingLevelHeight?.getOrNull(absoluteIdx) ?: 0.0
                val freezingP = PersianDateHelper.formatToPersianDigits(freezing.toInt())

                val rawWindDir = hourly.windDirection80m?.getOrNull(absoluteIdx) ?: hourly.windDirection10m?.getOrNull(absoluteIdx) ?: 0.0
                val windDirP = com.example.ui.util.MountaineeringHelper.getWindDirectionPersian(rawWindDir)

                val qnhP = hourly.pressureMsl?.getOrNull(absoluteIdx) ?: 1013.25
                val rawPress = com.example.ui.util.MountaineeringHelper.calculateBarometricPressure(
                    hourly.surfacePressure?.getOrNull(absoluteIdx),
                    rawTempVal,
                    mountain.altitude,
                    altitude,
                    targetTemp = adjTemp,
                    qnh = qnhP
                )
                val pressP = PersianDateHelper.formatToPersianDigits(rawPress.toInt())

                val isNightHour = hourly.isDay?.getOrNull(absoluteIdx) == 0
                val snowfallRate = hourly.snowfall?.getOrNull(absoluteIdx) ?: 0.0
                val hasSnowCover = snowfallRate > 0.0 || (precMm > 0.0 && adjTemp <= 0.5)
                val rawHourlyUv = MountaineeringHelper.calculateResolvedUvIndex(
                    current = com.example.data.remote.CurrentWeather(
                        time = fullTime,
                        isDay = if (isNightHour) 0 else 1
                    ),
                    hourly = hourly,
                    altitude = altitude,
                    mountainAltitude = mountain.altitude,
                    snowCover = hasSnowCover,
                    snowfallRate = snowfallRate,
                    hourlyIndex = absoluteIdx,
                    daily = daily,
                    offsetHours = peakOffsetHours
                )
                val uvIndexP = if (isNightHour || rawHourlyUv <= 0.0) "۰.۰" else PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", rawHourlyUv))

                CompiledHourlyItem(
                    id = fullTime,
                    index = absoluteIdx,
                    hourPersian = hourP,
                    weatherCode = finalWeatherCode,
                    tempVal = adjTemp,
                    tempPersian = tempP,
                    apparentTempVal = adjApparent,
                    apparentTempPersian = apparentP,
                    relativeHumidityVal = humidityVal,
                    relativeHumidityPersian = humidityP,
                    windSpeedVal = adjWindSp,
                    windSpeedPersian = windSpeedPersian,
                    windGustsVal = adjWindGusts,
                    windGustsPersian = windGustsP,
                    prob = probVal,
                    probPersian = PersianDateHelper.formatToPersianDigits(probVal),
                    precipitationMm = precMm,
                    precipitationPersian = precP,
                    cloudCoverVal = clouds,
                    cloudCoverPersian = cloudsP,
                    visibilityMeters = vis,
                    visibilityPersian = visP,
                    freezingLevelMeters = freezing,
                    freezingLevelPersian = freezingP,
                    uvIndexVal = rawHourlyUv,
                    uvIndexPersian = uvIndexP,
                    windDirectionVal = rawWindDir,
                    windDirectionPersian = windDirP,
                    pressureVal = rawPress,
                    pressurePersian = pressP,
                    isDay = hourly.isDay?.getOrNull(absoluteIdx) ?: 1
                )
            }

            rawItems.mapIndexed { i, item ->
                val frostbiteWarn = MountaineeringHelper.getFrostbiteWindowText(item.apparentTempVal)

                val nextItem = rawItems.getOrNull(i + 1)
                val suddenChangeWarn = if (nextItem != null) {
                    val windDiff = nextItem.windSpeedVal - item.windSpeedVal
                    val windPercent = if (item.windSpeedVal > 0) (windDiff / item.windSpeedVal) * 100.0 else 0.0
                    val tempDrop = item.tempVal - nextItem.tempVal
                    val pressureDrop = item.pressureVal - nextItem.pressureVal

                    when {
                        windPercent >= 50.0 && windDiff >= 10.0 -> {
                            val pDiff = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.0f", windDiff))
                            "هشدار: افزایش ۵۰٪+ سرعت باد در ساعت بعد (+$pDiff ک‌م/س)"
                        }
                        tempDrop >= 5.0 -> {
                            val pDrop = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", tempDrop))
                            "هشدار: افت ناگهانی دمای $pDrop درجه سانتی‌گراد در ساعت بعد"
                        }
                        pressureDrop >= 2.5 -> {
                            val pDrop = PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", pressureDrop))
                            "هشدار: افت شدید فشار هوای $pDrop هکتوپاسکال (احتمال طوفان) در ساعت بعد"
                        }
                        else -> null
                    }
                } else null

                item.copy(
                    frostbiteWarning = frostbiteWarn,
                    suddenChangeWarning = suddenChangeWarn
                )
            }
        }
    }

    var selectedHourIndex by rememberSaveable(mountain.id, altitude) { mutableStateOf(-1) }
    LaunchedEffect(currentHourIdx, itemsFlow) {
        if (selectedHourIndex == -1 || itemsFlow.none { it.index == selectedHourIndex }) {
            selectedHourIndex = if (currentHourIdx >= 0 && itemsFlow.any { it.index == currentHourIdx }) {
                currentHourIdx
            } else {
                itemsFlow.firstOrNull()?.index ?: 0
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.isDark
    val currentSelectedHour = itemsFlow.find { it.index == selectedHourIndex }
    
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "هواشناسی ۲۴ ساعت آینده (ساعتی)",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = getTextColor()
            )
            val activeElevPersian = remember(altitude) { PersianDateHelper.formatToPersianDigits(altitude) }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF1B2232) else Color(0xFFE3F2FD),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.35f) else Color(0xFF90CAF9))
            ) {
                Text(
                    text = "تراز صعود: $activeElevPersian متر",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(itemsFlow, key = { it.id }) { item ->
                val isSelected = selectedHourIndex == item.index
                val safetyReport = remember(item, hourly, altitude, isDark, mountain, peakOffsetHours, daily) { getHourlySafetyReport(item, hourly, altitude, isDark, mountain, peakOffsetHours, daily = daily) }
                val isPast = item.index < currentHourIdx
                val isCurrent = item.index == currentHourIdx
                
                // Adaptive Background colored according to hourly safety and current theme
                val cardThemeBgColor = remember(isSelected, safetyReport.color, isDark) {
                    if (isSelected) {
                        safetyReport.color.copy(alpha = if (isDark) 0.16f else 0.24f)
                    } else {
                        safetyReport.color.copy(alpha = if (isDark) 0.04f else 0.06f)
                    }
                }
                
                // Safe, high-contrast text and border selectors
                val themeAccentColor = safetyReport.color
                
                val cardThemeBorderColor = remember(isSelected, themeAccentColor, isDark) {
                    if (isSelected) {
                        themeAccentColor
                    } else {
                        themeAccentColor.copy(alpha = if (isDark) 0.25f else 0.38f)
                    }
                }

                val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, cardThemeBorderColor)

                Card(
                    modifier = Modifier
                        .width(86.dp)
                        .alpha(if (isPast) 0.5f else 1.0f),
                    shape = RoundedCornerShape(18.dp),
                    border = borderStroke,
                    colors = CardDefaults.cardColors(
                        containerColor = cardThemeBgColor
                    ),
                    onClick = {
                        selectedHourIndex = item.index
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .width(24.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(if (isDark) Color(0xFF00E5FF) else Color(0xFF00B0FF))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                        Text(
                            text = item.hourPersian,
                            fontSize = 11.sp,
                            color = getTextColor(0.80f),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeAccentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = WeatherCodeHelper.getIcon(item.weatherCode, item.isDay),
                                contentDescription = "Weather Icon",
                                tint = themeAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (item.suddenChangeWarning != null || item.frostbiteWarning != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = if (item.suddenChangeWarning != null) Color(0xFFE65100) else Color(0xFFB91C1C)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.suddenChangeWarning != null) Icons.Default.Bolt else Icons.Default.AcUnit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (item.suddenChangeWarning != null) "جهش" else "سرما",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${item.tempPersian}°",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = getTextColor()
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Micro Line Divider
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(1.dp)
                                .background(getTextColor(0.08f))
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Hourly Wind speed representation
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = "Wind Speed",
                                tint = getTextColor(0.5f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.windSpeedPersian,
                                fontSize = 9.sp,
                                color = getTextColor(0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Precipitation Probability
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Rain/Snow Prob",
                                tint = if (item.prob > 0) Color(0xFF0288D1) else getTextColor(0.3f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%${item.probPersian}",
                                fontSize = 9.sp,
                                color = if (item.prob > 0) Color(0xFF0288D1) else getTextColor(0.4f),
                                fontWeight = if (item.prob > 0) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Advanced Tactical Analyst Cockpit Panel
        currentSelectedHour?.let { item ->
            val report = remember(item, hourly, altitude, isDark, mountain, peakOffsetHours) { getHourlySafetyReport(item, hourly, altitude, isDark, mountain, peakOffsetHours) }
            
            // Soft high-contrast theme colours suited matching light/dark theme perfectly
            val cockpitThemeColor = report.color
            
            val cockpitBg = remember(cockpitThemeColor, isDark) {
                if (isDark) {
                    cockpitThemeColor.copy(alpha = 0.05f)
                } else {
                    cockpitThemeColor.copy(alpha = 0.08f)
                }
            }
            
            val cockpitBorderColor = remember(cockpitThemeColor, isDark) {
                if (isDark) {
                    cockpitThemeColor.copy(alpha = 0.18f)
                } else {
                    cockpitThemeColor.copy(alpha = 0.38f)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, cockpitBorderColor),
                colors = CardDefaults.cardColors(containerColor = cockpitBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Title and separate Status Badge to prevent wrapping overlap
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = cockpitThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "کابین تحلیل ایمنی صعود در ساعت ${item.hourPersian}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = cockpitThemeColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Badge
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = cockpitThemeColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, cockpitThemeColor.copy(alpha = 0.30f))
                            ) {
                                Text(
                                    text = "ضریب ایمنی: ${report.status}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = cockpitThemeColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Advice Badge
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f))
                            ) {
                                Text(
                                    text = report.advice,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(0.8f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Brief tactical analysis text block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(cockpitThemeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = report.message,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = getTextColor(),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (item.frostbiteWarning != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFB91C1C).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFB91C1C).copy(alpha = 0.40f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.frostbiteWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    if (item.suddenChangeWarning != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE65100).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.40f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.suddenChangeWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    // 2x3 Grid of microclimatic details colored dynamically based on hazard severity
                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Temp Card (Apparent Temperature / Wind Chill)
                        val apparentLimit = if (item.apparentTempVal < -20.0) "CRITICAL" else if (item.apparentTempVal < -10.0) "WARNING" else "SAFE"
                        val apparentStatus = getMetricStatus(apparentLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = apparentStatus.border,
                            colors = CardDefaults.cardColors(containerColor = apparentStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(apparentStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = apparentStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "دمای سوزباد (حسی)",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.apparentTempPersian}°C",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = apparentStatus.tint
                                    )
                                }
                            }
                        }

                        // Wind Wind Gusts card
                        val windLimit = MountaineeringHelper.getWindLimit(item.windSpeedVal, item.windGustsVal, altitude)
                        val windStatus = getMetricStatus(windLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = windStatus.border,
                            colors = CardDefaults.cardColors(containerColor = windStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(windStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = windStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "سرعت باد / تندباد",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.windSpeedPersian} / ${item.windGustsPersian} ک‌م/س",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = windStatus.tint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clouds & Visibility
                        val cloudsLimit = if (item.cloudCoverVal > 85 || item.visibilityMeters < 1500.0) "CRITICAL" else if (item.cloudCoverVal > 50 || item.visibilityMeters < 4000.0) "WARNING" else "SAFE"
                        val cloudsStatus = getMetricStatus(cloudsLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = cloudsStatus.border,
                            colors = CardDefaults.cardColors(containerColor = cloudsStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cloudsStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = cloudsStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "پوشش ابر / دید افقی",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.cloudCoverPersian}٪ / ${item.visibilityPersian} ک‌م",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = cloudsStatus.tint
                                    )
                                }
                            }
                        }

                        // Freezing Level
                        val freezingLimit = if (altitude > item.freezingLevelMeters) {
                            if ((altitude - item.freezingLevelMeters) > 2000) "CRITICAL" else "WARNING"
                        } else {
                            "SAFE"
                        }
                        val freezingStatus = getMetricStatus(freezingLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = freezingStatus.border,
                            colors = CardDefaults.cardColors(containerColor = freezingStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(freezingStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AcUnit,
                                        contentDescription = null,
                                        tint = freezingStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "تراز صفر درجه (انجماد)",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.freezingLevelPersian} متر",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = freezingStatus.tint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Precipitation Card
                        val precipitationLimit = if (item.precipitationMm > 5.0 || item.prob > 80 || (item.precipitationMm > 2.0 && item.relativeHumidityVal > 95)) "CRITICAL" else if (item.precipitationMm > 1.0 || item.prob > 40 || item.relativeHumidityVal > 85) "WARNING" else "SAFE"
                        val precipitationStatus = getMetricStatus(precipitationLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = precipitationStatus.border,
                            colors = CardDefaults.cardColors(containerColor = precipitationStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(precipitationStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = precipitationStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بارش و رطوبت نسبی",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.precipitationPersian} م‌م / ${item.relativeHumidityPersian}٪",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = precipitationStatus.tint
                                    )
                                }
                            }
                        }

                        // UV index
                        val uvLimit = if (item.uvIndexVal >= 8.0) "CRITICAL" else if (item.uvIndexVal >= 3.0) "WARNING" else "SAFE"
                        val uvStatus = getMetricStatus(uvLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = uvStatus.border,
                            colors = CardDefaults.cardColors(containerColor = uvStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(uvStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = uvStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "شاخص فرابنفش (UV)",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = if (item.uvIndexPersian == "-") "-" else "${item.uvIndexPersian} UVI",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = uvStatus.tint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wind Direction Card
                        val windDirLimit = if (item.windSpeedVal > 60.0 || item.windGustsVal > 80.0) "CRITICAL" else if (item.windSpeedVal > 35.0 || item.windGustsVal > 50.0) "WARNING" else "SAFE"
                        val windDirStatus = getMetricStatus(windDirLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = windDirStatus.border,
                            colors = CardDefaults.cardColors(containerColor = windDirStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(windDirStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = windDirStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "جهت جریان باد",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = item.windDirectionPersian,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = windDirStatus.tint
                                    )
                                }
                            }
                        }

                        // Air Pressure Card
                        val stdPress = 1013.25 * Math.pow(1.0 - 0.0000225577 * altitude, 5.25588)
                        val pressLimit = if (item.pressureVal < stdPress - 20.0) "CRITICAL" else if (item.pressureVal < stdPress - 10.0) "WARNING" else "SAFE"
                        val pressStatus = getMetricStatus(pressLimit, isDark)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = pressStatus.border,
                            colors = CardDefaults.cardColors(containerColor = pressStatus.bg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pressStatus.tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = pressStatus.tint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "فشار سطحی هوا",
                                        fontSize = 8.sp,
                                        color = getTextColor(0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.pressurePersian} hPa",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = pressStatus.tint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

data class CompiledDailyItem(
    val index: Int,
    val dayName: String,
    val shamsiDate: String,
    val tempMaxPersian: String,
    val tempMinPersian: String,
    val rawTempMax: Double,
    val rawTempMin: Double,
    val weatherCode: Int,
    val weatherDesc: String,
    val safetyLabel: String,
    val safetyColor: Color,
    val safetyDescription: String,
    val safeHoursCount: Int = 24,
    val totalHoursCount: Int = 24,
    val diurnalSwing: Double = 0.0,
    val diurnalSwingWarning: String? = null
)

@Composable
fun DailyForecastSection(
    viewModel: WeatherViewModel,
    daily: com.example.data.remote.DailyData,
    hourly: com.example.data.remote.HourlyData?,
    altitude: Int,
    mountain: com.example.data.local.MountainEntity,
    selectedDaysCount: Int,
    onDaysCountChanged: (Int) -> Unit,
    units: com.example.data.remote.WeatherUnits? = null
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    var expandedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val peakOffsetHours = remember(mountain) {
        com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
    }

    var statsTick by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60000L - (now % 60000L)
            kotlinx.coroutines.delay(delayToNextMinute)
            statsTick = System.currentTimeMillis() / 60000L
        }
    }

    val mountainLocalCalendar = remember(peakOffsetHours, statsTick) {
        val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
        val localTimeMillis = System.currentTimeMillis() + offsetMillis
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localTimeMillis
        }
    }

    val todayDateStr = remember(mountainLocalCalendar) {
        val yr = mountainLocalCalendar.get(java.util.Calendar.YEAR)
        val mn = mountainLocalCalendar.get(java.util.Calendar.MONTH) + 1
        val dy = mountainLocalCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        String.format(java.util.Locale.US, "%04d-%02d-%02d", yr, mn, dy)
    }

    val compiledItems = remember(daily, hourly, altitude, mountain, isDark, todayDateStr, peakOffsetHours, units, statsTick) {
        val daysCount = daily.time.size
        val diff = mountain.altitude - altitude

        val offsetMillis = (peakOffsetHours * 3600 * 1000).toLong()
        val localTimeMillis = System.currentTimeMillis() + offsetMillis
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localTimeMillis
        }
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val tomorrowDateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowDateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))

        val validIndices = java.util.ArrayList<Int>()
        for (idx in daily.time.indices) {
            val dateStr = daily.time.getOrNull(idx) ?: ""
            if (dateStr.isNotEmpty() && dateStr >= todayDateStr) {
                validIndices.add(idx)
            }
        }
        validIndices.map { i ->
            val dateStr = daily.time.getOrNull(i) ?: ""
            val dName = PersianDateHelper.getPersianDayOfWeek(dateStr)
            val shamsi = PersianDateHelper.getJalaliNumericDateString(dateStr)
            
            val label = when (dateStr) {
                todayDateStr -> "امروز"
                tomorrowDateStr -> "فردا"
                dayAfterTomorrowDateStr -> "پس‌فردا"
                else -> dName
            }
            
            val rawTempMax = MountaineeringHelper.normalizeTemperature(daily.temperature2mMax.getOrNull(i))
            val rawTempMin = MountaineeringHelper.normalizeTemperature(daily.temperature2mMin.getOrNull(i))
            var adjTempMax = rawTempMax + (diff * 0.0065)
            var adjTempMin = rawTempMin + (diff * 0.0065)

            val wCode = daily.weatherCode.getOrNull(i) ?: 0
            val wDesc = WeatherCodeHelper.getDescription(wCode)
            
            val rawWindMax = MountaineeringHelper.normalizeWindSpeed(daily.windSpeed10mMax?.getOrNull(i))
            val adjWindSpeedMax = MountaineeringHelper.adjustWindWithAltitude(
                referenceWind = rawWindMax,
                referenceElevation = mountain.altitude.toDouble(),
                targetAltitude = altitude.toDouble(),
                alpha = null
            )

            val rawGustsMax = MountaineeringHelper.normalizeWindSpeed(daily.windGusts10mMax?.getOrNull(i) ?: (rawWindMax * MountaineeringHelper.calculateDynamicGustFactor(null)))
            val adjWindGustsMax = MountaineeringHelper.adjustWindWithAltitude(
                referenceWind = rawGustsMax,
                referenceElevation = mountain.altitude.toDouble(),
                targetAltitude = altitude.toDouble(),
                alpha = null
            )

            val worstTemp = adjTempMin
            val worstWind = adjWindSpeedMax

            val foundIdx = hourly?.time?.indexOfFirst { it.startsWith(dateStr) }?.takeIf { it != -1 } ?: (i * 24)
            val appMinVal = MountaineeringHelper.calculateWindChill(worstTemp, worstWind)
            val rawBaseTempDaily = hourly?.temperature2m?.getOrNull(foundIdx) ?: (daily.temperature2mMin?.getOrNull(i) ?: worstTemp)
            val dailyQnh = hourly?.pressureMsl?.getOrNull(foundIdx) ?: 1013.25
            val adjPressure = MountaineeringHelper.calculateBarometricPressure(
                basePressure = hourly?.surfacePressure?.getOrNull(foundIdx),
                baseTemp = MountaineeringHelper.normalizeTemperature(rawBaseTempDaily),
                baseAltitude = mountain.altitude,
                targetAltitude = altitude,
                targetTemp = worstTemp,
                qnh = dailyQnh
            )
            val dewPointVal = MountaineeringHelper.calculateDewPoint(worstTemp, 65.0)

            val dailySimulatedCurrent = com.example.data.remote.CurrentWeather(
                time = dateStr,
                temperature2m = worstTemp,
                relativeHumidity2m = 65.0,
                apparentTemperature = appMinVal,
                precipitation = daily.precipitationSum?.getOrNull(i) ?: 0.0,
                snowfall = daily.snowfallSum?.getOrNull(i) ?: 0.0,
                weatherCode = wCode,
                windSpeed10m = worstWind,
                windDirection10m = 0.0,
                windSpeed80m = worstWind,
                windDirection80m = 0.0,
                surfacePressure = adjPressure,
                pressureMsl = dailyQnh,
                freezingLevelHeight = MountaineeringHelper.estimateFreezingLevel(worstTemp, altitude).toDouble(),
                windGusts10m = adjWindGustsMax,
                visibility = -1.0,
                cloudCover = -1.0,
                cloudCoverLow = hourly?.cloudCoverLow?.getOrNull(foundIdx),
                cloudCoverMid = hourly?.cloudCoverMid?.getOrNull(foundIdx),
                cloudCoverHigh = hourly?.cloudCoverHigh?.getOrNull(foundIdx),
                soilTemperature0cm = hourly?.soilTemperature0cm?.getOrNull(foundIdx),
                isDay = 1,
                dewPoint2m = dewPointVal,
                cape = hourly?.cape?.getOrNull(foundIdx)
            )

            var finalDailyReport = com.example.ui.util.MountaineeringHelper.evaluateSafety(
                current = dailySimulatedCurrent,
                hourly = hourly,
                daily = daily,
                offsetHours = peakOffsetHours,
                altitudeOverride = altitude,
                hourIndexOverride = foundIdx,
                slopeAngle = mountain.slopeAngle,
                aspect = mountain.aspect,
                summitElevation = mountain.altitude.toDouble(),
                baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
                units = units,
                latitude = mountain.latitude,
                longitude = mountain.longitude
            )

            var safeHoursCount = 24
            var totalHoursCount = 24

            if (hourly != null) {
                val foundIdx = hourly.time.indexOfFirst { it.startsWith(dateStr) }
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val nowPrefix = sdf.format(java.util.Date(localTimeMillis))
                val currentHourIdx = hourly.time.indexOfFirst { it.startsWith(nowPrefix) }

                val startHourIdx = if (dateStr == todayDateStr && currentHourIdx != -1 && currentHourIdx >= foundIdx) {
                    currentHourIdx
                } else if (foundIdx != -1) {
                    foundIdx
                } else {
                    i * 24
                }
                val endHourIdx = if (foundIdx != -1) foundIdx + 23 else i * 24 + 23

                if (startHourIdx < hourly.time.size) {
                    var worstStatus = com.example.ui.util.SafetyStatus.GREEN
                    var worstReport = finalDailyReport
                    var localSafeCount = 0
                    var localTotalCount = 0
                    
                    for (absoluteHourIdx in startHourIdx..endHourIdx) {
                        if (absoluteHourIdx >= hourly.time.size) break
                        localTotalCount++
                        val fullTime = hourly.time.getOrNull(absoluteHourIdx) ?: ""
                        
                        val rawWindSp = hourly.windSpeed80m?.getOrNull(absoluteHourIdx) ?: hourly.windSpeed10m?.getOrNull(absoluteHourIdx) ?: 0.0
                        val normWindSp = MountaineeringHelper.normalizeWindSpeed(rawWindSp)
                        val rawTempVal = hourly.temperature2m.getOrNull(absoluteHourIdx) ?: 0.0
                        val normTempVal = MountaineeringHelper.normalizeTemperature(rawTempVal)
                        val adjTemp = normTempVal + (diff * 0.0065)
                        
                        if (adjTemp > adjTempMax) adjTempMax = adjTemp
                        if (adjTemp < adjTempMin) adjTempMin = adjTemp

                        val adjWindSp = MountaineeringHelper.adjustWindWithAltitude(
                            referenceWind = normWindSp,
                            referenceElevation = mountain.altitude.toDouble(),
                            targetAltitude = altitude.toDouble(),
                            alpha = null
                        )
                        val isNightHour2 = hourly.isDay?.getOrNull(absoluteHourIdx) == 0
                        val adjApparent = com.example.ui.util.MountaineeringHelper.calculateWindChill(adjTemp, adjWindSp, isNight = isNightHour2)
                        val humidityVal = hourly.relativeHumidity2m?.getOrNull(absoluteHourIdx)?.toInt() ?: 60
                        val rawWindGusts = hourly.windGusts10m?.getOrNull(absoluteHourIdx) ?: (rawWindSp * MountaineeringHelper.calculateDynamicGustFactor(hourly.cape?.getOrNull(absoluteHourIdx)))
                        val normWindGusts = MountaineeringHelper.normalizeWindSpeed(rawWindGusts)
                        val adjWindGusts = MountaineeringHelper.adjustWindWithAltitude(
                            referenceWind = normWindGusts,
                            referenceElevation = mountain.altitude.toDouble(),
                            targetAltitude = altitude.toDouble(),
                            alpha = null
                        )
                        val precMm = hourly.precipitation?.getOrNull(absoluteHourIdx) ?: 0.0
                        val snowfallVal = hourly.snowfall?.getOrNull(absoluteHourIdx) ?: 0.0
                        val clouds = hourly.cloudCover?.getOrNull(absoluteHourIdx)?.toInt() ?: -1
                        val freezing = hourly.freezingLevelHeight?.getOrNull(absoluteHourIdx) ?: 0.0
                        val dewPointValHour = com.example.ui.util.MountaineeringHelper.calculateDewPoint(adjTemp, humidityVal.toDouble())
                        val hourQnh = hourly.pressureMsl?.getOrNull(absoluteHourIdx) ?: 1013.25
                        val adjPressureHour = com.example.ui.util.MountaineeringHelper.calculateBarometricPressure(hourly.surfacePressure?.getOrNull(absoluteHourIdx), normTempVal, mountain.altitude, altitude, qnh = hourQnh)

                        val hourSimulatedCurrent = com.example.data.remote.CurrentWeather(
                            time = fullTime,
                            temperature2m = adjTemp,
                            relativeHumidity2m = humidityVal.toDouble(),
                            apparentTemperature = adjApparent,
                            precipitation = precMm,
                            snowfall = snowfallVal,
                            weatherCode = hourly.weatherCode.getOrNull(absoluteHourIdx) ?: 0,
                            windSpeed10m = adjWindSp,
                            windDirection10m = 0.0,
                            windSpeed80m = adjWindSp,
                            windDirection80m = 0.0,
                            surfacePressure = adjPressureHour,
                            pressureMsl = hourQnh,
                            freezingLevelHeight = freezing,
                            windGusts10m = adjWindGusts,
                            visibility = hourly.visibility?.getOrNull(absoluteHourIdx) ?: -1.0,
                            cloudCover = if (clouds == -1) -1.0 else clouds.toDouble(),
                            cloudCoverLow = hourly.cloudCoverLow?.getOrNull(absoluteHourIdx),
                            cloudCoverMid = hourly.cloudCoverMid?.getOrNull(absoluteHourIdx),
                            cloudCoverHigh = hourly.cloudCoverHigh?.getOrNull(absoluteHourIdx),
                            soilTemperature0cm = hourly.soilTemperature0cm?.getOrNull(absoluteHourIdx),
                            isDay = hourly.isDay?.getOrNull(absoluteHourIdx) ?: 1,
                            dewPoint2m = dewPointValHour,
                            cape = hourly.cape?.getOrNull(absoluteHourIdx)
                        )

                        val hourReport = com.example.ui.util.MountaineeringHelper.evaluateSafety(
                            current = hourSimulatedCurrent,
                            hourly = hourly,
                            daily = daily,
                            offsetHours = peakOffsetHours,
                            altitudeOverride = altitude,
                            hourIndexOverride = absoluteHourIdx,
                            slopeAngle = mountain.slopeAngle,
                            aspect = mountain.aspect,
                            summitElevation = mountain.altitude.toDouble(),
                            baseElevation = (mountain.altitude - 1500.0).coerceAtLeast(1000.0),
                            units = units,
                            latitude = mountain.latitude,
                            longitude = mountain.longitude
                        )

                        if (hourReport.status == com.example.ui.util.SafetyStatus.GREEN) {
                            localSafeCount++
                        }

                        val scoreWeight = when (hourReport.status) {
                            com.example.ui.util.SafetyStatus.RED -> 3
                            com.example.ui.util.SafetyStatus.YELLOW -> 2
                            com.example.ui.util.SafetyStatus.GREEN -> 1
                        }
                        
                        val worstWeight = when (worstStatus) {
                            com.example.ui.util.SafetyStatus.RED -> 3
                            com.example.ui.util.SafetyStatus.YELLOW -> 2
                            com.example.ui.util.SafetyStatus.GREEN -> 1
                        }

                        if (scoreWeight > worstWeight) {
                            worstStatus = hourReport.status
                            worstReport = hourReport
                        } else if (scoreWeight == worstWeight) {
                            if (hourReport.riskScore > worstReport.riskScore) {
                                worstReport = hourReport
                            }
                        }
                    }
                    finalDailyReport = worstReport
                    safeHoursCount = localSafeCount
                    totalHoursCount = localTotalCount
                }
            }

            val absMax = kotlin.math.abs(adjTempMax)
            val absMin = kotlin.math.abs(adjTempMin)
            val signMax = if (adjTempMax < 0.0) "-" else ""
            val signMin = if (adjTempMin < 0.0) "-" else ""
            val tMaxP = "\u200E$signMax${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absMax))}"
            val tMinP = "\u200E$signMin${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absMin))}"

            val diurnalSwing = (adjTempMax - adjTempMin).coerceAtLeast(0.0)
            val diurnalSwingWarning = if (diurnalSwing >= 15.0) {
                "هشدار: نوسان شدید دمای شبانه‌روزی (${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", diurnalSwing))} درجه تفاوت بین روز و شب) - خطر هیپوترمی ناگهانی و لزوم لایه‌بندی زودهنگام لباس و تجهیزات خواب مناسب"
            } else null

            val (safetyLabelVal, safetyColorVal, safetyDescVal) = when (finalDailyReport.status) {
                com.example.ui.util.SafetyStatus.RED -> {
                    val c = if (isDark) Color(0xFFFF5252) else Color(0xFFB91C1C)
                    Triple("صعود پرخطر", c, "تحلیل ایمنی قله: " + finalDailyReport.description)
                }
                com.example.ui.util.SafetyStatus.YELLOW -> {
                    val c = if (isDark) Color(0xFFFFA726) else Color(0xFFB45309)
                    Triple("نیازمند احتیاط", c, "تحلیل ایمنی قله: " + finalDailyReport.description)
                }
                else -> {
                    val c = if (isDark) Color(0xFF00FF87) else Color(0xFF15803D)
                    Triple("صعود مناسب", c, "تحلیل ایمنی قله: " + finalDailyReport.description)
                }
            }

            CompiledDailyItem(
                index = i,
                dayName = label,
                shamsiDate = shamsi,
                tempMaxPersian = tMaxP,
                tempMinPersian = tMinP,
                rawTempMax = rawTempMax,
                rawTempMin = rawTempMin,
                weatherCode = wCode,
                weatherDesc = wDesc,
                safetyLabel = safetyLabelVal,
                safetyColor = safetyColorVal,
                safetyDescription = safetyDescVal,
                safeHoursCount = safeHoursCount,
                totalHoursCount = totalHoursCount,
                diurnalSwing = diurnalSwing,
                diurnalSwingWarning = diurnalSwingWarning
            )
        }
    }

    val filteredItems = remember(compiledItems, selectedDaysCount) {
        compiledItems.take(selectedDaysCount)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "پیش‌بینی و زمان‌بندی روزانه صعود",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = getTextColor()
            )
            val elevPersian = remember(altitude) { PersianDateHelper.formatToPersianDigits(altitude) }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF1B2232) else Color(0xFFE3F2FD),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.35f) else Color(0xFF90CAF9))
            ) {
                Text(
                    text = "تراز صعود: $elevPersian متر",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = getCardBorderStroke(getAccentColor()),
            colors = CardDefaults.cardColors(
                containerColor = getCardBgColor(Color(0xFF10141D))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(3, 7, 16).forEach { days ->
                        val isLocked = !isPremium && days > 3
                        val isSelected = selectedDaysCount == days && !isLocked
                        val label = when (days) {
                            3 -> "۳ روزه"
                            7 -> if (isLocked) "۷ روزه 🔒" else "۷ روزه"
                            else -> if (isLocked) "۱۶ روزه 🔒" else "۱۶ روزه"
                        }
                        val chipBg = if (isSelected) {
                            if (isDark) Color(0xFF1B253D) else Color(0xFFE3F2FD)
                        } else {
                            Color.Transparent
                        }
                        val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)
                        val chipText = if (isSelected) {
                            if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                        } else if (isLocked) {
                            goldColor.copy(alpha = 0.9f)
                        } else {
                            getTextColor(0.55f)
                        }
                        val chipBorder = if (isSelected) {
                            BorderStroke(1.2.dp, if (isDark) Color(0xFF3F51B5).copy(alpha = 0.85f) else Color(0xFF2196F3))
                        } else if (isLocked) {
                            BorderStroke(1.dp, goldColor.copy(alpha = 0.35f))
                        } else {
                            BorderStroke(1.dp, getTextColor(0.08f))
                        }

                        Surface(
                            onClick = { 
                                if (isLocked) {
                                    viewModel.triggerBilling(true)
                                } else {
                                    onDaysCountChanged(days)
                                    expandedIndex = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isLocked) FontWeight.Black else FontWeight.Bold,
                                color = chipText,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                filteredItems.forEach { item ->
                    val i = item.index
                    val isExpanded = expandedIndex == i

                    val animatedBgColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isExpanded) {
                            if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f)
                        } else {
                            Color.Transparent
                        },
                        label = "daily_item_bg"
                    )

                    val animatedBorderColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isExpanded) {
                            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
                        } else {
                            Color.Transparent
                        },
                        label = "daily_item_border"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(animatedBgColor)
                            .then(
                                if (isExpanded) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = animatedBorderColor,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                expandedIndex = if (isExpanded) null else i
                            }
                            .padding(vertical = 4.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(0.75f)) {
                                Text(
                                    text = item.dayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = getTextColor()
                                )
                                Text(
                                    text = item.shamsiDate,
                                    fontSize = 11.sp,
                                    color = getTextColor(0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Row(
                                modifier = Modifier.weight(1.95f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(item.safetyColor.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.2.dp, item.safetyColor.copy(alpha = 0.35f)), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = WeatherCodeHelper.getIcon(item.weatherCode),
                                        contentDescription = "Daily Weather Icon",
                                        tint = item.safetyColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.weatherDesc,
                                    fontSize = 12.sp,
                                    color = getTextColor(0.85f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.weight(1.3f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.tempMaxPersian}°",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (isDark) Color(0xFFFF8A65) else Color(0xFFD84315)
                                )
                                Text(
                                    text = " / ",
                                    color = getTextColor(0.35f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.tempMinPersian}°",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand Status",
                                    tint = getTextColor(0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isExpanded) {
                            DailyDetailExpandablePanel(
                                daily = daily,
                                index = i,
                                altitude = altitude,
                                mountain = mountain,
                                item = item
                            )
                        }
                    }

                    if (i < filteredItems.size - 1) {
                        HorizontalDivider(
                            color = getTextColor(0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
}

data class DailyDetailStats(
    val windSpeedMax: Double,
    val windGustsMax: Double,
    val windDirStr: String,
    val precipProbMax: Int,
    val uvMax: Double,
    val appMin: Double,
    val appMax: Double,
    val precipSum: Double,
    val snowfallSum: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val moonName: String,
    val moonEmoji: String,
    val moonIllum: Double,
    val moonriseTime: String,
    val moonsetTime: String,
    val alpineStart: String,
    val descentDeadline: String,
    val safetyStatus: String,
    val safetyBadgeColor: Color,
    val safetyDescription: String,
    val climbMoonAltitude: Double,
    val climbEffectiveIllum: Double,
    val weatherCode: Int = 0
)

@Composable
fun DailyDetailExpandablePanel(
    daily: com.example.data.remote.DailyData,
    index: Int,
    altitude: Int,
    mountain: com.example.data.local.MountainEntity,
    item: CompiledDailyItem
) {
    val stats = remember(daily, index, altitude, mountain, item) {
        val rawWindMax = daily.windSpeed10mMax?.getOrNull(index) ?: 0.0
        val windDirDegVal = daily.windDirection10mDominant?.getOrNull(index) ?: 0.0
        val windDirStrVal = MountaineeringHelper.getWindDirectionPersian(windDirDegVal)
        
        val diff = mountain.altitude - altitude
        
        val adjWindSpeedMax = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = rawWindMax,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = altitude.toDouble(),
            alpha = null
        )
        val rawGustsMax = daily.windGusts10mMax?.getOrNull(index) ?: (rawWindMax * MountaineeringHelper.calculateDynamicGustFactor(null))
        val adjWindGustsMax = MountaineeringHelper.adjustWindWithAltitude(
            referenceWind = rawGustsMax,
            referenceElevation = mountain.altitude.toDouble(),
            targetAltitude = altitude.toDouble(),
            alpha = null
        )

        val rawTempMax = daily.temperature2mMax.getOrNull(index) ?: 0.0
        val rawTempMin = daily.temperature2mMin.getOrNull(index) ?: 0.0
        val adjTempMax = rawTempMax + (diff * 0.0065)
        val adjTempMin = rawTempMin + (diff * 0.0065)

        val appMinVal = MountaineeringHelper.calculateWindChill(adjTempMin, adjWindSpeedMax)
        val appMaxVal = MountaineeringHelper.calculateWindChill(adjTempMax, adjWindSpeedMax)

        val precipProbMaxVal = daily.precipitationProbabilityMax?.getOrNull(index) ?: 0
        val rawUv = daily.uvIndexMax?.getOrNull(index) ?: 0.0
        val precipSumVal = daily.precipitationSum?.getOrNull(index) ?: 0.0
        val snowfallSumVal = daily.snowfallSum?.getOrNull(index) ?: 0.0
        val hasDaySnow = snowfallSumVal > 0.0 || (precipSumVal > 0.0 && adjTempMin <= 0.0)
        val dateStr = daily.time.getOrNull(index) ?: ""

        val peakOffsetHours = com.example.ui.util.AstronomicalCalculator.getStandardTimezoneOffset(mountain.name, mountain.latitude, mountain.longitude)
        val adjUvVal = MountaineeringHelper.calculateResolvedUvIndex(
            current = com.example.data.remote.CurrentWeather(
                time = "${dateStr}T12:00",
                isDay = 1
            ),
            hourly = null,
            altitude = altitude,
            mountainAltitude = mountain.altitude,
            snowCover = hasDaySnow,
            snowfallRate = snowfallSumVal,
            daily = daily,
            hourlyIndex = index * 24 + 12,
            offsetHours = peakOffsetHours
        )

        val wCode = daily.weatherCode.getOrNull(index) ?: 0
        val parsedDate = try {
            val parts = dateStr.split("-")
            val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val mn = parts.getOrNull(1)?.toIntOrNull() ?: 6
            val dy = parts.getOrNull(2)?.toIntOrNull() ?: 14
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.set(yr, mn - 1, dy, 12, 0, 0)
            cal.time
        } catch (e: Exception) {
            java.util.Date()
        }

        val sunTimesUTC = try {
            com.example.ui.util.AstronomicalCalculator.calculateSunriseSunsetUTC(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = parsedDate
            )
        } catch (e: Exception) {
            null
        }

        val sunriseTimeVal: String
        val sunsetTimeVal: String
        val alpineStartVal: String
        val descentDeadlineVal: String

        if (sunTimesUTC != null && !sunTimesUTC.isAlwaysBelow && !sunTimesUTC.isAlwaysAbove) {
            val riseUTC = sunTimesUTC.sunriseUTC ?: 6.0
            val setUTC = sunTimesUTC.sunsetUTC ?: 18.0
            val peakSunriseHour = ((riseUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
            val peakSunsetHour = ((setUTC + peakOffsetHours) % 24.0 + 24.0) % 24.0
            val formattedSunrise = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakSunriseHour)
            val formattedSunset = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(peakSunsetHour)
            sunriseTimeVal = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedSunrise)
            sunsetTimeVal = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedSunset)

            val alpineStartHour = (peakSunriseHour - 3.0 + 24.0) % 24.0
            val alpineEndHour = (peakSunsetHour - 6.0 + 24.0) % 24.0
            val formattedAlpineStart = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(alpineStartHour)
            val formattedAlpineEnd = com.example.ui.util.AstronomicalCalculator.formatFractionalHour(alpineEndHour)
            val alpineStartP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedAlpineStart)
            val alpineEndP = "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(formattedAlpineEnd)

            if (adjWindSpeedMax > 24.0 || precipProbMaxVal > 35 || adjTempMin < -10.0) {
                alpineStartVal = "شروع محافظه‌کارانه: $alpineStartP الی \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(com.example.ui.util.AstronomicalCalculator.formatFractionalHour((alpineStartHour + 1.5) % 24.0))} (تأخیر به دلیل ناپایداری‌های جوی تراز صعود)"
            } else {
                alpineStartVal = "زمان طلایی حرکت آلپاین: $alpineStartP الی \u200E${com.example.ui.util.PersianDateHelper.formatToPersianDigits(com.example.ui.util.AstronomicalCalculator.formatFractionalHour((alpineStartHour + 1.5) % 24.0))} بامداد (حرکت زودهنگام استاندارد)"
            }

            if (adjWindSpeedMax > 21.0 || precipProbMaxVal > 30 || wCode in listOf(51, 61, 71, 80)) {
                descentDeadlineVal = "ضرب‌الاجل بازگشت: ساعت $alpineEndP صبح (تضمین فرود قبل از ناپایداری‌های ظهرگاهی)"
            } else {
                descentDeadlineVal = "بازه فرود ایمن نهایی: قبل از ساعت $alpineEndP ظهر (ضرب‌الاجل جبهه هوا و طوفان حرارتی)"
            }
        } else {
            val errMsg = if (sunTimesUTC == null) "محاسبه زمان‌ها با مشکل مواجه شد. لطفاً دوباره تلاش کنید." else "اطلاعات نجومی در دسترس نیست"
            sunriseTimeVal = com.example.ui.util.PersianDateHelper.formatToPersianDigits(if (sunTimesUTC?.isAlwaysAbove == true) "دائم" else errMsg)
            sunsetTimeVal = com.example.ui.util.PersianDateHelper.formatToPersianDigits(if (sunTimesUTC?.isAlwaysAbove == true) "دائم" else errMsg)
            alpineStartVal = if (sunTimesUTC?.isAlwaysAbove == true) "شروع حرکت بر اساس شرایط صخره‌ای" else errMsg
            descentDeadlineVal = if (sunTimesUTC?.isAlwaysAbove == true) "بازه فرود آزاد" else errMsg
        }

        var moonNameVal = ""
        var moonEmojiVal = ""
        var moonIllumVal = 0.0
        var moonriseVal = ""
        var moonsetVal = ""
        var climbMoonAltitudeVal = -90.0
        var climbEffectiveIllumVal = 0.0
        try {
            val parsedDateEndOfDay = try {
                val parts = dateStr.split("-")
                val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                val mn = parts.getOrNull(1)?.toIntOrNull() ?: 6
                val dy = parts.getOrNull(2)?.toIntOrNull() ?: 14
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                cal.set(yr, mn - 1, dy, 23, 59, 59)
                val offsetMinutes = (peakOffsetHours * 60).toInt()
                cal.add(java.util.Calendar.MINUTE, -offsetMinutes)
                cal.time
            } catch (e: Exception) {
                parsedDate
            }

            val details = com.example.ui.util.AstronomicalCalculator.getLunarDetails(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = parsedDateEndOfDay,
                tzOffset = peakOffsetHours
            )
            val riseUTC = details.moonTimes.moonriseUTC
            val setUTC = details.moonTimes.moonsetUTC
            val localRise = riseUTC?.let { ((it + peakOffsetHours) % 24.0 + 24.0) % 24.0 }
            val localSet = setUTC?.let { ((it + peakOffsetHours) % 24.0 + 24.0) % 24.0 }
            val riseStr = if (details.moonTimes.isAlwaysAbove) {
                "دائم"
            } else if (details.moonTimes.isAlwaysBelow) {
                "--:--"
            } else {
                com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localRise)
            }
            val setStr = if (details.moonTimes.isAlwaysAbove) {
                "دائم"
            } else if (details.moonTimes.isAlwaysBelow) {
                "--:--"
            } else {
                com.example.ui.util.AstronomicalCalculator.formatFractionalHour(localSet)
            }
            moonNameVal = details.phaseName
            moonEmojiVal = details.phaseSymbol
            moonIllumVal = details.illuminationPercent
            moonriseVal = if (riseStr == "دائم" || riseStr.contains("-")) riseStr else "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(riseStr)
            moonsetVal = if (setStr == "دائم" || setStr.contains("-")) setStr else "\u200E" + com.example.ui.util.PersianDateHelper.formatToPersianDigits(setStr)

            // Calculate moon state at 2:00 AM local time of this day (peak night climbing hour)
            val localClimbingDate = try {
                val parts = dateStr.split("-")
                val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                val mn = parts.getOrNull(1)?.toIntOrNull() ?: 6
                val dy = parts.getOrNull(2)?.toIntOrNull() ?: 14
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                cal.set(yr, mn - 1, dy, 2, 0, 0)
                val offsetMinutes = (peakOffsetHours * 60).toInt()
                cal.add(java.util.Calendar.MINUTE, -offsetMinutes)
                cal.time
            } catch (e: Exception) {
                parsedDate
            }

            climbMoonAltitudeVal = com.example.ui.util.AstronomicalCalculator.getMoonAltitude(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = localClimbingDate
            )
            
            val climbDetails = com.example.ui.util.AstronomicalCalculator.getLunarDetails(
                latitude = mountain.latitude,
                longitude = mountain.longitude,
                elevation = altitude.toDouble(),
                date = localClimbingDate,
                tzOffset = peakOffsetHours
            )
            
            climbEffectiveIllumVal = if (
                com.example.ui.util.AstronomicalCalculator.isMoonAboveHorizon(
                    latitude = mountain.latitude,
                    longitude = mountain.longitude,
                    elevation = altitude.toDouble(),
                    date = localClimbingDate
                )
            ) {
                val altRad = Math.toRadians(climbMoonAltitudeVal)
                val factor = Math.sin(altRad).coerceIn(0.0, 1.0)
                climbDetails.illuminationPercent * factor
            } else {
                0.0
            }
        } catch (e: Exception) {
            moonNameVal = "اطلاعات نجومی در دسترس نیست"
            moonEmojiVal = "⚠️"
            moonIllumVal = 0.0
            moonriseVal = "اطلاعات نجومی در دسترس نیست"
            moonsetVal = "اطلاعات نجومی در دسترس نیست"
            climbMoonAltitudeVal = -90.0
            climbEffectiveIllumVal = 0.0
        }

        DailyDetailStats(
            windSpeedMax = adjWindSpeedMax,
            windGustsMax = adjWindGustsMax,
            windDirStr = windDirStrVal,
            precipProbMax = precipProbMaxVal,
            uvMax = adjUvVal,
            appMin = appMinVal,
            appMax = appMaxVal,
            precipSum = precipSumVal,
            snowfallSum = snowfallSumVal,
            sunriseTime = sunriseTimeVal,
            sunsetTime = sunsetTimeVal,
            moonName = moonNameVal,
            moonEmoji = moonEmojiVal,
            moonIllum = moonIllumVal,
            moonriseTime = moonriseVal,
            moonsetTime = moonsetVal,
            alpineStart = alpineStartVal,
            descentDeadline = descentDeadlineVal,
            safetyStatus = item.safetyLabel,
            safetyBadgeColor = item.safetyColor,
            safetyDescription = item.safetyDescription,
            climbMoonAltitude = climbMoonAltitudeVal,
            climbEffectiveIllum = climbEffectiveIllumVal,
            weatherCode = wCode
        )
    }

    val isDark = MaterialTheme.colorScheme.background.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(stats.safetyBadgeColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .border(1.dp, stats.safetyBadgeColor.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(stats.safetyBadgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stats.safetyStatus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = stats.safetyBadgeColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stats.safetyDescription,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                color = getTextColor(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of 6 metrics (2 columns, 3 rows)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Column 1
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val windLimit = MountaineeringHelper.getWindLimit(stats.windSpeedMax, stats.windGustsMax, altitude)
                val windStatus = getMetricStatus(windLimit, isDark)
                DailyMetricBadge(
                    icon = Icons.Default.Air,
                    title = "سرعت و جهت باد قله",
                    value = "${PersianDateHelper.formatToPersianDigits(stats.windSpeedMax.toInt())} ک.م/س",
                    subtitle = "تندباد: ${PersianDateHelper.formatToPersianDigits(stats.windGustsMax.toInt())} | جهت: ${stats.windDirStr}",
                    tint = windStatus.tint,
                    statusBg = windStatus.bg,
                    statusBorder = windStatus.border
                )

                val isFreezingRain = stats.weatherCode in listOf(56, 57, 66, 67)
                val precipLimit = if (isFreezingRain || stats.precipProbMax > 80) "CRITICAL" else if (stats.precipProbMax > 40) "WARNING" else "SAFE"
                val precipStatus = getMetricStatus(precipLimit, isDark)
                DailyMetricBadge(
                    icon = Icons.Default.WaterDrop,
                    title = "شانس بارش و بارندگی",
                    value = "%${PersianDateHelper.formatToPersianDigits(stats.precipProbMax)}",
                    subtitle = if (isFreezingRain) "هشدار بحرانی: احتمال باران یخ‌زده" else if (stats.precipProbMax > 40) "آمادگی حداکثر نفوذ باران/برف" else "ریسک بارندگی ناچیز منطقه",
                    tint = precipStatus.tint,
                    statusBg = precipStatus.bg,
                    statusBorder = precipStatus.border
                )

                val rainLimit = if (isFreezingRain || stats.precipSum > 15.0) "CRITICAL" else if (stats.precipSum > 5.0) "WARNING" else "SAFE"
                val rainStatus = getMetricStatus(rainLimit, isDark)
                val precipPersian = "\u200E${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", stats.precipSum))}"
                DailyMetricBadge(
                    icon = Icons.Default.Umbrella,
                    title = "ارتفاع بارندگی مایع (باران)",
                    value = "$precipPersian میلی‌متر",
                    subtitle = if (isFreezingRain) "هشدار بحرانی: باران یخ‌زده و خطرات لایه یخی" else if (stats.precipSum > 15.0) "ریسک بالای گل‌ولای و سیلاب کوهستان" else if (stats.precipSum > 5.0) "بارش قابل توجه و لغزندگی مسیر" else "عدم بارندگی موثر مایع",
                    tint = rainStatus.tint,
                    statusBg = rainStatus.bg,
                    statusBorder = rainStatus.border
                )
            }

            // Column 2
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val appLimit = if (stats.appMin < -20.0) "CRITICAL" else if (stats.appMin < -10.0) "WARNING" else "SAFE"
                val appStatus = getMetricStatus(appLimit, isDark)
                val absAppMin = kotlin.math.abs(stats.appMin)
                val absAppMax = kotlin.math.abs(stats.appMax)
                val appMinSign = if (stats.appMin < 0.0) "-" else if (stats.appMin > 0.0) "+" else ""
                val appMaxSign = if (stats.appMax < 0.0) "-" else if (stats.appMax > 0.0) "+" else ""
                val appMinP = "\u200E$appMinSign${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absAppMin))}"
                val appMaxP = "\u200E$appMaxSign${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absAppMax))}"
                DailyMetricBadge(
                    icon = Icons.Default.SevereCold,
                    title = "سرمای حسی قله (سوزباد)",
                    value = "$appMinP° الی $appMaxP°C",
                    subtitle = "سرمای موثر ناشی از سرعت باد",
                    tint = appStatus.tint,
                    statusBg = appStatus.bg,
                    statusBorder = appStatus.border
                )

                val uvLimit = if (stats.uvMax >= 8.0) "CRITICAL" else if (stats.uvMax >= 3.0) "WARNING" else "SAFE"
                val uvStatus = getMetricStatus(uvLimit, isDark)
                DailyMetricBadge(
                    icon = Icons.Default.WbSunny,
                    title = "شدت فرابنفش تراز نهایی",
                    value = "\u200E${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", stats.uvMax))} UVI",
                    subtitle = when {
                        stats.uvMax <= 0.0 -> "بدون تابش خورشیدی (شب)"
                        stats.uvMax >= 11.0 -> "فرابحرانی (خطر پوست و چشم)"
                        stats.uvMax >= 8.0 -> "بسیار زیاد (برف‌کوری شدید)"
                        stats.uvMax >= 6.0 -> "زیاد (عینک Cat 3/4 و کلاه)"
                        stats.uvMax >= 3.0 -> "متوسط (عینک آفتابی کوه)"
                        else -> "کم و ایمن (Low)"
                    },
                    tint = uvStatus.tint,
                    statusBg = uvStatus.bg,
                    statusBorder = uvStatus.border
                )

                val snowLimit = if (stats.snowfallSum > 25.0) "CRITICAL" else if (stats.snowfallSum > 5.0) "WARNING" else "SAFE"
                val snowStatus = getMetricStatus(snowLimit, isDark)
                val snowPersian = "\u200E${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", stats.snowfallSum))}"
                DailyMetricBadge(
                    icon = Icons.Default.AcUnit,
                    title = "انباشت برف تازه قله",
                    value = "$snowPersian سانتی‌متر",
                    subtitle = if (stats.snowfallSum > 25.0) "ریسک بالای سقوط بهمن و محو پوشش مسیر" else if (stats.snowfallSum > 5.0) "نشست برف و کندی حرکت" else "شرایط نرمال پوشش برف مسیر",
                    tint = snowStatus.tint,
                    statusBg = snowStatus.bg,
                    statusBorder = snowStatus.border
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom linear strips with extended/beautiful data (Sunrise/sunset, moon phase and alpine golden windows)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Daylight & Sun Windows (Neutral slate style)
            DailyLongMetricPanel(
                icon = Icons.Default.WbSunny,
                title = "زمان‌بندی روشنایی روز و بازه صعود آفتاب",
                value = "طلوع خورشید: ${stats.sunriseTime}   |   غروب خورشید: ${stats.sunsetTime}",
                subtitle = "ساعات ایمن نوری تراز قله. از صعود بدون ابزار پیشرفته روشنایی جلوگیری شود.",
                tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                bg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)),
                iconTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            )

            // 2. Night Atmosphere & Moon Tracking (Beautiful Lunar Slate Style with Dynamic Climbing Brightness)
            val effectiveIllumFormatted = "\u200E${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", stats.climbEffectiveIllum))}"
            val rawIllumFormatted = "\u200E${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", stats.moonIllum))}"
            val absMoonAlt = kotlin.math.abs(stats.climbMoonAltitude)
            val moonAltSign = if (stats.climbMoonAltitude < 0.0) "-" else ""
            val moonAltitudeFormatted = "\u200E$moonAltSign${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.1f", absMoonAlt))}"
            
            val climbMoonStatusText = if (stats.climbMoonAltitude >= 0.0) {
                "ماه بالای خط افق قله (زاویه ارتفاع: $moonAltitudeFormatted° / بازتاب اتمسفری مؤثر: $effectiveIllumFormatted٪)"
            } else {
                "ماه زیر خط افق کوهستان (تاریکی مطلق / بازتاب مؤثر: ۰٪)"
            }
            
            val moonAdvisoryText = if (stats.climbMoonAltitude >= 0.0) {
                if (stats.climbEffectiveIllum >= 45.0) {
                    "مهتاب بسیار درخشان و بازتاب مطلوب محیطی؛ دید مناسب چشمی روی یخچال‌ها و شیب‌ها جهت کاهش خستگی چشم و ذخیره انرژی باتری هدلامپ."
                } else {
                    "مهتاب ضعیف یا زاویه تابش کم؛ عدم تشخیص کامل جزئیات دوردست؛ پایش مستمر پاکوب با هدلامپ و مسیریاب فعال آفلاین الزامی است."
                }
            } else {
                "ماه در ساعت صعود شبانه (۲ بامداد) زیر افق است؛ ظلمت و تاریکی مطلق کوهستان. همراه داشتن هدلامپ پرقدرت (بالای ۳۰۰ لومن) و باتری پشتیبان کاملاً الزامی است."
            }

            DailyLongMetricPanel(
                icon = Icons.Default.NightsStay,
                title = "پایش اتمسفر شب و فاز ماه (جهت ناوبری تاریکی صعود)",
                value = "وضعیت فاز ماه: ${stats.moonName} (${rawIllumFormatted}٪)\nپایش اتمسفر ۲ بامداد آلپاین: $climbMoonStatusText\nطلوع ماه: ${stats.moonriseTime}   |   غروب ماه: ${stats.moonsetTime}",
                subtitle = "تحلیل فنی ناوبری: $moonAdvisoryText",
                tint = if (isDark) Color(0xFFC7D2FE) else Color(0xFF3F51B5),
                bg = if (isDark) Color(0xFF131A2D) else Color(0xFFECF0FA),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF818CF8).copy(alpha = 0.15f) else Color(0xFFC5CAE9)),
                emojiBadge = stats.moonEmoji
            )

            // 3. Alpine Start & Turnaround Windows (Dynamic Warning/Safety Adaptive Style)
            val alpineColor = stats.safetyBadgeColor
            DailyLongMetricPanel(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "پنجره طلایی آلپاین و آخرین ضرب‌الاجل بازگشت قله",
                value = stats.alpineStart,
                subtitle = stats.descentDeadline,
                tint = alpineColor,
                bg = alpineColor.copy(alpha = 0.05f),
                border = BorderStroke(1.2.dp, alpineColor.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun DailyMetricBadge(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    statusBg: Color,
    statusBorder: BorderStroke
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = statusBorder,
        colors = CardDefaults.cardColors(
            containerColor = statusBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTextColor(0.55f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = tint
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = getTextColor(0.6f),
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
fun DailyLongMetricPanel(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    bg: Color,
    border: BorderStroke,
    emojiBadge: String? = null,
    iconTint: Color = tint
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = bg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTextColor(0.55f)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = tint
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp,
                    color = getTextColor(0.65f)
                )
            }
            if (emojiBadge != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = emojiBadge,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RiskIndicator(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
    displayValue: String? = null,
    isLocked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val isUnspecified = value < 0
    val baseColor = if (isDark) {
        color
    } else {
        when (color) {
            Color(0xFF00FF87) -> Color(0xFF059669) // Dark Emerald Green for light background
            Color(0xFFFFA726) -> Color(0xFFD97706) // Deep Amber Orange for light background
            Color(0xFFFF5252) -> Color(0xFFDC2626) // Deep Red for light background
            else -> color
        }
    }
    val displayColor = if (isLocked) (if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)) else if (isUnspecified) (if (isDark) Color(0xFFFFA726) else Color(0xFFD97706)) else baseColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = getTextColor(0.7f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = displayColor,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(100))
                            .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (isUnspecified) 1.0f else (value / maxValue.toFloat()).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100))
                                .background(displayColor)
                        )
                    }
                    Text(
                        text = if (isUnspecified) "نامشخص" else (displayValue ?: PersianDateHelper.formatToPersianDigits("$value٪")),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = displayColor
                    )
                }
            }
        }
    }
}
