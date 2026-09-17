/**
 * Cloudflare Worker for Iran Mountain Weather - Subscription & Promo Management
 * 
 * This worker manages subscription plans and promo card configurations in real-time.
 * It is fully compatible with both the updated unified response format (BillingConfigResponse)
 * and legacy map-based parsing in older client versions.
 */

addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request))
})

async function handleRequest(request) {
  const url = new URL(request.url)
  const path = url.pathname

  // CORS headers for secure, dynamic communication
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Accept, User-Agent',
    'Content-Type': 'application/json; charset=utf-8'
  }

  // Handle preflight OPTIONS request
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      status: 204,
      headers: corsHeaders
    })
  }

  // Routing for Billing and Promo Endpoint
  if (path === '/api/billing/plans') {
    // Dynamic Pricing and Promo Data Configuration
    const data = {
      // 1. Subscription Plans Configuration (rendered in BillingDialog)
      plans: {
        monthly: {
          productId: "monthly_gold_sub",
          title: "اشتراک ۱ ماهه عادی",
          subtitle: "تمدید ماهانه صعود طلایی",
          priceText: "۱۷۹،۰۰۰ ت",
          priceValue: 179000,
          discountBadge: null,
          badgeType: null,
          originalPriceText: null
        },
        seasonal: {
          productId: "seasonal_gold_sub",
          title: "اشتراک ۳ ماهه (فصلی)",
          subtitle: "مناسب برنامه‌های فصل • ۱۵۹،۶۶۷ ت / ماه",
          priceText: "۴۷۹،۰۰۰ ت",
          priceValue: 479000,
          discountBadge: "۱۰٪ تخفیف",
          badgeType: "red",
          originalPriceText: "۵۳۰،۰۰۰ ت" // Strike-through original price
        },
        annual: {
          productId: "annual_gold_sub",
          title: "اشتراک ۱ ساله ویژه",
          subtitle: "بهترین و اقتصادی‌ترین صعود • ۱۰۷،۵۰۰ ت / ماه",
          priceText: "۱،۲۹۰،۰۰۰ ت",
          priceValue: 1290000,
          discountBadge: "۲۹٪ تخفیف",
          badgeType: "red", // Support presets: red, green, blue, orange, purple, or direct Hex code like #E11D48
          originalPriceText: "۱،۸۰۶،۰۰۰ ت", // Strike-through original price
          isPopular: true // App renders a "محبوب‌ترین" gold badge on the popular plan
        }
      },
      // 2. Real-time Promo Banner Configuration (rendered in SettingsScreen)
      promo: {
        title: "ارتقاء به سطح طلایی صعود",
        subtitle: "دسترسی محدود (نسخه رایگان)",
        discountBadge: "تخفیف ویژه",
        description: "با خرید اشتراک طلایی صعود، امکانات رادار ریسک حاد قله (بهمن، رعد و برق و سرمازدگی)، ترازهای ارتفاعی پیشرفته و پیش‌بینی ۱۶ روزه را باز کنید.",
        buttonText: "ارتقاء به اشتراک طلایی از کافه‌بازار"
      }
    }

    return new Response(JSON.stringify(data, null, 2), {
      status: 200,
      headers: {
        ...corsHeaders,
        // قیمتها باید همیشه تازه باشند؛ اپ هم با ?t= کش‌شکن میخواند
        "Cache-Control": "no-cache, no-store, must-revalidate"
      }
    })
  }

  // Fallback for unknown endpoints
  return new Response(JSON.stringify({ error: "Not Found" }), {
    status: 404,
    headers: corsHeaders
  })
}
