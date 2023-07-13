<template>
  <div class="wrapper input-wrapper">
    <div class="input-icon">
      <el-icon>
        <User/>
      </el-icon>
    </div>
    <div class="input-con">
      <input class="input" type="text" v-model="formData.username" placeholder="账号">
    </div>
  </div>
  <div class="wrapper input-wrapper">
    <div class="input-icon">
      <el-icon>
        <Lock/>
      </el-icon>
    </div>
    <div class="input-con">
      <input class="input" :type="showPwd?'password':'text'" v-model="formData.password" placeholder="密码">
    </div>
    <div class="input-icon icon-click" @click="showPwd=!showPwd" style="cursor: pointer">
      <el-icon>
        <Hide v-if="showPwd"/>
        <View v-else/>
      </el-icon>
    </div>
  </div>
  <div class="wrapper save-p-wrapper">
    <el-checkbox v-model="savePassword" label="保存密码" size="small"/>
  </div>
  <div class="wrapper submit-wrapper">
    <div class="submit-btn" @click="onSubmit">
      <div id="loading" v-show="isLoading">
        <el-icon :class="{'is-loading':isLoading}">
          <Loading/>
        </el-icon>
      </div>
      登录
    </div>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {Hide, Loading, Lock, User, View} from "@element-plus/icons-vue";

const showPwd = ref<Boolean>(true)
const config = JSON.parse(localStorage.getItem("config"))
const savePassword = config?.savePassword || ref<Boolean>(true)
const isLoading = ref(false)

interface FormData {
  username: string
  password: string
}

const formData = reactive<FormData>({
  username: '',
  password: '',
})

const onSubmit = () => {
  console.log(formData)


}

</script>


<style scoped>

.wrapper {
  height: 32px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.submit-wrapper {
  margin-top: 15px;

  .submit-btn {
    height: 32px;
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #FFF;
    font-weight: bold;
    background-image: var(--background-color);
    cursor: pointer;
  }

  .submit-btn:hover {
    opacity: .8;
  }

  .submit-btn:active {
    opacity: .5;
  }

  #loading {
    margin-right: 10px;
    display: flex;
    justify-content: center;
    align-items: center;

  }
}

.save-p-wrapper {
  margin-top: 5px;
  margin-left: 5px;
}

.icon-click:active {
  opacity: .5;
}

.input-wrapper {
  background: rgba(209, 215, 229, 0.26);
  margin-top: 15px;

  .input-icon {
    width: 30px;
    height: 26px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .input-con {
    height: 26px;
    flex: 1;

    .input {
      outline: none;
      border: none;
      line-height: 26px;
      color: #9e9ea7;
      background: transparent;
      width: 100%;
    }
  }
}

</style>
