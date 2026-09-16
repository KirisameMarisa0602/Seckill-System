<script setup lang="ts">
/**
 * 用户注册页。创建账户后跳转登录，不自动写入会话。
 *
 * 后端接口：
 * - POST /user/register — authApi.register
 *
 * 关键函数：
 * - submit：校验昵称/手机号/密码与二次确认后注册
 */
import { reactive, ref } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { authApi } from '../api'
import { errorCode, errorMessage } from '../api/errors'
import StatusBanner from '../components/StatusBanner.vue'
import { setFlash } from '../feedback'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const formError = ref('')
const alreadyRegistered = ref(false)
const form = reactive({ nickname: '', mobile: '', password: '', confirmPassword: '' })

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: ['blur', 'change'] },
    { min: 2, max: 20, message: '昵称长度为 2 至 20 位', trigger: ['blur', 'change'] },
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: ['blur', 'change'] },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号须为 11 位有效号码', trigger: ['blur', 'change'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'change'] },
    { min: 8, max: 72, message: '密码长度为 8 至 72 位', trigger: ['blur', 'change'] },
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (!value) callback(new Error('请再次输入密码'))
        else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
}

async function submit() {
  formError.value = ''
  alreadyRegistered.value = false
  try {
    await formRef.value?.validate()
  } catch {
    formError.value = '请检查昵称、手机号、密码以及两次密码是否一致'
    return
  }
  submitting.value = true
  try {
    await authApi.register(form.nickname, form.mobile, form.password)
    setFlash('success', '注册成功，请使用新账号登录')
    await router.replace({ path: '/login', query: { registered: '1' } })
  } catch (error) {
    alreadyRegistered.value = errorCode(error) === 500206
    formError.value = errorMessage(error, '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="surface auth-card">
      <span class="eyebrow">Create Account</span>
      <h1>注册账户</h1>
      <p>创建账户后即可进入秒杀会场，每件商品每位用户限购一件。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="2 至 20 个字符" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" size="large" placeholder="请输入 11 位手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <StatusBanner v-if="formError" kind="error" :text="formError" />
        <p v-if="alreadyRegistered" class="auth-switch" style="margin: 0 0 12px">
          <RouterLink to="/login">该手机号已注册，前往登录</RouterLink>
        </p>
        <el-button
          class="auth-submit"
          size="large"
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          创建账户
        </el-button>
      </el-form>

      <div class="auth-switch">
        已有账户？<RouterLink to="/login">返回登录</RouterLink>
      </div>
    </section>
  </div>
</template>
