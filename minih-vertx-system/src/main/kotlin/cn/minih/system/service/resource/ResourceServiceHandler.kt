package cn.minih.system.service.resource

import cn.minih.auth.annotation.AuthCheckRole
import cn.minih.auth.constants.CONTEXT_SYSTEM_ADMIN_ROLE_TAG
import cn.minih.auth.logic.AuthUtil
import cn.minih.core.repository.MongoQueryOption
import cn.minih.core.utils.*
import cn.minih.system.data.resource.ResourceCondition
import cn.minih.system.data.resource.SysResource
import cn.minih.system.data.role.SysRole
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.SystemException
import cn.minih.system.service.AuthServiceImpl
import cn.minih.system.service.role.RoleRepository
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import java.util.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object ResourceServiceHandler {


    suspend fun queryResources(page: Page<SysResource>, condition: ResourceCondition): Page<SysResource> {
        val queryOption = MongoQueryOption<SysResource>()
        if (condition.name?.isNotBlank() == true) {
            queryOption.put(SysResource::name, condition.name)
        }
        if (condition.state != null) {
            queryOption.put(SysResource::state, condition.state)
        }
        queryOption.put(SysResource::createTime, jsonObjectOf("\$gt" to page.nextCursor))
        if (!AuthUtil.currentIsSysAdmin()) {
            val roleTags = AuthServiceImpl.instance.getLoginRole(AuthUtil.getCurrentLoginId())
            val roleCondition = MongoQueryOption<SysRole>()
            roleCondition.put(SysRole::roleTag, jsonObjectOf("\$in" to roleTags))
            val roles = RoleRepository.instance.find(roleCondition)?.await()
            if (roles.isNullOrEmpty()) {
                return Page(0, listOf())
            }
            val reIds = mutableListOf<String>()
            roles.map { it.covertTo(SysRole::class) }.forEach{
                reIds.addAll(it.resources)
            }
            if (reIds.isEmpty()) {
                return Page(0, listOf())
            }
            queryOption.put("_id", jsonObjectOf("\$in" to reIds.toList()))
        }
        val sysResource = ResourceRepository.instance.find(queryOption)?.await()
        if (sysResource.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        val resources = sysResource.map {
            it.covertTo(SysResource::class)
        }
        return Page(resources.last().createTime, resources)
    }

    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun addResource(resource: SysResource) {
        Assert.notBlank(resource.name) {
            SystemException(
                msg = "资源名称不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysResource = resource.toJsonObject().covertTo(SysResource::class)
        sysResource.id = SnowFlake.nextId().toString()
        sysResource.createTime = Date().time
        ResourceRepository.instance.insert(sysResource).await()
    }

    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun editResource(resource: SysResource) {
        Assert.notBlank(resource.id) {
            SystemException(
                msg = "资源id不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysResource = ResourceRepository.instance.findOne("_id" to resource.id)?.await()
            ?.covertTo(SysResource::class)
        Assert.notNull(sysResource) { SystemException(errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_DATA_UN_FIND) }
        var update = false
        if (sysResource != null) {
            resource.name.notBlankAndExec { update = true;sysResource.name = it }
            resource.state.notBlankAndExec { update = true;sysResource.state = it }
            resource.permissionTag.notBlankAndExec { update = true;sysResource.permissionTag = it }
            resource.path.notBlankAndExec { update = true;sysResource.path = it }
            resource.icon.notBlankAndExec { update = true;sysResource.icon = it }
            if (update) {
                ResourceRepository.instance.update("_id" to sysResource.id, data = sysResource).await()
            }
        }
    }


}
