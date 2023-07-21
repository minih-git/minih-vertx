<template>
    <div class="wrapper input-wrapper">
        <div class="input-icon">
            <el-icon>
                <User/>
            </el-icon>
        </div>
        <div class="input-con">
            <input v-model="formData.username" class="input" placeholder="账号" type="text">
        </div>
    </div>
    <div class="wrapper input-wrapper">
        <div class="input-icon">
            <el-icon>
                <Lock/>
            </el-icon>
        </div>
        <div class="input-con">
            <input v-model="formData.password" :type="showPwd?'password':'text'" class="input" placeholder="密码"
                   @keydown.enter="onSubmit">
        </div>
        <div class="input-icon icon-click" style="cursor: pointer" @click="showPwd=!showPwd">
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
            <div v-show="isLoading" id="loading">
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
import {FormInfo, info, login} from "../../../api";
import {useRouter, useRoute} from 'vue-router'
import {useStore} from "../../../store";


const showPwd = ref<Boolean>(true)
const config = JSON.parse(localStorage.getItem("config") || "{}")
const savePassword = ref<Boolean>(config?.savePassword)
let isLoading = ref(false)
const router = useRouter()
const route = useRoute()
const store = useStore()

if (store.state.user.sessionInfo.tokenVale) {
    let target = route.query['target']?.toString() || '/home'
    router.push({path: target})
}

const formData = reactive<Partial<FormInfo>>({
    username: config?.localUser,
    password: config?.localPass,
})
const onSubmit = async () => {
    isLoading.value = true
    try {
        await login(formData)
        await info()
        config.savePassword = savePassword.value
        config.localUser = formData.username
        if (savePassword.value) {
            config.localPass = formData.password
        } else {
            config.localPass = ""
        }
        localStorage.setItem("config", JSON.stringify(config))
        isLoading.value = false
        await router.push({name: "首页"})
    } catch (e) {
        isLoading.value = false
    }
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
        background-image: var(--background-color-50);
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
