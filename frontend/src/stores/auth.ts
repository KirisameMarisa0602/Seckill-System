import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const userToken = ref(localStorage.getItem('seckill-user-token') || '')
  const userMobile = ref(localStorage.getItem('seckill-user-mobile') || '')
  const adminToken = ref(localStorage.getItem('seckill-admin-token') || '')

  const isLoggedIn = computed(() => Boolean(userToken.value))
  const isAdmin = computed(() => Boolean(adminToken.value))

  function setUserSession(token: string, mobile: string) {
    userToken.value = token
    userMobile.value = mobile
    localStorage.setItem('seckill-user-token', token)
    localStorage.setItem('seckill-user-mobile', mobile)
  }

  function setAdminSession(token: string) {
    adminToken.value = token
    localStorage.setItem('seckill-admin-token', token)
  }

  function logoutUser() {
    userToken.value = ''
    userMobile.value = ''
    localStorage.removeItem('seckill-user-token')
    localStorage.removeItem('seckill-user-mobile')
  }

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
