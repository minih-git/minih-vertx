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
import cn.minih.database.mysql.page.Page
import com.google.common.base.CaseFormat
import io.vertx.core.Future
import io.vertx.core.Promise
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
            .setShared(true).setMaxSize(config.pollSize)
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
                .onFailure {
                    printWarningLog(sql, tuple, it.message)
                    Future.succeededFuture(null)
                }.onComplete { conn.close() }
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
        return list(sql, tuple)
    }

    inline fun <reified T : Any> page(page: Page<T>, wrapper: Wrapper<T>): Future<Page<T>> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = generateQuerySql(wrapper).plus("limit ${page.pageSize}")
        val future: Promise<Page<T>> = Promise.promise()
        list<T>(sql, tuple).onComplete { page.data = it.result();future.complete(page) }
        return future.future()
    }

    inline fun <reified T : Any> list(sql: String, tuple: Tuple): Future<List<T>> {
        val future: Promise<List<T>> = Promise.promise()
        getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .onComplete { rowSet ->
                    val resultRaw = rowSet.result()
                    if (resultRaw.size() == 0) {
                        printLog(sql, tuple, listOf<T>())
                        future.complete(listOf())
                    } else {
                        val result: List<T> = resultRaw.map { covert(it) }
                        printLog(sql, tuple, result)
                        future.complete(result)
                    }
                }.onFailure {
                    printWarningLog(sql, tuple, it.message)
                    future.complete(listOf())
                }.onComplete { conn.close() }
        }
        return future.future()
    }

    inline fun <reified T : Any> insert(entity: T): Future<T> {
        val future: Promise<T> = Promise.promise()
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
        getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .onComplete {
                    printLog(sql, tuple, true)
                    future.complete(entity)
                }
                .onFailure {
                    printWarningLog(sql, tuple, it.message)
                    future.complete(null)
                }.onComplete { conn.close() }
        }
        return future.future()
    }

    inline fun <reified T : Any> insertBatch(entities: List<T>): Future<Boolean> {
        val future: Promise<Boolean> = Promise.promise()
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

        getPool().connection.compose { conn ->
            conn.preparedQuery(sql).executeBatch(batchTuple)
                .onComplete {
                    printLog(sql, batchTuple, true)
                    future.complete(true)
                }
                .onFailure {
                    printWarningLog(sql, batchTuple, it.message)
                    future.complete(false)
                }.onComplete { conn.close() }
        }
        return future.future()
    }

    inline fun <reified T : Any> update(wrapper: Wrapper<T>): Future<Boolean> {
        val future: Promise<Boolean> = Promise.promise()
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
        getPool().connection.compose { conn ->
            conn.preparedQuery(generateUpdateSql<T>(wrapper))
                .execute(tuple)
                .onComplete {
                    printLog(sql, tuple, true)
                    future.complete(true)
                }
                .onFailure {
                    printWarningLog(sql, tuple, it.message)
                    future.complete(false)
                }.onComplete { conn.close() }
        }
        return future.future()
    }

    inline fun <reified T : Any> update(entity: T): Future<Boolean> {
        val tuple = Tuple.tuple()
        val params = entity::class.primaryConstructor?.parameters
        val updateWrapper = UpdateWrapper<T>()
        params?.let {
            val primaryKey = it.first { p -> p.hasAnnotation<TableId>() }
            Assert.notNull(primaryKey) { MinihArgumentErrorException("未找到主键！") }
            val fields = T::class.memberProperties
            var primaryValue: Any? = null
            fields.forEach { field ->
                if (field is KMutableProperty1 && field.findAnnotation<TableId>() == null) {
                    var value = field.get(entity)
                    if (value is List<*>) {
                        value = value.joinToString(",")
                    }
                    updateWrapper.set(field.name, value)
                    tuple.addValue(value)
                }
                if (field.name == primaryKey.name) {
                    primaryValue = field.get(entity)!!
                }
            }
            tuple.addValue(primaryValue)
            Assert.notNull(primaryValue) { MinihArgumentErrorException("未找到主键数据！") }
            updateWrapper.eq(primaryKey.name!!, primaryValue!!)
        }
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
        val sql = """
                update $tableName  set 
            """.trimIndent().plus(wrapper.updateItems.joinToString(", ") {
            "${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} = ? "
        })
        return sql.plus("  ${generateConditionSql(wrapper)}")
    }

    inline fun <reified T : Any> generateConditionSql(wrapper: Wrapper<T>): String {
        var sql = ""
        wrapper.condition.forEach {
            sql = sql.plus(" and  ${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.key)} ").plus(
                when (it.type) {
                    QueryConditionType.EQ -> " = ?"
                    QueryConditionType.IN -> {
                        val perch = when {
                            it.value.isEmpty() -> "?"
                            else -> it.value.joinToString(",") { _ -> "?" }
                        }
                        " in (${perch})"
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
        val sql = """
                insert into $tableName (
            """.trimIndent()
            .plus(fields.joinToString(", ") { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it.name) })
            .plus(") values (")
            .plus(fields.filter { it is KMutableProperty1<*, *> }.joinToString(", ") { "?" })
        return sql.plus(")")

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
        log.info("====>sql:${sql}")
        log.info("====>参数:${tuple.deepToString()}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.info("====>结果:${result}")
    }

    fun printLog(sql: String, tuples: List<Tuple>, resultRaw: Any?) {
        log.info("====>sql:${sql}")
        log.info("====>参数:${tuples.map { it.deepToString() }}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.info("====>结果:${result}")
    }

    fun printWarningLog(sql: String, tuple: Tuple, resultRaw: Any?) {
        log.warn("====>sql:${sql}")
        log.warn("====>参数:${tuple.deepToString()}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.warn("====>结果:${result}")
    }

    fun printWarningLog(sql: String, tuples: List<Tuple>, resultRaw: Any?) {
        log.warn("====>sql:${sql}")
        log.warn("====>参数:${tuples.map { it.deepToString() }}")
        var result = resultRaw
        if (resultRaw is List<*>) {
            result = resultRaw.map { it?.toJsonObject() }
        }
        log.warn("====>结果:${result}")
    }


}