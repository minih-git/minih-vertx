import {createRouter, createWebHistory, RouteLocationNormalized} from 'vue-router'
import Login from "../components/login/Login.vue";
import Home from "../components/Home.vue";
import {store} from "../store";
import ErrorPage from "../components/ErrorPage.vue";
import List from "../components/user/List.vue";


const routes = [

    {
        path: "/login",
        name: "登录",
        component: Login,
        meta: {
            needAuth: false
        }
    },
    {
        path: '/',
        alias: '/home',
        name: "首页",
        component: Home,
        meta: {
            needAuth: true
        },
        children:[
            {path: '/user',
                name: "用户列表",
                component: List,
                meta: {
                    needAuth: true
                },
            }
        ]
    },
    {
        path: "/noAuth",
        name: "无权限",
        component: ErrorPage,
        meta: {
            needAuth: false
        },
    },
    {
        path: "/:catchAll(.*)",
        redirect: "/"
    },
]

const router = createRouter(
    {history: createWebHistory(), routes}
)

function find(arr1: String[], arr2: String[]) {
    let temp: String[] = []
    for (const item of arr2) {
        arr1.findIndex(i => i === item) !== -1 ? temp.push(item) : ''
    }
    return !!temp.length
}

const hasPermission = (to: RouteLocationNormalized): Boolean => {
    let needAuth = to.meta.needAuth || true
    let needRole = to.meta.needRole || []
    if (needAuth && needRole instanceof Array && needRole.length != 0) {
        let roles = store.state.user.userInfo.role
        if (!roles || roles.length == 0) {
            return false
        }
        return find(roles, needRole)
    }
    return true;
}
router.beforeEach((to, _) => {
    if (to.meta.needAuth && !store.state.user.token) {
        return {name: '登录'}
    }
    if (!hasPermission(to)) {
        return {name: "无权限"}
    }
})

export default router