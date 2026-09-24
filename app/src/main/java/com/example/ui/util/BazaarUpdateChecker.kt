package com.example.ui.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.farsitel.bazaar.IUpdateCheckService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/**
 * «بررسی به‌روزبودن برنامه» طبق راهنمای رسمی کافه‌بازار
 * (developers.cafebazaar.ir/fa/guidelines/feature/update-check):
 * به سرویس UpdateCheckService بازار bind می‌شویم و آخرین versionCode
 * این اپ در بازار را می‌پرسیم. بازار در صورتی که نسخه‌ی جدیدتر موجود
 * باشد آن versionCode را برمی‌گرداند، وگرنه 1- (منفی یک).
 *
 * قواعد صفر-ریسک:
 *  - اگر بازار نصب نباشد، bind جواب ندهد یا اپ هنوز در بازار نباشد
 *    (همیشه 1- برمی‌گردد) -> null برمی‌گردد و اپ بی‌صدا می‌ماند.
 *  - هیچ toast/خطایی به کاربر نشان داده نمی‌شود؛ تصمیم نمایش با
 *    shouldShowUpdateReminder (سیاست «یک بار در روز / تا دفعه‌ی بعد»).
 */
object BazaarUpdateChecker {

    private const val BIND_ACTION = "com.farsitel.bazaar.service.UpdateCheckService.BIND"
    private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
    private const val BIND_TIMEOUT_MS = 5_000L

    const val DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * آخرین versionCode موجود در بازار را برمی‌گرداند؛ null = بدون پاسخ
     * (بازار نصب نیست / bind شکست خورد / تایم‌اوت). مقدار برگشتی <= 0
     * یعنی بازار «بروزرسانی موجود نیست» (همیشه 1-).
     */
    suspend fun checkLatestVersionCode(activity: Activity, packageName: String): Long? {
        if (!BazaarBillingManager.isBazaarInstalled(activity)) return null

        val result = java.util.concurrent.atomic.AtomicReference<Long?>()
        val done = CompletableDeferred<Unit>()
        val intent = Intent(BIND_ACTION).setPackage(BAZAAR_PACKAGE)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                try {
                    val service = IUpdateCheckService.Stub.asInterface(binder)
                    result.set(service.getVersionCode(packageName))
                } catch (e: Exception) {
                    result.set(null)
                } finally {
                    done.complete(Unit)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                // nothing to clean — unbind happens in the caller's finally
            }
        }

        val bound = try {
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            false
        }
        if (!bound) return null

        try {
            withTimeout(BIND_TIMEOUT_MS) { done.await() }
        } catch (e: Exception) {
            // timeout -> treat as no answer (silent)
        } finally {
            try {
                activity.unbindService(connection)
            } catch (_: Exception) {
            }
        }
        return result.get()
    }

    /**
     * سیاست نمایش یادآور (خالص — تست‌پذیر):
     * فقط اگر بازار واقعاً نسخه‌ی جدیدتری دارد و کاربر هنوز همین نسخه‌ی
     * جدید را ندیده (یا بیش از یک روز از آخرین نمایش گذشته باشد).
     *
     * @param candidateVersionCode آخرین versionCode بازار (<= 0 یعنی نبود)
     * @param currentVersionCode نسخه‌ی نصب‌شده روی دستگاه
     * @param dismissedVersionCode آخرین versionCode‌ای که به کاربر پیشنهاد شد (به‌عنوان string)
     * @param lastShownAtMillis زمان آخرین نمایش یادآور
     * @param nowMillis زمان فعلی
     */
    fun shouldShowUpdateReminder(
        candidateVersionCode: Long,
        currentVersionCode: Int,
        dismissedVersionCode: String,
        lastShownAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (candidateVersionCode <= 0) return false
        if (candidateVersionCode <= currentVersionCode.toLong()) return false
        if (dismissedVersionCode == candidateVersionCode.toString()) {
            // همین نسخه قبلاً پیشنهاد شده؛ فقط بعد از یک روز دوباره
            return nowMillis - lastShownAtMillis >= DAY_MS
        }
        return true
    }
}
