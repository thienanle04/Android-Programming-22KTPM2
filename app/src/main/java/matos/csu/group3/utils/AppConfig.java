package matos.csu.group3.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.preference.PreferenceManager;

public class AppConfig {
    private static AppConfig instance;
    private SharedPreferences sharedPreferences;

    // Keys cho SharedPreferences
    private static final String KEY_PORTRAIT_SPAN = "portrait_span_count";
    private static final String KEY_LANDSCAPE_SPAN = "landscape_span_count";

    // Giá trị mặc định
    private static final int DEFAULT_PORTRAIT_SPAN = 3;
    private static final int DEFAULT_LANDSCAPE_SPAN = 6;

    private AppConfig(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized AppConfig getInstance(Context context) {
        if (instance == null) {
            instance = new AppConfig(context.getApplicationContext());
        }
        return instance;
    }

    public int getCurrentSpanCount(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        return (orientation == Configuration.ORIENTATION_LANDSCAPE)
                ? getLandscapeSpanCount()
                : getPortraitSpanCount();
    }

    public int getPortraitSpanCount() {
        return sharedPreferences.getInt(KEY_PORTRAIT_SPAN, DEFAULT_PORTRAIT_SPAN);
    }

    public int getLandscapeSpanCount() {
        return sharedPreferences.getInt(KEY_LANDSCAPE_SPAN, DEFAULT_LANDSCAPE_SPAN);
    }

    public void setPortraitSpanCount(int count) {
        sharedPreferences.edit()
                .putInt(KEY_PORTRAIT_SPAN, count)
                .apply();
    }

    public void setLandscapeSpanCount(int count) {
        sharedPreferences.edit()
                .putInt(KEY_LANDSCAPE_SPAN, count)
                .apply();
    }

    // Không cần initialize() nữa vì đã dùng SharedPreferences
}