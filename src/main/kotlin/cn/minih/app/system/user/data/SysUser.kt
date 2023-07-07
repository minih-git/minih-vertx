package cn.minih.app.system.user.data

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
data class SysUser(
    var username: String,
    var name: String,
    var password: String,
    var avatar: String,
    var state: String,
    var createTime: String
)
