package cn.minih.core.repository

import cn.minih.core.annotation.TableId
import cn.minih.core.annotation.TableName
import cn.minih.core.annotation.enum.TableIdType
import cn.minih.core.config.DbConfig
import cn.minih.core.exception.MinihNotFoundException
import cn.minih.core.repository.conditions.QueryConditionType
import cn.minih.core.repository.conditions.UpdateWrapper
import cn.minih.core.repository.conditions.Wrapper
import cn.minih.core.utils.SnowFlake
import cn.minih.core.utils.log
import com.google.common.base.CaseFormat
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


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
            .setHost(config.host)
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


    inline fun <reified T : Any> findOne(wrapper: Wrapper<T>): Future<T?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = generateQuerySql(wrapper)
        log.info("sql: $sql")
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
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

    inline fun <reified T : Any> list(wrapper: Wrapper<T>): Future<List<T>?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = generateQuerySql(wrapper)
        log.info("sql: $sql")
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple).compose { rowSet ->
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

    inline fun <reified T : Any> insert(entity: T): Future<Boolean> {
        val tuple = Tuple.tuple()
        insertPrimaryKey(entity)
        val fields = T::class.memberProperties
        fields.forEach {
            if (it is KMutableProperty1) {
                var value = it.get(entity)
                if (value is List<*>) {
                    value = value.joinToString(",")
                }
                tuple.addValue(value)
            }
        }
        return getPool().connection.compose { conn ->
            conn.query(generateInsertSql<T>(entity)).execute()
                .compose { Future.succeededFuture(true) }
                .onComplete { conn.close() }
        }
    }

    inline fun <reified T : Any> update(wrapper: Wrapper<T>) {
        val tuple = Tuple.tuple()
        wrapper.updateItems.forEach {
            var value = it.value
            if (it.value is List<*>) {
                value = it.value.joinToString(",")
            }
            tuple.addValue(value)
        }
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        getPool().connection.compose { conn ->
            conn.query(generateQuerySql<T>(wrapper)).execute().onComplete {
                conn.close()
            }
        }
    }

    inline fun <reified T : Any> update(entity: T): Future<Boolean> {
        val tuple = Tuple.tuple()
        val fields = T::class.memberProperties
        var primaryKey: KProperty1<T, *> = fields.first()
        val updateWrapper = UpdateWrapper<T>()
        fields.forEach {
            it.findAnnotation<TableId>()?.let { _ ->
                primaryKey = it
            }
            if (it is KMutableProperty1) {
                var value = it.get(entity)
                if (value is List<*>) {
                    value = value.joinToString(",")
                }
                updateWrapper.set(it.name, "1")
                tuple.addValue(value)
            }
        }
        tuple.addValue(primaryKey.get(entity))
        return getPool().connection.compose { conn ->
            conn.query(generateUpdateSql<T>(updateWrapper)).execute()
                .compose { Future.succeededFuture(true) }
                .onComplete {
                    conn.close()
                }
        }
    }


    inline fun <reified T : Any> generateQuerySql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        return """
                select * from $tableName  ${generateConditionSql(wrapper)}
            """.trimIndent()
    }


    inline fun <reified T : Any> generateUpdateSql(wrapper: Wrapper<T>): String {
        var tableName = T::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        var sql = """
                update $tableName  set 
            """.trimIndent()
        wrapper.updateItems.forEach {
            sql = sql.plus(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)).plus(" = ?,")
        }
        return sql.substring(0, sql.length - 1)
    }

    inline fun <reified T : Any> generateConditionSql(wrapper: Wrapper<T>): String {
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
        var tableName = entity::class.findAnnotation<TableName>()?.value
        if (tableName.isNullOrBlank()) {
            tableName = T::class.simpleName?.let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
        }
        val fields = T::class.memberProperties
        var sql = """
                insert into $tableName (
            """.trimIndent()
        fields.forEach {
            sql = sql.plus("${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name)},")
        }
        sql = sql.substring(0, sql.length - 1).plus(") values (")
        fields.forEach {
            if (it is KMutableProperty1) {
                sql = sql.plus("?,")
            }
        }
        return sql.substring(0, sql.length - 1)
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

    fun <T : Any> insertPrimaryKey(entity: T) {
        val params = entity::class.primaryConstructor?.parameters
        params?.let {
            val primaryKey = it.first { p -> p.hasAnnotation<TableId>() }
            val tableId = primaryKey.findAnnotation<TableId>() ?: return
            val fields = entity::class.memberProperties
            fields.forEach { filed ->
                if (filed.name == primaryKey.name) {
                    if (filed.getter.call(entity) == null && tableId.value != TableIdType.AUTO_INCREMENT) {
                        if (filed is KMutableProperty1<*, *>) {
                            filed.setter.call(
                                entity, when (tableId.value) {
                                    TableIdType.INPUT -> System.currentTimeMillis()
                                    TableIdType.SNOWFLAKE -> SnowFlake.nextId()
                                    TableIdType.UUID -> UUID.randomUUID().toString()
                                    else -> ""
                                }
                            )
                        }
                        return
                    }
                }
            }


        }


    }



}