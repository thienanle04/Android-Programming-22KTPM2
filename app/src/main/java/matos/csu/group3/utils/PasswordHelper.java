package matos.csu.group3.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PasswordHelper {
    private static final String PREF_NAME = "HiddenAlbumPrefs";
    private static final String PASSWORD_KEY = "hidden_album_password";

    public static void setPassword(Context context, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PASSWORD_KEY, password).apply();
    }

    public static boolean checkPassword(Context context, String inputPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPassword = prefs.getString(PASSWORD_KEY, "");
        return savedPassword.equals(inputPassword);
    }

    public static boolean isPasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(PASSWORD_KEY);
    }
}