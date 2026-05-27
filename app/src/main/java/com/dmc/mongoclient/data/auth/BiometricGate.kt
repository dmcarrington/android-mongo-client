package com.dmc.mongoclient.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [BiometricPrompt]. Uses BIOMETRIC_WEAK + DEVICE_CREDENTIAL
 * as the allowed authenticators — that combination is supported on our minSdk
 * (26+) via the AndroidX biometric library, and falls back to the device PIN /
 * pattern / password when no biometric is enrolled.
 */
@Singleton
class BiometricGate @Inject constructor() {

    enum class Availability { AVAILABLE, NO_HARDWARE, NONE_ENROLLED, UNKNOWN }

    fun availability(context: Context): Availability {
        val mgr = BiometricManager.from(context)
        return when (mgr.canAuthenticate(ALLOWED)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNKNOWN
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // ERROR_USER_CANCELED / ERROR_NEGATIVE_BUTTON arrive here too;
                // treat them as soft errors — caller decides whether to retry.
                onError(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED)
            // Note: must NOT call setNegativeButtonText() when DEVICE_CREDENTIAL
            // is in the authenticator set — the library throws IAE.
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        const val ALLOWED = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
    }
}
