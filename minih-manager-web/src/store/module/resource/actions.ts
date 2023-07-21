import {ActionTree} from "vuex";
import {RootState} from "../root-types";
import {ResourceInfo, ResourceState} from "./resource-types";
import {resourceList} from "../../../api";

const queryResource = async (resourceOptions: ResourceInfo[], nextCursor: number) => {
    const resources = await resourceList('', nextCursor);
    resourceOptions.push(...resources.data)
    if (resources.nextCursor != 0) {
        await queryResource(resourceOptions, resources.nextCursor)
    }
}
export const resourceActions: ActionTree<ResourceState, RootState> = {
    async getOrLoad({commit, state}, forceRefresh: boolean = false): Promise<ResourceInfo[]> {
        if (state.cache.length != '' && !forceRefresh) {
            return state.cache
        }
        const resourceInfos: ResourceInfo[] = []
        await queryResource(resourceInfos, 0)
        commit("setResourceCacheList", resourceInfos)
        return resourceInfos
    },
}