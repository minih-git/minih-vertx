package cn.minih.gateway.schema

data class ToolDefinition(
    val type: String = "function",
    val function: FunctionObj
)

data class FunctionObj(
    val name: String,
    val description: String,
    val parameters: JsonSchema
)

data class JsonSchema(
    val type: String = "object",
    val properties: Map<String, Any>,
    val required: List<String> = emptyList()
)
