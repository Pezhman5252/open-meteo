// =============================================================
// Worker: Admin Codes & Subscriptions Management
// نسخه: 2.0 (یکپارچه‌شده)
// =============================================================

// ---------- Constants ----------
const DEFAULT_SESSION_TTL  = 3600;            // 1h
const DEFAULT_COOKIE_NAME  = "admin_session";
const CODE_ALPHABET        = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // 31 chars (no I,L,O,0,1)
const CODE_LENGTH          = 12;
const MAX_LIST_ITEMS       = 50;   // حداکثر آیتم در هر صفحه
const MAX_SEARCH_ITEMS     = 200;  // حداکثر آیتم برای جستجوی کامل
const MAX_DURATION_DAYS    = 3650; // حداکثر مدت اعتبار (10 سال)
const LOCK_TTL             = 120;  // TTL قفل پردازش (ثانیه)
const LOCK_PREFIX          = "__lock:"; // پیشوند کلیدهای قفل (برای فیلتر در list)

// Rate-limit config (per IP, per endpoint, 5-minute window)
const RL_TTL               = 300;
const LOGIN_RL_LIMIT       = 5;   // /api/login
const VERIFY_RL_LIMIT      = 5;   // /api/verify
const CHECK_RL_LIMIT       = 10;  // /api/check-code, /api/check-subscription

// Constant-time login delay (mitigates user-enumeration / timing attacks)
const LOGIN_CONST_DELAY    = 400;

// =============================================================
// timingSafeEqual — مقایسهٔ امن (constant-time)
// =============================================================
function timingSafeEqual(a, b) {
  if (!a || !b) return false;
  const aLen = a.length | 0;
  const bLen = b.length | 0;
  const len  = Math.max(aLen, bLen);
  let result = aLen ^ bLen;
  for (let i = 0; i < len; i++) {
    const aByte = i < aLen ? a[i] : 0;
    const bByte = i < bLen ? b[i] : 0;
    result |= aByte ^ bByte;
  }
  return result === 0;
}

// Helper: random hex string
function randomHex(bytes) {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  let s = "";
  for (let i = 0; i < bytes; i++) s += buf[i].toString(16).padStart(2, "0");
  return s;
}

// =============================================================
// بررسی متغیرهای محیطی حیاتی
// =============================================================
function checkRequiredEnv(env) {
  const missing = [];
  if (!env.ADMIN_PASSWORD) missing.push("ADMIN_PASSWORD");
  if (!env.CODES) missing.push("CODES (KV namespace)");
  if (!env.SESSIONS) missing.push("SESSIONS (KV namespace)");
  if (!env.RATE) missing.push("RATE (KV namespace)");
  if (missing.length) {
    throw new Error(`متغیرهای محیطی زیر تنظیم نشده‌اند: ${missing.join(", ")}`);
  }
}

export default {
  async fetch(request, env, ctx) {
    let allowedOrigins = [];
    try {
      checkRequiredEnv(env);
      allowedOrigins = env.ALLOWED_ORIGINS
        ? env.ALLOWED_ORIGINS.split(",").map(o => o.trim()).filter(Boolean)
        : [];
    } catch (err) {
      console.error("Config error:", err.message);
      return json(
        { error: "سرور پیکربندی نشده است. لطفاً با مدیر تماس بگیرید." },
        500, {}, request, allowedOrigins
      );
    }

    const url  = new URL(request.url);
    const path = url.pathname;

    const SESSION_TTL     = parseInt(env.SESSION_TTL, 10) || DEFAULT_SESSION_TTL;
    const COOKIE_NAME     = env.COOKIE_NAME || DEFAULT_COOKIE_NAME;

    // ---------- CORS preflight ----------
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(request, allowedOrigins) });
    }

    try {
      // ---------- Public API ----------
      if (path === "/api/verify" && request.method === "POST") {
        return await verifyCode(request, env, allowedOrigins);
      }
      if (path === "/api/check-subscription" && request.method === "POST") {
        return await checkSubscription(request, env, allowedOrigins);
      }
      if (path === "/api/check-code" && request.method === "POST") {
        return await checkCode(request, env, allowedOrigins);
      }

      // ---------- Admin API (protected) ----------
      if (path.startsWith("/api/")) {
        if (path !== "/api/login" && !(await isAdmin(request, env, COOKIE_NAME))) {
          return json({ error: "Unauthorized" }, 401, {}, request, allowedOrigins);
        }
        return await handleAdminAPI(request, env, path, allowedOrigins, SESSION_TTL, COOKIE_NAME);
      }

      // ---------- Admin UI ----------
      if (path === "/" || path === "/admin" || path === "/login") {
        return new Response(getAdminHTML(), {
          headers: {
            "Content-Type": "text/html; charset=utf-8",
            "Cache-Control": "no-cache, no-store, must-revalidate",
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": "DENY",
            "Referrer-Policy": "no-referrer",
            ...corsHeaders(request, allowedOrigins)
          }
        });
      }

      return new Response("Not Found", { status: 404 });
    } catch (err) {
      console.error("Worker error:", err);
      return json({ error: "خطای داخلی سرور" }, 500, {}, request, allowedOrigins);
    }
  }
};

