// ============================================================
// دیتابیس جامع (قله‌های ایران + قله‌های بین‌المللی + پیست‌های اسکی)
// ============================================================
const MOUNTAINS_DB = {
  version: 3, // با هر تغییر در هر بخش، این عدد را +۱ کنید (v3: تفکیک نام پیست شیرباد از قله شیرباد)
  iran_peaks: [
    {
      "id": 1,
      "name": "دماوند",
      "english_name": "Damavand",
      "altitude_meters": 5610,
      "latitude": 35.9516,
      "longitude": 52.1102,
      "range": "البرز مرکزی",
      "province": "مازندران"
    },
    {
      "id": 2,
      "name": "علم کوه",
      "english_name": "Alam-Kuh",
      "altitude_meters": 4850,
      "latitude": 36.3756,
      "longitude": 50.9632,
      "range": "البرز (تخت سلیمان)",
      "province": "مازندران"
    },
    {
      "id": 3,
      "name": "سبلان",
      "english_name": "Sabalan",
      "altitude_meters": 4811,
      "latitude": 38.2662,
      "longitude": 47.8346,
      "range": "البرز غربی",
      "province": "اردبیل"
    },
    {
      "id": 4,
      "name": "هزار",
      "english_name": "Hezar",
      "altitude_meters": 4501,
      "latitude": 29.5134,
      "longitude": 57.2711,
      "range": "کوه‌های مرکزی",
      "province": "کرمان"
    },
    {
      "id": 5,
      "name": "خلنو",
      "english_name": "Kholeno",
      "altitude_meters": 4375,
      "latitude": 36.0125,
      "longitude": 51.5512,
      "range": "البرز مرکزی",
      "province": "تهران"
    },
    {
      "id": 6,
      "name": "دنا (قاش مستان)",
      "english_name": "Dena (Qash-Mastan)",
      "altitude_meters": 4459,
      "latitude": 30.9496,
      "longitude": 51.4243,
      "range": "زاگرس جنوبی",
      "province": "کهگیلویه و بویراحمد"
    },
    {
      "id": 7,
      "name": "زردکوه (کلونچین)",
      "english_name": "Zard-Kuh (Kolonchin)",
      "altitude_meters": 4221,
      "latitude": 32.3644,
      "longitude": 50.0784,
      "range": "زاگرس مرکزی",
      "province": "چهارمحال و بختیاری"
    },
    {
      "id": 8,
      "name": "سیالان",
      "english_name": "Sialan",
      "altitude_meters": 4185,
      "latitude": 36.5097,
      "longitude": 50.7814,
      "range": "البرز غربی",
      "province": "قزوین"
    },
    {
      "id": 9,
      "name": "اشترانکوه (سن بران)",
      "english_name": "Oshtorankuh (San-Boran)",
      "altitude_meters": 4150,
      "latitude": 33.3371,
      "longitude": 49.2785,
      "range": "زاگرس مرکزی",
      "province": "لرستان"
    },
    {
      "id": 10,
      "name": "شاه البرز",
      "english_name": "Shah Alborz",
      "altitude_meters": 4125,
      "latitude": 36.3146,
      "longitude": 50.7508,
      "range": "البرز غربی",
      "province": "البرز"
    },
    {
      "id": 11,
      "name": "شیرکوه",
      "english_name": "Shirkooh",
      "altitude_meters": 4075,
      "latitude": 31.6421,
      "longitude": 54.1755,
      "range": "کوه‌های مرکزی",
      "province": "یزد"
    },
    {
      "id": 12,
      "name": "شاهان کوه",
      "english_name": "Shahan Kuh",
      "altitude_meters": 4040,
      "latitude": 32.8125,
      "longitude": 49.9822,
      "range": "زاگرس مرکزی",
      "province": "اصفهان"
    },
    {
      "id": 13,
      "name": "شاهوار",
      "english_name": "Shahvar",
      "altitude_meters": 3945,
      "latitude": 36.5746,
      "longitude": 54.7612,
      "range": "البرز شرقی",
      "province": "سمنان"
    },
    {
      "id": 14,
      "name": "تفتان",
      "english_name": "Taftan",
      "altitude_meters": 3941,
      "latitude": 28.5997,
      "longitude": 61.1294,
      "range": "کوه‌های جنوب شرقی",
      "province": "سیستان و بلوچستان"
    },
    {
      "id": 15,
      "name": "بلوچ (بل)",
      "english_name": "Bel",
      "altitude_meters": 3943,
      "latitude": 31.1492,
      "longitude": 52.7486,
      "range": "زاگرس جنوبی",
      "province": "فارس"
    },
    {
      "id": 16,
      "name": "گاوکشان",
      "english_name": "Gavkoshan",
      "altitude_meters": 3813,
      "latitude": 36.5684,
      "longitude": 54.5421,
      "range": "البرز شرقی (شاهکوه)",
      "province": "گلستان"
    },
    {
      "id": 17,
      "name": "کینو",
      "english_name": "Kinu",
      "altitude_meters": 3745,
      "latitude": 32.4842,
      "longitude": 49.4894,
      "range": "زاگرس مرکزی",
      "province": "خوزستان"
    },
    {
      "id": 18,
      "name": "سماموس",
      "english_name": "Somamous",
      "altitude_meters": 3720,
      "latitude": 36.8392,
      "longitude": 50.3837,
      "range": "البرز غربی",
      "province": "گیلان"
    },
    {
      "id": 19,
      "name": "اورین",
      "english_name": "Avrin",
      "altitude_meters": 3702,
      "latitude": 38.5847,
      "longitude": 44.3642,
      "range": "مرزی زاگرس",
      "province": "آذربایجان غربی"
    },
    {
      "id": 20,
      "name": "کوبرى",
      "english_name": "Koubari",
      "altitude_meters": 3586,
      "latitude": 34.6432,
      "longitude": 48.4325,
      "range": "زاگرس شمالی (الوند)",
      "province": "همدان"
    },
    {
      "id": 21,
      "name": "کمال (سهند)",
      "english_name": "Kamal",
      "altitude_meters": 3707,
      "latitude": 37.7314,
      "longitude": 46.5111,
      "range": "توده سهند",
      "province": "آذربایجان شرقی"
    },
    {
      "id": 22,
      "name": "دومیر",
      "english_name": "Domir",
      "altitude_meters": 3505,
      "latitude": 33.9114,
      "longitude": 51.1328,
      "range": "اردهال (مرکزی)",
      "province": "مرکزی"
    },
    {
      "id": 23,
      "name": "پراو",
      "english_name": "Paraw",
      "altitude_meters": 3405,
      "latitude": 34.4024,
      "longitude": 47.2435,
      "range": "زاگرس بیستون",
      "province": "کرمانشاه"
    },
    {
      "id": 24,
      "name": "بلقیس",
      "english_name": "Belqeys",
      "altitude_meters": 3343,
      "latitude": 36.6084,
      "longitude": 47.3197,
      "range": "آذربایجان",
      "province": "زنجان"
    },
    {
      "id": 25,
      "name": "شیرباد",
      "english_name": "Shirbad",
      "altitude_meters": 3303,
      "latitude": 36.2828,
      "longitude": 59.0435,
      "range": "بینالود",
      "province": "خراسان رضوی"
    },
    {
      "id": 26,
      "name": "تشگر",
      "english_name": "Tashger",
      "altitude_meters": 3267,
      "latitude": 27.7686,
      "longitude": 56.3267,
      "range": "زاگرس جنوبی (هماگ)",
      "province": "هرمزگان"
    },
    {
      "id": 27,
      "name": "برف انبار",
      "english_name": "Barf Anbar",
      "altitude_meters": 3220,
      "latitude": 34.2312,
      "longitude": 50.9025,
      "range": "مرکزی ایران",
      "province": "قم"
    },
    {
      "id": 28,
      "name": "زلیخا (چهل چشمه)",
      "english_name": "Zoleikha",
      "altitude_meters": 3220,
      "latitude": 35.8012,
      "longitude": 46.5314,
      "range": "زاگرس کردستان",
      "province": "کردستان"
    },
    {
      "id": 29,
      "name": "شاه جهان",
      "english_name": "Shah Jahan",
      "altitude_meters": 3064,
      "latitude": 37.1008,
      "longitude": 57.8542,
      "range": "آلاداغ",
      "province": "خراسان شمالی"
    },
    {
      "id": 30,
      "name": "کان صیفی",
      "english_name": "Kan Seyfi",
      "altitude_meters": 3050,
      "latitude": 33.3421,
      "longitude": 46.5014,
      "range": "کبیرکوه (زاگرس)",
      "province": "ایلام"
    },
    {
      "id": 31,
      "name": "نایبند",
      "english_name": "Nayband",
      "altitude_meters": 3005,
      "latitude": 32.0684,
      "longitude": 57.1724,
      "range": "بلوک طبس",
      "province": "خراسان جنوبی"
    },
    {
   "id": 32,
   "name": "توچال",
   "english_name": "Tochal",
   "altitude_meters": 3964,
   "latitude": 35.884361,
   "longitude": 51.419939,
   "range": "البرز مرکزی",
   "province": "تهران"
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
    const responseBody = method === "HEAD" ? null : JSON_RESPONSE_BODY;

    return new Response(responseBody, {
      status: 200,
      headers: {
        "Content-Type": "application/json;charset=UTF-8",
        "Content-Length": method === "HEAD" ? "0" : String(JSON_RESPONSE_BODY.length),
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