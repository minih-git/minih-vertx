import {Module} from "vuex";
import {userState} from "./state.ts";
import {userActions} from "./actions.ts";
import {userMutations} from "./mutations.ts";
import {userGetters} from "./getters.ts";


export const index: Module<S, R> = {
    namespaced: true,
    state: userState,
    actions: userActions,
    mutations: userMutations,
    getters: userGetters


}
