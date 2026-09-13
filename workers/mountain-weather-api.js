// ============================================================
// Cloudflare Worker - Open-Meteo Mountain Safety Proxy (Ultimate Edition)
// کاملاً بهینه برای موبایل و وب‌سایت - با اصلاح Stale Cache-Control
// ============================================================

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Requested-With, Accept-Encoding, Accept",
  "Access-Control-Max-Age": "86400"
};

const SECURITY_HEADERS = {
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "X-Frame-Options": "DENY",
  "Content-Security-Policy": "default-src 'none'; frame-ancestors 'none'"
};

// TTL های تازگی (Freshness) برای تصمیم‌گیری در مورد تازه بودن داده
const FORECAST_FRESHNESS_TTL = 60; // 1 دقیقه برای پیش‌بینی زنده
const ARCHIVE_FRESHNESS_TTL = 3600; // 1 ساعت برای داده‌های تاریخی
const GEOCODING_FRESHNESS_TTL = 3600; // 1 ساعت برای جستجوی مکان

// TTL های ذخیره‌سازی (Storage) برای نگهداری طولانی‌مدت در کش Cloudflare (برای مواقع اضطراری)
const STORAGE_TTL_FORECAST = 86400; // ۱ روز
const STORAGE_TTL_ARCHIVE = 86400 * 7; // ۷ روز
const STORAGE_TTL_GEOCODING = 86400 * 7; // ۷ روز

const REQUEST_TIMEOUT_MS = 5000; // 5 ثانیه

const TARGET_DOMAINS = {
  forecast: "https://api.open-meteo.com",
  archive: "https://archive-api.open-meteo.com",
  geocoding: "https://geocoding-api.open-meteo.com"
};

/**
 * افزودن هدرهای CORS، امنیتی و هدرهای اضافی به پاسخ
 */
function addHeaders(response, extraHeaders = {}) {
  const newHeaders = new Headers(response.headers);
  Object.entries(CORS_HEADERS).forEach(([k, v]) => newHeaders.set(k, v));
  Object.entries(SECURITY_HEADERS).forEach(([k, v]) => newHeaders.set(k, v));
  Object.entries(extraHeaders).forEach(([k, v]) => newHeaders.set(k, v));
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: newHeaders
  });
}

/**
 * نرمال‌سازی URL برای ایجاد کلید کش یکنواخت:
 * - حذف پارامترهای مزاحم (مانند _ که برای کش نشکن استفاده می‌شود)
 * - حذف پارامترهای Tracking (برای وب‌سایت آینده)
 * - مرتب‌سازی پارامترها (حفظ مقادیر تکراری برای آرایه‌ها)
 */
