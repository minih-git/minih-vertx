package cn.minih.app.system.user

import cn.minih.app.system.auth.AuthUtil
import cn.minih.app.system.config.MongoQueryOption
import cn.minih.app.system.constants.MinihErrorCode
import cn.minih.app.system.exception.UserSystemException
import cn.minih.app.system.user.data.SysUser
import cn.minih.app.system.user.data.UserExtra
import cn.minih.app.system.user.data.UserInfo
import cn.minih.app.system.user.data.UserInfoCondition
import cn.minih.app.system.utils.Assert
import cn.minih.app.system.utils.Page
import cn.minih.app.system.utils.covertTo
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object UserServiceHandler {

    suspend fun getUserInfo(): UserInfo {
        val username = AuthUtil.getCurrentLoginId()
        val sysUser = UserRepository.instance.getUserByUsername(username)?.await()?.covertTo(SysUser::class)
        Assert.notNull(sysUser!!) { UserSystemException(errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_DATA_UN_FIND) }
        return UserInfo(sysUser)
    }

    suspend fun queryUsers(page: Page<UserInfo>, condition: UserInfoCondition): Page<UserInfo> {
        val queryOption = MongoQueryOption<SysUser>()
        if (condition.username?.isNotBlank() == true) {
            queryOption.put(SysUser::username, condition.username)
        }
        if (condition.name?.isNotBlank() == true) {
            queryOption.put(SysUser::name, condition.name)
        }
        if (condition.state != null) {
            queryOption.put(SysUser::state, condition.state)
        }
        queryOption.put(SysUser::createTime, jsonObjectOf("\$gt" to page.nextCursor))
        val sysUsersJson = UserRepository.instance.find(queryOption)?.await()
        if (sysUsersJson.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        val userInfo = sysUsersJson.map {
            val extraQueryOption = MongoQueryOption<UserExtra>()
            if (condition.mobile?.isNotBlank() == true) {
                extraQueryOption.put(UserExtra::mobile, condition.mobile)
            }
            extraQueryOption.put("_id", it.getInteger("_id"))
            val extra = UserExtraRepository.instance.findOne(extraQueryOption)?.await()
            UserInfo(sysUser = it.covertTo(SysUser::class), extra?.covertTo(UserExtra::class))
        }

        return Page(userInfo[userInfo.size - 1].sysUser.createTime, userInfo)
    }


}