import {createApp} from 'vue'
import './style.css'
import router from "./router";
import {key, store} from './store'
import App from './App.vue'
import 'element-plus/dist/index.css'
import 'virtual:svg-icons-register'
import {authEventBusHandler} from "./api";

let options = {
    vertxbus_reconnect_attempts_max: Infinity, // Max reconnect attempts
    vertxbus_reconnect_delay_min: 1000, // Initial delay (in ms) before first reconnect attempt
    vertxbus_reconnect_delay_max: 5000, // Max delay (in ms) between reconnect attempts
    vertxbus_reconnect_exponent: 2, // Exponential backoff factor
    vertxbus_randomization_factor: 0.5 // Randomization factor between 0 and 1
};
// @ts-ignore
const eb = new window.EventBus("/ws/minihEventbus",options)
eb.enableReconnect(true);
eb.onopen = () => {
    eb.registerHandler('cn.minih.core.web.config', async (_, message) => {
        if (message.body["minih.core.aesSecret"]) {
            store.commit("system/setSecret", message.body["minih.core.aesSecret"])
        }
    });
    eb.registerHandler('cn.minih.auth.session.offline', authEventBusHandler)
}

createApp(App).use(router).use(store, key).mount("#app")
