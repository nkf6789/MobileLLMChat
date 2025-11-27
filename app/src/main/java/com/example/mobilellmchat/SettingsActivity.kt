package com.example.mobilellmchat

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * SettingsActivity - 模型设置页面
 *
 * [MODIFIED] 使用属性访问代替方法调用
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var rgModelType: RadioGroup
    private lateinit var rbRemote: RadioButton
    private lateinit var rbLocal: RadioButton
    private lateinit var tvCurrentModel: TextView
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

    private val availableModels = listOf(
        "qwen2-0.5b-instruct-q4_0.gguf",
        "qwen2-1.5b-instruct-q4_0.gguf",
        "phi-2-q4_0.gguf",
        "tinyllama-1.1b-chat-q4_0.gguf"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initTools()
        initViews()
        loadCurrentSettings()
        setupListeners()
    }

    private fun initTools() {
        preferences = AppPreferences(this)
        fileManager = ModelFileManager(this)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rgModelType = findViewById(R.id.rgModelType)
        rbRemote = findViewById(R.id.rbRemote)
        rbLocal = findViewById(R.id.rbLocal)
        tvCurrentModel = findViewById(R.id.tvCurrentModel)
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
            // ✅ 使用属性访问
            val modelType = preferences.modelType
            val localModelPath = preferences.localModelPath
            val backend = preferences.computeBackend
            val threads = preferences.cpuThreads
            val temperature = preferences.temperature

            when (modelType) {
                ModelType.REMOTE -> {
                    rbRemote.isChecked = true
                    tvCurrentModel.text = "当前: 云端模型 (豆包)"
                    cardLocalConfig.visibility = View.GONE
                }
                ModelType.LOCAL -> {
                    rbLocal.isChecked = true
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
            tvTemperatureValue.text = String.format(Locale.US, "%.1f", temperature)

            if (localModelPath.isNotEmpty()) {
                val modelName = localModelPath.substringAfterLast("/")
                val index = availableModels.indexOf(modelName)
                if (index >= 0) {
                    spinnerLocalModel.setSelection(index)
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        rgModelType.setOnCheckedChangeListener { _, checkedId ->
            cardLocalConfig.visibility = if (checkedId == R.id.rbLocal) View.VISIBLE else View.GONE
        }

        seekBarThreads.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvThreadsValue.text = (progress + 1).toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = progress / 100.0f
                tvTemperatureValue.text = String.format(Locale.US, "%.1f", temp)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDownloadModel.setOnClickListener {
            Toast.makeText(this, "模型下载功能将在下一版本实现", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { saveSettings() }
    }

    private fun saveSettings() {
        lifecycleScope.launch {
            try {
                // ✅ 使用属性赋值
                val modelType = if (rbRemote.isChecked) ModelType.REMOTE else ModelType.LOCAL

                if (modelType == ModelType.LOCAL) {
                    val selectedModel = spinnerLocalModel.selectedItem.toString()

                    if (!fileManager.isModelDownloaded(selectedModel)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@SettingsActivity,
                                "⚠️ 模型 $selectedModel 尚未下载",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    preferences.localModelPath = fileManager.getModelPath(selectedModel)
                }

                preferences.modelType = modelType
                preferences.computeBackend = if (switchGPU.isChecked) {
                    ComputeBackend.VULKAN
                } else {
                    ComputeBackend.CPU
                }
                preferences.cpuThreads = seekBarThreads.progress + 1
                preferences.temperature = seekBarTemperature.progress / 100.0f

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "✅ 设置已保存！",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "❌ 保存失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun checkGpuAvailability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val hasVulkan = packageManager.hasSystemFeature("android.hardware.vulkan.version")
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val configInfo = activityManager.deviceConfigurationInfo
            val supportsEs3 = configInfo.reqGlEsVersion >= 0x30000
            hasVulkan && supportsEs3
        } catch (e: Exception) {
            false
        }
    }

    private fun updateGpuStatus(available: Boolean) {
        if (available) {
            tvGpuStatus.text = "✅ Vulkan 支持"
            tvGpuStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvGpuStatus.text = "⚠️ GPU 不可用"
            tvGpuStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            switchGPU.isChecked = false
        }
    }

    private fun updateLocalModelStatus(modelPath: String) {
        val modelName = if (modelPath.isEmpty()) {
            "未选择模型"
        } else {
            modelPath.substringAfterLast("/")
        }

        val isDownloaded = fileManager.isModelDownloaded(modelName)
        val status = if (isDownloaded) "✅ 已下载" else "⚠️ 未下载"

        tvCurrentModel.text = "当前: 本地模型 ($modelName) - $status"
    }
}
