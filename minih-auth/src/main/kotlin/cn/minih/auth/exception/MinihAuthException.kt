package cn.minih.auth.exception

import cn.minih.common.exception.IMinihErrorCode
import cn.minih.common.exception.MinihErrorCode
import cn.minih.common.exception.MinihException


/**
 * @author hubin
 * @date 2023/7/10
 * @desc
 */
open class MinihAuthException(
    override val msg: String? = null,
    override val errorCode: IMinihErrorCode = MinihErrorCode.ERR_CODE_UNDEFINED
) : MinihException("错误代码:${errorCode.code},${msg ?: errorCode.msg}")