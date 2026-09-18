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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.BuildConfig
import com.example.ui.theme.Vazirmatn
import com.example.ui.theme.isDark
import com.example.ui.util.PersianDateHelper
import android.content.Intent
import android.net.Uri
import com.example.ui.weather.ActivationUiState
import com.example.ui.weather.SyncUiState
import com.example.ui.weather.WeatherViewModel
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen_root")
    ) {
        // ===== Top bar =====
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
                text = "تنظیمات",
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

            // ==================== 1. GOLDEN ALPINIST MEMBERSHIP ====================
            if (isPremium) {
                if (activationCode.isNotBlank()) {
                    // Active premium state via activation code
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
                                            contentDescription = "اشتراک فعال",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "عضویت فعال",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "کد کوهنوردی شما فعال است",
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
                                text = "پیش‌بینی گسترده‌تر روزها، رادار ارزیابی بحران‌ها و تحلیل چندقله برای شما فعال است.",
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
                                            contentDescription = "عضویت طلایی",
                                            tint = goldColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "عضویت طلایی",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = goldTextColor
                                        )
                                        Text(
                                            text = "اشتراک شما فعال است",
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
                                text = "پیش‌بینی گسترده‌تر روزها، رادار ارزیابی بحران‌ها و تحلیل چندقله برای شما فعال است.",
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
                                    text = "لغو اشتراک",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Non-premium: Cafe Bazaar purchase card
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
                                        contentDescription = "بسته پرو",
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
                                contentDescription = "خرید",
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

            // ==================== 2. ACTIVATION WITH CODE ====================
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
                                        contentDescription = "اشتراک غیرفعال",
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "اشتراک غیرفعال است",
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
                            text = "اشتراک متصل به کد فعال‌سازی شما موقتاً غیرفعال یا منقضی شده است. در صورت فعال‌سازی یا تمدید مجدد می‌توانید وضعیت را بررسی کنید.",
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
                                    text = "حذف کد و بازگشت به نسخه رایگان",
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
                                text = "فعال‌سازی با کد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "اگر کد فعال‌سازی یا لایسنس هدیه از باشگاه‌ها یا مربیان کوهنوردی دریافت کرده‌اید، اینجا وارد کنید.",
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
                                text = "تایید و فعال‌سازی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ==================== 3. APPEARANCE (THEME) ====================
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
                                contentDescription = "پس‌زمینه",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "پس‌زمینه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "تم روشن برای روز و نور مستقیم خورشید، تم تاریک برای شب و گرگ‌ومیش. «خودکار» با تنظیمات دستگاه هماهنگ می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // System (Auto)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
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
                                            modifier = Modifier.size(14.dp).padding(end = 2.dp)
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
                                    text = "خودکار",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "system") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Dark
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
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
                                            modifier = Modifier.size(14.dp).padding(end = 2.dp)
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
                                    text = "تاریک",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "dark") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Light
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
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
                                            modifier = Modifier.size(14.dp).padding(end = 2.dp)
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
                                    text = "روشن",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (themeMode == "light") FontWeight.Bold else FontWeight.Medium,
                                        color = if (themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 4. APP FEATURES ====================
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
                                contentDescription = "قابلیت‌های اپلیکیشن",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "قابلیت‌های اپلیکیشن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "روی هر مورد بزنید تا توضیح کامل را ببینید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = Vazirmatn
                            )
                        }
                    }

                    val features = remember {
                        listOf(
                            FeatureItem(
                                title = "پیش‌بینی تراز به تراز",
                                subtitle = "دما، باد و اکسیژن هر تراز از کمپ تا قله",
                                badge = "مخصوص کوهنوردی 🏔️",
                                detail = "به‌جای یک هواشناسی کلی برای کل قله، وضعیت هوا را تراز به تراز از کمپ اصلی تا کاسه قله می‌بینید: دما، باد تصحیح‌شده با ارتفاع، اکسیژن مؤثر و ریسک یخبندان هر تراز جداگانه محاسبه و رنگ‌بندی ایمنی می‌شود.",
                                icon = Icons.Default.Cloud,
                                color = Color(0xFF0284C7)
                            ),
                            FeatureItem(
                                title = "رادار ریسک صعود",
                                subtitle = "هشدار لحظه‌ای صاعقه، بوران و ریزش فشار",
                                badge = "پایش آنی ⚡",
                                detail = "به‌همراه پیش‌بینی ۱۵ دقیقه‌ای، رادار ۲۴ ساعت آینده را برای صاعقه (CAPE بالا)، بوران و کاهش دید، و دمای محسوس خطرناک اسکن می‌کند و بازه‌های مخاطره‌آمیز را مشخص می‌کند تا زمان خروج از خط‌الرأس را دقیق برنامه‌ریزی کنید.",
                                icon = Icons.Default.Warning,
                                color = Color(0xFFDC2626)
                            ),
                            FeatureItem(
                                title = "خورشید و ماه قله",
                                subtitle = "طلوع و غروب خورشید و ماه برای محل صعود شما",
                                badge = "برنامه‌ریزی زمان ☀️",
                                detail = "زمان دقیق طلوع و غروب خورشید و ماه‌طلوع و ماه‌غرب برای موقعیت و ارتفاع قله محاسبه می‌شود تا پنجره صعود خود را با نور روز و کمترین نور ماه هماهنگ کنید.",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFFD97706)
                            ),
                            FeatureItem(
                                title = "پیش‌بینی گسترده روزها",
                                subtitle = "پیش‌بینی ساعتی و روزانه چندروزه با پنجره طلایی",
                                badge = "نمای بلندمدت 📅",
                                detail = "پیش‌بینی ساعتی با پنجره لغزان ۲۴ ساعته و پیش‌بینی روزانه چندروزه (در نسخه پرو تا ۱۶ روز) همراه با شناسایی خودکار «پنجره‌های طلایی» — بهترین روزهای پیاپی برای صعود.",
                                icon = Icons.Default.CalendarMonth,
                                color = Color(0xFF8B5CF6)
                            ),
                            FeatureItem(
                                title = "اطلس قله‌های ایران و جهان",
                                subtitle = "دیتابیس قله‌ها با بروزرسانی خودکار",
                                badge = "بروزرسانی هوشمند 🗺️",
                                detail = "مرجع کامل قله‌های البرز، زاگرس و قلل بین‌المللی با ارتفاع و مختصات دقیق. اطلس به‌صورت خودکار با سرور همگام می‌شود و قله‌های شخصی شما همیشه حفظ می‌مانند.",
                                icon = Icons.Default.Explore,
                                color = Color(0xFF0D9488)
                            ),
                            FeatureItem(
                                title = "قله‌های شخصی",
                                subtitle = "ثبت و مدیریت نقاط دلخواه شما",
                                badge = "مختص شما 📍",
                                detail = "پناهگاه، چشمه، قله‌های فرعی یا هر نقطه‌ای که می‌خواهید را با نام و ارتفاع دلخواه ثبت کنید تا همیشه در دسترس باشد.",
                                icon = Icons.Default.AddLocationAlt,
                                color = Color(0xFF7C3AED)
                            ),
                            FeatureItem(
                                title = "حالت آفلاین",
                                subtitle = "اطلس و تنظیمات روی دستگاه شما ذخیره می‌شوند",
                                badge = "بدون آنتن 📴",
                                detail = "اطلس قله‌ها، قله‌های شخصی و تنظیمات شما در حافظه دستگاه ذخیره می‌شوند و بدون اینترنت هم در دسترس‌اند. داده‌های زنده پیش‌بینی به اینترنت نیاز دارند؛ آخرین داده دانلودشده تا زمان بروزرسانی بعدی باقی می‌ماند.",
                                icon = Icons.Default.WifiOff,
                                color = Color(0xFF16A34A)
                            ),
                            FeatureItem(
                                title = "نمایش خوانا روز و شب",
                                subtitle = "تم روشن و تاریک با کنتراست بالا برای استفاده در کوه",
                                badge = "قابل اطمینان در نور شدید ❄️",
                                detail = "رنگ‌بندی با کنتراست بالا برای خواندن سریع زیر نور مستقیم خورشید و برف، تم تاریک برای صعود شبانه بدون خیرگی، و دکمه‌های بزرگ برای استفاده با دستکش.",
                                icon = Icons.Default.DarkMode,
                                color = Color(0xFF2563EB)
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
                                            contentDescription = "جزئیات",
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

            // ==================== 5. MOUNTAIN DATABASE SYNC ====================
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
                                contentDescription = "بروزرسانی اطلس",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اطلس قله‌ها",
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
                                    text = "بروزرسانی خودکار فعال",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF30D158),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "هر بار که آب‌وهوا را تازه می‌کنید، اطلس قله‌ها هم به‌روز می‌شود. قله‌های شخصی شما تغییری نمی‌کنند.",
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
                                    text = "آخرین بروزرسانی",
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
                                    .background(if (lastAddedCount > 0) (if (isDarkTheme) Color(0xFF1B3324) else Color(0xFFE8F5E9)) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(
                                        1.dp,
                                        if (lastAddedCount > 0) (if (isDarkTheme) Color(0xFF2E7D46).copy(alpha = 0.5f) else Color(0xFFC8E6C9)) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
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
                                        .background(if (lastAddedCount > 0) (if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF30D158)) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "افزوده: ${PersianDateHelper.formatToPersianDigits(lastAddedCount)} قله",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastAddedCount > 0) (if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF2E7D32)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (lastUpdatedCount > 0) (if (isDarkTheme) Color(0xFF16283A) else Color(0xFFE3F2FD)) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(
                                        1.dp,
                                        if (lastUpdatedCount > 0) (if (isDarkTheme) Color(0xFF1E5A8A).copy(alpha = 0.5f) else Color(0xFFBBDEFB)) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
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
                                        .background(if (lastUpdatedCount > 0) (if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF1976D2)) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "به‌روزرسانی: ${PersianDateHelper.formatToPersianDigits(lastUpdatedCount)} مورد",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastUpdatedCount > 0) (if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1565C0)) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                Text("در حال به‌روزرسانی اطلس...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("بروزرسانی اطلس", fontSize = 11.sp, fontWeight = FontWeight.Black)
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
                                        text = "اطلس با موفقیت به نسخه ${state.version} بروزرسانی شد.",
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
                                        text = "اطلس شما کاملاً به‌روز است (v${state.version})",
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
                                        text = "بروزرسانی ناموفق:",
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

            // ==================== 6. OFFLINE CACHE ====================
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
                                imageVector = Icons.Default.Storage,
                                contentDescription = "حافظه آفلاین",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "داده‌های ذخیره‌شده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "آخرین پیش‌بینی هر قله روی دستگاه ذخیره می‌شود تا در کوهستان، آخرین داده‌های دانلودشده در دسترس باشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعداد قله‌های ذخیره‌شده: ${PersianDateHelper.formatToPersianDigits(cachedCount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "پاکسازی",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 7. ABOUT & SUPPORT ====================
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
                                imageVector = Icons.Default.Info,
                                contentDescription = "درباره",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "درباره",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "دیدبان هواشناسی کوهستان ایران — پیش‌بینی نقطه‌ای آب‌وهوا برای صعود با تحلیل تراز به تراز و ارزیابی ریسک.",
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
                                    text = "تذکر ایمنی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "این اپلیکیشن ابزار کمکی تصمیم‌گیری است و جایگزین قضاوت حرفه‌ای سرپرست تیم نمی‌شود. همیشه آخرین گزارش‌های هواشناسی و شرایط میدانی را هم بررسی کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                text = "پشتیبانی فنی",
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
                        Text(
                            text = "نسخه ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // ==================== 8. OPEN SOURCES & LICENSES ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("licenses_card"),
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
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "مجوزها و منابع",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "مجوزها و منابع باز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "این اپلیکیشن از داده‌ها و اجزای زیر استفاده می‌کند که تحت مجوزهای باز منتشر شده‌اند. ذکر منبع در راستای رعایت آن مجوزها درج شده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Open-Meteo (weather data, CC BY 4.0) ──────────────────
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "داده‌های هواشناسی — Open-Meteo",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "پیش‌بینی‌ها و داده‌های هواشناسی توسط Open-Meteo.com (open-meteo.com) ارائه می‌شود و تحت مجوز CC BY 4.0 منتشر شده است؛ استفاده و توزیع شامل استفاده تجاری با ذکر منبع مجاز است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LicenseLink(
                                label = "open-meteo.com",
                                url = "https://open-meteo.com",
                                color = MaterialTheme.colorScheme.primary
                            )
                            LicenseLink(
                                label = "مجوز CC BY 4.0",
                                url = "https://creativecommons.org/licenses/by/4.0/",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Vazirmatn font (SIL OFL 1.1) ──────────────────────────
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "فونت — Vazirmatn",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "© 2015 The Vazirmatn Project Authors (rastikerdar.com). این فونت تحت مجوز SIL Open Font License 1.1 (OFL) در دسترس و توزیع می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LicenseLink(
                                label = "vazirmatn.com",
                                url = "https://vazirmatn.com",
                                color = MaterialTheme.colorScheme.primary
                            )
                            LicenseLink(
                                label = "مجوز OFL 1.1",
                                url = "https://scripts.sil.org/OFL",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Open-source libraries (Apache 2.0) ────────────────────
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "کتابخانه‌های متن‌باز",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "کتابخانه‌های AndroidX / Jetpack Compose، Kotlin، Retrofit، OkHttp، Moshi، Coil و کتابخانه پرداخت Poolakey تحت مجوز Apache License 2.0 استفاده می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LicenseLink(
                            label = "apache.org/licenses/LICENSE-2.0",
                            url = "https://www.apache.org/licenses/LICENSE-2.0",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} دیدبان هواشناسی کوهستان ایران. کلیه حقوق این اپلیکیشن (کد، طراحی و علائم) برای سازنده محفوظ است؛ داده‌ها و اجزای ذکرشده در بالا به مجوزهای باز خود مشروطند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 17.sp
                    )
                }
            }

            // فاصله انتهای اسکرول: بزرگ‌تر از ارتفاع نوار شناور شیشه‌ای تا آخرین
            // سطر (کپی‌رایت) زیر نوار پنهان نشود و کامل خوانده شود.
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

/** A small tappable pill that opens [url] in the system browser — for license/attribution links. */
@Composable
private fun LicenseLink(
    label: String,
    url: String,
    color: Color
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
