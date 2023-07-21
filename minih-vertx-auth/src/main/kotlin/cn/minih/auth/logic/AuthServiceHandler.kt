package cn.minih.auth.logic

import cn.hutool.core.text.AntPathMatcher
import cn.hutool.core.util.URLUtil
import cn.minih.auth.annotation.AuthCheckRole
import cn.minih.auth.annotation.CheckRoleType
import cn.minih.auth.constants.CONTEXT_LOGIN_ID
import cn.minih.auth.constants.MinihAuthErrorCode
import cn.minih.auth.constants.TOKEN_CONNECTOR_CHAT
import cn.minih.auth.data.TokenInfo
import cn.minih.auth.exception.AuthLoginException
import cn.minih.auth.exception.MinihAuthException
import cn.minih.auth.utils.getRequestBody
import cn.minih.core.exception.MinihArgumentErrorException
import cn.minih.core.exception.MinihException
import cn.minih.core.utils.Assert
import cn.minih.core.utils.R
import cn.minih.core.utils.covertTo
import cn.minih.core.utils.toJsonObject
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.Cookie
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@OptIn(DelicateCoroutinesApi::class)
fun Route.coroutineJsonHandlerHasAuth(fn: KFunction<Any?>) {
    val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
    handler { ctx ->
        GlobalScope.launch(v) {
            try {
                authCheckRole(fn, ctx)
                val args = generateArgs(fn.parameters, ctx)
                val result = when (fn.parameters.size) {
                    0 -> if (fn.isSuspend) fn.callSuspend() else fn.call()
                    else -> if (fn.isSuspend) fn.callSuspend(*args) else fn.call(*args)
                }
                ctx.json(R.ok(result).toJsonObject())
            } catch (e: Exception) {
                if (e.cause is MinihException) {
                    ctx.fail(e.cause)
                } else {
                    ctx.fail(e)
                }
            }
        }
    }
}

private fun generateArgs(argsNeed: List<KParameter>, ctx: RoutingContext): Array<Any?> {
    val args = mutableListOf<Any?>()
    val params = getRequestBody(ctx)
    argsNeed.forEach { argsType ->
        var type = argsType.type
        if (type.isMarkedNullable) {
            type = type.classifier?.createType()!!
        }
        val isMarkedNullable = argsType.type.isMarkedNullable
        val param: Any? = if (AuthLogic.isBasicType(type)) {
            argsType.name?.let { name -> params[name] }
        } else {
            argsType.name?.let { params.covertTo(argsType.type) }
        }
        if (!isMarkedNullable && param == null) {
            throw MinihArgumentErrorException("参数：${argsType.name} 不能为空！")
        }
        args.add(param)
    }
    return args.toTypedArray()
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun handle(ctx: RoutingContext) {
        val v: CoroutineDispatcher = Vertx.currentContext().dispatcher()
        GlobalScope.launch(v) {
            try {
                val path = ctx.request().path()
                val config = AuthUtil.getConfig()
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
                ctx.fail(e)
            }
        }
    }

    private suspend fun login(ctx: RoutingContext) {
        val tokenInfo = login(getRequestBody(ctx))
        val config = AuthUtil.getConfig()
        val tokenValue = config.tokenPrefix.plus(TOKEN_CONNECTOR_CHAT).plus(tokenInfo.tokenValue)
        ctx.response().addCookie(Cookie.cookie(config.tokenName, URLUtil.encode(tokenValue)))
        ctx.response().putHeader(config.tokenName, tokenValue)
        ctx.put(CONTEXT_LOGIN_ID, tokenInfo.loginId)
        ctx.json(R.ok(tokenInfo).toJsonObject())
    }

    private suspend fun kickOut(ctx: RoutingContext) {
        checkLogin(ctx)
        val self: String = ctx.get(CONTEXT_LOGIN_ID)
        checkRole(self, listOf("role_1"))
        val request = getRequestBody(ctx)
        val beKick = request.getString("loginId")
        Assert.notBlank(beKick) { MinihAuthException("loginId 不能为空！") }
        Assert.isTrue(beKick != self) { MinihAuthException("不能踢自己下线！") }
        AuthLogic.kickOut(beKick)
        ctx.json(R.ok<String>().toJsonObject())
    }

    private suspend fun logout(ctx: RoutingContext) {
        checkLogin(ctx)
        val self: String = ctx.get(CONTEXT_LOGIN_ID)
        Assert.notBlank(self) { MinihAuthException("loginId 不能为空！") }
        AuthLogic.logout(self)
        ctx.json(R.ok<String>().toJsonObject())
    }

    private suspend fun login(params: JsonObject): TokenInfo {
        val loginModel = authService.login(params.map)
        val tokenInfo = AuthUtil.login(loginModel.id, loginConfig = loginModel)
        authService.setLoginRole(tokenInfo.loginId)
        return tokenInfo
    }

    private suspend fun checkLogin(ctx: RoutingContext) {
        var tokenValue: String? = ""
        val config = AuthUtil.getConfig()
        val request = ctx.request()
        if (AntPathMatcher().match("/ws/**", request.path()) || config.ignoreAuthUri.any { AntPathMatcher().match(it, request.path()) }) {
            return
        }
        if (tokenValue.isNullOrBlank() && config.isReadHeader) {
            tokenValue = request.getHeader(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadBody) {
            tokenValue = ctx.body()?.asJsonObject()?.getString(config.tokenName)
        }
        if (tokenValue.isNullOrBlank() && config.isReadParams) {
            tokenValue = request.getParam(config.tokenName)
        }
        val loginId = AuthUtil.checkLogin(tokenValue)
        Vertx.currentContext().put(CONTEXT_LOGIN_ID, loginId)
        ctx.put(CONTEXT_LOGIN_ID, loginId)
    }

    suspend fun checkRole(longId: String, needRoles: List<String>, and: Boolean = false) {
        if (needRoles.isEmpty() || longId.isBlank()) {
            return
        }
        val roleIds = authService.getLoginRole(longId)
        if (roleIds.isEmpty()) {
            throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
        }
        if (and) {
            if (!needRoles.all { roleIds.contains(it) }) {
                throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        } else {
            if (!roleIds.any { needRoles.contains(it) }) {
                throw AuthLoginException(errorCode = MinihAuthErrorCode.ERR_CODE_LOGIN_TOKEN_NO_AUTH)
            }
        }

    }
}
