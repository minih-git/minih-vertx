package cn.minih.rocketmq.consumer.impl

import cn.minih.common.util.jsonConvertData
import cn.minih.rocketmq.consumer.RocketConsumerRecord
import cn.minih.rocketmq.consumer.RocketReadStream
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextInternal
import io.vertx.core.streams.ReadStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer
import org.apache.rocketmq.client.apis.message.MessageView
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass


/**
 *  mq消费流实现
 * @author hubin
 * @since 2023-08-10 09:09:39
 */
class RocketReadStreamImpl<T : Any>(
    vertx: Vertx, private val consumer: SimpleConsumer,
    override val msgClazz: KClass<T>
) : RocketReadStream<T> {

    private var context: ContextInternal = vertx.orCreateContext as ContextInternal

    private var recordHandler: Handler<RocketConsumerRecord<T>>? = null
    private var endHandler: Handler<Void>? = null
    private var errorHandler: Handler<Throwable>? = null
    private val consuming = AtomicBoolean(false)
    private val polling = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val demand = AtomicLong(Long.MAX_VALUE)
    private val maxMessageNum = 16
    private val invisibleDuration = Duration.ofSeconds(15)
    private var current: Iterator<MessageView> = listOf<MessageView>().listIterator()


    private fun run(handler: Handler<RocketConsumerRecord<T>>) {
        if (this.closed.get()) {
            return
        }
        if (!this.current.hasNext()) {
            pollRecords {
                if (it != null && it.isNotEmpty()) {
                    current = it.iterator()
                    schedule(0)
                } else {
                    schedule(1)
                }
            }
        } else {
            var count = 0
            out@ while (this.current.hasNext() && count++ < 10) {
                // 流被暂停后 不应该继续消费
                while (true) {
                    val v = demand.get()
                    if (v <= 0L) {
                        break@out
                    } else if (v == Long.MAX_VALUE || demand.compareAndSet(v, v - 1)) {
                        break
                    }
                }
                val data = current.next()

                val charset = Charset.forName("UTF-8")
                val s = charset.decode(data.body).toString()
                consumer.ack(data)
                context.emit { handler.handle(RocketConsumerRecord.create(s.jsonConvertData(msgClazz))) }
            }
            schedule(0)
        }
    }

    private fun schedule(delay: Long) {
        this.recordHandler?.let { handler ->
            if (consuming.get() && demand.get() > 0L) {
                context.runOnContext {
                    when (delay) {
                        0L -> run(handler)
                        else -> context.owner().setTimer(delay) { run(handler) }
                    }
                }
            }
        }
    }

    private fun pollRecords(handler: Handler<List<MessageView>>) {
        if (this.polling.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                if (!closed.get()) {
                    var submitted = false
                    try {
                        val messages = consumer.receive(maxMessageNum, invisibleDuration)
                        if (messages != null && messages.isNotEmpty()) {
                            submitted = true
                            context.runOnContext { polling.set(false);handler.handle(messages) }
                        }
                    } catch (e: Throwable) {
                        errorHandler?.handle(e)
                    } finally {
                        if (!submitted) {
                            context.runOnContext { polling.set(false); schedule(0) }
                        }
                    }
                }
            }
        }
    }


    override fun exceptionHandler(handler: Handler<Throwable>): ReadStream<RocketConsumerRecord<T>> {
        errorHandler = handler
        return this
    }

    override fun pause(): ReadStream<RocketConsumerRecord<T>> {
        demand.set(0L)
        return this
    }

    override fun resume(): ReadStream<RocketConsumerRecord<T>> {
        return fetch(Long.MAX_VALUE)
    }

    override fun fetch(amount: Long): ReadStream<RocketConsumerRecord<T>> {
        require(amount >= 0) { "Invalid claim $amount" }

        val op = demand.updateAndGet {
            var i = it
            i += amount
            if (i < 0L) {
                i = Long.MAX_VALUE
            }
            i
        }
        if (op > 0L) {
            schedule(0)
        }
        return this
    }

    override fun endHandler(endHandler: Handler<Void>): ReadStream<RocketConsumerRecord<T>> {
        this.endHandler = endHandler
        return this
    }

    override fun handler(handler: Handler<RocketConsumerRecord<T>>): ReadStream<RocketConsumerRecord<T>> {
        this.recordHandler = handler
        consuming.set(true)
        schedule(0)
        return this
    }

}