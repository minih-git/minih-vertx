package cn.minih.system.service.role

import cn.minih.auth.annotation.AuthCheckRole
import cn.minih.auth.constants.CONTEXT_SYSTEM_ADMIN_ROLE_TAG
import cn.minih.core.repository.RepositoryManager
import cn.minih.core.repository.conditions.QueryWrapper
import cn.minih.core.utils.*
import cn.minih.system.data.role.RoleCondition
import cn.minih.system.data.role.SysRole
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.SystemException
import io.vertx.kotlin.coroutines.await
import java.util.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object RoleServiceHandler {
    suspend fun queryRoles(page: Page<SysRole>, condition: RoleCondition): Page<SysRole> {
        val queryOption = QueryWrapper<SysRole>()
        if (condition.name?.isNotBlank() == true) {
            queryOption.eq(SysRole::name, condition.name)
        }
        queryOption.gt(SysRole::createTime, page.nextCursor)
        val roles = RepositoryManager.list(queryOption).await()
        if (roles.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        return Page(roles.last().createTime, roles)
    }

    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun addRole(role: SysRole) {
        Assert.notBlank(role.name) {
            SystemException(
                msg = "角色名字不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        Assert.notBlank(role.roleTag) {
            SystemException(
                msg = "角色tag不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        checkRoleTag(role.roleTag)
        val sysRole = role.toJsonObject().covertTo(SysRole::class)
        sysRole.id = SnowFlake.nextId()
        sysRole.createTime = Date().time
        RepositoryManager.insert(sysRole)
    }

    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun editRole(role: SysRole) {
        Assert.notBlank(role.id) {
            SystemException(
                msg = "角色id不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysRole = RepositoryManager.findOne(QueryWrapper<SysRole>().eq(SysRole::id, role.id)).await()
        Assert.notNull(sysRole) { SystemException(errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_DATA_UN_FIND) }
        var update = false
        if (sysRole != null) {
            role.name.notBlankAndExec { update = true;sysRole.name = it }
            role.resources.notBlankAndExec { update = true;sysRole.resources = it }
            if (update) {
                RepositoryManager.update(sysRole).await()
            }
        }
    }

    suspend fun checkRoleTag(roleTag: String) {
        Assert.notBlank(roleTag) {
            SystemException(
                msg = "角色tag不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysRole = RepositoryManager.findOne(QueryWrapper<SysRole>().eq(SysRole::roleTag, roleTag)).await()
        Assert.isNull(sysRole) {
            SystemException(
                msg = "角色tag已存在！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
    }


}
