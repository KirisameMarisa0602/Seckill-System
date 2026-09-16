/**
 * 前端入口：创建 Vue 应用，挂载 Pinia、路由与全局样式，渲染到 `#app`。
 * 开发时由 Vite 加载；生产构建后由 Nginx 提供静态资源。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'

createApp(App)
  .use(createPinia())
  .use(router)
  .mount('#app')
