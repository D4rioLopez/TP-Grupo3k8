import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo (npm run dev) las llamadas a /api se reenvian al API Gateway,
// asi el navegador siempre habla con el mismo origen y no hace falta CORS.
// Si el gateway corre en otro puerto: GATEWAY_URL=http://localhost:8088 npm run dev
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.GATEWAY_URL || 'http://localhost:8080'
    }
  }
})
