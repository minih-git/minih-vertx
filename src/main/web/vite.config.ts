import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
    server: {
        host: '0.0.0.0',
        port: 4500, // 设置服务启动端口号
        // open: true, // 设置服务启动时是否自动打开浏览器
        cors: true, // 允许跨域
        proxy: {
            '/api': {
                target: 'http://localhost:8080/',//代理的地址
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')//这里的/需要转义
            }
        }
    },
    plugins: [vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),],
})