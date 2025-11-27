package com.example.mobilellmchat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
 * [MODIFIED] 添加设置页面入口
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 2
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = ChatRepository(database)
        ChatViewModelFactory(application, repository)
    }

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
    private lateinit var btnSettings: ImageButton  // ✅ 新增

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupAdapters()
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
        drawerLayout = findViewById(R.id.drawerLayout)
        rvMessages = findViewById(R.id.rvMessages)
        rvConversations = findViewById(R.id.rvConversations)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnMenu = findViewById(R.id.btnMenu)
        btnSettings = findViewById(R.id.btnSettings)  // ✅ 新增

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

        // ✅ 新增：设置按钮
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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
}
