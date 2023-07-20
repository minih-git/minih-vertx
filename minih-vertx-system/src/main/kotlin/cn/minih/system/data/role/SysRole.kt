package cn.minih.system.data.role

import java.util.*

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
data class SysRole(
    val name: String = "",
    var resources: MutableList<String> = mutableListOf(),
    var roleId: String = "",
    var state: Int = 1,
    var createTime: Long = Date().time,
)

data class AddRole(
    val name: String? = null,
    var resources: MutableList<String>? = null,
    var roleId: String? = null,
    var state: Int? = null,
)

data class RoleCondition(
    val name: String?,
    val state: Int?,
)
