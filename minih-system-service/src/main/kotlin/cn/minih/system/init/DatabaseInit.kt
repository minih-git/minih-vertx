package cn.minih.system.init

import cn.minih.core.annotation.Component
import cn.minih.core.boot.PostStartingProcess
import io.vertx.core.Context
import io.vertx.core.logging.LoggerFactory
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.vertx.kotlin.coroutines.dispatcher

@Component
class DatabaseInit : PostStartingProcess {
    private val log = LoggerFactory.getLogger(DatabaseInit::class.java)

    override suspend fun exec(context: Context) {
        val config = context.config().getJsonObject("mysql")
        if (config == null) {
            log.warn("MySQL config not found, skipping DB init.")
            return
        }

        val connectOptions = MySQLConnectOptions()
            .setPort(config.getInteger("port"))
            .setHost(config.getString("host"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"))

        val poolOptions = PoolOptions().setMaxSize(5)
        val client = MySQLPool.pool(context.owner(), connectOptions, poolOptions)

        val sql = this::class.java.classLoader.getResource("init.sql")?.readText()
        if (sql == null) {
            log.warn("init.sql not found.")
            return
        }
        
        // Split by ; and execute each (Simplification)
        val statements = sql.split(";").filter { it.trim().isNotBlank() }

        statements.forEach { stmt ->
             try {
                // Not ideal for large scripts but okay for simple init
                 io.vertx.kotlin.coroutines.awaitResult<io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row>> { h ->
                    client.query(stmt).execute(h)
                }
             } catch (e: Exception) {
                 log.warn("Error executing SQL: $stmt", e)
             }
        }
        log.info("Database initialized.")
        client.close()
    }
}
