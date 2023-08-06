package cn.minih.ms.backend

import cn.minih.common.util.getEnv
import cn.minih.common.util.toJsonObject
import cn.minih.ms.client.MsClient
import io.vertx.core.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.consul.*
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.Status
import io.vertx.servicediscovery.impl.ServiceTypes
import io.vertx.servicediscovery.spi.ServiceDiscoveryBackend
import io.vertx.servicediscovery.spi.ServiceType
import java.util.*
import java.util.stream.Collectors
import kotlin.concurrent.scheduleAtFixedRate


/**
 *  服务发现consul后端
 * @author hubin
 * @since 2023-08-01 09:12:13
 */
@Suppress("UNCHECKED_CAST")
class ConsulBackendService : ServiceDiscoveryBackend {

    private lateinit var client: ConsulClient


    override fun init(vertx: Vertx?, config: JsonObject?) {
        val opt = ConsulClientOptions(config)
        this.client = ConsulClient.create(vertx, opt)
    }

    override fun store(record: Record, resultHandler: Handler<AsyncResult<Record>>?) {
        val serviceOptions: ServiceOptions = recordToServiceOptions(record)
        val registration: Promise<Void> = Promise.promise()
        client.registerService(serviceOptions).onComplete(registration)
        registration.future().map(record).onComplete {
            startHealthCheck(serviceOptions)
            resultHandler?.handle(it)
        }

    }

    private fun startHealthCheck(serviceOptions: ServiceOptions) {
        Timer().scheduleAtFixedRate(0, 15000) {
            client.passCheck(serviceOptions.checkOptions.id)
            MsClient.updateCache()
        }
        client.passCheck(serviceOptions.checkOptions.id)
    }

    override fun remove(record: Record?, resultHandler: Handler<AsyncResult<Record>>) {
        Objects.requireNonNull(record!!.registration, "No registration id in the record")
        val deRegistration = Promise.promise<Void>()
        client.deregisterService(record.registration).onComplete(deRegistration)
        deRegistration.future().map<Any>(record).onComplete(resultHandler as Handler<AsyncResult<Any>>)
    }

    override fun remove(uuid: String, resultHandler: Handler<AsyncResult<Record>>) {
        Objects.requireNonNull<Any>(uuid, "No registration id in the record")
        getRecord(uuid) { asyncRecord ->
            if (asyncRecord.succeeded()) {
                remove(asyncRecord.result(), resultHandler)
            } else {
                resultHandler.handle(Future.failedFuture(asyncRecord.cause()))
            }
        }
    }

    override fun update(record: Record, resultHandler: Handler<AsyncResult<Void>>?) {
        Objects.requireNonNull(record.registration, "No registration id in the record")
        client.registerService(recordToServiceOptions(record))?.onComplete(resultHandler)
    }


    override fun getRecords(resultHandler: Handler<AsyncResult<MutableList<Record>>>) {
        val nameList: Promise<ServiceList> = Promise.promise()
        client.catalogServices().onComplete(nameList)
        nameList.future().map { obj: ServiceList -> obj.list }.map { l ->
            val recordFutureList: MutableList<Future<ServiceList>> = ArrayList()
            l.forEach { s ->
                if ("consul" != s.name) {
                    val serviceList: Promise<ServiceList> = Promise.promise()
                    val options = ServiceQueryOptions()
                    options.tag = getEnv()
                    client.catalogServiceNodesWithOptions(s.name, options).onComplete(serviceList)
                    recordFutureList.add(serviceList.future())
                }
            }
            recordFutureList
        }.compose { Future.all(it) }
            .map { c ->
                c.list<ServiceList>().stream().flatMap { l -> l.list.stream().map { serviceToRecord(it) } }
            }.compose { Future.all(it.toList()) }.map { c ->
                c.list<Record>().stream().collect(Collectors.toList())
            }.onComplete(resultHandler)
    }

    override fun getRecord(uuid: String?, resultHandler: Handler<AsyncResult<Record>>?) {
        val recordList: Promise<List<Record>> = Promise.promise()
        getRecords(recordList as Handler<AsyncResult<MutableList<Record>>>)
        recordList.future().map { l ->
            l.stream().filter { r -> uuid.equals(r.registration) }.findFirst().orElse(null)
        }.onComplete(resultHandler)

    }


    private fun recordToServiceOptions(record: Record): ServiceOptions {
        val serviceOptions = ServiceOptions()
        serviceOptions.setName(record.name)
        val tags = JsonArray()
        serviceOptions.checkOptions = CheckOptions(
            jsonObjectOf(
                "id" to record.registration,
                "name" to "${record.name}  health check",
                "ttl" to "18s"
            )
        )
        serviceOptions.setId(record.registration)
        if (!tags.contains(record.type) && record.type != null) {
            tags.add(record.type)
        }
        val meta = mutableMapOf<String, String>()
        record.location?.let {
            if (it.containsKey("host")) {
                serviceOptions.setAddress(it.getString("host"))
            }
            if (it.containsKey("port")) {
                serviceOptions.setPort(it.getInteger("port"))
            }
            it.forEach { l ->
                meta[l.key] = l.value.toString()
                tags.add(l.value.toString())
            }
        }
        record.metadata?.let {
            if (it.containsKey("env")) {
                tags.add(it.getString("env"))
            }
            it.forEach { l ->
                meta[l.key] = l.value.toString()
            }
        }

        serviceOptions.meta = meta
        serviceOptions.setTags(tags.stream().map(java.lang.String::valueOf).collect(Collectors.toList()))
        return serviceOptions
    }

    private fun serviceToRecord(service: Service): Future<Record> {
        val checkListFuture: Promise<CheckList> = Promise.promise()
        client.healthChecks(service.name).onComplete(checkListFuture)
        return checkListFuture.future().map { cl ->
            val record = Record()
            record.status =
                if (cl.list.any { check -> (check.status == CheckStatus.PASSING) }) Status.UP else Status.DOWN
            record.setMetadata(JsonObject())
            record.setLocation(JsonObject())
            record.setName(service.name)
            val tags = service.tags
            record.setType(ServiceType.UNKNOWN)
            record.setRegistration(service.id)
            ServiceTypes.all().forEachRemaining { type: ServiceType ->
                if (service.tags.contains(type.name())) {
                    record.setType(type.name())
                    tags.remove(type.name())
                }
            }
            record.setRegistration(service.id)
            record.location = service.meta.toJsonObject()
            if (record.location.containsKey("port")) {
                record.location.put("port", record.location.getString("port").toInt())
            }

            record
        }
    }
}