@file:Suppress("unused")

package cn.minih.ms.client

import io.vertx.core.http.HttpClient
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.Record
import java.util.concurrent.ThreadLocalRandom

/**
 *  微服务客户端
 * @author hubin
 * @since 2023-08-01 13:58:12
 */
object MsClient {

    suspend fun getAvailableService(name: String): Record? {
        val records = MsClientContext.instance.discovery.getRecords { it.name == name }.await()
        if (records.size == 0) {
            return null
        }
        val number = ThreadLocalRandom.current().nextInt(records.size)
        return records[number % records.size]
    }

    suspend fun getAvailableHttpService(name: String): HttpClient? {
        val record = getAvailableService(name)
        val ref = MsClientContext.instance.discovery.getReference(record)
        return ref.getAs(HttpClient::class.java)

    }


}
