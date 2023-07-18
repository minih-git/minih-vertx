import {post} from "../utils/http";
import {Page, UserInfo} from "../store/module/user/user-types";


export async function userList(search: string): Promise<Page<UserInfo>> {
    let url = "/user/page"
    let res = await post(url, {"search": search})
    let data = res.data["data"]

    return {
        nextCursor: res.data["nextCursor"],
        data: data.map(it => {
            return {
                id: it.sysUser._id,
                name: it.sysUser.name,
                username: it.sysUser.username,
                avatar: it.sysUser.avatar,
                state: it.sysUser.state,
                role: it.sysUser.role,
                mobile: it.userExtra.mobile,
                createTime: it.sysUser.createTime,
                online: it.online,
            }
        })
    }
}