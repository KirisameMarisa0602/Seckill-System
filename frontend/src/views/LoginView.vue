<script setup lang="ts">
/**
 * 用户登录页。成功后把 Token 写入 auth store，并跳转到 `redirect` 或会场。
 *
 * 后端接口：
 * - POST /user/login — authApi.login，返回 Token 字符串
 *
 * 关键函数：
 * - submit：校验手机号/密码后登录；已登录用户由路由 guest 守卫拦回首页
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ mobile: '', password: '' })

const rules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为 8 至 72 位', trigger: 'blur' },
  ],
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const token = await authApi.login(form.mobile, form.password)
    auth.setUserSession(token, form.mobile)
    ElMessage.success('登录成功')
    router.replace(String(route.query.redirect || '/'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="surface auth-card">
      <span class="eyebrow">Welcome Back</span>
      <h1>登录账户</h1>
      <p>登录后参与秒杀、查询排队结果并管理个人订单。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" size="large" placeholder="请输入 11 位手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button
          class="auth-submit"
          size="large"
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          登录
        </el-button>
      </el-form>

      <div class="auth-switch">
        还没有账户？<RouterLink to="/register">立即注册</RouterLink>
      </div>
    </section>
  </div>
</template>
