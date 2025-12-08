package com.example.mobilellmchat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.ModelType
import com.example.mobilellmchat.utils.AppPreferences
import com.example.mobilellmchat.utils.ModelFileManager
import com.example.mobilellmchat.utils.ModelAssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var rgModelType: RadioGroup
    private lateinit var rbRemote: RadioButton
    private lateinit var rbLocal: RadioButton
    private lateinit var tvCurrentModel: TextView

    // ✅ 新增：远程配置相关
    private lateinit var cardRemoteConfig: CardView
    private lateinit var etApiKey: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var etModelName: EditText

    private lateinit var cardLocalConfig: CardView
    private lateinit var spinnerLocalModel: Spinner
    private lateinit var btnDownloadModel: Button
    private lateinit var switchGPU: SwitchCompat
    private lateinit var tvGpuStatus: TextView
    private lateinit var seekBarThreads: SeekBar
    private lateinit var tvThreadsValue: TextView
    private lateinit var seekBarTemperature: SeekBar
    private lateinit var tvTemperatureValue: TextView
    private lateinit var btnSave: Button

    private lateinit var preferences: AppPreferences
    private lateinit var fileManager: ModelFileManager

    // ✅ 包含预置模型 + 可下载模型
    private val availableModels = listOf(
        "tinyllama-1.1b-chat-q4_0.gguf",     // ✅ 预置模型（打包在APK中）
        "qwen2-0.5b-instruct-q4_0.gguf"      // ✅ 可下载模型
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initTools()
        initViews()
        loadCurrentSettings()
        setupListeners()

        // ✅ 首次运行时确保预置模型已复制
        lifecycleScope.launch {
            if (!ModelAssetManager.isPreinstalledModelReady(this@SettingsActivity)) {
                Toast.makeText(
                    this@SettingsActivity,
                    "正在准备本地模型...",
                    Toast.LENGTH_SHORT
                ).show()

                ModelAssetManager.ensurePreinstalledModel(this@SettingsActivity)
                loadCurrentSettings()  // 重新加载设置
            }
        }
    }

    private fun initTools() {
        preferences = AppPreferences(this)
        fileManager = ModelFileManager.getInstance(this)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rgModelType = findViewById(R.id.rgModelType)
        rbRemote = findViewById(R.id.rbRemote)
        rbLocal = findViewById(R.id.rbLocal)
        tvCurrentModel = findViewById(R.id.tvCurrentModel)

        // ✅ 新增：远程配置视图
        cardRemoteConfig = findViewById(R.id.cardRemoteConfig)
        etApiKey = findViewById(R.id.etApiKey)
        etBaseUrl = findViewById(R.id.etBaseUrl)
        etModelName = findViewById(R.id.etModelName)

        cardLocalConfig = findViewById(R.id.cardLocalConfig)
        spinnerLocalModel = findViewById(R.id.spinnerLocalModel)
        btnDownloadModel = findViewById(R.id.btnDownloadModel)
        switchGPU = findViewById(R.id.switchGPU)
        tvGpuStatus = findViewById(R.id.tvGpuStatus)
        seekBarThreads = findViewById(R.id.seekBarThreads)
        tvThreadsValue = findViewById(R.id.tvThreadsValue)
        seekBarTemperature = findViewById(R.id.seekBarTemperature)
        tvTemperatureValue = findViewById(R.id.tvTemperatureValue)
        btnSave = findViewById(R.id.btnSave)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableModels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocalModel.adapter = adapter
    }

    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            val modelType = preferences.modelType
            val localModelPath = preferences.localModelPath
            val backend = preferences.computeBackend
            val threads = preferences.cpuThreads
            val temperature = preferences.temperature

            // ✅ 加载远程配置
            etApiKey.setText(preferences.apiKey)
            etBaseUrl.setText(preferences.baseUrl)
            etModelName.setText(preferences.modelName)

            when (modelType) {
                ModelType.REMOTE -> {
                    rbRemote.isChecked = true
                    tvCurrentModel.text = "当前: 云端模型 (${preferences.modelName})"
                    cardRemoteConfig.visibility = View.VISIBLE
                    cardLocalConfig.visibility = View.GONE
                }
                ModelType.LOCAL -> {
                    rbLocal.isChecked = true
                    cardRemoteConfig.visibility = View.GONE
                    cardLocalConfig.visibility = View.VISIBLE
                    updateLocalModelStatus(localModelPath)
                }
            }

            val gpuAvailable = checkGpuAvailability()
            updateGpuStatus(gpuAvailable)
            switchGPU.isChecked = (backend == ComputeBackend.VULKAN && gpuAvailable)
            switchGPU.isEnabled = gpuAvailable

            seekBarThreads.progress = threads - 1
            tvThreadsValue.text = threads.toString()

            val tempProgress = (temperature * 100).toInt()
            seekBarTemperature.progress = tempProgress
            tvTemperatureValue.text = String.format(Locale.getDefault(), "%.2f", temperature)

            if (localModelPath.isNotEmpty()) {
                val modelName = localModelPath.substringAfterLast("/")
                val position = availableModels.indexOf(modelName)
                if (position >= 0) {
                    spinnerLocalModel.setSelection(position)
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // ✅ 模型类型切换监听
        rgModelType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRemote -> {
                    cardRemoteConfig.visibility = View.VISIBLE
                    cardLocalConfig.visibility = View.GONE
                }
                R.id.rbLocal -> {
                    cardRemoteConfig.visibility = View.GONE
                    cardLocalConfig.visibility = View.VISIBLE
                }
            }
        }

        btnDownloadModel.setOnClickListener {
            // ✅ 启用下载功能
            startActivity(Intent(this, DownloadActivity::class.java))
        }

        switchGPU.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "已启用 GPU 加速", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已切换到 CPU 模式", Toast.LENGTH_SHORT).show()
            }
        }

        seekBarThreads.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threads = progress + 1
                tvThreadsValue.text = threads.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = progress / 100.0f
                tvTemperatureValue.text = String.format(Locale.getDefault(), "%.2f", temp)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val selectedModelType = if (rbRemote.isChecked) ModelType.REMOTE else ModelType.LOCAL

        when (selectedModelType) {
            ModelType.REMOTE -> {
                // ✅ 保存远程配置
                val apiKey = etApiKey.text.toString().trim()
                val baseUrl = etBaseUrl.text.toString().trim()
                val modelName = etModelName.text.toString().trim()

                if (apiKey.isEmpty()) {
                    Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show()
                    return
                }
                if (baseUrl.isEmpty()) {
                    Toast.makeText(this, "请输入 Base URL", Toast.LENGTH_SHORT).show()
                    return
                }
                if (modelName.isEmpty()) {
                    Toast.makeText(this, "请输入模型名称", Toast.LENGTH_SHORT).show()
                    return
                }

                preferences.modelType = ModelType.REMOTE
                preferences.apiKey = apiKey
                preferences.baseUrl = baseUrl  // 会自动补 /
                preferences.modelName = modelName

                Toast.makeText(this, "远程模型配置已保存", Toast.LENGTH_SHORT).show()
                finish()
            }

            ModelType.LOCAL -> {
                val selectedModel = spinnerLocalModel.selectedItem.toString()

                if (!fileManager.isModelDownloaded(selectedModel)) {
                    Toast.makeText(
                        this,
                        "模型 $selectedModel 未下载，请先下载",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                preferences.modelType = ModelType.LOCAL
                preferences.localModelPath = fileManager.getModelPath(selectedModel)
                preferences.computeBackend = if (switchGPU.isChecked) {
                    ComputeBackend.VULKAN
                } else {
                    ComputeBackend.CPU
                }
                preferences.cpuThreads = seekBarThreads.progress + 1
                preferences.temperature = seekBarTemperature.progress / 100.0f

                Toast.makeText(this, "本地模型配置已保存", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private suspend fun checkGpuAvailability(): Boolean = withContext(Dispatchers.Default) {
        try {
            // 这里可以添加实际的 GPU 检测逻辑
            // 目前返回 true 表示支持
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun updateGpuStatus(available: Boolean) {
        if (available) {
            tvGpuStatus.text = "✅ Vulkan 支持"
            tvGpuStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvGpuStatus.text = "❌ 不支持 GPU"
            tvGpuStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun updateLocalModelStatus(modelPath: String) {
        if (modelPath.isEmpty()) {
            tvCurrentModel.text = "当前: 未选择本地模型"
            return
        }

        val modelName = modelPath.substringAfterLast("/")
        val isDownloaded = fileManager.isModelDownloaded(modelName)

        tvCurrentModel.text = if (isDownloaded) {
            "当前: $modelName ✅"
        } else {
            "当前: $modelName ❌ (未下载)"
        }
    }
}