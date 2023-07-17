package cn.minih.app.system.user

import cn.minih.app.system.auth.AuthUtil
import cn.minih.app.system.config.MongoQueryOption
import cn.minih.app.system.constants.MinihErrorCode
import cn.minih.app.system.exception.UserSystemException
import cn.minih.app.system.user.data.*
import cn.minih.app.system.utils.*
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import org.mindrot.jbcrypt.BCrypt


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

    suspend fun addUser(user: AddUser) {
        validateAddUserParams(user)
        user.password = if (user.password == null) BCrypt.hashpw(
            "${user.username}@123",
            BCrypt.gensalt()
        ) else BCrypt.hashpw(user.password, BCrypt.gensalt())
        val sysId = SnowFlake.nextId();
        val sysUser = user.toJsonObject().covertTo(SysUser::class)
        val extra = user.toJsonObject().covertTo(UserExtra::class)
        sysUser.id = sysId
        extra.id = sysId
        UserRepository.instance.insert(sysUser).await()
        UserExtraRepository.instance.insert(extra).await()
    }

    suspend fun editUser(user: EditUser) {
        Assert.notNull(user.id) {
            UserSystemException(
                msg = "用户id不能为空！",
                errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        Assert.isTrue(user.id != 0L) {
            UserSystemException(
                msg = "用户id不能为空！",
                errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysUser = UserRepository.instance.findOne("_id" to user.id)?.await()?.covertTo(SysUser::class)
        Assert.notNull(sysUser) { UserSystemException(errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_DATA_UN_FIND) }
        var userExtra = UserExtraRepository.instance.findOne("_id" to user.id)?.await()?.covertTo(UserExtra::class)
        if (sysUser != null) {
            var update = false
            if (userExtra == null || userExtra.id == 0L) {
                userExtra = UserExtra(id = sysUser.id)
            }
            user.password?.let { update = true;sysUser.password = BCrypt.hashpw(it, BCrypt.gensalt()) }
            user.avatar?.let { update = true;sysUser.avatar = it }
            user.name?.let { update = true;sysUser.name = it }
            user.mobile?.let { update = true;userExtra.mobile = it }
            user.realName?.let { update = true;userExtra.realName = it }
            user.idType?.let { update = true;userExtra.idType = it }
            user.idNo?.let { update = true; userExtra.idNo = it }
            user.state?.let { update = true;sysUser.state = it }
            if (update) {
                UserRepository.instance.update("_id" to sysUser.id, data = sysUser)
                UserExtraRepository.instance.update("_id" to sysUser.id, data = userExtra)
            }
        }
    }

    private fun validateAddUserParams(user: AddUser) {
        Assert.notBlank(user.name) {
            UserSystemException(
                msg = "用户名字不能为空！",
                errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        Assert.notBlank(user.mobile) {
            UserSystemException(
                msg = "手机号不能为空！",
                errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        Assert.notBlank(user.username) {
            UserSystemException(
                msg = "登录账号不能为空！",
                errorCode = MinihErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
    }


}
