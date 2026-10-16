package com.example.studymate.util;

import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/** Optional biometric / device-credential lock used by the Notes screen. */
public final class BiometricGate {
    private BiometricGate() {}

    public static void authenticate(FragmentActivity a, String title, Runnable onSuccess, Runnable onFail) {
        int auth = Build.VERSION.SDK_INT >= 30
                ? (BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                : BiometricManager.Authenticators.BIOMETRIC_WEAK;
        if (BiometricManager.from(a).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(a, "No biometric/screen lock set up on this device - lock skipped", Toast.LENGTH_LONG).show();
            onSuccess.run();
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt(a, ContextCompat.getMainExecutor(a),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        onSuccess.run();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        onFail.run();
                    }
                });
        BiometricPrompt.PromptInfo.Builder info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(auth);
        if (Build.VERSION.SDK_INT < 30) info.setNegativeButtonText("Cancel");
        prompt.authenticate(info.build());
    }
}
