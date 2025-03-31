package matos.csu.group3.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PasswordHelper {
    private static final String PREF_NAME = "AlbumPasswords";
    private static final String LOCK_PASS_KEY = "lock_password";
    private static final String HIDE_PASS_KEY = "hide_password";

    // ========== Lock Password ==========
    public static void setLockPassword(Context context, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LOCK_PASS_KEY, password).apply();
    }

    public static boolean checkLockPassword(Context context, String inputPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPass = prefs.getString(LOCK_PASS_KEY, "");
        return savedPass.equals(inputPassword);
    }

    public static boolean isLockPasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(LOCK_PASS_KEY);
    }

    public static void removeLockPassword(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(LOCK_PASS_KEY).apply();
    }

    // ========== Hide Password ==========
    public static void setHidePassword(Context context, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(HIDE_PASS_KEY, password).apply();
    }

    public static boolean checkHidePassword(Context context, String inputPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPass = prefs.getString(HIDE_PASS_KEY, "");
        return savedPass.equals(inputPassword);
    }

    public static boolean isHidePasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(HIDE_PASS_KEY);
    }

    public static void removeHidePassword(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(HIDE_PASS_KEY).apply();
    }

    // ========== Xóa tất cả mật khẩu ==========
    public static void clearAllPasswords(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}