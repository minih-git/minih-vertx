package cn.minih.semantic

import cn.minih.semantic.core.HnswIndexService
import cn.minih.semantic.core.InstanceTable
import cn.minih.semantic.core.OnnxEmbeddingService
import cn.minih.semantic.registry.SemanticRegistryVerticle
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    
    // 手动初始化服务，或者如果配置了依赖注入则通过DI初始化
    val embeddingService = OnnxEmbeddingService()
    val hnswIndexService = HnswIndexService()
    val instanceTable = InstanceTable()
    
    vertx.deployVerticle(SemanticRegistryVerticle(embeddingService, hnswIndexService, instanceTable))
        .onSuccess { 
            println("语义注册中心启动成功！")
        }
        .onFailure { 
            println("语义注册中心启动失败: ${it.message}")
            it.printStackTrace()
        }
}
