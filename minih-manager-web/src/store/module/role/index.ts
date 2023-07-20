import {Module} from "vuex";
import {roleState} from "./state.ts";
import {roleActions} from "./actions.ts";
import {roleMutations} from "./mutations.ts";
import {RoleState} from "./role-types";
import {RootState} from "../root-types";

export * from './role-types'
export const roleModule: Module<RoleState, RootState> = {
    namespaced: true,
    state: roleState,
    actions: roleActions,
    mutations: roleMutations
}