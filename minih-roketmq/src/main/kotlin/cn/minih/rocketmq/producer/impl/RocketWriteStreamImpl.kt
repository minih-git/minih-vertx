package cn.minih.rocketmq.producer.impl

import cn.minih.rocketmq.producer.IRocketProducerRecord
import cn.minih.rocketmq.producer.RocketWriteStream
import io.vertx.core.*
import io.vertx.core.impl.ContextInternal
import io.vertx.core.streams.WriteStream
import org.apache.rocketmq.client.apis.producer.Producer
import org.apache.rocketmq.client.apis.producer.SendReceipt

/**
 *  RocketWriteStream实现
 * @author hubin
 * @since 2023-08-09 17:56:09
 */
class RocketWriteStreamImpl<T : Any>(private val vertx: Vertx, private val producer: Producer) :
    RocketWriteStream<T> {
    private var drainHandler: Handler<Void>? = null
    private var exceptionHandler: Handler<Throwable>? = null
    private var endHandle: Handler<AsyncResult<Void>>? = null
    private var maxSize: Int = Int.MAX_VALUE
    private var pending: Int = 0
    override fun send(message: IRocketProducerRecord<T>): Future<SendReceipt> {
        val ctx: ContextInternal = vertx.orCreateContext as ContextInternal
        val promise = Promise.promise<SendReceipt>()
        pending += 1
        try {
            val future = producer.sendAsync(message)
            future.whenComplete { v, e ->
                ctx.runOnContext { v1 ->
                    synchronized(this) {
                        e?.let {
                            exceptionHandler?.let { ctx.runOnContext { _: Void -> it.handle(e) } }
                        }
                        drainHandler?.let {
                            val drainHandler = drainHandler
                            this.drainHandler = null
                            ctx.runOnContext(drainHandler)
                        }
                    }
                }
                e?.let {
                    promise.fail(e)
                    exceptionHandler?.let { ctx.runOnContext { _: Void -> it.handle(e) } }
                } ?: promise.complete(v)
                endHandle?.let { ctx.runOnContext { _: Void -> it.handle(Future.succeededFuture()) } }
            }
        } catch (e: Exception) {
            promise.fail(e)
            exceptionHandler?.let { ctx.runOnContext { _: Void -> it.handle(e) } }
        }
        return promise.future()
    }

    override fun send(message: IRocketProducerRecord<T>, handler: Handler<AsyncResult<Void>>) {
        send(message).onComplete { handler.handle(Future.succeededFuture()) }
    }

    override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<IRocketProducerRecord<T>> {
        exceptionHandler = handler
        return this
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        endHandle = handler
    }

    override fun setWriteQueueMaxSize(size: Int): WriteStream<IRocketProducerRecord<T>> {
        maxSize = size
        return this
    }

    override fun writeQueueFull(): Boolean {
        return this.pending >= maxSize
    }

    override fun drainHandler(handler: Handler<Void>?): WriteStream<IRocketProducerRecord<T>> {
        drainHandler = handler
        return this
    }

    override fun write(data: IRocketProducerRecord<T>, handler: Handler<AsyncResult<Void>>) {
        send(data).onComplete { Future.succeededFuture<Void>() }
    }

    override fun write(data: IRocketProducerRecord<T>): Future<Void> {
        return send(data).compose { Future.succeededFuture() }
    }
}