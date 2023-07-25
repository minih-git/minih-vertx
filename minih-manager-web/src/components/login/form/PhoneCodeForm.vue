<template>
    <div class="wrapper input-wrapper">
        <div class="input-icon">
            <el-icon>
                <Phone/>
            </el-icon>
        </div>
        <div class="input-con">
            <input ref="mobileRef" v-model="formData.mobile" class="input" placeholder="手机号" type="text">
        </div>
    </div>
    <div class="wrapper code-wrapper">
        <div class="wrapper input-wrapper">
            <div class="input-icon">
                <el-icon>
                    <Lock/>
                </el-icon>
            </div>
            <div class="input-con">
                <input v-model="formData.code" class="input" placeholder="验证码" type="text"
                       @keydown.enter="onSubmit">
            </div>
        </div>
        <el-tooltip
            :content="btnText!='获取验证码'?'验证码已发送，10分钟内输入有效！':'获取验证码'"
            placement="top-start"
            style="cursor: pointer"
        >
            <el-link :disabled="btnText!='获取验证码'" class="code-btn" style="width: 70px" type="primary"
                     @click="getCode">
                {{ btnText }}
            </el-link>
        </el-tooltip>

    </div>

    <div class="wrapper save-p-wrapper" style="margin-top: 5px;">
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

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {Loading, Lock, Phone} from "@element-plus/icons-vue";
import {FormInfo, info, login, submitGetCode} from "../../../api";
import {useRouter} from 'vue-router'
import {ElMessage} from "element-plus";


const config = JSON.parse(localStorage.getItem("config") || "{}")
let btnText = ref("获取验证码")
if (config.countdown && Number(config.countdown) > 0) {
    btnText.value = config.countdown
}
let isLoading = ref(false)
const mobileRef = ref();


const router = useRouter()

const formData = reactive<Partial<FormInfo>>({
    mobile: "",
    code: "",
})

const countdownStart = (countdown: number = 60) => {
    config.countdown = countdown
    localStorage.setItem("config", JSON.stringify(config))
    let i = setInterval(() => {
        config.countdown = countdown
        localStorage.setItem("config", JSON.stringify(config))
        if (countdown <= 0) {
            btnText.value = "获取验证码"
            clearInterval(i)
            return
        }
        countdown--
        btnText.value = String(countdown)
    }, 1000)
}


if (config.countdown && Number(config.countdown) > 0) {
    countdownStart(Number(config.countdown))
}


const getCode = async () => {
    let msg: string = ""

    if (!formData.mobile) {
        msg = "请输入手机号"
        mobileRef.value.focus()
    }
    if (formData.mobile && !/^1\d{10}$/.test(formData.mobile)) {
        msg = "手机号格式错误"
    }
    if (msg) {
        ElMessage({
            message: msg,
            type: 'warning',
            grouping:true
        })
        return;
    }
    countdownStart()
    try {
        await submitGetCode(formData)
    } catch (e) {
        isLoading.value = false
    }
}


const onSubmit = async () => {
    isLoading.value = true
    try {
        await login(formData)
        await info()
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

.code-wrapper {
    margin-top: 15px;
    display: flex;
    height: 32px;
    align-items: center;

    .code-btn {
        height: 32px;
        margin-left: 10px;
    }
}

.input-wrapper {
    background: rgba(209, 215, 229, 0.26);


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
