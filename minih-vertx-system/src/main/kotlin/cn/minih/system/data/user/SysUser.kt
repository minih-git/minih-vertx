package cn.minih.system.data.user

import cn.minih.core.annotation.TableId
import cn.minih.core.annotation.TableName
import java.util.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
@TableName("sys_user")
data class SysUser(
    @TableId
    var id: Long = 0,
    var username: String = "",
    var password: String = "",
    var name: String = "",
    var avatar: String = "",
    var state: Int = 1,
    var role: List<String> = mutableListOf(),
    var mobile: String = "",
    var idNo: String = "",
    var createTime: Long = Date().time,
    var lastActive: Long = 0
)