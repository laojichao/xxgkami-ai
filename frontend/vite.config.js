import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: '/', // 使用绝对路径，避免在子路由下资源加载失败
  build: {
    sourcemap: false,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // secure: false 仅限开发环境使用，允许代理到自签名证书的 HTTPS 后端
        // 生产环境应通过反向代理（如 Nginx）处理，并使用有效证书
        secure: false
      }
    }
  }
})
