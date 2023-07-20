import {Page, post} from "../utils/http";
import {RoleInfo} from "../store/module/role";


export async function roleList(search: string = "", nextCursor: number = 0): Promise<Page<RoleInfo>> {
    let url = "/role/page"
    let res = await post(url, {"search": search, "nextCursor": nextCursor})
    let data = res.data["data"]
    return {
        nextCursor: res.data["nextCursor"],
        data: data.map(it => {
            return {
                name: it.name,
                state: it.state,
                roleId: it.roleId,
                createTime: it.createTime,
                resources: it.resources,
            }
        })
    }
}