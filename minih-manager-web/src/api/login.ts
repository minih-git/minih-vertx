import {post, get} from "../utils/http";
import {store} from "../store";
import router from '../router'
import {UserInfo} from "../store/module/user";
import {ElMessage} from "element-plus";

export interface FormInfo {
    username: string
    password: string
    mobile: string
    code: string
}

export interface SessionInfo {
    tokenValue: string
    tokenName: string
    loginId: string
    expired: number
    loginDevice: string
    tokenPrefix: string
}


export async function login(form: Partial<FormInfo>): Promise<SessionInfo> {
    let url = "/auth/login"
    let res = await post(url, form, false)
    let sessionInfo = {
        tokenValue: res.data['tokenValue'],
        expired: res.data['expired'],
        loginDevice: res.data['loginDevice'],
        loginId: res.data['loginId'],
        tokenName: res.data['tokenName'],
        tokenPrefix: res.data['tokenPrefix'],
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
    let userInfo = {
        username: res.data["sysUser"]["username"],
        name: res.data["sysUser"]["name"],
        avatar: res.data["sysUser"]["avatar"],
        state: res.data["sysUser"]["state"],
        role: res.data["sysUser"]["role"],
        mobile: res.data["userExtra"]["mobile"],
        idNo: res.data["userExtra"]["idNo"],
        idType: res.data["userExtra"]["idType"],
    }
    await store.dispatch("user/setUserInfo", userInfo)
    return userInfo
}