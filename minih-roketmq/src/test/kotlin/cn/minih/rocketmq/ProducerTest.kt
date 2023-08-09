package cn.minih.rocketmq

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

fun main() {
    val vertx = Vertx.vertx()

    val config = vertx.orCreateContext.config()

    vertx.executeBlocking<Void> {
        config.put("minih", jsonObjectOf("rocketmq" to jsonObjectOf("endpoints" to "122.51.65.210:9801")))
        val data = RocketProducerRecord.from("aaa_aa", "abcdefsg")
        val producer = RocketProducer.create<String>(vertx)
        producer.write(data).onComplete { println(it.result()) }.onFailure { println(it.message) }
    }
}