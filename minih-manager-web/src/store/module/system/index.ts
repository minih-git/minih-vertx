import {Module} from "vuex";
import {RootState} from "../root-types";
import {systemActions} from "./actions.ts";
import {systemMutations} from "./mutations.ts";
import {SystemState} from "./system-types.ts";
import {systemState} from "./state.ts";

export * from './system-types'

export const systemModule: Module<SystemState, RootState> = {
    namespaced: true,
    state: systemState,
    actions: systemActions,
    mutations: systemMutations
}
