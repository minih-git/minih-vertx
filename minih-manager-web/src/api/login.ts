import {get, post} from "../utils";
import {store} from "../store";
import router from '../router'
import {UserInfo} from "../store/module/user";
import {ElMessage} from "element-plus";
import {MessageOptions} from "element-plus/es/components/message/src/message";

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


export const login = async (form: Partial<FormInfo>): Promise<SessionInfo> => {
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
export const submitGetCode = async (form: Partial<FormInfo>) => {
    let url = "/system/vCode"
    await post(url, form, false)
}

export const logout = async () => {
    let url = "/auth/logout"
    await get(url)
    await logoutHandler()
}

export const info = async (): Promise<UserInfo> => {
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

const logoutHandler = async (code: number = 0, message: string = "您已成功退出！") => {
    await store.dispatch("user/setSessionInfo", {})
    await store.dispatch("user/setUserInfo", {})
    const msg: MessageOptions = {
        message,
        type: 'success',
    }
    if (code != 0) {
        msg.type = "warning"
    }
    ElMessage(msg)
    await router.push({name: '登录'})
}
export const authEventBusHandler = () => {
    // @ts-ignore
    const eb = new window.EventBus("/ws/authEventbus")
    eb.onopen = () => {
        eb.registerHandler('cn.minih.auth.session.offline', async (_, message) => {
            if (message.body.token == store.state.user.rawToken) {
                await logoutHandler(message.body.type, message.body.msg)
            }
        });
    }


}