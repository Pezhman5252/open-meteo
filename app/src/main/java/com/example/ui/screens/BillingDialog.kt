package com.example.ui.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.Vazirmatn
import com.example.ui.theme.isDark
import com.example.ui.util.BazaarBillingManager
import com.example.ui.weather.WeatherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SubscriptionPlan {
    Monthly, Seasonal, Annual
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingDialog(
    viewModel: WeatherViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(BillingStep.Offer) }
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.Annual) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasBazaar = remember { BazaarBillingManager.isBazaarInstalled(context) }
    var isBazaarServiceConnected by remember { mutableStateOf(false) }

    val subscriptionPlans by viewModel.subscriptionPlans.collectAsState()
    val isLoadingPlans by viewModel.isLoadingPlans.collectAsState()

    LaunchedEffect(context) {
        viewModel.loadCachedSubscriptionPlans(context)
        viewModel.fetchSubscriptionPlans(context)
    }

    // Get unique user ID from ViewModel
    val userId = remember { viewModel.getUserId(context) }

    DisposableEffect(context) {
        if (hasBazaar) {
            val connection = BazaarBillingManager.connect(
                context = context,
                onConnected = {
                    isBazaarServiceConnected = true
                    Log.d("BillingDialog", "Connected to Cafe Bazaar Billing successfully.")
                },
                onFailed = { error ->
                    isBazaarServiceConnected = false
                    Log.e("BillingDialog", "Cafe Bazaar Billing connection failed: $error")
                },
                onDisconnected = {
                    isBazaarServiceConnected = false
                    Log.w("BillingDialog", "Disconnected from Cafe Bazaar Billing.")
                }
            )
            onDispose {
                BazaarBillingManager.disconnect()
            }
        } else {
            onDispose {}
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val isDark = MaterialTheme.colorScheme.background.isDark
    val dialogBg = if (isDark) Color(0xFF0C101B) else MaterialTheme.colorScheme.surface
    val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)

    ModalBottomSheet(
        onDismissRequest = { if (currentStep == BillingStep.Offer) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    goldColor.copy(alpha = 0.55f),
                                    goldColor.copy(alpha = 0.0f)
                                )
                            )
                        ),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = dialogBg,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Custom Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 5.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.18f))
                        )
                    }

                    // Error Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { errorMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "billing_step_transition"
                    ) { step ->
                        when (step) {
                            BillingStep.Offer -> {
                                OfferScreen(
                                    selectedPlan = selectedPlan,
                                    onPlanSelect = { selectedPlan = it },
                                    hasBazaar = hasBazaar,
                                    onBuyClick = {
                                        errorMessage = null
                                        val registry = (context as? ComponentActivity)?.activityResultRegistry
                                        if (hasBazaar && registry != null) {
                                            val currentPlans = viewModel.subscriptionPlans.value
                                            val productId = when (selectedPlan) {
                                                SubscriptionPlan.Monthly -> currentPlans["monthly"]?.productId ?: BazaarBillingManager.PLAN_MONTHLY_ID
                                                SubscriptionPlan.Seasonal -> currentPlans["seasonal"]?.productId ?: BazaarBillingManager.PLAN_SEASONAL_ID
                                                SubscriptionPlan.Annual -> currentPlans["annual"]?.productId ?: BazaarBillingManager.PLAN_ANNUAL_ID
                                            }
                                            currentStep = BillingStep.ConnectingBazaar
                                            BazaarBillingManager.subscribe(
                                                context = context,
                                                registry = registry,
                                                productId = productId,
                                                userId = userId,
                                                onFlowBegan = {
                                                    Log.d("BillingDialog", "Bazaar subscription flow started successfully.")
                                                },
                                                onFailedToBegin = { error ->
                                                    errorMessage = error
                                                    currentStep = BillingStep.Offer
                                                },
                                                onSucceed = { purchaseInfo ->
                                                    viewModel.viewModelScope.launch {
                                                        currentStep = BillingStep.ConnectingBazaar
                                                        val result = BazaarBillingManager.verifyPurchaseOnServer(context, purchaseInfo)
                                                        when (result) {
                                                            is BazaarBillingManager.ServerValidationResult.Success -> {
                                                                viewModel.setPremium(context, true)
                                                                currentStep = BillingStep.Celebration
                                                            }
                                                            is BazaarBillingManager.ServerValidationResult.Failed -> {
                                                                errorMessage = result.reason
                                                                currentStep = BillingStep.Offer
                                                            }
                                                        }
                                                    }
                                                },
                                                onCanceled = {
                                                    currentStep = BillingStep.Offer
                                                },
                                                onFailed = { error ->
                                                    errorMessage = error
                                                    currentStep = BillingStep.Offer
                                                }
                                            )
                                        } else {
                                            BazaarBillingManager.redirectToInstallBazaar(context)
                                        }
                                    },
                                    subscriptionPlans = subscriptionPlans,
                                    isLoadingPlans = isLoadingPlans
                                )
                            }
                            BillingStep.ConnectingBazaar -> {
                                ConnectingBazaarScreen()
                            }
                            BillingStep.Celebration -> {
                                CelebrationScreen(onDismiss = onDismiss)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class BillingStep {
    Offer, ConnectingBazaar, Celebration
}

@Composable
fun OfferScreen(
    selectedPlan: SubscriptionPlan,
    onPlanSelect: (SubscriptionPlan) -> Unit,
    hasBazaar: Boolean,
    onBuyClick: () -> Unit,
    subscriptionPlans: Map<String, com.example.ui.weather.SubscriptionPlanConfig>,
    isLoadingPlans: Boolean
) {
    val annualPlan = subscriptionPlans["annual"] ?: com.example.ui.weather.SubscriptionPlanConfig(
        productId = "annual_gold_sub",
        title = "اشتراک ۱ ساله ویژه",
        subtitle = "بهترین و اقتصادی‌ترین صعود • ۴,۰۰۰ ت / ماه",
        priceText = "۴۹,۰۰۰ ت",
        priceValue = 49000,
        discountBadge = "۸۰٪ تخفیف",
        badgeType = "red",
        originalPriceText = "۲۲۸,۰۰۰ ت",
        isPopular = true
    )
    val seasonalPlan = subscriptionPlans["seasonal"] ?: com.example.ui.weather.SubscriptionPlanConfig(
        productId = "seasonal_gold_sub",
        title = "اشتراک ۳ ماهه (فصلی)",
        subtitle = "مناسب برنامه‌های فصل • ۱۱,۰۰۰ ت / ماه",
        priceText = "۳۳,۰۰۰ ت",
        priceValue = 33000,
        discountBadge = "محبوب صعود",
        badgeType = "green",
        originalPriceText = "۵۷,۰۰۰ ت",
        isPopular = true
    )
    val monthlyPlan = subscriptionPlans["monthly"] ?: com.example.ui.weather.SubscriptionPlanConfig(
        productId = "monthly_gold_sub",
        title = "اشتراک ۱ ماهه عادی",
        subtitle = "تمدید ماهانه صعود طلایی",
        priceText = "۱۹,۰۰۰ ت",
        priceValue = 19000,
        discountBadge = null,
        badgeType = null,
        originalPriceText = null,
        isPopular = false
    )

    val isDark = MaterialTheme.colorScheme.background.isDark
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textMutedColor = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val textUltraMutedColor = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val textHalfColor = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)

    val cardBgSelected = if (isDark) Color(0xFFFFD700).copy(alpha = 0.06f) else Color(0xFF9A6A00).copy(alpha = 0.08f)
    val cardBgNormal = if (isDark) Color.White.copy(alpha = 0.02f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    val cardBorderSelected = goldColor
    val cardBorderNormal = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val radioInnerBg = if (isDark) Color(0xFF0C101B) else MaterialTheme.colorScheme.surface
    val radioBorderNormal = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoadingPlans && subscriptionPlans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = goldColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "در حال دریافت اطلاعات اشتراک...",
                            fontSize = 11.sp,
                            color = textMutedColor,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "صعود طلایی (اشتراک ویژه هواشناسی)",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = textColor,
                        fontFamily = Vazirmatn
                    )
                }

                Text(
                    text = "قفل پتانسیل کامل ناوبری و ایمنی کوهستان خود را باز کنید",
                    fontSize = 10.5.sp,
                    color = textMutedColor,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    fontFamily = Vazirmatn,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // Features
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.03f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    PremiumFeatureRow(
                        icon = Icons.Default.FilterHdr,
                        title = "پنجره طلایی صعود ایمن (Golden Window)",
                        desc = "آنالیز هوشمند و دقیق همزمان پارامترهای باد، دما، صاعقه، دید و بارش جهت یافتن امن‌ترین ساعات صعود"
                    )
                    PremiumFeatureRow(
                        icon = Icons.Default.DateRange,
                        title = "پیش‌بینی جامع ۷ روزه و ۱۶ روزه قله‌ها",
                        desc = "دسترسی کامل به زمان‌بندی صعودهای زمستانه و برنامه‌های چندروزه خط‌الراس‌های البرز و زاگرس"
                    )
                    PremiumFeatureRow(
                        icon = Icons.Default.Shield,
                        title = "رادار ریسک‌های حاد صعود",
                        desc = "پایش زنده پتانسیل صاعقه، خطر بهمن، هیپوترمی حاد، زمان یخ‌زدگی پوست و شاخص دید طوفان (Whiteout)"
                    )
                    PremiumFeatureRow(
                        icon = Icons.Default.Landscape,
                        title = "ترازهای ارتفاعی چندگانه باد و دما",
                        desc = "سنجش سرعت باد، دما، رطوبت و ارتفاع خط انجماد در سه تراز پویا (پای‌کار، کمپ میانی و قله اصلی)"
                    )
                    PremiumFeatureRow(
                        icon = Icons.Default.AddLocation,
                        title = "ثبت نامحدود قله‌های سفارشی با GPS",
                        desc = "افزودن نامحدود قله‌ها، دره‌ها، دیواره‌ها یا جان‌پناه‌های دلخواه با ثبت دقیق مختصات جغرافیایی"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Plans
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Annual Plan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlanSelect(SubscriptionPlan.Annual) }
                            .border(
                                BorderStroke(
                                    width = if (selectedPlan == SubscriptionPlan.Annual) 1.5.dp else 1.dp,
                                    color = if (selectedPlan == SubscriptionPlan.Annual) cardBorderSelected else cardBorderNormal
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlan == SubscriptionPlan.Annual) cardBgSelected else cardBgNormal
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedPlan == SubscriptionPlan.Annual) goldColor else Color.Transparent)
                                        .border(1.5.dp, if (selectedPlan == SubscriptionPlan.Annual) goldColor else radioBorderNormal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedPlan == SubscriptionPlan.Annual) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(radioInnerBg)
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = annualPlan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = textColor,
                                            fontFamily = Vazirmatn
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFD97706))
                                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                    Text(
                                                        text = "محبوب‌ترین",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontFamily = Vazirmatn
                                                    )
                                                }
                                            }
                                            if (!annualPlan.discountBadge.isNullOrBlank()) {
                                                val badgeBgColor = parseBadgeColor(annualPlan.badgeType, Color(0xFFE11D48))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeBgColor)
                                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = annualPlan.discountBadge!!,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontFamily = Vazirmatn
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = annualPlan.subtitle,
                                        fontSize = 9.5.sp,
                                        color = textHalfColor,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!annualPlan.originalPriceText.isNullOrBlank()) {
                                    Text(
                                        text = annualPlan.originalPriceText!!,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.5.sp,
                                        color = textUltraMutedColor,
                                        textDecoration = TextDecoration.LineThrough,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                Text(
                                    text = annualPlan.priceText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp,
                                    color = if (selectedPlan == SubscriptionPlan.Annual) goldColor else textColor,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }

                    // Seasonal Plan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlanSelect(SubscriptionPlan.Seasonal) }
                            .border(
                                BorderStroke(
                                    width = if (selectedPlan == SubscriptionPlan.Seasonal) 1.5.dp else 1.dp,
                                    color = if (selectedPlan == SubscriptionPlan.Seasonal) cardBorderSelected else cardBorderNormal
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlan == SubscriptionPlan.Seasonal) cardBgSelected else cardBgNormal
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedPlan == SubscriptionPlan.Seasonal) goldColor else Color.Transparent)
                                        .border(1.5.dp, if (selectedPlan == SubscriptionPlan.Seasonal) goldColor else radioBorderNormal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedPlan == SubscriptionPlan.Seasonal) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(radioInnerBg)
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = seasonalPlan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = textColor,
                                            fontFamily = Vazirmatn
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!seasonalPlan.discountBadge.isNullOrBlank()) {
                                                val badgeBgColor = parseBadgeColor(seasonalPlan.badgeType, Color(0xFF10B981))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeBgColor)
                                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = seasonalPlan.discountBadge!!,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontFamily = Vazirmatn
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = seasonalPlan.subtitle,
                                        fontSize = 9.5.sp,
                                        color = textHalfColor,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!seasonalPlan.originalPriceText.isNullOrBlank()) {
                                    Text(
                                        text = seasonalPlan.originalPriceText!!,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.5.sp,
                                        color = textUltraMutedColor,
                                        textDecoration = TextDecoration.LineThrough,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                Text(
                                    text = seasonalPlan.priceText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp,
                                    color = if (selectedPlan == SubscriptionPlan.Seasonal) goldColor else textColor,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }

                    // Monthly Plan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlanSelect(SubscriptionPlan.Monthly) }
                            .border(
                                BorderStroke(
                                    width = if (selectedPlan == SubscriptionPlan.Monthly) 1.5.dp else 1.dp,
                                    color = if (selectedPlan == SubscriptionPlan.Monthly) cardBorderSelected else cardBorderNormal
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlan == SubscriptionPlan.Monthly) cardBgSelected else cardBgNormal
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedPlan == SubscriptionPlan.Monthly) goldColor else Color.Transparent)
                                        .border(1.5.dp, if (selectedPlan == SubscriptionPlan.Monthly) goldColor else radioBorderNormal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedPlan == SubscriptionPlan.Monthly) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(radioInnerBg)
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = monthlyPlan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = textColor,
                                            fontFamily = Vazirmatn
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!monthlyPlan.discountBadge.isNullOrBlank()) {
                                                val badgeBgColor = parseBadgeColor(monthlyPlan.badgeType, Color(0xFFE11D48))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeBgColor)
                                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = monthlyPlan.discountBadge!!,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontFamily = Vazirmatn
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = monthlyPlan.subtitle,
                                        fontSize = 9.5.sp,
                                        color = textHalfColor,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!monthlyPlan.originalPriceText.isNullOrBlank()) {
                                    Text(
                                        text = monthlyPlan.originalPriceText!!,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.5.sp,
                                        color = textUltraMutedColor,
                                        textDecoration = TextDecoration.LineThrough,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                Text(
                                    text = monthlyPlan.priceText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp,
                                    color = if (selectedPlan == SubscriptionPlan.Monthly) goldColor else textColor,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buy Button
        Button(
            onClick = onBuyClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("buy_gold_subscription_bazaar"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasBazaar) goldColor else Color(0xFF179E4C),
                contentColor = if (isDark) Color(0xFF0C101B) else Color.White
            )
        ) {
            Icon(
                imageVector = if (hasBazaar) Icons.Default.ShoppingCart else Icons.Default.CheckCircle,
                contentDescription = if (hasBazaar) "Buy" else "Install",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasBazaar) "خرید اشتراک طلایی از کافه‌بازار" else "نصب برنامه کافه‌بازار جهت خرید",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Vazirmatn
            )
        }
    }
}

