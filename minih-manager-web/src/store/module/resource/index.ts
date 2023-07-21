import {Module} from "vuex";
import {resourceState} from "./state.ts";
import {resourceActions} from "./actions.ts";
import {roleMutations} from "./mutations.ts";
import {ResourceState} from "./resource-types";
import {RootState} from "../root-types";

export * from './resource-types'
export const resourceModule: Module<ResourceState, RootState> = {
    namespaced: true,
    state: resourceState,
    actions: resourceActions,
    mutations: roleMutations
}