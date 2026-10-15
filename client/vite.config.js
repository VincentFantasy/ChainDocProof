import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

// 开发服务器配置：/api 前缀的请求代理到后端 127.0.0.1:8080，规避浏览器跨域
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'node',
  },
});
