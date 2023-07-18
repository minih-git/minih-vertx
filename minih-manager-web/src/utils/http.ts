import {stringify} from 'qs'
import {store} from "../store";
// @ts-ignore
import {ElMessage} from "element-plus";


const baseUrl: string = "/api"

export interface BaseData {
    code: number,
    msg: string,
    data: JSON
}

export interface RequestOptions extends RequestInit {
    url: string,
}

class MinihError extends Error {
    code: number = 0
    msg: string = ""

    constructor(code: number, msg: string) {
        super(msg)
        this.msg = msg;
        this.code = code;
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
            url: url + stringify(params),
            headers: new Headers(processHeaders(params, needAuth, headers, "GET"))
        })
    } catch (e: any) {
        console.log(e)
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
    console.log(options)
    try {
        let res = await fetch(baseUrl + "" + options.url, options)
        let resultJson = await res.json()
        if (res.status == 200) {
            if (resultJson.code == 0) {
                resultData.code = resultJson.code
                resultData.msg = resultJson.msg
                resultData.data = JSON.parse(JSON.stringify(resultJson.data))
                return resultData
            }
            if (resultJson.code <= -9 && resultJson.code >= -18) {
                throw new NotLoginError(resultJson.code, resultJson.msg)
            }
            throw new MinihError(resultJson.code, resultJson.msg)
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

const globalHandleError = (e) => {
    console.log(e)
    let errData = {
        code: -1,
        data: JSON,
        msg: "服务器发生错误，请稍后重试！"
    }
    if (e instanceof MinihError) {
        errData.code = e.code
        errData.msg = e.msg
    }
    if (e instanceof NotLoginError && e.code == -11) {
        // window.location.href = "/login"
    }
    ElMessage({
        message: errData.code + "，" + errData.msg,
        type: 'warning',
    })
    return new Promise<BaseData>((_, reject) => reject(errData))
}
