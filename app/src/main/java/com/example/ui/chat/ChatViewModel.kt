package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.preferences.PreferencesManager
import com.example.data.repository.ChatRepository
import com.example.data.repository.StreamEvent
import com.example.domain.model.AiModel
import com.example.domain.model.AiModels
import com.example.domain.model.Attachment
import com.example.domain.model.Chat
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.model.Role
import com.example.domain.model.SuggestionPrompt
import com.example.ui.chat.components.LiveVoiceState
import com.example.util.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentChat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val selectedModel: AiModel = AiModels.FAST,
    val userFirstName: String = "Aazan",
    val inputText: String = "",
    val attachments: List<Attachment> = emptyList(),
    val searchQuery: String = "",
    val isGenerating: Boolean = false,
    val isThinking: Boolean = false,
    val isDictating: Boolean = false,
    val currentlySpeakingId: String? = null,
    val isLiveVoiceActive: Boolean = false,
    val liveVoiceState: LiveVoiceState = LiveVoiceState.LISTENING,
    val isLiveVoiceMuted: Boolean = false,
    val liveVoiceUserTranscript: String = "",
    val liveVoiceAiTranscript: String = "",
    val pinnedChats: List<Chat> = emptyList(),
    val recentChats: List<Chat> = emptyList(),
    val activeChatAction: Chat? = null,
    val chatToRename: Chat? = null,
    val messageToEdit: Message? = null,
    val showModelPicker: Boolean = false,
    val showAccountDialog: Boolean = false,
    val showAttachmentPicker: Boolean = false,
    val showSettingsDialog: Boolean = false
)

sealed interface ChatUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : ChatUiEvent
}

