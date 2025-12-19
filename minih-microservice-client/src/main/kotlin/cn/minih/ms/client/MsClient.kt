@file:Suppress("unused")

package cn.minih.ms.client

import cn.minih.common.exception.MinihException
import io.vertx.core.json.JsonObject
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadLocalRandom
import io.vertx.ext.web.client.WebClient

/**
 *  微服务客户端
 * @author hubin
 * @since 2023-08-01 13:58:12
 */
object MsClient {

    private val serviceCache = mutableMapOf<String, List<Record>>()

    private suspend fun getServiceFromCache(name: String): List<Record>? {
        // if (serviceCache.containsKey(name)) {
        //     return serviceCache[name]
        // }
        // val records = MsClientContext.instance.discovery.getRecords { it.name == name }.await()
        // serviceCache[name] = records
        // return records
        return null
    }

    private fun getServiceFromCacheNoSuspend(name: String): Future<List<Record>?> {
        // if (serviceCache.containsKey(name)) {
        //     return Future.succeededFuture(serviceCache[name])
        // }
        // return MsClientContext.instance.discovery.getRecords { it.name == name }.compose {
        //     serviceCache[name] = it
        //     Future.succeededFuture(it)
        // }
        return Future.succeededFuture(null)
    }


    suspend fun getAvailableService(name: String): Record? {
        val records = getServiceFromCache(name)
        if (records.isNullOrEmpty()) {
            return null
        }
        val number = ThreadLocalRandom.current().nextInt(records.size)
        return records[number % records.size]
    }

    fun getAvailableServiceNoSuspend(name: String): Future<Record?> {
        return getServiceFromCacheNoSuspend(name).compose {
            if (it.isNullOrEmpty()) {
                Future.failedFuture<Record>(MinihException("未找到远程服务！"))
            }
            try {
                var r: Record? = null
                it?.let {
                    val number = ThreadLocalRandom.current().nextInt(it.size)
                    r = it[number % it.size]
                }
                Future.succeededFuture(r)
            } catch (e: Throwable) {
                Future.failedFuture<Record>(MinihException("未找到远程服务！"))
            }
        }
    }


    suspend fun getAvailableHttpService(name: String): HttpClient? {
        val record = getAvailableService(name)
        // val ref = MsClientContext.instance.discovery.getReference(record)
        // return ref.getAs(HttpClient::class.java)
        return null
    }

    fun updateCache() {
        CoroutineScope(Dispatchers.Default).launch {
            if (serviceCache.isNotEmpty()) {
                serviceCache.forEach {
                    // val records = MsClientContext.instance.discovery.getRecords { rc -> rc.name == it.key }.await()
                    // if (records.isEmpty()) {
                    //    serviceCache.remove(it.key)
                    // } else {
                    //    serviceCache[it.key] = records
                    // }
                }
            }
        }
    }




    /**
     * 根据自然语言描述查找服务
     */
    suspend fun getServiceBySemantic(description: String): List<JsonObject> {
        return try {
            val endpoint = io.vertx.core.net.SocketAddress.inetSocketAddress(8099, "localhost") // Should be from config
            // For simplicity in this context, assuming localhost:8099 or using a static config
            // Better to access MsClientContext or similar if available, but for now hardcode/default is OK for the refactor scope

            val client = WebClient.create(MsClientContext.instance.vertx)
            val msg = JsonObject().put("query", description).put("k", 3)

            // Using default port 8099 for now as per plan
            val response = client.post(8099, "localhost", "/semantic/api/search")
                .sendJsonObject(msg)
                .await()

            if (response.statusCode() == 200) {
                 val body = response.bodyAsJsonArray()
                 val list = mutableListOf<JsonObject>()
                 for (i in 0 until body.size()) {
                     list.add(body.getJsonObject(i))
                 }
                 list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}