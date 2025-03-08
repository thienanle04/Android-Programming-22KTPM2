package matos.csu.group3.ui.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceFragmentCompat;

import matos.csu.group3.R;
import matos.csu.group3.service.GoogleSignInService;

import android.content.Intent;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import net.openid.appauth.TokenResponse;

public class SettingsFragment extends PreferenceFragmentCompat {
    private ActivityResultLauncher<Intent> authResultLauncher;

    private GoogleSignInService googleSignInService;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        googleSignInService = new GoogleSignInService(requireContext());

        Preference googleSignInPref = findPreference("google_sign_in");
        if (googleSignInPref != null) {
            googleSignInPref.setOnPreferenceClickListener(preference -> {
                googleSignInService.signIn(authResultLauncher);
                return true;
            });

            // Kiểm tra trạng thái đăng nhập
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

            if (isLoggedIn) {
                googleSignInPref.setSummary("Đã đăng nhập");
            } else {
                googleSignInPref.setSummary("Chưa đăng nhập");
            }
        }

        authResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        googleSignInService.handleAuthResponse(result.getData(), new GoogleSignInService.AuthResultCallback() {
                            @Override
                            public void onSuccess(TokenResponse response) {
                                updateLoginStatus(true);
                            }

                            @Override
                            public void onError(Exception e) {
                                updateLoginStatus(false);
                            }
                        });
                    }
                }
        );
    }

    private void updateLoginStatus(boolean isLoggedIn) {
        Preference googleSignInPref = findPreference("google_sign_in");
        if (googleSignInPref != null) {
            googleSignInPref.setSummary(isLoggedIn ? "Đã đăng nhập" : "Chưa đăng nhập");
        }
    }
}
