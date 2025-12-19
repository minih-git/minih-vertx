package cn.minih.system

import cn.minih.core.annotation.ComponentScan
import cn.minih.core.boot.MinihBootServiceRun

import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject
@ComponentScan("cn.minih.system")
class SystemApplication

suspend fun main(args: Array<String>) {
    MinihBootServiceRun
        .setSystemConfig {
             ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(JsonObject().put("path", "conf.json"))
        }
        .run(SystemApplication::class, "-standalone",*args)
}