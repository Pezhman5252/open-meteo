// ============================================================
// دیتابیس جامع (قله‌های ایران + قله‌های بین‌المللی + پیست‌های اسکی)
// ============================================================
const MOUNTAINS_DB = {
  // ⚠️ قانون نسخه‌بندی: با هر تغییر (افزودن/ویرایش/حذف هر قله یا پیست) این عدد را
  // +۱ کنید. اپلیکیشن فقط زمانی اطلس را به‌روز می‌کند که نسخه پاسخ > نسخه ذخیره‌شده
  // روی دستگاه کاربر باشد؛ اگر نسخه بالاتر از نسخه فعلی Production نباشد، کاربر
  // «به‌روزرسانی نیاز نیست» می‌بیند و هیچ داده‌ای تغییر نمی‌کند.
  // Production فعلی = v5 (۲۰۲۶-۱۰-۰۳)؛ قبل از هر تغییر همیشه نسخه‌ی زنده را چک کنید:
  // curl -s https://mountain-api.iranmountainweather.workers.dev/ | head -c 20
  version: 5, // با هر تغییر در هر بخش، این عدد را +۱ کنید (v5: دیتابیس جامع — ۱۵۲ قله ایران + ۲۰ بین‌المللی + ۲۴ پیست اسکی؛ مطابقت با Production)
  "iran_peaks": [
    {
      "id": 1,
      "name": "دماوند",
      "english_name": "Damavand",
      "altitude_meters": 5610,
      "latitude": 35.955,
      "longitude": 52.110,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 2,
      "name": "علم‌کوه",
      "english_name": "Alam Kuh",
      "altitude_meters": 4848,
      "latitude": 36.3757,
      "longitude": 50.9614,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 3,
      "name": "سبلان",
      "english_name": "Sabalan",
      "altitude_meters": 4811,
      "latitude": 38.267,
      "longitude": 47.837,
      "range": "کوه‌های آذربایجان",
      "province": "اردبیل"
    },
    {
      "id": 4,
      "name": "هرم",
      "english_name": "Heram",
      "altitude_meters": 4600,
      "latitude": 38.270,
      "longitude": 47.830,
      "range": "کوه‌های آذربایجان (سبلان)",
      "province": "اردبیل"
    },
    {
      "id": 5,
      "name": "کسری",
      "english_name": "Kasra",
      "altitude_meters": 4500,
      "latitude": 38.265,
      "longitude": 47.825,
      "range": "کوه‌های آذربایجان (سبلان)",
      "province": "اردبیل"
    },
    {
      "id": 6,
      "name": "شاخک",
      "english_name": "Shakhak",
      "altitude_meters": 4795,
      "latitude": 36.374,
      "longitude": 50.969,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 7,
      "name": "مرجی‌کش",
      "english_name": "Marji-Kesh",
      "altitude_meters": 4580,
      "latitude": 36.370,
      "longitude": 50.965,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 8,
      "name": "خرسان شمالی",
      "english_name": "Khersan Shomali",
      "altitude_meters": 4680,
      "latitude": 36.350,
      "longitude": 50.980,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 9,
      "name": "خرسان جنوبی",
      "english_name": "Khersan Jonubi",
      "altitude_meters": 4659,
      "latitude": 36.340,
      "longitude": 50.975,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 10,
      "name": "تخت سلیمان",
      "english_name": "Takht-e Suleyman",
      "altitude_meters": 4643,
      "latitude": 36.390,
      "longitude": 50.960,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 11,
      "name": "شانه کوه",
      "english_name": "Shaneh-Kuh",
      "altitude_meters": 4465,
      "latitude": 36.385,
      "longitude": 50.955,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 12,
      "name": "سیاه‌سنگ",
      "english_name": "Siah Sang",
      "altitude_meters": 4604,
      "latitude": 36.360,
      "longitude": 50.950,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 13,
      "name": "هفت‌خوان",
      "english_name": "Haft Khan",
      "altitude_meters": 4537,
      "latitude": 36.380,
      "longitude": 50.930,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 14,
      "name": "چالون",
      "english_name": "Chaloon",
      "altitude_meters": 4516,
      "latitude": 36.370,
      "longitude": 50.920,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 15,
      "name": "لنگری",
      "english_name": "Langari",
      "altitude_meters": 4510,
      "latitude": 36.250,
      "longitude": 51.000,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 16,
      "name": "هزار",
      "english_name": "Hezar",
      "altitude_meters": 4501,
      "latitude": 29.511,
      "longitude": 57.271,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 17,
      "name": "سیاه‌کمان",
      "english_name": "Siah Kaman",
      "altitude_meters": 4472,
      "latitude": 36.365,
      "longitude": 50.940,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 18,
      "name": "گرده‌کوه",
      "english_name": "Gerdeh Kuh",
      "altitude_meters": 4450,
      "latitude": 36.100,
      "longitude": 51.400,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 19,
      "name": "سیاه‌گوک شمالی",
      "english_name": "Siah Guk Shomali",
      "altitude_meters": 4445,
      "latitude": 36.355,
      "longitude": 50.945,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 20,
      "name": "دنا (قاش‌مستان)",
      "english_name": "Dena (Ghash Mastan)",
      "altitude_meters": 4435,
      "latitude": 30.950,
      "longitude": 51.433,
      "range": "زاگرس مرکزی",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 21,
      "name": "سیاه‌گوک جنوبی",
      "english_name": "Siah Guk Jonubi",
      "altitude_meters": 4430,
      "latitude": 36.348,
      "longitude": 50.942,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 22,
      "name": "میان سه چال",
      "english_name": "Mian-Seh-Chal",
      "altitude_meters": 4356,
      "latitude": 36.372,
      "longitude": 50.948,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 23,
      "name": "کالاهو",
      "english_name": "Kalahu",
      "altitude_meters": 4412,
      "latitude": 36.385,
      "longitude": 50.915,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 24,
      "name": "گردونکوه",
      "english_name": "Gardoon Kuh",
      "altitude_meters": 4402,
      "latitude": 36.375,
      "longitude": 50.910,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 25,
      "name": "شاه (کوه شاه)",
      "english_name": "Shah (Kuh-e Shah)",
      "altitude_meters": 4402,
      "latitude": 29.400,
      "longitude": 56.750,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 26,
      "name": "خلنو",
      "english_name": "Kholeno",
      "altitude_meters": 4390,
      "latitude": 36.064,
      "longitude": 51.551,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 27,
      "name": "سردشت",
      "english_name": "Sardasht",
      "altitude_meters": 4378,
      "latitude": 36.050,
      "longitude": 51.600,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 28,
      "name": "آزادکوه",
      "english_name": "Azad Kuh",
      "altitude_meters": 4375,
      "latitude": 36.169,
      "longitude": 51.502,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 29,
      "name": "چپکرو",
      "english_name": "Chapakro",
      "altitude_meters": 4338,
      "latitude": 36.150,
      "longitude": 51.450,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 30,
      "name": "لشگرک",
      "english_name": "Lashgarak",
      "altitude_meters": 4256,
      "latitude": 36.090,
      "longitude": 51.580,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 31,
      "name": "پالون گردن",
      "english_name": "Paloon Gardan",
      "altitude_meters": 4256,
      "latitude": 36.085,
      "longitude": 51.587,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 32,
      "name": "خرس چال",
      "english_name": "Khers Chal",
      "altitude_meters": 4253,
      "latitude": 36.054,
      "longitude": 51.539,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 33,
      "name": "میش چال",
      "english_name": "Mish Chal",
      "altitude_meters": 4253,
      "latitude": 36.068,
      "longitude": 51.562,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 34,
      "name": "پازن پیر",
      "english_name": "Pazan Pir",
      "altitude_meters": 4250,
      "latitude": 30.789,
      "longitude": 51.649,
      "range": "زاگرس (دنا)",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 35,
      "name": "قزل قله",
      "english_name": "Ghezel Gholleh",
      "altitude_meters": 4250,
      "latitude": 30.941,
      "longitude": 51.447,
      "range": "زاگرس (دنا)",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 36,
      "name": "ابیدر",
      "english_name": "Abeedar",
      "altitude_meters": 4250,
      "latitude": 36.376,
      "longitude": 51.065,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 37,
      "name": "شکرلقاس",
      "english_name": "Shekarlughas",
      "altitude_meters": 4250,
      "latitude": 36.120,
      "longitude": 51.450,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 38,
      "name": "سیالان",
      "english_name": "Sialan",
      "altitude_meters": 4250,
      "latitude": 36.512,
      "longitude": 50.698,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 39,
      "name": "تخت رستم",
      "english_name": "Takht-e Rostam",
      "altitude_meters": 4246,
      "latitude": 36.420,
      "longitude": 50.956,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 40,
      "name": "کمان کوه",
      "english_name": "Kaman Kuh",
      "altitude_meters": 4234,
      "latitude": 36.129,
      "longitude": 51.470,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 41,
      "name": "پلوار",
      "english_name": "Palvar",
      "altitude_meters": 4233,
      "latitude": 30.070,
      "longitude": 57.466,
      "range": "ایران مرکزی (هزار)",
      "province": "کرمان"
    },
    {
      "id": 42,
      "name": "زرد گل",
      "english_name": "Zard Gel",
      "altitude_meters": 4231,
      "latitude": 36.360,
      "longitude": 50.975,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 43,
      "name": "کلونچین",
      "english_name": "Kolunchin",
      "altitude_meters": 4221,
      "latitude": 32.365,
      "longitude": 50.077,
      "range": "زاگرس (زردکوه)",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 44,
      "name": "زردکوه",
      "english_name": "Zard Kuh",
      "altitude_meters": 4221,
      "latitude": 32.350,
      "longitude": 50.100,
      "range": "زاگرس مرکزی",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 45,
      "name": "خرسرک",
      "english_name": "Khar-Sarak",
      "altitude_meters": 4220,
      "latitude": 36.040,
      "longitude": 51.200,
      "range": "البرز مرکزی",
      "province": "البرز"
    },
    {
      "id": 46,
      "name": "گرمابسر",
      "english_name": "Garmab Sar",
      "altitude_meters": 4260,
      "latitude": 36.300,
      "longitude": 51.800,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 47,
      "name": "فراخه نو",
      "english_name": "Farakheh-no",
      "altitude_meters": 4210,
      "latitude": 36.059,
      "longitude": 51.525,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 48,
      "name": "سرکچال",
      "english_name": "Sarak Chal",
      "altitude_meters": 4210,
      "latitude": 36.029,
      "longitude": 51.539,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 49,
      "name": "کپیری",
      "english_name": "Kapiri",
      "altitude_meters": 4210,
      "latitude": 30.928,
      "longitude": 51.460,
      "range": "زاگرس (دنا)",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 50,
      "name": "سی یونه زا",
      "english_name": "Si Yoneh Za",
      "altitude_meters": 4208,
      "latitude": 36.069,
      "longitude": 51.548,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 51,
      "name": "نرگس",
      "english_name": "Narges",
      "altitude_meters": 4206,
      "latitude": 36.099,
      "longitude": 51.585,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 52,
      "name": "سرخرسنگ",
      "english_name": "Sar Khar Sang",
      "altitude_meters": 4203,
      "latitude": 36.079,
      "longitude": 51.563,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 53,
      "name": "اسب چال",
      "english_name": "Asbi Chal",
      "altitude_meters": 4200,
      "latitude": 36.072,
      "longitude": 51.545,
      "range": "البرز (خلنو)",
      "province": "تهران"
    },
    {
      "id": 54,
      "name": "کل شیدا",
      "english_name": "Kale Sheyda",
      "altitude_meters": 4200,
      "latitude": 31.055,
      "longitude": 51.339,
      "range": "زاگرس (دنا)",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 55,
      "name": "شاه البرز",
      "english_name": "Shah Alborz",
      "altitude_meters": 4200,
      "latitude": 36.315,
      "longitude": 50.753,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 56,
      "name": "کلون بستک",
      "english_name": "Kolon Bastak",
      "altitude_meters": 4200,
      "latitude": 36.200,
      "longitude": 51.350,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 57,
      "name": "زرین کوه",
      "english_name": "Zarrin Kuh",
      "altitude_meters": 4200,
      "latitude": 36.400,
      "longitude": 50.890,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 58,
      "name": "خشچال",
      "english_name": "Khashchal",
      "altitude_meters": 4180,
      "latitude": 36.450,
      "longitude": 50.800,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 59,
      "name": "آبند",
      "english_name": "Aband",
      "altitude_meters": 4187,
      "latitude": 36.150,
      "longitude": 51.300,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 60,
      "name": "سه یالان (سیالان)",
      "english_name": "Se Yalan (Sialan)",
      "altitude_meters": 4175,
      "latitude": 36.500,
      "longitude": 50.700,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 61,
      "name": "اشترانکوه",
      "english_name": "Oshtoran Kuh",
      "altitude_meters": 4150,
      "latitude": 33.340,
      "longitude": 49.304,
      "range": "زاگرس مرکزی",
      "province": "لرستان"
    },
    {
      "id": 62,
      "name": "سنبران",
      "english_name": "Sanboran",
      "altitude_meters": 4150,
      "latitude": 33.340,
      "longitude": 49.304,
      "range": "زاگرس",
      "province": "لرستان"
    },
    {
      "id": 63,
      "name": "خرتوئک",
      "english_name": "Khartook",
      "altitude_meters": 4150,
      "latitude": 36.100,
      "longitude": 51.350,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 64,
      "name": "شاه شهیدان",
      "english_name": "Shah Shahidan",
      "altitude_meters": 4150,
      "latitude": 32.300,
      "longitude": 50.200,
      "range": "زاگرس",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 65,
      "name": "لوکوره",
      "english_name": "Lokureh",
      "altitude_meters": 4150,
      "latitude": 33.200,
      "longitude": 49.500,
      "range": "زاگرس مرکزی",
      "province": "لرستان"
    },
    {
      "id": 66,
      "name": "جوپار",
      "english_name": "Jupar",
      "altitude_meters": 4135,
      "latitude": 30.100,
      "longitude": 57.200,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 67,
      "name": "نارچو",
      "english_name": "Narcho",
      "altitude_meters": 4130,
      "latitude": 36.050,
      "longitude": 51.400,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 68,
      "name": "سه سنگ",
      "english_name": "Se Sang",
      "altitude_meters": 4115,
      "latitude": 36.080,
      "longitude": 51.380,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 69,
      "name": "گدارکج اندرکج",
      "english_name": "Godar Kaj Andar Kaj",
      "altitude_meters": 4110,
      "latitude": 30.200,
      "longitude": 57.100,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 70,
      "name": "کهار بزرگ",
      "english_name": "Kuhar Bozorg",
      "altitude_meters": 4108,
      "latitude": 36.400,
      "longitude": 50.600,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 71,
      "name": "گلچین",
      "english_name": "Golchin",
      "altitude_meters": 4093,
      "latitude": 30.000,
      "longitude": 57.000,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 72,
      "name": "کازینستان",
      "english_name": "Kazinestan",
      "altitude_meters": 4082,
      "latitude": 33.500,
      "longitude": 49.000,
      "range": "زاگرس مرکزی",
      "province": "لرستان"
    },
    {
      "id": 73,
      "name": "قره داغ",
      "english_name": "Ghareh Dagh",
      "altitude_meters": 4076,
      "latitude": 36.150,
      "longitude": 51.200,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 74,
      "name": "طالقان",
      "english_name": "Taleghan",
      "altitude_meters": 4056,
      "latitude": 36.200,
      "longitude": 50.900,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 75,
      "name": "شیرکوه",
      "english_name": "Shir Kuh",
      "altitude_meters": 4055,
      "latitude": 31.606,
      "longitude": 54.068,
      "range": "ایران مرکزی",
      "province": "یزد"
    },
    {
      "id": 76,
      "name": "تفتان",
      "english_name": "Taftan",
      "altitude_meters": 4050,
      "latitude": 28.600,
      "longitude": 61.133,
      "range": "کوه‌های مرکزی",
      "province": "سیستان و بلوچستان"
    },
    {
      "id": 77,
      "name": "حصارچال",
      "english_name": "Hesar Chal",
      "altitude_meters": 4050,
      "latitude": 36.100,
      "longitude": 51.400,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 78,
      "name": "کائون",
      "english_name": "Kaun",
      "altitude_meters": 4050,
      "latitude": 36.120,
      "longitude": 51.420,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 79,
      "name": "آلانه سر",
      "english_name": "Alaneh Sar",
      "altitude_meters": 4050,
      "latitude": 36.350,
      "longitude": 51.900,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 80,
      "name": "سرمشک",
      "english_name": "Sarmashk",
      "altitude_meters": 4048,
      "latitude": 29.800,
      "longitude": 57.000,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 81,
      "name": "شاهانکوه",
      "english_name": "Shahan Kuh",
      "altitude_meters": 4040,
      "latitude": 32.803,
      "longitude": 49.983,
      "range": "زاگرس",
      "province": "اصفهان"
    },
    {
      "id": 82,
      "name": "میشینه مرگ",
      "english_name": "Mishineh Marg",
      "altitude_meters": 4022,
      "latitude": 36.300,
      "longitude": 51.000,
      "range": "البرز",
      "province": "مازندران"
    },
    {
      "id": 83,
      "name": "کرما کوه",
      "english_name": "Kerma Kuh",
      "altitude_meters": 4020,
      "latitude": 36.350,
      "longitude": 50.950,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 84,
      "name": "پسند کوه",
      "english_name": "Pasand Kuh",
      "altitude_meters": 4000,
      "latitude": 36.400,
      "longitude": 50.900,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 85,
      "name": "سردان",
      "english_name": "Sardan",
      "altitude_meters": 4000,
      "latitude": 32.250,
      "longitude": 50.150,
      "range": "زاگرس",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 86,
      "name": "کوه میلی",
      "english_name": "Kuh-e Mili",
      "altitude_meters": 4000,
      "latitude": 32.400,
      "longitude": 50.050,
      "range": "زاگرس",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 87,
      "name": "پیت غار",
      "english_name": "Pit Ghar",
      "altitude_meters": 4000,
      "latitude": 36.050,
      "longitude": 51.450,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 88,
      "name": "دهلا (وروشت)",
      "english_name": "Varevasht / Dehla",
      "altitude_meters": 4025,
      "latitude": 36.250,
      "longitude": 51.900,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 89,
      "name": "جانستون",
      "english_name": "Johnston",
      "altitude_meters": 3950,
      "latitude": 35.950,
      "longitude": 51.550,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 90,
      "name": "لاسوزو",
      "english_name": "Lasuzu",
      "altitude_meters": 3988,
      "latitude": 32.220,
      "longitude": 50.180,
      "range": "زاگرس",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 91,
      "name": "سیاه لیز",
      "english_name": "Siah Liz",
      "altitude_meters": 3975,
      "latitude": 36.380,
      "longitude": 50.880,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 92,
      "name": "توچال",
      "english_name": "Tochal",
      "altitude_meters": 3960,
      "latitude": 35.88,
      "longitude": 51.42,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 93,
      "name": "شاهوار",
      "english_name": "Shahvar",
      "altitude_meters": 3945,
      "latitude": 36.576,
      "longitude": 54.763,
      "range": "البرز شرقی",
      "province": "سمنان"
    },
    {
      "id": 94,
      "name": "بل",
      "english_name": "Bol",
      "altitude_meters": 3945,
      "latitude": 30.777,
      "longitude": 52.750,
      "range": "زاگرس",
      "province": "فارس"
    },
    {
      "id": 95,
      "name": "مهرچال",
      "english_name": "Mehr Chal",
      "altitude_meters": 3920,
      "latitude": 35.950,
      "longitude": 51.500,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 96,
      "name": "گاوکشان",
      "english_name": "Gavkoshan",
      "altitude_meters": 3813,
      "latitude": 36.509,
      "longitude": 54.401,
      "range": "البرز شرقی (شاهکوه)",
      "province": "گلستان"
    },
    {
      "id": 97,
      "name": "سماموس",
      "english_name": "Somamous",
      "altitude_meters": 3721,
      "latitude": 36.839,
      "longitude": 50.384,
      "range": "البرز غربی",
      "province": "گیلان"
    },
    {
      "id": 98,
      "name": "کینو",
      "english_name": "Kino",
      "altitude_meters": 3761,
      "latitude": 32.569,
      "longitude": 49.563,
      "range": "زاگرس",
      "province": "خوزستان"
    },
    {
      "id": 99,
      "name": "کمال",
      "english_name": "Kamal",
      "altitude_meters": 3718,
      "latitude": 37.730,
      "longitude": 46.500,
      "range": "کوه‌های آذربایجان شرقی",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 100,
      "name": "بینالود",
      "english_name": "Binalud",
      "altitude_meters": 3211,
      "latitude": 36.433,
      "longitude": 58.933,
      "range": "بینالود",
      "province": "خراسان رضوی"
    },
    {
      "id": 101,
      "name": "سهند",
      "english_name": "Sahand",
      "altitude_meters": 3707,
      "latitude": 37.733,
      "longitude": 46.500,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 102,
      "name": "جام",
      "english_name": "Jam",
      "altitude_meters": 3650,
      "latitude": 37.800,
      "longitude": 46.500,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 103,
      "name": "اورین",
      "english_name": "Orin",
      "altitude_meters": 3633,
      "latitude": 38.554,
      "longitude": 44.574,
      "range": "کوه‌های آذربایجان غربی",
      "province": "آذربایجان غربی"
    },
    {
      "id": 104,
      "name": "یخچال",
      "english_name": "Yakhchal",
      "altitude_meters": 3580,
      "latitude": 34.740,
      "longitude": 48.460,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 105,
      "name": "الوند",
      "english_name": "Alvand",
      "altitude_meters": 3584,
      "latitude": 34.750,
      "longitude": 48.450,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 106,
      "name": "کوبری",
      "english_name": "Koubari",
      "altitude_meters": 3586,
      "latitude": 34.643,
      "longitude": 48.432,
      "range": "زاگرس شمالی (الوند)",
      "province": "همدان"
    },
    {
      "id": 107,
      "name": "شاه نشین",
      "english_name": "Shah Neshin",
      "altitude_meters": 3495,
      "latitude": 34.760,
      "longitude": 48.440,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 108,
      "name": "بخیال صاحب",
      "english_name": "Bakhial Saheb",
      "altitude_meters": 3486,
      "latitude": 34.755,
      "longitude": 48.445,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 109,
      "name": "اردک و مردک",
      "english_name": "Ardak o Mordak",
      "altitude_meters": 3486,
      "latitude": 34.730,
      "longitude": 48.470,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 110,
      "name": "دائم برف",
      "english_name": "Daem Barf",
      "altitude_meters": 3450,
      "latitude": 34.720,
      "longitude": 48.480,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 111,
      "name": "ناریک دره",
      "english_name": "Narik Darreh",
      "altitude_meters": 3414,
      "latitude": 34.740,
      "longitude": 48.460,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 112,
      "name": "کلاغ لانه",
      "english_name": "Kalagh Laneh",
      "altitude_meters": 3410,
      "latitude": 34.710,
      "longitude": 48.490,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 113,
      "name": "پرو",
      "english_name": "Paraw",
      "altitude_meters": 3405,
      "latitude": 34.800,
      "longitude": 47.000,
      "range": "زاگرس",
      "province": "کرمانشاه"
    },
    {
      "id": 114,
      "name": "شاهو",
      "english_name": "Shaho",
      "altitude_meters": 3390,
      "latitude": 35.000,
      "longitude": 46.500,
      "range": "زاگرس",
      "province": "کردستان"
    },
    {
      "id": 115,
      "name": "بلقیس",
      "english_name": "Bolqeys",
      "altitude_meters": 3363,
      "latitude": 36.669,
      "longitude": 47.298,
      "range": "زاگرس",
      "province": "زنجان"
    },
    {
      "id": 116,
      "name": "کمرلوزان",
      "english_name": "Kamar Lozan",
      "altitude_meters": 3348,
      "latitude": 34.750,
      "longitude": 48.450,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 117,
      "name": "کوه کیه‌یامکی داغ",
      "english_name": "Kiyamaki Dagh",
      "altitude_meters": 3350,
      "latitude": 39.000,
      "longitude": 45.000,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان غربی"
    },
    {
      "id": 118,
      "name": "شیرباد",
      "english_name": "Shirbad",
      "altitude_meters": 3327,
      "latitude": 36.280,
      "longitude": 59.049,
      "range": "کوه‌های خراسان رضوی",
      "province": "خراسان رضوی"
    },
    {
      "id": 119,
      "name": "بزقوش",
      "english_name": "Bozqush",
      "altitude_meters": 3303,
      "latitude": 37.500,
      "longitude": 47.000,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 120,
      "name": "قزل ارسلان",
      "english_name": "Ghezel Arsalan",
      "altitude_meters": 3250,
      "latitude": 34.700,
      "longitude": 48.500,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 121,
      "name": "سیاه‌کوه",
      "english_name": "Siah Kuh",
      "altitude_meters": 3250,
      "latitude": 33.000,
      "longitude": 51.000,
      "range": "زاگرس",
      "province": "اصفهان"
    },
    {
      "id": 122,
      "name": "جبل سیاه (سیرجان)",
      "english_name": "Jabal Siah (Sirjan)",
      "altitude_meters": 3250,
      "latitude": 29.500,
      "longitude": 55.500,
      "range": "ایران مرکزی",
      "province": "کرمان"
    },
    {
      "id": 123,
      "name": "برف‌انبار",
      "english_name": "Barf Anbar",
      "altitude_meters": 3224,
      "latitude": 34.231,
      "longitude": 50.902,
      "range": "مرکزی ایران",
      "province": "قم"
    },
    {
      "id": 124,
      "name": "زلیخا (چهل چشمه)",
      "english_name": "Zoleikha",
      "altitude_meters": 3197,
      "latitude": 35.818917,
      "longitude": 46.536433,
      "range": "زاگرس کردستان",
      "province": "کردستان"
    },
    {
      "id": 125,
      "name": "چهار قله",
      "english_name": "Chahar Gholeh",
      "altitude_meters": 3184,
      "latitude": 34.690,
      "longitude": 48.510,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 126,
      "name": "کلاه قاضی",
      "english_name": "Kolah Ghazi",
      "altitude_meters": 3125,
      "latitude": 34.680,
      "longitude": 48.520,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 127,
      "name": "شاه جهان",
      "english_name": "Shah Jahan",
      "altitude_meters": 3081,
      "latitude": 37.032,
      "longitude": 57.900,
      "range": "کوه‌های خراسان شمالی",
      "province": "خراسان شمالی"
    },
    {
      "id": 128,
      "name": "کان صیفی",
      "english_name": "Kan Seifi",
      "altitude_meters": 3050,
      "latitude": 33.393,
      "longitude": 46.781,
      "range": "زاگرس",
      "province": "ایلام"
    },
    {
      "id": 129,
      "name": "ناپیند",
      "english_name": "Napind",
      "altitude_meters": 3005,
      "latitude": 32.440,
      "longitude": 57.369,
      "range": "کوه‌های خراسان جنوبی",
      "province": "خراسان جنوبی"
    },
    {
      "id": 130,
      "name": "آلمابلاغ",
      "english_name": "Almabolagh",
      "altitude_meters": 2997,
      "latitude": 34.670,
      "longitude": 48.530,
      "range": "زاگرس (الوند)",
      "province": "همدان"
    },
    {
      "id": 131,
      "name": "کرکس",
      "english_name": "Karkas",
      "altitude_meters": 2995,
      "latitude": 33.950,
      "longitude": 51.450,
      "range": "ایران مرکزی",
      "province": "اصفهان"
    },
    {
      "id": 132,
      "name": "کوه گیشتاسار",
      "english_name": "Kuh-e Geyshtasar",
      "altitude_meters": 2800,
      "latitude": 27.000,
      "longitude": 55.000,
      "range": "زاگرس",
      "province": "هرمزگان"
    },
    {
      "id": 133,
      "name": "کبیرکوه",
      "english_name": "Kabir Kuh",
      "altitude_meters": 2790,
      "latitude": 33.000,
      "longitude": 47.000,
      "range": "زاگرس",
      "province": "ایلام"
    },
    {
      "id": 134,
      "name": "کوه بند نیلاق",
      "english_name": "Kuh-e Band-e Nilaq",
      "altitude_meters": 2600,
      "latitude": 32.000,
      "longitude": 49.000,
      "range": "زاگرس",
      "province": "خوزستان"
    },
    {
      "id": 135,
      "name": "کوه صفه",
      "english_name": "Kuh-e Soffeh",
      "altitude_meters": 2250,
      "latitude": 32.600,
      "longitude": 51.600,
      "range": "زاگرس",
      "province": "اصفهان"
    },
    {
      "id": 136,
      "name": "بیرمی",
      "english_name": "Birmi",
      "altitude_meters": 1960,
      "latitude": 28.720,
      "longitude": 51.467,
      "range": "زاگرس",
      "province": "بوشهر"
    },
    {
      "id": 137,
      "name": "عینالی",
      "english_name": "Eynali",
      "altitude_meters": 1800,
      "latitude": 38.100,
      "longitude": 46.300,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 138,
      "name": "قندیل",
      "english_name": "Ghandil",
      "altitude_meters": 3800,
      "latitude": 36.700,
      "longitude": 45.200,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 139,
      "name": "مرگ",
      "english_name": "Marg",
      "altitude_meters": 3800,
      "latitude": 36.750,
      "longitude": 45.100,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 140,
      "name": "دیک داغ",
      "english_name": "Dik Dagh",
      "altitude_meters": 3800,
      "latitude": 36.800,
      "longitude": 45.000,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 141,
      "name": "اوردین بزرگ",
      "english_name": "Ordin Bozorg",
      "altitude_meters": 3800,
      "latitude": 36.850,
      "longitude": 44.900,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 142,
      "name": "زیئال نابوتال",
      "english_name": "Zial Nabutal",
      "altitude_meters": 3800,
      "latitude": 36.900,
      "longitude": 44.800,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 143,
      "name": "ارت بای بون",
      "english_name": "Art Bay Bon",
      "altitude_meters": 3800,
      "latitude": 36.950,
      "longitude": 44.700,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 144,
      "name": "دالامیر",
      "english_name": "Dalamir",
      "altitude_meters": 3800,
      "latitude": 37.000,
      "longitude": 44.600,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 145,
      "name": "آتشکوه",
      "english_name": "Atash Kuh",
      "altitude_meters": 3850,
      "latitude": 36.000,
      "longitude": 51.500,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 146,
      "name": "گوزلوک گری کوسلکوک",
      "english_name": "Guzluk Geri Kuselkuk",
      "altitude_meters": 3800,
      "latitude": 36.800,
      "longitude": 45.200,
      "range": "زاگرس",
      "province": "پیرانشهر"
    },
    {
      "id": 147,
      "name": "کوه جبال بارز",
      "english_name": "Kuh-e Jebal Barez",
      "altitude_meters": 3800,
      "latitude": 28.500,
      "longitude": 57.500,
      "range": "کوه‌های مرکزی",
      "province": "کرمان"
    },
    {
      "id": 148,
      "name": "کوه بزمان",
      "english_name": "Kuh-e Bazman",
      "altitude_meters": 3490,
      "latitude": 27.850,
      "longitude": 60.200,
      "range": "کوه‌های مرکزی",
      "province": "سیستان و بلوچستان"
    },
    {
      "id": 149,
      "name": "چهل مر شهیدان",
      "english_name": "Chehel Mar Shahidan",
      "altitude_meters": 3850,
      "latitude": 32.400,
      "longitude": 50.200,
      "range": "زاگرس",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 150,
      "name": "تشگر",
      "english_name": "Tashgar",
      "altitude_meters": 3267,
      "latitude": 27.768,
      "longitude": 56.326,
      "range": "زاگرس جنوبی (هماگ)",
      "province": "هرمزگان"
    },
    {
      "id": 151,
      "name": "دومیر",
      "english_name": "Domir",
      "altitude_meters": 3505,
      "latitude": 33.911,
      "longitude": 51.132,
      "range": "اردهال (مرکزی)",
      "province": "مرکزی"
    },
    {
      "id": 152,
      "name": "قله آرارات",
      "english_name": "Ararat",
      "altitude_meters": 5137,
      "latitude": 39.700,
      "longitude": 44.300,
      "range": "کوه‌های آذربایجان",
      "province": "آذربایجان غربی"
    }
  ],
  international_peaks: [
    {
      "id": 1,
      "name": "اورست",
      "english_name": "Everest",
      "altitude_meters": 8848,
      "latitude": 27.9881,
      "longitude": 86.9250,
      "range": "هیمالیا",
      "province": "نپال / چین (تبت)"
    },
    {
      "id": 2,
      "name": "کی ۲",
      "english_name": "K2",
      "altitude_meters": 8611,
      "latitude": 35.8808,
      "longitude": 76.5133,
      "range": "قراقروم",
      "province": "پاکستان / چین"
    },
    {
      "id": 3,
      "name": "کانچنچونگا",
      "english_name": "Kangchenjunga",
      "altitude_meters": 8586,
      "latitude": 27.7025,
      "longitude": 88.1475,
      "range": "هیمالیا",
      "province": "نپال / هند"
    },
    {
      "id": 4,
      "name": "لوتسه",
      "english_name": "Lhotse",
      "altitude_meters": 8516,
      "latitude": 27.9617,
      "longitude": 86.9333,
      "range": "هیمالیا",
      "province": "نپال / چین (تبت)"
    },
    {
      "id": 5,
      "name": "ماکالو",
      "english_name": "Makalu",
      "altitude_meters": 8485,
      "latitude": 27.8892,
      "longitude": 87.0889,
      "range": "هیمالیا",
      "province": "نپال / چین (تبت)"
    },
    {
      "id": 6,
      "name": "چو اویو",
      "english_name": "Cho Oyu",
      "altitude_meters": 8188,
      "latitude": 28.0942,
      "longitude": 86.6608,
      "range": "هیمالیا",
      "province": "نپال / چین (تبت)"
    },
    {
      "id": 7,
      "name": "نانگاپاربات",
      "english_name": "Nanga Parbat",
      "altitude_meters": 8126,
      "latitude": 35.2375,
      "longitude": 74.5891,
      "range": "هیمالیا",
      "province": "پاکستان"
    },
    {
      "id": 8,
      "name": "ماناسلو",
      "english_name": "Manaslu",
      "altitude_meters": 8163,
      "latitude": 28.5497,
      "longitude": 84.5597,
      "range": "هیمالیا",
      "province": "نپال"
    },
    {
      "id": 9,
      "name": "آکانکاگوا",
      "english_name": "Aconcagua",
      "altitude_meters": 6961,
      "latitude": -32.6532,
      "longitude": -70.0108,
      "range": "آند",
      "province": "آرژانتین"
    },
    {
      "id": 10,
      "name": "دینالی (مک کینلی)",
      "english_name": "Denali",
      "altitude_meters": 6190,
      "latitude": 63.0692,
      "longitude": -151.0070,
      "range": "آلاسکا",
      "province": "ایالات متحده آمریکا"
    },
    {
      "id": 11,
      "name": "کلیمانجارو",
      "english_name": "Kilimanjaro",
      "altitude_meters": 5895,
      "latitude": -3.0674,
      "longitude": 37.3556,
      "range": "شرق آفریقا",
      "province": "تانزانیا"
    },
    {
      "id": 12,
      "name": "البروس",
      "english_name": "Elbrus",
      "altitude_meters": 5642,
      "latitude": 43.3499,
      "longitude": 42.4453,
      "range": "قفقاز",
      "province": "روسیه"
    },
    {
      "id": 13,
      "name": "آرارات",
      "english_name": "Ararat",
      "altitude_meters": 5137,
      "latitude": 39.7024,
      "longitude": 44.2991,
      "range": "آرارات",
      "province": "ترکیه"
    },
    {
      "id": 14,
      "name": "توده وینسون",
      "english_name": "Vinson Massif",
      "altitude_meters": 4892,
      "latitude": -78.5255,
      "longitude": -85.6171,
      "range": "سنتینل",
      "province": "قاره آنتارکتیکا (قطب جنوب)"
    },
    {
      "id": 15,
      "name": "پونچاک جایا (کارستنز)",
      "english_name": "Puncak Jaya",
      "altitude_meters": 4884,
      "latitude": -4.0844,
      "longitude": 137.1866,
      "range": "سودیرمان",
      "province": "اندونزی (اقیانوسیه)"
    },
    {
      "id": 16,
      "name": "مون بلان",
      "english_name": "Mont Blanc",
      "altitude_meters": 4808,
      "latitude": 45.8326,
      "longitude": 6.8652,
      "range": "آلپ",
      "province": "فرانسه / ایتالیا"
    },
    {
      "id": 17,
      "name": "کازبک",
      "english_name": "Kazbek",
      "altitude_meters": 5054,
      "latitude": 42.6972,
      "longitude": 44.5192,
      "range": "قفقاز",
      "province": "گرجستان"
    },
    {
      "id": 18,
      "name": "لنین (ابن سینا)",
      "english_name": "Lenin Peak",
      "altitude_meters": 7134,
      "latitude": 39.3464,
      "longitude": 72.8694,
      "range": "پامیر",
      "province": "قرقیزستان / تاجیکستان"
    },
    {
      "id": 19,
      "name": "ماترهورن",
      "english_name": "Matterhorn",
      "altitude_meters": 4478,
      "latitude": 45.9766,
      "longitude": 7.6585,
      "range": "آلپ",
      "province": "سوئیس / ایتالیا"
    },
    {
      "id": 20,
      "name": "خان تنگری",
      "english_name": "Khan Tengri",
      "altitude_meters": 7010,
      "latitude": 42.2106,
      "longitude": 80.2681,
      "range": "تیان شان",
      "province": "قزاقستان / قرقیزستان"
    }
  ],
  ski_resorts: [
    {
      "id": 1,
      "name": "توچال (ایستگاه ۷)",
      "english_name": "Tochal (Station 7)",
      "altitude_meters": 3575,
      "latitude": 35.8835,
      "longitude": 51.4215,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 2,
      "name": "دیزین",
      "english_name": "Dizin",
      "altitude_meters": 2650,
      "latitude": 36.0491,
      "longitude": 51.4172,
      "range": "البرز مرکزی",
      "province": "البرز"
    },
    {
      "id": 3,
      "name": "دربندسر",
      "english_name": "Darbandsar",
      "altitude_meters": 2650,
      "latitude": 36.0285,
      "longitude": 51.4428,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 4,
      "name": "شمشک",
      "english_name": "Shemshak",
      "altitude_meters": 2550,
      "latitude": 36.0087,
      "longitude": 51.4947,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 5,
      "name": "آبعلی",
      "english_name": "Abali",
      "altitude_meters": 2400,
      "latitude": 35.7511,
      "longitude": 51.9542,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 6,
      "name": "پولادکف",
      "english_name": "Pooladkaf",
      "altitude_meters": 2820,
      "latitude": 30.3421,
      "longitude": 51.9135,
      "range": "زاگرس جنوبی",
      "province": "فارس"
    },
    {
      "id": 7,
      "name": "آلوارس",
      "english_name": "Alvares",
      "altitude_meters": 3050,
      "latitude": 38.2045,
      "longitude": 47.9351,
      "range": "توده سبلان",
      "province": "اردبیل"
    },
    {
      "id": 8,
      "name": "فریدون‌شهر",
      "english_name": "Fereydunshahr",
      "altitude_meters": 2630,
      "latitude": 32.9312,
      "longitude": 50.0415,
      "range": "زاگرس مرکزی",
      "province": "اصفهان"
    },
    {
      "id": 9,
      "name": "سهند",
      "english_name": "Sahand",
      "altitude_meters": 2915,
      "latitude": 37.7495,
      "longitude": 46.5162,
      "range": "توده سهند",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 10,
      "name": "پیست شیرباد",
      "english_name": "Shirbad Ski Resort",
      "altitude_meters": 3000,
      "latitude": 36.3045,
      "longitude": 59.0512,
      "range": "بینالود",
      "province": "خراسان رضوی"
    },
    {
      "id": 11,
      "name": "چلگرد (کوهرنگ)",
      "english_name": "Chelgerd (Kuhrang)",
      "altitude_meters": 2350,
      "latitude": 32.4772,
      "longitude": 50.1135,
      "range": "زاگرس مرکزی",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 12,
      "name": "خوشاکو",
      "english_name": "Khoshako",
      "altitude_meters": 2000,
      "latitude": 37.4912,
      "longitude": 44.6851,
      "range": "مرزی زاگرس",
      "province": "آذربایجان غربی"
    },
    {
      "id": 13,
      "name": "کاکان (دنا)",
      "english_name": "Kakan (Dena)",
      "altitude_meters": 2640,
      "latitude": 30.6842,
      "longitude": 51.7215,
      "range": "زاگرس جنوبی",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 14,
      "name": "بیجار (نسار)",
      "english_name": "Bijar (Nesar)",
      "altitude_meters": 2000,
      "latitude": 35.8752,
      "longitude": 47.6185,
      "range": "زاگرس کردستان",
      "province": "کردستان"
    },
    {
      "id": 15,
      "name": "خور",
      "english_name": "Khor",
      "altitude_meters": 2400,
      "latitude": 35.9172,
      "longitude": 51.1541,
      "range": "البرز مرکزی",
      "province": "البرز"
    },
    {
      "id": 16,
      "name": "تاریک‌دره",
      "english_name": "Tarik Dareh",
      "altitude_meters": 2600,
      "latitude": 34.7412,
      "longitude": 48.4515,
      "range": "زاگرس شمالی (الوند)",
      "province": "همدان"
    },
    {
      "id": 17,
      "name": "پاپایی",
      "english_name": "Papayi",
      "altitude_meters": 2150,
      "latitude": 36.5684,
      "longitude": 48.3541,
      "range": "آذربایجان",
      "province": "زنجان"
    },
    {
      "id": 18,
      "name": "پیام مرند",
      "english_name": "Payam Marand",
      "altitude_meters": 1850,
      "latitude": 38.3312,
      "longitude": 45.7485,
      "range": "توده میشو",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 19,
      "name": "شازند (پاکل)",
      "english_name": "Shazand (Pakal)",
      "altitude_meters": 2450,
      "latitude": 33.8142,
      "longitude": 49.3785,
      "range": "زاگرس مرکزی",
      "province": "مرکزی"
    },
    {
      "id": 20,
      "name": "تمندر الیگودرز",
      "english_name": "Tamandar Aligudarz",
      "altitude_meters": 2600,
      "latitude": 33.2541,
      "longitude": 49.6582,
      "range": "زاگرس مرکزی",
      "province": "لرستان"
    },
    {
      "id": 21,
      "name": "سیکان",
      "english_name": "Sikan",
      "altitude_meters": 1900,
      "latitude": 33.1245,
      "longitude": 47.3812,
      "range": "کبیرکوه (زاگرس)",
      "province": "ایلام"
    },
    {
      "id": 22,
      "name": "کامفیروز",
      "english_name": "Kamfiruz",
      "altitude_meters": 2200,
      "latitude": 30.2241,
      "longitude": 52.1852,
      "range": "زاگرس جنوبی",
      "province": "فارس"
    },
    {
      "id": 23,
      "name": "سقز (روشن کوه)",
      "english_name": "Saqqez (Roshan Kooh)",
      "altitude_meters": 2100,
      "latitude": 36.1425,
      "longitude": 46.2214,
      "range": "زاگرس کردستان",
      "province": "کردستان"
    },
    {
      "id": 24,
      "name": "گرکان (اراک)",
      "english_name": "Gerkan",
      "altitude_meters": 2200,
      "latitude": 34.2851,
      "longitude": 49.5142,
      "range": "مرکزی ایران",
      "province": "مرکزی"
    }
  ]
};

