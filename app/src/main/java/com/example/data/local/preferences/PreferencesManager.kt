package com.example.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("campusmind_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DRAFT_TEXT = "key_draft_text"
        private const val KEY_ACTIVE_CHAT_ID = "key_active_chat_id"
        private const val KEY_SELECTED_MODEL = "key_selected_model"
        private const val KEY_THEME_MODE = "key_theme_mode" // "SYSTEM", "LIGHT", "DARK"
    }

    var draftText: String
        get() = prefs.getString(KEY_DRAFT_TEXT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DRAFT_TEXT, value).apply()

    var activeChatId: String?
        get() = prefs.getString(KEY_ACTIVE_CHAT_ID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_CHAT_ID, value).apply()

    var selectedModelId: String
        get() = prefs.getString(KEY_SELECTED_MODEL, "campusmind-fast") ?: "campusmind-fast"
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()
}
