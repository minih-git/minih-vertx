package cn.minih.rocketmq.producer.impl

import cn.minih.rocketmq.producer.RocketProducerRecord
import cn.minih.rocketmq.producer.RocketWriteStream
import io.vertx.core.*
import io.vertx.core.impl.ContextInternal
import io.vertx.core.json.JsonObject
import io.vertx.core.streams.WriteStream
import org.apache.rocketmq.client.apis.producer.Producer
import org.apache.rocketmq.client.apis.producer.SendReceipt

/**
 *  RocketWriteStream实现
 * @author hubin
 * @since 2023-08-09 17:56:09
 */
class RocketWriteStreamImpl<K, V>(private val vertx: Vertx, val config: JsonObject, private val producer: Producer) :
    RocketWriteStream<K, V> {
    private var drainHandler: Handler<Void>? = null
    private var exceptionHandler: Handler<Throwable>? = null
    private var endHandle: Handler<AsyncResult<Void>>? = null
    private var maxSize: Int = Int.MAX_VALUE
    private var pending: Int = 0
    override fun send(message: RocketProducerRecord<K, V>): Future<SendReceipt> {
        val ctx: ContextInternal = vertx.orCreateContext as ContextInternal
        val promise = Promise.promise<SendReceipt>()
        pending += 1
        ctx.executeBlocking<SendReceipt> {
            try {
                val sendReceipt = producer.send(message)
                drainHandler?.let {
                    val drainHandler = drainHandler
                    this.drainHandler = null
                    ctx.runOnContext(drainHandler)
                }
                promise.complete(sendReceipt)
            } catch (e: Exception) {
                exceptionHandler?.let { ctx.runOnContext { _: Void -> it.handle(e) } }
                promise.fail(e)
            }
            endHandle?.let { ctx.runOnContext { _: Void -> it.handle(Future.succeededFuture()) } }
        }
        return promise.future()
    }


    override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<RocketProducerRecord<K, V>> {
        exceptionHandler = handler
        return this
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        endHandle = handler
    }

    override fun setWriteQueueMaxSize(size: Int): WriteStream<RocketProducerRecord<K, V>> {
        maxSize = size
        return this
    }

    override fun writeQueueFull(): Boolean {
        return this.pending >= maxSize
    }

    override fun drainHandler(handler: Handler<Void>?): WriteStream<RocketProducerRecord<K, V>> {
        drainHandler = handler
        return this
    }

    override fun write(data: RocketProducerRecord<K, V>, handler: Handler<AsyncResult<Void>>) {
        send(data).onComplete { Future.succeededFuture<Void>() }
    }

    override fun write(data: RocketProducerRecord<K, V>): Future<Void> {
        return send(data).compose { Future.succeededFuture<Void>() }
    }
}
