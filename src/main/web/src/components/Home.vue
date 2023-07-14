<template>
  <el-container id="container">
    <el-header id="header" height="60">
      <el-row :gutter="20" style="height: 100%">
        <el-col :span="4" style="height: 100%">
          <div id="logo"></div>
        </el-col>
        <el-col :span="16">
        </el-col>
        <el-col :span="4">
          <div id="user">
            <el-space wrap large>
              <h6>欢迎你！{{ store.state.user.sessionInfo.loginId }}</h6>

              <el-dropdown style="outline: none">
                <el-avatar
                    src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
                />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>
                      <el-icon>
                        <Edit/>
                      </el-icon>
                      修改密码
                    </el-dropdown-item>
                    <el-dropdown-item>
                      <el-icon>
                        <Star/>
                      </el-icon>
                      个人信息
                    </el-dropdown-item>
                    <el-dropdown-item>
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
        </el-col>
      </el-row>

    </el-header>
    <el-container id="center" style="padding-top: 30px">
      <el-aside width="200px" id="aside">
        <div class='menu-icon' style="padding-left: 13px">
          <el-icon class="menu-icon-rotate" @click="isCollapse = !isCollapse">
            <Menu/>
          </el-icon>

        </div>
        <el-menu
            default-active="1"
            :collapse="isCollapse"
        >
          <el-sub-menu index="1">
            <template #title>
              <el-icon>
                <Setting/>
              </el-icon>
              <span>系统设置</span>
            </template>
            <el-menu-item index="1-1">
              <span><el-icon><User/></el-icon>用户列表</span>
            </el-menu-item>
            <el-menu-item index="1-2">
              <span><el-icon><Lock/></el-icon>角色列表</span>
            </el-menu-item>
            <el-menu-item index="1-3">
              <span><el-icon><FullScreen/></el-icon>资源列表</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>


      </el-aside>
      <el-container>
        <el-main id="main">
          <router-view></router-view>
        </el-main>
      </el-container>

    </el-container>
    <el-container>
      <el-footer id="footer">Footer</el-footer>
    </el-container>
  </el-container>


</template>

<script setup lang="ts">

import {useStore} from "../store";
import {Edit, Star, Promotion, Setting, Menu, User, Lock, FullScreen} from "@element-plus/icons-vue";
import {ref} from "vue";

const isCollapse = ref<boolean>(false)

const store = useStore()


</script>


<style scoped>
#container {
  height: 100%;
  display: flex;
  flex-direction: column;

  #header {
    flex: 1;
    height: 60px;
    color: var(--second-color);
    border-bottom: 1px solid var(--first-color);

    #logo {
      width: 120px;
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
    }

    #user {
      height: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
    }

  }

  #center {
    flex: 8;

    #aside {
      text-align: left;
      --el-menu-item-height: 32px;
      --el-menu-sub-item-height: 28px;
      --el-menu-base-level-padding: 10px;

      .menu-icon {
        font-size: 21px;
        cursor: pointer;
      }

      .menu-icon i:hover {
        color: #8caede;

      }

      .menu-icon i:active {
        opacity: .5;
        animation: rotate linear .2s;
        transform-origin: center;
      }


      .el-menu {
        border-right: none;

        .is-active:not(.el-sub-menu) {
          background-image: var(--background-color-50);
          color: #FFF
        }
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
  }

  #footer {
    flex: 1;

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
