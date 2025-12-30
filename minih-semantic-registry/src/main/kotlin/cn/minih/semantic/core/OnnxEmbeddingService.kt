package cn.minih.semantic.core

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import cn.minih.common.exception.MinihException
import cn.minih.core.annotation.Component
import io.vertx.core.impl.logging.LoggerFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.LongBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 基于 ONNX Runtime 的 Embedding 服务
 * 使用 all-MiniLM-L6-v2 模型将文本转换为 384 维向量
 */
@Component
class OnnxEmbeddingService {

    companion object {
        /** MiniLM-L6-v2 模型最大序列长度 */
        private const val MAX_SEQUENCE_LENGTH = 512
    }

    private val log = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: HuggingFaceTokenizer? = null

    private var modelPath: String = "main/model.onnx"
    private var tokenizerPath: String = "main/tokenizer.json"

    /** 临时模型文件引用，用于 Shutdown Hook 清理 */
    private var modelTempFile: File? = null

    init {
        // 1. 启动时尝试清理旧的临时文件，解决 JVM 异常退出导致的残留问题
        cleanupOldTempFiles()

        // 注册 Shutdown Hook，确保临时文件在 JVM 退出时被清理
        Runtime.getRuntime().addShutdownHook(Thread {
            cleanup()
        })

        env = OrtEnvironment.getEnvironment()

        //  加载模型
        val configuredModelPath = System.getProperty("minih.semantic.model.path", modelPath)

        // 优化：不再读取为 ByteArray，而是确保有物理文件路径供 OnnxRuntime 读取
        val modelFile = getOrCopyResource(configuredModelPath)
            ?: throw MinihException("FATAL: ONNX Model not found at $configuredModelPath. Initialization aborted.")

        // 使用 loadModel(String path) 而不是 loadModel(byte[])，避免堆内存占用
        session = env?.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        log.info("ONNX Embedding Model loaded successfully from ${modelFile.absolutePath}")

        //  加载 Tokenizer
        val configuredTokenizerPath = System.getProperty("minih.semantic.tokenizer.path", tokenizerPath)
        val tokenStream = loadResourceStream(configuredTokenizerPath)
            ?: throw MinihException("FATAL: Tokenizer not found at $configuredTokenizerPath. Initialization aborted.")
        tokenizer = HuggingFaceTokenizer.newInstance(tokenStream, null)
        log.info("Tokenizer loaded successfully from $configuredTokenizerPath")

        log.info("ONNX Embedding Service initialized successfully.")
    }

