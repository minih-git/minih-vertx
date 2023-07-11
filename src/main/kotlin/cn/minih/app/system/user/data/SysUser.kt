package cn.minih.app.system.user.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class SysUser(
    @JsonProperty("_id")
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
    val id: Long = 0L,
    val mobile: String = "",
)