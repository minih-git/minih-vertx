package cn.minih.system.data.role

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
data class SysRole(
    @SerializedName("_id", alternate = ["id"])
    var id: String = "",
    var name: String = "",
    var resources: MutableList<String> = mutableListOf(),
    var roleTag: String = "",
    var state: Int = 1,
    var createTime: Long = Date().time,
)


data class RoleCondition(
    val name: String?,
    val state: Int?,
)