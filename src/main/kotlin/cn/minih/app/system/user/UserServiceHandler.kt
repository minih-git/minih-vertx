package cn.minih.app.system.user

import cn.minih.app.system.auth.AuthUtil
import cn.minih.app.system.constants.MinihErrorCode
import cn.minih.app.system.exception.UserSystemException
import cn.minih.app.system.user.data.SysUser
import cn.minih.app.system.user.data.UserExtra
import cn.minih.app.system.user.data.UserInfo
import cn.minih.app.system.user.data.UserInfoCondition
import cn.minih.app.system.utils.Assert
import cn.minih.app.system.utils.IPage
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

    suspend fun queryUsers(page: IPage, condition: UserInfoCondition): Page<UserInfo> {
        val queryOption = JsonObject()
        if (condition.username?.isNotBlank() == true) {
            queryOption.put(SysUser::username.toString(), condition.username)
        }
        if (condition.name?.isNotBlank() == true) {
            queryOption.put(SysUser::name.toString(), condition.name)
        }
        if (condition.state != null) {
            queryOption.put(SysUser::state.toString(), condition.state)
        }
        queryOption.put("\$gt", jsonObjectOf("\$createTime" to page.nextCursor))
        val sysUsersJson = UserRepository.instance.find(queryOption)?.await()
        if (sysUsersJson.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        val userInfo = sysUsersJson.map {
            val extraQueryOption = JsonObject()
            if (condition.mobile?.isNotBlank() == true) {
                extraQueryOption.put(UserExtra::mobile.toString(), condition.mobile)
            }
            extraQueryOption.put("_id", it.getInteger("_id"))
            val extra = UserExtraRepository.instance.findOne(extraQueryOption)?.await()
            UserInfo(sysUser = it.covertTo(SysUser::class), extra?.covertTo(UserExtra::class))
        }

        return Page(userInfo[userInfo.size - 1].sysUser.createTime, userInfo)
    }


}
