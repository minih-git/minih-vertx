import {post, get} from "../utils/http";
import {store} from "../store";
import router from '../router'
import {UserInfo} from "../store/module/user/user-types.ts";
import {ElMessage} from "element-plus";

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

export async function logout() {
    let url = "/auth/logout"
    await get(url)
    await store.dispatch("user/setSessionInfo", {})
    await store.dispatch("user/setUserInfo", {})
    ElMessage({
        message: "您已成功退出！",
        type: 'success',
    })
    await router.push({name: '登录'})
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
