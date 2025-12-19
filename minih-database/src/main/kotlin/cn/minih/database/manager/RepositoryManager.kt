package cn.minih.database.manager

import cn.minih.common.exception.MinihArgumentErrorException
import cn.minih.common.util.Assert
import cn.minih.common.util.log
import cn.minih.database.annotation.CursorKey
import cn.minih.database.annotation.LogicKey
import cn.minih.database.annotation.TableId
import cn.minih.database.config.DbConfig
import cn.minih.database.enums.DataStateType
import cn.minih.database.enums.OrderByType
import cn.minih.database.operation.QueryWrapper
import cn.minih.database.operation.SqlBuilder
import cn.minih.database.operation.UpdateWrapper
import cn.minih.database.operation.Wrapper
import cn.minih.database.page.Page
import cn.minih.database.page.PageType
import com.google.common.base.CaseFormat
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.mysqlclient.MySQLBuilder
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
@Suppress("unused")
object RepositoryManager {

    private lateinit var client: SqlClient

    fun initDb(vertx: Vertx, config: DbConfig) {
        when (config.type) {
            "mysql" -> {
                val connectOptions = MySQLConnectOptions()
                    .setPort(config.port)
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
                client = MySQLBuilder
                    .client()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build()
            }

            "pg" -> {
                val connectOptions = PgConnectOptions()
                    .setPort(config.port)
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
                client = PgBuilder
                    .client()
                    .with(poolOptions)
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build()
            }
        }
    }

    fun getClient(): SqlClient {
        return client
    }

    inline fun <reified T : Any> findOne(wrapper: Wrapper<T> = QueryWrapper()): Future<T?> {
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = SqlBuilder.generateQuerySql(wrapper).plus("limit 1")

        return getClient().preparedQuery(sql).execute(tuple)
            .compose { rowSet ->

                if (rowSet.size() == 0) {
                    printLog(sql, tuple, null)
                    Future.succeededFuture<T>(null)
                } else {
                    val data = covert<T>(rowSet.first(), wrapper)
                    printLog(sql, tuple, data)
                    Future.succeededFuture(data)
                }
            }
            .onFailure {
                printWarningLog(sql, tuple, it.message)
                Future.succeededFuture(null)
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
        wrapper.condition.forEach {
            it.value.forEach { v ->
                if (v is List<*>) {
                    v.forEach { v1 ->
                        tuple.addValue(v1)
                    }
                } else {
                    tuple.addValue(v)
                }
            }
        }
        val sql = SqlBuilder.generateQuerySql(wrapper)
        return list(sql, tuple, wrapper)
    }

    inline fun <reified T : Any> count(wrapper: Wrapper<T> = QueryWrapper()): Future<Long> {
        val future: Promise<Long> = Promise.promise()
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = SqlBuilder.generateCountQuerySql(wrapper)
        getClient().preparedQuery(sql).execute(tuple)
            .onComplete { rowSet ->
                val resultRaw = rowSet.result()
                var count = 0L
                if (resultRaw != null && resultRaw.size() != 0) {
                    count = resultRaw.firstOrNull()?.getLong("count") ?: 0L
                }
                printLog(sql, tuple, count)
                future.complete(count)
            }.onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(0)
            }
        return future.future()
    }

