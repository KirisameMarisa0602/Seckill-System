import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    server: {
      host: '127.0.0.1',
      port: 3000,
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
          rewrite: (path) => {
            const target = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
            try {
              const port = new URL(target).port
              if (port === '8080' || port === '8081') {
                return path.replace(/^\/api/, '')
              }
            } catch {
              return path.replace(/^\/api/, '')
            }
            return path
          },
        },
      },
    },
  }
})
