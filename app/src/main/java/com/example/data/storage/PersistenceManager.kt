package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ChatMessage
import com.example.data.model.StudentMemory
import org.json.JSONArray
import org.json.JSONObject

class PersistenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("campusmind_prefs", Context.MODE_PRIVATE)

    fun saveMemory(memory: StudentMemory) {
        val json = JSONObject().apply {
            put("name", memory.name ?: "")
            put("major", memory.major ?: "")
            put("year", memory.year ?: "")
            put("university", memory.university ?: "")

            val customObj = JSONObject()
            for ((k, v) in memory.customFacts) {
                customObj.put(k, v)
            }
            put("customFacts", customObj)
        }
        prefs.edit().putString(KEY_MEMORY, json.toString()).apply()
    }

    fun loadMemory(): StudentMemory {
        val jsonStr = prefs.getString(KEY_MEMORY, null) ?: return StudentMemory()
        return try {
            val json = JSONObject(jsonStr)
            val customMap = mutableMapOf<String, String>()
            val customObj = json.optJSONObject("customFacts")
            if (customObj != null) {
                val keys = customObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    customMap[k] = customObj.optString(k)
                }
            }

            StudentMemory(
                name = json.optString("name").takeIf { it.isNotBlank() },
                major = json.optString("major").takeIf { it.isNotBlank() },
                year = json.optString("year").takeIf { it.isNotBlank() },
                university = json.optString("university").takeIf { it.isNotBlank() },
                customFacts = customMap
            )
        } catch (e: Exception) {
            StudentMemory()
        }
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val array = JSONArray()
        // Save at most the latest 50 messages to keep storage snappy
        for (msg in messages.takeLast(50)) {
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("role", msg.role)
                put("content", msg.content)
                put("timestamp", msg.timestamp)
                put("modelName", msg.modelName)
                put("personaTitle", msg.personaTitle)
                put("isError", msg.isError)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    fun loadMessages(): List<ChatMessage> {
        val jsonStr = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        role = obj.optString("role"),
                        content = obj.optString("content"),
                        timestamp = obj.optLong("timestamp"),
                        modelName = obj.optString("modelName"),
                        personaTitle = obj.optString("personaTitle"),
                        isError = obj.optBoolean("isError")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSelectedPersonaId(personaId: String) {
        prefs.edit().putString(KEY_PERSONA, personaId).apply()
    }

    fun loadSelectedPersonaId(): String {
        return prefs.getString(KEY_PERSONA, "campus_assistant") ?: "campus_assistant"
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
    }

    fun loadSelectedModelId(): String {
        return prefs.getString(KEY_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun saveIsDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun loadIsDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_MEMORY = "key_memory"
        private const val KEY_MESSAGES = "key_messages"
        private const val KEY_PERSONA = "key_persona"
        private const val KEY_MODEL = "key_model"
        private const val KEY_DARK_MODE = "key_dark_mode"
    }
}
