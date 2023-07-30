package cn.minih.database.mysql.manager

import cn.minih.core.exception.MinihArgumentErrorException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.SnowFlake
import cn.minih.core.utils.log
import cn.minih.core.utils.toJsonObject
import cn.minih.database.mysql.annotation.TableId
import cn.minih.database.mysql.annotation.TableName
import cn.minih.database.mysql.config.DbConfig
import cn.minih.database.mysql.enum.TableIdType
import cn.minih.database.mysql.operation.QueryConditionType
import cn.minih.database.mysql.operation.QueryWrapper
import cn.minih.database.mysql.operation.UpdateWrapper
import cn.minih.database.mysql.operation.Wrapper
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
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
@Suppress("unused")
object RepositoryManager {


    private lateinit var pool: MySQLPool

    fun initDb(vertx: Vertx, config: DbConfig) {
        val connectOptions = MySQLConnectOptions()
            .setPort(3306)
            .setHost(config.host)
            .setDatabase(config.db)
            .setUser(config.user)
            .setPassword(config.password)
            .setIdleTimeout(240)
            .setReconnectAttempts(2)
            .setReconnectInterval(240)
        val poolOptions = PoolOptions()
            .setPoolCleanerPeriod(240)
            .setMaxLifetime(240)
            .setShared(true).setMaxSize(config.pollSize ?: 20)
        pool = MySQLPool.pool(vertx, connectOptions, poolOptions)
    }

    fun getPool(): MySQLPool {
        return this.pool
    }


    inline fun <reified T : Any> findOne(wrapper: Wrapper<T>): Future<T?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = generateQuerySql(wrapper)
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .compose { rowSet ->

                    if (rowSet.size() == 0) {
                        printLog(sql, tuple, null)
                        Future.succeededFuture<T>(null)
                    } else {
                        printLog(sql, tuple, rowSet.first())
                        Future.succeededFuture(covert(rowSet.first()))
                    }
                }
                .onFailure { log.warn(it.message) }.onComplete { conn.close() }
        }
    }

    inline fun <reified T : Any> findById(id: Any): Future<T?> {
        val params = T::class.primaryConstructor?.parameters
        var primaryKey: KParameter? = null
        params?.let {
            primaryKey = it.first { p -> p.hasAnnotation<TableId>() }
        }
        Assert.notNull(primaryKey) {
            throw MinihArgumentErrorException("请设置主键标志！")
        }
        primaryKey?.name?.let {
            val wrapper = QueryWrapper<T>().eq(it, id)
            return findOne<T>(wrapper)
        }
        return Future.succeededFuture(null)
    }


    inline fun <reified T : Any> list(wrapper: Wrapper<T>): Future<List<T>> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = generateQuerySql(wrapper)
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .compose { rowSet ->
                    if (rowSet.size() == 0) {
                        printLog(sql, tuple, listOf<T>())
                        Future.succeededFuture(listOf())
                    } else {
                        val result: List<T> = rowSet.map { covert(it) }
                        printLog(sql, tuple, result)
                        Future.succeededFuture(result)
                    }
                }.onFailure { log.warn(it.message) }.onComplete { conn.close() }
        }
    }

    inline fun <reified T : Any> insert(entity: T): Future<T> {
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
        val sql = generateInsertSql<T>(entity)
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .compose {
                    printLog(sql, tuple, true)
                    Future.succeededFuture(entity)
                }
                .onFailure { log.warn(it.message) }.onComplete { conn.close() }
        }
    }

    inline fun <reified T : Any> insertBatch(entities: List<T>): Future<Boolean> {
        val batchTuple = entities.map { entity ->
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
            tuple
        }
        val sql = generateInsertSql<T>(entities.first())

        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).executeBatch(batchTuple)
                .compose {
                    printLog(sql, batchTuple, true)
                    Future.succeededFuture(true)
                }
                .onFailure { log.warn(it.message) }.onComplete { conn.close() }
        }
    }

    inline fun <reified T : Any> update(wrapper: Wrapper<T>): Future<Boolean> {
        val tuple = Tuple.tuple()
        wrapper.updateItems.forEach {
            var value = it.value
            if (it.value is List<*>) {
                value = it.value.joinToString(",")
            }
            tuple.addValue(value)
        }
        val sql = generateUpdateSql<T>(wrapper)
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        return getPool().connection.compose { conn ->
            conn.preparedQuery(generateUpdateSql<T>(wrapper))
                .execute(tuple)
                .compose {
                    printLog(sql, tuple, true)
                    Future.succeededFuture(true)
                }
                .onFailure { log.warn(it.message) }.onComplete { conn.close() }
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
            if (it is KMutableProperty1 && it.findAnnotation<TableId>() == null) {
                var value = it.get(entity)
                if (value is List<*>) {
                    value = value.joinToString(",")
                }
                updateWrapper.set(it.name, value)
                tuple.addValue(value)
            }
        }
        tuple.addValue(primaryKey.get(entity))
        val pv = primaryKey.get(entity) ?: throw MinihArgumentErrorException("未找到主键数据！")
        updateWrapper.eq(primaryKey.name, pv)
        return update(updateWrapper)
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
        return sql.substring(0, sql.length - 1).plus("  ${generateConditionSql(wrapper)}")
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
        return sql.substring(0, sql.length - 1).plus(")")

    }


    inline fun <reified T : Any> covert(row: Row): T {
        val entity = T::class.createInstance()
        val fields = entity::class.memberProperties
        fields.forEach {
            if (it is KMutableProperty1) {
                var type = it.returnType.classifier as KClass<*>
                val fieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name)
                var stringToList = false
                if (type.simpleName === List::class.simpleName) {
                    stringToList = true
                    type = String::class
                }
                val valueRaw = row.get(type.javaObjectType, fieldName)

                valueRaw?.let { v ->
                    var value = v
                    if (stringToList) {
                        value = valueRaw.toString().split(",").toList()
                    }
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

    fun printLog(sql: String, tuple: Tuple, resultRaw: Any?) {
        log.debug("====>sql:${sql}")
        log.debug("====>参数:${tuple.deepToString()}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.debug("====>结果:${result}")
    }

    fun printLog(sql: String, tuples: List<Tuple>, resultRaw: Any?) {
        log.debug("====>sql:${sql}")
        log.debug("====>参数:${tuples.map { it.deepToString() }}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.debug("====>结果:${result}")
    }


}