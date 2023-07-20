package cn.minih.system.service.role

import cn.minih.core.repository.MongoQueryOption
import cn.minih.core.utils.*
import cn.minih.system.data.role.AddRole
import cn.minih.system.data.role.RoleCondition
import cn.minih.system.data.role.SysRole
import cn.minih.system.data.user.*
import cn.minih.system.exception.MinihSystemErrorCode
import cn.minih.system.exception.UserSystemException
import cn.minih.system.service.user.UserExtraRepository
import cn.minih.system.service.user.UserRepository
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import org.mindrot.jbcrypt.BCrypt
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

    suspend fun addUser(user: AddRole) {


    }

}
