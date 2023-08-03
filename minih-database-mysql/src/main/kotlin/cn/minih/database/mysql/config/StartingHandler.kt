package cn.minih.database.mysql.config

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.core.utils.getConfig
import cn.minih.database.mysql.manager.RepositoryManager
import io.vertx.core.Vertx

/**
 *  启动注入数据库连接池
 * @author hubin
 * @since 2023-08-03 16:02:51
 */
@Component
class PostStartingProcess : PostStartingProcess {
    override suspend fun exec(vertx: Vertx) {
        RepositoryManager.initDb(vertx, getConfig("db", DbConfig::class))
    }
}
