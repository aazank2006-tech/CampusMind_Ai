package com.example

import com.example.data.repository.FakeChatRepository
import com.example.domain.model.AiModels
import com.example.domain.model.Attachment
import com.example.domain.model.Chat
import com.example.domain.model.DefaultSuggestions
import com.example.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeChatRepository()
        viewModel = ChatViewModel(repository = repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultModelIsFast() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AiModels.FAST.id, state.selectedModel.id)
        assertFalse(state.selectedModel.isPro)

        collectJob.cancel()
    }

    @Test
    fun selectProModelUpdatesState() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.selectModel(AiModels.PRO)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AiModels.PRO.id, state.selectedModel.id)
        assertTrue(state.selectedModel.isPro)
        assertFalse(state.showModelPicker)

        collectJob.cancel()
    }

    @Test
    fun inputTextChangedUpdatesState() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("Explain dynamic programming")
        testScheduler.advanceUntilIdle()

        assertEquals("Explain dynamic programming", viewModel.uiState.value.inputText)

        collectJob.cancel()
    }

    @Test
    fun addAndRemoveAttachment() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        val testAttachment = Attachment(
            name = "lecture_notes.pdf",
            mimeType = "application/pdf"
        )
        viewModel.addAttachment(testAttachment)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.attachments.any { it.name == "lecture_notes.pdf" })

        viewModel.removeAttachment(testAttachment)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.attachments.any { it.name == "lecture_notes.pdf" })

        collectJob.cancel()
    }

    @Test
    fun searchFilterNarrowsChats() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Sorting")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Sorting", state.searchQuery)
        assertTrue(state.pinnedChats.all { it.title.contains("Sorting", ignoreCase = true) })

        collectJob.cancel()
    }

    @Test
    fun startNewChatResetsActiveChatAndMessages() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.selectChat(Chat(id = "test_chat", title = "Algorithms"))
        testScheduler.advanceUntilIdle()

        viewModel.startNewChat()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.currentChat)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.inputText.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun liveVoiceModeTogglesState() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.openLiveVoice()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLiveVoiceActive)

        viewModel.toggleLiveVoiceMute()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLiveVoiceMuted)

        viewModel.toggleLiveVoiceMute()
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLiveVoiceMuted)

        viewModel.closeLiveVoice()
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLiveVoiceActive)

        collectJob.cancel()
    }

    @Test
    fun suggestionClickedPopulatesAndSends() = runTest(testDispatcher) {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        val suggestion = DefaultSuggestions.ALL.first()
        viewModel.onSuggestionClicked(suggestion)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.currentChat)
        assertTrue(state.messages.isNotEmpty())
        assertEquals(suggestion.prompt, state.messages.first().content)

        collectJob.cancel()
    }
}
