export interface RoleInfo {
    name: string
    state: number
    resource: string[]
    roleId: string
    createTime: number
}

export interface RoleState {
    cache: RoleInfo[]
}
