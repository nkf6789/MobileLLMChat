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
import com.example.mobilellmchat.model.FavoriteMessageWithConversation
import java.text.SimpleDateFormat
import java.util.*

/**
 * 收藏消息适配器
 */
class FavoritesAdapter(
    private val onUnfavoriteClick: (FavoriteMessageWithConversation) -> Unit,
    private val onCopyClick: (FavoriteMessageWithConversation) -> Unit,
    private val onJumpClick: (FavoriteMessageWithConversation) -> Unit
) : ListAdapter<FavoriteMessageWithConversation, FavoritesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_message, parent, false)
        return ViewHolder(view, onUnfavoriteClick, onCopyClick, onJumpClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onUnfavoriteClick: (FavoriteMessageWithConversation) -> Unit,
        private val onCopyClick: (FavoriteMessageWithConversation) -> Unit,
        private val onJumpClick: (FavoriteMessageWithConversation) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvConversation: TextView = itemView.findViewById(R.id.tvConversation)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val btnUnfavorite: ImageButton = itemView.findViewById(R.id.btnUnfavorite)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)
        private val btnJump: ImageButton = itemView.findViewById(R.id.btnJump)

        fun bind(message: FavoriteMessageWithConversation) {
            tvContent.text = message.messageContent
            tvConversation.text = "来自：${message.conversationTitle}"
            tvTime.text = formatTime(message.messageTimestamp)

            btnUnfavorite.setOnClickListener { onUnfavoriteClick(message) }
            btnCopy.setOnClickListener { onCopyClick(message) }
            btnJump.setOnClickListener { onJumpClick(message) }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FavoriteMessageWithConversation>() {
        override fun areItemsTheSame(
            oldItem: FavoriteMessageWithConversation,
            newItem: FavoriteMessageWithConversation
        ): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(
            oldItem: FavoriteMessageWithConversation,
            newItem: FavoriteMessageWithConversation
        ): Boolean {
            return oldItem == newItem
        }
    }
}