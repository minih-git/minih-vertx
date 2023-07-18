<template>
    <el-container class="user-list">
        <div class="operation">
            <el-button type="primary">新增</el-button>
        </div>
        <div class="table">
            <el-table :data="tableData" height="250" style="width: 100%">
                <el-table-column :index="(i)=>i+1" align="center" label="id" prop="id" type="index" width="50">
                    <template #default="scope">
                        <el-tooltip
                            :content="scope.row.id"
                            placement="top-start"
                            style="cursor: pointer"
                        >
                            {{ scope.$index + 1 }}
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="名字" prop="name">
                    <template #default="scope">
                        <el-tag>{{ scope.row.name }}</el-tag>
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
                <el-table-column align="center" label="在线" prop="state">
                    <template #default="scope">
                        <div style="align-items: center;justify-content: center;display: flex">
                            <div :style="{background:scope.row.online === 1?'#13ce66':'#ff4949'} "
                                 style="width: 10px;height: 10px;border-radius: 10px">
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="角色" prop="role"/>
                <el-table-column align="center" label="创建时间" prop="createTime">
                    <template #default="scope">
                        <div style="display: flex; align-items: center;justify-content: center">
                            <el-icon>
                                <timer/>
                            </el-icon>
                            <span style="margin-left: 10px">{{
                                    DateFormat.format(new Date(scope.row.createTime), 'yyyy-MM-dd HH:mm:ss')
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
                                       @click="handleEdit(scope.$index, scope.row)"/>

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
                                type="warning" @click="handleDelete(scope.$index, scope.row)"/>
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
                                type="danger" @click="handleDelete(scope.$index, scope.row)"/>

                            <el-button
                                v-if="scope.row.state === 0"
                                :icon="Check"
                                circle
                                size="small"
                                type="success" @click="handleDelete(scope.$index, scope.row)"/>
                        </el-tooltip>
                    </template>
                </el-table-column>
            </el-table>

        </div>
    </el-container>

    <el-container>

    </el-container>

    <el-container>

    </el-container>


</template>

<script lang="ts" setup>
import {ref} from "vue";
import {Timer, Iphone, Edit, Remove, Promotion,Check} from "@element-plus/icons-vue";
import {UserInfo} from "../../store/module/user/user-types";
import {userList} from "../../api/user";
import {DateFormat} from "../../utils/utils.ts";


const tableData = ref<UserInfo []>()

userList("").then(it => {
    console.log(it)
    tableData.value = it.data
})


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