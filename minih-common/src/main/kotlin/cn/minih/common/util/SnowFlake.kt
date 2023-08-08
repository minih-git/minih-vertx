@file:Suppress("unused")
package cn.minih.common.util

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

private const val sp = 1585644268888L
private const val workerIdBits = 5L
private const val datacenterIdBits = 5L
private const val sequenceBits = 12L
private const val workerIdShift = sequenceBits
private const val datacenterIdShift = sequenceBits + workerIdBits
private const val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
private const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())

class SnowFlake(wid: String) {
    private val workerId: Long
    private val datacenterId = getCenterId(InetAddress.getLocalHost().hostAddress)
    private var sequence = AtomicLong(0)

    private var lastTimestamp = AtomicLong(-1L)

    init {
        workerId = getWorkId(wid)
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp: Long = System.currentTimeMillis()
        if (timestamp < lastTimestamp.get()) {
            log.error { "系统时间不正确" }
            throw RuntimeException("系统时间不正确")
        }
        if (lastTimestamp.get() == timestamp) {
            sequence.set((sequence.incrementAndGet() and sequenceMask))
            if (sequence.get() == 0L) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp.get())
            }

        } else {
            sequence.set(0)
        }
        lastTimestamp.set(timestamp)
        return timestamp - sp shl timestampLeftShift.toInt() or
                (datacenterId shl datacenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or sequence.get()
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
                (s[2].toLong() shl 8) + s[3].toLong())
    }

    private fun getWorkId(deployId: String): Long {
        return (deployId.hashCode() and Int.MAX_VALUE).toLong() % 31
    }
}
