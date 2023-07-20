<template>
  <el-form label-position="top" ref="formRef" :model="formData" :rules="rules">
    <el-form-item label="头像" prop="avatar">
      <el-upload
          class="avatar-uploader"
          action="https://run.mocky.io/v3/9d059bf9-4660-45f2-925d-ce80ad6c4d15"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
      >
        <el-avatar :size="50" :src="formData.avatar"/>
      </el-upload>
      <el-input v-model="formData.avatar" placeholder="头像地址">
        <template #prepend>Http://</template>
      </el-input>
    </el-form-item>
    <el-form-item label="账户" prop="username">
      <el-input v-model.trim="formData.username"/>
    </el-form-item>
    <el-form-item label="姓名" prop="name">
      <el-input v-model.trim="formData.name"/>
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input v-model.trim="formData.password"/>
    </el-form-item>
    <el-form-item label="手机号" prop="mobile">
      <el-input v-model.trim="formData.mobile"/>
    </el-form-item>
    <el-form-item label="证件类型" prop="idType">
      <el-radio-group v-model="formData.idType" class="ml-4">
        <el-radio label="01" size="large">身份证</el-radio>
        <el-radio label="02" size="large">护照</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="证件号码" prop="idNo">
      <el-input v-model.trim="formData.idNo"/>
    </el-form-item>
    <el-form-item label="角色" prop="role">
      <el-select
          v-model="formData.role"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择"
          style="width: 240px"
      >
        <el-option
            v-for="item in roleOptions"
            :key="item.roleId"
            :label="item.name"
            :value="item.roleId"
        />
      </el-select>
    </el-form-item>
    <div class="footer-con">
      <el-button type="warning" @click="cancelForm(formRef)">取消</el-button>
      <el-button type="primary" :loading="isLoading" @click="submit(formRef)">
        提交
      </el-button>
    </div>
  </el-form>

</template>
<script setup lang="ts">

import {reactive, ref} from "vue";
import {ElMessage, FormInstance, FormRules, UploadProps} from "element-plus";
import {roleList} from "../../api/role";
import {RoleInfo, UserEdit} from "../../store/module/user/user-types";
import {addUser, checkMobile, checkPassword, checkUsername} from "../../api/user";
import {MinihError} from "../../utils/http.ts";

const isLoading = ref(false)
const formData = reactive<UserEdit>({
  username: "hubin",
  name: "胡斌",
  avatar: "",
  state: 1,
  role: ['role_1'],
  mobile: "15999603031",
  password: null,
  idType: "01",
  idNo: "430421199502072114",
})
const roleOptions = reactive<RoleInfo []>([])
const formRef = ref<FormInstance>()
const emit = defineEmits(["close"])

const mobileCheck = async (rule: any, value: any, callback: any) => {
  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('电话号码格式错误'))
    return
  }
  try {
    await checkMobile(value.toString())
    callback()
  } catch (e) {
    if (e instanceof MinihError) {
      callback(new Error(e.msg))
      return
    }
    callback(new Error("电话号码校验不通过"))
  }
}
const idCheck = (rule: any, value: any, callback: any) => {
  if (formData.idType !== "01") {
    callback()
    return
  }
  let city = {
    11: '北京',
    12: '天津',
    13: '河北',
    14: '山西',
    15: '内蒙古',
    21: '辽宁',
    22: '吉林',
    23: '黑龙江',
    31: '上海',
    32: '江苏',
    33: '浙江',
    34: '安徽',
    35: '福建',
    36: '江西',
    37: '山东',
    41: '河南',
    42: '湖北',
    43: '湖南',
    44: '广东',
    45: '广西',
    46: '海南',
    50: '重庆',
    51: '四川',
    52: '贵州',
    53: '云南',
    54: '西藏',
    61: '陕西',
    62: '甘肃',
    63: '青海',
    64: '宁夏',
    65: '新疆',
    71: '台湾',
    81: '香港',
    82: '澳门',
    91: '国外',
  };
  if (!value) {
    callback();
    return
  }
  if (!value || !/^\d{6}(18|19|20)?\d{2}(0[1-9]|1[012])(0[1-9]|[12]\d|3[01])\d{3}(\d|X)$/i.test(value)
  ) {
    callback(new Error('请输入正确的身份证号'));
    return
  } else if (!city[value.slice(0, 2)]) {
    callback(new Error('请输入正确的地址编码'));
    return
  } else {
    if (value.length == 18) {
      value = value.split('');
      let factor = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
      let parity = [1, 0, 'X', 9, 8, 7, 6, 5, 4, 3, 2];
      let sum = 0;
      let ai = 0;
      let wi = 0;
      for (let i = 0; i < 17; i++) {
        ai = value[i];
        wi = factor[i];
        sum += ai * wi;
      }
      if (parity[sum % 11] != value[17]) {
        callback(new Error('校验位错误'));
        return
      } else {
        callback();
        return
      }
    }
  }
}
const userNameCheck = async (rule: any, value: any, callback: any) => {
  try {
    await checkUsername(value.toString())
    callback()
    return
  } catch (e) {
    if (e instanceof MinihError) {
      callback(new Error(e.msg))
      return
    }
    callback(new Error("用户名校验不通过"))
    return
  }

}

const passwordCheck = async (rule: any, value: any, callback: any) => {
  if (!value) {
    callback();
    return
  }
  try {
    await checkPassword(value.toString())
    callback()
    return
  } catch (e) {
    if (e instanceof MinihError) {
      callback(new Error(e.msg))
      return
    }
    callback(new Error("密码校验不通过"))
    return
  }

}


const rules = reactive<FormRules<UserEdit>>({
  name: [
    {required: true, message: '请输入姓名', trigger: 'blur'},
    {min: 2, max: 5, message: '姓名最大长度 3 到 5 位', trigger: 'blur'},
  ],
  username: [
    {required: true, message: '请输入登录用户名', trigger: 'blur'},
    {min: 3, max: 20, message: '用户名最大长度 3 到 20 位', trigger: 'blur'},
    {asyncValidator: userNameCheck, trigger: 'blur'},
  ],
  password: [
    {asyncValidator: passwordCheck, trigger: 'blur'},
  ],
  mobile: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {asyncValidator: mobileCheck, trigger: 'blur'},
  ],
  idNo: [
    {required: true, message: '请输入证件号码', trigger: 'blur'},
    {validator: idCheck, trigger: 'blur'},
  ],

})


const queryRoles = async (nextCursor) => {
  const roles = await roleList('', nextCursor);
  roleOptions.push(...roles.data)
  if (roles.nextCursor != 0) {
    await queryRoles(roles.nextCursor)
  }
}
queryRoles(0)

const cancelForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.resetFields()
  emit('close')
}
const submit = async (formEl: FormInstance | undefined) => {
  isLoading.value = true
  formEl.validate(async (valid) => {
    if (valid) {
      try {
        await addUser(formData)
        ElMessage.success('新增成功')
        emit('close')
      } catch (_) {
      }
    }
    isLoading.value = false
  })
}
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

</style>
