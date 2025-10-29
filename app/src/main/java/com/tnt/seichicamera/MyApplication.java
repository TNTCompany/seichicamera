package com.tnt.seichicamera;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 在 App 启动时应用保存的语言设置
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String langValue = prefs.getString("language", "default");
        updateAppLanguage(langValue);
    }

    public static void updateAppLanguage(String langValue) {
        if (langValue.equals("default")) {
            // 跟随系统
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            // 设置指定语言
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(langValue);
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }
}