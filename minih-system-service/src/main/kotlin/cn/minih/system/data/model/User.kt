package cn.minih.system.data.model

import cn.minih.database.annotation.TableId
import cn.minih.database.annotation.TableName

@TableName("sys_user")
data class User(
    @TableId
    var id: Long? = null,
    var username: String? = null,
    var password: String? = null, // In real app, this should be encrypted
    var nickname: String? = null,
    var email: String? = null
)
