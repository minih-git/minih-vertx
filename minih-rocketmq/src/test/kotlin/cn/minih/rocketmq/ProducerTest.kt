package cn.minih.rocketmq

import cn.minih.rocketmq.consumer.RocketConsumer
import cn.minih.rocketmq.producer.RocketProducer
import cn.minih.rocketmq.producer.RocketProducerRecord
import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.jsonObjectOf

/**
 * 生产者测试
 * @author hubin
 * @since 2023-08-09 23:41:45
 * @desc
 */
class ProducerTest

data class A(val a: String)

fun main() {


    val vertx = Vertx.vertx()

    val config = vertx.orCreateContext.config()


    vertx.executeBlocking<Void> {
        config.put("minih", jsonObjectOf("rocketmq" to jsonObjectOf("endpoints" to "122.51.65.210:8081")))

        RocketConsumer.create<A>(vertx).subscribe("aaa_aa", "aaa_aa").handler {
            println("AAAAAAAA:${it.data.a}")
        }
        val data = RocketProducerRecord.from("aaa_aa", A("aaa"))
        val producer = RocketProducer.create<A>(vertx)
        producer.write(data).onComplete { println(it.result()) }.onFailure { println(it.message) }


    }
}