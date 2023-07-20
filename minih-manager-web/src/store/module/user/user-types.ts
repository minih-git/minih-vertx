import {SessionInfo} from "../../../api/login";

export interface UserInfo {
    username?: string
    name?: string
    avatar?: string
    state?: number
    role?: string[]
    createTime?: number
    mobile?: string
    online?: number
}
export interface UserEdit {
    username?: string
    name?: string
    avatar?: string
    state?: number
    role?: string[]
    password?: string
    mobile?: string
    idType?: string
    idNo?: string
}

export interface RoleInfo {
    name?: string
    state?: number
    resource?: string[]
    roleId?: number
    createTime?: number
}

export interface UserState {
    token?: string
    sessionInfo?: SessionInfo
    userInfo?: UserInfo
}
