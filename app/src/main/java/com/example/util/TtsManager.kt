package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow = _isSpeakingFlow.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    val currentlySpeakingMessageId = _currentlySpeakingMessageId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeakingFlow.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeakingFlow.value = false
                        _currentlySpeakingMessageId.value = null
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeakingFlow.value = false
                        _currentlySpeakingMessageId.value = null
                    }
                })
            }
        }
    }

    fun speak(text: String, messageId: String? = null, onDone: (() -> Unit)? = null) {
        if (!isInitialized) return
        stop()

        // Strip markdown symbols for natural speech synthesis
        val cleanSpeech = text
            .replace(Regex("```[\\s\\S]*?```"), "Code snippet omitted.")
            .replace(Regex("[#*`_~>\\[\\]|]"), "")
            .replace(Regex("\\n+"), ". ")
            .trim()

        _currentlySpeakingMessageId.value = messageId
        _isSpeakingFlow.value = true

        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, messageId ?: "live_tts")
    }

    fun stop() {
        tts?.stop()
        _isSpeakingFlow.value = false
        _currentlySpeakingMessageId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
