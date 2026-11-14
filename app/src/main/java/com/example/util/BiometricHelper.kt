package com.example.util

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import java.util.concurrent.Executor

/**
 * Fingerprint unlock for the vault and App Lock (v1.17.0, repaired in v1.18.0).
 *
 * Uses the FRAMEWORK BiometricPrompt (android.hardware.biometrics) — no new
 * Gradle dependency. Everything is lazy: no prompt ever appears unless the user
 * taps the fingerprint option, and the PIN always stays available as a fallback
 * (no hardware, nothing enrolled, or a cancelled scan all just mean "use PIN").
 *
 * One prompt path for every API 30+ device: BiometricPrompt.Builder, called
 * directly. The Builder API has existed since API 30 — v1.17.0 wrongly assumed
 * it was new in API 36 and reached the classic constructor + PromptInfo by
 * reflection on API 30-35, and that reflection path threw on the user's
 * Android 12 (API 31) phone: the button showed (isAvailable true) but tapping
 * it failed with the generic "not available" message. The reflection is gone;
 * if the Builder call itself ever fails, the error now names the actual
 * exception (e.g. "could not start (IllegalStateException)") so the cause can
 * be reported back instead of guessed at.
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
                if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                    errorCode == ERROR_USER_CANCELED ||
                    errorCode == ERROR_NEGATIVE_BUTTON ||
                    errorCode == ERROR_CANCELED
                ) return
                onError(messageForError(errorCode, errString))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Not recognized — try again.")
            }
        }
        try {
            authenticateWithBuilder(activity, title, subtitle, executor, callback)
        } catch (e: Exception) {
            // Name the failure instead of the generic "not available": the
            // exception class is what lets the next fix target the real cause.
            onError("Fingerprint could not start (${e.javaClass.simpleName}).")
        }
    }

    /**
     * Turns a framework error code into a message the user can act on.
     * Not-enrolled and no-sensor are told apart from a sensor that is busy or
     * locked out; anything unrecognised falls back to the system's own text.
     */
    private fun messageForError(errorCode: Int, errString: CharSequence): String {
        return when (errorCode) {
            ERROR_NO_BIOMETRICS ->
                "No fingerprint enrolled. Add one in Settings first."
            ERROR_HW_NOT_PRESENT ->
                "This device has no fingerprint sensor."
            ERROR_HW_UNAVAILABLE ->
                "Fingerprint sensor is not available right now."
            ERROR_LOCKOUT ->
                "Too many attempts. Try again in a while."
            ERROR_LOCKOUT_PERMANENT ->
                "Fingerprint is locked. Use your PIN."
            ERROR_SECURITY_UPDATE_REQUIRED ->
                "A security update is required for fingerprint."
            else -> {
                val systemText = errString.toString().trim()
                if (systemText.isNotEmpty()) systemText else "Fingerprint error. Use your PIN."
            }
        }
    }

    /** The Builder is the supported way to make a prompt on API 30 and above. */
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

    // Framework biometric error codes. They were removed from the API 36 stubs
    // along with the classic API, so they are inlined here; the values are part
    // of the stable public API and never change.
    private const val ERROR_HW_UNAVAILABLE = 1
    private const val ERROR_CANCELED = 5
    private const val ERROR_LOCKOUT = 7
    private const val ERROR_LOCKOUT_PERMANENT = 9
    private const val ERROR_USER_CANCELED = 10
    private const val ERROR_NO_BIOMETRICS = 11
    private const val ERROR_HW_NOT_PRESENT = 12
    private const val ERROR_NEGATIVE_BUTTON = 13
    private const val ERROR_SECURITY_UPDATE_REQUIRED = 15
}
