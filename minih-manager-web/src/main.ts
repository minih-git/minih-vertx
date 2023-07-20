import {createApp} from 'vue'
import './style.css'
import router from "./router";
import {key, store} from './store'
import App from './App.vue'
import 'element-plus/dist/index.css'
import 'virtual:svg-icons-register'



createApp(App).use(router).use(store,key).mount("#app")
