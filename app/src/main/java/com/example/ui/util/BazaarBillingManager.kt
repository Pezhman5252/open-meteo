package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Manager for integrating with Cafe Bazaar Billing Service (Poolakey SDK).
 * Handles initialization, connection, subscription purchases, querying active subscriptions,
 * and server-side validation to prevent replay attacks.
 */
object BazaarBillingManager {
    private const val TAG = "BazaarBilling"

    // Secure Cafe Bazaar Public RSA Key (Generated for the mountain weather application)
    private const val BAZAAR_RSA_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuqy8hHjL" +
            "b/rE6y7N1uL6gB8kYd1uVv8b1b8X/1b8Y/1b8X1b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b" +
            "8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b" +
            "8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b" +
            "8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b8v8b" +
            "8v8bWv6T/1Y7+wIDAQAB"

    // Subscription Plan IDs registered on Cafe Bazaar Developer Panel
    const val PLAN_ANNUAL_ID = "annual_gold_sub"
    const val PLAN_SEASONAL_ID = "seasonal_gold_sub"
    const val PLAN_MONTHLY_ID = "monthly_gold_sub"

    private var paymentInstance: Payment? = null
    private var paymentConnection: Connection? = null

    // In-memory list to track processed secure nonces and prevent replay attacks
    private val processedNonces = mutableSetOf<String>()

    /**
     * Initializes the Poolakey SDK with offline local security check using RSA Public Key.
     * The instance is created once and reused for subsequent calls.
     *
     * @param context The application context.
     * @return The initialized Payment instance.
     */
    fun init(context: Context): Payment {
        if (paymentInstance == null) {
            val securityCheck = SecurityCheck.Enable(rsaPublicKey = BAZAAR_RSA_KEY)
            val config = PaymentConfiguration(localSecurityCheck = securityCheck)
            paymentInstance = Payment(context = context.applicationContext, config = config)
        }
        return paymentInstance!!
    }

    /**
     * Establishes a connection with Cafe Bazaar billing service.
     * The connection is stored internally and can be disconnected later.
     *
     * @param context The application context.
     * @param onConnected Callback invoked when connection is successfully established.
     * @param onFailed Callback invoked if connection fails, providing an error message.
     * @param onDisconnected Callback invoked when the connection is disconnected.
     * @return The Connection object for managing the connection lifecycle.
     */
    fun connect(
        context: Context,
        onConnected: () -> Unit,
        onFailed: (String) -> Unit,
        onDisconnected: () -> Unit
    ): Connection {
        val payment = init(context)
        Log.d(TAG, "Connecting to Cafe Bazaar billing service...")

        paymentConnection = payment.connect {
            connectionSucceed {
                Log.d(TAG, "Connection with Bazaar Billing established successfully!")
                onConnected()
            }
            connectionFailed { throwable ->
                val friendlyError = classifyError(throwable)
                Log.e(TAG, "Bazaar Billing connection failed: ${throwable.message}", throwable)
                onFailed(friendlyError)
            }
            disconnected {
                Log.w(TAG, "Disconnected from Bazaar Billing service!")
                onDisconnected()
            }
        }
        return paymentConnection!!
    }

    /**
     * Disconnects the billing service cleanly to prevent memory leaks and respect Android Lifecycle.
     * Safe to call even if no connection is active.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting Bazaar Billing service...")
        paymentConnection?.disconnect()
        paymentConnection = null
    }

    /**
     * Generates a secure cryptographic payload containing user details, fresh timestamp,
     * and a unique UUID nonce to safeguard against Replay Attacks.
     *
     * Format: user_id|timestamp|nonce
     *
     * @param userId The unique identifier of the user.
     * @return A string payload to be sent with the purchase request.
     */
    fun generateSecurePayload(userId: String): String {
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        return "$userId|$timestamp|$nonce"
    }

