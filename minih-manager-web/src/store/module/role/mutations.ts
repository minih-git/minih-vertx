import {MutationTree} from "vuex";
import {RoleInfo, RoleState} from "./role-types";
import {RootState} from "../root-types";

export const roleMutations: MutationTree<RoleState, RootState> = {
    setRoleCacheList(state: RoleState, data: RoleInfo []) {
        state.cache = data
    },
}