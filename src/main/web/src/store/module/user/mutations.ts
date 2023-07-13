import {MutationTree} from "vuex";
import {UserState} from "./user-types";
import {RootState} from "../root-types";
import {SessionInfo} from "../../../api/login";

export const userMutations: MutationTree<UserState, RootState> = {
    setSessionInfo(state, data: SessionInfo) {
        state.sessionInfo = data
    },
    setToken(state, data: string) {
        state.token = data
    }

}