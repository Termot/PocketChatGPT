package com.slowlii.pocketchatgpt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(private var chatList: List<Chat>) :
    RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var chatId: String = ""

    interface OnItemClickListener {
        fun onItemClick(chatId: String)
        fun onDeleteClick(chatId: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_chat,
            parent,
            false
        )
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    fun setChatId(chatId: String) {
        this.chatId = chatId
    }

    fun updateChatList(chatList: List<Chat>) {
        this.chatList = chatList
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatIdTextView: TextView = itemView.findViewById(R.id.chatIdTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chatId = chatList[position].chatId
                    onItemClickListener?.onItemClick(chatId)
                }
            }

            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chatId = chatList[position].chatId
                    onItemClickListener?.onDeleteClick(chatId)
                }
            }
        }

        fun bind(chat: Chat) {
            chatIdTextView.text = chat.chatId
        }
    }
}
