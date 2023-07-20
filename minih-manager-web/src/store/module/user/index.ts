import {Module} from "vuex";
import {userState} from "./state.ts";
import {userActions} from "./actions.ts";
import {userMutations} from "./mutations.ts";
import {UserState} from "./user-types";
import {RootState} from "../root-types";

export * from './user-types'

export const userModule: Module<UserState, RootState> = {
    namespaced: true,
    state: userState,
    actions: userActions,
    mutations: userMutations
}