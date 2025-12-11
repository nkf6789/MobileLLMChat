package com.example.mobilellmchat.adapter

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
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

/**
 * ChatAdapter - 聊天消息适配器
 *
 * [MODIFIED] 支持消息高亮功能
 *
 * @param highlightMessageId 需要高亮的消息ID，-1 表示无高亮
 */
class ChatAdapter(
    private val onLikeClick: (Message) -> Unit,
    private val onFavoriteClick: (Message) -> Unit,
    private var highlightMessageId: Long = -1L  // ✅ 新增：高亮消息ID
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2

        // ✅ 高亮颜色配置
        private const val HIGHLIGHT_COLOR = "#FFF9C4"  // 浅黄色高亮
        private const val NORMAL_COLOR = "#F2F3F5"     // 正常背景色
        private const val HIGHLIGHT_DURATION = 1500L   // 高亮持续时间(ms)
    }

    /**
     * ✅ 新增：更新高亮消息ID
     */
    fun setHighlightMessageId(messageId: Long) {
        val oldHighlightId = highlightMessageId
        highlightMessageId = messageId

        // 刷新受影响的项
        if (oldHighlightId != -1L) {
            val oldPosition = currentList.indexOfFirst { it.id == oldHighlightId }
            if (oldPosition != -1) notifyItemChanged(oldPosition)
        }
        if (messageId != -1L) {
            val newPosition = currentList.indexOfFirst { it.id == messageId }
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }

    /**
     * ✅ 新增：清除高亮
     */
    fun clearHighlight() {
        setHighlightMessageId(-1L)
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
        val shouldHighlight = message.id == highlightMessageId

        if (holder is UserMessageViewHolder) {
            holder.bind(message, shouldHighlight)
        } else if (holder is AiMessageViewHolder) {
            holder.bind(message, shouldHighlight)
        }
    }

    // 用户消息 ViewHolder
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: Message, shouldHighlight: Boolean) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)

            // ✅ 用户消息暂不高亮（因为收藏的是 AI 消息）
        }
    }

    // AI 消息 ViewHolder
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

        fun bind(message: Message, shouldHighlight: Boolean) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)

            // 设置视觉状态
            btnLike.isSelected = message.isLiked
            btnFavorite.isSelected = message.isFavorited

            // 设置点击监听
            btnLike.setOnClickListener { onLikeClick(message) }
            btnFavorite.setOnClickListener { onFavoriteClick(message) }
            btnCopy.setOnClickListener {
                copyToClipboard(itemView.context, message.content)
            }

            // ✅ 处理高亮效果 - 应用到整个消息容器
            if (shouldHighlight) {
                applyHighlight()
            } else {
                clearHighlight()
            }
        }

        /**
         * ✅ 应用高亮效果（从高亮色渐变回正常色）
         * [FIXED] 将高亮应用到整个消息容器而不是单个TextView
         */
        private fun applyHighlight() {
            val startColor = Color.parseColor(HIGHLIGHT_COLOR)
            val endColor = Color.parseColor(NORMAL_COLOR)

            val animator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
            animator.duration = HIGHLIGHT_DURATION
            animator.addUpdateListener { animation ->
                // ✅ 修复：应用到整个消息容器（itemView），而不只是TextView
                itemView.setBackgroundColor(animation.animatedValue as Int)
            }
            animator.start()
        }

        /**
         * ✅ 清除高亮效果
         * [FIXED] 恢复为透明背景而不是灰色，避免覆盖布局原本的背景
         */
        private fun clearHighlight() {
            itemView.setBackgroundColor(Color.TRANSPARENT)
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
            return oldItem == newItem
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}