    /**
     * 清理临时文件及 Native 资源
     */
    private fun cleanup() {
        //  关闭 Session
        try {
            session?.close()
            log.info("OnnxSession closed.")
        } catch (e: Exception) {
            log.warn("Failed to close OnnxSession", e)
        }

        //  关闭 Environment
        try {
            env?.close()
            log.info("OnnxEnvironment closed.")
        } catch (e: Exception) {
            log.warn("Failed to close OnnxEnvironment", e)
        }

        //  关闭 Tokenizer
        try {
            tokenizer?.close()
            log.info("Tokenizer closed.")
        } catch (e: Exception) {
            log.warn("Failed to close Tokenizer", e)
        }

        //  清理临时文件
        modelTempFile?.let { file ->
            try {
                if (file.exists() && file.delete()) {
                    log.info("Cleaned up temporary model file: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                log.warn("Failed to delete temporary model file: ${file.absolutePath}", e)
            }
        }
    }

    /**
     * 启动时清理残留的临时文件
     * 仅尝试删除，忽略被锁定的文件
     */
    private fun cleanupOldTempFiles() {
        try {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "minih-semantic")
            if (tempDir.exists() && tempDir.isDirectory) {
                log.info("Checking for old temporary files in: ${tempDir.absolutePath}")

                 // 列出所有符合模式且不是当前正在使用的文件
                tempDir.listFiles { f ->
                    f.isFile && f.name.startsWith("onnx-model-") && f.name.endsWith(".onnx")
                }?.forEach { file ->
                    try {
                        if (file.delete()) {
                            log.info("Cleaned up old temporary model file: ${file.absolutePath}")
                        }
                    } catch (_: Exception) {
                        // 忽略删除错误，可能是文件被锁定
                        log.debug("Could not delete temp file (might be in use): ${file.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to cleanup old temp files", e)
        }
    }

    /**
     * 获取资源文件。
     * 如果是本地文件，直接返回。
     * 如果是 Classpath 资源，复制到临时文件并返回。
     */
    private fun getOrCopyResource(path: String): File? {
        //  尝试作为绝对路径或相对路径直接读取
        val f = File(path)
        if (f.exists()) return f

        //  尝试从 Classpath 读取
        val stream = javaClass.classLoader.getResourceAsStream(path) ?: return null

        return try {
            // 创建临时文件（使用固定目录便于管理）
            val tempDir = File(System.getProperty("java.io.tmpdir"), "minih-semantic")
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw MinihException("Failed to create temp directory: ${tempDir.absolutePath}")
            }

            val tempFile = File(tempDir, "onnx-model-${System.currentTimeMillis()}.onnx")
            // 注意：不再使用 deleteOnExit()，改用 Shutdown Hook 主动清理

            // 复制流到文件
            tempFile.outputStream().use { output ->
                stream.copyTo(output)
            }

            // 保存引用供 Shutdown Hook 使用
            this.modelTempFile = tempFile

            log.info("Copied classpath resource '$path' to temporary file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            log.error("Failed to copy resource to temp file: $path", e)
            throw MinihException("Failed to copy resource to temp file: $path")
        } finally {
            stream.close()
        }
    }

    private fun loadResourceStream(path: String): InputStream? {
        val f = File(path)
        if (f.exists()) return f.inputStream()
        return javaClass.classLoader.getResourceAsStream(path)
    }

    /**
     * 计算文本的 Embedding 向量 (异步非阻塞)
     *
     * @param text 输入文本
     * @return 归一化后的向量 (384维)
     */
    /**
     * 计算文本的 Embedding 向量
     * 移除了 inferenceLock，利用 OrtSession 的线程安全特性实现高并发
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val currentSession = session ?: throw MinihException("Session not initialized")
        val currentTokenizer = tokenizer ?: throw MinihException("Tokenizer not initialized")
        val currentEnv = env ?: throw MinihException("Environment not initialized")

        try {
            // 1. Tokenization
            val encoding = currentTokenizer.encode(text)

            // 2. 准备输入数据 (限制最大长度)
            val inputIds = encoding.ids.let { if (it.size > MAX_SEQUENCE_LENGTH) it.copyOfRange(0, MAX_SEQUENCE_LENGTH) else it }
            val attentionMask = encoding.attentionMask.let { if (it.size > MAX_SEQUENCE_LENGTH) it.copyOfRange(0, MAX_SEQUENCE_LENGTH) else it }
            val tokenTypeIds = encoding.typeIds.let { if (it.size > MAX_SEQUENCE_LENGTH) it.copyOfRange(0, MAX_SEQUENCE_LENGTH) else it }

            val shape = longArrayOf(1, inputIds.size.toLong())

            // 3. 使用 Map 管理输入资源，确保即使出错也能释放
            val inputTensors = mutableMapOf<String, OnnxTensor>()
            try {
                inputTensors["input_ids"] = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(inputIds), shape)
                inputTensors["attention_mask"] = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(attentionMask), shape)
                inputTensors["token_type_ids"] = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(tokenTypeIds), shape)

                // 4. 执行推理
                currentSession.run(inputTensors).use { results ->
                    val outputTensor = results[0] as OnnxTensor

                    // 获取三维数组: [batch][seq_len][hidden_size]
                    // 对于 MiniLM，这里通常是 FloatArray 的多维表示
                    val rawOutput = outputTensor.value as Array<Array<FloatArray>>
                    val tokenEmbeddings = rawOutput[0] // 取 Batch 中的第一个

                    // 5. 池化与归一化
                    return@withContext meanPooling(tokenEmbeddings, attentionMask)
                }
            } finally {
                // 显式释放所有输入 Tensor 占用的 Native 内存
                inputTensors.values.forEach { it.close() }
            }
        } catch (e: Exception) {
            log.error("Embedding inference failed for text: ${text.take(20)}...", e)
            throw MinihException("Embedding failed: ${e.message}")
        }
    }

    private fun meanPooling(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        // tokenEmbeddings: [SeqLen, 384]
        // attentionMask: [SeqLen]

        val hiddenSize = tokenEmbeddings[0].size
        val sum = FloatArray(hiddenSize)
        var count = 0

        for (i in attentionMask.indices) {
            // Mask=1 表示有效 token
            if (attentionMask[i] == 1L) {
                val vec = tokenEmbeddings[i]
                for (j in 0 until hiddenSize) {
                    sum[j] += vec[j]
                }
                count++
            }
        }

        // Average
        if (count > 0) {
            for (j in 0 until hiddenSize) {
                sum[j] /= count.toFloat()
            }
        }

        return normalize(sum)
    }

    private fun normalize(vec: FloatArray): FloatArray {
        var dot = 0.0
        for (v in vec) {
            dot += (v * v)
        }
        val norm = Math.sqrt(dot).toFloat()

        // 避免除以 0
        if (norm > 1e-12) {
            for (i in vec.indices) {
                vec[i] /= norm
            }
        }
        return vec
    }
}