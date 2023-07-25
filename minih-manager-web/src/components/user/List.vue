<!--suppress TypeScriptValidateTypes -->
<template>
    <el-container class="user-list">
        <div class="operation">
            <el-button type="primary" @click="editInfo={};drawer=true">新增</el-button>
        </div>
        <div class="table">
            <el-table v-loading="loading" :data="tableData" height="100%" style="width: 100%">
                <el-table-column :index="(i)=>i+1" align="center" label="序号" type="index" width="80"/>
                <el-table-column align="center" label="名字" prop="name">
                    <template #default="scope">
                        <el-tooltip
                            :content="scope.row.id"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-tag style="cursor:pointer;">{{ scope.row.name }}</el-tag>
                        </el-tooltip>

                    </template>
                </el-table-column>
                <el-table-column align="center" label="状态" prop="state">
                    <template #default="scope">
                        <el-switch
                            :model-value="scope.row.state === 1"
                            active-text="正常"
                            inactive-text="封禁"
                            inline-prompt/>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="在  线" prop="state">
                    <template #default="scope">
                        <div style="align-items: center;justify-content: center;display: flex">
                            <div :style="{background:scope.row.online === 1?'#13ce66':'#ff4949'} "
                                 style="width: 10px;height: 10px;border-radius: 10px">
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="角色" prop="roleInfos">
                    <template #default="scope">
                        <el-popover :width="350" placement="right" trigger="hover">
                            <template #reference>
                                <el-tag effect="plain">
                                    <el-icon>
                                        <TrophyBase/>
                                    </el-icon>
                                    <span>角色列表</span>
                                </el-tag>
                            </template>
                            <el-table :data="scope.row.roleInfos">
                                <el-table-column align="center" label="角色名字" property="name" width="150"/>
                                <el-table-column align="center" label="角色标志" property="roleTag" width="180"/>
                            </el-table>
                        </el-popover>

                    </template>
                </el-table-column>
                <el-table-column align="center" label="最后活动时间" prop="createTime">
                    <template #default="scope">
                        <div style="display: flex; align-items: center;justify-content: center">
                            <el-icon>
                                <timer/>
                            </el-icon>
                            <span style="margin-left: 10px">{{
                                    DateFormat.format(new Date(scope.row.lastActive), 'yyyy-MM-dd HH:mm:ss')
                                }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="手机号" prop="mobile">
                    <template #default="scope">
                        <div style="display: flex; align-items: center;justify-content: center">
                            <el-icon>
                                <Iphone/>
                            </el-icon>
                            <span>{{ scope.row.mobile }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="操作" width="300">
                    <template #default="scope">
                        <el-tooltip
                            content="修改"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-button :icon="Edit" circle size="small" type="primary"
                                       @click="editInfo=scope.row;drawer=true"/>

                        </el-tooltip>
                        <el-tooltip
                            content="踢下线"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-button
                                :disabled="scope.row.online === 0"
                                :icon="Promotion"
                                circle
                                size="small"
                                type="warning" @click="kickOut(scope.row.username)"/>
                        </el-tooltip>
                        <el-tooltip
                            :content="scope.row.state === 1?'封禁账号':'解封账号'"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-button
                                v-if="scope.row.state === 1"
                                :icon="Remove"
                                circle
                                size="small"
                                type="danger" @click="lock(scope.row.username)"/>

                            <el-button
                                v-if="scope.row.state === 0"
                                :icon="Check"
                                circle
                                size="small"
                                type="success" @click="unlock(scope.row.username)"/>
                        </el-tooltip>
                    </template>
                </el-table-column>
            </el-table>

        </div>
    </el-container>
    <el-drawer
        v-model="drawer"
        direction="rtl"
    >
        <edit-form :edit-info="editInfo" @close="drawer = false" @update="queryUserList"></edit-form>
    </el-drawer>


</template>

<script lang="ts" setup>
import {ref} from "vue";
import {Check, Edit, Iphone, Promotion, Remove, Timer, TrophyBase} from "@element-plus/icons-vue";
import {UserInfo} from "../../store/module/user";
import {kickOut as kickOutApi, lock as lockApi, unlock as unlockApi, userList} from "../../api";
import {DateFormat} from "../../utils";
import EditForm from "./EditForm.vue";
import {useStore} from "../../store";
import {RoleInfo} from "../../store/module/role";


const tableData = ref<UserInfo []>()
const drawer = ref<Boolean>(false)
const loading = ref<Boolean>(true)
const editInfo = ref<UserInfo | {}>({})
const store = useStore()


const queryUserList = () => {
    loading.value = true
    try {
        userList("").then(async it => {
            const roles: RoleInfo[] = await store.dispatch("role/getOrLoad")
            it.data.map(it => {
                it.roleInfos = roles.filter(it1 => it.role.includes(it1.roleTag))
            })
            tableData.value = it.data
            loading.value = false
        })
    } catch (_) {
        loading.value = false
    }
}
queryUserList()

const kickOut = async (username: string) => {
    await kickOutApi(username)
    queryUserList()
}
const lock = async (username: string) => {
    await lockApi(username)
    queryUserList()
}
const unlock = async (username: string) => {
    await unlockApi(username)
    queryUserList()
}


</script>

<style scoped>
.user-list {
    flex-direction: column;

    .operation {
        padding: 10px;
        text-align: left;

        * {
            flex: none;
        }

    }
}


</style>
