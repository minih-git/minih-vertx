package cn.minih.semantic.core

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import cn.minih.common.exception.MinihException
import cn.minih.core.annotation.Component
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
            val modelStream = loadResource(modelPath)
            if (modelStream != null) {
                val modelBytes = modelStream.readBytes()
                session = env?.createSession(modelBytes, OrtSession.SessionOptions())
                log.info("ONNX Embedding Model loaded successfully from $modelPath")
            } else {
                log.warn("ONNX Model not found at $modelPath. Embedding service will be unavailable.")
            }

            // 2. 加载 Tokenizer
            val tokenStream = loadResource(tokenizerPath)
            if (tokenStream != null) {
                // DJL HuggingFaceTokenizer 支持从 InputStream 创建
                tokenizer = HuggingFaceTokenizer.newInstance(tokenStream, null)
                log.info("Tokenizer loaded successfully from $tokenizerPath")
            } else {
                log.warn("Tokenizer not found at $tokenizerPath. Please download 'tokenizer.json'.")
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

            // 注意：具体模型的 Input Name 可能不同，标准 BERT 类通常是 input_ids, attention_mask, token_type_ids
            // 这里假设模型包含这三个输入
            val tensorIds = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)
            val tensorMask = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)
            val tensorTypeIds = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape)

            val inputs = mapOf(
                "input_ids" to tensorIds,
                "attention_mask" to tensorMask,
                "token_type_ids" to tensorTypeIds
            )

            // 3. Run Inference
            // 必须关闭 results 以释放 native 内存
            session!!.run(inputs).use { results ->
                // 4. Extract Output
                // all-MiniLM-L6-v2 输出通常名为 'last_hidden_state' 或 '0'
                // dimensions: [batch=1, seq_len, hidden_size=384]
                // OnnxRuntime Java 返回的是 Object，对于 float tensor 可能是 multidimensional array
                // float[][][]
                val lastHiddenState = results[0].value as Array<Array<FloatArray>>

                // 提取 Batch 0
                val tokenEmbeddings = lastHiddenState[0] // [SeqLen, 384]

                // 5. Mean Pooling & Normalize
                val embedding = meanPooling(tokenEmbeddings, attentionMask)
                return embedding
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