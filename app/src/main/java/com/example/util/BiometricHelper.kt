package com.example.util

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import java.util.concurrent.Executor

/**
 * Fingerprint unlock for the vault and App Lock (v1.17.0).
 *
 * Uses the FRAMEWORK BiometricPrompt (android.hardware.biometrics) — no new
 * Gradle dependency. Everything is lazy: no prompt ever appears unless the user
 * taps the fingerprint option, and the PIN always stays available as a fallback
 * (no hardware, nothing enrolled, or a cancelled scan all just mean "use PIN").
 *
 * Two prompt paths, because the framework API changed in API 36:
 * - API 36+: the new BiometricPrompt.Builder, called directly.
 * - API 30-35: the classic (Activity, Executor, callback) constructor plus
 *   PromptInfo. Those members were REMOVED from the API 36 compile stubs, so
 *   they are reached by reflection — but they still exist on every API 29-35
 *   device, including this user's Android 12 phone. All reflection is wrapped
 *   in try/catch: if anything is missing, the fingerprint option just fails
 *   closed with a short message and the PIN stays available.
 *
 * API guards (minSdk is 24): canAuthenticate(int) is API 30+, so below API 30
 * the fingerprint option simply never shows.
 */
object BiometricHelper {

    /** True when a biometric prompt can actually run right now. */
    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system fingerprint prompt. Calls [onSuccess] on a good scan.
     * Calls [onError] with a short message on a hard failure; a user cancel (or
     * tapping "Use PIN") calls neither — the PIN dialog just stays up.
     * No-op when [activity] is null or the API level is below 30.
     */
    fun authenticate(
        activity: Activity?,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val executor = activity.mainExecutor
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // The user backing out is not an error — the PIN is right there.
                // API 36 renumbered the error codes (BIOMETRIC_ERROR_*); the
                // classic ERROR_* values still arrive on API 30-35 devices.
                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                    errorCode != ERROR_USER_CANCELED &&
                    errorCode != ERROR_NEGATIVE_BUTTON &&
                    errorCode != ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Not recognized — try again.")
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= API_36) {
                authenticateWithBuilder(activity, title, subtitle, executor, callback)
            } else {
                authenticateClassic(activity, title, subtitle, executor, callback)
            }
        } catch (e: Exception) {
            onError("Fingerprint is not available on this device.")
        }
    }

    /** API 36+: the Builder is the only public way to make a prompt. */
    private fun authenticateWithBuilder(
        activity: Activity,
        title: String,
        subtitle: String,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback
    ) {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButton("Use PIN", executor) { _, _ -> /* back to the PIN */ }
            .build()
        prompt.authenticate(CancellationSignal(), executor, callback)
    }

    /**
     * API 30-35: the classic constructor + PromptInfo, via reflection, because
     * the API 36 stubs dropped them. They exist on every API 29-35 device.
     */
    private fun authenticateClassic(
        activity: Activity,
        title: String,
        subtitle: String,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback
    ) {
        val promptClass = BiometricPrompt::class.java
        val prompt = promptClass
            .getConstructor(
                Activity::class.java,
                Executor::class.java,
                BiometricPrompt.AuthenticationCallback::class.java
            )
            .newInstance(activity, executor, callback)
        val builderClass =
            Class.forName("android.hardware.biometrics.BiometricPrompt\$PromptInfo\$Builder")
        val builder = builderClass.getConstructor().newInstance()
        builderClass.getMethod("setTitle", CharSequence::class.java).invoke(builder, title)
        builderClass.getMethod("setSubtitle", CharSequence::class.java).invoke(builder, subtitle)
        builderClass.getMethod("setNegativeButtonText", CharSequence::class.java)
            .invoke(builder, "Use PIN")
        val promptInfo = builderClass.getMethod("build").invoke(builder)
        promptClass.getMethod(
            "authenticate",
            Class.forName("android.hardware.biometrics.BiometricPrompt\$PromptInfo")
        ).invoke(prompt, promptInfo)
    }

    // API 36 (Android 16, Baklava). Kept as a literal: the named constant only
    // exists in the newest stubs, and the number is unambiguous.
    private const val API_36 = 36

    // Classic framework error codes (API 29-35). They were removed from the API
    // 36 stubs along with the classic API, so they are inlined here; the values
    // are part of the stable public API and never change.
    private const val ERROR_CANCELED = 5
    private const val ERROR_USER_CANCELED = 10
    private const val ERROR_NEGATIVE_BUTTON = 13
}
