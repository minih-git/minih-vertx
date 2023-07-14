package cn.minih.app.system.user.data

/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
data class UserInfo(
    val sysUser: SysUser = SysUser(),
    val userExtra: UserExtra? = UserExtra()
)

data class UserInfoCondition(
    val name: String?,
    val username: String?,
    val state: Int?,
    val mobile: String?,
)
