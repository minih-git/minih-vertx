package cn.minih.system.data.user

/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
data class UserInfo(
    val sysUser: SysUser = SysUser(),
    val userExtra: UserExtra? = UserExtra(),
    var online: Int = 0,
    )

data class UserInfoCondition(
    val name: String?,
    val username: String?,
    val state: Int?,
    val mobile: String?,
    var online: Int = 0,
    )

data class AddUser(
    var username: String?,
    var password: String?,
    var name: String?,
    var avatar: String?,
    var state: Int = 1,
    var role: List<String>?,
    var mobile: String?,
    var realName: String?,
    var idType: String?,
    var idNo: String?,
)

data class EditUser(
    var id: Long?,
    var username: String?,
    var password: String?,
    var name: String?,
    var avatar: String?,
    var state: Int?,
    var role: List<String>?,
    var mobile: String?,
    var realName: String?,
    var idType: String?,
    var idNo: String?,
)
