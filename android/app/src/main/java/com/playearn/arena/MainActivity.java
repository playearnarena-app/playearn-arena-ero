package com.playearn.arena;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private WebView webView;
    private SharedPreferences sessionPrefs;
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private final class SessionBridge {
        @JavascriptInterface public String getToken() {
            return sessionPrefs.getString("api_token", "");
        }
        @JavascriptInterface public void setToken(String token) {
            if (token != null && token.length() >= 32 && token.length() <= 256) {
                sessionPrefs.edit().putString("api_token", token).apply();
            }
        }
        @JavascriptInterface public void clearToken() {
            sessionPrefs.edit().remove("api_token").apply();
        }
    }

    private final class GoogleBridge {
        @JavascriptInterface public void signIn() {
            runOnUiThread(() -> startGoogleSignIn());
        }
    }

    private void startGoogleSignIn() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID == null || BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            notifyGoogleError("Google Sign-In is not configured yet.");
            return;
        }
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, options);
        startActivityForResult(client.getSignInIntent(), RC_GOOGLE_SIGN_IN);
    }

    private void notifyGoogleError(String message) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                    "window.onNativeGoogleSignInError && window.onNativeGoogleSignInError(" + JSONObject.quote(message) + ");", null));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_GOOGLE_SIGN_IN) return;
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            String idToken = account == null ? null : account.getIdToken();
            if (idToken == null || idToken.isEmpty()) {
                notifyGoogleError("Google did not return a sign-in token. Check OAuth setup.");
                return;
            }
            if (webView != null) {
                webView.post(() -> webView.evaluateJavascript(
                        "window.onNativeGoogleSignInSuccess && window.onNativeGoogleSignInSuccess(" + JSONObject.quote(idToken) + ");", null));
            }
        } catch (ApiException e) {
            String message = e.getStatusCode() == 12501 ? "Google sign-in cancelled." :
                    "Google sign-in failed (code " + e.getStatusCode() + "). Check Android OAuth package and SHA-1 settings.";
            notifyGoogleError(message);
        } catch (Exception e) {
            notifyGoogleError("Google sign-in could not be completed. Please try again.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionPrefs = getSharedPreferences("playearn_session", MODE_PRIVATE);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 3, 26));
        window.setNavigationBarColor(Color.rgb(7, 3, 26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 3, 26));

        TextView notice = new TextView(this);
        notice.setText("BETA TEST • Rewards shown in this preview are simulations. No real earnings or cash payouts.");
        notice.setTextColor(Color.WHITE);
        notice.setTextSize(11);
        notice.setPadding(14, 10, 14, 10);
        notice.setBackgroundColor(Color.rgb(54, 35, 105));
        root.addView(notice, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new SessionBridge(), "AndroidSession");
        webView.addJavascriptInterface(new GoogleBridge(), "AndroidGoogle");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if ("file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) {
                    return false;
                }
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                }
                return true;
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        webView.loadUrl("file:///android_asset/app/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
