package cn.minih.system.data.model

import cn.minih.database.annotation.TableId
import cn.minih.database.annotation.TableName

@TableName("sys_role")
data class Role(
    @TableId
    var id: Long? = null,
    var roleName: String? = null,
    var roleCode: String? = null,
    var description: String? = null
)
