package cn.minih.system.controller

import cn.minih.core.annotation.Service
import cn.minih.core.beans.BeanFactory
import cn.minih.web.annotation.Get
import cn.minih.web.annotation.Post
import cn.minih.web.annotation.RequestMapping
import cn.minih.system.data.model.Role
import cn.minih.system.service.RoleService
import cn.minih.web.annotation.Request

@Request("/role")
interface RoleController : cn.minih.web.service.Service {


    @Get("/list")
    suspend fun list(): List<Role>

    @Post("/add")
    suspend fun add(role: Role): Role

}

@Service("roleController")
class RoleControllerImpl : RoleController {
    private val roleService: RoleService
        get() = BeanFactory.instance.getBean("roleService") as RoleService

    override suspend fun list(): List<Role> {
        return roleService.list()
    }

    override suspend fun add(role: Role): Role {
        return roleService.add(role)
    }

}