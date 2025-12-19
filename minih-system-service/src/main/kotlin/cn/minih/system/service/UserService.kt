package cn.minih.system.service

import cn.minih.core.annotation.Service
import cn.minih.database.manager.RepositoryManager
import cn.minih.database.operation.QueryWrapper
import cn.minih.system.data.model.User
import io.vertx.kotlin.coroutines.await

@Service("userService")
class UserService {

    suspend fun list(): List<User> {
        return RepositoryManager.list(QueryWrapper<User>()).await()
    }

    suspend fun getById(id: Long): User? {
        return RepositoryManager.findById<User>(id).await()
    }

    suspend fun add(user: User): User {
        return RepositoryManager.insert(user).await()
    }

    suspend fun login(username: String, password: String): User? {
        val query = QueryWrapper<User>().eq(User::username, username).eq(User::password, password)
        return RepositoryManager.findOne(query).await()
    }
}