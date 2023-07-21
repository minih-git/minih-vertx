import {MutationTree} from "vuex";
import {ResourceInfo, ResourceState} from "./resource-types";
import {RootState} from "../root-types";

export const roleMutations: MutationTree<ResourceState, RootState> = {
    setResourceCacheList(state: ResourceState, data: ResourceInfo []) {
        state.cache = data
    },
}