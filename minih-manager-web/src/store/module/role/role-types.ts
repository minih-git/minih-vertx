export interface RoleInfo {
    name: string
    state: number
    resource: string[]
    roleId: number
    createTime: number
}

export interface RoleState {
    cache: RoleInfo[]
}