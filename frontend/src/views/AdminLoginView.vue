<script setup lang="ts">
/**
 * 管理员登录页。成功后写入 Admin-Token，进入运营控制台。
 *
 * 后端接口：
 * - POST /admin/login — adminApi.login
 *
 * 关键函数：
 * - submit：校验账号密码后保存管理员会话
 */
import { reactive, ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminApi } from '../api'
import { errorMessage } from '../api/errors'
import StatusBanner from '../components/StatusBanner.vue'
import { setFlash } from '../feedback'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const formError = ref('')
const formSuccess = ref('')
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入管理员密码', trigger: ['blur', 'change'] }],
}

/** 校验账号密码后保存管理员会话并进入控制台。 */
async function submit() {
  formError.value = ''
  formSuccess.value = ''
  try {
    await formRef.value?.validate()
  } catch {
    formError.value = '请填写管理员账号和密码'
    return
  }
  submitting.value = true
  try {
    const token = await adminApi.login(form.username, form.password)
    auth.setAdminSession(token)
    formSuccess.value = '认证成功，正在进入控制台…'
    setFlash('success', '管理员登录成功')
    await router.replace('/admin')
  } catch (error) {
    formError.value = errorMessage(error, '管理员登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page admin-auth">
    <section class="surface auth-card">
      <div class="admin-icon"><el-icon><Lock /></el-icon></div>
      <span class="eyebrow">Operations Console</span>
      <h1>运营后台</h1>
      <p>登录后管理商品、用户、缓存预热及待退款订单。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="管理员账号" prop="username">
          <el-input v-model="form.username" size="large" />
        </el-form-item>
        <el-form-item label="管理员密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <StatusBanner v-if="formSuccess" kind="success" :text="formSuccess" />
        <StatusBanner v-if="formError" kind="error" :text="formError" />
        <el-button
          class="auth-submit"
          type="primary"
          size="large"
          :loading="submitting"
          @click="submit"
        >
          进入控制台
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.admin-auth {
  background: #eef2f7;
}

.admin-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  margin-bottom: 22px;
  border-radius: 13px;
  color: #fff;
  background: var(--ink);
  font-size: 20px;
}
</style>