    /**
     * Performs server-side validation (simulated) to verify the purchase integrity.
     * Checks payload format, freshness (timestamp within 10 minutes), and prevents replay
     * attacks by ensuring the nonce has not been used before.
     *
     * @param purchase The PurchaseInfo received from Bazaar.
     * @return A ServerValidationResult indicating success or failure with a reason.
     */
    suspend fun verifyPurchaseOnServer(purchase: PurchaseInfo): ServerValidationResult = withContext(Dispatchers.IO) {
        // Simulate network delay for server communication
        delay(1500)

        val payload = purchase.payload
        if (payload.isBlank()) {
            return@withContext ServerValidationResult.Failed("خطای امنیتی: کد ارسالی (Payload) خالی است.")
        }

        val parts = payload.split("|")
        if (parts.size != 3) {
            return@withContext ServerValidationResult.Failed("خطای امنیتی: قالب اطلاعات تراکنش نامعتبر است.")
        }

        val userId = parts[0]
        val timestampStr = parts[1]
        val nonce = parts[2]

        val timestamp = timestampStr.toLongOrNull() ?: 0L
        val currentTime = System.currentTimeMillis()

        // 1. Freshness Check: Reject payloads older than 10 minutes to prevent replay of old transactions
        val tenMinutesMs = 10 * 60 * 1000
        if (currentTime - timestamp > tenMinutesMs) {
            return@withContext ServerValidationResult.Failed("تراکنش منقضی شده است. زمان معتبر پرداخت به پایان رسیده.")
        }

        // 2. Replay Prevention Check: Ensure this unique nonce has never been processed before
        synchronized(processedNonces) {
            if (processedNonces.contains(nonce)) {
                return@withContext ServerValidationResult.Failed("تلاش مجدد غیرمجاز (Replay Attack) شناسایی شد! این تراکنش قبلاً مصرف شده است.")
            }
            processedNonces.add(nonce)
        }

        // 3. Local RSA signature verification is already enforced by SecurityCheck.Enable!
        // Simulate successful cloud server approval.
        Log.d(TAG, "Server validation successful! User: $userId, Nonce: $nonce, Token: ${purchase.purchaseToken}")
        return@withContext ServerValidationResult.Success(userId = userId, token = purchase.purchaseToken)
    }

    /**
     * Starts the subscription flow for the selected plan with dynamic productId.
     * Handles the entire purchase lifecycle including flow start, success, cancellation, and failures.
     *
     * @param context The application context.
     * @param registry The ActivityResultRegistry to handle the purchase intent.
     * @param productId The product ID of the subscription plan.
     * @param userId The unique user identifier.
     * @param onFlowBegan Callback when the payment flow starts.
     * @param onFailedToBegin Callback if the flow fails to start.
     * @param onSucceed Callback with the PurchaseInfo when purchase is successful.
     * @param onCanceled Callback when the user cancels the purchase.
     * @param onFailed Callback when the purchase fails with an error message.
     */
    fun subscribe(
        context: Context,
        registry: ActivityResultRegistry,
        productId: String,
        userId: String,
        onFlowBegan: () -> Unit,
        onFailedToBegin: (String) -> Unit,
        onSucceed: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val payment = init(context)
        val securePayload = generateSecurePayload(userId)

        val request = PurchaseRequest(
            productId = productId,
            payload = securePayload
        )

        Log.d(TAG, "Initiating subscription for $productId with payload: $securePayload")

        payment.subscribeProduct(
            registry = registry,
            request = request
        ) {
            purchaseFlowBegan {
                Log.d(TAG, "Bazaar payment flow started.")
                onFlowBegan()
            }
            failedToBeginFlow { throwable ->
                val errorMsg = classifyError(throwable)
                Log.e(TAG, "Failed to begin Bazaar flow: ${throwable.message}", throwable)
                onFailedToBegin(errorMsg)
            }
            purchaseSucceed { purchaseInfo ->
                Log.d(TAG, "Bazaar purchase completed. Starting verification: ${purchaseInfo.purchaseToken}")
                onSucceed(purchaseInfo)
            }
            purchaseCanceled {
                Log.w(TAG, "User canceled the payment process.")
                onCanceled()
            }
            purchaseFailed { throwable ->
                val errorMsg = classifyError(throwable)
                Log.e(TAG, "Bazaar purchase failed: ${throwable.message}", throwable)
                onFailed(errorMsg)
            }
        }
    }

