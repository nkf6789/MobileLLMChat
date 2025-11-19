package com.example.mobilellmchat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.ChatAdapter
import com.example.mobilellmchat.adapter.ConversationAdapter
import com.example.mobilellmchat.viewmodel.ChatViewModel
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var conversationAdapter: ConversationAdapter

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var rvConversations: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnNewConversation: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        rvMessages = findViewById(R.id.rvMessages)
        rvConversations = findViewById(R.id.rvConversations)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnNewConversation = findViewById(R.id.btnNewConversation)
        progressBar = findViewById(R.id.progressBar)

        // 设置工具栏
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // 设置消息列表
        chatAdapter = ChatAdapter()
        rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
        }

        // 设置会话列表
        conversationAdapter = ConversationAdapter { conversation ->
            viewModel.switchConversation(conversation.id)
            drawerLayout.close()
        }
        rvConversations.apply {
            adapter = conversationAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // 观察会话列表
        viewModel.conversations.observe(this) { conversations ->
            conversationAdapter.submitList(conversations)
        }

        // 观察消息列表
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    rvMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSend.isEnabled = !isLoading
        }

        // 新建会话按钮
        btnNewConversation.setOnClickListener {
            viewModel.createNewConversation()
            drawerLayout.close()
        }

        // 发送按钮
        btnSend.setOnClickListener {
            val message = etMessage.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                etMessage.text.clear()
            }
        }
    }
}
