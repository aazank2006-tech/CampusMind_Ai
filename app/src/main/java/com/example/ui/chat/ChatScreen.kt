package com.example.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.model.Attachment
import com.example.domain.model.Role
import com.example.ui.chat.components.AccountDialog
import com.example.ui.chat.components.AttachmentPickerSheet
import com.example.ui.chat.components.ChatActionSheet
import com.example.ui.chat.components.ChatDrawerContent
import com.example.ui.chat.components.ChatTopAppBar
import com.example.ui.chat.components.EditMessageDialog
import com.example.ui.chat.components.EmptyStateHero
import com.example.ui.chat.components.GeminiComposer
import com.example.ui.chat.components.GeminiMessageItem
import com.example.ui.chat.components.LiveVoiceOverlay
import com.example.ui.chat.components.ModelPickerSheet
import com.example.ui.chat.components.RenameChatDialog
import com.example.ui.chat.components.ScrollToBottomFab
import com.example.ui.chat.components.ShimmerLoader
import com.example.util.VoiceDictationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Voice Dictation & Speech Recognition Helper
    val dictationHelper = remember {
        VoiceDictationHelper(
            context = context,
            onPartialResult = { partial ->
                if (uiState.isLiveVoiceActive) {
                    // In Live Voice mode
                } else {
                    viewModel.appendDictatedText(partial)
                }
            },
            onFinalResult = { final ->
                if (uiState.isLiveVoiceActive) {
                    viewModel.onLiveVoiceUserSpoke(final)
                } else {
                    viewModel.appendDictatedText(final)
                }
            },
            onError = { err ->
                if (!uiState.isLiveVoiceActive) {
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            },
            onListeningStateChanged = { listening -> viewModel.setDictating(listening) }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            dictationHelper.stopListening()
            viewModel.stopTts()
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dictationHelper.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice features", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Capture Launcher with FileProvider
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.addAttachment(
                Attachment(
                    name = "Photo_${System.currentTimeMillis().toString().takeLast(4)}.jpg",
                    mimeType = "image/jpeg",
                    uri = tempCameraUri.toString()
                )
            )
        }
    }

    // Zero-permission modern Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachment(
                Attachment(
                    name = "Image_${System.currentTimeMillis().toString().takeLast(4)}.jpg",
                    mimeType = "image/*",
                    uri = it.toString()
                )
            )
        }
    }

    // File / Document Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment ?: "document_${System.currentTimeMillis().toString().takeLast(4)}"
            viewModel.addAttachment(
                Attachment(
                    name = name,
                    mimeType = "application/*",
                    uri = it.toString()
                )
            )
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 1) return@derivedStateOf true
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 2
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChatUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (isAtBottom && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawerContent(
                currentChatId = uiState.currentChat?.id,
                pinnedChats = uiState.pinnedChats,
                recentChats = uiState.recentChats,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                onNewChatClick = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onChatClick = { chat ->
                    viewModel.selectChat(chat)
                    scope.launch { drawerState.close() }
                },
                onChatActionClick = { chat ->
                    viewModel.openChatAction(chat)
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowSettingsDialog(true)
                },
                onHelpClick = {
                    scope.launch { drawerState.close() }
                    Toast.makeText(context, "CampusMind Support • AI study companion", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ChatTopAppBar(
                    selectedModel = uiState.selectedModel,
                    isConversationNonEmpty = uiState.messages.isNotEmpty(),
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onModelClick = { viewModel.setShowModelPicker(true) },
                    onNewChatClick = { viewModel.startNewChat() },
                    onAvatarClick = { viewModel.setShowAccountDialog(true) },
                    userInitial = uiState.userFirstName.take(1)
                )
            },
            containerColor = Color.Transparent,
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Body: Empty State Hero OR Message List
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.messages.isEmpty()) {
                        EmptyStateHero(
                            userFirstName = uiState.userFirstName,
                            onSuggestionClick = { suggestion ->
                                viewModel.onSuggestionClicked(suggestion)
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                val isLastStreaming = message == uiState.messages.lastOrNull() &&
                                        message.role == Role.ASSISTANT &&
                                        uiState.isGenerating &&
                                        message.content.isEmpty()

                                if (isLastStreaming) {
                                    ShimmerLoader()
                                } else {
                                    GeminiMessageItem(
                                        message = message,
                                        isSpeaking = uiState.currentlySpeakingId == message.id,
                                        onRegenerateClick = {
                                            viewModel.sendMessage(message.content)
                                        },
                                        onReadAloudClick = {
                                            viewModel.toggleReadAloud(message)
                                        },
                                        onEditUserMessage = { userMsg ->
                                            viewModel.openEditMessageDialog(userMsg)
                                        }
                                    )
                                }
                            }
                        }

                        ScrollToBottomFab(
                            visible = !isAtBottom && uiState.messages.isNotEmpty(),
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(uiState.messages.size - 1)
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                // Voice Dictation Active Bar
                AnimatedVisibility(visible = uiState.isDictating && !uiState.isLiveVoiceActive) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Listening… Speak now",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Floating Composer at bottom
                GeminiComposer(
                    text = uiState.inputText,
                    attachments = uiState.attachments,
                    onTextChange = { viewModel.onInputTextChanged(it) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onSendClick = { viewModel.sendMessage() },
                    onAttachClick = {
                        viewModel.setShowAttachmentPicker(true)
                    },
                    onMicClick = {
                        if (uiState.isDictating) {
                            dictationHelper.stopListening()
                        } else {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                dictationHelper.startListening()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onLiveClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.openLiveVoice()
                            dictationHelper.startListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopClick = {
                        viewModel.stopGeneration()
                    },
                    isGenerating = uiState.isGenerating
                )
            }
        }
    }

    // Live Voice Mode Full Screen Overlay
    if (uiState.isLiveVoiceActive) {
        LiveVoiceOverlay(
            state = uiState.liveVoiceState,
            isMuted = uiState.isLiveVoiceMuted,
            userTranscript = uiState.liveVoiceUserTranscript,
            aiTranscript = uiState.liveVoiceAiTranscript,
            onToggleMute = { viewModel.toggleLiveVoiceMute() },
            onStopSpeaking = { viewModel.stopTts() },
            onClose = {
                dictationHelper.stopListening()
                viewModel.closeLiveVoice()
            }
        )
    }

    // Attachment Options Bottom Sheet (Camera, Gallery, Files)
    if (uiState.showAttachmentPicker) {
        AttachmentPickerSheet(
            onCameraClick = {
                val photoFile = File.createTempFile("campus_cam_", ".jpg", context.cacheDir)
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCameraUri = photoUri
                cameraLauncher.launch(photoUri)
            },
            onGalleryClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onFilesClick = {
                documentPickerLauncher.launch("*/*")
            },
            onDismiss = { viewModel.setShowAttachmentPicker(false) }
        )
    }

    // Model Picker Bottom Sheet
    if (uiState.showModelPicker) {
        ModelPickerSheet(
            selectedModel = uiState.selectedModel,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { viewModel.setShowModelPicker(false) }
        )
    }

    // User Account Dialog
    if (uiState.showAccountDialog) {
        AccountDialog(
            userFirstName = uiState.userFirstName,
            onDismiss = { viewModel.setShowAccountDialog(false) }
        )
    }

    // Chat Action Bottom Sheet (Pin, Rename, Share, Delete)
    uiState.activeChatAction?.let { chat ->
        ChatActionSheet(
            chat = chat,
            onPinClick = {
                viewModel.pinChat(chat.id, !chat.isPinned)
            },
            onRenameClick = {
                viewModel.openRenameDialog(chat)
            },
            onShareClick = {
                viewModel.closeChatAction()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "CampusMind Chat: ${chat.title}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share chat"))
            },
            onDeleteClick = {
                viewModel.deleteChat(chat.id)
            },
            onDismiss = { viewModel.closeChatAction() }
        )
    }

    // Rename Chat Dialog
    uiState.chatToRename?.let { chat ->
        RenameChatDialog(
            chat = chat,
            onRenameConfirm = { newTitle ->
                viewModel.renameChat(chat.id, newTitle)
            },
            onDismiss = { viewModel.closeRenameDialog() }
        )
    }

    // Edit User Message Dialog
    uiState.messageToEdit?.let { message ->
        EditMessageDialog(
            message = message,
            onSaveAndRerun = { newText ->
                viewModel.editAndRerun(message, newText)
            },
            onDismiss = { viewModel.closeEditMessageDialog() }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        com.example.ui.chat.components.SettingsDialog(
            selectedModel = uiState.selectedModel,
            onModelSelected = { viewModel.selectModel(it) },
            onClearAllHistory = { viewModel.clearAllHistory() },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }
}
