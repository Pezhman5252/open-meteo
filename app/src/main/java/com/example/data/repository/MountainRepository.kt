package com.example.data.repository

import com.example.data.local.MountainDao
import com.example.data.local.MountainEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.squareup.moshi.Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class MountainRepository(private val mountainDao: MountainDao) {
    val allMountains: Flow<List<MountainEntity>> = mountainDao.getAllMountains()
    val pinnedMountains: Flow<List<MountainEntity>> = mountainDao.getPinnedMountains()

    fun searchMountains(query: String): Flow<List<MountainEntity>> {
        return mountainDao.searchMountains(query)
    }

    fun getMountainsByProvince(province: String): Flow<List<MountainEntity>> {
        return mountainDao.getMountainsByProvince(province)
    }

    suspend fun getMountainById(id: Int): MountainEntity? = withContext(Dispatchers.IO) {
        return@withContext mountainDao.getMountainList().find { it.id == id }
    }

    suspend fun togglePin(id: Int, isPinned: Boolean) = withContext(Dispatchers.IO) {
        mountainDao.updatePinnedStatus(id, isPinned)
    }

    suspend fun updateMountain(mountain: MountainEntity) = withContext(Dispatchers.IO) {
        mountainDao.updateMountain(mountain)
    }

    suspend fun insertMountain(mountain: MountainEntity) = withContext(Dispatchers.IO) {
        mountainDao.insertMountains(listOf(mountain))
    }

    suspend fun deleteMountain(mountain: MountainEntity) = withContext(Dispatchers.IO) {
        mountainDao.deleteMountain(mountain)
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (mountainDao.getMountainCount() == 0) {
            val initialMountains = listOf(
                // ---- IRAN PEAKS ----
                MountainEntity(name = "Damavand", persianName = "دماوند", province = "مازندران", persianProvince = "مازندران", range = "البرز مرکزی", latitude = 35.9516, longitude = 52.1102, altitude = 5610, isPinned = true, type = "iran_peak"),
                MountainEntity(name = "Alam-Kuh", persianName = "علم کوه", province = "مازندران", persianProvince = "مازندران", range = "البرز (تخت سلیمان)", latitude = 36.3756, longitude = 50.9632, altitude = 4850, isPinned = true, type = "iran_peak"),
                MountainEntity(name = "Sabalan", persianName = "سبلان", province = "اردبیل", persianProvince = "اردبیل", range = "البرز غربی", latitude = 38.2662, longitude = 47.8346, altitude = 4811, isPinned = true, type = "iran_peak"),
                MountainEntity(name = "Hezar", persianName = "هزار", province = "کرمان", persianProvince = "کرمان", range = "کوه‌های مرکزی", latitude = 29.5134, longitude = 57.2711, altitude = 4501, type = "iran_peak"),
                MountainEntity(name = "Kholeno", persianName = "خلنو", province = "تهران", persianProvince = "تهران", range = "البرز مرکزی", latitude = 36.0125, longitude = 51.5512, altitude = 4375, type = "iran_peak"),
                MountainEntity(name = "Dena (Qash-Mastan)", persianName = "دنا (قاش مستان)", province = "کهگیلویه و بویراحمد", persianProvince = "کهگیلویه و بویراحمد", range = "زاگرس جنوبی", latitude = 30.9496, longitude = 51.4243, altitude = 4459, isPinned = true, type = "iran_peak"),
                MountainEntity(name = "Zard-Kuh (Kolonchin)", persianName = "زردکوه (کلونچین)", province = "چهارمحال و بختیاری", persianProvince = "چهارمحال و بختیاری", range = "زاگرس مرکزی", latitude = 32.3644, longitude = 50.0784, altitude = 4221, isPinned = true, type = "iran_peak"),
                MountainEntity(name = "Sialan", persianName = "سیالان", province = "قزوین", persianProvince = "قزوین", range = "البرز غربی", latitude = 36.5097, longitude = 50.7814, altitude = 4185, type = "iran_peak"),
                MountainEntity(name = "Oshtorankuh (San-Boran)", persianName = "اشترانکوه (سن بران)", province = "لرستان", persianProvince = "لرستان", range = "زاگرس مرکزی", latitude = 33.3371, longitude = 49.2785, altitude = 4150, type = "iran_peak"),
                MountainEntity(name = "Shah Alborz", persianName = "شاه البرز", province = "البرز", persianProvince = "البرز", range = "البرز غربی", latitude = 36.3146, longitude = 50.7508, altitude = 4125, type = "iran_peak"),
                MountainEntity(name = "Shirkooh", persianName = "شیرکوه", province = "یزد", persianProvince = "یزد", range = "کوه‌های مرکزی", latitude = 31.6421, longitude = 54.1755, altitude = 4075, type = "iran_peak"),
                MountainEntity(name = "Shahan Kuh", persianName = "شاهان کوه", province = "اصفهان", persianProvince = "اصفهان", range = "زاگرس مرکزی", latitude = 32.8125, longitude = 49.9822, altitude = 4040, type = "iran_peak"),
                MountainEntity(name = "Shahvar", persianName = "شاهوار", province = "سمنان", persianProvince = "سمنان", range = "البرز شرقی", latitude = 36.5746, longitude = 54.7612, altitude = 3945, type = "iran_peak"),
                MountainEntity(name = "Taftan", persianName = "تفتان", province = "سیستان و بلوچستان", persianProvince = "سیستان و بلوچستان", range = "کوه‌های جنوب شرقی", latitude = 28.5997, longitude = 61.1294, altitude = 3941, type = "iran_peak"),
                MountainEntity(name = "Bel", persianName = "بلوچ (بل)", province = "فارس", persianProvince = "فارس", range = "زاگرس جنوبی", latitude = 31.1492, longitude = 52.7486, altitude = 3943, type = "iran_peak"),
                MountainEntity(name = "Gavkoshan", persianName = "گاوکشان", province = "گلستان", persianProvince = "گلستان", range = "البرز شرقی (شاهکوه)", latitude = 36.5684, longitude = 54.5421, altitude = 3813, type = "iran_peak"),
                MountainEntity(name = "Kinu", persianName = "کینو", province = "خوزستان", persianProvince = "خوزستان", range = "زاگرس مرکزی", latitude = 32.4842, longitude = 49.4894, altitude = 3745, type = "iran_peak"),
                MountainEntity(name = "Somamous", persianName = "سماموس", province = "گیلان", persianProvince = "گیلان", range = "البرز غربی", latitude = 36.8392, longitude = 50.3837, altitude = 3720, type = "iran_peak"),
                MountainEntity(name = "Avrin", persianName = "اورین", province = "آذربایجان غربی", persianProvince = "آذربایجان غربی", range = "مرزی زاگرس", latitude = 38.5847, longitude = 44.3642, altitude = 3702, type = "iran_peak"),
                MountainEntity(name = "Koubari", persianName = "کوبرى", province = "همدان", persianProvince = "همدان", range = "زاگرس شمالی (الوند)", latitude = 34.6432, longitude = 48.4325, altitude = 3586, type = "iran_peak"),
                MountainEntity(name = "Kamal", persianName = "کمال (سهند)", province = "آذربایجان شرقی", persianProvince = "آذربایجان شرقی", range = "توده سهند", latitude = 37.7314, longitude = 46.5111, altitude = 3707, type = "iran_peak"),
                MountainEntity(name = "Domir", persianName = "دومیر", province = "مرکزی", persianProvince = "مرکزی", range = "اردهال (مرکزی)", latitude = 33.9114, longitude = 51.1328, altitude = 3505, type = "iran_peak"),
                MountainEntity(name = "Paraw", persianName = "پراو", province = "کرمانشاه", persianProvince = "کرمانشاه", range = "زاگرس بیستون", latitude = 34.4024, longitude = 47.2435, altitude = 3405, type = "iran_peak"),
                MountainEntity(name = "Belqeys", persianName = "بلقیس", province = "زنجان", persianProvince = "زنجان", range = "آذربایجان", latitude = 36.6084, longitude = 47.3197, altitude = 3343, type = "iran_peak"),
                MountainEntity(name = "Shirbad", persianName = "شیرباد", province = "خراسان رضوی", persianProvince = "خراسان رضوی", range = "بینالود", latitude = 36.2828, longitude = 59.0435, altitude = 3303, type = "iran_peak"),
                MountainEntity(name = "Tashger", persianName = "تشگر", province = "هرمزگان", persianProvince = "هرمزگان", range = "زاگرس جنوبی (هماگ)", latitude = 27.7686, longitude = 56.3267, altitude = 3267, type = "iran_peak"),
                MountainEntity(name = "Barf Anbar", persianName = "برف انبار", province = "قم", persianProvince = "قم", range = "مرکزی ایران", latitude = 34.2312, longitude = 50.9025, altitude = 3220, type = "iran_peak"),
                MountainEntity(name = "Zoleikha", persianName = "زلیخا (چهل چشمه)", province = "کردستان", persianProvince = "کردستان", range = "زاگرس کردستان", latitude = 35.8012, longitude = 46.5314, altitude = 3220, type = "iran_peak"),
                MountainEntity(name = "Shah Jahan", persianName = "شاه جهان", province = "خراسان شمالی", persianProvince = "خراسان شمالی", range = "آلاداغ", latitude = 37.1008, longitude = 57.8542, altitude = 3064, type = "iran_peak"),
                MountainEntity(name = "Kan Seyfi", persianName = "کان صیفی", province = "ایلام", persianProvince = "ایلام", range = "کبیرکوه (زاگرس)", latitude = 33.3421, longitude = 46.5014, altitude = 3050, type = "iran_peak"),
                MountainEntity(name = "Nayband", persianName = "نایبند", province = "خراسان جنوبی", persianProvince = "خراسان جنوبی", range = "بلوک طبس", latitude = 32.0684, longitude = 57.1724, altitude = 3005, type = "iran_peak"),

                // ---- INTERNATIONAL PEAKS ----
                MountainEntity(name = "Everest", persianName = "اورست", province = "نپال / چین (تبت)", persianProvince = "نپال / چین (تبت)", range = "هیمالیا", latitude = 27.9881, longitude = 86.9250, altitude = 8848, type = "international_peak"),
                MountainEntity(name = "K2", persianName = "کی ۲", province = "پاکستان / چین", persianProvince = "پاکستان / چین", range = "قراقروم", latitude = 35.8808, longitude = 76.5133, altitude = 8611, type = "international_peak"),
                MountainEntity(name = "Kangchenjunga", persianName = "کانچنچونگا", province = "نپال / هند", persianProvince = "نپال / هند", range = "هیمالیا", latitude = 27.7025, longitude = 88.1475, altitude = 8586, type = "international_peak"),
                MountainEntity(name = "Lhotse", persianName = "لوتسه", province = "نپال / چین (تبت)", persianProvince = "نپال / چین (تبت)", range = "هیمالیا", latitude = 27.9617, longitude = 86.9333, altitude = 8516, type = "international_peak"),
                MountainEntity(name = "Makalu", persianName = "ماکالو", province = "نپال / چین (تبت)", persianProvince = "نپال / چین (تبت)", range = "هیمالیا", latitude = 27.8892, longitude = 87.0889, altitude = 8485, type = "international_peak"),
                MountainEntity(name = "Cho Oyu", persianName = "چو اویو", province = "نپال / چین (تبت)", persianProvince = "نپال / چین (تبت)", range = "هیمالیا", latitude = 28.0942, longitude = 86.6608, altitude = 8188, type = "international_peak"),
                MountainEntity(name = "Nanga Parbat", persianName = "نانگاپاربات", province = "پاکستان", persianProvince = "پاکستان", range = "هیمالیا", latitude = 35.2375, longitude = 74.5891, altitude = 8126, type = "international_peak"),
                MountainEntity(name = "Manaslu", persianName = "ماناسلو", province = "نپال", persianProvince = "نپال", range = "هیمالیا", latitude = 28.5497, longitude = 84.5597, altitude = 8163, type = "international_peak"),
                MountainEntity(name = "Aconcagua", persianName = "آکانکاگوا", province = "آرژانتین", persianProvince = "آرژانتین", range = "آند", latitude = -32.6532, longitude = -70.0108, altitude = 6961, type = "international_peak"),
                MountainEntity(name = "Denali", persianName = "دینالی (مک کینلی)", province = "ایالات متحده آمریکا", persianProvince = "ایالات متحده آمریکا", range = "آلاسکا", latitude = 63.0692, longitude = -151.0070, altitude = 6190, type = "international_peak"),
                MountainEntity(name = "Kilimanjaro", persianName = "کلیمانجارو", province = "تانزانیا", persianProvince = "تانزانیا", range = "شرق آفریقا", latitude = -3.0674, longitude = 37.3556, altitude = 5895, type = "international_peak"),
                MountainEntity(name = "Elbrus", persianName = "البروس", province = "روسیه", persianProvince = "روسیه", range = "قفقاز", latitude = 43.3499, longitude = 42.4453, altitude = 5642, type = "international_peak"),
                MountainEntity(name = "Ararat", persianName = "آرارات", province = "ترکیه", persianProvince = "ترکیه", range = "آرارات", latitude = 39.7024, longitude = 44.2991, altitude = 5137, type = "international_peak"),
                MountainEntity(name = "Vinson Massif", persianName = "توده وینسون", province = "قاره آنتارکتیکا (قطب جنوب)", persianProvince = "قاره آنتارکتیکا (قطب جنوب)", range = "سنتینل", latitude = -78.5255, longitude = -85.6171, altitude = 4892, type = "international_peak"),
                MountainEntity(name = "Puncak Jaya", persianName = "پونچاک جایا (کارستنز)", province = "اندونزی (اقیانوسیه)", persianProvince = "اندونزی (اقیانوسیه)", range = "سودیرمان", latitude = -4.0844, longitude = 137.1866, altitude = 4884, type = "international_peak"),
                MountainEntity(name = "Mont Blanc", persianName = "مون بلان", province = "فرانسه / ایتالیا", persianProvince = "فرانسه / ایتالیا", range = "آلپ", latitude = 45.8326, longitude = 6.8652, altitude = 4808, type = "international_peak"),
                MountainEntity(name = "Kazbek", persianName = "کازبک", province = "گرجستان", persianProvince = "گرجستان", range = "قفقاز", latitude = 42.6972, longitude = 44.5192, altitude = 5054, type = "international_peak"),
                MountainEntity(name = "Lenin Peak", persianName = "لنین (ابن سینا)", province = "قرقیزستان / تاجیکستان", persianProvince = "قرقیزستان / تاجیکستان", range = "پامیر", latitude = 39.3464, longitude = 72.8694, altitude = 7134, type = "international_peak"),
                MountainEntity(name = "Matterhorn", persianName = "ماترهورن", province = "سوئیس / ایتالیا", persianProvince = "سوئیس / ایتالیا", range = "آلپ", latitude = 45.9766, longitude = 7.6585, altitude = 4478, type = "international_peak"),
                MountainEntity(name = "Khan Tengri", persianName = "خان تنگری", province = "قزاقستان / قرقیزستان", persianProvince = "قزاقستان / قرقیزستان", range = "تیان شان", latitude = 42.2106, longitude = 80.2681, altitude = 7010, type = "international_peak"),

                // ---- SKI RESORTS ----
                MountainEntity(name = "Tochal (Station 7)", persianName = "توچال (ایستگاه ۷)", province = "تهران", persianProvince = "تهران", range = "البرز مرکزی", latitude = 35.8835, longitude = 51.4215, altitude = 3575, type = "ski_resort"),
                MountainEntity(name = "Dizin", persianName = "دیزین", province = "البرز", persianProvince = "البرز", range = "البرز مرکزی", latitude = 36.0491, longitude = 51.4172, altitude = 2650, type = "ski_resort"),
                MountainEntity(name = "Darbandsar", persianName = "دربندسر", province = "تهران", persianProvince = "تهران", range = "البرز مرکزی", latitude = 36.0285, longitude = 51.4428, altitude = 2650, type = "ski_resort"),
                MountainEntity(name = "Shemshak", persianName = "شمشک", province = "تهران", persianProvince = "تهران", range = "البرز مرکزی", latitude = 36.0087, longitude = 51.4947, altitude = 2550, type = "ski_resort"),
                MountainEntity(name = "Abali", persianName = "آبعلی", province = "تهران", persianProvince = "تهران", range = "البرز مرکزی", latitude = 35.7511, longitude = 51.9542, altitude = 2400, type = "ski_resort"),
                MountainEntity(name = "Pooladkaf", persianName = "پولادکف", province = "فارس", persianProvince = "فارس", range = "زاگرس جنوبی", latitude = 30.3421, longitude = 51.9135, altitude = 2820, type = "ski_resort"),
                MountainEntity(name = "Alvares", persianName = "آلوارس", province = "اردبیل", persianProvince = "اردبیل", range = "توده سبلان", latitude = 38.2045, longitude = 47.9351, altitude = 3050, type = "ski_resort"),
                MountainEntity(name = "Fereydunshahr", persianName = "فریدون‌شهر", province = "اصفهان", persianProvince = "اصفهان", range = "زاگرس مرکزی", latitude = 32.9312, longitude = 50.0415, altitude = 2630, type = "ski_resort"),
                MountainEntity(name = "Sahand", persianName = "سهند", province = "آذربایجان شرقی", persianProvince = "آذربایجان شرقی", range = "توده سهند", latitude = 37.7495, longitude = 46.5162, altitude = 2915, type = "ski_resort"),
                MountainEntity(name = "Shirbad Ski Resort", persianName = "شیرباد", province = "خراسان رضوی", persianProvince = "خراسان رضوی", range = "بینالود", latitude = 36.3045, longitude = 59.0512, altitude = 3000, type = "ski_resort"),
                MountainEntity(name = "Chelgerd (Kuhrang)", persianName = "چلگرد (کوهرنگ)", province = "چهارمحال و بختیاری", persianProvince = "چهارمحال و بختیاری", range = "زاگرس مرکزی", latitude = 32.4772, longitude = 50.1135, altitude = 2350, type = "ski_resort"),
                MountainEntity(name = "Khoshako", persianName = "خوشاکو", province = "آذربایجان غربی", persianProvince = "آذربایجان غربی", range = "مرزی زاگرس", latitude = 37.4912, longitude = 44.6851, altitude = 2000, type = "ski_resort"),
                MountainEntity(name = "Kakan (Dena)", persianName = "کاکان (دنا)", province = "کهگیلویه و بویراحمد", persianProvince = "کهگیلویه و بویراحمد", range = "زاگرس جنوبی", latitude = 30.6842, longitude = 51.7215, altitude = 2640, type = "ski_resort"),
                MountainEntity(name = "Bijar (Nesar)", persianName = "بیجار (نسار)", province = "کردستان", persianProvince = "کردستان", range = "زاگرس کردستان", latitude = 35.8752, longitude = 47.6185, altitude = 2000, type = "ski_resort"),
                MountainEntity(name = "Khor", persianName = "خور", province = "البرز", persianProvince = "البرز", range = "البرز مرکزی", latitude = 35.9172, longitude = 51.1541, altitude = 2400, type = "ski_resort"),
                MountainEntity(name = "Tarik Dareh", persianName = "تاریک‌دره", province = "همدان", persianProvince = "همدان", range = "زاگرس شمالی (الوند)", latitude = 34.7412, longitude = 48.4515, altitude = 2600, type = "ski_resort"),
                MountainEntity(name = "Papayi", persianName = "پاپایی", province = "زنجان", persianProvince = "زنجان", range = "آذربایجان", latitude = 36.5684, longitude = 48.3541, altitude = 2150, type = "ski_resort"),
                MountainEntity(name = "Payam Marand", persianName = "پیام مرند", province = "آذربایجان شرقی", persianProvince = "آذربایجان شرقی", range = "توده میشو", latitude = 38.3312, longitude = 45.7485, altitude = 1850, type = "ski_resort"),
                MountainEntity(name = "Shazand (Pakal)", persianName = "شازند (پاکل)", province = "مرکزی", persianProvince = "مرکزی", range = "زاگرس مرکزی", latitude = 33.8142, longitude = 49.3785, altitude = 2450, type = "ski_resort"),
                MountainEntity(name = "Tamandar Aligudarz", persianName = "تمندر الیگودرز", province = "لرستان", persianProvince = "لرستان", range = "زاگرس مرکزی", latitude = 33.2541, longitude = 49.6582, altitude = 2600, type = "ski_resort"),
                MountainEntity(name = "Sikan", persianName = "سیکان", province = "ایلام", persianProvince = "ایلام", range = "کبیرکوه (زاگرس)", latitude = 33.1245, longitude = 47.3812, altitude = 1900, type = "ski_resort"),
                MountainEntity(name = "Kamfiruz", persianName = "کامفیروز", province = "فارس", persianProvince = "فارس", range = "زاگرس جنوبی", latitude = 30.2241, longitude = 52.1852, altitude = 2200, type = "ski_resort"),
                MountainEntity(name = "Saqqez (Roshan Kooh)", persianName = "سقز (روشن کوه)", province = "کردستان", persianProvince = "کردستان", range = "زاگرس کردستان", latitude = 36.1425, longitude = 46.2214, altitude = 2100, type = "ski_resort"),
                MountainEntity(name = "Gerkan", persianName = "گرکان (اراک)", province = "مرکزی", persianProvince = "مرکزی", range = "مرکزی ایران", latitude = 34.2851, longitude = 49.5142, altitude = 2200, type = "ski_resort")
            )
            mountainDao.insertMountains(initialMountains)
        }
    }

    suspend fun syncWithRemote(url: String, currentVersion: Int): SyncResult = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "IranMountainWeather-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("خطا در پاسخ سرور: کد پاسخ ${response.code}")
                }

                val body = response.body?.string() ?: return@withContext SyncResult.Error("پذیرش داده ناموفق بود - پاسخ خالی است")

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val adapter = moshi.adapter(MountainSyncResponse::class.java)
                val syncData = try {
                    adapter.fromJson(body)
                } catch (e: Exception) {
                    return@withContext SyncResult.Error("خطا در آنالیز فایل دیتابیس جیسون: ${e.localizedMessage}")
                } ?: return@withContext SyncResult.Error("اطلاعات دریافتی خالی است")

                if (syncData.version <= currentVersion) {
                    return@withContext SyncResult.NoUpdate(syncData.version)
                }

                // Query existing peaks
                val localItems = mountainDao.getMountainList()
                val localOfficialMap = localItems.filter { !it.isCustom }.associateBy { it.name }

                val flattenList = mutableListOf<Pair<MountainSyncItem, String>>()
                syncData.iranPeaks?.forEach { flattenList.add(it to "iran_peak") }
                syncData.internationalPeaks?.forEach { flattenList.add(it to "international_peak") }
                syncData.skiResorts?.forEach { flattenList.add(it to "ski_resort") }

                var addedCount = 0
                var updatedCount = 0

                val onlineNames = flattenList.map { it.first.englishName }.toSet()

                for ((remoteItem, typeValue) in flattenList) {
                    val existingLocal = localOfficialMap[remoteItem.englishName]
                    if (existingLocal != null) {
                        val isChanged = existingLocal.persianName != remoteItem.name ||
                                existingLocal.province != remoteItem.province ||
                                existingLocal.persianProvince != remoteItem.province ||
                                existingLocal.range != (remoteItem.range ?: "") ||
                                existingLocal.latitude != remoteItem.latitude ||
                                existingLocal.longitude != remoteItem.longitude ||
                                existingLocal.type != typeValue ||
                                existingLocal.altitude != remoteItem.altitudeMeters

                        if (isChanged) {
                            val updatedEntity = existingLocal.copy(
                                persianName = remoteItem.name,
                                province = remoteItem.province,
                                persianProvince = remoteItem.province,
                                range = remoteItem.range ?: "",
                                latitude = remoteItem.latitude,
                                longitude = remoteItem.longitude,
                                altitude = remoteItem.altitudeMeters,
                                type = typeValue
                            )
                            mountainDao.updateMountain(updatedEntity)
                            updatedCount++
                        }
                    } else {
                        // Safe insertion of new official mountain
                        val newEntity = MountainEntity(
                            name = remoteItem.englishName,
                            persianName = remoteItem.name,
                            province = remoteItem.province,
                            persianProvince = remoteItem.province,
                            range = remoteItem.range ?: "",
                            latitude = remoteItem.latitude,
                            longitude = remoteItem.longitude,
                            altitude = remoteItem.altitudeMeters,
                            isPinned = false,
                            isCustom = false,
                            type = typeValue
                        )
                        mountainDao.insertMountains(listOf(newEntity))
                        addedCount++
                    }
                }

                // Delete official mountains that were removed from the official online dataset
                for (localOfficialItem in localOfficialMap.values) {
                    if (!onlineNames.contains(localOfficialItem.name)) {
                        mountainDao.deleteMountain(localOfficialItem)
                    }
                }

                return@withContext SyncResult.Success(syncData.version, addedCount, updatedCount)
            }
        } catch (e: Exception) {
            return@withContext SyncResult.Error("خطا در شبکه یا کار با پایگاه داده محلی: ${e.localizedMessage}")
        }
    }
}

// Sealed status tracking for clean UI update flow
sealed class SyncResult {
    data class Success(val version: Int, val addedCount: Int, val updatedCount: Int) : SyncResult()
    data class NoUpdate(val version: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

// Moshi mappings
data class MountainSyncResponse(
    val version: Int,
    @param:Json(name = "iran_peaks") val iranPeaks: List<MountainSyncItem>? = emptyList(),
    @param:Json(name = "international_peaks") val internationalPeaks: List<MountainSyncItem>? = emptyList(),
    @param:Json(name = "ski_resorts") val skiResorts: List<MountainSyncItem>? = emptyList()
)

data class MountainSyncItem(
    val name: String, // Contains Persian Name e.g. "دماوند"
    @param:Json(name = "english_name") val englishName: String, // Contains English Name e.g. "Damavand"
    @param:Json(name = "altitude_meters") val altitudeMeters: Int, // Altitude meters
    val latitude: Double,
    val longitude: Double,
    val range: String? = "",
    val province: String // Persian Province e.g. "مازندران"
)