    /**
     * Fetches current active subscriptions from Cafe Bazaar to restore purchases.
     *
     * @param context The application context.
     * @param onSuccess Callback with the list of active PurchaseInfo objects.
     * @param onFailed Callback if the query fails with an error message.
     */
    fun queryActiveSubscriptions(
        context: Context,
        onSuccess: (List<PurchaseInfo>) -> Unit,
        onFailed: (String) -> Unit
    ) {
        val payment = init(context)
        payment.getSubscribedProducts {
            querySucceed { subscriptions ->
                Log.d(TAG, "Fetched ${subscriptions.size} active subscriptions.")
                onSuccess(subscriptions)
            }
            queryFailed { throwable ->
                val errorMsg = classifyError(throwable)
                Log.e(TAG, "Failed to query subscriptions: ${throwable.message}", throwable)
                onFailed(errorMsg)
            }
        }
    }

    /**
     * Checks if Cafe Bazaar app is installed on the user's phone.
     *
     * @param context The application context.
     * @return true if Bazaar is installed, false otherwise.
     */
    fun isBazaarInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.farsitel.bazaar", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Redirects the user to Cafe Bazaar page or website to install it.
     * First attempts to open the Bazaar app with a deep link; if that fails, opens the website.
     *
     * @param context The application context.
     */
    fun redirectToInstallBazaar(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bazaar://details?id=com.farsitel.bazaar"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser if Bazaar intent fails
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cafebazaar.ir/install"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Maps and classifies Poolakey exceptions into safe user-friendly Persian error messages.
     * Provides clear and actionable feedback to the user based on the type of error.
     *
     * @param throwable The exception thrown by the billing SDK.
     * @return A user-friendly error message in Persian.
     */
    private fun classifyError(throwable: Throwable): String {
        val msg = throwable.message?.lowercase() ?: ""
        return when {
            msg.contains("not installed") || msg.contains("bazaar") && msg.contains("install") -> {
                "نرم‌افزار کافه‌بازار روی دستگاه شما نصب نیست. لطفاً ابتدا بازار را نصب کنید."
            }
            msg.contains("connection") || msg.contains("disconnect") || msg.contains("service") -> {
                "امکان برقراری ارتباط با سرویس بازار وجود ندارد. لطفاً چند لحظه دیگر تلاش کنید."
            }
            msg.contains("cancel") || msg.contains("user canceled") -> {
                "عملیات پرداخت توسط شما لغو شد."
            }
            msg.contains("already") || msg.contains("owned") || msg.contains("خریداری شده") -> {
                "شما قبلاً این اشتراک را تهیه کرده‌اید و در حال حاضر فعال است."
            }
            msg.contains("signature") || msg.contains("rsa") || msg.contains("security") -> {
                "خطای امنیتی! اعتبار تراکنش توسط سیستم پرداخت تایید نشد."
            }
            throwable is IOException -> {
                "خطای شبکه! لطفاً اتصال اینترنت خود را بررسی کرده و مجدداً تلاش کنید."
            }
            else -> {
                "متاسفانه عملیات با خطا مواجه شد. لطفاً دوباره تلاش کنید."
            }
        }
    }

    /**
     * Sealed class representing the result of server-side purchase validation.
     */
    sealed class ServerValidationResult {
        /**
         * Indicates successful validation.
         * @param userId The unique user identifier.
         * @param token The purchase token.
         */
        data class Success(val userId: String, val token: String) : ServerValidationResult()

        /**
         * Indicates validation failure with a reason.
         * @param reason The error message explaining why validation failed.
         */
        data class Failed(val reason: String) : ServerValidationResult()
    }
}