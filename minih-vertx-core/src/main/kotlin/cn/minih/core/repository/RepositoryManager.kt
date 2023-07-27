package cn.minih.core.repository

import cn.minih.core.annotation.TableName
import cn.minih.core.config.DbConfig
import cn.minih.core.exception.MinihNotFoundException
import cn.minih.core.utils.log
import com.google.common.base.CaseFormat
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object RepositoryManager {


    private lateinit var pool: MySQLPool

    fun initDb(vertx: Vertx, config: DbConfig) {
        val connectOptions = MySQLConnectOptions()
            .setPort(3306)
            .setHost("124.71.143.78")
            .setDatabase(config.db)
            .setUser(config.user)
            .setPassword(config.password)
            .setPipeliningLimit(64)
        val poolOptions = PoolOptions().setMaxSize(config.pollSize ?: 8)
        pool = MySQLPool.pool(vertx, connectOptions, poolOptions)
    }

    fun getPool(): MySQLPool {
        return this.pool
    }


    inline fun <reified T : Any> findOne(wrapper: QueryWrapper<T>): Future<T?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        return getPool().connection.compose { conn ->
            conn.preparedQuery(generateQuerySql(wrapper)).execute(tuple)
                .compose { rowSet ->
                    log.info("查询完毕：数据条数：${rowSet.size()}")
                    if (rowSet.size() == 0) {
                        throw MinihNotFoundException()
                    } else {
                        Future.succeededFuture<T>(covert(rowSet.first()))
                    }
                }
                .onComplete {
                    conn.close()
                }
        }
    }


    inline fun <reified T : Any> list(wrapper: QueryWrapper<T>): Future<List<T>?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        return getPool().connection.compose { conn ->
            conn.preparedQuery(generateQuerySql(wrapper)).execute(tuple).compose { rowSet ->
                log.info("查询完毕：数据条数：${rowSet.size()}")
                if (rowSet.size() == 0) {
                    throw MinihNotFoundException()
                } else {
                    Future.succeededFuture<List<T>>(rowSet.map { covert(it) })
                }
            }.onComplete {
                conn.close()
            }
        }
    }


    inline fun <reified T : Any> insert(entity: T) {
        getPool().connection.compose { conn ->
            conn.query(generateInsertSql<T>(entity)).execute().onComplete {
                conn.close()
            }
        }
    }

    inline fun <reified T : Any> update(wrapper: QueryWrapper<T>, entity: T) {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }

        getPool().connection.compose { conn ->
            conn.query(generateQuerySql<T>(wrapper)).execute().onComplete {
                conn.close()
            }
        }
    }


    inline fun <reified T : Any> generateQuerySql(wrapper: QueryWrapper<T>): String {
        val tableName = T::class.findAnnotation<TableName>()?.value
        return """
                select * from $tableName  ${generateConditionSql(wrapper)}
            """.trimIndent()
    }

    inline fun <reified T : Any> generateUpdateSql(wrapper: QueryWrapper<T>, entity: T): String {
        val tableName = T::class.findAnnotation<TableName>()?.value
        var sql = """
                update $tableName  set 
            """.trimIndent()
        val fields = T::class.memberProperties
        fields.forEach {
            it.isAccessible = true

            sql = sql.plus(it.name).plus(" = ").plus("${it.get(entity)},")
        }

        return sql
    }

    inline fun <reified T : Any> generateConditionSql(wrapper: QueryWrapper<T>): String {
        var sql = ""
        wrapper.condition.forEach {
            sql = sql.plus(" and  ${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} ").plus(
                when (it.type) {
                    QueryConditionType.EQ -> " = ?"
                    QueryConditionType.IN -> {
                        var perch = ""
                        it.value.forEach { _ -> perch = perch.plus("?,") }
                        " in (${perch.substring(0, perch.length - 1)})"
                    }

                    QueryConditionType.BETWEEN -> " between ? an ?"
                    QueryConditionType.GT -> " > ? "
                    QueryConditionType.LT -> " < ? "
                    QueryConditionType.GTE -> " >= ? "
                    QueryConditionType.LTE -> " =< ? "
                }
            )
        }
        return sql.replaceFirst("and", "where")
    }

    inline fun <reified T : Any> generateInsertSql(entity: T): String {
        val tableName = entity::class.findAnnotation<TableName>()?.value
        val fields = T::class.memberProperties
        var sql = """
                insert into $tableName (
            """.trimIndent()
        fields.forEach {
            sql = sql.plus("${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name)},")
        }
        sql = sql.substring(0, sql.length - 1).plus(") values (")
        fields.forEach {
            it.isAccessible = true
            sql = sql.plus("${it.get(entity)},")
        }
        sql = sql.substring(0, sql.length - 1)
        return sql
    }


    inline fun <reified T : Any> covert(row: Row): T {
        val entity = T::class.createInstance()
        val fields = entity::class.memberProperties
        fields.forEach {
            if (it is KMutableProperty1) {
                var type = it.returnType.classifier as KClass<*>
                val fieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name)
                if (type.simpleName === List::class.simpleName) {
                    type = String::class
                }
                var valueRaw = row.get(type.javaObjectType, fieldName)
                if (it.returnType.classifier == Collection::class) {
                    valueRaw = valueRaw.toString().split(",").toList()
                }
                valueRaw?.let { value ->
                    it.setter.call(entity, value)
                }
            }
        }
        return entity
    }


}
