<template>
    <el-container id="container">
        <el-affix :offset="0">
            <el-header id="header" height="60">
                <div id="logo">
                    <svg class="image-svg-svg primary" height="30" style="overflow: visible;" width="30"
                         x="0" y="0">
                        <use xlink:href="#logo-icon"/>
                    </svg>
                    <svg height="30" style="margin-left: 5px" viewBox="0 0 168 42" width="60"
                         xmlns="http://www.w3.org/2000/svg">
                        <use xlink:href="#name"/>
                    </svg>
                </div>
                <div id="user">
                    <el-space large wrap>
                        <h6>欢迎你！{{ store.state.user.userInfo.name }}</h6>
                        <el-dropdown style="outline: none;border: none">
                            <el-avatar
                                :src="store.state.user.userInfo.avatar"
                            />
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item @click="logout">
                                        <el-icon>
                                            <Promotion/>
                                        </el-icon>
                                        退出登录
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>

                    </el-space>
                </div>
            </el-header>
        </el-affix>

        <el-container id="center">
            <el-aside id="aside" width="200px">
                <div class="menu">
                    <div class="sub-menu">
                        <div class="sub-menu-text">
                            <span><el-icon><Setting/></el-icon>系统管理</span>
                        </div>
                        <div class="sub-menu-con">
                            <div v-for="item in subMenu" :class="{'is-active':currenSelect===item.id}"
                                 class="menu-item "
                                 @click=" (currenSelect = item.id) && router.push({path:item.path})">
                                <el-icon>
                                    <component :is="item.icon" class="icons"></component>
                                </el-icon>
                                <span style="margin-left: 10px">{{ item.name }}</span>
                            </div>
                        </div>
                        <div class="sub-menu-split-line"></div>
                    </div>
                </div>
            </el-aside>
            <el-container id="main">
                <el-main id="main-con">
                    <el-affix :offset="0" target="#main-con">
                        <el-page-header>
                            <template #breadcrumb>
                                <el-breadcrumb separator="/">
                                    <el-breadcrumb-item :to="'/'">
                                        首页
                                    </el-breadcrumb-item>

                                </el-breadcrumb>
                            </template>
                            <template #content>
                                <span class="text-large font-600 mr-3"> {{ route.name }} </span>
                            </template>
                        </el-page-header>
                    </el-affix>
                    <div style="margin-top: 20px">
                        <router-view></router-view>
                    </div>
                </el-main>
            </el-container>

        </el-container>
    </el-container>


</template>

<script setup lang="ts">

import {useStore} from "../store";
import {useRoute, useRouter} from "vue-router";
import {FullScreen, Lock, Promotion, Setting, User} from "@element-plus/icons-vue";
import {ref, shallowRef} from "vue";
import {logout} from "../api/login.ts";

const store = useStore()
const route = useRoute()
const router = useRouter()
const currenSelect = ref<number>(0)
const subMenu = ref<any>([
    {id: 1, name: "用户列表", path: "/user", icon: shallowRef(User)},
    {id: 2, name: "角色列表", path: "/role", icon: shallowRef(Lock)},
    {id: 3, name: "资源列表", path: "/resource", icon: shallowRef(FullScreen)},
])


</script>


<style scoped>


#container {
    display: flex;
    flex-direction: column;
    background-color: #F7F9FA;
    color: rgb(51, 51, 51);
    height: 100%;
    overflow: hidden;

    #header {
        height: 50px;
        box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.16);
        color: #666;

        #logo {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100%;
            float: left;
        }

        #user {
            height: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            float: right;
        }

    }

    #center {
        flex: 8;

        #aside {
            text-align: left;
            --el-menu-item-height: 32px;
            --el-menu-sub-item-height: 28px;
            --el-menu-base-level-padding: 10px;
            --el-menu-bg-color: transparent;
            --el-menu-text-color: rgb(51, 51, 51);
            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.16);
            padding-top: 25px;
            height: 100%;

            .menu {

                .sub-menu-text {
                    order: 0;
                    flex: 0 1 auto;
                    align-self: auto;
                    font-weight: 700;
                    color: rgb(51, 51, 51);
                    margin: 20px;
                }

                .sub-menu-split-line {
                    height: 1px;
                    margin: 20px 0;
                    border: 0;
                    background: linear-gradient(to right, rgb(249, 249, 249), rgb(203, 203, 203), rgb(249, 249, 249)) rgb(203, 203, 203);
                }

                .menu-item {
                    padding: 8px;
                    margin: 10px 0;
                    cursor: pointer;
                //border-radius: 15px; display: flex; justify-content: center;
                }

                .menu-item:hover {
                    background-image: linear-gradient(120deg, transparent 0%, var(--second-color) 100%);

                }

                .is-active {
                    background-image: linear-gradient(120deg, transparent 0%, var(--second-color) 80%, var(--third-color) 100%);
                    color: #6b6161;
                    font-weight: bold;
                }
            }


            .el-menu {
                border-right: none;


            }

            .el-sub-menu__title {
                border-radius: 15px;
                margin: 10px;
            }

            .el-menu-item {
                border-radius: 15px;
                margin: 20px;

            }
        }

        #main {
            flex-direction: column;
            overflow-y: auto;

            #main-con {
                flex: 1;
            }

        }
    }


}

@keyframes rotate {
    0% {
        transform: rotate(0deg);
    }
    100% {
        transform: rotate(180deg);
    }
}
</style>