// ============================================================
// بهینه‌سازی حافظه: محاسبه یکبار JSON و ETag
// ============================================================
const JSON_RESPONSE_BODY = JSON.stringify(MOUNTAINS_DB);
// ETag بر اساس نسخه کلی
const ETAG_VALUE = `W/"v${String(MOUNTAINS_DB.version)}"`;

// ============================================================
// مدیریت درخواست‌ها
// ============================================================
addEventListener("fetch", (event) => {
  event.respondWith(handleRequest(event.request));
});

// ============================================================
// تابع اصلی handleRequest با مدیریت خطا
// ============================================================
async function handleRequest(request) {
  try {
    const url = new URL(request.url);
    const method = request.method;

    // ---------- ۱. مدیریت درخواست OPTIONS (Preflight CORS) ----------
    if (method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Access-Control-Allow-Headers": "Origin, Accept, Content-Type, If-None-Match",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    // ---------- ۲. بررسی متد (فقط GET و HEAD مجاز است) ----------
    if (method !== "GET" && method !== "HEAD") {
      return new Response(JSON.stringify({ error: "Method Not Allowed" }), {
        status: 405,
        headers: {
          "Content-Type": "application/json;charset=UTF-8",
          "Access-Control-Allow-Origin": "*",
          "Allow": "GET, HEAD, OPTIONS",
        },
      });
    }

    // ---------- ۳. بررسی مسیر (پشتیبانی از / و /api/mountains) ----------
    const normalizedPath = url.pathname.replace(/\/+$/, "") || "/";
    if (normalizedPath !== "/" && normalizedPath !== "/api/mountains") {
      return new Response(JSON.stringify({ error: "Not Found" }), {
        status: 404,
        headers: {
          "Content-Type": "application/json;charset=UTF-8",
          "Access-Control-Allow-Origin": "*",
        },
      });
    }

    // ---------- ۴. بررسی ETag برای پاسخ ۳۰۴ Not Modified ----------
    const clientETag = request.headers.get("If-None-Match");
    if (clientETag === ETAG_VALUE) {
      return new Response(null, {
        status: 304,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Cache-Control": "public, max-age=3600, stale-while-revalidate=86400",
          ETag: ETAG_VALUE,
        },
      });
    }

    // ---------- ۵. پاسخ نهایی با کل دیتابیس ----------
    // نکته: Content-Length به‌صورت دستی تنظیم نمی‌شود. `String(JSON_RESPONSE_BODY.length)`
    // تعداد code point (UTF-16) است نه بایت UTF-8؛ برای داده‌ی فارسی این دو با هم
    // فرق دارند و اگر سرور مقدار اشتباه را به‌طور واقعی اعمال کند، پاسخ نیمه‌کاره
    // بریده می‌شود و اپلیکیشن خطای «آنالیز فایل دیتابیس جیسون» می‌دهد.
    // ران‌تایم Worker خودش Content-Length درست (بر حسب بایت) می‌گذارد.
    const responseBody = method === "HEAD" ? null : JSON_RESPONSE_BODY;

    return new Response(responseBody, {
      status: 200,
      headers: {
        "Content-Type": "application/json;charset=UTF-8",
        "Access-Control-Allow-Origin": "*",
        "Vary": "Accept-Encoding, Origin",
        "Cache-Control": "public, max-age=3600, s-maxage=86400, stale-while-revalidate=43200",
        ETag: ETAG_VALUE,
        "X-Content-Type-Options": "nosniff",
        "X-Frame-Options": "DENY",
        "Referrer-Policy": "strict-origin-when-cross-origin",
        "Permissions-Policy": "geolocation=(), microphone=(), camera=()",
      },
    });
  } catch (error) {
    // ---------- ۶. مدیریت خطاهای پیش‌بینی‌نشده ----------
    return new Response(
      JSON.stringify({
        error: "Internal Server Error",
        message: error.message,
        timestamp: new Date().toISOString(),
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json;charset=UTF-8",
          "Access-Control-Allow-Origin": "*",
        },
      }
    );
  }
}