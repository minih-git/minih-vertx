import {SessionInfo} from "../../../api/login";
import {Optional} from "../../../utils/utils";

export interface SysUser {
    username: string
    password: string
    name: string
    avatar: string
    state: number
    role?: string[]
    createTime: number
}

export interface UserExtra {
    mobile: string
    online: number
    idType: string,
    idNo: string,
}

export type UserInfoExpand = SysUser & UserExtra
export type UserInfo = Optional<UserInfoExpand, 'password' | 'createTime' | 'online'>



export interface UserState {
    token: string
    sessionInfo: Partial<SessionInfo>
    userInfo: Partial<UserInfo>
}