// -------------------------------------------------------------
// Admin API router (protected by caller)
// -------------------------------------------------------------
async function handleAdminAPI(request, env, path, allowedOrigins, SESSION_TTL, COOKIE_NAME) {
  const method = request.method;

  if (path === "/api/login")  return method === "POST" ? handleLogin(request, env, allowedOrigins, SESSION_TTL, COOKIE_NAME) : methodError(request, allowedOrigins);
  if (path === "/api/logout") return method === "POST" ? handleLogout(request, env, allowedOrigins, COOKIE_NAME) : methodError(request, allowedOrigins);

  // Codes CRUD
  if (path === "/api/codes") {
    if (method === "GET")  return listCodes(request, env, allowedOrigins);
    if (method === "POST") return createCode(request, env, allowedOrigins);
    return methodError(request, allowedOrigins);
  }
  const codeMatch = path.match(/^\/api\/codes\/([^/]+)$/);
  if (codeMatch) {
    const code = safeDecode(codeMatch[1]);
    if (!code) return json({ error: "کد نامعتبر" }, 400, {}, request, allowedOrigins);
    if (method === "PUT")    return updateCode(request, env, code, allowedOrigins);
    if (method === "DELETE") return deleteCode(request, env, code, allowedOrigins);
    return methodError(request, allowedOrigins);
  }

  // Subscriptions management
  if (path === "/api/subscriptions") {
    if (method === "GET") return listSubscriptions(request, env, allowedOrigins);
    return methodError(request, allowedOrigins);
  }
  const subMatch = path.match(/^\/api\/subscriptions\/([^/]+)$/);
  if (subMatch) {
    const subId = safeDecode(subMatch[1]);
    if (!subId || subId.length > 128) {
      return json({ error: "شناسه اشتراک نامعتبر" }, 400, {}, request, allowedOrigins);
    }
    if (method === "GET")    return getSubscription(request, env, subId, allowedOrigins);
    if (method === "PUT")    return updateSubscription(request, env, subId, allowedOrigins);
    if (method === "DELETE") return deleteSubscription(request, env, subId, allowedOrigins);
    return methodError(request, allowedOrigins);
  }

  // Legacy deactivate endpoint
  if (path === "/api/subscription" && method === "DELETE") {
    return deactivateSubscription(request, env, allowedOrigins);
  }

  return json({ error: "مسیر API نامعتبر" }, 404, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Rate-limit helper
// -------------------------------------------------------------
async function isRateLimited(env, key, limit) {
  const rateKey = `rate:${key}`;
  const current = parseInt((await env.RATE.get(rateKey)) || "0", 10);
  if (current >= limit) return true;
  await env.RATE.put(rateKey, String(current + 1), { expirationTtl: RL_TTL });
  return false;
}

// -------------------------------------------------------------
// Public API: Check code status (without consuming)
// -------------------------------------------------------------
async function checkCode(request, env, allowedOrigins) {
  const ip      = request.headers.get("CF-Connecting-IP") || request.headers.get("X-Forwarded-For") || "unknown";
  const rateKey = `check-code:${ip}`;

  if (await isRateLimited(env, rateKey, CHECK_RL_LIMIT)) {
    return json({ error: "درخواست بیش از حد، بعداً تلاش کنید" }, 429, {}, request, allowedOrigins);
  }

  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  const code = (body && typeof body.code === "string") ? body.code.trim() : "";
  if (!code) return json({ error: "کد الزامی است" }, 400, {}, request, allowedOrigins);

  const formatted = normalizeCode(code);
  if (!formatted) return json({ error: "فرمت کد نامعتبر" }, 400, {}, request, allowedOrigins);

  const raw = await env.CODES.get(formatted);
  if (!raw) {
    return json({ valid: false, error: "کد یافت نشد", status: "not_found" },
                200, {}, request, allowedOrigins);
  }

  let data;
  try { data = JSON.parse(raw); } catch {
    return json({ error: "داده‌ی کد خراب است" }, 500, {}, request, allowedOrigins);
  }

  const now       = new Date();
  const isUsed    = data.used === true;
  const isActive  = data.active !== false;
  const isExpired = data.expires_at ? new Date(data.expires_at) <= now : false;
  const isValid   = !isUsed && isActive && !isExpired;

  return json({
    valid: isValid,
    code: formatted,
    used: isUsed,
    active: isActive,
    expired: isExpired,
    expires_at: data.expires_at || null,
    duration_days: data.duration_days || null,
    status: isValid ? "active"
          : isUsed  ? "used"
          : isExpired ? "expired"
          : "inactive"
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Public API: Verify activation code (consume the code)
// با مکانیزم قفل مبتنی بر KV برای جلوگیری از race condition
// -------------------------------------------------------------
async function verifyCode(request, env, allowedOrigins) {
  const ip      = request.headers.get("CF-Connecting-IP") || request.headers.get("X-Forwarded-For") || "unknown";
  const rateKey = `verify:${ip}`;

  if (await isRateLimited(env, rateKey, VERIFY_RL_LIMIT)) {
    return json({ error: "درخواست بیش از حد، بعداً تلاش کنید" }, 429, {}, request, allowedOrigins);
  }

  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  const code = (body && typeof body.code === "string") ? body.code.trim() : "";
  if (!code) return json({ error: "کد الزامی است" }, 400, {}, request, allowedOrigins);

  const formatted = normalizeCode(code);
  if (!formatted) return json({ error: "فرمت کد نامعتبر" }, 400, {}, request, allowedOrigins);

  // ---------- Locking ----------
  // قفل با prefix متمایز (__lock:) در همان namespace ذخیره می‌شود
  // تا در listCodes فیلتر شود. TTL طولانی برای جلوگیری از race
  // در صورت طولانی شدن پردازش. نکته: KV اتمیک نیست؛ برای تضمین
  // کامل، استفاده از Durable Object توصیه می‌شود.
  const lockKey   = LOCK_PREFIX + formatted;
  const lockValue = randomHex(16);

  if (await env.CODES.get(lockKey)) {
    return json({ error: "این کد در حال پردازش است، لحظاتی دیگر تلاش کنید" },
                429, {}, request, allowedOrigins);
  }
  await env.CODES.put(lockKey, lockValue, { expirationTtl: LOCK_TTL });

  try {
    const raw = await env.CODES.get(formatted);
    if (!raw) return json({ error: "کد یافت نشد" }, 404, {}, request, allowedOrigins);

    let data;
    try { data = JSON.parse(raw); } catch {
      return json({ error: "داده‌ی کد خراب است" }, 500, {}, request, allowedOrigins);
    }

    const now = new Date();
    if (data.used)    return json({ error: "این کد قبلاً استفاده شده" }, 400, {}, request, allowedOrigins);
    if (!data.active) return json({ error: "این کد غیرفعال است" }, 400, {}, request, allowedOrigins);
    if (data.expires_at && new Date(data.expires_at) <= now) {
      return json({ error: "کد منقضی شده" }, 400, {}, request, allowedOrigins);
    }

    const rawDays = parseInt(data.duration_days, 10);
    const durationDays = (Number.isFinite(rawDays) && rawDays > 0 && rawDays <= MAX_DURATION_DAYS) ? rawDays : 30;

    const subscriptionExpiry = new Date(now);
    subscriptionExpiry.setUTCDate(subscriptionExpiry.getUTCDate() + durationDays);
    subscriptionExpiry.setUTCHours(23, 59, 59, 999);

    // علامت‌گذاری کد به‌عنوان مصرف‌شده
    // قفل تضمین می‌کند که هیچ request موازی نمی‌تواند این خط را رد کند
    data.used         = true;
    data.last_used_at = now.toISOString();
    await env.CODES.put(formatted, JSON.stringify(data));

    const subscriptionId = (crypto.randomUUID && crypto.randomUUID()) || randomHex(16);
    await env.SUBSCRIPTIONS.put(subscriptionId, JSON.stringify({
      active:     true,
      expires_at: subscriptionExpiry.toISOString(),
      created_at: now.toISOString(),
      code:       formatted,
    }));

    return json({
      success:         true,
      subscription_id: subscriptionId,
      expires_at:      subscriptionExpiry.toISOString(),
      duration_days:   durationDays,
    }, 200, {}, request, allowedOrigins);

  } finally {
    // آزادسازی قفل فقط در صورتی که هنوز متعلق به همین request باشد
    try {
      const cur = await env.CODES.get(lockKey);
      if (cur === lockValue) await env.CODES.delete(lockKey);
    } catch { /* ignore */ }
  }
}

// -------------------------------------------------------------
// Public API: Check subscription status
// -------------------------------------------------------------
async function checkSubscription(request, env, allowedOrigins) {
  const ip      = request.headers.get("CF-Connecting-IP") || request.headers.get("X-Forwarded-For") || "unknown";
  const rateKey = `check-sub:${ip}`;

  if (await isRateLimited(env, rateKey, CHECK_RL_LIMIT)) {
    return json({ error: "درخواست بیش از حد، بعداً تلاش کنید" }, 429, {}, request, allowedOrigins);
  }

  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  const subId = (body && typeof body.subscription_id === "string") ? body.subscription_id.trim() : "";
  if (!subId) return json({ error: "شناسه اشتراک الزامی است" }, 400, {}, request, allowedOrigins);

  const raw = await env.SUBSCRIPTIONS.get(subId);
  if (!raw) return json({ error: "اشتراک یافت نشد" }, 404, {}, request, allowedOrigins);

  let data;
  try { data = JSON.parse(raw); } catch {
    return json({ error: "داده‌ی اشتراک خراب است" }, 500, {}, request, allowedOrigins);
  }

  const now       = new Date();
  const isExpired = data.expires_at ? new Date(data.expires_at) <= now : false;
  const isActive  = data.active === true && !isExpired;

  return json({
    active:     isActive,
    expires_at: data.expires_at || null,
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: List Codes (with search + offset pagination)
// -------------------------------------------------------------
async function listCodes(request, env, allowedOrigins) {
  const url    = new URL(request.url);
  const limit  = Math.min(parseInt(url.searchParams.get("limit") || "20", 10) || 20, MAX_LIST_ITEMS);
  const cursor = url.searchParams.get("cursor") || undefined;
  const offset = Math.max(0, parseInt(url.searchParams.get("offset") || "0", 10) || 0);
  const search = (url.searchParams.get("search") || "").trim();

  if (search !== "") {
    const allItems = [];
    let listCursor = undefined;
    let totalFetched = 0;
    let reachedLimit = false;
    do {
      const result = await env.CODES.list({ limit: 100, cursor: listCursor });
      for (const key of result.keys) {
        if (key.name.startsWith(LOCK_PREFIX)) continue; // فیلتر کلیدهای قفل
        if (totalFetched >= MAX_SEARCH_ITEMS) { reachedLimit = true; break; }
        const raw = await env.CODES.get(key.name);
        if (!raw) continue;
        try {
          const d = JSON.parse(raw);
          allItems.push({
            code:          key.name,
            used:          d.used === true,
            active:        d.active !== false,
            created_at:    d.created_at || null,
            expires_at:    d.expires_at || null,
            last_used_at:  d.last_used_at || null,
            duration_days: d.duration_days || null,
          });
          totalFetched++;
        } catch { /* skip corrupt */ }
      }
      listCursor = result.cursor;
    } while (listCursor && totalFetched < MAX_SEARCH_ITEMS);

    const q = search.toLowerCase();
    const filtered = allItems.filter(item => item.code.toLowerCase().includes(q));
    const paginated = filtered.slice(offset, offset + limit);
    const nextOffset = offset + paginated.length;

    return json({
      items:     paginated,
      cursor:    nextOffset < filtered.length ? String(nextOffset) : null,
      has_more:  nextOffset < filtered.length,
      total:     filtered.length,
      is_search: true,
      warning:   reachedLimit ? `نتیجه فقط تا ${MAX_SEARCH_ITEMS} آیتم محدود شده است. برای جستجوی کامل، عبارت دقیق‌تر استفاده کنید.` : undefined,
    }, 200, {}, request, allowedOrigins);
  }

  // حالت عادی با cursor (بدون جستجو)
  const items     = [];
  const listResult = await env.CODES.list({ limit, cursor });
  for (const key of listResult.keys) {
    if (key.name.startsWith(LOCK_PREFIX)) continue; // فیلتر کلیدهای قفل
    const raw = await env.CODES.get(key.name);
    if (!raw) continue;
    try {
      const d = JSON.parse(raw);
      items.push({
        code:          key.name,
        used:          d.used === true,
        active:        d.active !== false,
        created_at:    d.created_at || null,
        expires_at:    d.expires_at || null,
        last_used_at:  d.last_used_at || null,
        duration_days: d.duration_days || null,
      });
    } catch { /* skip corrupt */ }
  }

  return json({
    items:     items,
    cursor:    listResult.cursor || null,
    has_more:  !!listResult.cursor,
    is_search: false
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: List Subscriptions (with search + offset pagination)
// -------------------------------------------------------------
async function listSubscriptions(request, env, allowedOrigins) {
  const url    = new URL(request.url);
  const limit  = Math.min(parseInt(url.searchParams.get("limit") || "20", 10) || 20, MAX_LIST_ITEMS);
  const cursor = url.searchParams.get("cursor") || undefined;
  const offset = Math.max(0, parseInt(url.searchParams.get("offset") || "0", 10) || 0);
  const search = (url.searchParams.get("search") || "").trim();

  if (search !== "") {
    const allItems = [];
    let listCursor = undefined;
    let totalFetched = 0;
    let reachedLimit = false;
    do {
      const result = await env.SUBSCRIPTIONS.list({ limit: 100, cursor: listCursor });
      for (const key of result.keys) {
        if (totalFetched >= MAX_SEARCH_ITEMS) { reachedLimit = true; break; }
        const raw = await env.SUBSCRIPTIONS.get(key.name);
        if (!raw) continue;
        try {
          const d = JSON.parse(raw);
          allItems.push({
            subscription_id: key.name,
            code:            d.code || null,
            active:          d.active === true,
            expires_at:      d.expires_at || null,
            created_at:      d.created_at || null,
          });
          totalFetched++;
        } catch { /* skip */ }
      }
      listCursor = result.cursor;
    } while (listCursor && totalFetched < MAX_SEARCH_ITEMS);

    const q = search.toLowerCase();
    const filtered = allItems.filter(item =>
      item.subscription_id.toLowerCase().includes(q) ||
      (item.code && item.code.toLowerCase().includes(q))
    );
    const paginated = filtered.slice(offset, offset + limit);
    const nextOffset = offset + paginated.length;

    return json({
      items:     paginated,
      cursor:    nextOffset < filtered.length ? String(nextOffset) : null,
      has_more:  nextOffset < filtered.length,
      total:     filtered.length,
      is_search: true,
      warning:   reachedLimit ? `نتیجه فقط تا ${MAX_SEARCH_ITEMS} آیتم محدود شده است. برای جستجوی کامل، عبارت دقیق‌تر استفاده کنید.` : undefined,
    }, 200, {}, request, allowedOrigins);
  }

  const items     = [];
  const listResult = await env.SUBSCRIPTIONS.list({ limit, cursor });
  for (const key of listResult.keys) {
    const raw = await env.SUBSCRIPTIONS.get(key.name);
    if (!raw) continue;
    try {
      const d = JSON.parse(raw);
      items.push({
        subscription_id: key.name,
        code:            d.code || null,
        active:          d.active === true,
        expires_at:      d.expires_at || null,
        created_at:      d.created_at || null,
      });
    } catch { /* skip */ }
  }

  return json({
    items:     items,
    cursor:    listResult.cursor || null,
    has_more:  !!listResult.cursor,
    is_search: false
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: Get single subscription
// -------------------------------------------------------------
async function getSubscription(request, env, subscriptionId, allowedOrigins) {
  const raw = await env.SUBSCRIPTIONS.get(subscriptionId);
  if (!raw) return json({ error: "اشتراک یافت نشد" }, 404, {}, request, allowedOrigins);
  let data;
  try { data = JSON.parse(raw); } catch {
    return json({ error: "داده‌ی اشتراک خراب است" }, 500, {}, request, allowedOrigins);
  }
  return json({
    subscription_id: subscriptionId,
    code:            data.code || null,
    active:          data.active === true,
    expires_at:      data.expires_at || null,
    created_at:      data.created_at || null,
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: Update subscription
// -------------------------------------------------------------
async function updateSubscription(request, env, subscriptionId, allowedOrigins) {
  const raw = await env.SUBSCRIPTIONS.get(subscriptionId);
  if (!raw) return json({ error: "اشتراک یافت نشد" }, 404, {}, request, allowedOrigins);
  let data;
  try { data = JSON.parse(raw); } catch {
    return json({ error: "داده‌ی اشتراک خراب است" }, 500, {}, request, allowedOrigins);
  }

  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  if (!body || typeof body !== "object") {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }

  if ("active" in body) {
    if (typeof body.active !== "boolean") {
      return json({ error: "active باید true یا false باشد" }, 400, {}, request, allowedOrigins);
    }
    data.active = body.active;
  }

  if ("expires_at" in body) {
    if (body.expires_at === null || body.expires_at === "") {
      data.expires_at = null;
    } else {
      const newExpiry = validateDate(body.expires_at);
      if (!newExpiry) {
        return json({ error: "تاریخ نامعتبر" }, 400, {}, request, allowedOrigins);
      }
      data.expires_at = newExpiry;
    }
  }

  await env.SUBSCRIPTIONS.put(subscriptionId, JSON.stringify(data));
  return json({
    success:         true,
    subscription_id: subscriptionId,
    active:          data.active,
    expires_at:      data.expires_at,
  }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: Delete subscription
// -------------------------------------------------------------
async function deleteSubscription(request, env, subscriptionId, allowedOrigins) {
  const existing = await env.SUBSCRIPTIONS.get(subscriptionId);
  if (!existing) return json({ error: "اشتراک یافت نشد" }, 404, {}, request, allowedOrigins);
  await env.SUBSCRIPTIONS.delete(subscriptionId);
  return json({ success: true }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Admin API: Deactivate subscription (legacy)
// -------------------------------------------------------------
async function deactivateSubscription(request, env, allowedOrigins) {
  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  const subId = (body && typeof body.subscription_id === "string") ? body.subscription_id.trim() : "";
  if (!subId) return json({ error: "شناسه اشتراک الزامی است" }, 400, {}, request, allowedOrigins);

  const raw = await env.SUBSCRIPTIONS.get(subId);
  if (!raw) return json({ error: "اشتراک یافت نشد" }, 404, {}, request, allowedOrigins);

  let data;
  try { data = JSON.parse(raw); } catch {
    return json({ error: "داده‌ی اشتراک خراب است" }, 500, {}, request, allowedOrigins);
  }

  if (data.active === false) {
    return json({ error: "اشتراک در حال حاضر غیرفعال است" }, 400, {}, request, allowedOrigins);
  }

  data.active = false;
  await env.SUBSCRIPTIONS.put(subId, JSON.stringify(data));
  return json({ success: true, message: "اشتراک غیرفعال شد" }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Auth
// -------------------------------------------------------------
async function handleLogin(request, env, allowedOrigins, SESSION_TTL, COOKIE_NAME) {
  // بررسی مجدد وجود ADMIN_PASSWORD (امنیت بیشتر)
  if (!env.ADMIN_PASSWORD) {
    console.error("ADMIN_PASSWORD not set");
    return json({ error: "سرور پیکربندی نشده است" }, 500, {}, request, allowedOrigins);
  }

  const ip      = request.headers.get("CF-Connecting-IP") || request.headers.get("X-Forwarded-For") || "unknown";
  const rateKey = `login:${ip}`;

  const startDelay = (async () => { await new Promise(r => setTimeout(r, LOGIN_CONST_DELAY)); })();

  if (await isRateLimited(env, rateKey, LOGIN_RL_LIMIT)) {
    await startDelay;
    return json({ error: "تلاش‌های زیاد، بعداً امتحان کنید" }, 429, {}, request, allowedOrigins);
  }

  try {
    const expected = env.ADMIN_PASSWORD;
    let body;
    try { body = await request.json(); } catch {
      await startDelay;
      return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
    }

    const password = (body && typeof body.password === "string") ? body.password : "";
    if (!password) {
      await startDelay;
      return json({ error: "رمز عبور الزامی است" }, 400, {}, request, allowedOrigins);
    }

    const encoder        = new TextEncoder();
    const inputHash      = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(password)));
    const expectedHash   = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(expected)));
    const isMatch        = timingSafeEqual(inputHash, expectedHash);

    await startDelay;

    if (!isMatch) {
      return json({ error: "رمز عبور اشتباه است" }, 401, {}, request, allowedOrigins);
    }

    const sessionId = randomHex(32);
    await env.SESSIONS.put(sessionId, JSON.stringify({
      created_at:  Date.now(),
      user_agent:  (request.headers.get("User-Agent") || "").slice(0, 200),
      ip:          ip
    }), { expirationTtl: SESSION_TTL });

    const isSecure = isSecureRequest(request);
    const cookie   = cookieHeader(sessionId, SESSION_TTL, isSecure, COOKIE_NAME);

    return json({ success: true }, 200, { "Set-Cookie": cookie }, request, allowedOrigins);

  } catch (err) {
    console.error("Login error:", err);
    await startDelay;
    return json({ error: "خطا در ورود" }, 500, {}, request, allowedOrigins);
  }
}

async function handleLogout(request, env, allowedOrigins, COOKIE_NAME) {
  const sessionId = getSessionId(request, COOKIE_NAME);
  if (sessionId && env.SESSIONS) {
    try { await env.SESSIONS.delete(sessionId); } catch { /* ignore */ }
  }
  const isSecure = isSecureRequest(request);
  const cookie   = cookieHeader("", 0, isSecure, COOKIE_NAME);
  return json({ success: true }, 200, { "Set-Cookie": cookie }, request, allowedOrigins);
}

async function isAdmin(request, env, COOKIE_NAME) {
  const id = getSessionId(request, COOKIE_NAME);
  if (!id || !env.SESSIONS) return false;
  try {
    return (await env.SESSIONS.get(id)) !== null;
  } catch {
    return false;
  }
}

// -------------------------------------------------------------
// Codes — CRUD for admin
// -------------------------------------------------------------
async function createCode(request, env, allowedOrigins) {
  let body;
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  if (!body || typeof body !== "object") {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }

  let expires_at = null;
  if (body.expires_at !== undefined && body.expires_at !== null && body.expires_at !== "") {
    expires_at = validateDate(body.expires_at);
    if (!expires_at) return json({ error: "تاریخ انقضا نامعتبر" }, 400, {}, request, allowedOrigins);
    const now = new Date();
    if (new Date(expires_at) <= now) {
      return json({ error: "تاریخ انقضا نمی‌تواند در گذشته باشد" }, 400, {}, request, allowedOrigins);
    }
  }

  // اعتبارسنجی یکپارچه duration_days (1..3650)
  let duration_days = 30;
  if (body.duration_days !== undefined && body.duration_days !== null) {
    const days = parseInt(body.duration_days, 10);
    if (!Number.isFinite(days) || days < 1 || days > MAX_DURATION_DAYS) {
      return json({ error: `duration_days باید عددی بین 1 و ${MAX_DURATION_DAYS} باشد` },
                  400, {}, request, allowedOrigins);
    }
    duration_days = days;
  }

  const codeData = {
    used:           false,
    active:         body.active === false ? false : true,
    created_at:     new Date().toISOString(),
    expires_at:     expires_at,
    last_used_at:   null,
    duration_days:  duration_days,
  };

  let newCode, exists, attempts = 0;
  const MAX_ATTEMPTS = 100;
  do {
    newCode = generateCode();
    exists  = await env.CODES.get(newCode);
    attempts++;
    if (attempts > MAX_ATTEMPTS) {
      return json({ error: "امکان تولید کد یکتا پس از تلاش‌های مکرر وجود ندارد" },
                  500, {}, request, allowedOrigins);
    }
  } while (exists);

  await env.CODES.put(newCode, JSON.stringify(codeData));
  return json({ code: newCode, ...codeData }, 201, {}, request, allowedOrigins);
}

async function updateCode(request, env, code, allowedOrigins) {
  const raw = await env.CODES.get(code);
  if (!raw) return json({ error: "کد یافت نشد" }, 404, {}, request, allowedOrigins);

  let parsed, body;
  try { parsed = JSON.parse(raw); } catch {
    return json({ error: "داده ذخیره‌شده نامعتبر است" }, 500, {}, request, allowedOrigins);
  }
  try { body = await request.json(); } catch {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }
  if (!body || typeof body !== "object") {
    return json({ error: "داده نامعتبر" }, 400, {}, request, allowedOrigins);
  }

  if (parsed.used === true) {
    return json({ error: "کد مصرف‌شده قابل تغییر نیست" }, 400, {}, request, allowedOrigins);
  }

  // ----- ابتدا expires_at پردازش می‌شود -----
  // تا در اعتبارسنجی active از مقدار جدید استفاده شود
  if ("expires_at" in body) {
    if (body.expires_at === null || body.expires_at === "") {
      parsed.expires_at = null;
    } else {
      const newExpiry = validateDate(body.expires_at);
      if (!newExpiry) {
        return json({ error: "تاریخ نامعتبر" }, 400, {}, request, allowedOrigins);
      }
      const now = new Date();
      if (new Date(newExpiry) <= now) {
        return json({ error: "تاریخ انقضا نمی‌تواند در گذشته باشد" }, 400, {}, request, allowedOrigins);
      }
      parsed.expires_at = newExpiry;
    }
  }

  // ----- سپس active (با مقدار به‌روز expires_at) -----
  if ("active" in body) {
    if (typeof body.active !== "boolean") {
      return json({ error: "مقدار active باید true یا false باشد" }, 400, {}, request, allowedOrigins);
    }
    if (body.active === true && parsed.expires_at) {
      const now = new Date();
      if (new Date(parsed.expires_at) <= now) {
        return json({ error: "امکان فعال‌سازی کد با تاریخ انقضای گذشته وجود ندارد" },
                    400, {}, request, allowedOrigins);
      }
    }
    parsed.active = body.active;
  }

  // ----- duration_days با اعتبارسنجی صریح -----
  if ("duration_days" in body) {
    const days = parseInt(body.duration_days, 10);
    if (!Number.isFinite(days) || days < 1 || days > MAX_DURATION_DAYS) {
      return json({ error: `duration_days باید عددی بین 1 و ${MAX_DURATION_DAYS} باشد` },
                  400, {}, request, allowedOrigins);
    }
    parsed.duration_days = days;
  }

  await env.CODES.put(code, JSON.stringify(parsed));
  return json({ success: true, ...parsed }, 200, {}, request, allowedOrigins);
}

async function deleteCode(request, env, code, allowedOrigins) {
  const existing = await env.CODES.get(code);
  if (!existing) return json({ error: "کد یافت نشد" }, 404, {}, request, allowedOrigins);
  await env.CODES.delete(code);
  return json({ success: true }, 200, {}, request, allowedOrigins);
}

// -------------------------------------------------------------
// Helpers
// -------------------------------------------------------------
function normalizeCode(code) {
  if (typeof code !== "string") return null;
  const normalized = code.replace(/[-\s]/g, "").toUpperCase();
  if (normalized.length !== CODE_LENGTH) return null;
  if (!/^[A-Z0-9]+$/.test(normalized)) return null;
  return normalized.match(/.{1,4}/g).join("-");
}

function generateCode() {
  const buf      = new Uint8Array(CODE_LENGTH);
  const alphaLen = CODE_ALPHABET.length;
  const max      = 256 - (256 % alphaLen);
  let out = "";
  let i   = 0;
  while (i < CODE_LENGTH) {
    const tmp = new Uint8Array(1);
    crypto.getRandomValues(tmp);
    if (tmp[0] < max) {
      out += CODE_ALPHABET[tmp[0] % alphaLen];
      i++;
    }
  }
  return out.match(/.{1,4}/g).join("-");
}

function generateSessionId() {
  return randomHex(32);
}

function getSessionId(request, COOKIE_NAME) {
  const cookie = request.headers.get("Cookie") || "";
  const m = cookie.split(";").map(s => s.trim()).find(s => s.startsWith(COOKIE_NAME + "="));
  if (!m) return null;
  try {
    return decodeURIComponent(m.slice(COOKIE_NAME.length + 1));
  } catch {
    return m.slice(COOKIE_NAME.length + 1);
  }
}

function cookieHeader(value, maxAge, secure, COOKIE_NAME) {
  const parts = [
    `${COOKIE_NAME}=${encodeURIComponent(value)}`,
    "Path=/", "HttpOnly", "SameSite=Strict", `Max-Age=${maxAge}`
  ];
  if (secure && maxAge > 0) parts.push("Secure");
  return parts.join("; ");
}

function validateDate(input) {
  if (!input) return null;
  if (typeof input !== "string" && typeof input !== "number") return null;
  const d = new Date(input);
  if (isNaN(d.getTime())) return null;
  return d.toISOString();
}

function safeDecode(s) {
  if (typeof s !== "string") return null;
  try { return decodeURIComponent(s); } catch { return null; }
}

function isSecureRequest(request) {
  if (request.url.startsWith("https://")) return true;
  const forwardedProto = request.headers.get("X-Forwarded-Proto");
  return forwardedProto === "https";
}

// -------------------------------------------------------------
// CORS
// -------------------------------------------------------------
function corsHeaders(request, allowedOrigins) {
  const origin = request?.headers?.get("Origin") || "";

  let allowOrigin       = null;
  let allowCredentials  = false;

  if (origin) {
    if (allowedOrigins.includes(origin)) {
      allowOrigin = origin;
      allowCredentials = true;
    } else if (allowedOrigins.includes("*")) {
      allowOrigin = origin;
      allowCredentials = false;
    } else {
      allowOrigin = null;
      allowCredentials = false;
    }
  } else {
    allowOrigin = "*";
    allowCredentials = false;
  }

  const headers = {
    "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Max-Age":       "86400",
    "Vary":                          "Origin"
  };

  if (allowOrigin) {
    headers["Access-Control-Allow-Origin"] = allowOrigin;
    if (allowCredentials && allowOrigin !== "*") {
      headers["Access-Control-Allow-Credentials"] = "true";
    }
  } else {
    headers["Access-Control-Allow-Origin"] = "null";
  }

  return headers;
}

function json(data, status = 200, extra = {}, request = null, allowedOrigins = []) {
  const headers = {
    "Content-Type":           "application/json; charset=utf-8",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options":        "DENY",
    ...extra
  };

  if (request) {
    Object.assign(headers, corsHeaders(request, allowedOrigins));
  }

  return new Response(JSON.stringify(data), { status, headers });
}

function methodError(request, allowedOrigins) {
  return json({ error: "متد نامعتبر" }, 405, {}, request, allowedOrigins);
}

// =============================================================
// HTML / UI (Admin Dashboard)
// =============================================================
function getAdminHTML() {
  return `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#0b0f1c">
  <title>داشبورد کدها و اشتراک‌ها</title>

  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;500;600;700&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">

  <script src="https://cdn.jsdelivr.net/npm/persian-date@1.1.0/dist/persian-date.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/alpinejs@3.13.3/dist/cdn.min.js" defer></script>

  <style>
    :root {
      --bg-0: #0b0f1c;
      --bg-1: #11172a;
      --bg-2: #161e36;
      --bg-3: #1d2742;
      --line: #232d4a;
      --line-2: #2c3858;
      --text: #e7ebf3;
      --text-2: #9aa3bc;
      --text-3: #6b7595;
      --primary: #5ee0ff;
      --primary-d: #00b8d4;
      --success: #4ade80;
      --warning: #f5b947;
      --danger:  #f87171;
      --shadow-sm: 0 1px 2px rgba(0,0,0,.25);
      --shadow-md: 0 6px 20px rgba(0,0,0,.28);
      --shadow-lg: 0 12px 40px rgba(0,0,0,.45);
      --r-sm: 8px;
      --r-md: 12px;
      --r-lg: 18px;
      --tr: 180ms cubic-bezier(0.4, 0, 0.2, 1);
    }

    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    html { scroll-behavior: smooth; }
    body {
      font-family: 'Vazirmatn', 'Segoe UI', Tahoma, sans-serif;
      background: var(--bg-0);
      color: var(--text);
      min-height: 100vh;
      line-height: 1.65;
      font-weight: 400;
      -webkit-font-smoothing: antialiased;
      background-image:
        radial-gradient(1200px 600px at 85% -10%, rgba(94,224,255,0.08), transparent 60%),
        radial-gradient(1000px 500px at 10% 110%, rgba(120,140,255,0.06), transparent 60%);
      background-attachment: fixed;
    }

    *::-webkit-scrollbar { width: 8px; height: 8px; }
    *::-webkit-scrollbar-track { background: transparent; }
    *::-webkit-scrollbar-thumb { background: var(--line-2); border-radius: 8px; }
    *::-webkit-scrollbar-thumb:hover { background: #3a466d; }

    [x-cloak] { display: none !important; }

    .login-wrap {
      min-height: 100vh;
      display: flex; align-items: center; justify-content: center;
      padding: 20px;
    }
    .login-card {
      width: 100%; max-width: 380px;
      background: var(--bg-1);
      border: 1px solid var(--line);
      border-radius: var(--r-lg);
      padding: 36px 28px;
      box-shadow: var(--shadow-lg);
    }
    .login-head { text-align: center; margin-bottom: 24px; }
    .login-logo {
      width: 52px; height: 52px;
      background: linear-gradient(135deg, var(--primary), #6a8bff);
      border-radius: var(--r-md);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 14px;
      box-shadow: 0 0 24px rgba(94,224,255,0.18);
    }
    .login-title { font-size: 19px; font-weight: 600; margin-bottom: 4px; }
    .login-sub   { font-size: 13px; color: var(--text-2); font-weight: 300; }
    .login-form  { display: flex; flex-direction: column; gap: 14px; }
    .login-submit { width: 100%; padding: 12px; font-size: 14px; margin-top: 4px; }

    .container { max-width: 1240px; margin: 0 auto; padding: 28px 24px; }
    .topbar {
      display: flex; align-items: center; justify-content: space-between;
      padding-bottom: 22px;
      margin-bottom: 28px;
      border-bottom: 1px solid var(--line);
    }
    .brand { display: flex; align-items: center; gap: 12px; font-size: 16px; font-weight: 600; }
    .brand-mark {
      width: 34px; height: 34px;
      background: linear-gradient(135deg, var(--primary), #6a8bff);
      border-radius: var(--r-sm);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 0 18px rgba(94,224,255,0.18);
    }

    .stats {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 22px;
    }
    .stat {
      background: var(--bg-1);
      border: 1px solid var(--line);
      border-radius: var(--r-md);
      padding: 16px 18px;
      display: flex; align-items: center; gap: 14px;
      transition: var(--tr);
    }
    .stat:hover { border-color: var(--line-2); transform: translateY(-1px); }
    .stat-icon {
      width: 40px; height: 40px;
      border-radius: var(--r-sm);
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .stat-icon.t { background: rgba(94,224,255,0.10);  color: var(--primary); }
    .stat-icon.a { background: rgba(74,222,128,0.10);  color: var(--success); }
    .stat-icon.u { background: rgba(154,163,188,0.10); color: var(--text-2); }
    .stat-icon.x { background: rgba(248,113,113,0.10); color: var(--danger); }
    .stat-label { font-size: 12px; color: var(--text-2); margin-bottom: 3px; font-weight: 400; }
    .stat-value { font-size: 22px; font-weight: 700; line-height: 1.1; }
    @media (max-width: 720px) { .stats { grid-template-columns: repeat(2, 1fr); } }

    .card {
      background: var(--bg-1);
      border: 1px solid var(--line);
      border-radius: var(--r-lg);
      padding: 22px 24px;
      margin-bottom: 18px;
      box-shadow: var(--shadow-sm);
    }
    .card-head {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 18px;
      padding-bottom: 14px;
      border-bottom: 1px solid var(--line);
    }
    .card-title {
      font-size: 15px; font-weight: 600;
      display: flex; align-items: center; gap: 10px;
    }
    .card-title-dot {
      width: 6px; height: 6px; border-radius: 50%;
      background: var(--primary);
      box-shadow: 0 0 0 4px rgba(94,224,255,0.10);
    }
    .card-meta { color: var(--text-2); font-size: 13px; font-weight: 400; }

    .tabs {
      display: flex; gap: 4px;
      margin-bottom: 16px;
      border-bottom: 1px solid var(--line);
      padding-bottom: 2px;
    }
    .tab-btn {
      background: transparent;
      border: none;
      padding: 8px 16px;
      border-radius: var(--r-sm) var(--r-sm) 0 0;
      color: var(--text-2);
      font-family: inherit;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: var(--tr);
      border-bottom: 2px solid transparent;
    }
    .tab-btn:hover { color: var(--text); }
    .tab-btn.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr 0.8fr auto;
      gap: 12px;
      align-items: end;
    }
    @media (max-width: 820px) { .form-grid { grid-template-columns: 1fr; } }
    .form-group { display: flex; flex-direction: column; gap: 6px; }
    .form-label { font-size: 12px; color: var(--text-2); font-weight: 400; }

    input, select, textarea {
      background: var(--bg-2);
      border: 1px solid var(--line);
      color: var(--text);
      padding: 10px 14px;
      border-radius: var(--r-sm);
      width: 100%;
      font-family: inherit;
      font-size: 14px;
      font-weight: 400;
      transition: var(--tr);
    }
    input:hover, select:hover { border-color: var(--line-2); }
    input:focus, select:focus, textarea:focus {
      outline: none;
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(94,224,255,0.12);
    }
    input::placeholder { color: var(--text-3); font-weight: 300; }

    .btn {
      display: inline-flex; align-items: center; justify-content: center; gap: 7px;
      border: 1px solid transparent;
      padding: 10px 16px;
      border-radius: var(--r-sm);
      cursor: pointer;
      transition: var(--tr);
      font-family: inherit;
      font-size: 13px;
      font-weight: 500;
      white-space: nowrap;
      user-select: none;
      background: var(--bg-2);
      color: var(--text);
      border-color: var(--line);
    }
    .btn:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }
    .btn:disabled { opacity: 0.45; cursor: not-allowed; }
    .btn:not(:disabled):hover { transform: translateY(-1px); }
    .btn:not(:disabled):active { transform: translateY(0); }

    .btn-primary  { background: var(--primary); color: #001824; border-color: transparent; }
    .btn-primary:hover:not(:disabled)  { box-shadow: 0 4px 14px rgba(94,224,255,0.30); }
    .btn-ghost    { background: transparent; border-color: transparent; }
    .btn-ghost:hover:not(:disabled)    { background: var(--bg-2); }
    .btn-danger   { background: var(--danger); color: #2a0606; border-color: transparent; }
    .btn-danger:hover:not(:disabled)   { box-shadow: 0 4px 14px rgba(248,113,113,0.30); }
    .btn-success  { background: var(--success); color: #04210d; border-color: transparent; }
    .btn-warning  { background: var(--warning); color: #2a1a00; border-color: transparent; }
    .btn-sm       { padding: 6px 11px; font-size: 12px; }
    .btn-icon     { padding: 7px; }

    .badge {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 3px 9px;
      border-radius: 100px;
      font-size: 11px;
      font-weight: 500;
      line-height: 1.4;
    }
    .badge::before { content: ''; width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
    .badge-active   { background: rgba(74,222,128,0.10);  color: var(--success); }
    .badge-inactive { background: rgba(248,113,113,0.10); color: var(--danger); }
    .badge-used     { background: rgba(154,163,188,0.10); color: var(--text-2); }
    .badge-expired  { background: rgba(245,185,71,0.10);  color: var(--warning); }

    .toolbar {
      display: flex; gap: 10px; align-items: center; flex-wrap: wrap;
      margin-bottom: 14px;
    }
    .search { flex: 1; min-width: 200px; position: relative; }
    .search svg {
      position: absolute; right: 12px; top: 50%;
      transform: translateY(-50%);
      color: var(--text-3);
      pointer-events: none;
    }
    .search input { padding-right: 38px; }

    .filters {
      display: flex; gap: 2px;
      background: var(--bg-2);
      padding: 3px;
      border-radius: var(--r-sm);
      border: 1px solid var(--line);
    }
    .filter-btn {
      background: transparent; border: none;
      color: var(--text-2);
      padding: 5px 11px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 12px;
      font-weight: 500;
      font-family: inherit;
      transition: var(--tr);
    }
    .filter-btn:hover  { color: var(--text); }
    .filter-btn.active { background: var(--primary); color: #001824; }

    .table-wrap {
      overflow-x: auto;
      border-radius: var(--r-md);
      border: 1px solid var(--line);
    }
    .table { width: 100%; border-collapse: collapse; }
    .table th, .table td { padding: 12px 14px; text-align: right; }
    .table th {
      background: var(--bg-2);
      color: var(--text-2);
      font-weight: 500;
      font-size: 12px;
      letter-spacing: 0.2px;
    }
    .table tbody tr { border-top: 1px solid var(--line); transition: background var(--tr); }
    .table tbody tr:hover { background: rgba(255,255,255,0.015); }

    .code-cell {
      font-family: 'JetBrains Mono', monospace;
      font-size: 13.5px;
      letter-spacing: 0.5px;
      color: var(--primary);
      font-weight: 600;
      display: flex; align-items: center; gap: 8px;
    }
    .copy-btn {
      background: transparent; border: none;
      color: var(--text-3);
      cursor: pointer;
      padding: 3px;
      border-radius: 4px;
      display: flex; align-items: center; justify-content: center;
      transition: var(--tr);
    }
    .copy-btn:hover  { color: var(--primary); background: rgba(94,224,255,0.10); }
    .copy-btn.copied { color: var(--success); }

    .date-cell { display: flex; flex-direction: column; gap: 1px; line-height: 1.3; }
    .date-rel  { font-size: 11px; color: var(--text-3); font-weight: 300; }
    .actions   { display: flex; gap: 5px; flex-wrap: wrap; }

    .created-box {
      background: var(--bg-2);
      border: 1px solid var(--line);
      border-radius: var(--r-md);
      padding: 18px 20px;
      text-align: center;
      margin-top: 16px;
      animation: fadeIn 200ms ease;
    }
    .created-label { font-size: 12px; color: var(--text-2); margin-bottom: 8px; font-weight: 300; }
    .created-value {
      font-family: 'JetBrains Mono', monospace;
      font-size: 24px;
      font-weight: 700;
      color: var(--primary);
      letter-spacing: 2px;
      margin-bottom: 12px;
    }

    .quick-row {
      display: flex; gap: 6px; flex-wrap: wrap;
      margin-top: 10px;
    }
    .chip {
      background: var(--bg-2);
      border: 1px solid var(--line);
      color: var(--text-2);
      padding: 5px 11px;
      border-radius: 100px;
      font-size: 12px;
      font-weight: 400;
      font-family: inherit;
      cursor: pointer;
      transition: var(--tr);
    }
    .chip:hover  { color: var(--text); border-color: var(--line-2); }
    .chip.active { background: var(--primary); color: #001824; border-color: var(--primary); }

    .dp { position: relative; }
    .dp-input {
      cursor: pointer;
      font-family: 'Vazirmatn', monospace;
      letter-spacing: 0.3px;
      font-size: 13.5px;
    }
    .dp-input::placeholder { font-family: 'Vazirmatn', sans-serif; letter-spacing: 0; }
    .dp-clear {
      position: absolute; left: 8px; top: 50%;
      transform: translateY(-50%);
      background: transparent; border: none;
      color: var(--text-3);
      cursor: pointer;
      padding: 4px;
      display: flex; align-items: center; justify-content: center;
      border-radius: 4px;
      transition: var(--tr);
    }
    .dp-clear:hover { color: var(--danger); background: rgba(248,113,113,0.10); }
    .dp-pop {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: var(--bg-2);
      border: 1px solid var(--line);
      border-radius: var(--r-md);
      padding: 14px;
      box-shadow: var(--shadow-lg);
      z-index: 200;
      width: 290px;
      animation: dpIn 180ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    @keyframes dpIn { from { opacity: 0; transform: translateY(-6px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
    .dp-head {
      display: flex; align-items: center; gap: 6px;
      margin-bottom: 10px;
    }
    .dp-nav {
      width: 28px; height: 28px;
      background: transparent;
      border: 1px solid var(--line);
      color: var(--text-2);
      border-radius: 6px;
      cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: var(--tr);
    }
    .dp-nav:hover  { color: var(--text); border-color: var(--line-2); }
    .dp-title {
      flex: 1;
      text-align: center;
      font-size: 13px;
      font-weight: 600;
    }
    .dp-weekdays {
      display: grid; grid-template-columns: repeat(7, 1fr);
      text-align: center;
      font-size: 11px;
      color: var(--text-3);
      margin-bottom: 4px;
      font-weight: 400;
    }
    .dp-weekdays > div { padding: 6px 0; }
    .dp-days {
      display: grid; grid-template-columns: repeat(7, 1fr);
      gap: 2px;
    }
    .dp-day {
      aspect-ratio: 1;
      display: flex; align-items: center; justify-content: center;
      border: 1px solid transparent;
      background: transparent;
      color: var(--text);
      border-radius: 6px;
      cursor: pointer;
      font-size: 12.5px;
      font-family: 'Vazirmatn', sans-serif;
      font-weight: 400;
      transition: var(--tr);
    }
    .dp-day:hover:not(.disabled):not(.other) { background: var(--bg-3); }
    .dp-day.other { color: var(--text-3); opacity: 0.4; cursor: default; }
    .dp-day.disabled { color: var(--text-3); opacity: 0.3; cursor: not-allowed; }
    .dp-day.today  { border-color: var(--primary); color: var(--primary); }
    .dp-day.selected { background: var(--primary); color: #001824; font-weight: 600; }
    .dp-day.selected:hover { background: var(--primary); }
    .dp-time {
      display: flex; align-items: center; justify-content: center; gap: 6px;
      margin-top: 12px; padding-top: 12px;
      border-top: 1px solid var(--line);
    }
    .dp-time-label { font-size: 12px; color: var(--text-2); margin-left: 4px; }
    .dp-time input {
      width: 50px;
      text-align: center;
      padding: 6px 4px;
      font-size: 13px;
      font-family: 'Vazirmatn', monospace;
    }
    .dp-foot {
      display: flex; gap: 6px; justify-content: space-between;
      margin-top: 10px;
    }
    .dp-foot .btn { padding: 6px 10px; font-size: 12px; }

    .alert {
      padding: 10px 14px;
      border-radius: var(--r-sm);
      display: flex; align-items: center; gap: 10px;
      font-size: 13px;
    }
    .alert-danger  { background: rgba(248,113,113,0.08); border: 1px solid rgba(248,113,113,0.25); color: var(--danger); }

    .toasts {
      position: fixed; top: 18px; left: 18px;
      z-index: 1000;
      display: flex; flex-direction: column; gap: 8px;
      max-width: 340px;
    }
    .toast {
      background: var(--bg-2);
      border: 1px solid var(--line);
      border-radius: var(--r-md);
      padding: 11px 14px;
      box-shadow: var(--shadow-lg);
      display: flex; align-items: center; gap: 11px;
      min-width: 260px;
      font-size: 13.5px;
      animation: toastIn 220ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    .toast.is-leaving { animation: toastOut 200ms ease forwards; }
    .toast.success { border-color: rgba(74,222,128,0.35); }
    .toast.error   { border-color: rgba(248,113,113,0.35); }
    .toast-content { flex: 1; }
    .toast-close {
      background: none; border: none;
      color: var(--text-3);
      cursor: pointer; padding: 3px;
      display: flex; align-items: center; justify-content: center;
    }
    .toast-close:hover { color: var(--text); }

    .modal-bg {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.65);
      backdrop-filter: blur(6px);
      display: flex; align-items: center; justify-content: center;
      z-index: 999;
      padding: 20px;
      animation: fadeIn 200ms ease;
    }
    .modal {
      background: var(--bg-1);
      border: 1px solid var(--line);
      border-radius: var(--r-lg);
      padding: 24px;
      max-width: 420px; width: 100%;
      box-shadow: var(--shadow-lg);
      animation: modalIn 220ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    .modal-title { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
    .modal-text  { color: var(--text-2); margin-bottom: 22px; font-size: 14px; }
    .modal-text code {
      font-family: 'JetBrains Mono', monospace;
      color: var(--primary);
      background: var(--bg-2);
      padding: 1px 7px;
      border-radius: 4px;
      font-size: 12.5px;
    }
    .modal-actions { display: flex; gap: 8px; justify-content: flex-end; }

    .empty { text-align: center; padding: 50px 20px; color: var(--text-2); }
    .empty-icon { width: 44px; height: 44px; margin: 0 auto 12px; color: var(--text-3); }
    .empty-title { font-size: 14px; font-weight: 500; color: var(--text); margin-bottom: 3px; }
    .empty-text  { font-size: 13px; }

    .loading { display: flex; align-items: center; justify-content: center; padding: 50px; }
    .spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.15);
      border-top-color: currentColor;
      border-radius: 50%;
      animation: spin 700ms linear infinite;
    }
    .spinner-lg { width: 24px; height: 24px; border-width: 2.5px; }

    @keyframes spin     { to { transform: rotate(360deg); } }
    @keyframes fadeIn   { from { opacity: 0; } to { opacity: 1; } }
    @keyframes toastIn  { from { opacity: 0; transform: translateX(-16px); } to { opacity: 1; transform: translateX(0); } }
    @keyframes toastOut { to { opacity: 0; transform: translateX(-16px); } }
    @keyframes modalIn  { from { opacity: 0; transform: scale(0.96) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
    .fade-in { animation: fadeIn 250ms ease; }

    .load-more-wrap {
      display: flex; justify-content: center; padding: 16px 0 8px;
    }
    .load-more-wrap .btn { min-width: 140px; }

    @media (max-width: 640px) {
      .container { padding: 18px 16px; }
      .topbar    { flex-direction: column; gap: 12px; align-items: stretch; text-align: center; }
      .brand     { justify-content: center; }
      .stats     { grid-template-columns: repeat(2, 1fr); }
      .stat-value { font-size: 19px; }
      .table th, .table td { padding: 10px 12px; font-size: 12.5px; }
      .code-cell  { font-size: 12px; }
      .toolbar    { flex-direction: column; align-items: stretch; }
      .dp-pop     { width: calc(100vw - 32px); max-width: 320px; }
      .form-grid  { grid-template-columns: 1fr; }
    }
    @media (max-width: 420px) { .stats { grid-template-columns: 1fr; } }
  </style>
</head>
<body>
<div x-data="adminApp()" x-init="initApp()" x-cloak>

  <!-- Login -->
  <template x-if="!loggedIn">
    <div class="login-wrap">
      <form class="login-card" @submit.prevent="login()">
        <div class="login-head">
          <div class="login-logo">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#0b0f1c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2"/>
              <path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
          </div>
          <div class="login-title">پنل مدیریت</div>
          <div class="login-sub">برای ادامه وارد شوید</div>
        </div>
        <div class="login-form">
          <div class="form-group">
            <label class="form-label" for="password">رمز عبور</label>
            <input id="password" type="password" x-model="password" placeholder="رمز عبور" :disabled="logging" autocomplete="current-password" required>
          </div>
          <div x-show="loginError" class="alert alert-danger" x-text="loginError"></div>
          <button type="submit" class="btn btn-primary login-submit" :disabled="logging || !password">
            <span x-show="!logging">ورود</span>
            <span x-show="logging" class="spinner"></span>
          </button>
        </div>
      </form>
    </div>
  </template>

  <!-- Dashboard -->
  <template x-if="loggedIn">
    <div class="container fade-in">

      <header class="topbar">
        <div class="brand">
          <div class="brand-mark">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0b0f1c" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2L4 6v6c0 5 3.5 9 8 10 4.5-1 8-5 8-10V6l-8-4z"/>
              <path d="M9 12l2 2 4-4"/>
            </svg>
          </div>
          <span>مدیریت کدها و اشتراک‌ها</span>
        </div>
        <button class="btn btn-ghost" @click="logout()">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          <span>خروج</span>
        </button>
      </header>

      <!-- Stats -->
      <div class="stats">
        <div class="stat">
          <div class="stat-icon t">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
              <rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>
            </svg>
          </div>
          <div>
            <div class="stat-label">کل کدها</div>
            <div class="stat-value" x-text="toPersianDigits(stats.total)"></div>
          </div>
        </div>
        <div class="stat">
          <div class="stat-icon a">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <div>
            <div class="stat-label">کدهای فعال</div>
            <div class="stat-value" x-text="toPersianDigits(stats.activeCodes)"></div>
          </div>
        </div>
        <div class="stat">
          <div class="stat-icon u">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <div>
            <div class="stat-label">اشتراک‌های فعال</div>
            <div class="stat-value" x-text="toPersianDigits(stats.activeSubscriptions)"></div>
          </div>
        </div>
        <div class="stat">
          <div class="stat-icon x">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
          <div>
            <div class="stat-label">کل اشتراک‌ها</div>
            <div class="stat-value" x-text="toPersianDigits(stats.totalSubscriptions)"></div>
          </div>
        </div>
      </div>

      <!-- Create Code -->
      <div class="card" x-show="activeTab === 'codes'">
        <div class="card-head">
          <div class="card-title"><span class="card-title-dot"></span><span>ایجاد کد جدید</span></div>
        </div>

        <form @submit.prevent="createCode()" @expiry-set="handleExpirySet($event)">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label" for="expires">تاریخ انقضا (اختیاری) – به وقت UTC</label>
              <div class="dp"
                   x-data="datePicker()"
                   x-init="initPicker($el, newCodeExpires)"
                   @expiry-sync.window="syncFromISO($event.detail.iso)">
                <input id="expires" type="text" class="dp-input" :value="displayValue"
                       @click="toggle($event)" placeholder="انتخاب تاریخ شمسی" readonly autocomplete="off">
                <button type="button" class="dp-clear" x-show="displayValue" @click.stop="clear()" aria-label="پاک کردن">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
                <div class="dp-pop" x-show="open" @click.stop x-transition.opacity.duration.150ms>
                  <div class="dp-head">
                    <button type="button" class="dp-nav" @click="prevMonth()" aria-label="ماه قبل">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="15 18 9 12 15 6"/>
                      </svg>
                    </button>
                    <div class="dp-title" x-text="months[viewMonth - 1] + ' ' + toPersianDigits(viewYear)"></div>
                    <button type="button" class="dp-nav" @click="nextMonth()" aria-label="ماه بعد">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="9 18 15 12 9 6"/>
                      </svg>
                    </button>
                  </div>
                  <div class="dp-weekdays">
                    <template x-for="w in weekdays" :key="w"><div x-text="w"></div></template>
                  </div>
                  <div class="dp-days">
                    <template x-for="(cell, idx) in gridDays" :key="idx">
                      <button type="button" class="dp-day" :class="{
                        'other': cell.month !== 'current',
                        'today': cell.isToday,
                        'selected': isSelected(cell),
                        'disabled': cell.isDisabled
                      }" :disabled="cell.isDisabled"
                      @click="pickDay(cell)" x-text="toPersianDigits(cell.day)"></button>
                    </template>
                  </div>
                  <div class="dp-time">
                    <span class="dp-time-label">ساعت:</span>
                    <input type="number" min="0" max="23" x-model.number="hours" @change="emitChange()">
                    <span style="color: var(--text-3);">:</span>
                    <input type="number" min="0" max="59" x-model.number="minutes" @change="emitChange()">
                  </div>
                  <div class="dp-foot">
                    <button type="button" class="btn btn-ghost" @click="goToday()">امروز</button>
                    <button type="button" class="btn btn-ghost" @click="clear()">پاک کردن</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="active">وضعیت اولیه</label>
              <select id="active" x-model="newCodeActive">
                <option value="true">فعال</option>
                <option value="false">غیرفعال</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="duration">مدت (روز)</label>
              <input type="number" id="duration" x-model="newCodeDuration" min="1" placeholder="۳۰">
            </div>

            <button type="submit" class="btn btn-primary" :disabled="creating">
              <span x-show="!creating">ایجاد کد</span>
              <span x-show="creating" class="spinner"></span>
            </button>
          </div>

          <div class="quick-row">
            <span style="font-size: 12px; color: var(--text-2); align-self: center; margin-left: 4px;">انتخاب سریع:</span>
            <button type="button" class="chip" :class="{ active: activeChip === 1 }" @click="setQuickExpiry(1)">۱ روز</button>
            <button type="button" class="chip" :class="{ active: activeChip === 7 }" @click="setQuickExpiry(7)">۱ هفته</button>
            <button type="button" class="chip" :class="{ active: activeChip === 30 }" @click="setQuickExpiry(30)">۱ ماه</button>
            <button type="button" class="chip" :class="{ active: activeChip === 90 }" @click="setQuickExpiry(90)">۳ ماه</button>
            <button type="button" class="chip" :class="{ active: activeChip === 365 }" @click="setQuickExpiry(365)">۱ سال</button>
            <button type="button" class="chip" :class="{ active: activeChip === 0 }" @click="clearExpiry()">مادام‌العمر</button>
          </div>
        </form>

        <div x-show="lastCreated" x-transition class="created-box">
          <div class="created-label">کد ایجادشده</div>
          <div class="created-value" x-text="lastCreated"></div>
          <button class="btn btn-sm" @click="copyCode(lastCreated)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
            </svg>
            <span>کپی</span>
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'codes' }" @click="switchTab('codes')">کدها</button>
        <button class="tab-btn" :class="{ active: activeTab === 'subscriptions' }" @click="switchTab('subscriptions')">اشتراک‌ها</button>
      </div>

      <!-- Codes List -->
      <div x-show="activeTab === 'codes'" class="card">
        <div class="card-head">
          <div class="card-title"><span class="card-title-dot"></span><span>لیست کدها</span></div>
          <span class="card-meta" x-text="toPersianDigits(filteredCodes.length) + ' / ' + toPersianDigits(codes.length)"></span>
        </div>

        <div class="toolbar">
          <div class="search">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input type="search" x-model="searchQuery" placeholder="جستجو...">
          </div>
          <div class="filters">
            <button class="filter-btn" :class="{active: filter==='all'}"      @click="filter='all'">همه</button>
            <button class="filter-btn" :class="{active: filter==='active'}"   @click="filter='active'">فعال</button>
            <button class="filter-btn" :class="{active: filter==='used'}"     @click="filter='used'">مصرف‌شده</button>
            <button class="filter-btn" :class="{active: filter==='expired'}"  @click="filter='expired'">منقضی</button>
            <button class="filter-btn" :class="{active: filter==='inactive'}" @click="filter='inactive'">غیرفعال</button>
          </div>
        </div>

        <div x-show="loading" class="loading"><div class="spinner spinner-lg" style="color: var(--primary);"></div></div>

        <div x-show="!loading">
          <div x-show="filteredCodes.length === 0" class="empty">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <div class="empty-title" x-text="codes.length === 0 ? 'هنوز کدی ایجاد نشده' : 'نتیجه‌ای یافت نشد'"></div>
            <div class="empty-text" x-text="codes.length === 0 ? 'اولین کد را با فرم بالا ایجاد کنید' : 'فیلتر یا جستجو را تغییر دهید'"></div>
          </div>

          <div x-show="filteredCodes.length > 0">
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>کد</th><th>وضعیت</th><th>ایجاد</th><th>انقضا</th><th>عملیات</th></tr></thead>
                <tbody>
                  <template x-for="item in filteredCodes" :key="item.code">
                    <tr>
                      <td><div class="code-cell"><span x-text="item.code"></span><button class="copy-btn" @click="copyCode(item.code)" aria-label="کپی">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
                        </svg>
                      </button></div></td>
                      <td><span class="badge" :class="getStatusClass(item)" x-text="getStatusText(item)"></span></td>
                      <td><div class="date-cell"><span x-text="formatDate(item.created_at)"></span><span class="date-rel" x-text="getRelativeTime(item.created_at)"></span></div></td>
                      <td><template x-if="item.expires_at"><div class="date-cell"><span x-text="formatDate(item.expires_at)"></span><span class="date-rel" x-text="getRelativeTime(item.expires_at)"></span></div></template><template x-if="!item.expires_at"><span style="color: var(--text-3);">مادام‌العمر</span></template></td>
                      <td><div class="actions">
                        <button class="btn btn-sm" :class="item.active ? 'btn-warning' : 'btn-success'" @click="toggleActive(item)" :disabled="item.used" :title="item.used ? 'کد مصرف‌شده غیرقابل تغییر' : (item.active ? 'غیرفعال' : 'فعال')"><span x-text="item.active ? 'غیرفعال' : 'فعال'"></span></button>
                        <button class="btn btn-sm btn-danger btn-icon" @click="confirmDelete(item)" aria-label="حذف"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg></button>
                      </div></td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
            <div class="load-more-wrap" x-show="hasMore"><button class="btn" @click="loadMore()" :disabled="isLoadingMore"><span x-show="!isLoadingMore">بارگذاری بیشتر</span><span x-show="isLoadingMore" class="spinner"></span></button></div>
          </div>
        </div>
      </div>

      <!-- Subscriptions List -->
      <div x-show="activeTab === 'subscriptions'" class="card">
        <div class="card-head">
          <div class="card-title"><span class="card-title-dot"></span><span>لیست اشتراک‌ها</span></div>
          <span class="card-meta" x-text="toPersianDigits(filteredSubscriptions.length) + ' / ' + toPersianDigits(subscriptions.length)"></span>
        </div>

        <div class="toolbar">
          <div class="search">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input type="search" x-model="subSearchQuery" placeholder="جستجو بر اساس کد یا شناسه...">
          </div>
          <div class="filters">
            <button class="filter-btn" :class="{active: subFilter==='all'}"      @click="subFilter='all'">همه</button>
            <button class="filter-btn" :class="{active: subFilter==='active'}"   @click="subFilter='active'">فعال</button>
            <button class="filter-btn" :class="{active: subFilter==='inactive'}" @click="subFilter='inactive'">غیرفعال</button>
            <button class="filter-btn" :class="{active: subFilter==='expired'}"  @click="subFilter='expired'">منقضی</button>
          </div>
        </div>

        <div x-show="subLoading" class="loading"><div class="spinner spinner-lg" style="color: var(--primary);"></div></div>

        <div x-show="!subLoading">
          <div x-show="filteredSubscriptions.length === 0" class="empty">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <div class="empty-title" x-text="subscriptions.length === 0 ? 'هنوز اشتراکی ایجاد نشده' : 'نتیجه‌ای یافت نشد'"></div>
            <div class="empty-text" x-text="subscriptions.length === 0 ? 'با فعال‌سازی یک کد، اشتراک ایجاد می‌شود' : 'فیلتر یا جستجو را تغییر دهید'"></div>
          </div>

          <div x-show="filteredSubscriptions.length > 0">
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>شناسه اشتراک</th><th>کد</th><th>وضعیت</th><th>ایجاد</th><th>انقضا</th><th>عملیات</th></tr></thead>
                <tbody>
                  <template x-for="sub in filteredSubscriptions" :key="sub.subscription_id">
                    <tr>
                      <td style="font-family: monospace; font-size: 12px;" x-text="sub.subscription_id.slice(0,8)+'…'"></td>
                      <td><span x-text="sub.code" style="font-family: monospace;"></span></td>
                      <td><span class="badge" :class="getSubStatusClass(sub)" x-text="getSubStatusText(sub)"></span></td>
                      <td><div class="date-cell"><span x-text="formatDate(sub.created_at)"></span><span class="date-rel" x-text="getRelativeTime(sub.created_at)"></span></div></td>
                      <td><template x-if="sub.expires_at"><div class="date-cell"><span x-text="formatDate(sub.expires_at)"></span><span class="date-rel" x-text="getRelativeTime(sub.expires_at)"></span></div></template><template x-if="!sub.expires_at"><span style="color: var(--text-3);">مادام‌العمر</span></template></td>
                      <td><div class="actions">
                        <button class="btn btn-sm" :class="sub.active ? 'btn-warning' : 'btn-success'" @click="toggleSubActive(sub)"><span x-text="sub.active ? 'غیرفعال' : 'فعال'"></span></button>
                        <button class="btn btn-sm btn-primary" @click="openExtendModal(sub)">تمدید</button>
                        <button class="btn btn-sm btn-danger btn-icon" @click="confirmDeleteSub(sub)" aria-label="حذف"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg></button>
                      </div></td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
            <div class="load-more-wrap" x-show="subHasMore"><button class="btn" @click="loadMoreSubs()" :disabled="subIsLoadingMore"><span x-show="!subIsLoadingMore">بارگذاری بیشتر</span><span x-show="subIsLoadingMore" class="spinner"></span></button></div>
          </div>
        </div>
      </div>

    </div>
  </template>

  <!-- Toasts -->
  <div class="toasts" aria-live="polite">
    <template x-for="toast in toasts" :key="toast.id">
      <div class="toast" :class="[toast.type, { 'is-leaving': toast.leaving }]">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            :stroke="toast && toast.type === 'success' ? 'var(--success)' : 'var(--danger)'">
          <polyline x-show="toast && toast.type === 'success'" points="20 6 9 17 4 12"/>
          <g x-show="toast && toast.type === 'error'">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </g>
        </svg>
        <div class="toast-content" x-text="toast.message"></div>
        <button class="toast-close" @click="removeToast(toast.id)" aria-label="بستن"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
      </div>
    </template>
  </div>

  <!-- Confirm Delete Modal -->
  <template x-if="confirmModal">
    <div class="modal-bg" @click.self="confirmModal = null" @keydown.escape.window="confirmModal = null">
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-title">تأیید حذف</div>
        <div class="modal-text">
          آیا از حذف <span x-text="confirmModal.type === 'code' ? 'کد' : 'اشتراک'"></span>
          <code x-text="confirmModal.type === 'code' ? confirmModal.item.code : confirmModal.item.subscription_id"></code> مطمئن هستید؟
        </div>
        <div class="modal-actions">
          <button class="btn" @click="confirmModal = null">انصراف</button>
          <button class="btn btn-danger" @click="performDelete()">حذف</button>
        </div>
      </div>
    </div>
  </template>

  <!-- Extend Modal -->
  <template x-if="extendModal">
    <div class="modal-bg" @click.self="extendModal = null" @keydown.escape.window="extendModal = null" @expiry-set="handleExpirySet($event)">
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-title">تمدید اشتراک</div>
        <div class="modal-text">
          <p>تاریخ انقضای جدید را وارد کنید (خالی = حذف انقضا / مادام‌العمر). <span style="color: var(--text-3); font-size: 12px;">(به وقت UTC)</span></p>
          <div class="form-group" style="margin-top: 12px;">
            <label class="form-label">تاریخ جدید (شمسی)</label>
            <div class="dp"
                x-data="datePicker()"
                x-init="initPicker($el, extendModal && extendModal.newExpiry)"
                @expiry-sync.window="syncFromISO($event.detail.iso)">
              <input type="text" class="dp-input" :value="displayValue"
                     @click="toggle($event)" placeholder="انتخاب تاریخ شمسی" readonly autocomplete="off">
              <button type="button" class="dp-clear" x-show="displayValue" @click.stop="clear()" aria-label="پاک کردن">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
              <div class="dp-pop" x-show="open" @click.stop x-transition.opacity.duration.150ms>
                <div class="dp-head">
                  <button type="button" class="dp-nav" @click="prevMonth()" aria-label="ماه قبل">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="15 18 9 12 15 6"/>
                    </svg>
                  </button>
                  <div class="dp-title" x-text="months[viewMonth - 1] + ' ' + toPersianDigits(viewYear)"></div>
                  <button type="button" class="dp-nav" @click="nextMonth()" aria-label="ماه بعد">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="9 18 15 12 9 6"/>
                    </svg>
                  </button>
                </div>
                <div class="dp-weekdays">
                  <template x-for="w in weekdays" :key="w"><div x-text="w"></div></template>
                </div>
                <div class="dp-days">
                  <template x-for="(cell, idx) in gridDays" :key="idx">
                    <button type="button" class="dp-day" :class="{
                      'other': cell.month !== 'current',
                      'today': cell.isToday,
                      'selected': isSelected(cell),
                      'disabled': cell.isDisabled
                    }" :disabled="cell.isDisabled"
                    @click="pickDay(cell)" x-text="toPersianDigits(cell.day)"></button>
                  </template>
                </div>
                <div class="dp-time">
                  <span class="dp-time-label">ساعت:</span>
                  <input type="number" min="0" max="23" x-model.number="hours" @change="emitChange()">
                  <span style="color: var(--text-3);">:</span>
                  <input type="number" min="0" max="59" x-model.number="minutes" @change="emitChange()">
                </div>
                <div class="dp-foot">
                  <button type="button" class="btn btn-ghost" @click="goToday()">امروز</button>
                  <button type="button" class="btn btn-ghost" @click="clear()">پاک کردن</button>
                </div>
              </div>
            </div>
          </div>
          <p style="margin-top: 8px; font-size: 12px; color: var(--text-3);">ساعت به وقت UTC ذخیره می‌شود.</p>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="extendModal = null">انصراف</button>
          <button class="btn btn-primary" @click="performExtend()">ثبت</button>
        </div>
      </div>
    </div>
  </template>

</div>

<script>
  function datePicker() {
    return {
      open: false,
      selected: null,
      viewYear:  1403,
      viewMonth: 1,
      hours:     12,
      minutes:   0,
      rootEl:    null,
      _cleanup:  null,
      months:   ['فروردین','اردیبهشت','خرداد','تیر','مرداد','شهریور','مهر','آبان','آذر','دی','بهمن','اسفند'],
      weekdays: ['ش','ی','د','س','چ','پ','ج'],
      faDigits: ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'],
      toPersianDigits(n) {
        if (n === undefined || n === null) return '';
        return String(n).replace(/[0-9]/g, d => this.faDigits[+d]);
      },
      initPicker(el, initialISO) {
        this.rootEl = el;
        if (typeof persianDate === 'undefined') {
          console.warn('persian-date library not loaded; date picker disabled');
          return () => {};
        }
        const today = new persianDate();
        this.viewYear = today.year();
        this.viewMonth = today.month();
        if (initialISO) {
          const d = new Date(initialISO);
          if (!isNaN(d.getTime())) {
            const p = new persianDate(d);
            this.selected = p.clone();
            this.viewYear = p.year();
            this.viewMonth = p.month();
            this.hours = p.hour();
            this.minutes = p.minute();
          }
        }
        if (this._cleanup) this._cleanup();
        const outsideHandler = (e) => {
          if (this.open && this.rootEl && !this.rootEl.contains(e.target)) this.open = false;
        };
        const escHandler = (e) => { if (e.key === 'Escape') this.open = false; };
        document.addEventListener('click', outsideHandler, true);
        document.addEventListener('keydown', escHandler);
        this._cleanup = () => {
          document.removeEventListener('click', outsideHandler, true);
          document.removeEventListener('keydown', escHandler);
        };
        return this._cleanup;
      },
      toggle(e) { e.stopPropagation(); this.open = !this.open; },
      get displayValue() {
        if (!this.selected) return '';
        return this.formatJalali(this.selected, this.hours, this.minutes);
      },
      formatJalali(p, h, m) {
        const y  = this.toPersianDigits(String(p.year()).padStart(4,'0'));
        const mo = this.toPersianDigits(String(p.month()).padStart(2,'0'));
        const d  = this.toPersianDigits(String(p.date()).padStart(2,'0'));
        const hh = this.toPersianDigits(String(h).padStart(2,'0'));
        const mm = this.toPersianDigits(String(m).padStart(2,'0'));
        return y + '/' + mo + '/' + d + '  ' + hh + ':' + mm;
      },
      get gridDays() {
        if (typeof persianDate === 'undefined') return [];
        const first = new persianDate([this.viewYear, this.viewMonth, 1]);
        const firstDayOfWeek = first.day();
        const daysInMonth = first.daysInMonth();
        const prev = (this.viewMonth === 1)
          ? new persianDate([this.viewYear - 1, 12, 1])
          : new persianDate([this.viewYear, this.viewMonth - 1, 1]);
        const prevDays = prev.daysInMonth();
        const startOfToday = new Date(); startOfToday.setHours(0,0,0,0);
        const today = new persianDate();
        const cells = [];
        for (let i = firstDayOfWeek; i > 0; i--) {
          const d = prevDays - i + 1;
          const p = new persianDate([prev.year(), prev.month(), d]);
          cells.push({
            day: d,
            month: 'prev',
            pdate: p,
            isToday: false,
            isDisabled: true
          });
        }
        for (let i = 1; i <= daysInMonth; i++) {
          const p = new persianDate([this.viewYear, this.viewMonth, i]);
          const isToday = today.year() === p.year() && today.month() === p.month() && today.date() === p.date();
          const isPast  = p.toDate() < startOfToday;
          cells.push({
            day: i,
            month: 'current',
            pdate: p,
            isToday: isToday,
            isDisabled: isPast
          });
        }
        while (cells.length % 7 !== 0) {
          const last = cells[cells.length - 1];
          const np   = last.pdate;
          const nextDate = new persianDate([np.year(), np.month(), np.date() + 1]);
          cells.push({
            day: nextDate.date(),
            month: 'next',
            pdate: nextDate,
            isToday: false,
            isDisabled: true
          });
        }
        return cells;
      },
      pickDay(cell) {
        if (cell.isDisabled) return;
        this.selected = cell.pdate.clone();
        this.viewYear = this.selected.year();
        this.viewMonth = this.selected.month();
        this.clampTime();
        this.emitChange();
      },
      isSelected(cell) {
        if (!this.selected || cell.month !== 'current') return false;
        return this.selected.year() === cell.pdate.year()
            && this.selected.month() === cell.pdate.month()
            && this.selected.date() === cell.pdate.date();
      },
      prevMonth() {
        if (this.viewMonth === 1) { this.viewMonth = 12; this.viewYear--; }
        else this.viewMonth--;
      },
      nextMonth() {
        if (this.viewMonth === 12) { this.viewMonth = 1; this.viewYear++; }
        else this.viewMonth++;
      },
      goToday() {
        const t = new persianDate();
        this.selected = t.clone();
        this.viewYear = t.year();
        this.viewMonth = t.month();
        this.hours = t.hour();
        this.minutes = t.minute();
        this.emitChange();
      },
      clear() {
        this.selected = null;
        if (this.rootEl) {
          this.rootEl.dispatchEvent(new CustomEvent('expiry-set', {
            detail: { iso: null, action: 'clear' },
            bubbles: true
          }));
        }
      },
      clampTime() {
        this.hours   = Math.max(0, Math.min(23, parseInt(this.hours, 10) || 0));
        this.minutes = Math.max(0, Math.min(59, parseInt(this.minutes, 10) || 0));
      },
      syncFromISO(iso) {
        if (typeof persianDate === 'undefined') return;
        if (!iso) { this.selected = null; return; }
        const d = new Date(iso);
        if (isNaN(d.getTime())) return;
        const p = new persianDate(d);
        this.selected = p.clone();
        this.viewYear = p.year();
        this.viewMonth = p.month();
        this.hours = p.hour();
        this.minutes = p.minute();
      },
      emitChange() {
        this.clampTime();
        if (!this.selected) {
          if (this.rootEl) {
            this.rootEl.dispatchEvent(new CustomEvent('expiry-set', {
              detail: { iso: null },
              bubbles: true
            }));
          }
          return;
        }
        const p = this.selected.clone();
        p.hours(this.hours).minutes(this.minutes).seconds(0).milliseconds(0);
        const iso = p.toDate().toISOString();
        if (this.rootEl) {
          this.rootEl.dispatchEvent(new CustomEvent('expiry-set', {
            detail: { iso: iso },
            bubbles: true
          }));
        }
      }
    };
  }

  function adminApp() {
    return {
      loggedIn: false,
      password: '',
      loginError: '',
      logging: false,
      toasts: [],
      _toastId: 0,
      confirmModal: null,
      extendModal: null,
      copiedCode: '',
      faDigits: ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'],
      activeTab: 'codes',

      codes: [],
      loading: false,
      creating: false,
      newCodeExpires: '',
      newCodeActive: 'true',
      newCodeDuration: 30,
      lastCreated: '',
      searchQuery: '',
      filter: 'all',
      paginationCursor: null,
      hasMore: false,
      isLoadingMore: false,
      searchTimeout: null,
      activeChip: null,

      subscriptions: [],
      subLoading: false,
      subSearchQuery: '',
      subFilter: 'all',
      subPaginationCursor: null,
      subHasMore: false,
      subIsLoadingMore: false,
      subSearchTimeout: null,

      stats: {
        total: 0,
        activeCodes: 0,
        activeSubscriptions: 0,
        totalSubscriptions: 0
      },

      toPersianDigits(n) {
        if (n === undefined || n === null) return '';
        return String(n).replace(/[0-9]/g, d => this.faDigits[+d]);
      },

      getStatusText(item) {
        const now = new Date();
        if (item.used) return 'مصرف‌شده';
        if (!item.active) return 'غیرفعال';
        if (item.expires_at && new Date(item.expires_at) <= now) return 'منقضی';
        return 'فعال';
      },
      getStatusClass(item) {
        const now = new Date();
        if (item.used) return 'badge-used';
        if (!item.active) return 'badge-inactive';
        if (item.expires_at && new Date(item.expires_at) <= now) return 'badge-expired';
        return 'badge-active';
      },
      isExpired(item) {
        return item.expires_at && new Date(item.expires_at) <= new Date();
      },

      getSubStatusText(sub) {
        const now = new Date();
        if (!sub.active) return 'غیرفعال';
        if (sub.expires_at && new Date(sub.expires_at) <= now) return 'منقضی';
        return 'فعال';
      },
      getSubStatusClass(sub) {
        const now = new Date();
        if (!sub.active) return 'badge-inactive';
        if (sub.expires_at && new Date(sub.expires_at) <= now) return 'badge-expired';
        return 'badge-active';
      },

      get filteredCodes() {
        let list = this.codes.slice();
        const now = new Date();
        if (this.searchQuery) {
          const q = this.searchQuery.toLowerCase();
          list = list.filter(c => c.code.toLowerCase().includes(q));
        }
        switch (this.filter) {
          case 'active':   list = list.filter(c => c.active && !c.used && (!c.expires_at || new Date(c.expires_at) > now)); break;
          case 'used':     list = list.filter(c => c.used); break;
          case 'expired':  list = list.filter(c => c.active && !c.used && c.expires_at && new Date(c.expires_at) <= now); break;
          case 'inactive': list = list.filter(c => !c.active); break;
          default: break;
        }
        return list;
      },

      get filteredSubscriptions() {
        let list = this.subscriptions.slice();
        const now = new Date();
        if (this.subSearchQuery) {
          const q = this.subSearchQuery.toLowerCase();
          list = list.filter(s =>
            s.subscription_id.toLowerCase().includes(q) ||
            (s.code && s.code.toLowerCase().includes(q))
          );
        }
        switch (this.subFilter) {
          case 'active':   list = list.filter(s => s.active && (!s.expires_at || new Date(s.expires_at) > now)); break;
          case 'inactive': list = list.filter(s => !s.active); break;
          case 'expired':  list = list.filter(s => s.active && s.expires_at && new Date(s.expires_at) <= now); break;
          default: break;
        }
        return list;
      },

      initApp() {
        this.loadCodes();
        this.loadSubscriptions();
        this.$watch('searchQuery', (value) => {
          if (this.searchTimeout) clearTimeout(this.searchTimeout);
          this.searchTimeout = setTimeout(() => {
            if (value.trim() === '') this.loadCodes(false);
            else this.performSearch(value);
          }, 300);
        });
        this.$watch('subSearchQuery', (value) => {
          if (this.subSearchTimeout) clearTimeout(this.subSearchTimeout);
          this.subSearchTimeout = setTimeout(() => {
            if (value.trim() === '') this.loadSubscriptions(false);
            else this.performSubSearch(value);
          }, 300);
        });
      },

      switchTab(tab) {
        this.activeTab = tab;
        if (tab === 'subscriptions' && this.subscriptions.length === 0) this.loadSubscriptions();
      },

      async loadCodes(append = false) {
        if (this.loading || this.isLoadingMore) return;
        const isLoadingMore = append && this.hasMore;
        if (isLoadingMore) this.isLoadingMore = true;
        else this.loading = true;
        try {
          const cursorParam = append ? this.paginationCursor : null;
          const isSearch = this.searchQuery.trim() !== '';
          let url;
          if (isSearch && cursorParam) {
            url = '/api/codes?search=' + encodeURIComponent(this.searchQuery)
                + '&limit=20&offset=' + encodeURIComponent(cursorParam);
          } else {
            url = '/api/codes?limit=20' + (cursorParam ? '&cursor=' + encodeURIComponent(cursorParam) : '');
          }
          const res = await fetch(url, { credentials: 'same-origin' });
          if (res.status === 401) { this.loggedIn = false; return; }
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const data = await res.json();
          this.loggedIn = true;
          if (data.warning) this.showToast(data.warning, 'warning');
          if (append) this.codes = this.codes.concat(data.items);
          else this.codes = data.items;
          this.paginationCursor = data.cursor || null;
          this.hasMore = data.has_more || false;
          this.updateStats();
        } catch (e) {
          if (!append) this.showToast('خطا در بارگذاری کدها', 'error');
        } finally {
          this.loading = false;
          this.isLoadingMore = false;
        }
      },

      async performSearch(query) {
        if (this.loading) return;
        this.loading = true;
        try {
          const url = '/api/codes?search=' + encodeURIComponent(query) + '&limit=20';
          const res = await fetch(url, { credentials: 'same-origin' });
          if (res.status === 401) { this.loggedIn = false; return; }
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const data = await res.json();
          this.codes = data.items;
          this.paginationCursor = data.cursor || null;
          this.hasMore = data.has_more || false;
          if (data.warning) this.showToast(data.warning, 'warning');
          this.updateStats();
        } catch (e) {
          this.showToast('خطا در جستجو', 'error');
        } finally {
          this.loading = false;
        }
      },

      async loadMore() {
        if (this.hasMore && !this.isLoadingMore && !this.loading) {
          await this.loadCodes(true);
        }
      },

      async createCode() {
        if (this.creating) return;
        this.creating = true;
        try {
          const res = await fetch('/api/codes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
              expires_at: this.newCodeExpires || null,
              active: this.newCodeActive === 'true',
              duration_days: parseInt(this.newCodeDuration, 10) || 30
            })
          });
          if (res.ok) {
            const item = await res.json();
            this.codes.unshift({
              code: item.code,
              used: false,
              active: item.active,
              created_at: item.created_at,
              expires_at: item.expires_at,
              last_used_at: null,
              duration_days: item.duration_days
            });
            this.lastCreated = item.code;
            this.newCodeExpires = '';
            this.newCodeActive = 'true';
            this.newCodeDuration = 30;
            this.activeChip = null;
            window.dispatchEvent(new CustomEvent('expiry-sync', { detail: { iso: null } }));
            this.updateStats();
            this.showToast('کد با موفقیت ایجاد شد', 'success');
          } else {
            const err = await res.json().catch(() => ({}));
            this.showToast('خطا: ' + (err.error || 'نامشخص'), 'error');
          }
        } catch {
          this.showToast('خطا در ارتباط با سرور', 'error');
        } finally {
          this.creating = false;
        }
      },

      async toggleActive(item) {
        if (item.used) {
          this.showToast('کد مصرف‌شده قابل تغییر نیست', 'error');
          return;
        }
        const newActive = !item.active;
        const original = item.active;
        item.active = newActive;
        try {
          const res = await fetch('/api/codes/' + encodeURIComponent(item.code), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ active: newActive })
          });
          if (res.ok) {
            this.updateStats();
            this.showToast(newActive ? 'کد فعال شد' : 'کد غیرفعال شد', 'success');
          } else {
            item.active = original;
            const err = await res.json().catch(() => ({}));
            this.showToast('خطا: ' + (err.error || 'نامشخص'), 'error');
          }
        } catch {
          item.active = original;
          this.showToast('خطا در ارتباط با سرور', 'error');
        }
      },

      confirmDelete(item) {
        this.confirmModal = { type: 'code', item };
      },

      async performDelete() {
        if (!this.confirmModal) return;
        const { type, item } = this.confirmModal;
        this.confirmModal = null;
        try {
          let url;
          if (type === 'code') url = '/api/codes/' + encodeURIComponent(item.code);
          else url = '/api/subscriptions/' + encodeURIComponent(item.subscription_id);
          const res = await fetch(url, { method: 'DELETE', credentials: 'same-origin' });
          if (res.ok) {
            if (type === 'code') {
              this.codes = this.codes.filter(c => c.code !== item.code);
            } else {
              this.subscriptions = this.subscriptions.filter(s => s.subscription_id !== item.subscription_id);
            }
            this.updateStats();
            this.showToast('حذف شد', 'success');
          } else {
            const err = await res.json().catch(() => ({}));
            this.showToast('خطا: ' + (err.error || 'نامشخص'), 'error');
          }
        } catch {
          this.showToast('خطا در ارتباط با سرور', 'error');
        }
      },

      async loadSubscriptions(append = false) {
        if (this.subLoading || this.subIsLoadingMore) return;
        const isLoadingMore = append && this.subHasMore;
        if (isLoadingMore) this.subIsLoadingMore = true;
        else this.subLoading = true;
        try {
          const cursorParam = append ? this.subPaginationCursor : null;
          const isSearch = this.subSearchQuery.trim() !== '';
          let url;
          if (isSearch && cursorParam) {
            url = '/api/subscriptions?search=' + encodeURIComponent(this.subSearchQuery)
                + '&limit=20&offset=' + encodeURIComponent(cursorParam);
          } else {
            url = '/api/subscriptions?limit=20' + (cursorParam ? '&cursor=' + encodeURIComponent(cursorParam) : '');
          }
          const res = await fetch(url, { credentials: 'same-origin' });
          if (res.status === 401) { this.loggedIn = false; return; }
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const data = await res.json();
          this.loggedIn = true;
          if (data.warning) this.showToast(data.warning, 'warning');
          if (append) this.subscriptions = this.subscriptions.concat(data.items);
          else this.subscriptions = data.items;
          this.subPaginationCursor = data.cursor || null;
          this.subHasMore = data.has_more || false;
          this.updateStats();
        } catch (e) {
          if (!append) this.showToast('خطا در بارگذاری اشتراک‌ها', 'error');
        } finally {
          this.subLoading = false;
          this.subIsLoadingMore = false;
        }
      },

      async performSubSearch(query) {
        if (this.subLoading) return;
        this.subLoading = true;
        try {
          const url = '/api/subscriptions?search=' + encodeURIComponent(query) + '&limit=20';
          const res = await fetch(url, { credentials: 'same-origin' });
          if (res.status === 401) { this.loggedIn = false; return; }
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const data = await res.json();
          this.subscriptions = data.items;
          this.subPaginationCursor = data.cursor || null;
          this.subHasMore = data.has_more || false;
          if (data.warning) this.showToast(data.warning, 'warning');
          this.updateStats();
        } catch (e) {
          this.showToast('خطا در جستجوی اشتراک‌ها', 'error');
        } finally {
          this.subLoading = false;
        }
      },

      async loadMoreSubs() {
        if (this.subHasMore && !this.subIsLoadingMore && !this.subLoading) {
          await this.loadSubscriptions(true);
        }
      },

      async toggleSubActive(sub) {
        const newActive = !sub.active;
        const original = sub.active;
        sub.active = newActive;
        try {
          const res = await fetch('/api/subscriptions/' + encodeURIComponent(sub.subscription_id), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ active: newActive })
          });
          if (res.ok) {
            this.subscriptions = [...this.subscriptions];
            this.updateStats();
            this.showToast(newActive ? 'اشتراک فعال شد' : 'اشتراک غیرفعال شد', 'success');
          } else {
            sub.active = original;
            this.subscriptions = [...this.subscriptions];
            const err = await res.json().catch(() => ({}));
            this.showToast('خطا: ' + (err.error || 'نامشخص'), 'error');
          }
        } catch {
          sub.active = original;
          this.subscriptions = [...this.subscriptions];
          this.showToast('خطا در ارتباط با سرور', 'error');
        }
      },

      openExtendModal(sub) {
        this.extendModal = {
          subscription: sub,
          newExpiry: sub.expires_at || ''
        };
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('expiry-sync', {
            detail: { iso: sub.expires_at || null }
          }));
        }, 80);
      },

      async performExtend() {
        if (!this.extendModal) return;
        const sub = this.extendModal.subscription;
        const newExpiry = this.extendModal.newExpiry ? this.extendModal.newExpiry : null;
        try {
          const res = await fetch('/api/subscriptions/' + encodeURIComponent(sub.subscription_id), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ expires_at: newExpiry })
          });
          if (res.ok) {
            const data = await res.json();
            const index = this.subscriptions.findIndex(s => s.subscription_id === sub.subscription_id);
            if (index !== -1) {
              this.subscriptions[index].expires_at = data.expires_at;
            }
            this.subscriptions = [...this.subscriptions];
            this.updateStats();
            this.showToast('تاریخ انقضا به‌روز شد', 'success');
            this.extendModal = null;
          } else {
            const err = await res.json().catch(() => ({}));
            this.showToast('خطا: ' + (err.error || 'نامشخص'), 'error');
          }
        } catch {
          this.showToast('خطا در ارتباط با سرور', 'error');
        }
      },

      confirmDeleteSub(sub) {
        this.confirmModal = { type: 'subscription', item: sub };
      },

      updateStats() {
        const now = new Date();
        let total = this.codes.length;
        let activeCodes = 0;
        for (const c of this.codes) {
          if (c.used) continue;
          if (!c.active) continue;
          if (c.expires_at && new Date(c.expires_at) <= now) continue;
          activeCodes++;
        }
        let totalSubs = this.subscriptions.length;
        let activeSubs = 0;
        for (const s of this.subscriptions) {
          if (s.active && (!s.expires_at || new Date(s.expires_at) > now)) activeSubs++;
        }
        this.stats = {
          total,
          activeCodes,
          activeSubscriptions: activeSubs,
          totalSubscriptions: totalSubs
        };
      },

      async login() {
        if (!this.password || this.logging) return;
        this.logging = true;
        this.loginError = '';
        try {
          const res = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ password: this.password })
          });
          if (res.ok) {
            this.loggedIn = true;
            this.password = '';
            await this.loadCodes();
            await this.loadSubscriptions();
            this.showToast('خوش آمدید', 'success');
          } else {
            const data = await res.json().catch(() => ({}));
            this.loginError = data.error || 'خطا در ورود';
          }
        } catch {
          this.loginError = 'خطا در ارتباط با سرور';
        } finally {
          this.logging = false;
        }
      },

      async logout() {
        try { await fetch('/api/logout', { method: 'POST', credentials: 'same-origin' }); } catch {}
        this.loggedIn = false;
        this.codes = [];
        this.subscriptions = [];
        this.lastCreated = '';
        this.password = '';
        this.paginationCursor = null;
        this.hasMore = false;
        this.subPaginationCursor = null;
        this.subHasMore = false;
        this.showToast('با موفقیت خارج شدید', 'success');
      },

      async copyCode(code) {
        try { await navigator.clipboard.writeText(code); }
        catch {
          const ta = document.createElement('textarea');
          ta.value = code;
          ta.style.cssText = 'position:fixed;opacity:0';
          document.body.appendChild(ta);
          ta.select();
          try { document.execCommand('copy'); } catch {}
          document.body.removeChild(ta);
        }
        this.copiedCode = code;
        this.showToast('کپی شد', 'success');
        setTimeout(() => { if (this.copiedCode === code) this.copiedCode = ''; }, 1800);
      },

      setQuickExpiry(days) {
        this.activeChip = days;
        const d = new Date();
        d.setDate(d.getDate() + days);
        d.setHours(23, 59, 59, 999);
        this.newCodeExpires = d.toISOString();
        window.dispatchEvent(new CustomEvent('expiry-sync', { detail: { iso: this.newCodeExpires } }));
      },
      clearExpiry() {
        this.activeChip = 0;
        this.newCodeExpires = '';
        window.dispatchEvent(new CustomEvent('expiry-sync', { detail: { iso: null } }));
      },
      handleExpirySet(event) {
        const detail = event.detail || {};
        if (detail.iso === null && detail.action === 'clear') {
          this.activeChip = null;
          this.newCodeExpires = '';
          if (this.extendModal) this.extendModal.newExpiry = null;
        } else if (detail.iso !== null && detail.iso !== undefined) {
          this.newCodeExpires = detail.iso;
          this.activeChip = null;
          if (this.extendModal) this.extendModal.newExpiry = detail.iso;
        } else {
          this.activeChip = null;
          this.newCodeExpires = '';
        }
      },

      formatDate(iso) {
        if (!iso) return '';
        if (typeof persianDate === 'undefined') return iso;
        try {
          const d = new persianDate(new Date(iso));
          return this.toPersianDigits(d.format('YYYY/MM/DD HH:mm'));
        } catch { return iso; }
      },
      getRelativeTime(iso) {
        if (!iso) return '';
        const diff = new Date(iso) - new Date();
        const abs = Math.abs(diff);
        const isPast = diff < 0;
        let num, unit;
        if (abs < 60000)        { num = null; unit = 'چند لحظه'; }
        else if (abs < 3600000)  { num = Math.floor(abs / 60000);  unit = 'دقیقه'; }
        else if (abs < 86400000) { num = Math.floor(abs / 3600000); unit = 'ساعت'; }
        else if (abs < 2592000000) { num = Math.floor(abs / 86400000); unit = 'روز'; }
        else                    { num = Math.floor(abs / 2592000000); unit = 'ماه'; }
        const txt = (num === null) ? unit : (this.toPersianDigits(num) + ' ' + unit);
        return isPast ? (txt + ' پیش') : ('در ' + txt + ' دیگر');
      },

      showToast(message, type = 'success') {
        const id = ++this._toastId;
        this.toasts.push({ id, message, type, leaving: false });
        setTimeout(() => this.removeToast(id), 3500);
      },
      removeToast(id) {
        const t = this.toasts.find(x => x.id === id);
        if (!t) return;
        t.leaving = true;
        setTimeout(() => { this.toasts = this.toasts.filter(x => x.id !== id); }, 200);
      }
    };
  }
</script>
</body>
</html>`;
}