package cn.minih.app.system.config

import mu.KLogger
import mu.KotlinLogging

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Log {
    companion object {
        val log: KLogger inline get() = KotlinLogging.logger {}
    }
}