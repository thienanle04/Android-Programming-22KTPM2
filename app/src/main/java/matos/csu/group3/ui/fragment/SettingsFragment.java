package matos.csu.group3.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import matos.csu.group3.R;
import matos.csu.group3.service.GoogleSignInService;
import matos.csu.group3.ui.main.MainActivity;
import matos.csu.group3.utils.AppConfig;
import matos.csu.group3.utils.PasswordHelper;

import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import net.openid.appauth.TokenResponse;

public class SettingsFragment extends PreferenceFragmentCompat {
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
        findPreference("span_count_portrait").setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                int spanCount = Integer.parseInt(newValue.toString());
                if (spanCount < 1 || spanCount > 5) {
                    Toast.makeText(getContext(), "Please enter between 1-5", Toast.LENGTH_SHORT).show();
                    return false;
                }
                updateGridLayoutIfPortrait(spanCount); // Cập nhật nếu đang ở chế độ dọc
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });

        // Lắng nghe thay đổi của span count (Landscape)
        findPreference("span_count_landscape").setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                int spanCount = Integer.parseInt(newValue.toString());
                if (spanCount < 1 || spanCount > 5) {
                    Toast.makeText(getContext(), "Please enter between 1-5", Toast.LENGTH_SHORT).show();
                    return false;
                }
                updateGridLayoutIfLandscape(spanCount); // Cập nhật nếu đang ở chế độ ngang
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });
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
                googleSignInPref.setSummary("Đã đăng nhập với " + prefs.getString("user_email", ""));
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
            googleSignInPref.setSummary(isLoggedIn ? "Đã đăng nhập với " + findPreference("user_email"): "Chưa đăng nhập");
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
    // Kiểm tra hướng màn hình và cập nhật GridLayout
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
            Log.d("GridUpdate", "Update request sent");
        }
    }
}
