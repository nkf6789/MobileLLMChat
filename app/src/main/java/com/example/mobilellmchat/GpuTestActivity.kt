// GpuTestActivity.kt
package com.example.mobilellmchat

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mobilellmchat.llm.LlamaWrapper
import com.example.mobilellmchat.utils.ModelFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * GPU加速测试工具
 * 对比CPU和GPU模式的推理速度
 */
class GpuTestActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var btnTestCpu: Button
    private lateinit var btnTestGpu: Button
    private lateinit var btnCompare: Button

    private var cpuTime: Long = 0
    private var gpuTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gpu_test)

        tvResult = findViewById(R.id.tvResult)
        btnTestCpu = findViewById(R.id.btnTestCpu)
        btnTestGpu = findViewById(R.id.btnTestGpu)
        btnCompare = findViewById(R.id.btnCompare)

        btnTestCpu.setOnClickListener { testCpuMode() }
        btnTestGpu.setOnClickListener { testGpuMode() }
        btnCompare.setOnClickListener { comparePerformance() }
    }

    private fun testCpuMode() {
        lifecycleScope.launch {
            tvResult.text = "🔄 测试 CPU 模式...\n"

            val result = withContext(Dispatchers.IO) {
                runInferenceTest(useGpu = false)
            }

            cpuTime = result.first
            val output = result.second

            tvResult.append("✅ CPU 模式完成\n")
            tvResult.append("⏱️ 耗时: ${cpuTime}ms\n")
            tvResult.append("📝 输出: ${output.take(50)}...\n\n")
        }
    }

    private fun testGpuMode() {
        lifecycleScope.launch {
            tvResult.append("🔄 测试 GPU 模式...\n")

            val result = withContext(Dispatchers.IO) {
                runInferenceTest(useGpu = true)
            }

            gpuTime = result.first
            val output = result.second

            tvResult.append("✅ GPU 模式完成\n")
            tvResult.append("⏱️ 耗时: ${gpuTime}ms\n")
            tvResult.append("📝 输出: ${output.take(50)}...\n\n")
        }
    }

    private fun comparePerformance() {
        if (cpuTime == 0L || gpuTime == 0L) {
            tvResult.text = "⚠️ 请先运行 CPU 和 GPU 测试\n"
            return
        }

        val speedup = cpuTime.toFloat() / gpuTime.toFloat()
        val faster = if (speedup > 1) "GPU 更快" else "CPU 更快"
        val diff = kotlin.math.abs(speedup - 1) * 100

        tvResult.append("━━━━━━━━━━━━━━━━━━━━━\n")
        tvResult.append("📊 性能对比:\n")
        tvResult.append("CPU: ${cpuTime}ms\n")
        tvResult.append("GPU: ${gpuTime}ms\n")
        tvResult.append("结论: $faster ${String.format("%.1f", diff)}%\n")
        tvResult.append("加速比: ${String.format("%.2f", speedup)}x\n")

        if (speedup < 1.1) {
            tvResult.append("\n⚠️ GPU加速不明显,建议:\n")
            tvResult.append("1. 检查设备是否支持 Vulkan\n")
            tvResult.append("2. 尝试更大的模型\n")
            tvResult.append("3. 增加推理长度\n")
        }
    }

    /**
     * 执行推理测试
     * @return Pair(耗时ms, 输出文本)
     */
    private fun runInferenceTest(useGpu: Boolean): Pair<Long, String> {
        val fileManager = ModelFileManager.getInstance(this)
        val modelPath = fileManager.getModelPath("qwen2-0.5b-instruct-q4_0.gguf")

        val wrapper = LlamaWrapper(this)

        try {
            // 加载模型
            val loadSuccess = wrapper.loadModel(
                modelPath = modelPath,
                cpuThreads = if (useGpu) 1 else 4  // GPU模式减少CPU线程
            )

            if (!loadSuccess) {
                return Pair(0L, "模型加载失败")
            }

            // 测试 Prompt
            val testPrompt = """
                <|im_start|>system
                You are a helpful assistant.<|im_end|>
                <|im_start|>user
                用一句话介绍人工智能。<|im_end|>
                <|im_start|>assistant
            """.trimIndent()

            // 计时推理
            var output = ""
            val timeMs = measureTimeMillis {
                output = wrapper.generate(
                    prompt = testPrompt,
                    temperature = 0.7f,
                    maxTokens = 50  // 限制长度以便快速测试
                )
            }

            wrapper.release()

            return Pair(timeMs, output)

        } catch (e: Exception) {
            wrapper.release()
            return Pair(0L, "错误: ${e.message}")
        }
    }
}