private fun parseBadgeColor(badgeType: String?, defaultColor: Color): Color {
    if (badgeType == null) return defaultColor
    val trimmed = badgeType.trim()
    if (trimmed.startsWith("#")) {
        return try {
            Color(android.graphics.Color.parseColor(trimmed))
        } catch (e: Exception) {
            defaultColor
        }
    }
    return when (trimmed.lowercase()) {
        "red" -> Color(0xFFE11D48)
        "green" -> Color(0xFF10B981)
        "blue" -> Color(0xFF2563EB)
        "orange" -> Color(0xFFEA580C)
        "yellow" -> Color(0xFFD97706)
        "purple" -> Color(0xFF9333EA)
        else -> defaultColor
    }
}

@Composable
fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val goldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textMutedColor = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(goldColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = Vazirmatn
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = textMutedColor,
                lineHeight = 14.sp,
                fontFamily = Vazirmatn,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ConnectingBazaarScreen(
    message: String = "در حال اتصال به درگاه پرداخت بازار..."
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textUltraMutedColor = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF179E4C).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF179E4C),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            fontFamily = Vazirmatn
        )

        Text(
            text = "لطفاً برنامه بازار را نبندید. سیستم در حال بررسی خرید است.",
            fontSize = 10.sp,
            color = textUltraMutedColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            fontFamily = Vazirmatn,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun CelebrationScreen(
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.isDark
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textMutedColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    val successColor = if (isDark) Color(0xFF00FFE0) else Color(0xFF00897B)
    val btnContentColor = if (isDark) Color(0xFF0C101B) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(successColor.copy(alpha = 0.12f))
                .border(2.dp, successColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = successColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "تبریک! صعود طلایی شما فعال شد!",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            fontFamily = Vazirmatn
        )

        Text(
            text = "هم‌اکنون تمامی ویژگی‌های تخصصی پیش‌بینی چند ارتفاعی، رادار ریسک صعود و رصد ۷ روزه قله‌ها برای شما باز شده است. با خیالی آسوده صعود کنید.",
            fontSize = 11.sp,
            color = textMutedColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontFamily = Vazirmatn,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("celebration_dismiss"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = successColor,
                contentColor = btnContentColor
            )
        ) {
            Text(
                text = "ورود به دنیای صعود حرفه‌ای",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = Vazirmatn
            )
        }
    }
}