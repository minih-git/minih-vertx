package cn.minih.common.annotation

/**
 * 标记接口为 AI 工具，用于语义注册和发现
 * @property description 功能的自然语言描述
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiTool(val description: String)
