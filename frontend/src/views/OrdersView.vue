<script setup lang="ts">

import { onMounted, reactive, ref } from 'vue'
import { RefreshRight, Wallet } from '@element-plus/icons-vue'
import { authApi, orderApi } from '../api'
import { errorMessage } from '../api/errors'
import StatusBanner from '../components/StatusBanner.vue'
import { OrderStatus, type Order } from '../types'

const loading = ref(true)
const payingId = ref('')
const orders = ref<Order[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const pageError = ref('')
const payHint = ref('')

const address = reactive({ receiverName: '', receiverPhone: '', detail: '' })
const savingAddress = ref(false)
const addressHint = ref('')

const statusMap: Record<number, { label: string; type: 'warning' | 'success' | 'info' | 'danger' }> = {
  [OrderStatus.unpaid]: { label: '待支付', type: 'warning' },
  [OrderStatus.paid]: { label: '已支付', type: 'success' },
  [OrderStatus.canceled]: { label: '已取消', type: 'info' },
  [OrderStatus.refundPending]: { label: '待退款处理', type: 'danger' },
}

async function loadOrders() {
  loading.value = true
  pageError.value = ''
  try {
    const result = await orderApi.list(page.value, pageSize)
    orders.value = result.records
    total.value = result.total
  } catch (error) {
    orders.value = []
    pageError.value = errorMessage(error, '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAddress() {
  try {
    const saved = await authApi.address()
    address.receiverName = saved?.receiverName || ''
    address.receiverPhone = saved?.receiverPhone || ''
    address.detail = saved?.detail || ''
  } catch (error) {
    addressHint.value = errorMessage(error, '收货地址加载失败')
  }
}

async function saveAddress() {
  addressHint.value = ''
  if (!/^1[3-9]\d{9}$/.test(address.receiverPhone)) {
    addressHint.value = '请填写 11 位收货手机号'
    return
  }
  if (address.receiverName.trim().length < 1 || address.detail.trim().length < 1) {
    addressHint.value = '请填写收货人和详细地址'
    return
  }
  savingAddress.value = true
  try {
    await authApi.saveAddress(address.receiverName.trim(), address.receiverPhone.trim(), address.detail.trim())
    addressHint.value = '收货地址已保存，未支付订单会使用这份地址'
    await loadOrders()
  } catch (error) {
    addressHint.value = errorMessage(error, '收货地址保存失败')
  } finally {
    savingAddress.value = false
  }
}

async function pay(order: Order) {
  payHint.value = ''
  if (!order.receiverDetail) {
    payHint.value = '请先填写收货地址，再支付'
    return
  }
  const paymentWindow = window.open('', '_blank')
  payingId.value = order.id
  try {
    const html = await orderApi.paymentPage(order.id)
    if (!paymentWindow) throw new Error('浏览器阻止了支付窗口，请允许弹窗后重试')
    paymentWindow.document.open()
    paymentWindow.document.write(html)
    paymentWindow.document.close()
    payHint.value = '已打开支付窗口，完成付款后请刷新订单状态'
  } catch (error) {
    paymentWindow?.close()
    payHint.value = errorMessage(error, '支付页面打开失败')
  } finally {
    payingId.value = ''
  }
}

onMounted(() => {
  loadOrders()
  loadAddress()
})
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

    <section class="surface address-card">
      <div>
        <h2>收货地址</h2>
        <p>秒杀下单会带上默认地址。没有地址的待支付订单，保存后会补上，支付前必须填写。</p>
      </div>
      <div class="address-form">
        <el-input v-model="address.receiverName" maxlength="64" placeholder="收货人" />
        <el-input v-model="address.receiverPhone" maxlength="11" placeholder="手机号" />
        <el-input v-model="address.detail" maxlength="255" placeholder="详细地址" />
        <el-button type="primary" :loading="savingAddress" @click="saveAddress">保存地址</el-button>
      </div>
    </section>
    <StatusBanner
      v-if="addressHint"
      :kind="addressHint.includes('已保存') ? 'success' : 'error'"
      :text="addressHint"
    />

    <StatusBanner v-if="pageError" kind="error" :text="pageError" />
    <StatusBanner
      v-if="payHint"
      :kind="payHint.includes('已打开') ? 'success' : 'error'"
      :text="payHint"
    />

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
            <span v-if="order.receiverDetail">
              {{ order.receiverName }} {{ order.receiverPhone }} {{ order.receiverDetail }}
            </span>
          </div>
        </div>

        <div class="order-price">
          <small>实付金额</small>
          <strong>¥{{ Number(order.goodsPrice).toFixed(2) }}</strong>
        </div>

        <el-button
          v-if="order.status === OrderStatus.unpaid"
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
.address-card {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
  padding: 22px 26px;
}

.address-card h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.address-card p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.address-form {
  display: grid;
  grid-template-columns: 140px 160px minmax(0, 1fr) auto;
  gap: 12px;
}

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
  .address-form {
    grid-template-columns: 1fr;
  }

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