    inline fun <reified T : Any> covertPageSql(
        sql: String,
        page: Page<T>,
        wrapper: Wrapper<T> = QueryWrapper()
    ): String {
        if (page.pageType == PageType.CURSOR) {
            if (wrapper.orderByItems.isEmpty()) {
                val cursorKey = getFieldByAnnotation<CursorKey>(T::class) ?: getFieldByAnnotation<TableId>(T::class)
                val cursorAnno = getFieldAnnotation<CursorKey>(T::class)
                Assert.notNull(cursorKey, "未找到主键字段或分页排序cursor字段，请用@Cursor标记分页排序字段！")
                val cursorKeyName = cursorKey!!.name!!
                if (wrapper.selectItems.size > 0) {
                    val cursorField1 = wrapper.selectItems.firstOrNull { p -> p == cursorKeyName }
                    Assert.notNull(cursorField1, "结果字段中（${cursorKeyName}）未找到!")
                }
                val cursorName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, cursorKeyName)
                val orderType = cursorAnno?.type?.name ?: ""
                if (page.nextCursor == 0L || page.nextCursor == -1L) {
                    return sql.plus(" order by  $cursorName $orderType").plus(" limit ${page.pageSize}")
                }
                return sql.plus((if (sql.contains(" where ")) " and " else " where "))
                    .plus(" $cursorName ")
                    .plus(
                        if (cursorAnno == null || cursorAnno.type == OrderByType.ASC) " > ? " else " < ? "
                    )
                    .plus(" order by  $cursorName $orderType").plus(" limit ${page.pageSize}")
            }
            log.debug("复杂sql分页查询，自动转换为offset模式！")
            page.pageType = PageType.OFFSET
        }
        val pageStart = if (page.nextCursor < 1) 0 else page.nextCursor
        return "select * from ( ".plus(sql).plus(" ) page ")
            .plus(" limit ${pageStart * page.pageSize},${page.pageSize}")
    }

    inline fun <reified T : Any> page(page: Page<T>, wrapper: Wrapper<T> = QueryWrapper()): Future<Page<T>> {
        Assert.isTrue(page.nextCursor >= 0) {
            MinihArgumentErrorException("分页游标应该大于0！")
        }
        val tuple = Tuple.tuple()
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        val sql = covertPageSql(SqlBuilder.generateQuerySql(wrapper), page, wrapper)
        if (page.pageType == PageType.CURSOR) {
            if (page.nextCursor != 0L && page.nextCursor != -1L) {
                tuple.addValue(page.nextCursor)
            }
        }
        val future: Promise<Page<T>> = Promise.promise()
        list<T>(sql, tuple, wrapper).onComplete {
            val data = it.result()
            if (data.isEmpty() || data.size < page.pageSize) {
                page.nextCursor = -1
            } else {
                val cursorKey = getFieldByAnnotation<CursorKey>(T::class) ?: getFieldByAnnotation<TableId>(T::class)
                Assert.notNull(cursorKey, "未找到主键字段或分页排序cursor字段，请用@Cursor标记分页排序字段！")
                val cursorKeyName = cursorKey!!.name!!
                page.nextCursor = when (page.pageType) {
                    PageType.CURSOR -> getNextCursorByFieldName(cursorKeyName, data.last())
                    PageType.OFFSET -> page.nextCursor + 1
                }
            }
            page.data = it.result();future.complete(page)
        }
        return future.future()
    }

    inline fun <reified T : Any> list(sql: String, tuple: Tuple, wrapper: Wrapper<T>): Future<List<T>> {
        val future: Promise<List<T>> = Promise.promise()
        getClient().preparedQuery(sql).execute(tuple)
            .onComplete { rowSet ->
                val resultRaw = rowSet.result()
                if (resultRaw == null || resultRaw.size() == 0) {
                    printLog(sql, tuple, listOf<T>())
                    future.complete(listOf())
                } else {
                    val result: List<T> = resultRaw.map {
                        covert(it, wrapper)
                    }
                    printLog(sql, tuple, result)
                    future.complete(result)
                }
            }.onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(listOf())
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
                if (value is String) {
                    if (value.isBlank()) {
                        value = null
                    }
                }
                tuple.addValue(value)
            }
        }
        val sql = SqlBuilder.generateInsertSql<T>(entity)
        getClient().preparedQuery(sql).execute(tuple)
            .onComplete {
                printLog(sql, tuple, true)
                future.complete(entity)
            }
            .onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(null)
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
                    if (value is String) {
                        if (value.isBlank()) {
                            value = null
                        }
                    }
                    tuple.addValue(value)
                }
            }
            tuple
        }
        val sql = SqlBuilder.generateInsertSql<T>(entities.first())

        getClient().preparedQuery(sql).executeBatch(batchTuple)
            .onComplete {
                printLog(sql, batchTuple, true)
                future.complete(true)
            }
            .onFailure {
                printWarningLog(sql, batchTuple, it.message)
                future.complete(false)
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
            if (value is String) {
                if (value.isBlank()) {
                    value = null
                }
            }
            tuple.addValue(value)
        }
        val sql = SqlBuilder.generateUpdateSql<T>(wrapper)
        wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
        getClient().preparedQuery(sql)
            .execute(tuple)
            .onComplete {
                printLog(sql, tuple, true)
                future.complete(true)
            }
            .onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(false)
            }
        return future.future()
    }

    inline fun <reified T : Any> update(entity: T): Future<Boolean> {
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
                    if (value is String) {
                        if (value.isBlank()) {
                            value = null
                        }
                    }
                    updateWrapper.set(field.name, value)
                }
                if (field.name == primaryKey.name) {
                    primaryValue = field.get(entity)!!
                }
            }
            Assert.notNull(primaryValue) { MinihArgumentErrorException("未找到主键数据！") }
            updateWrapper.eq(primaryKey.name!!, primaryValue!!)
        }
        return update(updateWrapper)
    }

    inline fun <reified T : Any> delete(wrapper: Wrapper<T>): Future<Boolean> {
        val tuple = Tuple.tuple()
        val params = T::class.primaryConstructor?.parameters
        Assert.notNull(params, "未找到数据类字段！")
        val updateWrapper = UpdateWrapper<T>()
        val fields = T::class.memberProperties
        val logicKey = params!!.firstOrNull { p -> p.hasAnnotation<LogicKey>() }
        return if (logicKey == null) {
            val future: Promise<Boolean> = Promise.promise()
            val sql = SqlBuilder.generateDeleteSql<T>(wrapper)
            wrapper.condition.forEach { it.value.forEach { v -> tuple.addValue(v) } }
            getClient().preparedQuery(sql)
                .execute(tuple)
                .onComplete {
                    printLog(sql, tuple, true)
                    future.complete(true)
                }
                .onFailure {
                    printWarningLog(sql, tuple, it.message)
                    future.complete(false)
                }
            future.future()
        } else {
            val logicKeyType = logicKey.type.classifier as KClass<*>
            Assert.isTrue(logicKeyType == DataStateType::class, "logic字段的类型必须是DataStateType")
            val logicKeyField = fields.find { field -> field.name == logicKey.name }
            updateWrapper.set(logicKeyField!!.name, DataStateType.N)
            updateWrapper.condition.addAll(wrapper.condition)
            update(updateWrapper)
        }
    }

    inline fun <reified T : Any> delete(entity: T): Future<Boolean> {
        val params = entity::class.primaryConstructor?.parameters
        Assert.notNull(params, "未找到数据类字段！")
        val wrapper = QueryWrapper<T>()
        val fields = T::class.memberProperties
        val primaryKey = params!!.first { p -> p.hasAnnotation<TableId>() }
        Assert.notNull(primaryKey) { MinihArgumentErrorException("未找到主键！") }
        val primaryField = fields.first { field -> field.name == primaryKey.name }
        Assert.notNull(primaryField) { MinihArgumentErrorException("未找到主键字段！") }
        val primaryValue = primaryField.getter.call(entity)
        Assert.notNull(primaryValue) { MinihArgumentErrorException("未找到主键数据！") }
        wrapper.eq(primaryField.name, primaryValue!!)
        return delete(wrapper)
    }

    inline fun <reified T : Any> execSqlAndResult(sql: String, tuple: Tuple = Tuple.tuple()): Future<RowSet<Row>> {
        val future: Promise<RowSet<Row>> = Promise.promise()
        getClient().preparedQuery(sql)
            .execute(tuple)
            .onComplete {
                printLog(sql, tuple, true)
                future.complete(it.result())
            }
            .onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(null)
            }
        return future.future()
    }

    inline fun <reified T : Any> listOfSql(sql: String, tuple: Tuple = Tuple.tuple()): Future<List<T>> {
        val future: Promise<List<T>> = Promise.promise()
        getClient().preparedQuery(sql)
            .execute(tuple)
            .onComplete {
                val resultRaw = it.result()
                if (resultRaw == null || resultRaw.size() == 0) {
                    printLog(sql, tuple, listOf<T>())
                    future.complete(listOf())
                } else {
                    val result: List<T> = resultRaw.map { row ->
                        covert<T>(row)
                    }
                    printLog(sql, tuple, result)
                    future.complete(result)
                }
            }
            .onFailure {
                printWarningLog(sql, tuple, it.message)
                future.complete(null)
            }
        return future.future()
    }


}