<template>
  <el-container class="user-list">
    <div class="operation">
      <el-button type="primary" @click="drawer=true">新增</el-button>
    </div>
    <div class="table">
      <el-table :data="tableData" height="100%" style="width: 100%">
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
  <el-drawer
      v-model="drawer"
      title="新增"
      direction="rtl"
      size="50%"
  >
    <el-form label-position="top">

      <el-form-item label="头像" >
        <el-upload
            class="avatar-uploader"
            action="https://run.mocky.io/v3/9d059bf9-4660-45f2-925d-ce80ad6c4d15"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
        >
          <el-avatar :size="50" :src="imageUrl"/>
        </el-upload>
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.name"/>
      </el-form-item>
    </el-form>
  </el-drawer>


</template>

<script lang="ts" setup>
import {ref, reactive} from "vue";
import {Timer, Iphone, Edit, Remove, Promotion, Check} from "@element-plus/icons-vue";
import {UserInfo} from "../../store/module/user/user-types";
import {userList} from "../../api/user";
import {DateFormat} from "../../utils/utils.ts";
import {ElMessage, UploadProps} from "element-plus";

const imageUrl = ref('')


const tableData = ref<UserInfo []>()
const drawer = ref<Boolean>(false)

userList("").then(it => {
  tableData.value = it.data
})


const form = reactive<UserInfo>({
  username: "",
  name: "",
  avatar: "string",
  state: 1,
  role: [],
  mobile: "",
})

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg') {
    ElMessage.error('Avatar picture must be JPG format!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('Avatar picture size can not exceed 2MB!')
    return false
  }
  return true
}
const handleAvatarSuccess: UploadProps['onSuccess'] = () => {

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
