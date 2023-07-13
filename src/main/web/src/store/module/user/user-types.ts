import {SessionInfo} from "../../../api/login";

export interface UserInfo {
    username: string | undefined
    name: string | undefined
    avatar: string | undefined
    state: Number | undefined
    role: string | undefined
    mobile: Date | undefined
}

export interface UserState {
    token: string
    sessionInfo: SessionInfo  | undefined
    userInfo: UserInfo | undefined
}