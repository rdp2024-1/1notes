package com.notes.satu

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object LockManager {
    // PIN disimpan sederhana; ganti ke EncryptedSharedPreferences untuk produksi
    private const val PREF = "1notes_lock"
    private const val KEY_PIN = "pin"

    fun setPin(activity: FragmentActivity, pin: String) {
        activity.getSharedPreferences(PREF, 0)
            .edit().putString(KEY_PIN, pin).apply()
    }

    fun hasPin(activity: FragmentActivity) =
        activity.getSharedPreferences(PREF, 0).getString(KEY_PIN, null) != null

    fun unlockWithBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val manager = BiometricManager.from(activity)
        if (manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            unlockWithPin(activity, onSuccess)
            return
        }
        val prompt = BiometricPrompt(activity,
            androidx.core.content.ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka catatan terkunci")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    fun unlockWithPin(activity: FragmentActivity, onSuccess: () -> Unit) {
        val input = android.widget.EditText(activity).apply {
            hint = "Masukkan PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        android.app.AlertDialog.Builder(activity)
            .setTitle("Catatan Terkunci")
            .setView(input)
            .setPositiveButton("Buka") { _, _ ->
                val saved = activity.getSharedPreferences(PREF, 0)
                    .getString(KEY_PIN, null)
                if (input.text.toString() == saved) onSuccess()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
