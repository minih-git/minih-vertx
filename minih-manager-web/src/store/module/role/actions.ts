import {ActionTree} from "vuex";
import {RootState} from "../root-types";
import {RoleInfo, RoleState} from "./role-types";
import {roleList} from "../../../api";

const queryRoles = async (roleOptions: RoleInfo[], nextCursor: number) => {
    const roles = await roleList('', nextCursor);
    roleOptions.push(...roles.data)
    if (roles.nextCursor != 0) {
        await queryRoles(roleOptions, roles.nextCursor)
    }
}
export const roleActions: ActionTree<RoleState, RootState> = {
    async getOrLoad({commit, state}, forceRefresh: boolean = false): Promise<RoleInfo[]> {
        if (state.cache.length != '' && !forceRefresh) {
            return state.cache
        }
        const roles: RoleInfo[] = []
        await queryRoles(roles, 0)
        commit("setRoleCacheList", roles)
        return roles
    },
}