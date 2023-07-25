<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
    <el-form-item label="角色标志" prop="roleTag">
      <el-input v-model.trim="formData.roleTag" :disabled="edit"/>
    </el-form-item>
    <el-form-item label="角色名称" prop="name">
      <el-input v-model.trim="formData.name"/>
    </el-form-item>

    <el-form-item label="资源" prop="role">
      <el-select
          v-model="formData.resources"
          collapse-tags
          collapse-tags-tooltip
          multiple
          placeholder="请选择"
          style="width: 240px"
      >
        <el-option
            v-for="item in resourceOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
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
import {useStore} from "../../store";
import {RoleInfo} from "../../store/module/role";
import {anyIsEqual, MinihError} from "../../utils";
import {addRole, checkRoleTag, editRole} from "../../api";
import {ResourceInfo} from "../../store/module/resource";

const store = useStore()
const props = defineProps<{
  editInfo: Partial<RoleInfo>
}>()
const edit = ref<boolean>(Object.keys(props.editInfo).length !== 0)
const isLoading = ref(false)
let formData = ref<Partial<RoleInfo>>({})
formData.value = JSON.parse(JSON.stringify(props.editInfo))
watch(() => props.editInfo, it => {
  formData.value = JSON.parse(JSON.stringify(it))
  edit.value = Object.keys(it).length !== 0
})

const resourceOptions = reactive<ResourceInfo []>([])
const formRef = ref<FormInstance>()
const emit = defineEmits(["close", "update"])
const roleTagCheck = async (_: any, value: any, callback: any) => {
  try {
    await checkRoleTag(value.toString())
    callback()
    return
  } catch (e) {
    if (e instanceof MinihError) {
      callback(new Error(e.msg))
      return
    }
    callback(new Error("角色标志校验不通过"))
    return
  }

}

const rules = reactive<FormRules<Partial<RoleInfo>>>({
  name: [
    {required: true, message: '请输入角色名称', trigger: 'blur'},
    {min: 2, max: 11, message: '角色名称最大长度 3 到 10 位', trigger: 'blur'},
  ],
  roleTag: [
    {required: true, message: '请输入角色标志（唯一）', trigger: 'blur'},
    {
      asyncValidator: (rule: any, value: any, callback: any) => {
        if (props.editInfo.roleTag == value) {
          callback()
          return
        }
        roleTagCheck(rule, value, callback)
      }, trigger: 'blur'
    },
  ]
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
        if (!edit.value) {
          await addRole(formData.value)
          ElMessage.success('新增成功')
          emit("update")
        } else {
          let update = JSON.parse(JSON.stringify(formData.value))
          Object.keys(update).map(key => {
            if (key != "id" && (anyIsEqual(props.editInfo[key], update[key]))) {
              delete update[key]
            }
          })
          if (Object.keys(update).length > 1) {
            await editRole(update)
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

const queryResource = async () => {
  const resources = await store.dispatch("resource/getOrLoad", false)
  resourceOptions.push(...resources)
}
queryResource()


</script>

<style scoped>

</style>
