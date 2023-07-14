import {post, get} from "../utils/http";
import {store} from "../store";
import {UserInfo} from "../store/module/user/user-types.ts";

export interface FormInfo {
    username?: string
    password?: string
    mobile?: string
    code?: string
}

export interface SessionInfo {
    tokenVale?: string
    tokenName?: string
    loginId?: string
    expired?: number
    loginDevice?: string
    tokenPrefix?: string
}


export async function login(form: FormInfo): Promise<SessionInfo> {
    let url = "/auth/login"
    let res = await post(url, form, false)
    let sessionInfo = {
        expired: res.data['expired'],
        loginDevice: res.data['loginDevice'],
        loginId: res.data['loginId'],
        tokenName: res.data['tokenName'],
        tokenPrefix: res.data['tokenPrefix'],
        tokenValue: res.data['tokenValue']
    }
    await store.dispatch("user/setSessionInfo", sessionInfo)
    return sessionInfo
}

export async function info(): Promise<UserInfo> {
    let url = "/user/info"
    let res = await get(url, {})
    console.log(res)
    let userInfo = {
        username: res.data["sysUser"]["username"],
        name: res.data["sysUser"]["name"],
        avatar: res.data["sysUser"]["avatar"],
        state: res.data["sysUser"]["state"],
        role: res.data["sysUser"]["role"],
        mobile: res.data["userExtra"]["mobile"],
    }
    await store.dispatch("user/setUserInfo", userInfo)
    return userInfo
}
