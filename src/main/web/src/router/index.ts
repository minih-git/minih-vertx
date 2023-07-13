import {createRouter, createWebHistory} from 'vue-router'
import Login from "../components/login/Login.vue";
import Home from "../components/Home.vue";

const routes = [
    {
        path: "/login",
        name: "登录",
        component: Login
    },
    {
        path: "/home",
        name: "首页",
        component: Home
    },
]

const router = createRouter(
    {history: createWebHistory(), routes}
)

export default router
