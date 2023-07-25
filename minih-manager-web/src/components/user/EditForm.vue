<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
    <el-form-item label="头像" prop="avatar">
      <el-upload
          :before-upload="beforeAvatarUpload"
          :on-success="handleAvatarSuccess"
          :show-file-list="false"
          action="https://run.mocky.io/v3/9d059bf9-4660-45f2-925d-ce80ad6c4d15"
          class="avatar-uploader"
      >
        <el-avatar :size="50" :src="formData.avatar"/>
      </el-upload>
      <el-input v-model="formData.avatar" placeholder="头像地址">
        <template #prepend>url地址：</template>
      </el-input>
    </el-form-item>
    <el-form-item label="账户" prop="username">
      <el-input v-model.trim="formData.username" :disabled="edit"/>
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
      <el-radio-group v-model="formData.idType">
        <el-radio label="01" size="large">身份证</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="证件号码" prop="idNo">
      <el-input v-model.trim="formData.idNo"/>
    </el-form-item>
    <el-form-item label="角色" prop="role">
      <el-select
          v-model="formData.role"
          collapse-tags
          collapse-tags-tooltip
          multiple
          placeholder="请选择"
          style="width: 240px"
      >
        <el-option
            v-for="item in roleOptions"
            :key="item.id"
            :label="item.name"
            :value="item.roleTag"
        />
      </el-select>
    </el-form-item>
    <div class="footer-con">
      <el-button type="warning" @click="cancelForm(formRef)">取消</el-button>
      <el-button :loading="isLoading" type="primary" @click="submit(formRef)">
        提交
      </el-button>
    </div>
  </el-form>

</template>
<script lang="ts" setup>
import {reactive, ref, watch} from "vue";
import {ElMessage, FormInstance, FormRules, UploadProps} from "element-plus";
import {addUser, checkMobile, checkPassword, checkUsername, editUser} from "../../api";
import {RoleInfo} from "../../store/module/role";
import {anyIsEqual, idCheck, MinihError} from "../../utils";
import {UserInfo} from "../../store/module/user";
import {useStore} from "../../store";

const store = useStore()

const props = defineProps<{
  editInfo: Partial<UserInfo>
}>()
const edit = ref<boolean>(Object.keys(props.editInfo).length !== 0)

const isLoading = ref(false)
let formData = ref<UserInfo>({
  username: "hubin",
  name: "胡斌",
  avatar: "",
  state: 1,
  role: ['role_1'],
  mobile: "15999603031",
  idType: "01",
  idNo: "430421199502072114",
})
formData.value = JSON.parse(JSON.stringify(props.editInfo))
watch(() => props.editInfo, it => {
  formData.value = JSON.parse(JSON.stringify(it))
  edit.value = Object.keys(it).length !== 0
})

const userNameCheck = async (_: any, value: any, callback: any) => {
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
const passwordCheck = async (_: any, value: any, callback: any) => {
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


const roleOptions = reactive<RoleInfo []>([])
const formRef = ref<FormInstance>()
const emit = defineEmits(["close", "update"])

const mobileCheck = async (_: any, value: any, callback: any) => {
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
const rules = reactive<FormRules<Partial<UserInfo>>>({
  name: [
    {required: true, message: '请输入姓名', trigger: 'blur'},
    {min: 2, max: 5, message: '姓名最大长度 3 到 5 位', trigger: 'blur'},
  ],
  username: [
    {required: true, message: '请输入登录用户名', trigger: 'blur'},
    {min: 3, max: 20, message: '用户名最大长度 3 到 20 位', trigger: 'blur'},
    {
      asyncValidator: (rule: any, value: any, callback: any) => {
        if (props.editInfo.username == value) {
          callback()
          return
        }
        userNameCheck(rule, value, callback)
      }, trigger: 'blur'
    },
  ],
  password: [
    {asyncValidator: passwordCheck, trigger: 'blur'},
  ],
  mobile: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {
      asyncValidator: (rule: any, value: any, callback: any) => {
        if (props.editInfo.mobile == value) {
          callback()
          return
        }
        mobileCheck(rule, value, callback)
      }, trigger: 'blur'
    },
  ],
  idNo: [
    {required: true, message: '请输入证件号码', trigger: 'blur'},
    {
      validator: (rule: any, value: any, callback: any) => {
        if (formData.value.idType != "01" || props.editInfo.idNo == value) {
          callback()
          return
        }
        idCheck(rule, value, callback)
      }, trigger: 'blur'
    },
  ],

})

const queryRole = async () => {
  const roles = await store.dispatch("role/getOrLoad")
  roleOptions.push(...roles)
}
queryRole()


const cancelForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.resetFields()
  emit('close')
}
const submit = async (formEl: FormInstance | undefined) => {
  isLoading.value = true
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        if (!edit.value) {
          await addUser(formData.value)
          ElMessage.success('新增成功')
          emit("update")
        } else {
          let update = JSON.parse(JSON.stringify(formData.value))
          Object.keys(update).map(key => {
            if (key != "username" && (anyIsEqual(props.editInfo[key], update[key]))) {
              delete update[key]
            }
          })
          if (Object.keys(update).length > 1) {
            await editUser(update)
            ElMessage.success('修改成功')
            emit("update")
          }
        }
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
