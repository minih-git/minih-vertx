import {BaseData, get, Page, post} from "../utils";
import {RoleInfo} from "../store/module/role";


export async function roleList(search: string = "", nextCursor: number = 0): Promise<Page<RoleInfo>> {
    let url = "/role/page"
    let res = await post(url, {"search": search, "nextCursor": nextCursor})
    let data = res.data["data"]
    return {
        nextCursor: res.data["nextCursor"],
        data: data.map(it => {
            return {
                id: it._id,
                name: it.name,
                state: it.state,
                roleTag: it.roleTag,
                createTime: it.createTime,
                resources: it.resources,
            }
        })
    }
}

export async function addRole(roleInfo: Partial<RoleInfo>): Promise<BaseData> {
    let url = "/role/addRole"
    return await post(url, roleInfo)
}


export async function editRole(roleInfo: RoleInfo): Promise<BaseData> {
    let url = "/role/editRole"
    return await post(url, roleInfo)
}

export async function checkRoleTag(roleTag: String): Promise<BaseData> {
    let url = "/role/checkRoleTag"
    return await get(url, {roleTag})
}
