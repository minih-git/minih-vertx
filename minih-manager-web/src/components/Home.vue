<template>
  <el-container id="container">
    <el-affix :offset="0">
      <el-header id="header" height="60">
        <div id="logo">
          <svg class="image-svg-svg primary" height="30" style="overflow: visible;" width="30"
               x="0" y="0">
            <svg viewBox="0 0 86.52999877929688 100" xmlns="http://www.w3.org/2000/svg">
              <path d="M20.82 87.16L0 75.28V25.04l20.82 12.33v49.79z" fill="#2084f7"></path>
              <path d="M18.95 14.03L0 25.04l20.82 12.33 18.95-10.83-20.82-12.51z" fill="#22cfee"></path>
              <path d="M86.53 50.55l-.11 24.39L43.03 100l.14-24.56 43.36-24.89z" fill="#23b0f1"></path>
              <path d="M24.1 88.87L43.03 100l.14-24.56-19.02-10.58-.05 24.01z" fill="#2084f7"></path>
              <path d="M22.41 12.17L43.06 0l43.28 25.04L65.92 37.2 22.41 12.17z" fill="#22cfee"></path>
              <path d="M86.37 46.98l-.03-21.94L65.92 37.2l.02 21.81 20.43-12.03z" fill="#23b0f1"></path>
              <path d="M60.79 60.12L43.27 70.23 25.75 60.12V39.88l17.52-10.11 17.52 10.11v20.24z"
                    fill="#22cfee"></path>
            </svg>
          </svg>
          <svg height="30" style="margin-left: 5px" viewBox="0 0 168 42" width="60" xmlns="http://www.w3.org/2000/svg">
            <path
                d="M23.52-5.46L29.1-5.46 41.16-26.88 41.46-26.88 41.46 0 49.56 0 49.56-42 41.34-42 26.82-15.18 26.58-15.18 12.18-42 4.08-42ZM4.08 0L11.94 0 11.94-16.5 4.08-31.26ZM65.82-35.52L65.82-42 57.66-42 57.66-35.52ZM57.66 0L65.76 0 65.76-30 57.66-30ZM102.36 0L110.28 0 110.28-42 102.54-42 102.54-13.08 102.24-13.08 81.96-42 73.62-42ZM73.98 0L81.84 0 81.84-20.58 73.98-31.8ZM126.54-35.52L126.54-42 118.38-42 118.38-35.52ZM118.38 0L126.48 0 126.48-30 118.38-30ZM146.52-18.12L164.34-18.12 164.34 0 172.44 0 172.44-42 164.34-42 164.34-25.8 146.52-25.8ZM134.7 0L142.8 0 142.8-42 134.7-42Z"
                transform="translate(-4.079999923706055, 42)"></path>
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
                  <el-breadcrumb-item :to="{ path: '/' }">
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
import {Edit, Star, Promotion, Setting, Menu, User, Lock, FullScreen} from "@element-plus/icons-vue";
import {ref, shallowRef} from "vue";
import {logout} from "../api/login.ts";
import {ElMessage} from "element-plus";

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

      #footer {
        flex: none;
        height: 50px;


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
