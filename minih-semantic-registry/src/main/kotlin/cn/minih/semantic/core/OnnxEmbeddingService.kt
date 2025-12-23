package cn.minih.semantic.core

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import cn.minih.common.exception.MinihException
import cn.minih.core.annotation.Component
import cn.minih.core.config.Config
import io.vertx.core.impl.logging.LoggerFactory
import java.io.File
import java.io.InputStream
import java.nio.LongBuffer

/**
 * 基于 ONNX Runtime 的 Embedding 服务
 * 使用 all-MiniLM-L6-v2 模型将文本转换为 384 维向量
 */
@Component
class OnnxEmbeddingService {

    private val log = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: HuggingFaceTokenizer? = null

    private var modelPath: String = "main/model.onnx"
    private var tokenizerPath: String = "main/tokenizer.json"

    init {
        try {
            env = OrtEnvironment.getEnvironment()

            // 1. 加载模型
            // Check if configured in system properties or env vars, else default
            val configuredModelPath = System.getProperty("minih.semantic.model.path", modelPath)
            val modelStream = loadResource(configuredModelPath)
            
            if (modelStream != null) {
                val modelBytes = modelStream.readBytes()
                session = env?.createSession(modelBytes, OrtSession.SessionOptions())
                log.info("ONNX Embedding Model loaded successfully from $configuredModelPath")
            } else {
                log.warn("ONNX Model not found at $configuredModelPath. Embedding service will be unavailable.")
            }

            // 2. 加载 Tokenizer
            val configuredTokenizerPath = System.getProperty("minih.semantic.tokenizer.path", tokenizerPath)
            val tokenStream = loadResource(configuredTokenizerPath)
            
            if (tokenStream != null) {
                // DJL HuggingFaceTokenizer 支持从 InputStream 创建
                tokenizer = HuggingFaceTokenizer.newInstance(tokenStream, null)
                log.info("Tokenizer loaded successfully from $configuredTokenizerPath")
            } else {
                log.warn("Tokenizer not found at $configuredTokenizerPath. Please download 'tokenizer.json'.")
            }

        } catch (e: Exception) {
            log.error("Failed to initialize ONNX Runtime or Tokenizer", e)
        }
    }

    private fun loadResource(path: String): InputStream? {
        val f = File(path)
        if (f.exists()) return f.inputStream()
        return javaClass.classLoader.getResourceAsStream(path)
    }

    /**
     * 计算文本的 Embedding 向量
     */
    fun embed(text: String): FloatArray {
        if (session == null || tokenizer == null) {
            throw MinihException("Model or Tokenizer not initialized. Ensure 'model.onnx' and 'tokenizer.json' exist.")
        }

        try {
            // 1. Tokenize
            val encoding = tokenizer!!.encode(text)
            val inputIds = encoding.ids
            val attentionMask = encoding.attentionMask
            val tokenTypeIds = encoding.typeIds

            // 2. Prepare Tensors
            val env = this.env!!
            // input_ids: [1, seq_len]
            val shape = longArrayOf(1, inputIds.size.toLong())

            // 使用 Kotlin 的 .use() 自动管理资源闭包，确保护资源在退出作用域时自动释放
            return OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape).use { tensorIds ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape).use { tensorMask ->
                    OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape).use { tensorTypeIds ->
                        val inputs = mapOf(
                            "input_ids" to tensorIds,
                            "attention_mask" to tensorMask,
                            "token_type_ids" to tensorTypeIds
                        )

                        // 3. Run Inference
                        session!!.run(inputs).use { results ->
                            // 4. Extract Output
                            val lastHiddenState = results[0].value as Array<Array<FloatArray>>

                            // 提取 Batch 0
                            val tokenEmbeddings = lastHiddenState[0] // [SeqLen, 384]

                            // 5. Mean Pooling & Normalize
                            meanPooling(tokenEmbeddings, attentionMask)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            log.error("Embedding failed", e)
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