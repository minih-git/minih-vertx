import {MutationTree} from "vuex";
import {UserInfo, UserState} from "./user-types";
import {RootState} from "../root-types";
import {SessionInfo} from "../../../api";

export const userMutations: MutationTree<UserState, RootState> = {
    setSessionInfo(state: UserState, data: SessionInfo) {
        state.sessionInfo = data
    },
    setToken(state: UserState, data: string) {
        state.token = data
    },
    setRawToken(state: UserState, data: string) {
        state.rawToken = data
    },
    setUserInfo(state: UserState, data: UserInfo) {
        state.userInfo = data
    }

}
