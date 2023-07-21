import {stringify} from 'qs'
import {store} from "../store";
import {ElMessage} from "element-plus";
import router from '../router'


const baseUrl: string = "/api"

export interface BaseData {
    code: number,
    msg: string,
    data: JSON
}

export interface Page<T> {
    nextCursor: number
    data: T []
}

export interface RequestOptions extends RequestInit {
    url: string,
}

export class MinihError extends Error {
    code: number = 0
    msg: string = ""
    data: any = ""

    constructor(code: number, msg: string, data: any = "") {
        super(msg)
        this.msg = msg;
        this.code = code;
        this.data = data;
    }
}

class NotLoginError extends MinihError {
}

const processHeaders = (params, needAuth, headers, type: string) => {
    let header = headers || {
        "Content-Type": "application/json",
        "Authorization": store.state.user.token
    }
    if ((params instanceof FormData) || type === "GET") {
        header["Content-Type"] = "application/x-www-form-urlencoded"
    }
    if (!needAuth) {
        delete header["Authorization"]
    } else {
        if (store.state.user.token == "") {
            throw new NotLoginError(-11, "未能读取到有效 token！")
        }
    }
    return header
}
export const get = async (url: string, params = {}, needAuth = true, headers = undefined): Promise<BaseData> => {
    try {
        return request({
            method: "GET",
            url: url + "?" + stringify(params),
            headers: new Headers(processHeaders(params, needAuth, headers, "GET"))
        })
    } catch (e: any) {
        return globalHandleError(e)
    }
}
export const post = async (url: string, params = {}, needAuth = true, headers = undefined): Promise<BaseData> => {
    try {
        return request({
            method: "POST",
            url: url,
            body: JSON.stringify(params),
            headers: new Headers(processHeaders(params, needAuth, headers, "POST"))
        })
    } catch (e: any) {
        return globalHandleError(e)
    }

}
export const request = async (options: RequestOptions): Promise<BaseData> => {
    let resultData = {
        code: -1,
        data: JSON,
        msg: "服务器发生错误，请稍后重试！"
    }
    try {
        let res = await fetch(baseUrl + "" + options.url, options)
        let resultJson = await res.json()
        if (res.status == 200) {
            if (resultJson.code == 0) {
                resultData.code = resultJson.code
                resultData.msg = resultJson.msg
                if (resultJson.data) {
                    resultData.data = JSON.parse(JSON.stringify(resultJson.data))
                }
                return resultData
            }
            let errorMsg = resultJson.msg
            if (resultJson.data) {
                errorMsg = errorMsg + "，" + resultJson.data
            }
            if (resultJson.code <= -10 && resultJson.code >= -20) {
                throw new NotLoginError(resultJson.code, errorMsg)
            }
            throw new MinihError(resultJson.code, errorMsg)
        }
        if (resultJson.status == 404) {
            resultData.msg = "地址错误"
        }

        if (resultJson.status == 500) {
            resultData.msg = "服务器错误"
        }
    } catch (e) {
        return globalHandleError(e)
    }
    return resultData


}
const globalHandleError = async (e) => {
    let errData: MinihError = new MinihError(
        -1,
        "服务器发生错误，请稍后重试！"
    )
    if (e instanceof MinihError) {
        errData.code = e.code
        errData.msg = e.msg
    }
    ElMessage({
        message: errData.code + "，" + errData.msg,
        type: 'warning',
        grouping: true,
    })
    if (e instanceof NotLoginError) {
        await router.push({name: '登录'})
    }

    return new Promise<BaseData>((_, reject) => reject(errData))
}
