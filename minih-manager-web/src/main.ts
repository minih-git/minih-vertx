import {createApp} from 'vue'
import './style.css'
import router from "./router";
import {key, store} from './store'
import App from './App.vue'
import 'element-plus/dist/index.css'
import 'virtual:svg-icons-register'
import {authEventBusHandler} from "./api";
authEventBusHandler()


// @ts-ignore
const eb = new window.EventBus("/ws/systemEvent")
eb.onopen = () => {
    eb.send("cn.minih.system.event.bus","1212121212",{"token":store.state.user.token})
}


createApp(App).use(router).use(store, key).mount("#app")
