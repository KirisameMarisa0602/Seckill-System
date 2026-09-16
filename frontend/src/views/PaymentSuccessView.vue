<script setup lang="ts">
/**
 * 支付宝同步跳回页（`/success` 与 `/payment/success`）。不调后端，最终状态以异步回调为准。
 *
 * 无后端接口。按钮仅导航回会场或订单列表，由用户自行刷新订单状态。
 */
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="result-page">
    <section class="surface result-card">
      <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
      <span class="eyebrow">Payment Result</span>
      <h1>支付请求已完成</h1>
      <p>支付结果将以支付宝异步回调为准，请返回订单中心查看最终状态。</p>
      <div class="result-actions">
        <el-button size="large" @click="router.push('/')">返回会场</el-button>
        <el-button size="large" type="primary" @click="router.push('/orders')">
          查看订单
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.result-page {
  min-height: calc(100vh - 136px);
  display: grid;
  place-items: center;
  padding: 40px 20px;
}

.result-card {
  width: min(520px, 100%);
  padding: 50px 40px;
  text-align: center;
}

.success-icon {
  display: block;
  margin: 0 auto 20px;
  color: var(--success);
  font-size: 64px;
}

.result-card h1 {
  margin: 8px 0 12px;
  font-size: 30px;
}

.result-card p {
  color: var(--muted);
  line-height: 1.7;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 28px;
}
</style>
