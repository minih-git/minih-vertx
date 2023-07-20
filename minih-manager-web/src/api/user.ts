import {BaseData, Page, post, get} from "../utils/http";
import {UserInfo} from "../store/module/user";


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
                idType: it.userExtra.idType,
                idNo: it.userExtra.idNo,
            }
        })
    }
}

export async function addUser(userInfo: UserInfo): Promise<BaseData> {
    let url = "/user/addUser"
    return post(url, userInfo)
}

export async function editUser(userInfo: UserInfo): Promise<BaseData> {
    let url = "/user/editUser"
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

export async function lock(username: string): Promise<BaseData> {
    let url = "/user/lock"
    return get(url, {username})
}

export async function unlock(username: string): Promise<BaseData> {
    let url = "/user/unlock"
    return get(url, {username})
}

export async function kickOut(username: string): Promise<BaseData> {
    let url = "/auth/kickOut"
    return get(url, {"loginId": username})
}