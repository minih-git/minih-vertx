<template>
    <el-container class="user-list">
        <div class="operation">
            <el-button type="primary">新增</el-button>
        </div>
        <div class="table">
            <el-table :data="tableData" height="250" style="width: 100%" >
                <el-table-column align="center" label="id" prop="id" width="50"/>
                <el-table-column align="center" label="名字"  prop="name">
                    <template #default="scope">
                        <el-tag>{{ scope.row.name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="状态"  prop="state"/>
                <el-table-column align="center" label="角色"  prop="role"/>
                <el-table-column align="center" label="创建时间"  prop="createTime">
                    <template #default="scope">
                        <div style="display: flex; align-items: center">
                            <el-icon>
                                <timer/>
                            </el-icon>
                            <span style="margin-left: 10px">{{ DateFormat.format(new Date(scope.row.createTime),'yyyy-MM-dd HH:mm:ss') }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="手机号"  prop="mobile"/>
                <el-table-column align="center"  label="操作" width="300">
                    <template #default="scope">
                        <el-button size="small" @click="handleEdit(scope.$index, scope.row)"
                        >修改</el-button
                        >
                        <el-button
                            size="small"
                            type="danger"
                            @click="handleDelete(scope.$index, scope.row)"
                        >删除</el-button
                        >
                        <el-button
                            size="small"
                            type="danger"
                            @click="handleDelete(scope.$index, scope.row)"
                        >下线</el-button
                        >
                        <el-button
                            size="small"
                            type="danger"
                            @click="handleDelete(scope.$index, scope.row)"
                        >封禁</el-button
                        >
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
import {Timer} from "@element-plus/icons-vue";
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
