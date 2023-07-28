package cn.minih.system.data.resource

import cn.minih.core.annotation.TableId
import cn.minih.core.annotation.TableName
import java.util.*

/**
 * @author hubin
 * @date 2023/7/21
 * @desc
 */
@TableName("sys_resource")
data class SysResource(
    @TableId
    var id: Long = 0,
    var name: String = "",
    var parentId: Long = 0L,
    var permissionTag: List<String> = mutableListOf(),
    var path: String = "",
    var type: String = "",
    var icon: String = "",
    var createTime: Long = Date().time,
)

data class ResourceCondition(
    val name: String?,
    val state: Int?,
)
