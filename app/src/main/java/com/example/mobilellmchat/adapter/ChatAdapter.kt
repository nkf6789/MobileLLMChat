package com.example.mobilellmchat.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
        // 假设 Role "user" 是用户，其他都是 AI
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

    // 用户消息 ViewHolder (保持简单，也可以加复制功能)
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: Message) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)
        }
    }

    // AI 消息 ViewHolder (包含互动逻辑)
    class AiMessageViewHolder(
        itemView: View,
        private val onLikeClick: (Message) -> Unit,
        private val onFavoriteClick: (Message) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)

        fun bind(message: Message) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)

            // 1. 设置视觉状态：isSelected 会触发 selector xml 切换图标颜色
            btnLike.isSelected = message.isLiked
            btnFavorite.isSelected = message.isFavorited

            // 2. 设置点击监听 (调用 Activity/ViewModel 的逻辑)
            btnLike.setOnClickListener { onLikeClick(message) }
            btnFavorite.setOnClickListener { onFavoriteClick(message) }

            // 3. 实现复制功能 (直接在这里实现，最简单有效)
            btnCopy.setOnClickListener {
                copyToClipboard(itemView.context, message.content)
            }
        }

        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chat Content", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            // 必须完全比较内容，包括点赞状态，才能触发 UI 刷新
            return oldItem == newItem
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
