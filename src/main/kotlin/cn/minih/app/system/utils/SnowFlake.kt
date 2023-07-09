package cn.minih.app.system.utils

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.InetAddress

/**
 * @author hubin
 * @date 2023/7/9
 * @desc
 */
val log: KLogger = KotlinLogging.logger {}

object SnowFlake {
    private val workerId = getWorkId(InetAddress.getLocalHost().hostAddress)
    private val datacenterId = getCenterId(InetAddress.getLocalHost().hostAddress)
    private var sequence = 0
    private const val sp = 1585644268888L
    private const val workerIdBits = 5L
    private const val datacenterIdBits = 5L
    private const val sequenceBits = 12L
    private const val workerIdShift = sequenceBits
    private const val datacenterIdShift = sequenceBits + workerIdBits
    private const val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
    private const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())
    private var lastTimestamp = -1L


    @Synchronized
    fun nextId(): Long {
        var timestamp: Long = System.currentTimeMillis()
        if (timestamp < lastTimestamp) {
            log.error { "系统时间不正确" }
            throw RuntimeException("系统时间不正确")
        }
        if (lastTimestamp == timestamp) {
            sequence = ((sequence + 1).toLong() and sequenceMask).toInt()
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp)
            }

        } else {
            sequence = 0
        }
        lastTimestamp = timestamp
        return timestamp - sp shl timestampLeftShift.toInt() or
                (datacenterId shl datacenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or sequence.toLong()
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp: Long = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }

    private fun getCenterId(ip: String): Long {
        val s: List<String> = ip.split(".")
        return ((s[0].toLong() shl 24)
                + (s[1].toLong() shl 16) +
                (0 shl 8) + 0)


    }

    private fun getWorkId(ip: String): Long {
        val s: List<String> = ip.split(".")
        return ((s[0].toLong() shl 24)
                + (s[1].toLong() shl 16) +
                (s[2].toLong() shl 8)
                + s[3].toLong())
    }
}