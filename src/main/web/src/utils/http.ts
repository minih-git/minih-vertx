import {stringify} from 'qs'

export interface BaseData {
    code: number,
    msg: String,
    data: {}
}

const baseUrl: String = "/api"

const processHeaders = (params, needAuth, headers) => {
    let header = headers || {
        "Content-Type": "application/json",
        "Authorization": ""
    }
    if (params instanceof FormData) {
        header["Content-Type"] = "application/x-www-form-urlencoded"
    }
    if (!needAuth) {
        delete header["Authorization"]
    }
    return headers
}

export const get = async (url: String, params = {}, needAuth = true, headers = undefined): Promise<BaseData> => {
    let options = {
        method: "GET",
        body: stringify(params),
        headers: new Headers(processHeaders(params, needAuth, headers))
    }

    let result = fetch(baseUrl + url, options)


}
