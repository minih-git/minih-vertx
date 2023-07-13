import {SessionInfo} from "../../../api/login.ts";
import {UserInfo} from "./user-info.ts";

export const userState = () => ({
    token: String,
    sessionInfo: SessionInfo,
    userInfo: UserInfo,
})
