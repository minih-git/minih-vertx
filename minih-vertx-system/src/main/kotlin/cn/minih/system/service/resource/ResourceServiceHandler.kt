package cn.minih.system.service.resource

import cn.minih.auth.annotation.AuthCheckRole
import cn.minih.auth.constants.CONTEXT_SYSTEM_ADMIN_ROLE_TAG
import cn.minih.auth.logic.AuthUtil
import cn.minih.core.repository.QueryWrapper
import cn.minih.core.repository.RepositoryManager
import cn.minih.core.utils.*
import cn.minih.system.data.resource.ResourceCondition
import cn.minih.system.data.resource.SysResource
import cn.minih.system.data.role.SysRole
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.SystemException
import cn.minih.system.service.AuthServiceImpl
import io.vertx.kotlin.coroutines.await
import java.util.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object ResourceServiceHandler {


    suspend fun queryResources(page: Page<SysResource>, condition: ResourceCondition): Page<SysResource> {
        val queryOption = QueryWrapper<SysResource>()
        if (condition.name?.isNotBlank() == true) {
            queryOption.eq(SysResource::name, condition.name)
        }
        if (condition.state != null) {
            queryOption.eq(SysResource::state, condition.state)
        }
        queryOption.gt(SysResource::createTime, page.nextCursor)
        if (!AuthUtil.currentIsSysAdmin()) {
            val roleTags = AuthServiceImpl.instance.getLoginRole(AuthUtil.getCurrentLoginId())
            val roleCondition = QueryWrapper<SysRole>().`in`(SysRole::roleTag, roleTags)
            val roles = RepositoryManager.list(roleCondition).await()
            if (roles.isNullOrEmpty()) {
                return Page(0, listOf())
            }
            val reIds = mutableListOf<String>()
            roles.forEach {
                reIds.addAll(it.resources)
            }
            if (reIds.isEmpty()) {
                return Page(0, listOf())
            }
            queryOption.`in`(SysResource::id, reIds.toList())
        }
        val sysResource = RepositoryManager.list(queryOption).await()
        if (sysResource.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        return Page(sysResource.last().createTime, sysResource)
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
        RepositoryManager.insert(sysResource)
    }

    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun editResource(resource: SysResource) {
        Assert.notBlank(resource.id) {
            SystemException(
                msg = "资源id不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
//        val sysResource = ResourceRepository.instance.findOne("_id" to resource.id)?.await()
//            ?.covertTo(SysResource::class)
//        Assert.notNull(sysResource) { SystemException(errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_DATA_UN_FIND) }
//        var update = false
//        if (sysResource != null) {
//            resource.name.notBlankAndExec { update = true;sysResource.name = it }
//            resource.state.notBlankAndExec { update = true;sysResource.state = it }
//            resource.permissionTag.notBlankAndExec { update = true;sysResource.permissionTag = it }
//            resource.path.notBlankAndExec { update = true;sysResource.path = it }
//            resource.icon.notBlankAndExec { update = true;sysResource.icon = it }
//            if (update) {
//                ResourceRepository.instance.update("_id" to sysResource.id, data = sysResource).await()
//            }
//        }
    }


}
