package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.remote.RetrofitHelper
import com.example.data.repository.MountainRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.screens.BillingDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Vazirmatn
import com.example.ui.util.BazaarBillingManager
import com.example.ui.weather.WeatherViewModel
import com.example.ui.weather.WeatherViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val mountainRepository = MountainRepository(database.mountainDao())
        val weatherRepository = WeatherRepository(RetrofitHelper.apiService)

        setContent {
            val viewModel: WeatherViewModel = viewModel(
                factory = WeatherViewModelFactory(application, mountainRepository, weatherRepository)
            )

            val context = LocalContext.current

            LaunchedEffect(Unit) {
                viewModel.initSyncStates(context)
                viewModel.loadCachedSubscriptionPlans(context)
                viewModel.fetchSubscriptionPlans(context)
                viewModel.triggerMountainSync(context)
                
                // Trigger startup server-side subscription check (if activation code exists)
                viewModel.checkSubscriptionOnStartup(context)

                if (BazaarBillingManager.isBazaarInstalled(context)) {
                    BazaarBillingManager.connect(
                        context = context,
                        onConnected = {
                            BazaarBillingManager.queryActiveSubscriptions(
                                context = context,
                                onSuccess = { subscriptions ->
                                    val isUserPremium = subscriptions.any {
                                        it.productId == BazaarBillingManager.PLAN_ANNUAL_ID ||
                                        it.productId == BazaarBillingManager.PLAN_SEASONAL_ID ||
                                        it.productId == BazaarBillingManager.PLAN_MONTHLY_ID
                                    }
                                    if (isUserPremium) {
                                        viewModel.setPremium(context, true)
                                        android.util.Log.d("MainActivity", "Successfully restored active gold subscription from Cafe Bazaar.")
                                    } else {
                                        // Only deactivate premium if no activation code is actually persisted.
                                        // Read from DataStore (not the in-memory StateFlow) to avoid a cold-start
                                        // race that could wipe a valid activation code.
                                        viewModel.deactivatePremiumIfNoActivation()
                                    }
                                    BazaarBillingManager.disconnect()
                                },
                                onFailed = { error ->
                                    android.util.Log.e("MainActivity", "Failed to query active subscriptions on startup: $error")
                                    BazaarBillingManager.disconnect()
                                }
                            )
                        },
                        onFailed = { error ->
                            android.util.Log.e("MainActivity", "Failed to connect to billing service to query subscriptions: $error")
                        },
                        onDisconnected = {}
                    )
                }
            }

            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            LaunchedEffect(isSystemDark) {
                viewModel.updateTheme(context)
            }
            val showBillingDialog by viewModel.showBillingDialog.collectAsStateWithLifecycle()
            var currentTab by remember { mutableStateOf(AppTab.Dashboard) }

            // Liquid-glass shared state: links the content backdrop to the glass bar.
            val glassState = com.example.ui.components.rememberLiquidGlassState()
            val glassTabs = remember {
                listOf(
                    com.example.ui.components.LiquidGlassTab(
                        label = "خانه",
                        icon = Icons.Default.Terrain,
                        contentDescription = "خانه",
                        testTag = "nav_item_dashboard",
                    ),
                    com.example.ui.components.LiquidGlassTab(
                        label = "قله‌ها",
                        icon = Icons.Default.Search,
                        contentDescription = "قله‌ها",
                        testTag = "nav_item_search",
                    ),
                    com.example.ui.components.LiquidGlassTab(
                        label = "تنظیمات",
                        icon = Icons.Default.Settings,
                        contentDescription = "تنظیمات",
                        testTag = "nav_item_settings",
                    ),
                )
            }

            val activity = remember(context) { context as? Activity }
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = true) {
                if (currentTab != AppTab.Dashboard) {
                    currentTab = AppTab.Dashboard
                } else {
                    showExitDialog = true
                }
            }

            var isBottomBarVisible by remember { mutableStateOf(true) }

            LaunchedEffect(currentTab) {
                isBottomBarVisible = true
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        if (delta < -10f) {
                            isBottomBarVisible = false
                        } else if (delta > 10f) {
                            isBottomBarVisible = true
                        }
                        return Offset.Zero
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showBillingDialog) {
                            BillingDialog(
                                viewModel = viewModel,
                                onDismiss = { viewModel.triggerBilling(false) }
                            )
                        }
                        if (showExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitDialog = false },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showExitDialog = false
                                            activity?.finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "خروج",
                                            fontFamily = Vazirmatn,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showExitDialog = false }
                                    ) {
                                        Text(
                                            text = "انصراف",
                                            fontFamily = Vazirmatn,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                title = {
                                    Text(
                                        text = "خروج از برنامه",
                                        fontFamily = Vazirmatn,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                text = {
                                    Text(
                                        text = "آیا مطمئن هستید که می‌خواهید از برنامه خارج شوید؟",
                                        fontFamily = Vazirmatn,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        Scaffold(
                            modifier = Modifier.nestedScroll(nestedScrollConnection),
                            containerColor = Color.Transparent,
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = isBottomBarVisible,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(300)
                                    ) + fadeIn(animationSpec = tween(300)),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(300)
                                    ) + fadeOut(animationSpec = tween(300))
                                ) {
                                    // Floating liquid-glass capsule — never touches the edges.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 24.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        com.example.ui.components.LiquidGlassNavBar(
                                            tabs = glassTabs,
                                            selectedIndex = currentTab.ordinal,
                                            onTabSelected = { index ->
                                                currentTab = AppTab.entries[index]
                                            },
                                            state = glassState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("app_bottom_bar"),
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            // Records the content into a GraphicsLayer so the glass
                            // bar can blur the exact pixels behind itself (Full tier).
                            com.example.ui.components.GlassBackdropSource(
                                state = glassState,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = innerPadding.calculateTopPadding(),
                                            bottom = 0.dp
                                        )
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    when (currentTab) {
                                        AppTab.Dashboard -> {
                                            HomeScreen(
                                                viewModel = viewModel,
                                                onSearchClick = { currentTab = AppTab.Search },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        AppTab.Search -> {
                                            SearchScreen(
                                                viewModel = viewModel,
                                                onMountainSelected = { mountain ->
                                                    viewModel.selectMountain(mountain)
                                                    currentTab = AppTab.Dashboard
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        AppTab.Settings -> {
                                            SettingsScreen(
                                                viewModel = viewModel,
                                                onBackClick = { currentTab = AppTab.Dashboard },
                                                modifier = Modifier.fillMaxSize()
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

enum class AppTab {
    Dashboard, Search, Settings
}