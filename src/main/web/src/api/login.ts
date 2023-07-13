import {post} from "../utils/http";
import {store} from "../store";

export interface FormInfo {
    username?: string | undefined
    password?: string | undefined
    mobile?: string | undefined;
    code?: string
}

export interface SessionInfo {
    tokenValue: string | undefined
    tokenName: string | undefined
    loginId: string | undefined
    expired: number | undefined
    loginDevice: string | undefined
    tokenPrefix: string | undefined
}


export async function login(form: FormInfo): Promise<SessionInfo> {
    let url = "/auth/login"
    let res = await post(url, form, false)
    console.log(res)
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