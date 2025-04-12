package matos.csu.group3.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import matos.csu.group3.R;
import matos.csu.group3.service.GoogleSignInService;
import matos.csu.group3.utils.AppConfig;
import matos.csu.group3.utils.PasswordHelper;

import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;

import net.openid.appauth.TokenResponse;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";
    private ActivityResultLauncher<Intent> authResultLauncher;
    private GoogleSignInService googleSignInService;

    public interface OnGridUpdateListener {
        void onGridUpdateRequested();
    }

    private OnGridUpdateListener gridUpdateListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnGridUpdateListener) {
            gridUpdateListener = (OnGridUpdateListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGridUpdateListener");
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference darkModePref = findPreference("dark_mode");
        if (darkModePref != null) {
            darkModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if ("dark_mode".equals(preference.getKey())) {
                    String themeOption = newValue.toString();
                    switch (themeOption) {
                        case "system":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                        case "light":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case "dark":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                    }
                }
                return true;
            });
        }

        googleSignInService = new GoogleSignInService(requireContext());

        // Existing preferences
        Preference setHidePasswordPref = findPreference("set_hide_password");
        if (setHidePasswordPref != null) {
            setHidePasswordPref.setOnPreferenceClickListener(preference -> {
                showSetHidePasswordDialog();
                return true;
            });
        }

        Preference setLockPasswordPref = findPreference("set_lock_password");
        if (setLockPasswordPref != null) {
            setLockPasswordPref.setOnPreferenceClickListener(preference -> {
                showSetLockPasswordDialog();
                return true;
            });
        }

        Preference spanCountPortraitPref = findPreference("span_count_portrait");
        if (spanCountPortraitPref != null) {
            spanCountPortraitPref.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    int spanCount = Integer.parseInt(newValue.toString());
                    if (spanCount < 1 || spanCount > 5) {
                        Toast.makeText(getContext(), "Please enter between 1-5", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    updateGridLayoutIfPortrait(spanCount);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
        }

        Preference spanCountLandscapePref = findPreference("span_count_landscape");
        if (spanCountLandscapePref != null) {
            spanCountLandscapePref.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    int spanCount = Integer.parseInt(newValue.toString());
                    if (spanCount < 1 || spanCount > 5) {
                        Toast.makeText(getContext(), "Please enter between 1-5", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    updateGridLayoutIfLandscape(spanCount);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
        }

        // Google Sign-In preference
        Preference googleSignInPref = findPreference("google_sign_in");
        if (googleSignInPref != null) {
            googleSignInPref.setOnPreferenceClickListener(preference -> {
                googleSignInService.signIn(authResultLauncher);
                return true;
            });
        }

        // New Sync Photos preference
        Preference syncPhotosPref = findPreference("sync_photos");
        if (syncPhotosPref != null) {
            syncPhotosPref.setOnPreferenceClickListener(preference -> {
                googleSignInService.syncPhotosToDrive();
                Toast.makeText(getContext(), "Starting photo sync...", Toast.LENGTH_LONG).show();
                return true;
            });
        }

        // Initialize login status and sync button state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        updateLoginStatus(isLoggedIn);

        authResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        googleSignInService.handleAuthResponse(result.getData(), new GoogleSignInService.AuthResultCallback() {
                            @Override
                            public void onSuccess(TokenResponse response) {
                                updateLoginStatus(true);
                                Toast.makeText(getContext(), "Signed in successfully", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                updateLoginStatus(false);
                                Toast.makeText(getContext(), "Sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Sign-in cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void updateLoginStatus(boolean isLoggedIn) {
        Preference googleSignInPref = findPreference("google_sign_in");
        Preference syncPhotosPref = findPreference("sync_photos");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (googleSignInPref != null) {
            if (isLoggedIn) {
                String userEmail = prefs.getString("user_email", "unknown");
                googleSignInPref.setSummary("Đã đăng nhập với " + userEmail);
            } else {
                googleSignInPref.setSummary("Chưa đăng nhập");
            }
        }

        if (syncPhotosPref != null) {
            syncPhotosPref.setEnabled(isLoggedIn);
            syncPhotosPref.setSummary(isLoggedIn ? "Upload all device photos to Google Drive" : "Sign in to enable photo sync");
        }
    }

    private void showSetHidePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thiết lập mật khẩu album ẩn");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                PasswordHelper.setHidePassword(getContext(), password);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showSetLockPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thiết lập mật khẩu khóa album");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                PasswordHelper.setLockPassword(getContext(), password);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateGridLayoutIfPortrait(int spanCount) {
        updateGridLayout();
        AppConfig.getInstance(getContext()).setPortraitSpanCount(spanCount);
    }

    private void updateGridLayoutIfLandscape(int spanCount) {
        updateGridLayout();
        AppConfig.getInstance(getContext()).setLandscapeSpanCount(spanCount);
    }

    private void updateGridLayout() {
        if (gridUpdateListener != null) {
            gridUpdateListener.onGridUpdateRequested();
            Log.d(TAG, "Update request sent");
        }
    }
}