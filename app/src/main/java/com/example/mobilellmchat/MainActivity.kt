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
 * [MODIFIED] 支持流式消息实时显示 + 停止生成按钮
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 2 - Stream Support with Stop Button
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
    private lateinit var btnStop: ImageButton  // ✅ 新增停止按钮
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNewChat: Button
    private lateinit var btnMenu: ImageButton

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "📄 onNewIntent 被调用")
        setIntent(intent)
        handleIncomingIntent(intent)
    }

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

            if (messageId != -1L) {
                Log.d(TAG, "📌 准备高亮消息 $messageId")
                highlightAndScrollToMessage(messageId)
            }
        } else {
            Log.d(TAG, "ℹ️ 无会话ID,使用默认行为")
        }
    }

    private fun highlightAndScrollToMessage(messageId: Long) {
        viewModel.messages.observe(this) { messages ->
            if (messages.isEmpty()) {
                Log.d(TAG, "⚠️ 消息列表为空，等待加载")
                return@observe
            }

            val position = messages.indexOfFirst { it.id == messageId }

            if (position != -1) {
                Log.d(TAG, "✅ 找到消息位置: $position")

                handler.postDelayed({
                    val layoutManager = rvMessages.layoutManager as? LinearLayoutManager
                    layoutManager?.scrollToPositionWithOffset(position, 100)

                    chatAdapter.setHighlightMessageId(messageId)

                    clearHighlightRunnable?.let { handler.removeCallbacks(it) }
                    clearHighlightRunnable = Runnable {
                        chatAdapter.clearHighlight()
                        Log.d(TAG, "🎨 高亮已清除")
                    }
                    handler.postDelayed(clearHighlightRunnable!!, 3000L)

                    Log.d(TAG, "🎨 消息已高亮并滚动到可见位置")
                }, 300L)

            } else {
                Log.d(TAG, "⚠️ 未找到消息ID $messageId 在列表中")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(configChangeReceiver)
        clearHighlightRunnable?.let { handler.removeCallbacks(it) }
        chatAdapter.onDetachedFromRecyclerView()  // ✅ 释放光标动画资源
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
        btnStop = findViewById(R.id.btnStop)  // ✅ 初始化停止按钮
        progressBar = findViewById(R.id.progressBar)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnMenu = findViewById(R.id.btnMenu)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnFavorites: Button = findViewById(R.id.btnFavorites)

        // 发送按钮
        btnSend.setOnClickListener {
            val content = etMessage.text.toString()
            if (content.isNotBlank()) {
                viewModel.sendMessageStream(content)
                etMessage.text.clear()
            }
        }

        // ✅ 新增：停止按钮
        btnStop.setOnClickListener {
            viewModel.stopGeneration()
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
        chatAdapter = ChatAdapter(
            onLikeClick = { msg -> viewModel.toggleLike(msg) },
            onFavoriteClick = { msg -> viewModel.toggleFavorite(msg) },
            highlightMessageId = -1L
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
        // 消息列表
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty() && intent.getLongExtra("message_id", -1L) == -1L) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        // 监听流式消息更新
        viewModel.streamingContent.observe(this) { content ->
            val messageId = viewModel.streamingMessageId.value
            if (messageId != null && content.isNotEmpty()) {
                chatAdapter.updateStreamingMessage(messageId, content)

                // 自动滚动到底部
                rvMessages.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        // ✅ 新增：根据流式状态切换按钮显示
        viewModel.streamingMessageId.observe(this) { messageId ->
            if (messageId == null) {
                // 流式结束，显示发送按钮，隐藏停止按钮
                btnSend.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                chatAdapter.clearStreamingState()
            } else {
                // 流式进行中，隐藏发送按钮，显示停止按钮
                btnSend.visibility = View.GONE
                btnStop.visibility = View.VISIBLE
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