class ChatViewModel(
    private val repository: ChatRepository,
    private val preferencesManager: PreferencesManager? = null,
    private val ttsManager: TtsManager? = null
) : ViewModel() {

    private val _currentChat = MutableStateFlow<Chat?>(null)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _selectedModel = MutableStateFlow(
        if (preferencesManager?.selectedModelId == AiModels.PRO.id) AiModels.PRO else AiModels.FAST
    )
    private val _userFirstName = MutableStateFlow("Aazan")
    private val _inputText = MutableStateFlow(preferencesManager?.draftText ?: "")
    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isGenerating = MutableStateFlow(false)
    private val _isThinking = MutableStateFlow(false)
    private val _isDictating = MutableStateFlow(false)
    private val _activeChatAction = MutableStateFlow<Chat?>(null)
    private val _chatToRename = MutableStateFlow<Chat?>(null)
    private val _messageToEdit = MutableStateFlow<Message?>(null)
    private val _showModelPicker = MutableStateFlow(false)
    private val _showAccountDialog = MutableStateFlow(false)
    private val _showAttachmentPicker = MutableStateFlow(false)
    private val _showSettingsDialog = MutableStateFlow(false)

    // Live Voice state
    private val _isLiveVoiceActive = MutableStateFlow(false)
    private val _liveVoiceState = MutableStateFlow(LiveVoiceState.LISTENING)
    private val _isLiveVoiceMuted = MutableStateFlow(false)
    private val _liveVoiceUserTranscript = MutableStateFlow("")
    private val _liveVoiceAiTranscript = MutableStateFlow("")

    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    private var streamingJob: Job? = null
    private var recentlyDeletedChat: Chat? = null

    val uiState: StateFlow<ChatUiState> = combine(
        repository.getChats(),
        _currentChat,
        _messages,
        _selectedModel,
        _inputText,
        _attachments,
        _searchQuery,
        _isGenerating,
        _isDictating,
        ttsManager?.currentlySpeakingMessageId ?: MutableStateFlow(null),
        _isLiveVoiceActive,
        _liveVoiceState,
        _isLiveVoiceMuted,
        _liveVoiceUserTranscript,
        _liveVoiceAiTranscript,
        _activeChatAction,
        _chatToRename,
        _messageToEdit,
        _showModelPicker,
        _showAccountDialog,
        _showAttachmentPicker,
        _showSettingsDialog
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allChats = params[0] as List<Chat>
        val currentChat = params[1] as Chat?
        val messages = params[2] as List<Message>
        val model = params[3] as AiModel
        val input = params[4] as String
        val attachments = params[5] as List<Attachment>
        val query = params[6] as String
        val generating = params[7] as Boolean
        val dictating = params[8] as Boolean
        val speakingId = params[9] as String?
        val liveActive = params[10] as Boolean
        val liveState = params[11] as LiveVoiceState
        val liveMuted = params[12] as Boolean
        val userSpeech = params[13] as String
        val aiSpeech = params[14] as String
        val chatAction = params[15] as Chat?
        val renameChat = params[16] as Chat?
        val editMessage = params[17] as Message?
        val showPicker = params[18] as Boolean
        val showAccount = params[19] as Boolean
        val showAttachment = params[20] as Boolean
        val showSettings = params[21] as Boolean

        val filtered = if (query.isBlank()) {
            allChats
        } else {
            allChats.filter { it.title.contains(query, ignoreCase = true) }
        }

        ChatUiState(
            currentChat = currentChat,
            messages = messages,
            selectedModel = model,
            userFirstName = _userFirstName.value,
            inputText = input,
            attachments = attachments,
            searchQuery = query,
            isGenerating = generating,
            isThinking = _isThinking.value,
            isDictating = dictating,
            currentlySpeakingId = speakingId,
            isLiveVoiceActive = liveActive,
            liveVoiceState = liveState,
            isLiveVoiceMuted = liveMuted,
            liveVoiceUserTranscript = userSpeech,
            liveVoiceAiTranscript = aiSpeech,
            pinnedChats = filtered.filter { it.isPinned },
            recentChats = filtered.filterNot { it.isPinned },
            activeChatAction = chatAction,
            chatToRename = renameChat,
            messageToEdit = editMessage,
            showModelPicker = showPicker,
            showAccountDialog = showAccount,
            showAttachmentPicker = showAttachment,
            showSettingsDialog = showSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        preferencesManager?.activeChatId?.let { savedChatId ->
            viewModelScope.launch {
                repository.getChats().collect { chats ->
                    if (_currentChat.value == null) {
                        chats.find { it.id == savedChatId }?.let { selectChat(it) }
                    }
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        preferencesManager?.draftText = text
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addAttachment(attachment: Attachment) {
        _attachments.value = _attachments.value + attachment
    }

    fun removeAttachment(attachment: Attachment) {
        _attachments.value = _attachments.value.filterNot { it.id == attachment.id }
    }

    fun setShowAttachmentPicker(show: Boolean) {
        _showAttachmentPicker.value = show
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllData()
            startNewChat()
            preferencesManager?.draftText = ""
            preferencesManager?.activeChatId = null
        }
    }

    fun setDictating(dictating: Boolean) {
        _isDictating.value = dictating
    }

    fun appendDictatedText(partial: String) {
        val current = _inputText.value
        _inputText.value = if (current.isBlank()) partial else "$current $partial"
        preferencesManager?.draftText = _inputText.value
    }

    fun handleSharedContent(sharedText: String?, sharedUri: String?) {
        if (!sharedText.isNullOrBlank()) {
            _inputText.value = sharedText
            preferencesManager?.draftText = sharedText
        }
        if (!sharedUri.isNullOrBlank()) {
            val attachment = Attachment(
                name = "Shared Image",
                mimeType = "image/*",
                uri = sharedUri
            )
            addAttachment(attachment)
        }
    }

    // TTS Read Aloud
    fun toggleReadAloud(message: Message) {
        if (ttsManager?.currentlySpeakingMessageId?.value == message.id) {
            ttsManager.stop()
        } else {
            ttsManager?.speak(text = message.content, messageId = message.id)
        }
    }

    fun stopTts() {
        ttsManager?.stop()
    }

    // Live Voice Mode
    fun openLiveVoice() {
        _isLiveVoiceActive.value = true
        _liveVoiceState.value = LiveVoiceState.LISTENING
        _liveVoiceUserTranscript.value = ""
        _liveVoiceAiTranscript.value = ""
    }

    fun closeLiveVoice() {
        _isLiveVoiceActive.value = false
        stopTts()
    }

    fun toggleLiveVoiceMute() {
        _isLiveVoiceMuted.value = !_isLiveVoiceMuted.value
    }

    fun onLiveVoiceUserSpoke(speech: String) {
        if (_isLiveVoiceMuted.value || speech.isBlank()) return

        _liveVoiceUserTranscript.value = speech
        _liveVoiceState.value = LiveVoiceState.THINKING

        viewModelScope.launch {
            delay(1200)
            val answer = "I heard you ask: \"$speech\". In academic algorithms, understanding asymptotic complexity like Big-O gives you the predictive power to choose optimal data structures."
            _liveVoiceAiTranscript.value = answer
            _liveVoiceState.value = LiveVoiceState.SPEAKING
            ttsManager?.speak(text = answer, messageId = "live_turn")
            delay(4000)
            if (_isLiveVoiceActive.value) {
                _liveVoiceState.value = LiveVoiceState.LISTENING
            }
        }
    }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        _showModelPicker.value = false
        preferencesManager?.selectedModelId = model.id
    }

    fun setShowModelPicker(show: Boolean) {
        _showModelPicker.value = show
    }

    fun setShowAccountDialog(show: Boolean) {
        _showAccountDialog.value = show
    }

    fun selectChat(chat: Chat) {
        streamingJob?.cancel()
        stopTts()
        _isGenerating.value = false
        _isThinking.value = false
        _currentChat.value = chat
        _inputText.value = ""
        _attachments.value = emptyList()
        preferencesManager?.activeChatId = chat.id

        viewModelScope.launch {
            repository.getMessages(chat.id).collect {
                _messages.value = it
            }
        }
    }

    fun startNewChat() {
        streamingJob?.cancel()
        stopTts()
        _isGenerating.value = false
        _isThinking.value = false
        _currentChat.value = null
        _messages.value = emptyList()
        _inputText.value = ""
        _attachments.value = emptyList()
        preferencesManager?.activeChatId = null
    }

    fun openChatAction(chat: Chat) {
        _activeChatAction.value = chat
    }

    fun closeChatAction() {
        _activeChatAction.value = null
    }

    fun openRenameDialog(chat: Chat) {
        _activeChatAction.value = null
        _chatToRename.value = chat
    }

    fun closeRenameDialog() {
        _chatToRename.value = null
    }

    fun openEditMessageDialog(message: Message) {
        _messageToEdit.value = message
    }

    fun closeEditMessageDialog() {
        _messageToEdit.value = null
    }

    fun pinChat(chatId: String, pinned: Boolean) {
        viewModelScope.launch {
            repository.pinChat(chatId, pinned)
            _activeChatAction.value = null
        }
    }

    fun renameChat(chatId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameChat(chatId, newTitle.trim())
            if (_currentChat.value?.id == chatId) {
                _currentChat.value = _currentChat.value?.copy(title = newTitle.trim())
            }
            _chatToRename.value = null
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val deleted = repository.deleteChat(chatId)
            recentlyDeletedChat = deleted
            _activeChatAction.value = null

            if (_currentChat.value?.id == chatId) {
                startNewChat()
            }

            _events.emit(
                ChatUiEvent.ShowSnackbar(
                    message = "Chat deleted",
                    actionLabel = "Undo",
                    onAction = { undoDelete() }
                )
            )
        }
    }

    fun undoDelete() {
        val toRestore = recentlyDeletedChat ?: return
        viewModelScope.launch {
            repository.restoreChat(toRestore)
            recentlyDeletedChat = null
        }
    }

    fun onSuggestionClicked(suggestion: SuggestionPrompt) {
        sendMessage(suggestion.prompt)
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isGenerating.value = false
        _isThinking.value = false

        val currentList = _messages.value
        val last = currentList.lastOrNull()
        if (last != null && last.role == Role.ASSISTANT && last.status == MessageStatus.STREAMING) {
            val updated = last.copy(status = MessageStatus.STOPPED)
            _messages.value = currentList.dropLast(1) + updated
            viewModelScope.launch { repository.updateMessage(updated) }
        }
    }

    fun editAndRerun(originalMessage: Message, newText: String) {
        closeEditMessageDialog()
        val chatId = originalMessage.chatId

        viewModelScope.launch {
            repository.deleteMessagesAfter(chatId, originalMessage.id)

            val index = _messages.value.indexOfFirst { it.id == originalMessage.id }
            val truncated = if (index != -1) _messages.value.take(index) else emptyList()

            val updatedUserMessage = originalMessage.copy(content = newText)
            _messages.value = truncated + updatedUserMessage
            repository.updateMessage(updatedUserMessage)

            startStreaming(chatId = chatId, userPrompt = newText)
        }
    }

    fun sendMessage(text: String = _inputText.value) {
        val trimmed = text.trim()
        val currentAttachments = _attachments.value
        if ((trimmed.isBlank() && currentAttachments.isEmpty()) || _isGenerating.value) return

        val promptToSend = if (trimmed.isBlank() && currentAttachments.isNotEmpty()) {
            "Analyze attached file: ${currentAttachments.joinToString { it.name }}"
        } else {
            trimmed
        }

        viewModelScope.launch {
            var chat = _currentChat.value
            if (chat == null) {
                val derivedTitle = if (promptToSend.length > 28) promptToSend.take(28) + "…" else promptToSend
                chat = repository.createNewChat(derivedTitle)
                _currentChat.value = chat
                preferencesManager?.activeChatId = chat.id
            }

            val userMessage = Message(
                chatId = chat.id,
                role = Role.USER,
                content = promptToSend,
                attachments = currentAttachments
            )
            repository.addMessage(userMessage)
            _messages.value = _messages.value + userMessage

            _inputText.value = ""
            _attachments.value = emptyList()
            preferencesManager?.draftText = ""

            startStreaming(chatId = chat.id, userPrompt = promptToSend)
        }
    }

    private fun startStreaming(chatId: String, userPrompt: String) {
        streamingJob?.cancel()
        _isGenerating.value = true
        _isThinking.value = false

        streamingJob = viewModelScope.launch {
            val assistantMessage = Message(
                chatId = chatId,
                role = Role.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            repository.addMessage(assistantMessage)
            _messages.value = _messages.value + assistantMessage

            var accumulatedThinking = StringBuilder()
            var accumulatedText = StringBuilder()

            repository.streamChatReply(
                chatId = chatId,
                userPrompt = userPrompt,
                isPro = _selectedModel.value.isPro
            ).collect { event ->
                when (event) {
                    is StreamEvent.Thinking -> {
                        _isThinking.value = true
                        accumulatedThinking.append(event.chunk)
                        val updated = assistantMessage.copy(
                            thinkingContent = accumulatedThinking.toString(),
                            status = MessageStatus.STREAMING
                        )
                        updateLastAssistantMessage(updated)
                    }
                    is StreamEvent.Token -> {
                        _isThinking.value = false
                        accumulatedText.append(event.text)
                        val updated = assistantMessage.copy(
                            content = accumulatedText.toString(),
                            thinkingContent = accumulatedThinking.toString().takeIf { it.isNotBlank() },
                            status = MessageStatus.STREAMING
                        )
                        updateLastAssistantMessage(updated)
                    }
                    is StreamEvent.Done -> {
                        _isGenerating.value = false
                        _isThinking.value = false
                        val finalMessage = assistantMessage.copy(
                            content = accumulatedText.toString(),
                            thinkingContent = accumulatedThinking.toString().takeIf { it.isNotBlank() },
                            status = MessageStatus.COMPLETE
                        )
                        updateLastAssistantMessage(finalMessage)
                        repository.updateMessage(finalMessage)
                    }
                    is StreamEvent.Error -> {
                        _isGenerating.value = false
                        _isThinking.value = false
                        val errMessage = assistantMessage.copy(
                            content = accumulatedText.toString() + "\n\n*(Generation interrupted)*",
                            status = MessageStatus.ERROR
                        )
                        updateLastAssistantMessage(errMessage)
                        repository.updateMessage(errMessage)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun updateLastAssistantMessage(updated: Message) {
        val list = _messages.value
        if (list.isNotEmpty() && list.last().role == Role.ASSISTANT) {
            _messages.value = list.dropLast(1) + updated
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
    }

    companion object {
        fun provideFactory(
            repository: ChatRepository,
            preferencesManager: PreferencesManager,
            ttsManager: TtsManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository, preferencesManager, ttsManager) as T
            }
        }
    }
}
