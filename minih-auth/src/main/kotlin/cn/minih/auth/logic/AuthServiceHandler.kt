@file:Suppress("unused")

package cn.minih.auth.logic

import cn.hutool.core.text.AntPathMatcher
import cn.hutool.core.util.URLUtil
import cn.minih.auth.config.AuthConfig
import cn.minih.auth.config.LockType
import cn.minih.auth.constants.*
import cn.minih.auth.data.TokenInfo
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.exception.MinihAuthException
import cn.minih.auth.service.AbstractAuthService
import cn.minih.auth.service.AuthService
import cn.minih.auth.utils.*
import cn.minih.common.util.*
import cn.minih.core.annotation.AuthCheckRole
import cn.minih.core.annotation.CheckRoleType
import cn.minih.core.beans.BeanFactory
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER
import cn.minih.core.config.MICROSERVICE_INNER_REQUEST_HEADER_VALUE
import cn.minih.web.response.R
import cn.minih.web.service.FileUpload
import cn.minih.web.service.Service
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.Cookie
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

fun getBeanCall(params: List<KParameter>): Any? {
    if (params.isNotEmpty()) {
        val p1 = params.first()
        val clazz = p1.type.classifier as KClass<*>
        val superClasses = getSuperClassRecursion(clazz)
        if (superClasses.contains(Service::class)) {
            return BeanFactory.instance.getBeanFromType(clazz.createType())
        }
    }
    return null
}


fun Route.coroutineJsonHandlerHasAuth(fn: KFunction<Any?>) {
    handler { ctx ->
        val dispatcher: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        CoroutineScope(dispatcher).launch {
            try {
                authCheckRole(fn, ctx)
                val bean: Any? = getBeanCall(fn.parameters)
                val parameters = bean?.let {
                    fn.parameters.subList(1, fn.parameters.size)
                } ?: fn.parameters
                val bodyParameters =
                    parameters.filter { it.type != RoutingContext::class.createType() && it.type != HttpServerRequest::class.createType() }
                val args = generateArgs(bodyParameters, getRequestBody(ctx))
                val realArgs = mutableMapOf<KParameter, Any?>()
                parameters.forEach {
                    if (it.type == RoutingContext::class.createType()) {
                        realArgs[it] = ctx
                        return@forEach
                    }
                    if (it.type == HttpServerRequest::class.createType()) {
                        realArgs[it] = ctx.request()
                        return@forEach
                    }
                    if (args.isNotEmpty()) {
                        val v = args.first { a -> a.first == it }.second
                        if (it.isOptional && v == null) {
                            return@forEach
                        }
                        realArgs[it] = v
                    }
                }
                bean?.let {
                    realArgs.put(fn.parameters.first(), it)
                }
                val rawResult = when {
                    realArgs.isEmpty() -> if (fn.isSuspend) fn.callSuspend() else fn.call()
                    else -> if (fn.isSuspend) fn.callSuspendBy(realArgs) else fn.callBy(realArgs)
                }

                val config = getConfig("auth", AuthConfig::class)
                if (config.encryptData) {
                    val se = generateAesSecret()
                    val result = encrypt(rawResult!!.toJsonString(), se)
                    ctx.json(R.encryptOk(result).jsToJsonString())
                } else {
                    ctx.json(R.ok(rawResult).jsToJsonString())
                }
            } catch (e: Exception) {
                val config = getConfig("auth", AuthConfig::class)

                log.warn(
                    "接口调用出现错误:${getMinihException(e).message},${ctx.request().path()},${
                        ctx.request().getHeader(config.tokenName)
                    }"
                )
                ctx.fail(getMinihException(e))
            }
        }
    }
}


fun Route.coroutineFileUploadHandler(fn: KFunction<Any?>) {
    blockingHandler { ctx ->
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        CoroutineScope(v).launch {
            try {
                val bean: Any? = getBeanCall(fn.parameters)
                val files = ctx.fileUploads().map {
                    FileUpload(
                        it.name(),
                        it.uploadedFileName(),
                        it.fileName(),
                        it.size(),
                        it.contentType(),
                        it.contentTransferEncoding(),
                        it.charSet()
                    )
                }
                val result = when {
                    bean == null -> fn.callSuspend(files)
                    else -> fn.callSuspend(bean, files)
                }
                ctx.json(R.ok(result).jsToJsonString())
            } catch (e: Exception) {
                log.warn("接口调用出现错误:${e.message}")
                ctx.fail(getMinihException(e))
            }
        }
    }
}


private suspend fun authCheckRole(fn: KFunction<Any?>, ctx: RoutingContext) {
    if (fn.hasAnnotation<AuthCheckRole>()) {
        val annotations = fn.findAnnotation<AuthCheckRole>()
        AuthServiceHandler.instance.checkRole(
            ctx.get(CONTEXT_LOGIN_ID),
            annotations!!.value.toList(),
            annotations.type == CheckRoleType.AND
        )
    }
}

