package cn.minih.system.service

import cn.minih.core.annotation.Service
import cn.minih.database.manager.RepositoryManager
import cn.minih.database.operation.QueryWrapper
import cn.minih.system.data.model.Role
import io.vertx.kotlin.coroutines.await

@Service("roleService")
class RoleService {

    suspend fun list(): List<Role> {
        return RepositoryManager.list(QueryWrapper<Role>()).await()
    }

    suspend fun add(role: Role): Role {
        return RepositoryManager.insert(role).await()
    }
}