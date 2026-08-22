package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.theme.Vazirmatn
import com.example.ui.theme.isDark
import com.example.ui.util.PersianDateHelper
import com.example.ui.weather.SyncUiState
import com.example.ui.weather.WeatherViewModel
import com.example.ui.weather.ActivationUiState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val promoConfig by viewModel.promoConfig.collectAsStateWithLifecycle()
    val activationCode by viewModel.activationCode.collectAsStateWithLifecycle()
    val subscriptionExpiresAt by viewModel.subscriptionExpiresAt.collectAsStateWithLifecycle()
    val activationUiState by viewModel.activationUiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetActivationUiState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen_root")
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "تنظیمات اپلیکیشن",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_title")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "پیکربندی هویت بصری هوشمند و راهنمای بقاء در ارتفاعات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ==================== SECTION: GOLDEN ALPINIST MEMBERSHIP ====================
            val goldColor = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF9A6A00)
            val goldCardBg = if (isDarkTheme) Color(0xFF0C101B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            val goldTextColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
            val goldTextMuted = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            val goldTextUltraMuted = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            val activeGreen = if (isDarkTheme) Color(0xFF00FFE0) else Color(0xFF00897B)
            val cancelBtnBg = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            val cancelBtnContent = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            val borderAlpha = if (isDarkTheme) 0.15f else 0.25f
            val discountBgAlpha = if (isDarkTheme) 0.15f else 0.22f
            val discountTextColor = if (isDarkTheme) Color(0xFFFF5252) else Color(0xFFC62828)
            val buttonContentColor = if (isDarkTheme) Color(0xFF0C101B) else Color.White

            fun formatUtcToJalali(utcString: String): String {
                if (utcString.isBlank()) return "مادام‌العمر"
                return try {
                    val cleanUtc = if (utcString.contains(".")) {
                        utcString.substringBefore(".")
                    } else {
                        utcString.substringBefore("Z")
                    }
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    val date = format.parse(cleanUtc) ?: return utcString
                    
                    val jalaliDate = PersianDateHelper.getJalaliDateString(date)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                    timeFormat.timeZone = TimeZone.getTimeZone("Asia/Tehran")
                    val timeStr = PersianDateHelper.formatToPersianDigits(timeFormat.format(date))
                    
                    "$jalaliDate - ساعت $timeStr"
                } catch (e: Exception) {
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val date = format.parse(utcString) ?: return utcString
                        PersianDateHelper.getJalaliDateString(date)
                    } catch (ex: Exception) {
                        utcString
                    }
                }
            }

            var codeInput by remember { mutableStateOf("") }

            LaunchedEffect(activationUiState) {
                if (activationUiState is ActivationUiState.Success) {
                    codeInput = ""
                }
            }

            if (isPremium) {
                if (activationCode.isNotBlank()) {
                    // Active premium state using activation code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("premium_membership_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF0C1A14) else Color(0xFFE8F5E9)
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (isDarkTheme) Color(0xFF00C853).copy(alpha = 0.5f) else Color(0xFF4CAF50)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active Premium",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "فعال‌سازی با کد کوهنوردی",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "طرح طلایی با لایسنس فعال است",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkTheme) Color(0xFF00E676) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "پرو 🚀",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "کد فعال‌سازی:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = activationCode,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "تاریخ انقضای اعتبار:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = formatUtcToJalali(subscriptionExpiresAt),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF9A6A00)
                                    )
                                }
                            }

                            Text(
                                text = "هم‌اکنون تمامی قابلیت‌های پیش‌بینی ۷ روزه، رادار ارزیابی بحران‌ها و ترازسنجی چندگانه قله برای شما فعال است. صعود خوبی داشته باشید!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = { viewModel.setPremium(context, false) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "غیرفعال‌سازی کد و بازگشت به نسخه رایگان",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Active premium state via standard billing (Cafe Bazaar)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("premium_membership_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = goldCardBg
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    goldColor,
                                    goldColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(goldColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = "Premium Status",
                                            tint = goldColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "عضویت طلایی صعود",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = goldTextColor
                                        )
                                        Text(
                                            text = "طرح طلایی فعال است",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = activeGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(goldColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "طلایی 👑",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor
                                    )
                                }
                            }

                            Text(
                                text = "هم‌اکنون تمامی قابلیت‌های پیش‌بینی ۷ روزه، رادار ارزیابی بحران‌ها و ترازسنجی چندگانه قله برای شما فعال است. صعود خوبی داشته باشید!",
                                style = MaterialTheme.typography.bodySmall,
                                color = goldTextMuted,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = { viewModel.setPremium(context, false) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cancelBtnBg,
                                    contentColor = cancelBtnContent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لغو اشتراک طلایی (تست و دمو)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Non-premium section: Show normal Cafe Bazaar Purchase Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("premium_membership_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = goldCardBg
                    ),
                    border = BorderStroke(1.dp, goldColor.copy(alpha = borderAlpha))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(goldColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Premium Locked",
                                        tint = goldColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = promoConfig.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = goldTextColor
                                    )
                                    Text(
                                        text = promoConfig.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = goldTextUltraMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!promoConfig.discountBadge.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE11D48).copy(alpha = discountBgAlpha))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = promoConfig.discountBadge!!,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = discountTextColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = promoConfig.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = goldTextMuted,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { viewModel.triggerBilling(true) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = goldColor,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Buy",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = promoConfig.buttonText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // ==================== SECTION: ACTIVATION WITH CODE ====================
            if (!isPremium && activationCode.isNotBlank()) {
                // Suspended or deactivated subscription state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suspended_membership_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF231815) else Color(0xFFFFF3E0)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isDarkTheme) Color(0xFFFF5722).copy(alpha = 0.5f) else Color(0xFFFF5722)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFF5722).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Suspended Status",
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "اشتراک غیرفعال شده است",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "نیاز به بررسی وضعیت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDarkTheme) Color(0xFFFFAB91) else Color(0xFFD84315),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF5722).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "غیرفعال ⚠️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5722)
                                )
                            }
                        }

                        Text(
                            text = "اشتراک متصل به کد فعال‌سازی شما موقتاً غیرفعال یا منقضی شده است. در صورت فعال‌سازی یا تمدید مجدد توسط مدیر سیستم، می‌توانید وضعیت را بررسی مجدد کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "کد فعال‌سازی:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = activationCode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "آخرین تاریخ اعتبار:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatUtcToJalali(subscriptionExpiresAt),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = activationUiState !is ActivationUiState.Idle,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            when (val state = activationUiState) {
                                is ActivationUiState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFFF5722)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "در حال ارتباط با سرور و بررسی اشتراک...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                is ActivationUiState.Success -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is ActivationUiState.Error -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkTheme) Color(0xFFE57373) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.checkSubscriptionOnStartup(context, isManual = true) },
                                enabled = activationUiState !is ActivationUiState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "بررسی مجدد وضعیت اشتراک",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.setPremium(context, false) },
                                enabled = activationUiState !is ActivationUiState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "حذف کد فعال‌سازی و بازگشت به نسخه رایگان",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (!isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_code_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "کد فعال‌سازی",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "فعال‌سازی با کد صعود",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "اگر کد فعال‌سازی یا لایسنس هدیه از باشگاه‌های کوهنوردی، مربیان یا حامیان صعود دریافت کرده‌اید، آن را در کادر زیر وارد کنید تا ویژگی‌های پرو اپلیکیشن برای شما فعال شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )

                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { 
                                codeInput = it
                                if (activationUiState !is ActivationUiState.Idle) {
                                    viewModel.resetActivationUiState()
                                }
                            },
                            label = { Text("کد فعال‌سازی") },
                            placeholder = { Text("مثال: ALP-2026-X9", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_code_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (codeInput.isNotBlank()) {
                                    IconButton(onClick = { codeInput = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن")
                                    }
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = activationUiState !is ActivationUiState.Idle,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            when (val state = activationUiState) {
                                is ActivationUiState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "در حال بررسی و فعال‌سازی اشتراک...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                is ActivationUiState.Success -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (state.expiresAt.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "تاریخ انقضا: ${formatUtcToJalali(state.expiresAt)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57F17),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                is ActivationUiState.Error -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkTheme) Color(0xFFE57373) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        Button(
                            onClick = { viewModel.activateCode(codeInput) },
                            enabled = codeInput.isNotBlank() && activationUiState !is ActivationUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("activation_verify_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "تایید و فعال‌سازی اشتراک صعود",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ==================== SECTION 1: THEME SELECTION ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "تم روشن و تاریک",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "تم روشن و تاریک",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "برای صعود در روز و شرایط نوری مستقیم (تم روشن) و صعودهای زمستانه یا گرگ‌ومیش صبحگاهی (تم تاریک) رنگبندی مناسب را انتخاب کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // System Theme (Auto)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setThemeMode(context, "system") }
                                .testTag("theme_chip_system"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (themeMode == "system") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (themeMode == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (themeMode == "system") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "فعال",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = if (themeMode == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "خودکار سیستم",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "system") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Dark Theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setThemeMode(context, "dark") }
                                .testTag("theme_chip_dark"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (themeMode == "dark") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (themeMode == "dark") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "فعال",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تم تاریک (شب)",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "dark") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Light Theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setThemeMode(context, "light") }
                                .testTag("theme_chip_light"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (themeMode == "light") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (themeMode == "light") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "فعال",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تم روشن (روز)",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "light") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "پیش‌نمایش زنده خوانایی جاده‌ای و شبانه:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قله دماوند (جبهه جنوبی)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "هشدار صعود",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "ارتفاع فرضی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "۴,۲۵۰ متر",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "دمای محسوس",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "۱۲- درجه",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF30D158) else Color(0xFF1E88E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 2: MOUNTAIN DATABASE SYNC ====================
            val syncUiState by viewModel.syncUiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                viewModel.initSyncStates(context)
            }

            val dbVersion by viewModel.dbVersion.collectAsStateWithLifecycle()
            val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
            val lastAddedCount by viewModel.lastSyncAdded.collectAsStateWithLifecycle()
            val lastUpdatedCount by viewModel.lastSyncUpdated.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("db_sync_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = "بروزرسانی خودکار",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "بروزرسانی هوشمند مرجع قله‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(Color(0xFF30D158))
                                )
                                Text(
                                    text = "فعال و هوشمند",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF30D158),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "هنگام اجرای برنامه یا بروز رسانی آب و هوا، آخرین تغییرات اطلس ملی قله‌ها با دیتابیس محلی تلفیق می‌شود. قله‌ها و صعودهای دست‌ساز شما کاملاً محفوظ می‌مانند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "نسخه اطلس",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "v$dbVersion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "زمان بروزرسانی",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (lastSyncTime == "هنوز بروزرسانی انجام نشده") "ثبت نشده" else lastSyncTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (lastAddedCount > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(
                                        1.dp,
                                        if (lastAddedCount > 0) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(if (lastAddedCount > 0) Color(0xFF30D158) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "افزوده: $lastAddedCount قله",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastAddedCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (lastUpdatedCount > 0) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(
                                        1.dp,
                                        if (lastUpdatedCount > 0) Color(0xFFBBDEFB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(if (lastUpdatedCount > 0) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اصلاح: $lastUpdatedCount مورد",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastUpdatedCount > 0) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.triggerMountainSync(context)
                        },
                        enabled = syncUiState !is SyncUiState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (syncUiState is SyncUiState.Loading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("دروازه اتصال... شکیبا باشید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("همگام‌سازی دستی اطلس", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    when (val state = syncUiState) {
                        is SyncUiState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "اطلس با موفقیت به نسخه ${state.version} بروز شد.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                        is SyncUiState.NoUpdate -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "اطلس شما کاملاً بروز است (V${state.version})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        is SyncUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "بررسی ناموفق:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            // ==================== SECTION 3: OFFLINE CACHE MANAGEMENT ====================
            val cachedMountainIds by viewModel.cachedMountainIds.collectAsStateWithLifecycle()
            val cachedCount = cachedMountainIds.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_cache_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "مدیریت حافظه آفلاین",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "مدیریت حافظه آفلاین و کش",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "پیش‌بینی‌های هواشناسی دانلود شده برای قله‌ها جهت دسترسی کاملاً آفلاین در کوهستان در حافظه محلی ذخیره می‌شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 22.sp,
                        fontFamily = Vazirmatn
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعداد قله‌های ذخیره شده: ${PersianDateHelper.formatToPersianDigits(cachedCount)} قله",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Vazirmatn
                        )

                        if (cachedCount > 0) {
                            Button(
                                onClick = { viewModel.clearAllCachedWeather() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "پاکسازی کل کش",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 3.5: APP FEATURES DISCOVERY ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_features_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "ویژگی‌های اپلیکیشن",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ویژگی‌ها و قابلیت‌های مهندسی صعود",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val features = remember {
                        listOf(
                            FeatureItem(
                                title = "پیش‌بینی نقطه‌ای جبهه‌های صعود قله",
                                subtitle = "تحلیل مجزای ترازهای ارتفاعی و تفاوت سوزباد جبهه‌های جغرافیایی",
                                badge = "تخصصی و امنیتی 🏔️",
                                detail = "این سیستم منحصر‌به‌فرد، هواشناسی قله را نه به صورت یک کلیت، بلکه بر اساس ترازهای ارتفاعی مختلف (از کمپ اصلی تا جان‌پناه و کاسه قله) و در چهار جبهه جغرافیایی اصلی تحلیل می‌کند. سرعت باد، دمای محسوس (سوزباد) و شانس بارش در جبهه شمالی همواره با جبهه جنوبی تفاوت دارد؛ با این قابلیت مسیر صعود خود را بر اساس دقیق‌ترین داده‌های جبهه‌ای انتخاب کنید تا ضامن سلامت تیم شما باشد.",
                                icon = Icons.Default.Cloud,
                                color = Color(0xFF0284C7)
                            ),
                            FeatureItem(
                                title = "رادار ارزیابی ریسک و بحران‌های آلپاین",
                                subtitle = "سامانه هوشمند پایش صاعقه، بوران و افت ناگهانی فشار در خط‌الرأس",
                                badge = "پایش هوشمند آنی ⚡",
                                detail = "پیش از لمس طوفان، رادار بحران به کمک الگوریتم‌های اختصاصی ایمنی صعود، پارامترهای جوی را پایش می‌کند. در صورتی که احتمال وقوع رعدوبرق (صاعقه در خط‌الرأس)، افت دید افقی زیر ۵۰ متر، یا دمای محسوس زیر منفی ۲۵ درجه باشد، بلافاصله هشدارهای قرمز صادر می‌کند. این رادار تفاوت بین یک صعود موفق و یک وضعیت بقا در کوهستان را رقم می‌زند.",
                                icon = Icons.Default.Warning,
                                color = Color(0xFFDC2626)
                            ),
                            FeatureItem(
                                title = "شبیه‌ساز هوشمند ترازهای ارتفاعی (Lapse Rate)",
                                subtitle = "محاسبه افت دما، رقت هوا و فشار اتمسفر در ارتفاعات بالای ۴۰۰۰ متر",
                                badge = "آنالیز ارتفاع 📈",
                                detail = "ابزار هوشمند برای بررسی تغییرات فیزیکی جو در ترازهای مختلف ارتفاعی. با افزایش ارتفاع، دما به طور میانگین به ازای هر ۱۰۰۰ متر ۶.۵ درجه کاهش یافته و فشار هوا دچار افت شدیدی می‌شود. این شبیه‌ساز با محاسبه ترازهای مختلف (۲۰۰۰م، ۳۰۰۰م، ۴۰۰۰م) و اعمال ضریب سوزباد، دمای واقعی روی تیغه‌ها و قله را بازسازی می‌کند تا پیش از صعود، تجهیزات مناسب (پَر، گورتکس) را آماده سازید.",
                                icon = Icons.Default.Bolt,
                                color = Color(0xFF8B5CF6)
                            ),
                            FeatureItem(
                                title = "سامانه ۱۰۰٪ آفلاین بقاء در ارتفاعات",
                                subtitle = "ذخیره‌سازی پایدار تمام لایه‌های جوی در حافظه محلی برای نبود آنتن",
                                badge = "بدون نیاز به شبکه 📴",
                                detail = "در اعماق دره‌ها و بر فراز تیغه‌های سرد که آنتن‌دهی موبایل به صفر می‌رسد، «صعود» همچنان فعال است. آخرین داده‌های پیش‌بینی، جزئیات ارتفاعات، و اطلاعات اطلس به صورت فشرده در دیتابیس محلی (Room) دستگاه ذخیره می‌شوند. بدون نیاز به سیگنال، می‌توانید به جدول پیش‌بینی روزهای آینده و اطلاعات حیاتی دسترسی کامل داشته باشید.",
                                icon = Icons.Default.WifiOff,
                                color = Color(0xFF0D9488)
                            ),
                            FeatureItem(
                                title = "اطلس بومی و هوشمند قله‌های ایران",
                                subtitle = "مرجع جامع بیش از صدها قله ملی و مسیرهای صعود البرز و زاگرس",
                                badge = "همگام‌سازی پویا 🗺️",
                                detail = "اطلس یکپارچه، دیتابیسی پویا از قله‌های مرتفع البرز، زاگرس و قلل منفرد کشور است. این دیتابیس شامل نام دقیق، ارتفاع کالیبره شده به متر، موقعیت جغرافیایی و امکان ثبت و مدیریت مسیرهای صعود دلخواه شماست. این اطلس به صورت هوشمند با سرور همگام‌سازی شده و قلل دست‌ساز و دلخواه شما را نیز در کمال امنیت حفظ می‌کند.",
                                icon = Icons.Default.Explore,
                                color = Color(0xFFD97706)
                            ),
                            FeatureItem(
                                title = "طراحی بهینه ضد بازتاب و ضد کولاک",
                                subtitle = "رابط کاربری بهینه، کنتراست شدید رنگی و دکمه‌های بزرگ برای کار با دستکش",
                                badge = "ارگونومی شرایط حدی ❄️",
                                detail = "نور شدید خورشید در برف (برف‌کوری) یا کولاک شدید خواندن تلفن همراه را ناممکن می‌سازد. ما با کالیبره کردن تباین رنگی شدید (کنتراست بالای ۵:۱)، ابعاد بزرگ نشانگرها و استفاده از قلم بهینه‌سازی شده «وزیرمتن» تضمین می‌کنیم که صفحه گوشی با یک نگاه سریع و بدون ابهام خوانده شود. همچنین لبه‌های امن و دکمه‌های بزرگ با بازخورد لرزشی عالی، کار با برنامه را حتی با دستکش‌های ضخیم کوهنوردی امکان‌پذیر می‌سازند.",
                                icon = Icons.Default.Visibility,
                                color = Color(0xFF7C3AED)
                            ),
                            FeatureItem(
                                title = "ثبت و ناوبری قله‌های دلخواه و سفارشی",
                                subtitle = "امکان افزودن آسان تپه‌ها، دره‌ها یا پناهگاه‌های محلی به بانک داده‌ها",
                                badge = "پایگاه داده محلی 💾",
                                detail = "با صعود می‌توانید نقاط شخصی مانند پناهگاه‌ها، چشمه‌ها، قله‌های فرعی، یا هر موقعیت دلخواه دیگر را روی نقشه و دیتابیس ثبت و ویرایش کنید تا همواره ابزار ناوبری جی‌پی‌اس مخصوص به خود را در صعودهای انفرادی داشته باشید.",
                                icon = Icons.Default.Landscape,
                                color = Color(0xFF8B5CF6)
                            ),
                            FeatureItem(
                                title = "مدیریت فوق پیشرفته منابع و مصرف باتری",
                                subtitle = "کاهش بار پردازشی سخت‌افزار برای بقای حداکثری باتری در سرمای شدید",
                                badge = "صرفه‌جویی هوشمند 🔋",
                                detail = "باتری گوشی‌های هوشمند در دمای زیر صفر درجه به شدت و با سرعت تخلیه می‌شوند. اپلیکیشن «صعود» با معماری فوق‌العاده سبک، عدم استفاده از فرآیندهای مداوم پس‌زمینه (Background Workers) غیرضروری، بهینه‌سازی خوانش حافظه و استفاده از انیمیشن‌های سبک سخت‌افزاری، مصرف باتری را به حداقل مطلق رسانده است تا مطمئن باشید گوشی شما تا آخرین گام صعود روشن و پشتیبان شما خواهد ماند.",
                                icon = Icons.Default.Bolt,
                                color = Color(0xFF16A34A)
                            )
                        )
                    }

                    var expandedIndex by remember { mutableStateOf<Int?>(-1) }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        features.forEachIndexed { index, feature ->
                            val isExpanded = expandedIndex == index

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        expandedIndex = if (isExpanded) -1 else index
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) {
                                        feature.color.copy(alpha = if (isDarkTheme) 0.06f else 0.04f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isExpanded) {
                                        feature.color.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(feature.color.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = feature.icon,
                                                contentDescription = feature.title,
                                                tint = feature.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = feature.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isExpanded) feature.color else MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = Vazirmatn
                                                )
                                            )
                                            if (!isExpanded) {
                                                Text(
                                                    text = feature.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontFamily = Vazirmatn,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand details",
                                            tint = if (isExpanded) feature.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            HorizontalDivider(
                                                color = feature.color.copy(alpha = 0.15f),
                                                thickness = 1.dp
                                            )

                                            Text(
                                                text = feature.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = Vazirmatn
                                                ),
                                                lineHeight = 18.sp
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(feature.color.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = feature.badge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = Vazirmatn,
                                                    color = feature.color
                                                )
                                            }

                                            Text(
                                                text = feature.detail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                fontFamily = Vazirmatn,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 4: ABOUT & SUPPORT ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("support_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "درباره و پشتیبانی",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "درباره و پشتیبانی صعود",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "اپلیکیشن دیدبان هواشناسی کوهستان ایران، ابزاری پایدار و کاملاً آفلاین جهت سنجش و تحلیل دقیق شرایط جوی است.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "تذکر ایمنی",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "تذکر مهم ایمنی و سلب مسئولیت",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "این اپلیکیشن ابزار کمکی تصمیم‌گیری است و جایگزین قضاوت حرفه‌ای سرپرست تیم کوهنوردی نمی‌شود. همیشه آخرین گزارش‌های محلی هواشناسی و شرایط میدانی را بررسی کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "استانداردهای مهندسی خوانایی در ارتفاعات:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        listOf(
                            "پشتیبانی آفلاین ۱۰۰ درصد از جبهه‌ها و ذخیره‌سازی محلی داده‌ها",
                            "رعایت نسبت کنتراست شدید (بالای ۵:۱) جهت وضوح زیر نور خورشید بالا",
                            "رابط کاربری جاده‌ای بهینه با لبه‌های امن بزرگ جهت کاربری با دستکش",
                            "قلم یکپارچه Vazirmatn همراه با اعداد فارسی اختصاصی کالیبره شده",
                            "پایبندی کامل به سایز فونت بهینه ملیلة کوهستان (حداقل ۱۲ اس‌پی)",
                            "بدون قطعی و کرش حین تحلیل جبهه‌های هوایی غیر همگن"
                        ).forEach { spec ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = spec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ارتباط با پشتیبانی فنی کوهستان",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "persianboy.1991g@gmail.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "نسخه برنامه",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نسخه مجاز ایمنی: v2.4.0 (نسخه پایدار آفلاین)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}