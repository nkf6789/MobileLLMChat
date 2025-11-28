package com.example.mobilellmchat

import android.content.Intent
import android.os.Bundle
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

/**
 * MainActivity - 主界面
 *
 * 功能：
 * - 聊天消息显示和发送
 * - 会话管理（左侧抽屉）
 * - 设置和下载管理入口
 *
 * @author AI-Assisted
 * @since Sprint 2
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)  // ✅ 修复
        val repository = ChatRepository(database)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()
        setupAdapters()
        setupBackPressHandler()  // ✅ 新增：设置返回键处理
        observeViewModel()

        viewModel.conversations.observe(this) { list ->
            if (list.isEmpty()) {
                viewModel.createNewConversation()
            } else if (viewModel.currentConversationId.value == null) {
                viewModel.switchConversation(list[0].id)
            }
        }
    }

    private fun initViews() {
        // 如果你的布局中有 Toolbar，取消注释下面这行
        // toolbar = findViewById(R.id.toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        rvMessages = findViewById(R.id.rvMessages)
        rvConversations = findViewById(R.id.rvConversations)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnMenu = findViewById(R.id.btnMenu)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)  // ← 新增这行


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

        // ✅ 新增：设置按钮点击事件
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
            onFavoriteClick = { msg -> viewModel.toggleFavorite(msg) }
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

    /**
     * ✅ 设置返回键处理（替换已弃用的 onBackPressed）
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // 调用默认行为（退出 Activity）
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
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

    // ❌ 移除已弃用的方法
    // override fun onBackPressed() { ... }
}
