<template>
    <el-container class="user-list">
        <div class="operation">
            <el-button type="primary" @click="editInfo.value={type:'01'};drawer=true;drawerType=1">新增顶级菜单
            </el-button>
        </div>
        <div class="table">
            <el-table v-loading="loading" :data="tableData" height="100%" row-key="id" style="width: 100%">
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
                <el-table-column align="center" label="类型" prop="type">
                    <template #default="scope">
                        <el-tooltip
                            :content="scope.row.type"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-tag v-if="scope.row.type === '01'" style="cursor:pointer;" type="success">菜单组
                            </el-tag>
                            <el-tag v-if="scope.row.type === '02'" style="cursor:pointer;" type="warning">主菜单
                            </el-tag>
                            <el-tag v-if="scope.row.type === '03'" style="cursor:pointer;" type="danger">组件内菜单
                            </el-tag>
                            <el-tag style="cursor:pointer">组件</el-tag>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="路径" prop="path"/>
                <el-table-column align="center" label="权限标志" prop="path">
                    <template #default="scope">
                        <el-popover :width="100" placement="right" trigger="hover">
                            <template #reference>
                                <el-link>权限标志</el-link>
                            </template>
                            <div v-for="o in scope.row.permissionTag" :key="o" class="text item">
                                <el-tag>{{ o }}</el-tag>
                            </div>
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
                                       @click="editInfo.value=scope.row;drawer=true;drawerType=4"/>
                        </el-tooltip>
                        <el-tooltip
                            content="增加子菜单"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            <el-button :disabled="scope.row.type === '03'" :icon="AddLocation" circle size="small"
                                       type="warning"
                                       @click="editInfo.value={parentId:scope.row.id,type:(scope.row.type === '01'?'02':null)};drawer=true;drawerType=(scope.row.type === '01'?2:3)"/>
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
        <edit-form :edit-info="editInfo.value" :type="drawerType" @close="drawer = false"
                   @update="queryResourceList"></edit-form>
    </el-drawer>


</template>

<script lang="ts" setup>
import {ref} from "vue";
import {AddLocation, Edit} from "@element-plus/icons-vue";
import EditForm from "./EditForm.vue";
import {useStore} from "../../store";
import {ResourceInfo} from "../../store/module/resource";


const tableData = ref<ResourceInfo []>()
const drawer = ref<Boolean>(false)
const drawerType = ref<number>(1)
const loading = ref<Boolean>(true)
const editInfo = ref<RoleInfo | {}>({})
const store = useStore()


const queryResourceList = async () => {
    loading.value = true
    try {
        tableData.value = await store.dispatch("resource/getOrLoad", true)
        loading.value = false
    } catch (_) {
        loading.value = false
    }
}
queryResourceList()


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