package cn.minih.system.exception

import cn.minih.core.exception.IMinihErrorCode

/**
 * @author hubin
 * @date 2023/7/19
 * @desc
 */
enum class MinihSystemErrorCode(override val code: Int, override val msg: String) : IMinihErrorCode {

    //用户系统错误
    ERR_CODE_SYSTEM(-20, "用户系统产生错误"),
    ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT(-21, "非法参数"),
    ERR_CODE_SYSTEM_DATA_UN_FIND(-22, "未找到数据")
}