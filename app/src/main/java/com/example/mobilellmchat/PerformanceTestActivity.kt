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
 * 性能对比测试 Activity
 *
 * 功能:
 * - 对比不同量化版本的推理速度
 * - 对比 CPU vs GPU 模式
 * - 生成性能报告
 */
class PerformanceTestActivity : AppCompatActivity() {

    private lateinit var btnTestQ3: Button
    private lateinit var btnTestQ4: Button
    private lateinit var btnCompare: Button
    private lateinit var btnExportReport: Button
    private lateinit var tvResult: TextView

    // 测试结果数据
    private val testResults = mutableMapOf<String, TestResult>()

    // 测试问题集
    private val testQuestions = listOf(
        "你是谁?",
        "什么是人工智能?",
        "解释一下 Android Activity 生命周期",
        "写一个冒泡排序算法",
        "推荐一部科幻电影"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_test)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnTestQ3 = findViewById(R.id.btnTestQ3)
        btnTestQ4 = findViewById(R.id.btnTestQ4)
        btnCompare = findViewById(R.id.btnCompare)
        btnExportReport = findViewById(R.id.btnExportReport)
        tvResult = findViewById(R.id.tvResult)
    }

    private fun setupListeners() {
        btnTestQ3.setOnClickListener { testModel("qwen2-0.5b-instruct-q3_k_m.gguf", "Q3_K_M") }
        btnTestQ4.setOnClickListener { testModel("qwen2-0.5b-instruct-q4_0.gguf", "Q4_0") }
        btnCompare.setOnClickListener { compareResults() }
        btnExportReport.setOnClickListener { exportReport() }
    }

    private fun testModel(modelName: String, label: String) {
        lifecycleScope.launch {
            tvResult.text = "🔄 正在测试 $label 模型...\n"
            btnTestQ3.isEnabled = false
            btnTestQ4.isEnabled = false

            val result = withContext(Dispatchers.IO) {
                runModelTest(modelName, label)
            }

            testResults[label] = result

            btnTestQ3.isEnabled = true
            btnTestQ4.isEnabled = true

            displayResult(label, result)
        }
    }

    private fun runModelTest(modelName: String, label: String): TestResult {
        val fileManager = ModelFileManager.getInstance(this)
        val modelPath = fileManager.getModelPath(modelName)

        // 检查文件是否存在
        val modelFile = java.io.File(modelPath)
        if (!modelFile.exists()) {
            return TestResult(
                modelName = label,
                success = false,
                error = "模型文件不存在: $modelPath"
            )
        }

        val wrapper = LlamaWrapper(this)

        try {
            // 1. 测试加载时间
            val loadTime = measureTimeMillis {
                wrapper.loadModel(modelPath = modelPath, cpuThreads = 4)
            }

            if (!wrapper.isLoaded()) {
                return TestResult(
                    modelName = label,
                    success = false,
                    error = "模型加载失败"
                )
            }

            // 2. 测试推理性能(多次取平均)
            val inferenceTimes = mutableListOf<Long>()
            val outputs = mutableListOf<String>()

            testQuestions.forEach { question ->
                val prompt = buildPrompt(question)

                var output = ""
                val inferTime = measureTimeMillis {
                    output = wrapper.generate(
                        prompt = prompt,
                        temperature = 0.7f,
                        maxTokens = 100
                    )
                }

                inferenceTimes.add(inferTime)
                outputs.add(output)
            }

            wrapper.release()

            // 3. 计算统计数据
            val avgInferTime = inferenceTimes.average()
            val minInferTime = inferenceTimes.minOrNull() ?: 0L
            val maxInferTime = inferenceTimes.maxOrNull() ?: 0L

            return TestResult(
                modelName = label,
                success = true,
                loadTime = loadTime,
                avgInferenceTime = avgInferTime,
                minInferenceTime = minInferTime,
                maxInferenceTime = maxInferTime,
                fileSize = modelFile.length(),
                sampleOutputs = outputs
            )

        } catch (e: Exception) {
            wrapper.release()
            return TestResult(
                modelName = label,
                success = false,
                error = "测试失败: ${e.message}"
            )
        }
    }

    private fun buildPrompt(question: String): String {
        return """
            <|im_start|>system
            You are a helpful assistant.<|im_end|>
            <|im_start|>user
            $question<|im_end|>
            <|im_start|>assistant
        """.trimIndent()
    }

    private fun displayResult(label: String, result: TestResult) {
        if (!result.success) {
            tvResult.append("\n❌ $label 测试失败\n")
            tvResult.append("错误: ${result.error}\n")
            return
        }

        tvResult.append("\n✅ $label 测试完成\n")
        tvResult.append("━━━━━━━━━━━━━━━━━━━━━\n")
        tvResult.append("📦 文件大小: ${formatSize(result.fileSize)}\n")
        tvResult.append("⏱️ 加载时间: ${result.loadTime}ms\n")
        tvResult.append("📊 推理性能:\n")
        tvResult.append("   • 平均: ${result.avgInferenceTime.toInt()}ms\n")
        tvResult.append("   • 最快: ${result.minInferenceTime}ms\n")
        tvResult.append("   • 最慢: ${result.maxInferenceTime}ms\n")
        tvResult.append("\n📝 示例回答(第1题):\n")
        tvResult.append("${result.sampleOutputs.firstOrNull()?.take(80)}...\n\n")
    }

    private fun compareResults() {
        if (testResults.size < 2) {
            tvResult.append("\n⚠️ 请先运行两个模型的测试\n")
            return
        }

        val q3 = testResults["Q3_K_M"]
        val q4 = testResults["Q4_0"]

        if (q3 == null || q4 == null) {
            tvResult.append("\n⚠️ 缺少测试数据\n")
            return
        }

        tvResult.append("\n━━━━━━━━━━━━━━━━━━━━━\n")
        tvResult.append("📊 性能对比报告\n")
        tvResult.append("━━━━━━━━━━━━━━━━━━━━━\n\n")

        // 文件大小对比
        val sizeDiff = ((q4.fileSize - q3.fileSize).toFloat() / q4.fileSize * 100)
        tvResult.append("📦 文件大小:\n")
        tvResult.append("   Q4_0: ${formatSize(q4.fileSize)}\n")
        tvResult.append("   Q3_K_M: ${formatSize(q3.fileSize)}\n")
        tvResult.append("   👉 Q3 减小了 ${String.format("%.1f", sizeDiff)}%\n\n")

        // 加载速度对比
        val loadSpeedup = q4.loadTime.toFloat() / q3.loadTime
        tvResult.append("⏱️ 加载速度:\n")
        tvResult.append("   Q4_0: ${q4.loadTime}ms\n")
        tvResult.append("   Q3_K_M: ${q3.loadTime}ms\n")
        tvResult.append("   👉 Q3 快了 ${String.format("%.2f", loadSpeedup)}x\n\n")

        // 推理速度对比
        val inferSpeedup = q4.avgInferenceTime / q3.avgInferenceTime
        tvResult.append("🚀 推理速度(平均):\n")
        tvResult.append("   Q4_0: ${q4.avgInferenceTime.toInt()}ms\n")
        tvResult.append("   Q3_K_M: ${q3.avgInferenceTime.toInt()}ms\n")
        tvResult.append("   👉 Q3 快了 ${String.format("%.2f", inferSpeedup)}x\n\n")

        // 综合评价
        tvResult.append("📋 综合评价:\n")
        if (inferSpeedup > 1.2) {
            tvResult.append("   ✅ Q3_K_M 速度提升明显(>20%)\n")
        } else {
            tvResult.append("   ⚠️ Q3_K_M 速度提升有限(<20%)\n")
        }

        tvResult.append("\n💡 建议:\n")
        if (sizeDiff > 20 && inferSpeedup > 1.15) {
            tvResult.append("   推荐使用 Q3_K_M,体积和速度优势明显\n")
        } else {
            tvResult.append("   建议继续使用 Q4_0,质量更有保障\n")
        }
    }

    private fun exportReport() {
        if (testResults.isEmpty()) {
            tvResult.append("\n⚠️ 暂无测试数据\n")
            return
        }

        lifecycleScope.launch {
            val reportContent = generateMarkdownReport()

            // 保存到文件
            val fileName = "performance_report_${System.currentTimeMillis()}.md"
            val file = java.io.File(getExternalFilesDir(null), fileName)

            withContext(Dispatchers.IO) {
                file.writeText(reportContent)
            }

            tvResult.append("\n✅ 报告已导出:\n${file.absolutePath}\n")
        }
    }

    private fun generateMarkdownReport(): String {
        val sb = StringBuilder()

        sb.appendLine("# Qwen2 模型性能对比报告")
        sb.appendLine()
        sb.appendLine("**测试时间:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        sb.appendLine("**测试设备:** iQOO Z9 Turbo+ (12GB RAM)")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        testResults.forEach { (label, result) ->
            if (!result.success) return@forEach

            sb.appendLine("## $label")
            sb.appendLine()
            sb.appendLine("| 指标 | 数值 |")
            sb.appendLine("|------|------|")
            sb.appendLine("| 文件大小 | ${formatSize(result.fileSize)} |")
            sb.appendLine("| 加载时间 | ${result.loadTime}ms |")
            sb.appendLine("| 平均推理时间 | ${result.avgInferenceTime.toInt()}ms |")
            sb.appendLine("| 最快推理 | ${result.minInferenceTime}ms |")
            sb.appendLine("| 最慢推理 | ${result.maxInferenceTime}ms |")
            sb.appendLine()
        }

        if (testResults.size >= 2) {
            val q3 = testResults["Q3_K_M"]
            val q4 = testResults["Q4_0"]

            if (q3 != null && q4 != null) {
                sb.appendLine("## 对比结论")
                sb.appendLine()

                val sizeDiff = ((q4.fileSize - q3.fileSize).toFloat() / q4.fileSize * 100)
                val inferSpeedup = q4.avgInferenceTime / q3.avgInferenceTime

                sb.appendLine("- **体积优化:** Q3_K_M 比 Q4_0 小 ${String.format("%.1f", sizeDiff)}%")
                sb.appendLine("- **速度提升:** Q3_K_M 比 Q4_0 快 ${String.format("%.1f", (inferSpeedup - 1) * 100)}%")
                sb.appendLine()
            }
        }

        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## 测试问题")
        testQuestions.forEachIndexed { index, q ->
            sb.appendLine("${index + 1}. $q")
        }

        return sb.toString()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    // 数据类
    data class TestResult(
        val modelName: String,
        val success: Boolean,
        val error: String? = null,
        val loadTime: Long = 0,
        val avgInferenceTime: Double = 0.0,
        val minInferenceTime: Long = 0,
        val maxInferenceTime: Long = 0,
        val fileSize: Long = 0,
        val sampleOutputs: List<String> = emptyList()
    )
}