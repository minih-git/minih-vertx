import {BaseData, Page, post, get} from "../utils/http";
import {UserEdit, UserInfo} from "../store/module/user/user-types";


export async function userList(search: string): Promise<Page<UserInfo>> {
    let url = "/user/page"
    let res = await post(url, {"search": search})
    let data = res.data["data"]

    return {
        nextCursor: Number(res.data["nextCursor"]),
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
                lastActive: it.sysUser.lastActive,
                online: it.online,
            }
        })
    }
}

export async function addUser(userInfo: UserEdit): Promise<BaseData> {
    let url = "/user/addUser"
    return post(url, userInfo)
}

export async function checkUsername(username: string): Promise<BaseData> {
    let url = "/user/checkUsername"
    return get(url, {username})
}

export async function checkPassword(password: string): Promise<BaseData> {
    let url = "/user/checkPassword"
    return get(url, {password})
}

export async function checkMobile(mobile: string): Promise<BaseData> {
    let url = "/user/checkMobile"
    return get(url, {mobile})
}