class DefaultAuthService : AbstractAuthService() {
    override suspend fun getUserRoles(loginId: String): List<String> {
        return emptyList()
    }

}

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class AuthServiceHandler private constructor() : Handler<RoutingContext> {
    private var authService: AuthService

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AuthServiceHandler()
        }
    }

    init {
        authService = DefaultAuthService()
    }

    fun setAuthService(authService: AuthService) {
        this.authService = authService
    }

    override fun handle(ctx: RoutingContext) {
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        CoroutineScope(v).launch {
            try {
                val path = ctx.request().path()
                val config = getConfig("auth", AuthConfig::class)
                when (path) {
                    config.loginPath -> login(ctx)
                    config.kickOutPath -> kickOut(ctx)
                    config.logoutPath -> logout(ctx)
                    else -> {
                        checkLogin(ctx)
                        ctx.next()
                    }
                }
            } catch (e: Exception) {
                log.warn("接口调用出现错误:${getMinihException(e).message}")
                ctx.fail(getMinihException(e))
            }
        }
    }

    private suspend fun login(ctx: RoutingContext) {
        val config = getConfig("auth", AuthConfig::class)
        val body = getRequestBody(ctx)
        try {
            val tokenInfo = login(body)
            val tokenValue = config.tokenPrefix.plus(TOKEN_CONNECTOR_CHAT).plus(tokenInfo.tokenValue)
            ctx.response().addCookie(Cookie.cookie(config.tokenName, URLUtil.encode(tokenValue)))
            ctx.response().putHeader(config.tokenName, tokenValue)
            ctx.put(CONTEXT_LOGIN_ID, tokenInfo.loginId)
            ctx.put(CONTEXT_LOGIN_TOKEN, tokenValue)
            ctx.put(CONTEXT_LOGIN_DEVICE, tokenInfo.loginDevice)
            ctx.json(R.ok(tokenInfo).toJsonObject())
        } catch (e: Exception) {
            var key = ""
            if (config.loginMaxTryLockType == LockType.ACCOUNT) {
                if (body.containsKey("username")) {
                    key = body.getString("username")
                }
                if (body.containsKey("mobile")) {
                    key = body.getString("mobile")
                }
            }
            if (config.loginMaxTryLockType == LockType.IP) {
                key = getRemoteIp(ctx.request())
            }
            if (key.isNotBlank()) {
                AuthLogic.checkLoginState(key)
            }

            throw e
        }

    }

    private suspend fun kickOut(ctx: RoutingContext) {
        try {
            checkLogin(ctx)
            val self: String = ctx.get(CONTEXT_LOGIN_ID)
            checkRole(self, listOf(CONTEXT_SYSTEM_ADMIN_ROLE_TAG))
            val request = getRequestBody(ctx)
            val beKick = request.getString("loginId")
            Assert.notBlank(beKick) { MinihAuthException("loginId 不能为空！") }
            Assert.isTrue(beKick != self) { MinihAuthException("不能踢自己下线！") }
            AuthLogic.kickOut(beKick)
            ctx.json(R.ok<String>().toJsonObject())
        } catch (e: Exception) {
            log.warn("接口调用出现错误:${getMinihException(e).message}")
            ctx.fail(getMinihException(e))
        }

    }

    private suspend fun logout(ctx: RoutingContext) {
        try {
            checkLogin(ctx)
            val self: String = ctx.get(CONTEXT_LOGIN_TOKEN)
            Assert.notBlank(self) { MinihAuthException("loginId 不能为空！") }
            AuthLogic.logout(self)
            ctx.json(R.ok<String>().toJsonObject())
        } catch (e: Exception) {
            log.warn("接口调用出现错误:${getMinihException(e).message}")
            ctx.fail(getMinihException(e))
        }
    }

    private suspend fun login(params: JsonObject): TokenInfo {
        val loginModel = authService.login(params.map)
        val tokenInfo = AuthUtil.login(loginModel.id, loginConfig = loginModel)
        authService.setLoginRole(tokenInfo.loginId)
        return tokenInfo
    }

    private suspend fun checkLogin(ctx: RoutingContext) {

        var tokenValue: String? = ""
        val config = getConfig("auth", AuthConfig::class)
        val request = ctx.request()
        if (tokenValue.isNullOrBlank() && config.isReadHeader) {
            tokenValue = request.getHeader(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadBody) {
            tokenValue = ctx.body()?.asJsonObject()?.getString(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadParams) {
            tokenValue = request.getParam(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() &&
            (AntPathMatcher().match("/ws/**", request.path()) ||
                    config.ignoreAuthUri.any { AntPathMatcher().match(it, request.path()) } ||
                    request.getHeader(MICROSERVICE_INNER_REQUEST_HEADER) == MICROSERVICE_INNER_REQUEST_HEADER_VALUE)
        ) {
            return
        }
        val loginId = AuthUtil.checkLogin(tokenValue)
        Vertx.currentContext().put(CONTEXT_LOGIN_ID, loginId)
        Vertx.currentContext().put(CONTEXT_LOGIN_TOKEN, tokenValue)
        val session = AuthLogic.getSessionByLoginId(loginId)
        session?.tokenSignList?.let {
            val realToken = tokenValue!!.substring(config.tokenPrefix.length + TOKEN_CONNECTOR_CHAT.length)
            val sign = it.firstOrNull { s -> s.token == realToken }
            sign?.let { s ->
                Vertx.currentContext().put(CONTEXT_LOGIN_DEVICE, s.device)
                ctx.put(CONTEXT_LOGIN_DEVICE, s.device)
            }
        }
        ctx.put(CONTEXT_LOGIN_ID, loginId)
        ctx.put(CONTEXT_LOGIN_TOKEN, tokenValue)
        val roleTags = authService.getLoginRole(loginId)
        val isSystemAdmin = roleTags.isNotEmpty() && roleTags.contains(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
        Vertx.currentContext().put(CONTEXT_IS_SYSTEM_ADMIN, isSystemAdmin)
        ctx.put(CONTEXT_IS_SYSTEM_ADMIN, isSystemAdmin)
    }

    suspend fun checkRole(loginId: String, needRoles: List<String>, and: Boolean = false) {
        if (needRoles.isEmpty() || loginId.isBlank()) {
            return
        }
        val roleTags = authService.getLoginRole(loginId)
        if (roleTags.isEmpty()) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
        }
        if (and) {
            if (!needRoles.all { roleTags.contains(it) }) {
                throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        } else {
            if (!roleTags.any { needRoles.contains(it) }) {
                throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        }

    }
}
