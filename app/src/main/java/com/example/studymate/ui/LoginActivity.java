package com.example.studymate.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.example.studymate.databinding.ActivityLoginBinding;
import com.example.studymate.firebase.FirebaseSync;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding b;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Cloud Sync Sign In", true);

        mAuth = FirebaseAuth.getInstance();

        b.btnLogin.setOnClickListener(v -> doLogin());
        b.btnRegister.setOnClickListener(v -> doRegister());
        b.btnAnonymous.setOnClickListener(v -> doAnonymous());
        b.btnSkip.setOnClickListener(v -> {
            com.example.studymate.util.Prefs.putBool(this, "skip_login_prompt", true);
            UiUtil.toast(this, "Using StudyMate in Offline Mode");
            finish();
        });
    }

    private void setLoading(boolean loading) {
        b.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.btnLogin.setEnabled(!loading);
        b.btnRegister.setEnabled(!loading);
        b.btnAnonymous.setEnabled(!loading);
    }

    private void doLogin() {
        String email = FormDialog.str(b.etEmail);
        String password = FormDialog.str(b.etPassword);

        if (TextUtils.isEmpty(email)) {
            b.etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            b.etPassword.setError("Password required");
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    com.example.studymate.util.Prefs.putBool(this, "skip_login_prompt", false);
                    UiUtil.toast(this, "Signed in successfully");
                    pullDataAndFinish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    UiUtil.toast(this, "Sign in failed: " + parseError(e));
                });
    }

    private void doRegister() {
        String email = FormDialog.str(b.etEmail);
        String password = FormDialog.str(b.etPassword);

        if (TextUtils.isEmpty(email)) {
            b.etEmail.setError("Email required");
            return;
        }
        if (password.length() < 6) {
            b.etPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    com.example.studymate.util.Prefs.putBool(this, "skip_login_prompt", false);
                    UiUtil.toast(this, "Account created!");
                    FirebaseSync.pushToCloud(this, null);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    UiUtil.toast(this, "Registration failed: " + parseError(e));
                });
    }

    private void doAnonymous() {
        setLoading(true);
        mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    com.example.studymate.util.Prefs.putBool(this, "skip_login_prompt", false);
                    UiUtil.toast(this, "Signed in as Guest");
                    FirebaseSync.pushToCloud(this, null);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    UiUtil.toast(this, "Guest sign in failed: " + parseError(e));
                });
    }

    private String parseError(Exception e) {
        if (e == null) return "Unknown error occurred";
        String msg = e.getMessage();
        if (msg != null && msg.contains("disabled")) {
            return "Email/Password or Guest sign-in is disabled in your Firebase Console. Please enable it under Authentication -> Sign-in method.";
        }
        return msg;
    }

    private void pullDataAndFinish() {
        FirebaseSync.pullFromCloud(this, new FirebaseSync.SyncCallback() {
            @Override
            public void onSuccess() {
                UiUtil.toast(LoginActivity.this, "Cloud data synced successfully");
                finish();
            }

            @Override
            public void onError(String error) {
                FirebaseSync.pushToCloud(LoginActivity.this, null);
                finish();
            }
        });
    }
}
