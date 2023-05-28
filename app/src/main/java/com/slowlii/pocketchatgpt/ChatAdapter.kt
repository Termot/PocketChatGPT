package com.slowlii.pocketchatgpt

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val chatList: MutableList<String>,
    private val onChatClicked: (String) -> Unit
) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatItemTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val textView = TextView(parent.context)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.setPadding(16, 16, 16, 16)
        textView.setTextColor(Color.WHITE)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        textView.gravity = Gravity.CENTER_VERTICAL
        return ChatViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatId = chatList[position]
        holder.chatItemTextView.text = chatId
        holder.itemView.setOnClickListener {
            onChatClicked(chatId) // Вызываем слушатель нажатия на чат и передаем идентификатор чата
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    // Метод для добавления нового чата
    fun addChat(chat: String) {
        chatList.add(chat)
    }
}
