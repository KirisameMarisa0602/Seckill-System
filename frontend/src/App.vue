<script setup lang="ts">
/**
 * 全局壳层：顶栏导航、账号区、主内容区与页脚。本身不调后端接口，登录态来自 Pinia auth store。
 *
 * 关键 computed / 函数：
 * - activePath：把 `/goods/:id` 归到会场高亮、`/admin*` 归到后台高亮
 * - logout：清用户会话并回到首页（不影响管理员 Token）
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Management, ShoppingBag, Tickets, User } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activePath = computed(() => {
  if (route.path.startsWith('/goods')) return '/'
  if (route.path.startsWith('/admin')) return '/admin'
  return route.path
})

function logout() {
  auth.logoutUser()
  router.push('/')
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div class="app-shell">
    <header class="topbar">
      <button class="brand" type="button" @click="router.push('/')">
        <span class="brand-mark">S</span>
        <span>
          <strong>Seckill</strong>
          <small>高并发秒杀平台</small>
        </span>
      </button>

      <nav class="main-nav" aria-label="主导航">
        <RouterLink :class="{ active: activePath === '/' }" to="/">
          <el-icon><ShoppingBag /></el-icon>
          秒杀会场
        </RouterLink>
        <RouterLink
          v-if="auth.isLoggedIn"
          :class="{ active: activePath === '/orders' }"
          to="/orders"
        >
          <el-icon><Tickets /></el-icon>
          我的订单
        </RouterLink>
        <RouterLink
          :class="{ active: activePath === '/admin' }"
          :to="auth.isAdmin ? '/admin' : '/admin/login'"
        >
          <el-icon><Management /></el-icon>
          运营后台
        </RouterLink>
      </nav>

      <div class="account-actions">
        <template v-if="auth.isLoggedIn">
          <span class="account-label">
            <el-icon><User /></el-icon>
            {{ auth.userMobile }}
          </span>
          <el-button text @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button text @click="router.push('/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>

    <main>
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="site-footer">
      <span>Seckill System</span>
      <span>Redis · RabbitMQ · MySQL</span>
    </footer>
    </div>
  </el-config-provider>
</template>
