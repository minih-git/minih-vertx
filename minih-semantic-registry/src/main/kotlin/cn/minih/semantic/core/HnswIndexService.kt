package cn.minih.semantic.core

import cn.minih.core.annotation.Component
import com.github.jelmerk.knn.hnsw.HnswIndex
import java.util.Optional

/**
 * HNSW 向量索引服务 (In-Memory)
 */
@Component
class HnswIndexService {

    // maxItemCount = 10000 假设
    private val index: HnswIndex<String, FloatArray, SemanticItem, Float> = HnswIndex
        .newBuilder(384, com.github.jelmerk.knn.DistanceFunctions.FLOAT_COSINE_DISTANCE, 10000)
        .withM(16)
        .withEf(200)
        .build()

    fun add(id: String, vector: FloatArray, metadata: String) {
        val item = SemanticItem(id, vector, metadata)
        index.add(item)
    }

    fun remove(id: String) {
        index.remove(id, 0) // version 0 if concurrent support needed, but using default
    }

    /**
     * 搜索最相似的 K 个服务
     */
    fun search(vector: FloatArray, k: Int): List<SearchResult> {
        val results = index.findNearest(vector, k)
        return results.map {
            SearchResult(it.item().id(), it.distance(), it.item().payload)
        }
    }

    data class SearchResult(
        val id: String,
        val distance: Float,
        val payload: String
    )
}