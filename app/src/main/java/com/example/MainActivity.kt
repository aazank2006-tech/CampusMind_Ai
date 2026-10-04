package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DocumentSheet
import com.example.ui.components.MemorySheet
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.components.StatsBar
import com.example.ui.theme.CampusBlue
import com.example.ui.theme.CampusMindTheme
import com.example.ui.theme.CampusOrange
import com.example.ui.viewmodel.CampusMindViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CampusMindViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val messages by viewModel.messages.collectAsState()
            val activePersona by viewModel.activePersona.collectAsState()
            val studentMemory by viewModel.studentMemory.collectAsState()
            val activeDocument by viewModel.activeDocument.collectAsState()
            val selectedModel by viewModel.selectedModel.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val isSpeaking by viewModel.isSpeaking.collectAsState()

            var showPersonaSheet by remember { mutableStateOf(false) }
            var showMemorySheet by remember { mutableStateOf(false) }
            var showDocumentSheet by remember { mutableStateOf(false) }
            var showModelDialog by remember { mutableStateOf(false) }
            var showMenu by remember { mutableStateOf(false) }

            var inputText by remember { mutableStateOf("") }
            val listState = rememberLazyListState()
            val context = LocalContext.current

            // Auto-scroll when new messages arrive
            LaunchedEffect(messages.size, isLoading) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            CampusMindTheme(darkTheme = isDarkMode) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🎓", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "CampusMind AI",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { showModelDialog = true }
                                        ) {
                                            Text(
                                                text = selectedModel.displayName,
                                                fontSize = 11.sp,
                                                color = CampusOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = " ▾",
                                                fontSize = 10.sp,
                                                color = CampusOrange
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // Theme toggle (☀️ / 🌙)
                                IconButton(
                                    onClick = { viewModel.toggleDarkMode() },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme",
                                        tint = if (isDarkMode) CampusOrange else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Persona shortcut
                                IconButton(
                                    onClick = { showPersonaSheet = true },
                                    modifier = Modifier.testTag("persona_quick_button")
                                ) {
                                    Text(text = activePersona.emoji, fontSize = 20.sp)
                                }

                                // Overflow menu
                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.testTag("overflow_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("🎭 Personas (${activePersona.title})") },
                                            onClick = {
                                                showMenu = false
                                                showPersonaSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🧠 Student Memory (${if (studentMemory.isEmpty) "Empty" else "Active"})") },
                                            onClick = {
                                                showMenu = false
                                                showMemorySheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("📄 Lecture Q&A / Notes (${if (activeDocument != null) "1 Loaded" else "None"})") },
                                            onClick = {
                                                showMenu = false
                                                showDocumentSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🤖 Select AI Model") },
                                            onClick = {
                                                showMenu = false
                                                showModelDialog = true
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("💾 Export Chat JSON") },
                                            onClick = {
                                                showMenu = false
                                                val json = viewModel.exportChatAsJson()
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, json)
                                                    type = "application/json"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Export CampusMind Chat")
                                                context.startActivity(shareIntent)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🗑 Clear Chat") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.clearChat()
                                                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                            .navigationBarsPadding()
                    ) {
                        // 1. Non-default Persona Banner
                        if (activePersona.id != "campus_assistant") {
                            Surface(
                                color = CampusOrange.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPersonaSheet = true }
                                    .border(1.dp, CampusOrange.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = activePersona.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${activePersona.title} mode active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CampusOrange
                                        )
                                    }
                                    Text(
                                        text = "Change",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CampusOrange
                                    )
                                }
                            }
                        }

                        // 2. Active Document Banner
                        if (activeDocument != null) {
                            Surface(
                                color = CampusBlue.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CampusBlue.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = CampusBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${activeDocument!!.title} loaded — asking questions!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CampusBlue,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearDocument() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unload Document",
                                            tint = CampusBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Chat Messages / Empty State
                        Box(modifier = Modifier.weight(1f)) {
                            if (messages.isEmpty()) {
                                EmptyChatState(
                                    persona = activePersona,
                                    onSuggestionClick = { suggestion ->
                                        viewModel.sendMessage(suggestion)
                                    },
                                    onOpenDocumentClick = { showDocumentSheet = true },
                                    onOpenMemoryClick = { showMemorySheet = true }
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(messages, key = { it.id }) { message ->
                                        ChatMessageItem(
                                            message = message,
                                            isDarkMode = isDarkMode,
                                            isSpeakingThis = isSpeaking,
                                            onSpeakClick = { viewModel.speakText(message.content) },
                                            onStopSpeakClick = { viewModel.stopSpeaking() }
                                        )
                                    }

                                    if (isLoading) {
                                        item {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = CampusOrange
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "CampusMind AI is thinking…",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Stats bar
                        val turns = messages.size / 2
                        val words = messages.sumOf { it.content.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
                        if (messages.isNotEmpty()) {
                            StatsBar(turns = turns, words = words)
                        }

                        // 5. Input row
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Fast Document shortcut
                                IconButton(
                                    onClick = { showDocumentSheet = true },
                                    modifier = Modifier.size(40.dp).testTag("attach_notes_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Lecture Notes Q&A",
                                        tint = if (activeDocument != null) CampusBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Message ${activePersona.title}…",
                                            fontSize = 13.sp
                                        )
                                    },
                                    maxLines = 4,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CampusOrange,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .testTag("chat_input_field")
                                )

                                IconButton(
                                    onClick = {
                                        if (inputText.isNotBlank()) {
                                            val toSend = inputText
                                            inputText = ""
                                            viewModel.sendMessage(toSend)
                                        }
                                    },
                                    enabled = inputText.isNotBlank() && !isLoading,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = if (inputText.isNotBlank()) CampusOrange else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (inputText.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Sheets and Dialogs
                if (showPersonaSheet) {
                    PersonaSelectorSheet(
                        currentPersona = activePersona,
                        onPersonaSelected = { viewModel.selectPersona(it) },
                        onDismiss = { showPersonaSheet = false }
                    )
                }

                if (showMemorySheet) {
                    MemorySheet(
                        memory = studentMemory,
                        onUpdateName = { viewModel.updateStudentName(it) },
                        onUpdateMajor = { viewModel.updateStudentMajor(it) },
                        onUpdateYear = { viewModel.updateStudentYear(it) },
                        onUpdateUniversity = { viewModel.updateStudentUniversity(it) },
                        onAddCustomFact = { k, v -> viewModel.addCustomFact(k, v) },
                        onClearMemory = { viewModel.clearMemory() },
                        onDismiss = { showMemorySheet = false }
                    )
                }

                if (showDocumentSheet) {
                    DocumentSheet(
                        activeDocument = activeDocument,
                        onLoadDocument = { t, c -> viewModel.loadCustomDocument(t, c) },
                        onLoadSampleLecture = { viewModel.loadSampleLecture(it) },
                        onClearDocument = { viewModel.clearDocument() },
                        onDismiss = { showDocumentSheet = false }
                    )
                }

                if (showModelDialog) {
                    ModelSelectorDialog(
                        selectedModel = selectedModel,
                        onModelSelected = { viewModel.selectModel(it) },
                        onDismiss = { showModelDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(
    persona: com.example.data.model.CampusPersona,
    onSuggestionClick: (String) -> Unit,
    onOpenDocumentClick: () -> Unit,
    onOpenMemoryClick: () -> Unit
) {
    val suggestions = when (persona.id) {
        "python_tutor" -> listOf(
            "Explain list comprehensions with an example",
            "Write a function to invert a binary tree",
            "What's the difference between @staticmethod and @classmethod?"
        )
        "writing_coach" -> listOf(
            "Review this thesis statement for clarity",
            "How do I transition between argument paragraphs?",
            "Make this academic paragraph more concise"
        )
        "study_planner" -> listOf(
            "Help me create a 5-day study plan for finals",
            "How should I split revision across 3 subjects?",
            "Techniques to prevent exam burnout"
        )
        "research_helper" -> listOf(
            "How do I write an empirical literature review?",
            "What are the best IEEE keywords for RAG architectures?",
            "Help me formulate research questions"
        )
        else -> listOf(
            "I'm studying Computer Science at IIUI",
            "Explain static vs automatic variables in C++",
            "Give me 3 proven techniques for active recall"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(CampusOrange.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .border(1.dp, CampusOrange, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = persona.emoji, fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = persona.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = persona.greeting,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Suggested Questions:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CampusOrange
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (suggestion in suggestions) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .testTag("suggestion_chip_${suggestion.take(15)}")
                ) {
                    Text(
                        text = "💬 $suggestion",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CampusBlue.copy(alpha = 0.12f),
                modifier = Modifier
                    .border(1.dp, CampusBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onOpenDocumentClick() }
            ) {
                Text(
                    text = "📄 Load Lecture Notes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CampusBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CampusOrange.copy(alpha = 0.12f),
                modifier = Modifier
                    .border(1.dp, CampusOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onOpenMemoryClick() }
            ) {
                Text(
                    text = "🧠 Set Student Memory",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CampusOrange,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
