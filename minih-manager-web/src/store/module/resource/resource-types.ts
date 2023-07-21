export interface ResourceInfo {
    id: string
    name: string
    state: number
    parentId: string
    permissionTag: string[]
    path: string
    type: string
    icon: string
    createTime: number
}

export interface ResourceState {
    cache: ResourceInfo[]
}