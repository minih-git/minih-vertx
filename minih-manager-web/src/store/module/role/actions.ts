import {ActionTree} from "vuex";
import {RootState} from "../root-types";
import {RoleInfo, RoleState} from "./role-types";

export const roleActions: ActionTree<RoleState, RootState> = {
    setRoleCacheList({commit}, data: RoleInfo []) {
        commit("setRoleCacheList", data)
    },


}