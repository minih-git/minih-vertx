package cn.minih.app.system.auth.data

import cn.minih.app.system.constants.DEFAULT_DEVICE
import cn.minih.app.system.constants.DEFAULT_LOGIN_TYPE

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthLoginModel(
    var id: String = "",
    var device: String = DEFAULT_DEVICE,
    var timeout: Long = -1,
    var loginType: String = DEFAULT_LOGIN_TYPE
)
