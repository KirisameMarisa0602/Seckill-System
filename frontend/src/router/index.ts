/**
 * 前端路由表与导航守卫。
 *
 * 路由 meta：
 * - title：写入 document.title
 * - guest：已登录用户禁止进入（登录/注册页）
 * - requiresAuth：需要用户 Token
 * - requiresAdmin：需要管理员 Token
 *
 * 未知路径重定向到会场；支付成功页同时兼容 `/success` 与 `/payment/success`。
 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '限时秒杀' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '用户登录', guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { title: '注册账户', guest: true },
    },
    {
      path: '/goods/:id',
      name: 'goods-detail',
      component: () => import('../views/GoodsDetailView.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('../views/OrdersView.vue'),
      meta: { title: '我的订单', requiresAuth: true },
    },
    {
      path: '/success',
      alias: '/payment/success',
      name: 'payment-success',
      component: () => import('../views/PaymentSuccessView.vue'),
      meta: { title: '支付结果' },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/AdminLoginView.vue'),
      meta: { title: '管理端登录' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminView.vue'),
      meta: { title: '运营控制台', requiresAdmin: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

/** 根据 Token 拦截访客页、用户订单页与运营后台。 */
router.beforeEach((to) => {
  document.title = `${String(to.meta.title || '首页')} · Seckill`
  const loggedIn = Boolean(localStorage.getItem('seckill-user-token'))
  if (to.meta.guest && loggedIn) {
    return { name: 'home' }
  }
  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !localStorage.getItem('seckill-admin-token')) {
    return { name: 'admin-login' }
  }
  return true
})

/** 应用路由实例。 */
export default router
