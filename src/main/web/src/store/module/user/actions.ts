import {ActionTree} from "vuex";
import {UserState} from "./user-types";
import {RootState} from "../root-types";
import {SessionInfo} from "../../../api/login";

export const userActions: ActionTree<UserState, RootState> = {

    setSessionInfo({commit}, data: SessionInfo) {
        commit("setSessionInfo", data)
        commit("setToken", data.tokenValue)
    }


}