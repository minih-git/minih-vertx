@file:Suppress("unused")

package cn.minih.ms.client

import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadLocalRandom

/**
 *  微服务客户端
 * @author hubin
 * @since 2023-08-01 13:58:12
 */
object MsClient {

    private val serviceCache = mutableMapOf<String, List<Record>>()

    private suspend fun getServiceFromCache(name: String): List<Record>? {
        if (serviceCache.containsKey(name)) {
            return serviceCache[name]
        }
        val records = MsClientContext.instance.discovery.getRecords { it.name == name }.await()
        serviceCache[name] = records
        return records
    }

    private fun getServiceFromCacheNoSuspend(name: String): Future<List<Record>?> {
        if (serviceCache.containsKey(name)) {
            return Future.succeededFuture(serviceCache[name])
        }
        return MsClientContext.instance.discovery.getRecords { it.name == name }.compose {
            serviceCache[name] = it
            Future.succeededFuture(it)
        }

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
            var r: Record? = null
            it?.let {
                val number = ThreadLocalRandom.current().nextInt(it.size)
                r = it[number % it.size]
            }
            Future.succeededFuture(r)
        }
    }


    suspend fun getAvailableHttpService(name: String): HttpClient? {
        val record = getAvailableService(name)
        val ref = MsClientContext.instance.discovery.getReference(record)
        return ref.getAs(HttpClient::class.java)

    }

    fun updateCache() {
        CoroutineScope(Dispatchers.Default).launch {
            if (serviceCache.isNotEmpty()) {
                serviceCache.forEach {
                    val records = MsClientContext.instance.discovery.getRecords { rc -> rc.name == it.key }.await()
                    if (records.isEmpty()) {
                        serviceCache.remove(it.key)
                    } else {
                        serviceCache[it.key] = records
                    }
                }
            }
        }
    }


}