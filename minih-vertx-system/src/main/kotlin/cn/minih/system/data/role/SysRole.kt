package cn.minih.system.data.role

import java.util.*

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
data class SysRole(
    var id: Long = 0L,
    var name: String = "",
    var resources: MutableList<String> = mutableListOf(),
    var roleTag: String = "",
    var createTime: Long = Date().time,
)


data class RoleCondition(
    val name: String?,
    val state: Int?,
)
