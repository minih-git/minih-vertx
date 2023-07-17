package cn.minih.app.system.user.data

import com.google.gson.annotations.SerializedName

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class SysUser(
    @SerializedName("_id")
    var id: Long = 0L,
    val username: String = "",
    var password: String = "",
    var name: String = "",
    var avatar: String = "",
    var state: Int = 1,
    var role: List<String> = mutableListOf(),
    val createTime: Long = 0L,
)

data class UserExtra(
    @SerializedName("_id")
    var id: Long = 0L,
    var mobile: String = "",
    var realName: String = "",
    var idType: String = "",
    var idNo: String = "",
)
