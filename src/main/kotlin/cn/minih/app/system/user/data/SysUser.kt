package cn.minih.app.system.user.data

import com.google.gson.annotations.SerializedName

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class SysUser(
    @SerializedName("_id")
    val id: Long = 0L,
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val avatar: String = "",
    val state: Int = 1,
    val role: List<String> = mutableListOf(),
    val createTime: Long = 0L,
)

data class UserExtra(
    @SerializedName("_id")
    val id: Long = 0L,
    val mobile: String = "",
)
