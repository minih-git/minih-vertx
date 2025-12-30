package cn.minih.semantic.core

import cn.minih.core.annotation.Component
import io.vertx.core.impl.logging.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 实例心跳表
 * 维护 (ServiceId -> LastHeartbeat) 的映射，用于检测实例是否存活
 * @author hubin
 * @since 2024-12-19
 */
@Component
class InstanceTable {

    private val log = LoggerFactory.getLogger(InstanceTable::class.java)

    /**
     * 实例心跳记录: id -> 最后心跳时间戳(毫秒)
     */
    private val instances = ConcurrentHashMap<String, Long>()

    /**
     * 超时时间: 90秒
     */
    private val TTL_MS = 90_000L

    /**
     * 注册实例，记录当前时间为最后心跳时间
     */
    fun register(id: String) {
        instances[id] = System.currentTimeMillis()
        log.debug("Instance registered: $id")
    }

    /**
     * 更新心跳时间
     * @return true 如果实例存在且更新成功，false 如果实例不存在
     */
    fun heartbeat(id: String): Boolean {
        return if (instances.containsKey(id)) {
            instances[id] = System.currentTimeMillis()
            log.debug("Heartbeat received: $id")
            true
        } else {
            log.warn("Heartbeat for unknown instance: $id")
            false
        }
    }

    /**
     * 注销实例
     */
    fun unregister(id: String) {
        instances.remove(id)
        log.debug("Instance unregistered: $id")
    }

    /**
     * 获取所有超时的实例 ID 列表
     * 超时判定: 当前时间 - 最后心跳时间 > TTL
     */
    fun getExpiredInstances(): List<String> {
        val now = System.currentTimeMillis()
        return instances.entries
            .filter { now - it.value > TTL_MS }
            .map { it.key }
    }
    
    /**
     * 原子操作：检查并移除已过期的实例。
     * 使用 computeIfPresent 确保如果在检查过程中收到了新的心跳，移除操作会取消。
     * @return 移除的实例列表
     */
    fun removeAndGetExpired(): List<String> {
         val now = System.currentTimeMillis()
         val expired = mutableListOf<String>()
         
         // 遍历所有实例，检查是否超时（只读检查）
         instances.forEach { (id, lastHeartbeat) ->
             if (now - lastHeartbeat > TTL_MS) {
                 // 原子操作：仅当值仍为 lastHeartbeat 时移除
                 // 这可以避免移除在检查期间刚刚更新了心跳的实例
                 if (instances.remove(id, lastHeartbeat)) {
                     // 只有移除成功才认为该实例确已过期并被当前线程清理
                     expired.add(id)
                     log.debug("Instance expired and removed: $id")
                 }
             }
         }
         return expired
    }

    /**
     * 获取当前注册的实例数量
     */
    fun size(): Int = instances.size

    /**
     * 检查实例是否存在
     */
    fun exists(id: String): Boolean = instances.containsKey(id)
}
