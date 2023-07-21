import {createStore, Store, useStore as baseUseStore} from "vuex";
import {RootState} from "./module/root-types";
import {InjectionKey} from "vue";
import {userModule} from './module/user'
import createPersistedState from 'vuex-persistedstate'
import {roleModule} from "./module/role";
import {resourceModule} from "./module/resource";

export const key: InjectionKey<Store<RootState>> = Symbol()

export const store = createStore<RootState>({
    modules: {
        user: userModule,
        role: roleModule,
        resource: resourceModule
    },
    plugins: [createPersistedState({
        storage: window.sessionStorage,
    })]
})

// 定义自己的 `useStore` 组合式函数
export function useStore() {
    return baseUseStore(key)
}