import {BaseData, Page, post} from "../utils";
import {ResourceInfo} from "../store/module/resource";


export async function resourceList(search: string = "", nextCursor: number = 0): Promise<Page<ResourceInfo>> {
    let url = "/resource/page"
    let res = await post(url, {"search": search, "nextCursor": nextCursor})
    let data = res.data["data"]
    return {
        nextCursor: res.data["nextCursor"],
        data: data.map(it => {
            return {
                id: it._id,
                name: it.name,
                state: it.state,
                parentId: it.parentId,
                permissionTag: it.permissionTag,
                path: it.path,
                createTime: it.createTime,
                type: it.type,
                icon: it.icon,
            }
        })
    }
}

export async function addResource(resourceInfo: Partial<ResourceInfo>): Promise<BaseData> {
    let url = "/resource/addResource"
    return await post(url, resourceInfo)
}


export async function editResource(resourceInfo: ResourceInfo): Promise<BaseData> {
    let url = "/resource/editResource"
    return await post(url, resourceInfo)
}
