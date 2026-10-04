package com.example.di

import android.content.Context
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ChatDaoImpl
import com.example.data.local.db.CampusMindDatabaseHelper
import com.example.data.local.preferences.PreferencesManager
import com.example.data.repository.ChatRepository
import com.example.data.repository.OfflineFirstChatRepository
import com.example.util.TtsManager

class AppContainer(context: Context) {
    val databaseHelper: CampusMindDatabaseHelper by lazy {
        CampusMindDatabaseHelper(context.applicationContext)
    }

    val chatDao: ChatDao by lazy {
        ChatDaoImpl(databaseHelper)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context.applicationContext)
    }

    val chatRepository: ChatRepository by lazy {
        OfflineFirstChatRepository(chatDao)
    }

    val ttsManager: TtsManager by lazy {
        TtsManager(context.applicationContext)
    }
}
