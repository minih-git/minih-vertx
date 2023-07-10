package cn.minih.app.system.auth

import cn.hutool.core.lang.Assert
import cn.hutool.core.text.AntPathMatcher
import cn.hutool.core.util.URLUtil
import cn.minih.app.system.auth.data.TokenInfo
import cn.minih.app.system.constants.TOKEN_CONNECTOR_CHAT
import cn.minih.app.system.exception.AuthLoginException
import cn.minih.app.system.exception.PasswordErrorException
import cn.minih.app.system.user.UserRepository
import cn.minih.app.system.utils.R
import cn.minih.app.system.utils.log
import cn.minih.app.system.utils.toJsonObject
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.Cookie
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
class AuthServiceHandler private constructor() : Handler<RoutingContext> {
    private val userRepository by lazy { UserRepository.instance }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AuthServiceHandler()
        }
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
                    val tokenValue =
                        config.tokenPrefix.plus(TOKEN_CONNECTOR_CHAT).plus(tokenInfo.tokenValue)
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
                    checkLogin(ctx)
                    ctx.next()
                }
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }

    private suspend fun login(params: JsonObject): TokenInfo {
        val username = params.getString("username")
        val password = params.getString("password")
        val device = params.getString("device") ?: "PC"
        Assert.notBlank(username) { AuthLoginException("username不能为空!") }
        Assert.notBlank(password) { AuthLoginException("password不能为空!") }
        log.info("$username 开始登录...")
        val user = userRepository.getUserByUsername(username)
        Assert.notNull(user) { AuthLoginException("${username}用户未找到!") }
        Assert.isTrue(user?.getString("password") == password) { PasswordErrorException() }
        val tokenInfo = AuthUtil.login(username, device)
        log.info("$username 登录成功...")
        return tokenInfo
    }

    private suspend fun checkLogin(ctx: RoutingContext) {
        var tokenValue: String? = ""
        val config = AuthUtil.getConfig()
        val request = ctx.request()
        if (config.ignoreAuthUri.any { AntPathMatcher().match(it, request.path()) }) {
            return
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
        AuthUtil.checkLogin(tokenValue)
    }


}
