package cn.minih.app.system.user.data

import java.util.Date

/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
data class SysUser(
    var username: String = "",
    var name: String = "",
    var password: String = "",
    var avatar: String = "",
    var state: Int = 1,
    var createTime: Date = Date()
)