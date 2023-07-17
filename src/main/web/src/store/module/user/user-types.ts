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
export interface Page<T> {
    nextCursor?: string
    data: T []
}
export interface UserState {
    token?: string
    sessionInfo?: SessionInfo
    userInfo?: UserInfo
}