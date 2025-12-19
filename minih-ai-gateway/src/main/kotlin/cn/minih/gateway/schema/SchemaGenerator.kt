package cn.minih.gateway.schema

import cn.minih.common.annotation.AiTool
import java.lang.reflect.Method
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction

object SchemaGenerator {

    fun generateSchema(method: Method): ToolDefinition {
        val kFunction = method.kotlinFunction ?: throw IllegalArgumentException("Not a Kotlin function")
        val aiTool = kFunction.findAnnotation<AiTool>()

        val params = kFunction.valueParameters.associate { param ->
            param.name!! to mapKotlinTypeToJsonType(param.type)
        }

        return ToolDefinition(
            type = "function",
            function = FunctionObj(
                name = kFunction.name,
                description = aiTool?.description ?: "",
                parameters = JsonSchema(
                    type = "object",
                    properties = params,
                    required = kFunction.valueParameters.filter { !it.type.isMarkedNullable }.mapNotNull { it.name }
                )
            )
        )
    }

    private fun mapKotlinTypeToJsonType(type: KType): Map<String, Any> {
        val classifier = type.classifier
        val typeName = classifier.toString()

        return when {
            typeName.contains("String") -> mapOf("type" to "string")
            typeName.contains("Int") || typeName.contains("Long") -> mapOf("type" to "integer")
            typeName.contains("Double") || typeName.contains("Float") -> mapOf("type" to "number")
            typeName.contains("Boolean") -> mapOf("type" to "boolean")
            typeName.contains("List") || typeName.contains("Array") -> {
                val componentType = type.arguments.firstOrNull()?.type
                if (componentType != null) {
                    mapOf(
                        "type" to "array",
                        "items" to mapKotlinTypeToJsonType(componentType)
                    )
                } else {
                    mapOf("type" to "array")
                }
            }
            classifier is kotlin.reflect.KClass<*> && classifier.isData -> {
                // Recursively map Data Class
                val properties = classifier.memberProperties.associate { prop ->
                    prop.name to mapKotlinTypeToJsonType(prop.returnType)
                }
                val required = classifier.memberProperties
                    .filter { !it.returnType.isMarkedNullable }
                    .map { it.name }

                mapOf(
                    "type" to "object",
                    "properties" to properties,
                    "required" to required
                )
            }
            else -> mapOf("type" to "string", "description" to "Unsupported type converted to string representation")
        }
    }
}