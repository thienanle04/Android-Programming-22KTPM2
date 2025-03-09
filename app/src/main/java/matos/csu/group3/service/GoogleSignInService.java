package matos.csu.group3.service;

import matos.csu.group3.BuildConfig;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.PreferenceManager;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleSignInService {
    private static final String TAG = "GoogleSignInService";
    private static final String CLIENT_ID = BuildConfig.GOOGLE_CLIENT_ID;
    private static final String REDIRECT_URI = "matos.csu.group3:/oauth2redirect";
    private AuthorizationService authService;
    private AuthState authState;
    private Context context;

    public GoogleSignInService(Context context) {
        this.context = context;
        this.authService = new AuthorizationService(context);
        this.authState = new AuthState();
    }

    public void signIn(ActivityResultLauncher<Intent> launcher) {
        AuthorizationServiceConfiguration serviceConfig =
                new AuthorizationServiceConfiguration(
                        Uri.parse("https://accounts.google.com/o/oauth2/auth"), // Auth endpoint
                        Uri.parse("https://oauth2.googleapis.com/token")  // Token endpoint
                );

        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI))
                .setScopes("openid", "profile", "email")
                .build();

        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        launcher.launch(authIntent);
    }

    public void handleAuthResponse(Intent data, AuthResultCallback callback) {
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        if (resp == null) {
            callback.onError(new Exception("Authorization response is null"));
            return;
        }

        TokenRequest tokenRequest = resp.createTokenExchangeRequest();

        authService.performTokenRequest(tokenRequest, (TokenResponse response, net.openid.appauth.AuthorizationException ex) -> {
            if (response != null) {
                authState.update(response, ex);
                String accessToken = response.accessToken;

                if (accessToken != null) {
                    getUserInfoAsync(accessToken);
                } else {
                    callback.onError(new Exception("Access token is null"));
                }
            } else {
                callback.onError(ex);
            }
        });
    }

    private JSONObject getUserInfo(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestMethod("GET");

            InputStream response = conn.getInputStream();
            Scanner scanner = new Scanner(response);
            scanner.useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";

            return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user info", e);
            return null;
        }
    }

    private void getUserInfoAsync(String accessToken) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            JSONObject userInfo = getUserInfo(accessToken);
            if (userInfo != null) {
                // Ensure UI updates are done on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    saveUserInfoToSharedPreferences(userInfo, accessToken);
                });
            } else {
                Log.e(TAG, "Failed to retrieve user info.");
            }
        });
    }

    private void saveUserInfoToSharedPreferences(JSONObject userInfo, String accessToken) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            editor.putBoolean("is_logged_in", true);
            editor.putString("access_token", accessToken);
            editor.putString("user_id", userInfo.optString("sub", ""));
            editor.putString("user_name", userInfo.optString("name", ""));
            editor.putString("user_email", userInfo.optString("email", ""));
            editor.putString("user_picture", userInfo.optString("picture", ""));
            editor.putString("user_given_name", userInfo.optString("given_name", ""));
            editor.putString("user_family_name", userInfo.optString("family_name", ""));
            editor.putString("user_locale", userInfo.optString("locale", ""));

            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user info to SharedPreferences", e);
        }
    }

    public interface AuthResultCallback {
        void onSuccess(TokenResponse response);
        void onError(Exception e);
    }
}
