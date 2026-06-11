import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 正式環境：Nginx 同源代理 /api 與 /ws → backend，前端用相對路徑即可。
// 開發環境：Vite dev server (3000) 直接把 /api、/ws 代理到 backend:8080。
export default defineConfig({
  plugins: [react()],
  // sockjs-client 內部用到 `global`，瀏覽器沒有，需指到 globalThis，否則
  // 會在 runtime 噴 "global is not defined"。
  define: {
    global: 'globalThis',
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true, // WebSocket / SockJS 升級需要
      },
    },
  },
});