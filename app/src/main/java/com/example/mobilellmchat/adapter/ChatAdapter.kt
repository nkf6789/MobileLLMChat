package com.example.mobilellmchat.adapter

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
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
 * [MODIFIED] 支持流式消息动态更新 + 光标闪烁动画
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 2 - Stream Support with Cursor Animation
 */
class ChatAdapter(
    private val onLikeClick: (Message) -> Unit,
    private val onFavoriteClick: (Message) -> Unit,
    private var highlightMessageId: Long = -1L
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val HIGHLIGHT_COLOR = "#FFF9C4"
        private const val NORMAL_COLOR = "#F2F3F5"
        private const val HIGHLIGHT_DURATION = 1500L
        private const val CURSOR_BLINK_INTERVAL = 500L  // ✅ 光标闪烁间隔
    }

    // 流式消息状态
    private var streamingMessageId: Long? = null
    private var streamingContent: String = ""

    // ✅ 新增：光标闪烁控制
    private val handler = Handler(Looper.getMainLooper())
    private var cursorBlinkRunnable: Runnable? = null
    private var showCursor = true

    /**
     * 更新流式消息内容
     */
    fun updateStreamingMessage(messageId: Long, content: String) {
        streamingMessageId = messageId
        streamingContent = content

        // 启动光标闪烁
        startCursorBlink()

        val position = currentList.indexOfFirst { it.id == messageId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    /**
     * 清除流式状态
     */
    fun clearStreamingState() {
        streamingMessageId = null
        streamingContent = ""
        stopCursorBlink()  // ✅ 停止光标闪烁
    }

    /**
     * ✅ 新增：启动光标闪烁动画
     */
    private fun startCursorBlink() {
        // 先停止之前的闪烁
        stopCursorBlink()

        cursorBlinkRunnable = object : Runnable {
            override fun run() {
                showCursor = !showCursor

                // 只刷新正在流式输出的消息
                val position = currentList.indexOfFirst { it.id == streamingMessageId }
                if (position != -1) {
                    notifyItemChanged(position, "cursor_blink")  // 使用 payload 优化刷新
                }

                handler.postDelayed(this, CURSOR_BLINK_INTERVAL)
            }
        }
        handler.post(cursorBlinkRunnable!!)
    }

    /**
     * ✅ 新增：停止光标闪烁
     */
    private fun stopCursorBlink() {
        cursorBlinkRunnable?.let { handler.removeCallbacks(it) }
        cursorBlinkRunnable = null
        showCursor = true
    }

    fun setHighlightMessageId(messageId: Long) {
        val oldHighlightId = highlightMessageId
        highlightMessageId = messageId

        if (oldHighlightId != -1L) {
            val oldPosition = currentList.indexOfFirst { it.id == oldHighlightId }
            if (oldPosition != -1) notifyItemChanged(oldPosition)
        }
        if (messageId != -1L) {
            val newPosition = currentList.indexOfFirst { it.id == messageId }
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }

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

        // 检查是否为流式消息
        val isStreaming = message.id == streamingMessageId
        val displayContent = if (isStreaming) {
            // ✅ 根据闪烁状态显示/隐藏光标
            if (showCursor) "$streamingContent▌" else streamingContent
        } else {
            message.content
        }

        if (holder is UserMessageViewHolder) {
            holder.bind(message, shouldHighlight)
        } else if (holder is AiMessageViewHolder) {
            holder.bind(message.copy(content = displayContent), shouldHighlight, isStreaming)
        }
    }

    /**
     * ✅ 优化：支持 payload 局部刷新（只更新文本，不重新绑定按钮）
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] == "cursor_blink") {
            // 只更新文本内容，不重新绑定整个 ViewHolder
            if (holder is AiMessageViewHolder) {
                val message = getItem(position)
                val isStreaming = message.id == streamingMessageId
                val displayContent = if (isStreaming) {
                    if (showCursor) "$streamingContent▌" else streamingContent
                } else {
                    message.content
                }
                holder.updateContent(displayContent)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // 用户消息 ViewHolder
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: Message, shouldHighlight: Boolean) {
            contentTextView.text = message.content
            timeTextView.text = formatTime(message.timestamp)
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

        fun bind(message: Message, shouldHighlight: Boolean, isStreaming: Boolean = false) {
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

            // 处理高亮效果
            if (shouldHighlight) {
                applyHighlight()
            } else {
                clearHighlight()
            }
        }

        /**
         * ✅ 新增：只更新内容（用于光标闪烁优化）
         */
        fun updateContent(content: String) {
            contentTextView.text = content
        }

        private fun applyHighlight() {
            val startColor = Color.parseColor(HIGHLIGHT_COLOR)
            val endColor = Color.parseColor(NORMAL_COLOR)

            val animator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
            animator.duration = HIGHLIGHT_DURATION
            animator.addUpdateListener { animation ->
                itemView.setBackgroundColor(animation.animatedValue as Int)
            }
            animator.start()
        }

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
            return false  // 内容可能动态变化
        }
    }

    /**
     * ✅ 释放资源
     */
    fun onDetachedFromRecyclerView() {
        stopCursorBlink()
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}