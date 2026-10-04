package com.example.data.local.dao

import android.content.ContentValues
import android.database.Cursor
import com.example.data.local.db.CampusMindDatabaseHelper
import com.example.data.local.entities.AttachmentEntity
import com.example.data.local.entities.ChatEntity
import com.example.data.local.entities.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

interface ChatDao {
    fun getAllChats(): Flow<List<ChatEntity>>
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    suspend fun getChatById(chatId: String): ChatEntity?
    suspend fun insertChat(chat: ChatEntity)
    suspend fun updateChat(chat: ChatEntity)
    suspend fun setChatPinned(chatId: String, pinned: Boolean)
    suspend fun renameChat(chatId: String, newTitle: String)
    suspend fun deleteChat(chatId: String)
    suspend fun insertMessage(message: MessageEntity)
    suspend fun updateMessage(message: MessageEntity)
    suspend fun deleteMessagesAfter(chatId: String, messageId: String)
    suspend fun insertAttachment(attachment: AttachmentEntity)
    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity>
    fun searchChats(query: String): Flow<List<ChatEntity>>
    suspend fun clearAllData()
}

class ChatDaoImpl(private val dbHelper: CampusMindDatabaseHelper) : ChatDao {

    private val chatUpdateNotifier = MutableSharedFlow<Unit>(replay = 1)
    private val messageUpdateNotifier = MutableSharedFlow<String>(replay = 1)

    init {
        chatUpdateNotifier.tryEmit(Unit)
    }

    override fun getAllChats(): Flow<List<ChatEntity>> = flow {
        chatUpdateNotifier.collect {
            val list = queryAllChats()
            emit(list)
        }
    }.onStart { emit(queryAllChats()) }.flowOn(Dispatchers.IO)

