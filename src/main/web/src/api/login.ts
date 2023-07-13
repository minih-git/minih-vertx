export interface FormInfo {
    username: String | undefined
    password: String | undefined
    mobile: String | undefined
    code: String | undefined
}

export interface SessionInfo {
    tokenValue: String | undefined
    tokenName: String | undefined
    loginId: String | undefined
    expired: Number | undefined
    loginDevice: String | undefined
    tokenPrefix: String | undefined
}


export async function login(form: FormInfo): SessionInfo {
    let url = "/auth/login"

    return window.fetch()

}
