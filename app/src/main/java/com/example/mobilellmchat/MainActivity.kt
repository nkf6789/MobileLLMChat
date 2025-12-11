package com.example.mobilellmchat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.ChatAdapter
import com.example.mobilellmchat.adapter.ConversationAdapter
import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.repository.ChatRepository
import com.example.mobilellmchat.viewmodel.ChatViewModel
import com.example.mobilellmchat.viewmodel.ChatViewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * MainActivity - 主界面
 *
 * 功能：
 * - 聊天消息显示和发送
 * - 会话管理（左侧抽屉）
 * - 设置和下载管理入口
 * - [MODIFIED] 支持从收藏页面跳转到指定会话并高亮消息
 *
 * @author AI-Assisted
 * @since Sprint 2
 */
class MainActivity : AppCompatActivity() {

    private val repository: ChatRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        ChatRepository(database)
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(application, repository)
    }

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var rvConversations: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNewChat: Button
    private lateinit var btnMenu: ImageButton

    // ✅ 延迟执行器，用于高亮后的清除
    private val handler = Handler(Looper.getMainLooper())
    private var clearHighlightRunnable: Runnable? = null

    private val configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "📢 收到配置更改广播")
            if (intent?.action == "com.example.mobilellmchat.CONFIG_CHANGED") {
                repository.reinitialize(applicationContext)
                Toast.makeText(
                    this@MainActivity,
                    "✅ 配置已更新并生效",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "📱 onCreate 执行")
        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()
        setupAdapters()
        setupBackPressHandler()
        observeViewModel()
        registerConfigChangeReceiver()

        handleIncomingIntent(intent)

        viewModel.conversations.observe(this) { list ->
            if (list.isEmpty()) {
                viewModel.createNewConversation()
            } else if (viewModel.currentConversationId.value == null) {
                viewModel.switchConversation(list[0].id)
            }
        }
    }

    /**
     * ✅ 处理 SINGLE_TOP 启动模式的 Intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "🔄 onNewIntent 被调用")
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * ✅ 统一处理传入的 Intent，包含高亮逻辑
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(TAG, "⚠️ Intent 为 null")
            return
        }

        val conversationId = intent.getLongExtra("conversation_id", -1L)
        val messageId = intent.getLongExtra("message_id", -1L)

        Log.d(TAG, "📥 接收到的会话ID: $conversationId")
        Log.d(TAG, "📥 接收到的消息ID: $messageId")

        if (conversationId != -1L) {
            Log.d(TAG, "✅ 准备切换到会话 $conversationId")
            viewModel.switchConversation(conversationId)

            // ✅ 如果有消息ID，等待消息加载后高亮并滚动
            if (messageId != -1L) {
                Log.d(TAG, "📌 准备高亮消息 $messageId")
                highlightAndScrollToMessage(messageId)
            }
        } else {
            Log.d(TAG, "ℹ️ 无会话ID，使用默认行为")
        }
    }

    /**
     * ✅ 高亮并滚动到指定消息
     */
    private fun highlightAndScrollToMessage(messageId: Long) {
        // 等待消息列表加载完成（使用 observe 确保数据已更新）
        viewModel.messages.observe(this) { messages ->
            if (messages.isEmpty()) {
                Log.d(TAG, "⚠️ 消息列表为空，等待加载")
                return@observe
            }

            // 查找消息在列表中的位置
            val position = messages.indexOfFirst { it.id == messageId }

            if (position != -1) {
                Log.d(TAG, "✅ 找到消息位置: $position")

                // 延迟执行，确保 RecyclerView 已经渲染完成
                handler.postDelayed({
                    // 1. 滚动到目标位置（居中显示）
                    val layoutManager = rvMessages.layoutManager as? LinearLayoutManager
                    layoutManager?.scrollToPositionWithOffset(position, 100)

                    // 2. 设置高亮
                    chatAdapter.setHighlightMessageId(messageId)

                    // 3. 3秒后自动清除高亮
                    clearHighlightRunnable?.let { handler.removeCallbacks(it) }
                    clearHighlightRunnable = Runnable {
                        chatAdapter.clearHighlight()
                        Log.d(TAG, "🎨 高亮已清除")
                    }
                    handler.postDelayed(clearHighlightRunnable!!, 3000L)

                    Log.d(TAG, "🎨 消息已高亮并滚动到可见位置")
                }, 300L)  // 延迟 300ms 确保渲染完成

            } else {
                Log.d(TAG, "⚠️ 未找到消息ID $messageId 在列表中")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(configChangeReceiver)

        // ✅ 清理 Handler 回调
        clearHighlightRunnable?.let { handler.removeCallbacks(it) }

        Log.d(TAG, "🗑️ 本地广播接收器已注销")
    }

    private fun registerConfigChangeReceiver() {
        Log.d(TAG, "🔧 开始注册本地广播接收器")
        val filter = IntentFilter("com.example.mobilellmchat.CONFIG_CHANGED")
        LocalBroadcastManager.getInstance(this).registerReceiver(configChangeReceiver, filter)
        Log.d(TAG, "✅ 本地广播接收器注册成功")
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        rvMessages = findViewById(R.id.rvMessages)
        rvConversations = findViewById(R.id.rvConversations)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnMenu = findViewById(R.id.btnMenu)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnFavorites: Button = findViewById(R.id.btnFavorites)

        btnSend.setOnClickListener {
            val content = etMessage.text.toString()
            if (content.isNotBlank()) {
                viewModel.sendMessage(content)
                etMessage.text.clear()
            }
        }

        btnNewChat.setOnClickListener {
            viewModel.createNewConversation()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupToolbar() {
        // 如果你的 activity_main.xml 中有 Toolbar，取消注释以下代码
        // setSupportActionBar(toolbar)
        // supportActionBar?.setDisplayShowTitleEnabled(true)
        // supportActionBar?.title = "Mobile LLM Chat"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_download -> {
                startActivity(Intent(this, DownloadActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupAdapters() {
        // ✅ 创建支持高亮的 ChatAdapter
        chatAdapter = ChatAdapter(
            onLikeClick = { msg -> viewModel.toggleLike(msg) },
            onFavoriteClick = { msg -> viewModel.toggleFavorite(msg) },
            highlightMessageId = -1L  // 初始无高亮
        )
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = chatAdapter

        conversationAdapter = ConversationAdapter { conversation ->
            viewModel.switchConversation(conversation.id)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = conversationAdapter
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                // 只在非高亮跳转时自动滚动到底部
                if (messages.isNotEmpty() && intent.getLongExtra("message_id", -1L) == -1L) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.conversations.observe(this) { conversations ->
            conversationAdapter.submitList(conversations)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}