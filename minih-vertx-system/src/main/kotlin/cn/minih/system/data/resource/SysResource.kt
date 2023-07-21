package cn.minih.system.data.resource

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * @author hubin
 * @date 2023/7/21
 * @desc
 */
data class SysResource(
    @SerializedName("_id", alternate = ["id"])
    var id: String = "",
    var name: String = "",
    var state: Int = 1,
    var parentId: String = "",
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