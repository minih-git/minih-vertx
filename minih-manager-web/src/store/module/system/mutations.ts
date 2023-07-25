import {MutationTree} from "vuex";
import {RootState} from "../root-types";
import {SystemState} from "./system-types.ts";

export const systemMutations: MutationTree<SystemState, RootState> = {
    setSecret(state: SystemState, data: string) {
        state.secret = data
    }
}
