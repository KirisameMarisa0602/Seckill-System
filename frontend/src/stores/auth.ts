/**
 * 登录态 Store。用户 Token / 手机号、管理员 Token 分别写入 localStorage，
 * 与 `api/index.ts` 请求头、路由守卫读取的 key 保持一致。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/** 用户与管理员两套独立会话，分别对应请求头 `token` 与 `Admin-Token`。 */
export const useAuthStore = defineStore('auth', () => {
  const userToken = ref(localStorage.getItem('seckill-user-token') || '')
  const userMobile = ref(localStorage.getItem('seckill-user-mobile') || '')
  const adminToken = ref(localStorage.getItem('seckill-admin-token') || '')

  /** 是否已登录普通用户。 */
  const isLoggedIn = computed(() => Boolean(userToken.value))
  /** 是否已登录管理员。 */
  const isAdmin = computed(() => Boolean(adminToken.value))

  /** 写入用户 Token 与展示用手机号。 */
  function setUserSession(token: string, mobile: string) {
    userToken.value = token
    userMobile.value = mobile
    localStorage.setItem('seckill-user-token', token)
    localStorage.setItem('seckill-user-mobile', mobile)
  }

  /** 写入管理员 Token。 */
  function setAdminSession(token: string) {
    adminToken.value = token
    localStorage.setItem('seckill-admin-token', token)
  }

  /** 退出用户登录，不影响管理员会话。 */
  function logoutUser() {
    userToken.value = ''
    userMobile.value = ''
    localStorage.removeItem('seckill-user-token')
    localStorage.removeItem('seckill-user-mobile')
  }

  /** 退出管理员登录，不影响用户会话。 */
  function logoutAdmin() {
    adminToken.value = ''
    localStorage.removeItem('seckill-admin-token')
  }

  return {
    userToken,
    userMobile,
    adminToken,
    isLoggedIn,
    isAdmin,
    setUserSession,
    setAdminSession,
    logoutUser,
    logoutAdmin,
  }
})
