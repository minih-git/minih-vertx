<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
    <el-form-item label="资源id" prop="id">
      <el-input v-model.trim="formData.id" :disabled="true"/>
    </el-form-item>
    <el-form-item v-show="type !== 1" label="父节点" prop="parentId">
      <el-input v-model.trim="formData.parentId" :disabled="true"/>
    </el-form-item>
    <el-form-item label="资源名称" prop="name">
      <el-input v-model.trim="formData.name"/>
    </el-form-item>
    <el-form-item label="资源类型" prop="type">
      <el-radio-group v-model="formData.type">
        <el-radio :disabled="type!==1" label="01" size="large">菜单组</el-radio>
        <el-radio :disabled="type!==2" label="02" size="large">主菜单</el-radio>
        <el-radio :disabled="type===1 ||type===2|| type===4" label="03" size="large">组件内菜单</el-radio>
        <el-radio :disabled="type===1 ||type===2|| type===4" label="04" size="large">组件</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="菜单路径" prop="path">
      <el-input v-model.trim="formData.path" :disabled="formData.type === '04'"/>
    </el-form-item>
    <el-form-item label="菜单图标" prop="icon">
      <el-input v-model.trim="formData.icon" :disabled="formData.type === '04'"/>
    </el-form-item>
    <el-form-item label="权限标志" prop="permissionTag">
      <el-select
          v-model="formData.permissionTag"
          :reserve-keyword="false"
          allow-create
          default-first-option
          filterable
          multiple
          placeholder="权限标志"
      >
        <el-option
            label="基础权限"
            value="basic"
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
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {anyIsEqual} from "../../utils";
import {ResourceInfo} from "../../store/module/resource";
import {addResource, editResource} from "../../api";

const props = defineProps<{
  type: number
  editInfo: Partial<ResourceInfo>
}>()
const isLoading = ref(false)
let formData = ref<Partial<ResourceInfo>>( {
  id: "",
  name: "",
  state: 1,
  parentId: "",
  permissionTag: [],
  path: "",
  type: "",
  icon: "",
})
formData.value = JSON.parse(JSON.stringify(props.editInfo))
watch(() => props.editInfo, it => {
  formData.value = JSON.parse(JSON.stringify(it))
})


const formRef = ref<FormInstance>()
const emit = defineEmits(["close", "update"])


const rules = reactive<FormRules<Partial<ResourceInfo>>>({
  name: [
    {required: true, message: '请输入资源名称', trigger: 'blur'},
    {min: 2, max: 11, message: '角色名称最大长度 3 到 10 位', trigger: 'blur'},
  ],
  type: [
    {required: true, message: '请选择菜单类型', trigger: 'blur'},
  ],
})

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
        if (props.type === 1 || props.type === 2 || props.type === 3) {
          if (props.type === 1) {
            formData.value.parentId = "0"
          }
          await addResource(formData.value)
          ElMessage.success('新增成功')
          emit("update")
        } else {
          let update = JSON.parse(JSON.stringify(formData.value))
          Object.keys(update).map(key => {
            if (key != "id" &&
                (anyIsEqual(props.editInfo[key], update[key]))) {
              delete update[key]
            }
          })
          if (Object.keys(update).length > 1) {
            await editResource(update)
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


</script>

<style scoped>

</style>
