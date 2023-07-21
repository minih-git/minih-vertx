package cn.minih.system.exception

import cn.minih.core.exception.IMinihErrorCode
import cn.minih.core.exception.MinihException


/**
 * @author hubin
 * @date 2023/7/11
 * @desc
 */
class SystemException(msg: String? = null, errorCode: IMinihErrorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM) :
    MinihException(msg, errorCode)