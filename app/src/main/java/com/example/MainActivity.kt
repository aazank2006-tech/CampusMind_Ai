package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.theme.CampusMindTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val app = application as CampusMindApplication
        ChatViewModel.provideFactory(
            repository = app.container.chatRepository,
            preferencesManager = app.container.preferencesManager,
            ttsManager = app.container.ttsManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            CampusMindTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val sharedUri = (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.toString()
            viewModel.handleSharedContent(sharedText, sharedUri)
        }
        when (intent.getStringExtra("shortcut_action")) {
            "new_chat" -> viewModel.startNewChat()
            "live_voice" -> viewModel.openLiveVoice()
        }
    }
}
