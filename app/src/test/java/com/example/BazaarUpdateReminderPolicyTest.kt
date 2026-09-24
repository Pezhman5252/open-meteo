package com.example

import com.example.ui.util.BazaarUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the Bazaar update-reminder policy
 * (the only logic that decides WHETHER to show the dialog —
 * the bind/service part is exercised live on device).
 */
class BazaarUpdateReminderPolicyTest {

    private val now = 1_000_000_000_000L
    private val current = 5

    @Test
    fun `no answer from bazaar means no reminder`() {
        // -1 = "no update / app not on bazaar"; 0 = treated as absent
        assertFalse(BazaarUpdateChecker.shouldShowUpdateReminder(-1, current, "", 0L, now))
        assertFalse(BazaarUpdateChecker.shouldShowUpdateReminder(0, current, "", 0L, now))
    }

    @Test
    fun `candidate not newer than installed means no reminder`() {
        assertFalse(BazaarUpdateChecker.shouldShowUpdateReminder(current.toLong(), current, "", 0L, now))
        assertFalse(BazaarUpdateChecker.shouldShowUpdateReminder((current - 1).toLong(), current, "", 0L, now))
    }

    @Test
    fun `newer candidate never shown means reminder`() {
        assertTrue(BazaarUpdateChecker.shouldShowUpdateReminder((current + 1).toLong(), current, "", 0L, now))
    }

    @Test
    fun `dismissed candidate is not repeated within one day`() {
        val dismissed = (current + 1).toLong()
        val oneHourAgo = now - 60 * 60 * 1000L
        assertFalse(
            BazaarUpdateChecker.shouldShowUpdateReminder(dismissed, current, dismissed.toString(), oneHourAgo, now)
        )
    }

    @Test
    fun `dismissed candidate comes back after one full day`() {
        val dismissed = (current + 1).toLong()
        val oneDayPlusOneSec = now - (BazaarUpdateChecker.DAY_MS + 1000L)
        assertTrue(
            BazaarUpdateChecker.shouldShowUpdateReminder(dismissed, current, dismissed.toString(), oneDayPlusOneSec, now)
        )
    }

    @Test
    fun `exactly one day elapsed is enough to re-remind`() {
        val dismissed = (current + 1).toLong()
        val exactlyOneDay = now - BazaarUpdateChecker.DAY_MS
        assertTrue(
            BazaarUpdateChecker.shouldShowUpdateReminder(dismissed, current, dismissed.toString(), exactlyOneDay, now)
        )
    }

    @Test
    fun `a different newer version always reminds even if old one was dismissed yesterday`() {
        // user dismissed v6 yesterday; v7 now -> remind immediately
        val yesterday = now - BazaarUpdateChecker.DAY_MS
        assertTrue(
            BazaarUpdateChecker.shouldShowUpdateReminder(7L, current, "6", yesterday, now)
        )
    }

    @Test
    fun `bigger jump while a smaller one is dismissed still reminds`() {
        // dismissed v6 an hour ago, but market now has v8
        val hourAgo = now - 60 * 60 * 1000L
        assertTrue(
            BazaarUpdateChecker.shouldShowUpdateReminder(8L, current, "6", hourAgo, now)
        )
    }
}
