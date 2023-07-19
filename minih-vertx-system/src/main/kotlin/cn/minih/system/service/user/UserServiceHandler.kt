package cn.minih.system.service.user

import cn.minih.auth.logic.AuthUtil
import cn.minih.core.repository.MongoQueryOption
import cn.minih.core.utils.*
import cn.minih.system.data.user.*
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.UserSystemException
import cn.minih.system.util.CheckPwdUtils
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import org.mindrot.jbcrypt.BCrypt
import java.util.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object UserServiceHandler {

    suspend fun getUserInfo(): UserInfo {
        val username = AuthUtil.getCurrentLoginId()
        val sysUser = UserRepository.instance.getUserByUsername(username)?.await()?.covertTo(SysUser::class)
        Assert.notNull(sysUser!!) { UserSystemException(errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_DATA_UN_FIND) }
        val extra = UserExtraRepository.instance.findOne("_id" to sysUser.id)?.await()?.covertTo(UserExtra::class)
        return UserInfo(sysUser, extra)
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
        val extraQueryOption = MongoQueryOption<UserExtra>()
        val userInfo = sysUsersJson.map {
            if (condition.mobile?.isNotBlank() == true) {
                extraQueryOption.put(UserExtra::mobile, condition.mobile)
            }
            extraQueryOption.put("_id", it.getString("_id"))
            val extra = UserExtraRepository.instance.findOne(extraQueryOption)?.await()
            val online = AuthUtil.getOnline(it.getString("username"))
            UserInfo(sysUser = it.covertTo(SysUser::class), extra?.covertTo(UserExtra::class), online)
        }
        return Page(userInfo.last().sysUser.createTime, userInfo)
    }

    suspend fun addUser(user: UserExpand) {
        validateAddUserParams(user, true)
        user.password = if (user.password == null) BCrypt.hashpw(
            "${user.username}@123",
            BCrypt.gensalt()
        ) else BCrypt.hashpw(user.password, BCrypt.gensalt())
        val sysId = SnowFlake.nextId()
        val sysUser = user.toJsonObject().covertTo(SysUser::class)
        val extra = user.toJsonObject().covertTo(UserExtra::class)
        sysUser.id = sysId.toString()
        extra.id = sysId.toString()
        sysUser.createTime = Date().time
        sysUser.state = 1
        UserRepository.instance.insert(sysUser).await()
        UserExtraRepository.instance.insert(extra).await()
    }

    suspend fun editUser(user: UserExpand) {
        validateAddUserParams(user)
        val sysUser = UserRepository.instance.findOne("_id" to user.id)?.await()?.covertTo(SysUser::class)
        Assert.notNull(sysUser) { UserSystemException(errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_DATA_UN_FIND) }
        var userExtra = UserExtraRepository.instance.findOne("_id" to user.id)?.await()?.covertTo(UserExtra::class)
        if (sysUser != null) {
            var update = false
            if (userExtra == null || userExtra.id.isBlank()) {
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
                UserRepository.instance.update("_id" to sysUser.id, data = sysUser).await()
                UserExtraRepository.instance.update("_id" to sysUser.id, data = userExtra).await()
            }
        }
    }

    private suspend fun validateAddUserParams(user: UserExpand, newAdd: Boolean = false) {
        Assert.notBlank(user.name) {
            UserSystemException(
                msg = "用户名字不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        Assert.notBlank(user.mobile) {
            UserSystemException(
                msg = "手机号不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        user.password?.let {
            Assert.isTrue(CheckPwdUtils.checkPwd(it, 8, 20, 3)) {
                UserSystemException(
                    msg = "密码强度不够！",
                    errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
                )
            }

        }
        if (newAdd) {
            Assert.notBlank(user.username) {
                UserSystemException(
                    msg = "登录账号不能为空！",
                    errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
                )
            }
            val sysUser = UserRepository.instance.findOne("username" to user.username)?.await()
            Assert.isNull(sysUser) {
                UserSystemException(
                    msg = "账号已存在！",
                    errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
                )
            }
            val sysExtra = UserExtraRepository.instance.findOne("mobile" to user.mobile)?.await()
            Assert.isNull(sysExtra) {
                UserSystemException(
                    msg = "手机号已存在！",
                    errorCode = MinihSystemErrorCode.ERR_CODE_USER_SYSTEM_ILLEGAL_ARGUMENT
                )
            }
        }
    }
}
