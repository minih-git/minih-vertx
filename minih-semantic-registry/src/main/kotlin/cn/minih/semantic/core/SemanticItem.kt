package cn.minih.semantic.core

import com.github.jelmerk.knn.Item

data class SemanticItem(
    private val id: String,
    private val vector: FloatArray,
    val payload: String // 存储服务元数据 JSON
) : Item<String, FloatArray> {
    
    override fun id(): String = id
    override fun vector(): FloatArray = vector
    override fun dimensions(): Int = vector.size
}
