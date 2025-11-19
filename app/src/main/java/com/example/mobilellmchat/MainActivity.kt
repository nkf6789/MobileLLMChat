package com.example.mobilellmchat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.ChatAdapter
import com.example.mobilellmchat.viewmodel.ChatViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)

        // 设置 RecyclerView
        chatAdapter = ChatAdapter()
        rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true  // 从底部开始显示
            }
        }

        // 观察消息列表
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                // 列表更新后滚动到底部
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

        // 发送按钮点击事件
        btnSend.setOnClickListener {
            val message = etMessage.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                etMessage.text.clear()
            }
        }
    }
}
