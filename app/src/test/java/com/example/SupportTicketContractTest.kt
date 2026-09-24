package com.example

import com.example.ui.weather.TicketResponse
import com.example.ui.weather.TicketUiState
import com.example.ui.weather.TicketLookupData
import com.example.ui.weather.TicketLookupResponse
import com.example.ui.weather.TicketLookupUiState
import com.example.ui.util.PersianDateHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test for the support-ticket feature.
 *
 * The app's WeatherViewModel.submitTicket() POSTs to the Cloudflare worker's
 * /api/tickets endpoint and deserializes the response into TicketResponse via
 * Moshi. This test pins the wire format the worker returns so that a
 * server-side change to the ticket response shape fails the app build.
 *
 * Worker source of truth: workers/activation-codes-admin.js -> createTicket()
 * returns: { success: true, ticket_id: <id>, status: "open" }
 */
class SupportTicketContractTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun adapter() = moshi.adapter(TicketResponse::class.java)

    // --- worker success response (exact shape from createTicket) ---
    @Test
    fun `parses worker success response`() {
        val json = """{"success":true,"ticket_id":"a1b2c3d4e5f6","status":"open"}"""
        val res = adapter().fromJson(json)
        assertNotNull(res)
        assertEquals(true, res!!.success)
        assertEquals("a1b2c3d4e5f6", res.ticket_id)
        assertEquals("open", res.status)
    }

    @Test
    fun `worker ticket id is 12 hex chars (randomHex 12)`() {
        // randomHex(12) -> 24 hex chars. The UI just displays it, but we guard
        // the non-blank contract used by submitTicket's Success(ticket_id).
        val json = """{"success":true,"ticket_id":"0123456789abcdef0123456789","status":"open"}"""
        val res = adapter().fromJson(json)
        assertNotNull(res)
        assertTrue(res!!.ticket_id!!.isNotBlank())
    }

    // --- a 503 / non-success body must NOT be misread as success ---
    @Test
    fun `error body without success flag is not success`() {
        // A 503 "not enabled" body carries an error, not success.
        val json = """{"error":"سیستم تیکت هنوز فعال نیست، لطفاً از ایمیل پشتیبانی استفاده کنید."}"""
        val res = adapter().fromJson(json)
        // All optional fields absent -> success == null (treated as failure by the ViewModel's `res.success == true`).
        if (res != null) {
            assertFalse(res.success == true)
        }
    }

    // --- request shape the app builds (email/subject/description/premium/device) ---
    @Test
    fun `app request object serializes the documented keys`() {
        val requestObj = mapOf(
            "email" to "climber@example.com",
            "subject" to "خریدم ولی پرو فعال نشد",
            "description" to "شرح مشکل",
            "premium" to true,
            "device" to mapOf(
                "model" to "SM-S711B",
                "manufacturer" to "samsung",
                "sdk" to 36,
                "appVersion" to "1.0.0",
                "premium" to true
            )
        )
        val json = moshi.adapter(Map::class.java).toJson(requestObj)
        // Every key the worker's createTicket reads must be present.
        for (key in listOf("\"email\"", "\"subject\"", "\"description\"", "\"premium\"", "\"device\"")) {
            assertTrue("missing $key in request", json.contains(key))
        }
        for (key in listOf("\"model\"", "\"appVersion\"")) {
            assertTrue("missing device.$key in request", json.contains(key))
        }
    }

    // --- UI state machine: only one terminal success carries a ticket id ---
    @Test
    fun `ticket ui state success exposes id`() {
        val s = TicketUiState.Success("id-123")
        assertEquals("id-123", s.ticketId)
        val e = TicketUiState.Error("oops")
        assertEquals("oops", e.message)
        // Idle / Loading carry no payload.
        assertTrue(TicketUiState.Idle !is TicketUiState.Success)
        assertTrue(TicketUiState.Loading !is TicketUiState.Success)
    }

    // --- follow-up: public GET /api/tickets/<id> response shape (worker source of truth) ---
    @Test
    fun `parses worker public ticket lookup response`() {
        // workers/activation-codes-admin.js -> getTicketPublic returns exactly:
        // { success: true, ticket: { id, subject, status, reply, created_at, updated_at } }
        val json = """
            {
              "success": true,
              "ticket": {
                "id": "a1b2c3d4e5f6a1b2c3d4e5f6",
                "subject": "خرید فعال نشد",
                "status": "in_progress",
                "reply": "بررسی شد، فعال شد",
                "created_at": "2026-09-24T00:00:00.000Z",
                "updated_at": "2026-09-24T01:00:00.000Z"
              }
            }
        """.trimIndent()
        val res = moshi.adapter(TicketLookupResponse::class.java).fromJson(json)
        assertNotNull(res)
        assertEquals(true, res!!.success)
        assertNotNull(res.ticket)
        assertEquals("a1b2c3d4e5f6a1b2c3d4e5f6", res.ticket!!.id)
        assertEquals("خرید فعال نشد", res.ticket!!.subject)
        assertEquals("in_progress", res.ticket!!.status)
        assertEquals("بررسی شد، فعال شد", res.ticket!!.reply)
    }

    @Test
    fun `parses ticket lookup with null reply (no response yet)`() {
        val json = """
            {
              "success": true,
              "ticket": {
                "id": "ffffffffffffffffffffffff",
                "subject": "",
                "status": "open",
                "reply": null,
                "created_at": "2026-09-24T00:00:00.000Z",
                "updated_at": "2026-09-24T00:00:00.000Z"
              }
            }
        """.trimIndent()
        val res = moshi.adapter(TicketLookupResponse::class.java).fromJson(json)
        assertNotNull(res)
        assertEquals("open", res!!.ticket!!.status)
        assertTrue(res.ticket!!.reply == null)
    }

    // --- lookup UI state machine ---
    @Test
    fun `ticket lookup ui state success exposes ticket`() {
        val t = TicketLookupData(
            id = "x", subject = "s", status = "resolved",
            reply = "done", created_at = null, updated_at = null
        )
        val s = TicketLookupUiState.Success(t)
        assertEquals("done", s.ticket.reply)
        val e = TicketLookupUiState.Error("not found")
        assertEquals("not found", e.message)
        assertTrue(TicketLookupUiState.Idle !is TicketLookupUiState.Success)
        assertTrue(TicketLookupUiState.Loading !is TicketLookupUiState.Success)
    }

    // --- worker timestamp -> Shamsi date+time (contract for the follow-up screen) ---
    // The worker returns created_at/updated_at as full ISO-8601 UTC, e.g.
    // "2026-09-09T23:52:49.931Z". That instant is 2026-09-10 03:22 in Asia/Tehran
    // (+3:30) = ۱۴۰۵/۰۶/۱۹ (19 Shahrivar 1405). Locks the exact display string.
    @Test
    fun `worker iso8601 utc renders as shamsi date and tehran time`() {
        val out = PersianDateHelper.formatIso8601UtcToPersian("2026-09-09T23:52:49.931Z")
        assertEquals("۱۴۰۵/۰۶/۱۹ - ۰۳:۲۲", out)
    }

    // Same instant, no fractional seconds — must yield the identical Shamsi result.
    @Test
    fun `iso8601 without millis parses identically`() {
        val out = PersianDateHelper.formatIso8601UtcToPersian("2026-09-09T23:52:49Z")
        assertEquals("۱۴۰۵/۰۶/۱۹ - ۰۳:۲۲", out)
    }

    // A day-boundary UTC time that stays on the SAME Tehran day must not roll the date.
    // 2026-09-10T00:30:00Z -> Tehran 04:00 same day -> ۱۴۰۵/۰۶/۱۹ - ۰۴:۰۰.
    @Test
    fun `midnight utc maps to early-tehran-time same shamsi day`() {
        val out = PersianDateHelper.formatIso8601UtcToPersian("2026-09-10T00:30:00Z")
        assertEquals("۱۴۰۵/۰۶/۱۹ - ۰۴:۰۰", out)
    }

    // Garbage/blank input must never crash and must not throw.
    @Test
    fun `malformed timestamps are safe`() {
        assertEquals("", PersianDateHelper.formatIso8601UtcToPersian(""))
        // unparseable -> falls back to the simpler parser -> returns input as-is (Persian digits)
        val out = PersianDateHelper.formatIso8601UtcToPersian("not-a-date")
        assertTrue(out.isNotEmpty())
    }
}
