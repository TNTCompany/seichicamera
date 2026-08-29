package com.tnt.seichicamera.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    data class LanguageOption(val tag: String, val displayName: String)

    val languages = listOf(
        LanguageOption("", "System Default"),
        LanguageOption("en", "English"),
        LanguageOption("zh-CN", "简体中文"),
        LanguageOption("zh-HK", "繁體中文（香港）"),
        LanguageOption("zh-TW", "繁體中文（台灣）"),
        LanguageOption("ja", "日本語")
    )

    fun setLocale(tag: String) {
        val localeList = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLocaleTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "" else locales.toLanguageTags()
    }
}