function getNormalizedUrl(request) {
  const url = new URL(request.url);
  const params = new URLSearchParams(url.search);
  
  // حذف پارامترهای Cache Buster
  params.delete("_");
  
  // حذف پارامترهای Tracking (برای وب‌سایت آینده)
  const trackingParams = [
    "utm_source", 
    "utm_medium", 
    "utm_campaign", 
    "utm_content", 
    "utm_term", 
    "fbclid", 
    "gclid", 
    "msclkid"
  ];
  trackingParams.forEach(p => params.delete(p));
  
  // مرتب‌سازی پارامترها (به صورت استاندارد و حفظ مقادیر تکراری)
  params.sort();
  
  const searchString = params.toString();
  return new URL(url.pathname + (searchString ? "?" + searchString : ""), url.origin);
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    
    // پاسخ به درخواست OPTIONS (CORS preflight)
    if (request.method === "OPTIONS") {
      return new Response(null, { 
        status: 204, 
        headers: { ...CORS_HEADERS, ...SECURITY_HEADERS } 
      });
    }

    // فقط متدهای GET و HEAD مجاز هستند
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response(JSON.stringify({ error: true, reason: "Method not allowed" }), {
        status: 405,
        headers: { 
          "Content-Type": "application/json", 
          ...CORS_HEADERS, 
          ...SECURITY_HEADERS 
        }
      });
    }

    // تشخیص نوع درخواست و انتخاب دامنه و TTL مناسب
    let targetBaseUrl = TARGET_DOMAINS.forecast;
    let freshnessTTL = FORECAST_FRESHNESS_TTL;
    let storageTTL = STORAGE_TTL_FORECAST;
    const pathname = url.pathname;
    
    if (pathname.startsWith("/v1/archive")) {
      targetBaseUrl = TARGET_DOMAINS.archive;
      freshnessTTL = ARCHIVE_FRESHNESS_TTL;
      storageTTL = STORAGE_TTL_ARCHIVE;
    } else if (pathname.startsWith("/v1/search")) {
      targetBaseUrl = TARGET_DOMAINS.geocoding;
      freshnessTTL = GEOCODING_FRESHNESS_TTL;
      storageTTL = STORAGE_TTL_GEOCODING;
    }

    // ساخت URL مقصد (Open-Meteo)
    const targetUrl = new URL(pathname + url.search, targetBaseUrl);
    
    // ساخت کلید کش نرمال‌سازی شده
    const normalizedKeyUrl = getNormalizedUrl(request);
    const cacheKey = new Request(normalizedKeyUrl.toString(), { method: "GET" });
    const cache = caches.default;
    const isHead = request.method === "HEAD";

    // متغیر برای نگهداری پاسخ کش‌شده (در صورت وجود)
    let cachedResponse = null;

    try {
      // 1. بررسی کش و اعتبارسنجی تازگی (Freshness)
      cachedResponse = await cache.match(cacheKey);
      
      let isCacheFresh = false;
      if (cachedResponse) {
        // استفاده از X-Cached-At برای دقت بالا (نه هدر Date که ممکن است تغییر کند)
        const cachedAtHeader = cachedResponse.headers.get("X-Cached-At");
        if (cachedAtHeader) {
          const cacheTime = new Date(cachedAtHeader).getTime();
          const ageMs = Date.now() - cacheTime;
          if (ageMs < (freshnessTTL * 1000)) {
            isCacheFresh = true;
          }
        }
      }

      // اگر کش موجود و تازه است، همان را برگردان
      if (cachedResponse && isCacheFresh) {
        const statusHeader = isHead ? null : { "X-Cache-Status": "HIT - Edge" };
        const resp = isHead 
          ? new Response(null, { status: cachedResponse.status, headers: cachedResponse.headers }) 
          : cachedResponse;
        return addHeaders(resp, { 
          ...statusHeader, 
          "Cache-Control": `public, max-age=${freshnessTTL}` 
        });
      }

      // 2. دریافت از سرور اصلی (Open-Meteo)
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
      
      const fetchOptions = {
        method: "GET",
        headers: {
          "User-Agent": request.headers.get("User-Agent") || "Mountain-Weather-Secure-Proxy/1.0",
          // هدر Accept ثابت روی JSON برای جلوگیری از کش شدن فرمت‌های دیگر (مانند CSV)
          "Accept": "application/json",
          "Accept-Encoding": "gzip" // درخواست فشرده‌سازی از سرور اصلی
        },
        signal: controller.signal,
        redirect: "follow"
      };

      let response;
      try {
        response = await fetch(targetUrl.toString(), fetchOptions);
        clearTimeout(timeoutId);
      } catch (fetchErr) {
        clearTimeout(timeoutId);
        throw fetchErr;
      }

      // 3. مدیریت خطاهای سرور اصلی
      if (!response.ok) {
        // برای خطاهای ۵xx (مشکل سرور) و ۴۲۹ (Too Many Requests) 
        // در صورت وجود کش، از داده‌های کهنه به عنوان Fallback استفاده می‌کنیم.
        if ((response.status >= 500 || response.status === 429) && cachedResponse) {
          console.log(`Upstream error ${response.status}, serving stale cache for safety.`);
          const staleResp = isHead 
            ? new Response(null, { status: cachedResponse.status, headers: cachedResponse.headers }) 
            : cachedResponse;
          
          // استانداردسازی هدر Warning طبق RFC 7234
          const warnText = response.status === 429 ? "Too Many Requests" : "Upstream Error";
          return addHeaders(staleResp, { 
            "X-Cache-Status": "STALE - UPSTREAM ERROR",
            "Warning": `110 Cloudflare-Worker "${warnText}, serving stale data"`,
            // ✅ جلوگیری از کش شدن داده قدیمی در اپلیکیشن کاربر
            "Cache-Control": "no-cache, no-store, must-revalidate"
          });
        }
        // در غیر این صورت (سایر خطاهای ۴xx) خطا را به کاربر برگردان
        return addHeaders(response, { "X-Cache-Status": "ERROR - Upstream Failed" });
      }

      // 4. ذخیره در کش با TTL طولانی (برای مواقع اضطراری)
      const responseToCache = response.clone();
      responseToCache.headers.set("Cache-Control", `public, max-age=${storageTTL}`);
      // ثبت زمان دقیق برای محاسبات Freshness در آینده
      responseToCache.headers.set("X-Cached-At", new Date().toUTCString());
      // استفاده از Vary استاندارد برای مدیریت فشردهسازی توسط Edge
      responseToCache.headers.set("Vary", "Accept-Encoding");
      
      ctx.waitUntil(
        cache.put(cacheKey, responseToCache).catch(e => console.error("Cache write failed", e))
      );

      // 5. ساخت پاسخ نهایی برای کاربر
      const finalResp = isHead 
        ? new Response(null, { status: response.status, headers: response.headers }) 
        : response;
      
      return addHeaders(finalResp, {
        "X-Cache-Status": "MISS - Fresh Fetch",
        "X-Upstream-Source": targetBaseUrl,
        "Cache-Control": `public, max-age=${freshnessTTL}`,
        "Vary": "Accept-Encoding"
      });

    } catch (error) {
      // مدیریت قطعی اینترنت یا تایم‌اوت (نجات‌بخش در کوهستان!)
      if (cachedResponse) {
        const staleResp = isHead 
          ? new Response(null, { status: cachedResponse.status, headers: cachedResponse.headers }) 
          : cachedResponse;
        // استانداردسازی هدر Warning برای خطای شبکه
        return addHeaders(staleResp, { 
          "X-Cache-Status": "STALE - NETWORK ERROR FALLBACK",
          "Warning": `112 Cloudflare-Worker "Network error, serving stale data"`,
          // ✅ جلوگیری از کش شدن داده قدیمی در اپلیکیشن کاربر
          "Cache-Control": "no-cache, no-store, must-revalidate"
        });
      }

      // اگر کشی در دسترس نباشد، خطای 504 را با پیام مناسب برگردان
      const msg = error.name === "AbortError" ? "Gateway Timeout (5s limit)" : "Network Unreachable";
      return addHeaders(new Response(JSON.stringify({ error: true, reason: msg }), {
        status: 504,
        headers: { "Content-Type": "application/json" }
      }), { "X-Cache-Status": "ERROR - No Cache Available" });
    }
  }
};