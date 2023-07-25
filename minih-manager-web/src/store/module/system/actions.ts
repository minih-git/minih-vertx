import {ActionTree} from "vuex";
import {SystemState} from "./system-types.ts";
import {RootState} from "../root-types";

export const systemActions: ActionTree<SystemState, RootState> = {

    async getOrLoadSecret({commit, state}, forceRefresh: boolean = false) {
        if (state.secret.length != '' && !forceRefresh) {
            return state.secret
        }
        let res = await fetch("/api/options")
        let resultJson = await res.json()
        commit("setSecret", resultJson.data.se)
        return resultJson.data.se
    },


}
