package com.example.ui.weather

import okhttp3.RequestBody.Companion.toRequestBody

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MountainEntity
import com.example.data.remote.OpenMeteoApiException
import com.example.data.remote.WeatherResponse
import com.example.data.repository.MountainRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.util.PersianDateHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class SubscriptionPlanConfig(
    val productId: String,
    val title: String,
    val subtitle: String,
    val priceText: String,
    val priceValue: Int,
    val discountBadge: String? = null,
    val badgeType: String? = null,
    val originalPriceText: String? = null,
    val isPopular: Boolean? = null
)

data class PromoConfig(
    val title: String,
    val subtitle: String,
    val discountBadge: String? = null,
    val description: String,
    val buttonText: String
)

data class BillingConfigResponse(
    val plans: Map<String, SubscriptionPlanConfig>,
    val promo: PromoConfig? = null
)

class WeatherViewModel(
    application: android.app.Application,
    private val mountainRepository: MountainRepository,
    private val weatherRepository: WeatherRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    val defaultPromoConfig = PromoConfig(
        title = "ارتقاء به سطح طلایی صعود",
        subtitle = "دسترسی محدود (نسخه رایگان)",
        discountBadge = "۵۰٪ تخفیف",
        description = "با خرید اشتراک طلایی صعود، امکانات رادار ریسک حاد قله (بهمن، رعد و برق و سرمازدگی)، ترازهای ارتفاعی پیشرفته و پیش‌بینی ۷ روزه را باز کنید.",
        buttonText = "ارتقاء به اشتراک طلایی از کافه‌بازار"
    )

    private val defaultPlans = mapOf(
        "monthly" to SubscriptionPlanConfig(
            productId = "monthly_gold_sub",
            title = "اشتراک ۱ ماهه عادی",
            subtitle = "تمدید ماهانه صعود طلایی",
            priceText = "۱۹,۰۰۰ ت",
            priceValue = 19000,
            discountBadge = null,
            badgeType = null,
            originalPriceText = null,
            isPopular = false
        ),
        "seasonal" to SubscriptionPlanConfig(
            productId = "seasonal_gold_sub",
            title = "اشتراک ۳ ماهه (فصلی)",
            subtitle = "مناسب برنامه‌های فصل • ۱۱,۰۰۰ ت / ماه",
            priceText = "۳۳,۰۰۰ ت",
            priceValue = 33000,
            discountBadge = "محبوب صعود",
            badgeType = "green",
            originalPriceText = "۵۷,۰۰۰ ت",
            isPopular = true
        ),
        "annual" to SubscriptionPlanConfig(
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
    )

    private val _subscriptionPlans = MutableStateFlow<Map<String, SubscriptionPlanConfig>>(defaultPlans)
    val subscriptionPlans = _subscriptionPlans.asStateFlow()

    private val _promoConfig = MutableStateFlow<PromoConfig>(defaultPromoConfig)
    val promoConfig = _promoConfig.asStateFlow()

    private val _isLoadingPlans = MutableStateFlow(false)
    val isLoadingPlans = _isLoadingPlans.asStateFlow()

    private val settingsDataStore = com.example.data.local.SettingsDataStore(application)

    fun getUserId(context: android.content.Context): String {
        val prefs = getPrefs(context)
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", userId).apply()
            Log.d("WeatherViewModel", "Generated new user ID: $userId")
        }
        return userId
    }

    fun loadCachedSubscriptionPlans(context: android.content.Context) {
        val cachedJson = getPrefs(context).getString("cached_subscription_plans", null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                try {
                    val responseAdapter = moshi.adapter(BillingConfigResponse::class.java)
                    val response = responseAdapter.fromJson(cachedJson)
                    if (response != null && response.plans.isNotEmpty()) {
                        _subscriptionPlans.value = response.plans
                        response.promo?.let { _promoConfig.value = it }
                        Log.d("WeatherViewModel", "Loaded cached unified subscription config successfully.")
                        return
                    }
                } catch (e: Exception) {
                    Log.d("WeatherViewModel", "Cache is not in unified format, trying legacy format: ${e.message}")
                }

                val legacyType = com.squareup.moshi.Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    SubscriptionPlanConfig::class.java
                )
                val legacyAdapter = moshi.adapter<Map<String, SubscriptionPlanConfig>>(legacyType)
                val plansMap = legacyAdapter.fromJson(cachedJson)
                if (plansMap != null) {
                    _subscriptionPlans.value = plansMap
                    Log.d("WeatherViewModel", "Loaded legacy cached subscription plans successfully.")
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to parse cached subscription plans: ${e.message}")
            }
        } else {
            try {
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val responseAdapter = moshi.adapter(BillingConfigResponse::class.java)
                val initialConfig = BillingConfigResponse(plans = defaultPlans, promo = defaultPromoConfig)
                val jsonString = responseAdapter.toJson(initialConfig)
                getPrefs(context).edit().putString("cached_subscription_plans", jsonString).apply()
                _subscriptionPlans.value = defaultPlans
                _promoConfig.value = defaultPromoConfig
                Log.d("WeatherViewModel", "Pre-populated SharedPreferences with default subscription plans for offline usage.")
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to pre-populate default subscription plans cache: ${e.message}")
            }
        }
    }

    fun fetchSubscriptionPlans(context: android.content.Context) {
        _isLoadingPlans.value = true
        viewModelScope.launch {
            try {
                val billingUrl = getBillingUrl(context)
                val basePlansUrl = if (billingUrl.endsWith("/")) "${billingUrl}api/billing/plans" else "$billingUrl/api/billing/plans"
                val plansUrl = "$basePlansUrl?t=${System.currentTimeMillis()}"
                Log.d("WeatherViewModel", "Fetching subscription plans dynamically from Cloudflare Worker: $plansUrl")

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(plansUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "IranMountainWeather-Android")
                    .build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                val moshi = Moshi.Builder()
                                    .add(KotlinJsonAdapterFactory())
                                    .build()

                                var parsedSuccessfully = false
                                try {
                                    val responseAdapter = moshi.adapter(BillingConfigResponse::class.java)
                                    val configRes = responseAdapter.fromJson(body)
                                    if (configRes != null && configRes.plans.isNotEmpty()) {
                                        _subscriptionPlans.value = configRes.plans
                                        configRes.promo?.let { _promoConfig.value = it }
                                        getPrefs(context).edit().putString("cached_subscription_plans", body).apply()
                                        Log.d("WeatherViewModel", "Successfully fetched and parsed unified remote config.")
                                        parsedSuccessfully = true
                                    }
                                } catch (e: Exception) {
                                    Log.d("WeatherViewModel", "Remote response not in unified format, trying legacy format: ${e.message}")
                                }

                                if (!parsedSuccessfully) {
                                    val legacyType = com.squareup.moshi.Types.newParameterizedType(
                                        Map::class.java,
                                        String::class.java,
                                        SubscriptionPlanConfig::class.java
                                    )
                                    val legacyAdapter = moshi.adapter<Map<String, SubscriptionPlanConfig>>(legacyType)
                                    val plansMap = legacyAdapter.fromJson(body)
                                    if (plansMap != null) {
                                        _subscriptionPlans.value = plansMap
                                        getPrefs(context).edit().putString("cached_subscription_plans", body).apply()
                                        Log.d("WeatherViewModel", "Successfully fetched and parsed legacy remote plans.")
                                    }
                                }
                            }
                        } else {
                            Log.e("WeatherViewModel", "Failed to fetch plans, HTTP Code: ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception while fetching remote plans: ${e.message}")
            } finally {
                _isLoadingPlans.value = false
            }
        }
    }

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode = _themeMode.asStateFlow()

    private fun isSystemInDarkTheme(context: android.content.Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun updateTheme(context: android.content.Context) {
        val mode = _themeMode.value
        _isDarkTheme.value = when (mode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme(context)
        }
    }

    fun setThemeMode(context: android.content.Context, mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
        updateTheme(context)
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    private val _activationCode = MutableStateFlow("")
    val activationCode = _activationCode.asStateFlow()

    private val _subscriptionId = MutableStateFlow("")
    val subscriptionId = _subscriptionId.asStateFlow()

    private val _subscriptionExpiresAt = MutableStateFlow("")
    val subscriptionExpiresAt = _subscriptionExpiresAt.asStateFlow()

    private val _activationUiState = MutableStateFlow<ActivationUiState>(ActivationUiState.Idle)
    val activationUiState = _activationUiState.asStateFlow()

    fun resetActivationUiState() {
        _activationUiState.value = ActivationUiState.Idle
    }

    private val _showBillingDialog = MutableStateFlow(false)
    val showBillingDialog = _showBillingDialog.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun setDarkTheme(context: android.content.Context, enabled: Boolean) {
        val mode = if (enabled) "dark" else "light"
        setThemeMode(context, mode)
    }

    fun setPremium(context: android.content.Context, enabled: Boolean) {
        _isPremium.value = enabled
        viewModelScope.launch {
            if (!enabled) {
                settingsDataStore.clearActivationDetails()
            } else {
                settingsDataStore.setPremium(enabled)
            }
        }
    }

    /**
     * Deactivates premium ONLY when no activation code is persisted.
     *
     * Unlike reading [activationCode] (an in-memory StateFlow that is populated
     * asynchronously from DataStore on cold start), this reads the authoritative
     * persisted value. This prevents a startup race (e.g. a Cafe Bazaar restore
     * callback running before the DataStore collector emits) from wiping a valid
     * user activation code via [clearActivationDetails].
     */
    fun deactivatePremiumIfNoActivation() {
        viewModelScope.launch {
            val hasPersistedActivation = settingsDataStore.activationCode.first().isNotBlank()
            if (!hasPersistedActivation) {
                setPremium(getApplication(), false)
            }
        }
    }

    fun triggerBilling(show: Boolean) {
        _showBillingDialog.value = show
    }

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState = _syncUiState.asStateFlow()

    private val _cachedMountainIds = MutableStateFlow<Set<Int>>(emptySet())
    val cachedMountainIds = _cachedMountainIds.asStateFlow()

    private val _offlineErrorEvent = MutableStateFlow<MountainEntity?>(null)
    val offlineErrorEvent = _offlineErrorEvent.asStateFlow()

    fun clearOfflineErrorEvent() {
        _offlineErrorEvent.value = null
    }

    fun isNetworkAvailable(context: android.content.Context): Boolean {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getCacheFile(context: android.content.Context, mountainId: Int): File {
        return File(context.filesDir, "cached_weather_$mountainId.json")
    }

    fun hasCachedWeather(mountainId: Int): Boolean {
        val ctx = getApplication<android.app.Application>()
        val file = getCacheFile(ctx, mountainId)
        if (file.exists() && file.length() > 0) return true
        val prefs = getPrefs(ctx)
        val json = prefs.getString("cached_weather_json_$mountainId", null)
        return !json.isNullOrBlank()
    }

    fun updateCachedMountainIds() {
        val ctx = getApplication<android.app.Application>()
        val prefs = getPrefs(ctx)
        val cached = allMountains.value.map { it.id }.filter { id ->
            val file = getCacheFile(ctx, id)
            (file.exists() && file.length() > 0) || !prefs.getString("cached_weather_json_$id", null).isNullOrBlank()
        }.toSet()
        _cachedMountainIds.value = cached
    }

    private val _dbVersion = MutableStateFlow(1)
    val dbVersion = _dbVersion.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("هنوز بروزرسانی انجام نشده")
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _lastSyncAdded = MutableStateFlow(0)
    val lastSyncAdded = _lastSyncAdded.asStateFlow()

    private val _lastSyncUpdated = MutableStateFlow(0)
    val lastSyncUpdated = _lastSyncUpdated.asStateFlow()

    companion object {
        const val PRODUCTION_WORKER_URL = "https://mountain-weather-api.persianboy-1991g.workers.dev/"
        const val BILLING_WORKER_URL = "https://ir-mountain-weather-billing.persianboy-1991g.workers.dev/"
    }

    private fun getPrefs(context: android.content.Context) =
        context.getSharedPreferences("mountain_sync_prefs", android.content.Context.MODE_PRIVATE)

    fun getSyncUrl(context: android.content.Context): String {
        return getPrefs(context).getString("sync_url", PRODUCTION_WORKER_URL) ?: PRODUCTION_WORKER_URL
    }

    fun setSyncUrl(context: android.content.Context, url: String) {
        getPrefs(context).edit().putString("sync_url", url).apply()
    }

    fun getBillingUrl(context: android.content.Context): String {
        return getPrefs(context).getString("billing_url", BILLING_WORKER_URL) ?: BILLING_WORKER_URL
    }

    fun setBillingUrl(context: android.content.Context, url: String) {
        getPrefs(context).edit().putString("billing_url", url).apply()
    }

    fun initSyncStates(context: android.content.Context) {
        val prefs = getPrefs(context)
        _dbVersion.value = prefs.getInt("db_version", 1)
        _lastSyncTime.value = prefs.getString("last_sync_time", "هنوز بروزرسانی انجام نشده") ?: "هنوز بروزرسانی انجام نشده"
        _lastSyncAdded.value = prefs.getInt("last_sync_added", 0)
        _lastSyncUpdated.value = prefs.getInt("last_sync_updated", 0)
        
        updateCachedMountainIds()

        val savedId = prefs.getInt("last_selected_mountain_id", -1)
        if (savedId != -1) {
            viewModelScope.launch {
                val current = _selectedMountain.value
                if (current == null || current.id != savedId) {
                    val mountain = mountainRepository.getMountainById(savedId)
                    if (mountain != null) {
                        selectMountain(mountain)
                    }
                }
            }
        }
    }

    fun getDbVersion(context: android.content.Context): Int {
        return getPrefs(context).getInt("db_version", 1)
    }

    fun getLastSyncTime(context: android.content.Context): String {
        return getPrefs(context).getString("last_sync_time", "هنوز بروزرسانی انجام نشده") ?: "هنوز بروزرسانی انجام نشده"
    }

    fun getLastSyncAdded(context: android.content.Context): Int {
        return getPrefs(context).getInt("last_sync_added", 0)
    }

    fun getLastSyncUpdated(context: android.content.Context): Int {
        return getPrefs(context).getInt("last_sync_updated", 0)
    }

    fun triggerMountainSync(context: android.content.Context, onComplete: (() -> Unit)? = null) {
        _syncUiState.value = SyncUiState.Loading
        viewModelScope.launch {
            val url = getSyncUrl(context)
            val version = getDbVersion(context)

            val result = mountainRepository.syncWithRemote(url, version)

            when (result) {
                is com.example.data.repository.SyncResult.Success -> {
                    val now = java.util.Date()
                    val jalaliStr = com.example.ui.util.PersianDateHelper.getJalaliDateString(now)
                    val fullPersianTime = jalaliStr

                    getPrefs(context).edit().apply {
                        putInt("db_version", result.version)
                        putString("last_sync_time", fullPersianTime)
                        putInt("last_sync_added", result.addedCount)
                        putInt("last_sync_updated", result.updatedCount)
                        apply()
                    }

                    _dbVersion.value = result.version
                    _lastSyncTime.value = fullPersianTime
                    _lastSyncAdded.value = result.addedCount
                    _lastSyncUpdated.value = result.updatedCount

                    _syncUiState.value = SyncUiState.Success(result.version, result.addedCount, result.updatedCount)
                    onComplete?.invoke()

                    val currentSelected = _selectedMountain.value
                    if (currentSelected != null) {
                        allMountains.value.find { it.name == currentSelected.name }?.let { updated ->
                            _selectedMountain.value = updated
                        }
                    }
                }
                is com.example.data.repository.SyncResult.NoUpdate -> {
                    val now = java.util.Date()
                    val jalaliStr = com.example.ui.util.PersianDateHelper.getJalaliDateString(now)
                    val fullPersianTime = jalaliStr

                    getPrefs(context).edit().putString("last_sync_time", fullPersianTime).apply()
                    _lastSyncTime.value = fullPersianTime

                    _syncUiState.value = SyncUiState.NoUpdate(result.version)
                    onComplete?.invoke()
                }
                is com.example.data.repository.SyncResult.Error -> {
                    val rawMsg = result.message
                    val friendlyMsg = when {
                        rawMsg.contains("UnknownHostException", ignoreCase = true) ||
                        rawMsg.contains("No address associated with hostname", ignoreCase = true) ||
                        rawMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        rawMsg.contains("connect", ignoreCase = true) -> {
                            "اتصال به اینترنت برقرار نیست. لطفا شبکه خود را متصل کنید."
                        }
                        rawMsg.contains("timeout", ignoreCase = true) -> {
                            "زمان انتظار برای اتصال به سرور به پایان رسید."
                        }
                        rawMsg.contains("workers.dev", ignoreCase = true) || rawMsg.contains("50") -> {
                            "اختلال موقت در سرور ابری همگام‌سازی."
                        }
                        else -> {
                            val clean = rawMsg.replace(Regex("https?://[^\\s]+"), "").trim()
                            if (clean.isBlank()) "خطای شبکه یا دیتابیس محلی" else clean
                        }
                    }
                    val errType = when {
                        rawMsg.contains("UnknownHost", ignoreCase = true) || rawMsg.contains("No address associated", ignoreCase = true) -> WeatherErrorType.NO_INTERNET
                        rawMsg.contains("timeout", ignoreCase = true) -> WeatherErrorType.TIMEOUT
                        else -> WeatherErrorType.SERVER_ERROR
                    }
                    _syncUiState.value = SyncUiState.Error(friendlyMsg, errType)
                    onComplete?.invoke()
                }
            }
        }
    }

    fun resetSyncUiState() {
        _syncUiState.value = SyncUiState.Idle
    }

    private val _selectedMountain = MutableStateFlow<MountainEntity?>(null)
    val selectedMountain = _selectedMountain.asStateFlow()

    private val _selectedAltitude = MutableStateFlow<Int?>(null)
    val selectedAltitude = _selectedAltitude.asStateFlow()

    private val _selectedDaysCount = MutableStateFlow(3)
    val selectedDaysCount = _selectedDaysCount.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow<String>("")
    val lastUpdatedTime = _lastUpdatedTime.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState = _weatherUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedProvince = MutableStateFlow<String?>(null)
    val selectedProvince = _selectedProvince.asStateFlow()

    private val _altitudeRange = MutableStateFlow<Pair<Int, Int>>(0 to 10000)
    val altitudeRange = _altitudeRange.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>("iran_peak")
    val selectedType = _selectedType.asStateFlow()

    val searchResults: StateFlow<List<MountainEntity>> = combine(
        _searchQuery,
        _selectedProvince,
        _altitudeRange,
        _selectedType,
        mountainRepository.allMountains
    ) { query, province, range, type, allList ->
        allList.filter { mountain ->
            val cleanQuery = query.trim()
            val normQuery = com.example.ui.util.PersianDateHelper.normalizePersianDigits(cleanQuery)

            val matchesQuery = cleanQuery.isEmpty() ||
                    mountain.persianName.contains(cleanQuery, ignoreCase = true) ||
                    mountain.name.contains(cleanQuery, ignoreCase = true) ||
                    mountain.persianProvince.contains(cleanQuery, ignoreCase = true) ||
                    mountain.province.contains(cleanQuery, ignoreCase = true) ||
                    mountain.range.contains(cleanQuery, ignoreCase = true) ||
                    (normQuery.isNotEmpty() && mountain.altitude.toString().contains(normQuery))

            val matchesProvince = province == null || mountain.persianProvince == province
            val matchesAltitude = mountain.altitude in range.first..range.second
            val matchesType = type == null || mountain.type == type

            matchesQuery && matchesProvince && matchesAltitude && matchesType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pinnedMountains: StateFlow<List<MountainEntity>> = mountainRepository.pinnedMountains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMountains: StateFlow<List<MountainEntity>> = mountainRepository.allMountains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Migrate old SharedPreferences values if they exist, then clear them
        val prefs = getPrefs(application)
        if (prefs.contains("is_premium") || prefs.contains("theme_mode")) {
            val isPrem = prefs.getBoolean("is_premium", false)
            val themeM = prefs.getString("theme_mode", "system") ?: "system"
            viewModelScope.launch {
                settingsDataStore.setPremium(isPrem)
                settingsDataStore.setThemeMode(themeM)
                // Clear migrated values so we don't migrate again
                prefs.edit().remove("is_premium").remove("theme_mode").apply()
            }
        }

        viewModelScope.launch {
            settingsDataStore.isPremium.collect { premium ->
                _isPremium.value = premium
            }
        }
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode ->
                _themeMode.value = mode
                updateTheme(getApplication())
            }
        }
        viewModelScope.launch {
            settingsDataStore.activationCode.collect { code ->
                _activationCode.value = code
            }
        }
        viewModelScope.launch {
            settingsDataStore.subscriptionId.collect { subId ->
                _subscriptionId.value = subId
            }
        }
        viewModelScope.launch {
            settingsDataStore.subscriptionExpiresAt.collect { expiresAt ->
                _subscriptionExpiresAt.value = expiresAt
            }
        }

        viewModelScope.launch {
            mountainRepository.ensureSeeded()

            allMountains.collectLatest { list ->
                if (list.isNotEmpty()) {
                    updateCachedMountainIds()
                    val currentSelected = _selectedMountain.value
                    if (currentSelected == null) {
                        val savedId = getPrefs(getApplication()).getInt("last_selected_mountain_id", -1)
                        val initial = if (savedId != -1) {
                            list.find { it.id == savedId } ?: list.find { it.isPinned } ?: list.first()
                        } else {
                            list.find { it.isPinned } ?: list.first()
                        }
                        selectMountain(initial)
                    } else {
                        val updated = list.find { it.id == currentSelected.id }
                        if (updated != null) {
                            if (_selectedMountain.value != updated) {
                                _selectedMountain.value = updated
                            }
                            val currentState = _weatherUiState.value
                            if (currentState is WeatherUiState.Success && currentState.mountain.id == updated.id) {
                                if (currentState.mountain != updated) {
                                    _weatherUiState.value = currentState.copy(mountain = updated)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectMountain(mountain: MountainEntity) {
        val context = getApplication<android.app.Application>()
        _selectedMountain.value = mountain
        _selectedAltitude.value = mountain.altitude
        getPrefs(context).edit().putInt("last_selected_mountain_id", mountain.id).apply()

        if (!isNetworkAvailable(context) && !hasCachedWeather(mountain.id)) {
            _offlineErrorEvent.value = mountain
            _weatherUiState.value = WeatherUiState.Error(
                message = "اتصال به اینترنت برقرار نیست و پیش‌بینی آفلاین این قله ذخیره نشده است.",
                errorType = WeatherErrorType.NO_INTERNET
            )
            return
        }

        fetchWeather(mountain)
    }

    fun setSelectedAltitude(altitude: Int) {
        _selectedAltitude.value = altitude
    }

    fun setSelectedDaysCount(days: Int) {
        _selectedDaysCount.value = days
    }

    fun refreshCurrentMountainWeather(onResult: ((Boolean) -> Unit)? = null) {
        val mountain = _selectedMountain.value
        if (mountain == null) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            try {
                // تصمیم محصول: کاربران رایگان ۳ روز پیش‌بینی و کاربران پریمیوم حداکثر ۱۶ روز (سقف مجاز Open-Meteo) دریافت می‌کنند.
                // Open-Meteo به‌طور پیش‌فرض ۷ روز پیش‌بینی رایگان ارائه می‌دهد، اما این اپلیکیشن به‌دلیل محدودیت
                // سرور پراکسی و پهنای باند موبایل، نسخه رایگان را به ۳ روز محدود کرده است.
                val forecastDaysToFetch = if (_isPremium.value) 16 else 3
                val response = weatherRepository.fetchWeatherForecast(
                    lat = mountain.latitude,
                    lng = mountain.longitude,
                    elevation = mountain.altitude.toDouble(),
                    forecastDays = forecastDaysToFetch,
                    pastHours = 24,
                    temperatureUnit = "celsius",
                    windSpeedUnit = "kmh",
                    precipitationUnit = "mm",
                    timezone = "auto"
                )

                val serverTime = if (response.current?.time != null) {
                    PersianDateHelper.formatIsoDateTimeToPersian(response.current.time)
                } else {
                    PersianDateHelper.getPersianDateTimeString(Date())
                }
                _lastUpdatedTime.value = serverTime

                saveWeatherToCache(mountain.id, response, serverTime)

                _weatherUiState.value = WeatherUiState.Success(
                    mountain = mountain,
                    weather = response,
                    isOffline = false,
                    offlineTime = null
                )
                onResult?.invoke(true)
            } catch (e: Throwable) {
                val cached = loadWeatherFromCache(mountain.id)
                if (cached != null) {
                    _lastUpdatedTime.value = cached.second
                    _weatherUiState.value = WeatherUiState.Success(
                        mountain = mountain,
                        weather = cached.first,
                        isOffline = true,
                        offlineTime = cached.second
                    )
                }
                onResult?.invoke(false)
            }
        }
    }

    private fun getFriendlyError(throwable: Throwable): Pair<String, WeatherErrorType> {
        val msg = throwable.localizedMessage ?: ""
        return when {
            // Open-Meteo HTTP 400 validation error: surface the API reason to the user
            throwable is OpenMeteoApiException -> {
                val reason = throwable.apiReason?.takeIf { it.isNotBlank() }
                    ?: "پارامترهای درخواست هواشناسی نامعتبر است (HTTP ${throwable.httpCode})."
                Pair("خطای سامانه هواشناسی: $reason", WeatherErrorType.SERVER_ERROR)
            }
            throwable is java.net.UnknownHostException ||
            msg.contains("UnknownHostException", ignoreCase = true) ||
            msg.contains("No address associated with hostname", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) -> {
                Pair(
                    "اتصال به اینترنت برقرار نیست! لطفا شبکه وای‌فای یا داده همراه خود را متصل کنید.",
                    WeatherErrorType.NO_INTERNET
                )
            }
            throwable is java.net.SocketTimeoutException ||
            throwable is java.util.concurrent.TimeoutException ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("SocketTimeoutException", ignoreCase = true) -> {
                Pair(
                    "زمان پاسخگویی به پایان رسید! اتصال اینترنت شما بسیار ضعیف یا ناپایدار است.",
                    WeatherErrorType.TIMEOUT
                )
            }
            msg.contains("workers.dev", ignoreCase = true) ||
            msg.contains("500") ||
            msg.contains("502") ||
            msg.contains("503") ||
            msg.contains("504") ||
            msg.contains("HTTP Code", ignoreCase = true) -> {
                Pair(
                    "اختلال موقت در پاسخگویی سرورهای ابری! لطفا دقایقی دیگر تلاش کنید.",
                    WeatherErrorType.SERVER_ERROR
                )
            }
            else -> {
                Pair(
                    "خطایی در بارگیری اطلاعات رخ داد. لطفا اتصال اینترنت را بررسی کرده و مجددا تلاش کنید.",
                    WeatherErrorType.UNKNOWN
                )
            }
        }
    }

    private fun saveWeatherToCache(mountainId: Int, response: WeatherResponse, timeStr: String) {
        val context = getApplication<android.app.Application>()
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(WeatherResponse::class.java)
            val json = adapter.toJson(response)
            
            // Save JSON to internal storage file to avoid large SharedPreferences allocations (ANR prevention)
            val file = getCacheFile(context, mountainId)
            file.writeText(json)

            // Save timestamp in prefs for fast metadata lookup and clean up any legacy SharedPreferences json
            getPrefs(context).edit().apply {
                putString("cached_weather_time_$mountainId", timeStr)
                remove("cached_weather_json_$mountainId")
                apply()
            }
            updateCachedMountainIds()
            Log.d("WeatherViewModel", "Cached weather for mountain $mountainId at $timeStr")
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to cache weather: ${e.message}")
        }
    }

    private fun loadWeatherFromCache(mountainId: Int): Pair<WeatherResponse, String>? {
        val context = getApplication<android.app.Application>()
        val prefs = getPrefs(context)
        val timeStr = prefs.getString("cached_weather_time_$mountainId", null)

        val file = getCacheFile(context, mountainId)
        val json = if (file.exists() && file.length() > 0) {
            file.readText()
        } else {
            prefs.getString("cached_weather_json_$mountainId", null)
        }

        if (json.isNullOrBlank() || timeStr.isNullOrBlank()) return null

        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(WeatherResponse::class.java)
            val response = adapter.fromJson(json)
            if (response != null) {
                Pair(response, PersianDateHelper.formatIsoDateTimeToPersian(timeStr))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to parse cached weather: ${e.message}")
            null
        }
    }

    fun fetchWeather(mountain: MountainEntity) {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val forecastDaysToFetch = if (_isPremium.value) 16 else 3
                val response = weatherRepository.fetchWeatherForecast(
                    lat = mountain.latitude,
                    lng = mountain.longitude,
                    elevation = mountain.altitude.toDouble(),
                    forecastDays = forecastDaysToFetch,
                    pastHours = 24,
                    temperatureUnit = "celsius",
                    windSpeedUnit = "kmh",
                    precipitationUnit = "mm",
                    timezone = "auto"
                )

                val serverTime = if (response.current?.time != null) {
                    PersianDateHelper.formatIsoDateTimeToPersian(response.current.time)
                } else {
                    PersianDateHelper.getPersianDateTimeString(Date())
                }
                _lastUpdatedTime.value = serverTime

                saveWeatherToCache(mountain.id, response, serverTime)

                _weatherUiState.value = WeatherUiState.Success(
                    mountain = mountain,
                    weather = response,
                    isOffline = false,
                    offlineTime = null
                )
            } catch (e: Throwable) {
                val cached = loadWeatherFromCache(mountain.id)
                if (cached != null) {
                    _lastUpdatedTime.value = cached.second
                    _weatherUiState.value = WeatherUiState.Success(
                        mountain = mountain,
                        weather = cached.first,
                        isOffline = true,
                        offlineTime = cached.second
                    )
                } else {
                    val (friendlyMsg, errType) = getFriendlyError(e)
                    _weatherUiState.value = WeatherUiState.Error(
                        message = friendlyMsg,
                        errorType = errType
                    )
                }
            }
        }
    }

    fun clearAllCachedWeather() {
        val context = getApplication<android.app.Application>()
        val prefs = getPrefs(context)
        val edit = prefs.edit()
        val allKeys = prefs.all.keys.toList()
        allKeys.forEach { key ->
            if (key.startsWith("cached_weather_json_") || key.startsWith("cached_weather_time_")) {
                edit.remove(key)
            }
        }
        edit.apply()

        try {
            context.filesDir.listFiles()?.forEach { f ->
                if (f.name.startsWith("cached_weather_") && f.name.endsWith(".json")) {
                    f.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to clear cache files: ${e.message}")
        }

        updateCachedMountainIds()

        val currentSelected = _selectedMountain.value
        if (currentSelected != null) {
            fetchWeather(currentSelected)
        }
    }

    fun clearCachedWeatherForMountain(mountainId: Int) {
        val context = getApplication<android.app.Application>()
        getPrefs(context).edit().apply {
            remove("cached_weather_json_$mountainId")
            remove("cached_weather_time_$mountainId")
            apply()
        }
        try {
            val file = getCacheFile(context, mountainId)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to delete cache file for mountain $mountainId: ${e.message}")
        }
        updateCachedMountainIds()

        val currentSelected = _selectedMountain.value
        if (currentSelected != null && currentSelected.id == mountainId) {
            fetchWeather(currentSelected)
        }
    }

    fun getCachedTimeForMountain(mountainId: Int): String? {
        val context = getApplication<android.app.Application>()
        return getPrefs(context).getString("cached_weather_time_$mountainId", null)
    }


    fun togglePin(mountain: MountainEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val dbMountain = allMountains.value.find { it.id == mountain.id } ?: mountain
            val targetNewPinned = !dbMountain.isPinned
            mountainRepository.togglePin(mountain.id, targetNewPinned)

            if (_selectedMountain.value?.id == mountain.id) {
                _selectedMountain.value = _selectedMountain.value?.copy(isPinned = targetNewPinned)
            }
            val currentState = _weatherUiState.value
            if (currentState is WeatherUiState.Success && currentState.mountain.id == mountain.id) {
                _weatherUiState.value = currentState.copy(
                    mountain = currentState.mountain.copy(isPinned = targetNewPinned)
                )
            }
            onResult?.invoke(targetNewPinned)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNewMountain(persianName: String, altitude: Int, latitude: Double, longitude: Double, province: String, range: String, type: String = "iran_peak") {
        viewModelScope.launch {
            val newMountain = MountainEntity(
                name = persianName,
                persianName = persianName,
                province = province,
                persianProvince = province,
                range = range,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                isPinned = true,
                isCustom = true,
                type = type
            )
            mountainRepository.insertMountain(newMountain)
            try {
                // Improved: using firstOrNull to avoid NoSuchElementException
                val inserted = allMountains.firstOrNull { list ->
                    list.any { it.latitude == latitude && it.longitude == longitude && it.type == type }
                }?.find { it.latitude == latitude && it.longitude == longitude && it.type == type }

                if (inserted != null) {
                    selectMountain(inserted)
                } else {
                    selectMountain(newMountain)
                }
            } catch (e: Throwable) {
                _weatherUiState.value = WeatherUiState.Error("خطا در ایجاد تراز صعود جدید: ${e.localizedMessage}")
            }
        }
    }

    fun deleteMountain(mountain: MountainEntity) {
        viewModelScope.launch {
            mountainRepository.deleteMountain(mountain)
            if (_selectedMountain.value?.id == mountain.id) {
                val rem = allMountains.value.firstOrNull { it.id != mountain.id }
                if (rem != null) {
                    selectMountain(rem)
                }
            }
        }
    }

    fun updateMountain(mountain: MountainEntity) {
        viewModelScope.launch {
            mountainRepository.updateMountain(mountain)
            if (_selectedMountain.value?.id == mountain.id) {
                _selectedMountain.value = mountain
                refreshCurrentMountainWeather()
            }
        }
    }

    fun setProvinceFilter(province: String?) {
        _selectedProvince.value = province
    }

    fun setAltitudeRange(min: Int, max: Int) {
        _altitudeRange.value = min to max
    }

    fun setSelectedType(type: String?) {
        _selectedType.value = type
        _selectedProvince.value = null
        _altitudeRange.value = 0 to 10000
    }

    fun activateCode(code: String) {
        if (code.isBlank()) {
            _activationUiState.value = ActivationUiState.Error("لطفاً کد فعال‌سازی را وارد کنید.")
            return
        }

        _activationUiState.value = ActivationUiState.Loading

        viewModelScope.launch {
            try {
                val url = "https://activation-codes-admin.persianboy-1991g.workers.dev/api/verify"
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val requestObj = mapOf("code" to code)
                val jsonRequest = moshi.adapter(Map::class.java).toJson(requestObj)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = jsonRequest.toRequestBody(mediaType)

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("User-Agent", "IranMountainWeather-Android")
                    .build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                            val adapter = moshi.adapter(ActivationVerifyResponse::class.java)
                            val verifyRes = adapter.fromJson(responseBody)
                            if (verifyRes != null && verifyRes.success == true && !verifyRes.subscription_id.isNullOrBlank()) {
                                val expiresAt = verifyRes.expires_at ?: ""
                                // Save to Datastore
                                settingsDataStore.setActivationDetails(
                                    code = code,
                                    subId = verifyRes.subscription_id,
                                    expiresAt = expiresAt
                                )
                                _isPremium.value = true
                                _activationUiState.value = ActivationUiState.Success(
                                    message = "کد فعال‌سازی با موفقیت تایید و اشتراک پرو شما فعال شد. صعود ایمنی داشته باشید!",
                                    expiresAt = expiresAt
                                )
                                Log.d("WeatherViewModel", "Activation code verified successfully: ${verifyRes.subscription_id}")
                            } else {
                                val errorMsg = parseErrorMessage(responseBody ?: "") ?: "کد فعال‌سازی نامعتبر، استفاده شده یا منقضی شده است."
                                _activationUiState.value = ActivationUiState.Error(errorMsg)
                            }
                        } else {
                            val errorMsg = responseBody?.let { parseErrorMessage(it) } ?: "کد فعال‌سازی نامعتبر است (خطای سرور: ${response.code})."
                            _activationUiState.value = ActivationUiState.Error(errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception while verifying activation code", e)
                _activationUiState.value = ActivationUiState.Error("خطا در برقراری ارتباط با سرور. لطفا اتصال اینترنت خود را بررسی کنید.")
            }
        }
    }

    private fun parseErrorMessage(json: String): String? {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
            val adapter: com.squareup.moshi.JsonAdapter<Map<String, Any>> = moshi.adapter(type)
            val map = adapter.fromJson(json)
            val raw = map?.get("reason") ?: map?.get("error") ?: map?.get("message")
            when (raw) {
                is String -> raw
                is Boolean -> null
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun checkSubscriptionOnStartup(context: android.content.Context, isManual: Boolean = false) {
        if (isManual) {
            _activationUiState.value = ActivationUiState.Loading
        }

        viewModelScope.launch {
            val code = settingsDataStore.activationCode.first()
            val subId = settingsDataStore.subscriptionId.first()

            if (code.isBlank() || subId.isBlank()) {
                Log.d("WeatherViewModel", "No local activation code or subscription ID found. Skipping startup subscription verification.")
                if (isManual) {
                    _activationUiState.value = ActivationUiState.Error("کد فعال‌سازی محلی یافت نشد.")
                }
                return@launch
            }

            Log.d("WeatherViewModel", "Local activation info found. Checking subscription status on server...")

            try {
                val url = "https://activation-codes-admin.persianboy-1991g.workers.dev/api/check-subscription"
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val requestObj = mapOf("subscription_id" to subId)
                val jsonRequest = moshi.adapter(Map::class.java).toJson(requestObj)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = jsonRequest.toRequestBody(mediaType)

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("User-Agent", "IranMountainWeather-Android")
                    .build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                            val adapter = moshi.adapter(CheckSubscriptionResponse::class.java)
                            val subRes = adapter.fromJson(responseBody)
                            if (subRes != null) {
                                if (subRes.active == true) {
                                    val newExpiresAt = subRes.expires_at ?: ""
                                    settingsDataStore.setActivationDetails(code, subId, newExpiresAt)
                                    _isPremium.value = true
                                    if (isManual) {
                                        _activationUiState.value = ActivationUiState.Success(
                                            message = "اشتراک پرو شما با موفقیت تایید و مجدداً فعال شد.",
                                            expiresAt = newExpiresAt
                                        )
                                    }
                                    Log.d("WeatherViewModel", "Subscription check: ACTIVE. Expires at: $newExpiresAt")
                                } else {
                                    // Keep details but disable premium status
                                    settingsDataStore.setPremium(false)
                                    _isPremium.value = false
                                    if (isManual) {
                                        _activationUiState.value = ActivationUiState.Error("اشتراک شما غیرفعال یا منقضی شده است.")
                                    }
                                    Log.w("WeatherViewModel", "Subscription check: INACTIVE/SUSPENDED. Kept subscription details but disabled premium.")
                                }
                            }
                        } else if (response.code == 404 || response.code == 400) {
                            settingsDataStore.clearActivationDetails()
                            _isPremium.value = false
                            if (isManual) {
                                _activationUiState.value = ActivationUiState.Error("کد فعال‌سازی شما دیگر معتبر نیست یا حذف شده است.")
                            }
                            Log.w("WeatherViewModel", "Subscription check: Server returned invalid/not found (${response.code}). Cleared local details.")
                        } else {
                            if (isManual) {
                                val errorMsg = responseBody?.let { parseErrorMessage(it) } ?: "خطا در بررسی وضعیت اشتراک (${response.code})."
                                _activationUiState.value = ActivationUiState.Error(errorMsg)
                            }
                            Log.e("WeatherViewModel", "Subscription check: Server error (${response.code}). Retaining current local subscription state.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    _activationUiState.value = ActivationUiState.Error("خطا در برقراری ارتباط با سرور. لطفا اتصال اینترنت خود را بررسی کنید.")
                }
                Log.e("WeatherViewModel", "Subscription check failed due to network error: ${e.message}. Retaining current local subscription state.")
            }
        }
    }
}

class WeatherViewModelFactory(
    private val application: android.app.Application,
    private val mountainRepository: MountainRepository,
    private val weatherRepository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(application, mountainRepository, weatherRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class SyncUiState {
    object Idle : SyncUiState()
    object Loading : SyncUiState()
    data class Success(val version: Int, val addedCount: Int, val updatedCount: Int) : SyncUiState()
    data class NoUpdate(val version: Int) : SyncUiState()
    data class Error(
        val message: String,
        val errorType: WeatherErrorType = WeatherErrorType.UNKNOWN
    ) : SyncUiState()
}

sealed class ActivationUiState {
    object Idle : ActivationUiState()
    object Loading : ActivationUiState()
    data class Success(val message: String, val expiresAt: String) : ActivationUiState()
    data class Error(val message: String) : ActivationUiState()
}

data class ActivationVerifyResponse(
    val success: Boolean?,
    val subscription_id: String?,
    val expires_at: String?,
    val duration_days: Int?
)

data class CheckSubscriptionResponse(
    val active: Boolean?,
    val expires_at: String?
)