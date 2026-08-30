package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.OpenMeteoApiService
import com.example.data.remote.WeatherResponse
import retrofit2.Response
import com.example.data.repository.MountainRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.weather.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PremiumSubscriptionTest {

    private lateinit var database: AppDatabase
    private lateinit var mountainRepository: MountainRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var viewModel: WeatherViewModel
    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Guarantee a completely pristine isolated state by deleting all persistent files (SharedPreferences, DataStore, etc.)
        context.filesDir.parentFile?.listFiles()?.forEach { file ->
            if (file.name != "lib") {
                file.deleteRecursively()
            }
        }
        
        // In-memory database for testing Room interactions safely on JVM
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        mountainRepository = MountainRepository(database.mountainDao())
        
        // Stub service for WeatherRepository since we are testing VM logic and state persistence
        val stubApiService = object : OpenMeteoApiService {
            override suspend fun getForecast(
                latitude: String,
                longitude: String,
                elevation: String?,
                temperatureUnit: String,
                timeformat: String,
                pastDays: Int,
                pastHours: Int?,
                startDate: String?,
                endDate: String?,
                startHour: String?,
                endHour: String?,
                models: String?,
                cellSelection: String,
                forecastHours: Int?,
                current: String,
                hourly: String,
                daily: String,
                minutely15: String,
                windSpeedUnit: String,
                precipitationUnit: String,
                forecastDays: Int,
                timezone: String
            ): Response<WeatherResponse> {
                throw NotImplementedError("Stub api is not supposed to be called in this test.")
            }
        }
        
        weatherRepository = WeatherRepository(stubApiService)
        
        // Explicitly reset Datastore singleton in-memory state to false to avoid cross-test pollution in Robolectric
        runBlocking {
            SettingsDataStore(context).setPremium(false)
            delay(100)
        }
        
        viewModel = WeatherViewModel(context as android.app.Application, mountainRepository, weatherRepository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testPremiumStateFlow_defaultsToFalse() = runBlocking {
        // Assert default premium state is false
        assertFalse("By default, isPremium should be false", viewModel.isPremium.value)
    }

    @Test
    fun testSetPremium_updatesStateFlowAndPersistsToPreferences() = runBlocking {
        // Act: Set premium to true
        viewModel.setPremium(context, true)
        
        // Let background datastore writes and eager unconfined collection complete
        delay(300)
        
        // Assert StateFlow has updated
        assertTrue("StateFlow should be updated to true", viewModel.isPremium.value)
        
        // Assert it is saved in SettingsDataStore
        val savedPremium = SettingsDataStore(context).isPremium.first()
        assertTrue("is_premium should be saved as true in SettingsDataStore", savedPremium)
    }

    @Test
    fun testSetPremium_updatesStateFlowToFalseAndPersistsToPreferences() = runBlocking {
        // Arrange
        viewModel.setPremium(context, true)
        delay(300)
        assertTrue("Initial state should be true", viewModel.isPremium.value)
        
        // Act: Set premium to false
        viewModel.setPremium(context, false)
        delay(300)
        
        // Assert StateFlow has updated
        assertFalse("StateFlow should be updated to false", viewModel.isPremium.value)
        
        // Assert it is saved in SettingsDataStore
        val savedPremium = SettingsDataStore(context).isPremium.first()
        assertFalse("is_premium should be saved as false in SettingsDataStore", savedPremium)
    }

    @Test
    fun testInitBlock_migratesOldPremiumStateToDataStore() = runBlocking {
        // Arrange: manually insert true to SharedPreferences
        val prefs = context.getSharedPreferences("mountain_sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium", true).commit()
        
        // Create a fresh ViewModel which migrates old preference to DataStore in init
        val freshViewModel = WeatherViewModel(context as android.app.Application, mountainRepository, weatherRepository)
        
        // Let initialization collection run
        delay(300)
        
        // Assert StateFlow is restored correctly
        assertTrue("isPremium should be restored as true upon initialization", freshViewModel.isPremium.value)
    }
}
