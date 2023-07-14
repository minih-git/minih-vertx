import {SessionInfo} from "../../../api/login";

export interface UserInfo {
    username?: string
    name?: string
    avatar?: string
    state?: Number
    role?: string[]
    mobile?: Date
}

export interface UserState {
    token?: string
    sessionInfo?: SessionInfo
    userInfo?: UserInfo
}
