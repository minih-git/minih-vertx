package cn.minih.database.config

import cn.minih.common.util.getConfig
import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import cn.minih.database.manager.RepositoryManager
import io.vertx.core.Context

/**
 *  启动注入数据库连接池
 * @author hubin
 * @since 2023-08-03 16:02:51
 */
@Component
class PostStartingProcess : PostStartingProcess {
    override suspend fun exec(context: Context) {
        RepositoryManager.initDb(context.owner(), getConfig("db", DbConfig::class))
    }
}