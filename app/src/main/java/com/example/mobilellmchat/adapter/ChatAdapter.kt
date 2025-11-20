package com.example.mobilellmchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onLikeClick: (Message) -> Unit,
    private val onFavoriteClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == "user") VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_ai, parent, false)
            AiMessageViewHolder(view, onLikeClick, onFavoriteClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is AiMessageViewHolder) {
            holder.bind(message)
        }
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: Message) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)
        }
    }

    class AiMessageViewHolder(
        itemView: View,
        private val onLikeClick: (Message) -> Unit,
        private val onFavoriteClick: (Message) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)

        fun bind(message: Message) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)

            // 设置按钮状态图标（假设你有这些资源，如果没有请替换为标准资源）
            btnLike.isSelected = message.isLiked
            btnFavorite.isSelected = message.isFavorited

            btnLike.setOnClickListener { onLikeClick(message) }
            btnFavorite.setOnClickListener { onFavoriteClick(message) }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}

// 辅助方法：放在顶层或工具类中
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
