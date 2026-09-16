<script setup lang="ts">
/**
 * 我的订单。需用户登录（路由 requiresAuth）。待支付订单可打开支付宝收银台。
 *
 * 后端接口：
 * - GET /order/list — orderApi.list
 * - GET /pay/create/{orderId} — orderApi.paymentPage，返回收银台 HTML
 *
 * 关键函数：
 * - loadOrders：分页拉取当前用户订单
 * - pay：先开空白窗口再写入 HTML，避免弹窗拦截导致无法支付
 */
import { onMounted, ref } from 'vue'
import { RefreshRight, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '../api'
import type { Order } from '../types'

const loading = ref(true)
const payingId = ref('')
const orders = ref<Order[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)

const statusMap: Record<number, { label: string; type: 'warning' | 'success' | 'info' | 'danger' }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  [-1]: { label: '已取消', type: 'info' },
  [-2]: { label: '待退款处理', type: 'danger' },
}

async function loadOrders() {
  loading.value = true
  try {
    const result = await orderApi.list(page.value, pageSize)
    orders.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function pay(order: Order) {
  const paymentWindow = window.open('', '_blank')
  payingId.value = order.id
  try {
    const html = await orderApi.paymentPage(order.id)
    if (!paymentWindow) throw new Error('浏览器阻止了支付窗口，请允许弹窗')
    paymentWindow.document.open()
    paymentWindow.document.write(html)
    paymentWindow.document.close()
  } catch (error) {
    paymentWindow?.close()
    ElMessage.error(error instanceof Error ? error.message : '支付页面打开失败')
  } finally {
    payingId.value = ''
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <span class="eyebrow">My Orders</span>
        <h1 class="page-title">我的订单</h1>
        <p class="page-description">查看订单状态，并在 15 分钟有效期内完成支付。</p>
      </div>
      <el-button :icon="RefreshRight" @click="loadOrders">刷新状态</el-button>
    </div>

    <section class="surface order-list" v-loading="loading">
      <article v-for="order in orders" :key="order.id" class="order-row">
        <div class="order-main">
          <div class="order-title">
            <h3>{{ order.goodsName }}</h3>
            <el-tag :type="(statusMap[order.status] || statusMap[-1]).type" effect="plain">
              {{ (statusMap[order.status] || statusMap[-1]).label }}
            </el-tag>
          </div>
          <div class="order-meta">
            <span>订单号 {{ order.id }}</span>
            <span>创建于 {{ order.createDate }}</span>
            <span v-if="order.payDate">支付于 {{ order.payDate }}</span>
          </div>
        </div>

        <div class="order-price">
          <small>实付金额</small>
          <strong>¥{{ Number(order.goodsPrice).toFixed(2) }}</strong>
        </div>

        <el-button
          v-if="order.status === 0"
          type="primary"
          :icon="Wallet"
          :loading="payingId === order.id"
          @click="pay(order)"
        >
          立即支付
        </el-button>
        <span v-else class="order-finished">订单状态已更新</span>
      </article>

      <div v-if="!loading && !orders.length" class="empty-panel">
        暂无订单，前往秒杀会场挑选商品吧
      </div>
    </section>

    <el-pagination
      v-if="total > pageSize"
      background
      layout="prev, pager, next"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      @current-change="(value: number) => { page = value; loadOrders() }"
    />
  </div>
</template>

<style scoped>
.order-list {
  min-height: 220px;
  overflow: hidden;
}

.order-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px 120px;
  align-items: center;
  gap: 24px;
  padding: 22px 26px;
  border-bottom: 1px solid var(--line);
}

.order-row:last-child {
  border-bottom: 0;
}

.order-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-title h3 {
  margin: 0;
  font-size: 16px;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 9px;
  color: var(--muted);
  font-size: 12px;
}

.order-price {
  text-align: right;
}

.order-price small {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
}

.order-price strong {
  color: var(--danger);
  font-size: 18px;
}

.order-finished {
  color: var(--muted);
  text-align: center;
  font-size: 12px;
}

@media (max-width: 720px) {
  .order-row {
    grid-template-columns: 1fr auto;
  }

  .order-row > .el-button,
  .order-finished {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
