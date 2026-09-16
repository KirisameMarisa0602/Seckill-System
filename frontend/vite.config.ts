/**
 * Vite 开发/构建配置。
 *
 * - AutoImport / Components：按需引入 Element Plus，类型写入 `src/auto-imports.d.ts`、`src/components.d.ts`（勿手改）
 * - 开发服务器固定 127.0.0.1:3000，把 `/api` 代理到后端
 * - 代理目标为 8080/8081 时去掉 `/api` 前缀（Spring 控制器无该前缀）；其它目标则原样转发
 */
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

/** Vite 配置工厂：按 mode 加载环境变量，导出开发代理与 Element Plus 按需引入。 */
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
          // 直连 Spring 时去掉 /api；经 Nginx 等仍带 /api 前缀的目标则原样转发
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
