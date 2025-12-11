package com.example.mobilellmchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.FavoritesAdapter
import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.repository.ChatRepository
import kotlinx.coroutines.launch

/**
 * 收藏页面
 * 显示所有收藏的 AI 回复消息
 *
 * [MODIFIED] 跳转时传递消息ID，支持高亮显示
 */
class FavoritesActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: LinearLayout  // ✅ 修复：改为 LinearLayout
    private lateinit var adapter: FavoritesAdapter
    private lateinit var repository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        initRepository()
        initViews()
        observeData()
    }

    private fun initRepository() {
        val database = AppDatabase.getDatabase(applicationContext)
        repository = ChatRepository(database)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.rvFavorites)
        tvEmpty = findViewById(R.id.tvEmpty)  // ✅ 类型已修正

        // 设置 Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "我的收藏"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // 设置 RecyclerView
        adapter = FavoritesAdapter(
            onUnfavoriteClick = { message ->
                unfavoriteMessage(message.messageId)
            },
            onCopyClick = { message ->
                copyToClipboard(message.messageContent)
            },
            onJumpClick = { message ->
                // ✅ 修改：同时传递会话ID和消息ID
                jumpToConversation(message.conversationId, message.messageId)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getAllFavoritedMessagesWithConversation().collect { favorites ->
                if (favorites.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.submitList(favorites)
                }
            }
        }
    }

    /**
     * 取消收藏
     */
    private fun unfavoriteMessage(messageId: Long) {
        lifecycleScope.launch {
            repository.toggleFavorite(messageId, true)  // currentStatus=true 表示当前已收藏
        }
    }

    /**
     * 复制内容
     */
    private fun copyToClipboard(content: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Favorite Message", content)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(this, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * ✅ 修改：跳转到原会话并高亮显示消息
     * @param conversationId 会话ID
     * @param messageId 消息ID（用于高亮）
     */
    private fun jumpToConversation(conversationId: Long, messageId: Long) {
        Log.d(TAG, "准备跳转到会话ID: $conversationId, 消息ID: $messageId")

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("conversation_id", conversationId)
            putExtra("message_id", messageId)  // ✅ 新增：传递消息ID
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "FavoritesActivity"
    }
}