    private fun queryAllChats(): List<ChatEntity> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            CampusMindDatabaseHelper.TABLE_CHATS,
            null,
            null,
            null,
            null,
            null,
            "${CampusMindDatabaseHelper.COL_CHAT_PINNED} DESC, ${CampusMindDatabaseHelper.COL_CHAT_UPDATED} DESC"
        )
        val result = mutableListOf<ChatEntity>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_TITLE)
            val pinnedIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_PINNED)
            val createdIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_CREATED)
            val updatedIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_UPDATED)

            while (cursor.moveToNext()) {
                result.add(
                    ChatEntity(
                        id = cursor.getString(idIdx),
                        title = cursor.getString(titleIdx),
                        isPinned = cursor.getInt(pinnedIdx) == 1,
                        createdAt = cursor.getLong(createdIdx),
                        updatedAt = cursor.getLong(updatedIdx)
                    )
                )
            }
        }
        return result
    }

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = flow {
        emit(queryMessages(chatId))
        messageUpdateNotifier.collect { updatedChatId ->
            if (updatedChatId == chatId) {
                emit(queryMessages(chatId))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun queryMessages(chatId: String): List<MessageEntity> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            CampusMindDatabaseHelper.TABLE_MESSAGES,
            null,
            "${CampusMindDatabaseHelper.COL_MSG_CHAT_ID} = ?",
            arrayOf(chatId),
            null,
            null,
            "${CampusMindDatabaseHelper.COL_MSG_CREATED} ASC"
        )
        val result = mutableListOf<MessageEntity>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_ID)
            val chatIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_CHAT_ID)
            val roleIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_ROLE)
            val contentIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_CONTENT)
            val thinkIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_THINKING)
            val statusIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_STATUS)
            val createdIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_MSG_CREATED)

            while (cursor.moveToNext()) {
                result.add(
                    MessageEntity(
                        id = cursor.getString(idIdx),
                        chatId = cursor.getString(chatIdx),
                        role = cursor.getString(roleIdx),
                        content = cursor.getString(contentIdx),
                        thinkingContent = if (cursor.isNull(thinkIdx)) null else cursor.getString(thinkIdx),
                        status = cursor.getString(statusIdx),
                        createdAt = cursor.getLong(createdIdx)
                    )
                )
            }
        }
        return result
    }

    override suspend fun getChatById(chatId: String): ChatEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            CampusMindDatabaseHelper.TABLE_CHATS,
            null,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(chatId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                ChatEntity(
                    id = it.getString(it.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_TITLE)),
                    isPinned = it.getInt(it.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_PINNED)) == 1,
                    createdAt = it.getLong(it.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_CREATED)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_UPDATED))
                )
            } else null
        }
    }

    override suspend fun insertChat(chat: ChatEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_CHAT_ID, chat.id)
            put(CampusMindDatabaseHelper.COL_CHAT_TITLE, chat.title)
            put(CampusMindDatabaseHelper.COL_CHAT_PINNED, if (chat.isPinned) 1 else 0)
            put(CampusMindDatabaseHelper.COL_CHAT_CREATED, chat.createdAt)
            put(CampusMindDatabaseHelper.COL_CHAT_UPDATED, chat.updatedAt)
        }
        db.insertWithOnConflict(
            CampusMindDatabaseHelper.TABLE_CHATS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun updateChat(chat: ChatEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_CHAT_TITLE, chat.title)
            put(CampusMindDatabaseHelper.COL_CHAT_PINNED, if (chat.isPinned) 1 else 0)
            put(CampusMindDatabaseHelper.COL_CHAT_UPDATED, chat.updatedAt)
        }
        db.update(
            CampusMindDatabaseHelper.TABLE_CHATS,
            values,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(chat.id)
        )
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun setChatPinned(chatId: String, pinned: Boolean): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_CHAT_PINNED, if (pinned) 1 else 0)
            put(CampusMindDatabaseHelper.COL_CHAT_UPDATED, System.currentTimeMillis())
        }
        db.update(
            CampusMindDatabaseHelper.TABLE_CHATS,
            values,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(chatId)
        )
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun renameChat(chatId: String, newTitle: String): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_CHAT_TITLE, newTitle.trim())
            put(CampusMindDatabaseHelper.COL_CHAT_UPDATED, System.currentTimeMillis())
        }
        db.update(
            CampusMindDatabaseHelper.TABLE_CHATS,
            values,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(chatId)
        )
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun deleteChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            CampusMindDatabaseHelper.TABLE_CHATS,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(chatId)
        )
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun insertMessage(message: MessageEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_MSG_ID, message.id)
            put(CampusMindDatabaseHelper.COL_MSG_CHAT_ID, message.chatId)
            put(CampusMindDatabaseHelper.COL_MSG_ROLE, message.role)
            put(CampusMindDatabaseHelper.COL_MSG_CONTENT, message.content)
            put(CampusMindDatabaseHelper.COL_MSG_THINKING, message.thinkingContent)
            put(CampusMindDatabaseHelper.COL_MSG_STATUS, message.status)
            put(CampusMindDatabaseHelper.COL_MSG_CREATED, message.createdAt)
        }
        db.insertWithOnConflict(
            CampusMindDatabaseHelper.TABLE_MESSAGES,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        // Touch chat updated_at
        val chatValues = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_CHAT_UPDATED, System.currentTimeMillis())
        }
        db.update(
            CampusMindDatabaseHelper.TABLE_CHATS,
            chatValues,
            "${CampusMindDatabaseHelper.COL_CHAT_ID} = ?",
            arrayOf(message.chatId)
        )

        messageUpdateNotifier.emit(message.chatId)
        chatUpdateNotifier.emit(Unit)
    }

    override suspend fun updateMessage(message: MessageEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_MSG_CONTENT, message.content)
            put(CampusMindDatabaseHelper.COL_MSG_THINKING, message.thinkingContent)
            put(CampusMindDatabaseHelper.COL_MSG_STATUS, message.status)
        }
        db.update(
            CampusMindDatabaseHelper.TABLE_MESSAGES,
            values,
            "${CampusMindDatabaseHelper.COL_MSG_ID} = ?",
            arrayOf(message.id)
        )
        messageUpdateNotifier.emit(message.chatId)
    }

    override suspend fun deleteMessagesAfter(chatId: String, messageId: String): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        // Find created_at of target message
        val cursor = db.query(
            CampusMindDatabaseHelper.TABLE_MESSAGES,
            arrayOf(CampusMindDatabaseHelper.COL_MSG_CREATED),
            "${CampusMindDatabaseHelper.COL_MSG_ID} = ?",
            arrayOf(messageId),
            null,
            null,
            null
        )
        var targetCreatedAt: Long? = null
        cursor.use {
            if (it.moveToFirst()) {
                targetCreatedAt = it.getLong(0)
            }
        }

        if (targetCreatedAt != null) {
            db.delete(
                CampusMindDatabaseHelper.TABLE_MESSAGES,
                "${CampusMindDatabaseHelper.COL_MSG_CHAT_ID} = ? AND ${CampusMindDatabaseHelper.COL_MSG_CREATED} > ?",
                arrayOf(chatId, targetCreatedAt.toString())
            )
            messageUpdateNotifier.emit(chatId)
        }
    }

    override suspend fun insertAttachment(attachment: AttachmentEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CampusMindDatabaseHelper.COL_ATT_ID, attachment.id)
            put(CampusMindDatabaseHelper.COL_ATT_MSG_ID, attachment.messageId)
            put(CampusMindDatabaseHelper.COL_ATT_NAME, attachment.name)
            put(CampusMindDatabaseHelper.COL_ATT_MIME, attachment.mimeType)
            put(CampusMindDatabaseHelper.COL_ATT_URI, attachment.uri)
            put(CampusMindDatabaseHelper.COL_ATT_SIZE, attachment.sizeBytes)
        }
        db.insertWithOnConflict(
            CampusMindDatabaseHelper.TABLE_ATTACHMENTS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    override suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            CampusMindDatabaseHelper.TABLE_ATTACHMENTS,
            null,
            "${CampusMindDatabaseHelper.COL_ATT_MSG_ID} = ?",
            arrayOf(messageId),
            null,
            null,
            null
        )
        val result = mutableListOf<AttachmentEntity>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_ID)
            val msgIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_MSG_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_MIME)
            val uriIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_URI)
            val sizeIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_ATT_SIZE)

            while (cursor.moveToNext()) {
                result.add(
                    AttachmentEntity(
                        id = cursor.getString(idIdx),
                        messageId = cursor.getString(msgIdx),
                        name = cursor.getString(nameIdx),
                        mimeType = cursor.getString(mimeIdx),
                        uri = if (cursor.isNull(uriIdx)) null else cursor.getString(uriIdx),
                        sizeBytes = cursor.getLong(sizeIdx)
                    )
                )
            }
        }
        result
    }

    override fun searchChats(query: String): Flow<List<ChatEntity>> = flow {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            CampusMindDatabaseHelper.TABLE_CHATS,
            null,
            "${CampusMindDatabaseHelper.COL_CHAT_TITLE} LIKE ?",
            arrayOf("%$query%"),
            null,
            null,
            "${CampusMindDatabaseHelper.COL_CHAT_PINNED} DESC, ${CampusMindDatabaseHelper.COL_CHAT_UPDATED} DESC"
        )
        val result = mutableListOf<ChatEntity>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_TITLE)
            val pinnedIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_PINNED)
            val createdIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_CREATED)
            val updatedIdx = cursor.getColumnIndexOrThrow(CampusMindDatabaseHelper.COL_CHAT_UPDATED)

            while (cursor.moveToNext()) {
                result.add(
                    ChatEntity(
                        id = cursor.getString(idIdx),
                        title = cursor.getString(titleIdx),
                        isPinned = cursor.getInt(pinnedIdx) == 1,
                        createdAt = cursor.getLong(createdIdx),
                        updatedAt = cursor.getLong(updatedIdx)
                    )
                )
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override suspend fun clearAllData(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(CampusMindDatabaseHelper.TABLE_CHATS, null, null)
        db.delete(CampusMindDatabaseHelper.TABLE_MESSAGES, null, null)
        db.delete(CampusMindDatabaseHelper.TABLE_ATTACHMENTS, null, null)
        chatUpdateNotifier.emit(Unit)
    }
}
