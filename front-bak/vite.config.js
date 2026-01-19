import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
    server: {
        proxy: {
            '/baseInfo': {
                target: 'http://localhost:19997',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/baseInfo/, '/baseInfo')
            }
        }
    },
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src')
        }
    }
})
