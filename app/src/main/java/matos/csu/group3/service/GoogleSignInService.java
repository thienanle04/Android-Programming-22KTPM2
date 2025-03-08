package matos.csu.group3.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

public class GoogleSignInService {
    private static final String TAG = "GoogleSignInService";
    private static final String CLIENT_ID = "754399029-jr1nuobn0t7rflnmp39hh9qf4efp87e4.apps.googleusercontent.com";
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

        Log.d("OAuthRequest", "Auth URL: " + authRequest.toUri().toString());

        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        launcher.launch(authIntent);
    }


    public void handleAuthResponse(Intent data, AuthResultCallback callback) {
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        assert resp != null;
        TokenRequest tokenRequest = resp.createTokenExchangeRequest();

        authService.performTokenRequest(tokenRequest, (TokenResponse response, net.openid.appauth.AuthorizationException ex) -> {
            if (response != null) {
                authState.update(response, ex);

                // Lưu trạng thái đăng nhập
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.apply();

                callback.onSuccess(response);
            } else {
                callback.onError(ex);
            }
        });
    }

    public static JSONObject getUserInfo(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

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

    public interface AuthResultCallback {
        void onSuccess(TokenResponse response);
        void onError(Exception e);
    }
}