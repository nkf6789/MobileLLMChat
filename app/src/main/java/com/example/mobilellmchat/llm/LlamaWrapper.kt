package com.example.mobilellmchat.llm

import android.content.Context
import android.util.Log
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import de.kherud.llama.InferenceParameters
import java.io.File

class LlamaWrapper(
    private val context: Context
) {
    companion object {
        private const val TAG = "LlamaWrapper"

        // ✅ 新增：静态初始化块，手动加载 native 库
        init {
            try {
                // 按照依赖顺序加载（libllama.so 依赖 libjllama.so）
                System.loadLibrary("jllama")
                System.loadLibrary("llama")
                Log.d(TAG, "✅ Native 库手动加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Native 库手动加载失败", e)
                // 让 llama 库自己尝试加载（备用方案）
            }
        }
    }

    // ✅ 在 LlamaWrapper 类中添加这个方法（放在 companion object 之后）
    private fun getStopStrings(prompt: String): Array<String> {
        return if (prompt.contains("<|im_start|>")) {
            // Qwen2 格式
            arrayOf("<|im_end|>", "<|endoftext|>")
        } else {
            // TinyLlama 格式
            arrayOf("</s>", "<|endoftext|>")
        }
    }

    private var llamaModel: LlamaModel? = null
    private var isModelLoaded = false

    fun loadModel(
        modelPath: String,
        cpuThreads: Int = 4
    ): Boolean {
        return try {
            Log.d(TAG, "开始加载模型: $modelPath")

            // ✅ 添加：详细的路径验证
            val modelFile = File(modelPath)
            Log.d(TAG, "文件路径: ${modelFile.absolutePath}")
            Log.d(TAG, "文件存在: ${modelFile.exists()}")
            Log.d(TAG, "文件大小: ${modelFile.length() / (1024 * 1024)} MB")
            Log.d(TAG, "可读: ${modelFile.canRead()}")
            Log.d(TAG, "父目录: ${modelFile.parent}")

            if (!modelFile.exists()) {
                Log.e(TAG, "模型文件不存在: $modelPath")
                return false
            }

            if (!modelFile.canRead()) {
                Log.e(TAG, "模型文件无法读取: $modelPath")
                return false
            }

            if (modelFile.length() == 0L) {
                Log.e(TAG, "模型文件为空: $modelPath")
                return false
            }

            // ✅ 修改：使用 absolutePath 确保路径规范化
            val normalizedPath = modelFile.absolutePath
            Log.d(TAG, "规范化后的路径: $normalizedPath")

            val modelParams = ModelParameters()
                .setModelFilePath(normalizedPath)  // ← 使用规范化路径
                .setNThreads(cpuThreads)
                .setNCtx(2048)
                .setNGpuLayers(0)

            llamaModel = LlamaModel(modelParams)
            isModelLoaded = true

            Log.d(TAG, "✅ 模型加载成功")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ 模型加载失败", e)
            e.printStackTrace()  // ← 添加完整的堆栈跟踪
            isModelLoaded = false
            false
        }
    }

    fun generate(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): String {
        if (!isModelLoaded || llamaModel == null) {
            throw IllegalStateException("模型未加载，请先调用 loadModel()")
        }

        return try {
            Log.d(TAG, "开始推理，输入长度: ${prompt.length}")

            val stopStrings = getStopStrings(prompt)  // ← 添加这行

            val inferParams = InferenceParameters(prompt)
                .setTemperature(temperature)
                .setNPredict(maxTokens)
                .setStopStrings(*stopStrings)  // ← 使用动态停止符
                .setRepeatPenalty(1.1f)
                .setTopK(40)
                .setTopP(0.9f)

            val output = StringBuilder()

            for (outputItem in llamaModel!!.generate(inferParams)) {
                output.append(outputItem.text)
            }

            val result = output.toString().trim()
            Log.d(TAG, "✅ 推理完成，输出长度: ${result.length}")

            result

        } catch (e: Exception) {
            Log.e(TAG, "❌ 推理失败", e)
            throw e
        }
    }

    fun generateComplete(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): String {
        if (!isModelLoaded || llamaModel == null) {
            throw IllegalStateException("模型未加载，请先调用 loadModel()")
        }

        return try {
            Log.d(TAG, "开始同步推理，输入长度: ${prompt.length}")

            val stopStrings = getStopStrings(prompt)  // ← 添加这行

            val inferParams = InferenceParameters(prompt)
                .setTemperature(temperature)
                .setNPredict(maxTokens)
                .setStopStrings(*stopStrings)  // ← 使用动态停止符
                .setRepeatPenalty(1.1f)  // ← 添加这行
                .setTopK(40)        // ← 添加这行
                .setTopP(0.9f)      // ← 添加这行

            val result = llamaModel!!.complete(inferParams)

            Log.d(TAG, "✅ 同步推理完成，输出长度: ${result.length}")

            result.trim()

        } catch (e: Exception) {
            Log.e(TAG, "❌ 同步推理失败", e)
            throw e
        }
    }

    fun generateStream(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): Iterable<String> {
        if (!isModelLoaded || llamaModel == null) {
            throw IllegalStateException("模型未加载，请先调用 loadModel()")
        }

        val stopStrings = getStopStrings(prompt)  // ← 添加这行

        val inferParams = InferenceParameters(prompt)
            .setTemperature(temperature)
            .setNPredict(maxTokens)
            .setStopStrings(*stopStrings)  // ← 使用动态停止符
            .setRepeatPenalty(1.1f)  // ← 添加这行
            .setTopK(40)        // ← 添加这行
            .setTopP(0.9f)      // ← 添加这行

        return llamaModel!!.generate(inferParams).map { it.text }
    }

    fun stopGeneration() {
        Log.d(TAG, "请求停止生成")
    }

    fun release() {
        try {
            llamaModel?.close()
            llamaModel = null
            isModelLoaded = false
            Log.d(TAG, "✅ 模型资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }

    fun isLoaded(): Boolean = isModelLoaded

    fun getModelInfo(): String {
        return if (isModelLoaded) {
            "已加载"
        } else {
            "未加载"
        }
    }
}