<template>
    <div id="bg">
        <div id="login-model">
            <div id="banner">
                <div>
                    <div id="logo">
                        <svg height="100" width="100">
                            <use xlink:href="#logo-icon"/>
                        </svg>
                        <svg fill="#FFF" height="100" style="margin-left: 5px;" width="100">
                            <use xlink:href="#name"/>
                        </svg>
                    </div>
                    <h2 style="margin-top: 50px"> welcome back :)</h2>
                </div>
            </div>
            <div id="login">
                <div id="welcome">
                    <div id="text-content">
                        <h2>你好！</h2>
                        <h2>欢迎使用！</h2>
                    </div>
                </div>
                <div id="login-form">
                    <div v-show="current === 'password'">
                        <basic-form ></basic-form>
                    </div>
                    <div v-show="current === 'phone'">
                        <phone-code-form ></phone-code-form>
                    </div>
                </div>
                <div style="margin-top: 36px">
                    <el-tooltip
                        content="验证码登录"
                        placement="top-start"
                        style="cursor: pointer"
                    >
                        <el-button :icon="Phone" circle size="large" type="primary" @click="current = 'phone'" />
                    </el-tooltip>
                    <el-tooltip
                        content="密码登录"
                        placement="top-start"
                        style="cursor: pointer"
                    >
                        <el-button :icon="Lock" circle size="large" type="warning"  @click="current = 'password'"/>
                    </el-tooltip>

                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import BasicForm from "./form/BasicForm.vue";
import PhoneCodeForm from "./form/PhoneCodeForm.vue";
import {Lock, Phone} from "@element-plus/icons-vue";
import {ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useStore} from "../../store";


const router = useRouter()
const route = useRoute()
const store = useStore()

const current = ref<String>("phone")

if (store.state.user.sessionInfo.tokenVale) {
    let target = route.query['target']?.toString() || '/home'
    router.push({path: target})
}

</script>

<style scoped>

#bg {
    width: 100vw;
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background-image: var(--background-color);

    #login-model {
        width: var(--model-with);
        height: calc(var(--model-with) / 2);
        background-color: rgba(0, 0, 0, 0.2);
        border-radius: 15px;
        overflow: hidden;
        display: flex;

        #banner {
            width: calc(var(--model-with) / 2);
            flex: 1;
            color: #FFF;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        #login {
            box-sizing: border-box;
            padding: var(--login-model-padding);
            width: calc(var(--model-with) / 2);
            background-color: #FFF;
            height: 100%;
            flex-direction: column;

            #welcome {
                flex-direction: column;

                #text-content {
                    flex-direction: column;
                    text-align: left;
                    align-items: center;

                    * {
                        display: block;
                        flex: none;
                    }
                }
            }

            #login-form {
                margin-top: 30px;
            }
        }


    }
}


</style>