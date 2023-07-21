export interface RoleInfo {
    id: string
    name: string
    state: number
    resources: string[]
    resourceInfos: string[]
    roleTag: string
    createTime: number
}

export interface RoleState {
    cache: RoleInfo[]
}