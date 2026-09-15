import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Le proxy renvoie tous les appels /api vers Spring Boot.
 * Le navigateur ne voit donc qu'une seule origine (localhost:5173) :
 * pas de CORS a configurer cote backend.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    // 5173, le port par defaut de Vite, tombe dans une plage TCP reservee par
    // Windows (Hyper-V/WSL) sur ce poste : le bind echoue en EACCES. 3000 est
    // hors de ces plages. A verifier avec :
    //   netsh interface ipv4 show excludedportrange protocol=tcp
    port: 3000,
    strictPort: false,
    host: '127.0.0.1',
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
