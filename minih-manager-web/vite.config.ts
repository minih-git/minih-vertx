import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import requireTransform from 'vite-plugin-require-transform';
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import {createSvgIconsPlugin} from 'vite-plugin-svg-icons'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    define:{
        global:{},
    },
    server: {
        host: '0.0.0.0',
        port: 4500, // 设置服务启动端口号
        // open: true, // 设置服务启动时是否自动打开浏览器
        cors: true, // 允许跨域
        proxy: {
            '/api': {
                target: 'http://localhost:8090/',//代理的地址
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')//这里的/需要转义
            },
            '/ws': {
                target: 'ws://localhost:8090/',//代理的地址
                changeOrigin: true,
                ws: true,
            }
        }
    },
    plugins: [
        vue(),
        requireTransform(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
        createSvgIconsPlugin({
            iconDirs: [path.resolve(process.cwd(), 'src/assets')],
            // 指定symbolId格式
            symbolId: '[name]'
        })
    ],
})
