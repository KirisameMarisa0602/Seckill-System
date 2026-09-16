<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminApi } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入管理员密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const token = await adminApi.login(form.username, form.password)
    auth.setAdminSession(token)
    ElMessage.success('管理员认证成功')
    router.replace('/admin')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员登录失败')
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
