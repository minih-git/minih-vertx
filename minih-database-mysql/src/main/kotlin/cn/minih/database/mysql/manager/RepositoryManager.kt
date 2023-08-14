package cn.minih.database.mysql.manager

import cn.minih.common.exception.MinihArgumentErrorException
import cn.minih.common.util.Assert
import cn.minih.common.util.log
import cn.minih.common.util.toJsonObject
import cn.minih.core.util.SnowFlakeContext
import cn.minih.database.mysql.annotation.TableId
import cn.minih.database.mysql.config.DbConfig
import cn.minih.database.mysql.enum.TableIdType
import cn.minih.database.mysql.operation.*
import cn.minih.database.mysql.page.Page
import cn.minih.database.mysql.page.PageType
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


    inline fun <reified T : Any> findOne(wrapper: Wrapper<T> = QueryWrapper()): Future<T?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = SqlBuilder.generateQuerySql(wrapper).plus("limit 1")
        return getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .compose { rowSet ->

                    if (rowSet.size() == 0) {
                        printLog(sql, tuple, null)
                        Future.succeededFuture<T>(null)
                    } else {
                        val data = covert<T>(rowSet.first())
                        printLog(sql, tuple, data)
                        Future.succeededFuture(data)
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
            val wrapper = QueryWrapper<T>()
            wrapper.eq(it, id)
            return findOne<T>(wrapper)
        }
        return Future.succeededFuture(null)
    }


    inline fun <reified T : Any> list(wrapper: Wrapper<T> = QueryWrapper()): Future<List<T>> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = SqlBuilder.generateQuerySql(wrapper)
        return list(sql, tuple)
    }

    inline fun <reified T : Any> count(wrapper: Wrapper<T> = QueryWrapper()): Future<Long> {
        val future: Promise<Long> = Promise.promise()
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = SqlBuilder.generateCountQuerySql(wrapper)
        getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .onComplete { rowSet ->
                    val resultRaw = rowSet.result()
                    var count = 0L
                    if (resultRaw != null && resultRaw.size() != 0) {
                        count = resultRaw.first().get(Long::class.java, "count")
                    }
                    printLog(sql, tuple, count)
                    future.complete(count)
                }
        }
        return future.future()
    }

    inline fun <reified T : Any> page(page: Page<T>, wrapper: Wrapper<T> = QueryWrapper()): Future<Page<T>> {
        Assert.isTrue(page.nextCursor >= 0) {
            MinihArgumentErrorException("分页游标应该大于0！")
        }
        val tuple = Tuple.tuple()
        if (page.pageType == PageType.CURSOR) {
            wrapper.condition.add(
                QueryCondition(
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, page.cursorName),
                    listOf(page.nextCursor),
                    QueryConditionType.GT
                )
            )
        }
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }

        val sql = SqlBuilder.generateQuerySql(wrapper)
            .plus(
                when (page.pageType) {
                    PageType.CURSOR -> "limit ${page.pageSize}"
                    PageType.OFFSET -> "limit ${(page.nextCursor - 1) * page.pageSize} ${page.pageSize}"
                }
            )


        val future: Promise<Page<T>> = Promise.promise()
        list<T>(sql, tuple).onComplete {
            val data = it.result()
            if (data.isEmpty() || data.size < page.pageSize) {
                page.nextCursor = -1
            } else {

                page.nextCursor = when (page.pageType) {
                    PageType.CURSOR -> getNextCursorByFieldName(page.cursorName, data.last())
                    PageType.OFFSET -> page.nextCursor + 1
                }
            }
            page.data = it.result();future.complete(page)
        }
        return future.future()
    }


    inline fun <reified T : Any> list(sql: String, tuple: Tuple): Future<List<T>> {
        val future: Promise<List<T>> = Promise.promise()
        getPool().connection.compose { conn ->
            conn.preparedQuery(sql).execute(tuple)
                .onComplete { rowSet ->
                    val resultRaw = rowSet.result()
                    if (resultRaw == null || resultRaw.size() == 0) {
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
        val sql = SqlBuilder.generateInsertSql<T>(entity)
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
        val sql = SqlBuilder.generateInsertSql<T>(entities.first())

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
        val sql = SqlBuilder.generateUpdateSql<T>(wrapper)
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        getPool().connection.compose { conn ->
            conn.preparedQuery(sql)
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
                    val id = filed.getter.call(entity)
                    if ((id == null || id == 0L || id == 0) && tableId.value != TableIdType.AUTO_INCREMENT) {
                        if (filed is KMutableProperty1<*, *>) {
                            filed.setter.call(
                                entity, when (tableId.value) {
                                    TableIdType.INPUT -> System.currentTimeMillis()
                                    TableIdType.SNOWFLAKE -> SnowFlakeContext.instance.currentContext()
                                        .nextId(tableId.sfBusId)

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

    fun getNextCursorByFieldName(name: String, data: Any): Long {
        val clazz = data::class
        val field = clazz.memberProperties.first { it.name == name }
        return when (val value = field.getter.call(data)) {
            null -> 0
            is Long -> value
            else -> value.hashCode().toLong()
        }
    }

}
