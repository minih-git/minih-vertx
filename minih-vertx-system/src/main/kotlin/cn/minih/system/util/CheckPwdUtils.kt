package cn.minih.system.util

object CheckPwdUtils {
    //数字
    private const val REG_NUMBER = ".*\\d+.*"

    //小写字母
    private const val REG_UPPERCASE = ".*[A-Z]+.*"

    //大写字母
    private const val REG_LOWERCASE = ".*[a-z]+.*"

    //特殊符号
    private const val REG_SYMBOL = ".*[~!@#$%^&*()_+|<>,.?/:;'\\[\\]{}\"]+.*"

    /**
     * 长度至少minLength位,且最大长度不超过maxlength,须包含大小写字母,数字,特殊字符matchCount种以上组合
     * @param password 输入的密码
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param matchCount 次数
     * @return 是否通过验证
     */
    fun checkPwd(password: String, minLength: Int, maxLength: Int, matchCount: Int): Boolean {
        //密码为空或者长度小于8位则返回false
        if (password.length < minLength || password.length > maxLength) return false
        var i = 0
        if (password.matches(Regex(REG_NUMBER))) i++
        if (password.matches(Regex(REG_LOWERCASE))) i++
        if (password.matches(Regex(REG_UPPERCASE))) i++
        if (password.matches(Regex(REG_SYMBOL))) i++
        return i >= matchCount
    }
}
