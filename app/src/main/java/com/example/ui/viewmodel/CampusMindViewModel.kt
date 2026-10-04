package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.ChatMessage
import com.example.data.model.CampusPersona
import com.example.data.model.ModelOption
import com.example.data.model.ModelOptions
import com.example.data.model.Personas
import com.example.data.model.StudentMemory
import com.example.data.rag.DocumentStore
import com.example.data.rag.LoadedDocument
import com.example.data.rag.SampleLecture
import com.example.data.storage.PersistenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class CampusMindViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val persistence = PersistenceManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activePersona = MutableStateFlow(Personas.CAMPUS_ASSISTANT)
    val activePersona: StateFlow<CampusPersona> = _activePersona.asStateFlow()

    private val _studentMemory = MutableStateFlow(StudentMemory())
    val studentMemory: StateFlow<StudentMemory> = _studentMemory.asStateFlow()

    private val _activeDocument = MutableStateFlow<LoadedDocument?>(null)
    val activeDocument: StateFlow<LoadedDocument?> = _activeDocument.asStateFlow()

    private val _selectedModel = MutableStateFlow(ModelOptions.GEMINI_FLASH)
    val selectedModel: StateFlow<ModelOption> = _selectedModel.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Restore saved memory
        _studentMemory.value = persistence.loadMemory()

        // Restore saved messages
        val savedMsgs = persistence.loadMessages()
        _messages.value = savedMsgs

        // Restore persona
        val savedPersonaId = persistence.loadSelectedPersonaId()
        _activePersona.value = Personas.ALL.find { it.id == savedPersonaId } ?: Personas.CAMPUS_ASSISTANT

        // Restore model
        val savedModelId = persistence.loadSelectedModelId()
        _selectedModel.value = ModelOptions.ALL.find { it.id == savedModelId } ?: ModelOptions.GEMINI_FLASH

        // Restore dark mode
        _isDarkMode.value = persistence.loadIsDarkMode()

        // Initialize TextToSpeech
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            // TTS unavailable
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        // 1. Auto-extract student memory
        val updatedMemory = StudentMemory.extractFromMessage(_studentMemory.value, trimmed)
        if (updatedMemory != _studentMemory.value) {
            _studentMemory.value = updatedMemory
            persistence.saveMemory(updatedMemory)
        }

        // 2. Add user message
        val userMsg = ChatMessage(
            role = "user",
            content = trimmed,
            personaTitle = _activePersona.value.title
        )
        val currentList = _messages.value + userMsg
        _messages.value = currentList
        persistence.saveMessages(currentList)

        // 3. Assemble system prompt
        val basePrompt = _activePersona.value.systemPrompt
        val memoryBlock = _studentMemory.value.toPromptBlock()
        val combinedPromptWithMemory = if (memoryBlock.isNotBlank()) {
            "$basePrompt\n\n$memoryBlock"
        } else {
            basePrompt
        }

        // 4. Retrieve RAG excerpts if document is loaded
        val excerpts = DocumentStore.retrieveRelevantContext(trimmed)
        val finalSystemPrompt = DocumentStore.buildSystemPromptWithDocument(
            basePrompt = combinedPromptWithMemory,
            excerpts = excerpts
        )

        // 5. Call API
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val modelId = _selectedModel.value.id
            val result = GeminiClient.generateResponse(
                modelId = modelId,
                systemPrompt = finalSystemPrompt,
                history = currentList,
                latestUserMessage = trimmed
            )

            _isLoading.value = false

            result.fold(
                onSuccess = { reply ->
                    val assistantMsg = ChatMessage(
                        role = "assistant",
                        content = reply,
                        modelName = _selectedModel.value.displayName,
                        personaTitle = _activePersona.value.title
                    )
                    val newList = _messages.value + assistantMsg
                    _messages.value = newList
                    persistence.saveMessages(newList)
                },
                onFailure = { error ->
                    val fallbackContent = generateOfflineOrHelpResponse(trimmed, error.message.orEmpty())
                    val errorMsg = ChatMessage(
                        role = "assistant",
                        content = fallbackContent,
                        modelName = _selectedModel.value.displayName,
                        personaTitle = _activePersona.value.title,
                        isError = false
                    )
                    val newList = _messages.value + errorMsg
                    _messages.value = newList
                    persistence.saveMessages(newList)
                }
            )
        }
    }

    private fun generateOfflineOrHelpResponse(query: String, errorDetails: String): String {
        val apiKey = GeminiClient.getApiKey()
        val hasKey = apiKey.isNotBlank()

        val persona = _activePersona.value
        val nameGreeting = _studentMemory.value.name?.let { " $it" } ?: ""

        val sb = StringBuilder()

        if (!hasKey) {
            sb.append("👋 Hi$nameGreeting! I'm running in local prototype mode. To enable live cloud responses from ${_selectedModel.value.displayName}:\n\n")
            sb.append("1. Open the **Secrets panel** in Google AI Studio.\n")
            sb.append("2. Add or verify `GEMINI_API_KEY`.\n\n")
            sb.append("---\n\n")
        }

        // Provide educational context-aware help based on active persona and document
        val activeDoc = _activeDocument.value
        if (activeDoc != null) {
            val excerpts = DocumentStore.retrieveRelevantContext(query, maxChunks = 2)
            sb.append("📄 **Document Analysis (${activeDoc.title}):**\n\n")
            if (excerpts.isNotBlank()) {
                sb.append("Based on the relevant lecture excerpts:\n\n")
                sb.append(excerpts.replace("[Excerpt 1]\n", "").replace("[Excerpt 2]\n", ""))
                sb.append("\n\n*Tip: Connect your Gemini API key for full multi-paragraph reasoning and conversational synthesis!*")
            } else {
                sb.append("The document does not explicitly match this search query. Try asking about topics present in ${activeDoc.title}.")
            }
            return sb.toString()
        }

        when (persona.id) {
            "python_tutor" -> {
                sb.append("🐍 **Python Tutor Answer:**\n\n")
                sb.append("Here is a clean Python pattern related to your query:\n\n")
                sb.append("```python\n")
                sb.append("# CampusMind AI - Python Tutor Example\n")
                sb.append("def study_helper(topic: str) -> dict:\n")
                sb.append("    \"\"\"Quick template for working with student tasks.\"\"\"\n")
                sb.append("    return {\n")
                sb.append("        'topic': topic,\n")
                sb.append("        'status': 'reviewed',\n")
                sb.append("        'focus_areas': ['syntax', 'algorithmic_complexity', 'clean_code']\n")
                sb.append("    }\n\n")
                sb.append("result = study_helper('${query.take(30)}')\n")
                sb.append("print(result)\n")
                sb.append("```\n\n")
                sb.append("Feel free to paste your code or error traceback!")
            }
            "writing_coach" -> {
                sb.append("✍️ **Writing Coach Feedback:**\n\n")
                sb.append("For academic writing clarity regarding '$query':\n\n")
                sb.append("- **Structure:** Use the Claim → Evidence → Analysis formula for strong body paragraphs.\n")
                sb.append("- **Conciseness:** Replace passive constructions with active voice.\n")
                sb.append("- **Flow:** Ensure seamless transitions between topic sentences.\n\n")
                sb.append("Share a draft paragraph for direct line edits!")
            }
            "study_planner" -> {
                sb.append("📊 **Study Planner Strategy:**\n\n")
                sb.append("Here is an optimal revision schedule for your coursework:\n\n")
                sb.append("| Time Block | Task | Focus |\n")
                sb.append("|---|---|---|\n")
                sb.append("| 09:00 - 10:30 | Core Concept Review | Active recall & flashcards |\n")
                sb.append("| 10:45 - 12:00 | Practice Problems | High-difficulty problem sets |\n")
                sb.append("| 13:00 - 14:00 | Lecture Summary | Summarize key definitions in 1 page |\n")
            }
            "research_helper" -> {
                sb.append("🌍 **Research Assistant Guidance:**\n\n")
                sb.append("When researching '$query':\n\n")
                sb.append("1. **Search Keywords:** Combine key terminology with operators (e.g. `\"$query\" AND \"empirical study\"`).\n")
                sb.append("2. **Academic Databases:** Check IEEE Xplore, ACM Digital Library, Google Scholar, and JSTOR.\n")
                sb.append("3. **Source Evaluation:** Prioritize peer-reviewed journal papers from the last 3-5 years.")
            }
            else -> {
                sb.append("🎓 **Campus Assistant:**\n\n")
                sb.append("I'm here to help with your academic questions, coursework, and revision.\n\n")
                if (_studentMemory.value.major != null) {
                    sb.append("Since you're studying ${_studentMemory.value.major}, I can tailor coding explanations, study schedules, or lecture notes specifically for your semester.\n\n")
                }
                sb.append("Try switching to any of the 5 personas or uploading lecture notes using the top bar!")
            }
        }

        return sb.toString()
    }

    fun selectPersona(persona: CampusPersona) {
        _activePersona.value = persona
        persistence.saveSelectedPersonaId(persona.id)
    }

    fun selectModel(model: ModelOption) {
        _selectedModel.value = model
        persistence.saveSelectedModelId(model.id)
    }

    fun toggleDarkMode() {
        val newDark = !_isDarkMode.value
        _isDarkMode.value = newDark
        persistence.saveIsDarkMode(newDark)
    }

    fun updateStudentName(name: String) {
        val updated = _studentMemory.value.copy(name = name.takeIf { it.isNotBlank() })
        _studentMemory.value = updated
        persistence.saveMemory(updated)
    }

    fun updateStudentMajor(major: String) {
        val updated = _studentMemory.value.copy(major = major.takeIf { it.isNotBlank() })
        _studentMemory.value = updated
        persistence.saveMemory(updated)
    }

    fun updateStudentYear(year: String) {
        val updated = _studentMemory.value.copy(year = year.takeIf { it.isNotBlank() })
        _studentMemory.value = updated
        persistence.saveMemory(updated)
    }

    fun updateStudentUniversity(uni: String) {
        val updated = _studentMemory.value.copy(university = uni.takeIf { it.isNotBlank() })
        _studentMemory.value = updated
        persistence.saveMemory(updated)
    }

    fun addCustomFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        val currentFacts = _studentMemory.value.customFacts.toMutableMap()
        currentFacts[key.trim()] = value.trim()
        val updated = _studentMemory.value.copy(customFacts = currentFacts)
        _studentMemory.value = updated
        persistence.saveMemory(updated)
    }

    fun clearMemory() {
        val empty = StudentMemory()
        _studentMemory.value = empty
        persistence.saveMemory(empty)
    }

    fun loadCustomDocument(title: String, text: String) {
        val doc = DocumentStore.loadDocument(title, text)
        _activeDocument.value = doc
    }

    fun loadSampleLecture(sample: SampleLecture) {
        val doc = DocumentStore.loadDocument(sample.title, sample.content)
        _activeDocument.value = doc
    }

    fun clearDocument() {
        DocumentStore.clearDocument()
        _activeDocument.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
        persistence.saveMessages(emptyList())
        stopSpeaking()
    }

    fun exportChatAsJson(): String {
        val array = JSONArray()
        for (msg in _messages.value) {
            val obj = JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
                put("timestamp", msg.timestamp)
                put("model", msg.modelName)
                put("persona", msg.personaTitle)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun speakText(text: String) {
        if (!isTtsInitialized || tts == null) return
        stopSpeaking()
        _isSpeaking.value = true
        // Clean markdown for speech
        val cleaned = text
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), "Code snippet omitted.")
            .replace(Regex("[#*_`~]"), "")
            .take(600)

        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "CampusMindTTS")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
