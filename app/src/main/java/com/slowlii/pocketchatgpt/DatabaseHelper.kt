package com.slowlii.pocketchatgpt

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ChatDatabase.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_MESSAGES = "MessageTable"
        private const val KEY_ID = "id"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_ROLE = "role"
        private const val KEY_CONTENT = "content"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_MESSAGES_TABLE = ("CREATE TABLE $TABLE_MESSAGES (" +
                                    "$KEY_ID INTEGER PRIMARY KEY," +
                                    "$KEY_CHAT_ID TEXT," +
                                    "$KEY_ROLE TEXT," +
                                    "$KEY_CONTENT TEXT)")

        db.execSQL(CREATE_MESSAGES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    fun addChatMessage(chatId: String, role: String, content: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(KEY_CHAT_ID, chatId)
            put(KEY_ROLE, role)
            put(KEY_CONTENT, content)
        }
        db.insert(TABLE_MESSAGES, null, contentValues)
        db.close()
    }

    fun getChatMessages(chatId: String): List<Message> {
        val messages = mutableListOf<Message>()
        val selectQuery = "SELECT * FROM $TABLE_MESSAGES WHERE $KEY_CHAT_ID = ?"
        val db = this.readableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(selectQuery, arrayOf(chatId))
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return emptyList()
        }

        if (cursor.moveToFirst()) {
            val messageIdIndex = cursor.getColumnIndexOrThrow(KEY_ID)
            val roleIndex = cursor.getColumnIndexOrThrow(KEY_ROLE)
            val contentIndex = cursor.getColumnIndexOrThrow(KEY_CONTENT)

            do {
                val messageId = cursor.getInt(messageIdIndex)
                val role = cursor.getString(roleIndex)
                val content = cursor.getString(contentIndex)
                val message = Message(chatId, role, content, messageId)
                messages.add(message)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return messages
    }

    fun getChatHistory(chatId: String): MutableList<MutableMap<String, String>> {
        val chatHistory: MutableList<MutableMap<String, String>> = mutableListOf()
        val selectQuery = "SELECT * FROM $TABLE_MESSAGES WHERE $KEY_CHAT_ID = ?"
        val db = this.readableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(selectQuery, arrayOf(chatId))
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return chatHistory
        }

        if (cursor.moveToFirst()) {
            val roleIndex = cursor.getColumnIndexOrThrow(KEY_ROLE)
            val contentIndex = cursor.getColumnIndexOrThrow(KEY_CONTENT)

            do {
                val role = cursor.getString(roleIndex)
                val content = cursor.getString(contentIndex)
                val message = mutableMapOf<String, String>("role" to role, "content" to content)
                chatHistory.add(message)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return chatHistory
    }

    fun deleteMessagesByChatId(chatId: String) {
        val db = this.writableDatabase
        val whereClause = "$KEY_CHAT_ID = ?"
        val whereArgs = arrayOf(chatId)
        db.delete(TABLE_MESSAGES, whereClause, whereArgs)
        db.close()
    }

    fun getChatList(): List<Chat> {
        val chatList = mutableListOf<Chat>()
        val selectQuery = "SELECT DISTINCT $KEY_CHAT_ID FROM $TABLE_MESSAGES"
        val db = this.readableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return chatList
        }

        if (cursor.moveToFirst()) {
            val chatIdIndex = cursor.getColumnIndexOrThrow(KEY_CHAT_ID)

            do {
                val chatId = cursor.getString(chatIdIndex)
                val chat = Chat(chatId) // Chat должен быть создан соответствующим образом
                chatList.add(chat)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return chatList
    }

    fun getLastId(): String {
        val selectQuery = "SELECT $KEY_CHAT_ID FROM $TABLE_MESSAGES ORDER BY $KEY_ID DESC LIMIT 1"
        val db = this.readableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return generateNewChatId()
        }

        val lastId = if (cursor.moveToFirst()) {
            val lastChatId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAT_ID))
            if (lastChatId.isNullOrEmpty()) generateNewChatId() else lastChatId
        } else {
            generateNewChatId()
        }

        cursor.close()
        return lastId
    }


    // Функция для генерации нового идентификатора чата
    fun generateNewChatId(): String {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // добавляем 1, так как месяцы в Calendar начинаются с 0
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        val formattedChatId = String.format(
            "%04d %02d %02d %02d:%02d:%02d",
            year,
            month,
            day,
            hours,
            minutes,
            seconds
        )

        return formattedChatId
    }


    fun deleteChat(chatId: String): Boolean {
        val db = this.writableDatabase
        val whereClause = "$KEY_CHAT_ID = ?"
        val whereArgs = arrayOf(chatId)

        return try {
            db.beginTransaction()
            // Удаляем сообщения, связанные с чатом
            db.delete(TABLE_MESSAGES, whereClause, whereArgs)
            // Здесь можно добавить дополнительные действия по удалению других связанных данных
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
            db.close()
        }
    }

}