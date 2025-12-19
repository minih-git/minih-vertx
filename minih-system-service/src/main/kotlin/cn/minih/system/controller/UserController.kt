package cn.minih.system.controller

import cn.minih.common.annotation.AiTool

import cn.minih.core.annotation.Service
import cn.minih.core.beans.BeanFactory
import cn.minih.web.annotation.Get
import cn.minih.web.annotation.Post
import cn.minih.system.data.model.User
import cn.minih.system.service.UserService
import cn.minih.web.annotation.Request

@Request("/user")
interface UserController : cn.minih.web.service.Service {


    @Get("/list")
    @AiTool("List all users")
    suspend fun list(): List<User>

    @Get("/get")
    @AiTool("Get user details by ID")
    suspend fun get(id: Long): User?

    @Post("/add")
    @AiTool("Add a new user")
    suspend fun add(user: User): User

    @Post("/login")
    @AiTool("User login")
    suspend fun login(user: User): User?
}

@Service("userController")
class UserControllerImpl : UserController {

    private val userService: UserService
        get() = BeanFactory.instance.getBean("userService") as UserService


    @Get("/list")
    override suspend fun list(): List<User> {
        return userService.list()
    }

    @Get("/get")
    override suspend fun get(id: Long): User? {
        return userService.getById(id)
    }

    @Post("/add")
    override suspend fun add(user: User): User {
        return userService.add(user)
    }

    @Post("/login")
    override suspend fun login(user: User): User? {
        return userService.login(user.username ?: "", user.password ?: "")
    }

}