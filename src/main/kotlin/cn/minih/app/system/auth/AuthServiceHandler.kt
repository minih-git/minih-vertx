package cn.minih.app.system.auth

import cn.hutool.core.text.AntPathMatcher
import cn.hutool.core.util.URLUtil
import cn.minih.app.system.auth.annotation.AuthCheckRole
import cn.minih.app.system.auth.annotation.AuthCheckRoleAnd
import cn.minih.app.system.auth.annotation.AuthCheckRoleOr
import cn.minih.app.system.auth.data.TokenInfo
import cn.minih.app.system.constants.CONTEXT_LOGIN_ID
import cn.minih.app.system.constants.DEVICE_KEY
import cn.minih.app.system.constants.MinihErrorCode
import cn.minih.app.system.constants.TOKEN_CONNECTOR_CHAT
import cn.minih.app.system.exception.AuthLoginException
import cn.minih.app.system.utils.R
import cn.minih.app.system.utils.toJsonObject
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.Cookie
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineJsonHandlerHasAuth(fn: KFunction<Any>) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                val body = ctx.body().asJsonObject()
                if (fn.hasAnnotation<AuthCheckRole>()) {
                    val annotations = fn.findAnnotation<AuthCheckRole>()
                    AuthServiceHandler.instance.checkRole(ctx.get(CONTEXT_LOGIN_ID), listOf(annotations!!.value), true)
                }
                if (fn.hasAnnotation<AuthCheckRoleAnd>()) {
                    val annotations = fn.findAnnotation<AuthCheckRoleAnd>()
                    AuthServiceHandler.instance.checkRole(ctx.get(CONTEXT_LOGIN_ID),annotations!!.value.toList(), true)
                }
                if (fn.hasAnnotation<AuthCheckRoleOr>()) {
                    val annotations = fn.findAnnotation<AuthCheckRoleOr>()
                    AuthServiceHandler.instance.checkRole(ctx.get(CONTEXT_LOGIN_ID), annotations!!.value.toList(), false)
                }
                ctx.json(R.ok(fn.call(body)).toJsonObject())
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
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
        this.authService = DefaultAuthServiceImpl.instance
    }

    fun setAuthService(authService: AuthService) {
        this.authService = authService
    }

    fun getAuthService(): AuthService {
        return this.authService
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun handle(ctx: RoutingContext) {
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        GlobalScope.launch(v) {
            try {
                val request = ctx.request()
                val path = request.path()
                if (path == AuthUtil.getConfig().loginPath) {
                    val body = ctx.body().asJsonObject()
                    val tokenInfo = login(body)
                    val config = AuthUtil.getConfig()
                    val tokenValue = config.tokenPrefix.plus(TOKEN_CONNECTOR_CHAT).plus(tokenInfo.tokenValue)
                    ctx.response().addCookie(
                        Cookie.cookie(
                            config.tokenName,
                            URLUtil.encode(tokenValue)
                        )
                    )
                    ctx.response().putHeader(
                        config.tokenName,
                        tokenValue
                    )
                    ctx.json(R.ok(tokenInfo).toJsonObject())
                } else {
                    val loginId = checkLogin(ctx)
                    ctx.put(CONTEXT_LOGIN_ID, loginId)
                    ctx.next()
                }
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }

    private suspend fun login(params: JsonObject): TokenInfo {
        val loginId = authService.login(params.map)
        val tokenInfo = AuthUtil.login(loginId, params.getString(DEVICE_KEY))
        authService.setLoginRole(tokenInfo.loginId)
        return tokenInfo
    }

    private suspend fun checkLogin(ctx: RoutingContext): String {
        var tokenValue: String? = ""
        val config = AuthUtil.getConfig()
        val request = ctx.request()
        if (config.ignoreAuthUri.any { AntPathMatcher().match(it, request.path()) }) {
            return ""
        }
        if (tokenValue.isNullOrBlank() && config.isReadHeader) {
            tokenValue = request.getHeader(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadBody) {
            tokenValue = ctx.body().asJsonObject().getString(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadParams) {
            tokenValue = request.getParam(config.tokenName)
        }
        return AuthUtil.checkLogin(tokenValue)
    }

    suspend fun checkRole(longId: String, needRoles: List<String>, and: Boolean) {
        if (needRoles.isEmpty() || longId.isBlank()) {
            return
        }
        val roleIds = authService.getLoginRole(longId)
        if (roleIds.isEmpty()) {
            throw AuthLoginException(errorCode = MinihErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
        }
        if (and) {
            if (!roleIds.all { needRoles.contains(it) }) {
                throw AuthLoginException(errorCode = MinihErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        } else {
            if (!roleIds.any { needRoles.contains(it) }) {
                throw AuthLoginException(errorCode = MinihErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        }

    }
}