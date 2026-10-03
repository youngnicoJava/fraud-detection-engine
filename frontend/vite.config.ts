import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_');
  const api = env.VITE_API_URL || 'http://localhost:8083';
  return {
    plugins: [react()],
    server: {
      port: 5175,
      strictPort: true,
      host: '0.0.0.0',
      proxy: {
        '/api': { target: api, changeOrigin: true },
        '/q': { target: api, changeOrigin: true },
      },
    },
  };
});
