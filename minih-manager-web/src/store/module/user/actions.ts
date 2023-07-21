import {ActionTree} from "vuex";
import {UserInfo, UserState} from "./user-types";
import {RootState} from "../root-types";
import {SessionInfo} from "../../../api/login";

export const userActions: ActionTree<UserState, RootState> = {

    setSessionInfo({commit}, data: SessionInfo) {
        commit("setSessionInfo", data)
        commit("setToken", data.tokenPrefix + " " + data.tokenValue)
        commit("setRawToken", data.tokenValue)
    },
    setUserInfo({commit}, data: UserInfo) {
        commit("setUserInfo", data)
    }


}
