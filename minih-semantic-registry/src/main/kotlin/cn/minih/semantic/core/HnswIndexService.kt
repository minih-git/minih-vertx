package cn.minih.semantic.core

import cn.minih.core.annotation.Component
import com.github.jelmerk.knn.hnsw.HnswIndex
import java.util.Optional

/**
 * HNSW 向量索引服务 (In-Memory)
 */
@Component
class HnswIndexService {

    // HNSW Parameters
    // M: Number of bi-directional links created for every new element during construction. 
    // Higher M works better for high dimensional data/high recall needs but slower construction.
    private val M = 16 
    
    // ef: Size of the dynamic list for the nearest neighbors (used during construction).
    // Higher ef leads to more accurate but slower construction.
    private val EF_CONSTRUCTION = 200

    private val MAX_ITEM_COUNT = 10000

    private val index: HnswIndex<String, FloatArray, SemanticItem, Float> = HnswIndex
        .newBuilder(384, com.github.jelmerk.knn.DistanceFunctions.FLOAT_COSINE_DISTANCE, MAX_ITEM_COUNT)
        .withM(M)
        .withEf(EF_CONSTRUCTION)
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