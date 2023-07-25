import {SessionInfo} from "../../../api";
import {Optional} from "../../../utils";
import {RoleInfo} from "../role";

export interface SysUser {
    username: string
    password: string
    name: string
    avatar: string
    state: number
    role: string[]
    roleInfos: RoleInfo[]
    createTime: number
}

export interface UserExtra {
    mobile: string
    online: number
    idType: string,
    idNo: string,
}

export type UserInfoExpand = SysUser & UserExtra
export type UserInfo = Optional<UserInfoExpand, 'password' | 'createTime' | 'online' | 'roleInfos'>


export interface UserState {
    token: string
    rawToken: string
    sessionInfo: Partial<SessionInfo>
    userInfo: Partial<UserInfo>
}
