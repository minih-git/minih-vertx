package cn.minih.system.data.user

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class SysUser(
    @SerializedName("_id")
    var id: String = "",
    val username: String = "",
    var password: String = "",
    var name: String = "",
    var avatar: String = "",
    var state: Int = 1,
    var role: List<String> = mutableListOf(),
    var createTime: Long = Date().time,
)

data class UserExtra(
    @SerializedName("_id")
    var id: String = "",
    var mobile: String = "",
    var realName: String = "",
    var idType: String = "",
    var idNo: String = "",
)
