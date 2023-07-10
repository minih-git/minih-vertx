package cn.minih.app.system.auth.data

import cn.minih.app.system.constants.DEFAULT_DEVICE

/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
data class AuthLoginModel(
    var device: String = DEFAULT_DEVICE,
    var timeout: Long = -1
)
