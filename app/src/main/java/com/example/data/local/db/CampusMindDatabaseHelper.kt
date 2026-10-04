package com.example.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CampusMindDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "campusmind.db"
        const val DATABASE_VERSION = 1

        const val TABLE_CHATS = "chats"
        const val COL_CHAT_ID = "id"
        const val COL_CHAT_TITLE = "title"
        const val COL_CHAT_PINNED = "is_pinned"
        const val COL_CHAT_CREATED = "created_at"
        const val COL_CHAT_UPDATED = "updated_at"

        const val TABLE_MESSAGES = "messages"
        const val COL_MSG_ID = "id"
        const val COL_MSG_CHAT_ID = "chat_id"
        const val COL_MSG_ROLE = "role"
        const val COL_MSG_CONTENT = "content"
        const val COL_MSG_THINKING = "thinking_content"
        const val COL_MSG_STATUS = "status"
        const val COL_MSG_CREATED = "created_at"

        const val TABLE_ATTACHMENTS = "attachments"
        const val COL_ATT_ID = "id"
        const val COL_ATT_MSG_ID = "message_id"
        const val COL_ATT_NAME = "name"
        const val COL_ATT_MIME = "mime_type"
        const val COL_ATT_URI = "uri"
        const val COL_ATT_SIZE = "size_bytes"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CHATS (
                $COL_CHAT_ID TEXT PRIMARY KEY,
                $COL_CHAT_TITLE TEXT NOT NULL,
                $COL_CHAT_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_CHAT_CREATED INTEGER NOT NULL,
                $COL_CHAT_UPDATED INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                $COL_MSG_ID TEXT PRIMARY KEY,
                $COL_MSG_CHAT_ID TEXT NOT NULL,
                $COL_MSG_ROLE TEXT NOT NULL,
                $COL_MSG_CONTENT TEXT NOT NULL,
                $COL_MSG_THINKING TEXT,
                $COL_MSG_STATUS TEXT NOT NULL,
                $COL_MSG_CREATED INTEGER NOT NULL,
                FOREIGN KEY ($COL_MSG_CHAT_ID) REFERENCES $TABLE_CHATS($COL_CHAT_ID) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ATTACHMENTS (
                $COL_ATT_ID TEXT PRIMARY KEY,
                $COL_ATT_MSG_ID TEXT NOT NULL,
                $COL_ATT_NAME TEXT NOT NULL,
                $COL_ATT_MIME TEXT NOT NULL,
                $COL_ATT_URI TEXT,
                $COL_ATT_SIZE INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_ATT_MSG_ID) REFERENCES $TABLE_MESSAGES($COL_MSG_ID) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        // Indexes for lightning fast queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON $TABLE_MESSAGES($COL_MSG_CHAT_ID);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chats_pinned ON $TABLE_CHATS($COL_CHAT_PINNED);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chats_updated ON $TABLE_CHATS($COL_CHAT_UPDATED DESC);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations
    }
}
