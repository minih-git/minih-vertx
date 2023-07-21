<template>
    <el-container class="user-list">
        <div class="operation">
            <el-button type="primary" @click="editInfo.value={};drawer=true">新增</el-button>
        </div>
        <div class="table">
            <el-table v-loading="loading" :data="tableData" height="100%" style="width: 100%">
                <el-table-column :index="(i)=>i+1" align="center" label="序号" type="index" width="80"/>
                <el-table-column align="center" label="名字" prop="name">
                    <template #default="scope">
                        <el-tooltip
                            :content="scope.row.name"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-tag style="cursor:pointer;">{{ scope.row.name }}</el-tag>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="拥有资源" prop="roleInfos">
                    <template #default="scope">
                        <el-popover :width="250" placement="right" trigger="hover">
                            <template #reference>
                                <el-tag effect="plain">
                                    <el-icon>
                                        <TrophyBase/>
                                    </el-icon>
                                    资源列表
                                </el-tag>
                            </template>
                            <el-table :data="scope.row.resourceInfos">
                                <el-table-column align="center" label="资源名字" property="name" width="150"/>
                            </el-table>
                        </el-popover>

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
                                       @click="editInfo.value=scope.row;drawer=true"/>

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
        <edit-form :edit-info="editInfo.value" @close="drawer = false" @update="queryRoleList"></edit-form>
    </el-drawer>


</template>

<script lang="ts" setup>
import {ref} from "vue";
import {Edit, TrophyBase} from "@element-plus/icons-vue";
import EditForm from "./EditForm.vue";
import {useStore} from "../../store";
import {RoleInfo} from "../../store/module/role";
import {ResourceInfo} from "../../store/module/resource";


const tableData = ref<RoleInfo []>()
const drawer = ref<Boolean>(false)
const loading = ref<Boolean>(true)
const editInfo = ref<RoleInfo | {}>({})
const store = useStore()


const queryRoleList = async () => {
    loading.value = true
    try {
        const roles = await store.dispatch("role/getOrLoad",false)
        const resourceInfos: ResourceInfo[] = await store.dispatch("resource/getOrLoad")
        roles.map(it => {
            it.resourceInfos = resourceInfos.filter(it1 => it.resources.includes(it1.id))
        })
        tableData.value = roles
        loading.value = false
    } catch (_) {
        loading.value = false
    }
}
queryRoleList()


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