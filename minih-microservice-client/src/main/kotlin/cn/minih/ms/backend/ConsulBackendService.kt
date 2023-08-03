package cn.minih.ms.backend

import cn.minih.core.exception.MinihException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.SnowFlakeContext
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
        Assert.isNull(record.registration) { throw MinihException("The record has already been registered") }
        val serviceOptions: ServiceOptions = recordToServiceOptions(record)
        record.setRegistration(serviceOptions.id)
        val registration: Promise<Void> = Promise.promise()
        client.registerService(serviceOptions).onComplete(registration)
        registration.future().map(record).onComplete(resultHandler)
        Timer().scheduleAtFixedRate(0, 10000) {
            client.passCheck(serviceOptions.checkOptions.id)
        }
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
        nameList.future().map { obj: ServiceList -> obj.list }
            .map { l ->
                val recordFutureList: MutableList<Future<ServiceList>> =
                    ArrayList()
                l.forEach { s ->
                    if ("consul" != s.name) {
                        val opt = ServiceQueryOptions()
                        if (s.tags.isNotEmpty()) {
                            opt.setTag(s.tags[0])
                        }
                        val serviceList: Promise<ServiceList> = Promise.promise()
                        client.catalogServiceNodesWithOptions(s.name, opt).onComplete(serviceList)
                        recordFutureList.add(serviceList.future())
                    }
                }
                recordFutureList
            }
            .compose {
                Future.all(it)
            }
            .map { c ->
                c.list<ServiceList>().stream().flatMap { l -> l.list.stream().map { serviceToRecord(it) } }
            }
            .compose { Future.all(it.toList()) }
            .map { c ->
                c.list<Record>().stream().collect(Collectors.toList())
            }
            .onComplete(resultHandler)
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
        if (record.metadata != null) {
            tags.addAll(record.metadata.getJsonArray("tags", JsonArray()))
            //only put CheckOptions to newly registered services
            if (record.registration == null) {
                serviceOptions.setCheckOptions(
                    CheckOptions(
                        jsonObjectOf(
                            "id" to SnowFlakeContext.instance.currentContext().nextId().toString(),
                            "name" to "${record.name}  health check",
                            "ttl" to "15s"
                        )
                    )
                )
                record.metadata.remove("checkOptions")
            }

            record.metadata.remove("tags")
            tags.add("metadata:" + record.metadata.encode())
        }
        if (record.registration != null) {
            serviceOptions.setId(record.registration)
        } else {
            serviceOptions.setId(record.metadata.getString("serverId"))
        }
        if (!tags.contains(record.type) && record.type != null) {
            tags.add(record.type)
        }
        if (record.location != null) {
            if (record.location.containsKey("host")) {
                serviceOptions.setAddress(record.location.getString("host"))
            }
            if (record.location.containsKey("port")) {
                serviceOptions.setPort(record.location.getInteger("port"))
            }
            tags.add("location:" + record.location.encode())
        }
        serviceOptions.setTags(tags.stream().map(java.lang.String::valueOf).collect(Collectors.toList()))
        return serviceOptions
    }

    private fun serviceToRecord(service: Service): Future<Record> {
        //use the checks to set the record status
        val checkListFuture: Promise<CheckList> = Promise.promise()
        client.healthChecks(service.name).onComplete(checkListFuture)
        return checkListFuture.future().map { cl ->
            cl.list.stream().map(Check::getStatus).allMatch(CheckStatus.PASSING::equals)
        }
            .map { st ->
                if (st) Record().setStatus(Status.UP) else Record()
                    .setStatus(Status.DOWN)
            }
            .map { record ->
                record.setMetadata(JsonObject())
                record.setLocation(JsonObject())
                record.setName(service.name)
                record.setRegistration(service.id)
                val tags = service.tags
                record.setType(ServiceType.UNKNOWN)
                ServiceTypes.all()
                    .forEachRemaining { type: ServiceType ->
                        if (service.tags.contains(type.name())) {
                            record.setType(type.name())
                            tags.remove(type.name())
                        }
                    }
                //retrieve the metadata object
                tags.stream()
                    .filter { t: String? -> t!!.startsWith("metadata:") }
                    .map { s: String? -> s!!.substring("metadata:".length) }
                    .map { json: String? ->
                        JsonObject(
                            json
                        )
                    }.forEach { json: JsonObject? ->
                        record.metadata.mergeIn(json)
                    }
                //retrieve the location object
                tags.stream()
                    .filter { t: String? -> t!!.startsWith("location:") }
                    .map { s: String? -> s!!.substring("location:".length) }
                    .map { json: String? ->
                        JsonObject(
                            json
                        )
                    }.forEach { json: JsonObject? ->
                        record.location.mergeIn(json)
                    }
                record.metadata.put(
                    "tags",
                    JsonArray(
                        tags.stream().filter { t: String? ->
                            !t!!.startsWith(
                                "metadata:"
                            ) && !t.startsWith("location:")
                        }
                            .collect(Collectors.toList())
                    )
                )
                record
            }
    }
}