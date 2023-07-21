package cn.minih.system.service.role

import cn.minih.auth.annotation.AuthCheckRole
import cn.minih.auth.constants.CONTEXT_SYSTEM_ADMIN_ROLE_TAG
import cn.minih.core.repository.MongoQueryOption
import cn.minih.core.utils.*
import cn.minih.system.data.role.RoleCondition
import cn.minih.system.data.role.SysRole
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.SystemException
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import java.util.*


/**
 * @author hubin
 * @date 2023/7/7
 * @desc
 */
object RoleServiceHandler {
    suspend fun queryRoles(page: Page<SysRole>, condition: RoleCondition): Page<SysRole> {
        val queryOption = MongoQueryOption<SysRole>()
        if (condition.name?.isNotBlank() == true) {
            queryOption.put(SysRole::name, condition.name)
        }
        if (condition.state != null) {
            queryOption.put(SysRole::state, condition.state)
        }
        queryOption.put(SysRole::createTime, jsonObjectOf("\$gt" to page.nextCursor))
        val sysRoleJson = RoleRepository.instance.find(queryOption)?.await()
        if (sysRoleJson.isNullOrEmpty()) {
            return Page(0, listOf())
        }
        val roles = sysRoleJson.map {
            it.covertTo(SysRole::class)
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
        sysRole.id = SnowFlake.nextId().toString()
        sysRole.createTime = Date().time
        RoleRepository.instance.insert(sysRole).await()
    }
    @AuthCheckRole(CONTEXT_SYSTEM_ADMIN_ROLE_TAG)
    suspend fun editRole(role: SysRole) {
        Assert.notBlank(role.id) {
            SystemException(
                msg = "角色id不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysRole = RoleRepository.instance.findOne("_id" to role.id)?.await()?.covertTo(SysRole::class)
        Assert.notNull(sysRole) { SystemException(errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_DATA_UN_FIND) }
        var update = false
        if (sysRole != null) {
            role.name.notBlankAndExec { update = true;sysRole.name = it }
            role.state.notBlankAndExec { update = true;sysRole.state = it }
            role.resources.notBlankAndExec { update = true;sysRole.resources = it }
            if (update) {
                RoleRepository.instance.update("_id" to sysRole.id, data = sysRole).await()
            }
        }
    }

    suspend fun checkRoleTag(roleTag: String?) {
        Assert.notBlank(roleTag) {
            SystemException(
                msg = "角色tag不能为空！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
        val sysRole = RoleRepository.instance.findOne("roleTag" to roleTag)?.await()
        Assert.isNull(sysRole) {
            SystemException(
                msg = "角色tag已存在！",
                errorCode = MinihSystemErrorCode.ERR_CODE_SYSTEM_ILLEGAL_ARGUMENT
            )
        }
    }


}