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
    createTime: number,
    mobile: string
    online: number
    idNo: string,
}


export type UserInfo = Optional<SysUser, 'password' | 'createTime' | 'online' | 'roleInfos'>


export interface UserState {
    token: string
    rawToken: string
    sessionInfo: Partial<SessionInfo>
    userInfo: Partial<UserInfo>
}
