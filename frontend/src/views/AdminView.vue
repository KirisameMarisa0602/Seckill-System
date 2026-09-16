<script setup lang="ts">
/**
 * 运营控制台：概览、商品 CRUD、用户列表、待退款工单与 Redis 预热。需管理员 Token。
 *
 * 后端接口：
 * - GET /admin/goods/list — adminApi.goods
 * - POST /admin/goods/add | /update | /delete/{id} — 增改下架
 * - GET /admin/user/list — adminApi.users
 * - GET /admin/payment/refund-pending — adminApi.refunds
 * - POST /admin/warmup — adminApi.warmup（安全预热，不覆盖运行中库存）
 *
 * 关键 computed / 函数：
 * - totalInventory：当前页秒杀库存合计，仅作对账参考
 * - openAdd / openEdit / saveGoods / removeGoods：商品弹窗与下架确认
 * - warmup：触发缓存与布隆过滤器预热
 * - logout：清管理员会话并回到后台登录
 */
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Box,
  Delete,
  Edit,
  Plus,
  Refresh,
  SwitchButton,
  User,
  Wallet,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { Goods, GoodsForm, PaymentRecord, UserSummary } from '../types'

const router = useRouter()
const auth = useAuthStore()
const activeTab = ref('overview')
const loading = ref(false)
const saving = ref(false)
const warming = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const formRef = ref<FormInstance>()

const goods = ref<Goods[]>([])
const users = ref<UserSummary[]>([])
const refunds = ref<PaymentRecord[]>([])
const goodsTotal = ref(0)
const usersTotal = ref(0)
const refundsTotal = ref(0)
const goodsPage = ref(1)
const usersPage = ref(1)
const refundsPage = ref(1)
const pageSize = 20

function formatDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const emptyForm = (): GoodsForm => ({
  goodsName: '',
  goodsTitle: '',
  goodsImg: '',
  goodsDetail: '',
  goodsPrice: 100,
  goodsStock: 100,
  seckillPrice: 9.9,
  seckillStock: 10,
  startDate: formatDate(new Date(Date.now() + 5 * 60_000)),
  endDate: formatDate(new Date(Date.now() + 65 * 60_000)),
})

const form = reactive<GoodsForm>(emptyForm())
const rules: FormRules = {
  goodsName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  goodsPrice: [{ required: true, message: '请输入商品原价', trigger: 'blur' }],
  goodsStock: [{ required: true, message: '请输入普通库存', trigger: 'blur' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价格', trigger: 'blur' }],
  seckillStock: [{ required: true, message: '请输入秒杀库存', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

const totalInventory = computed(() =>
  goods.value.reduce((sum, item) => sum + Number(item.stockCount || 0), 0),
)

async function loadGoods() {
  const result = await adminApi.goods(goodsPage.value, pageSize)
  goods.value = result.records
  goodsTotal.value = result.total
}

async function loadUsers() {
  const result = await adminApi.users(usersPage.value, pageSize)
  users.value = result.records
  usersTotal.value = result.total
}

async function loadRefunds() {
  const result = await adminApi.refunds(refundsPage.value, pageSize)
  refunds.value = result.records
  refundsTotal.value = result.total
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadGoods(), loadUsers(), loadRefunds()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '控制台数据加载失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editing.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(item: Goods) {
  editing.value = true
  Object.assign(form, {
    id: item.id,
    goodsName: item.goodsName,
    goodsTitle: item.goodsTitle || '',
    goodsImg: item.goodsImg || '',
    goodsDetail: item.goodsDetail || '',
    goodsPrice: Number(item.goodsPrice),
    goodsStock: Number(item.goodsStock),
    seckillPrice: Number(item.seckillPrice),
    seckillStock: Number(item.stockCount),
    startDate: item.startDate,
    endDate: item.endDate,
  })
  dialogVisible.value = true
}

async function saveGoods() {
  await formRef.value?.validate()
  if (form.seckillStock > form.goodsStock) {
    ElMessage.warning('秒杀库存不能大于普通库存')
    return
  }
  if (form.seckillPrice > form.goodsPrice) {
    ElMessage.warning('秒杀价格不能高于商品原价')
    return
  }
  if (new Date(form.endDate.replace(' ', 'T')) <= new Date(form.startDate.replace(' ', 'T'))) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  saving.value = true
  try {
    const message = editing.value
      ? await adminApi.updateGoods(form)
      : await adminApi.addGoods(form)
    ElMessage.success(message)
    dialogVisible.value = false
    await loadGoods()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品保存失败')
  } finally {
    saving.value = false
  }
}

async function removeGoods(item: Goods) {
  try {
    await ElMessageBox.confirm(
      `确认下架“${item.goodsName}”吗？存在待支付订单时后端会拒绝操作。`,
      '下架商品',
      { type: 'warning', confirmButtonText: '确认下架', cancelButtonText: '取消' },
    )
    const message = await adminApi.deleteGoods(item.id)
    ElMessage.success(message)
    await loadGoods()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '下架失败')
    }
  }
}

async function warmup() {
  warming.value = true
  try {
    ElMessage.success(await adminApi.warmup())
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '缓存预热失败')
  } finally {
    warming.value = false
  }
}

function logout() {
  auth.logoutAdmin()
  router.replace('/admin/login')
}

onMounted(loadAll)
</script>

<template>
  <div class="admin-page">
    <aside class="admin-sidebar">
      <div class="console-title">
        <small>Operations</small>
        <strong>运营控制台</strong>
      </div>
      <button :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">
        <el-icon><Box /></el-icon>运行概览
      </button>
      <button :class="{ active: activeTab === 'goods' }" @click="activeTab = 'goods'">
        <el-icon><Box /></el-icon>商品管理
      </button>
      <button :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">
        <el-icon><User /></el-icon>用户管理
      </button>
      <button :class="{ active: activeTab === 'refunds' }" @click="activeTab = 'refunds'">
        <el-icon><Wallet /></el-icon>待退款工单
      </button>
      <button class="logout-button" @click="logout">
        <el-icon><SwitchButton /></el-icon>退出后台
      </button>
    </aside>

    <section class="admin-content" v-loading="loading">
      <div class="admin-heading">
        <div>
          <span class="eyebrow">Admin Workspace</span>
          <h1>
            {{
              activeTab === 'overview'
                ? '运行概览'
                : activeTab === 'goods'
                  ? '商品管理'
                  : activeTab === 'users'
                    ? '用户管理'
                    : '待退款工单'
            }}
          </h1>
        </div>
        <div class="heading-actions">
          <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
          <el-button
            v-if="activeTab === 'goods'"
            type="primary"
            :icon="Plus"
            @click="openAdd"
          >
            新增商品
          </el-button>
        </div>
      </div>

      <template v-if="activeTab === 'overview'">
        <div class="metric-grid">
          <article class="metric-card">
            <span>秒杀商品</span><strong>{{ goodsTotal }}</strong><small>当前配置商品数</small>
          </article>
          <article class="metric-card">
            <span>注册用户</span><strong>{{ usersTotal }}</strong><small>平台用户总量</small>
          </article>
          <article class="metric-card warning">
            <span>待退款工单</span><strong>{{ refundsTotal }}</strong><small>需要人工跟进</small>
          </article>
          <article class="metric-card">
            <span>当前页库存</span><strong>{{ totalInventory }}</strong><small>Redis 对账参考值</small>
          </article>
        </div>
        <div class="surface operation-panel">
          <div>
            <h2>安全缓存预热</h2>
            <p>补充商品缓存和布隆过滤器，不会覆盖运行中的 Redis 可售库存。</p>
          </div>
          <el-button type="primary" :loading="warming" @click="warmup">执行预热</el-button>
        </div>
      </template>

      <template v-else-if="activeTab === 'goods'">
        <div class="surface table-panel">
          <el-table :data="goods" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <div class="goods-cell">
                  <img v-if="row.goodsImg" :src="row.goodsImg" :alt="row.goodsName" />
                  <div><b>{{ row.goodsName }}</b><small>{{ row.goodsTitle }}</small></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="130">
              <template #default="{ row }">
                <b class="price">¥{{ Number(row.seckillPrice).toFixed(2) }}</b>
                <small class="table-subtext">原价 ¥{{ Number(row.goodsPrice).toFixed(2) }}</small>
              </template>
            </el-table-column>
            <el-table-column prop="stockCount" label="秒杀库存" width="100" />
            <el-table-column prop="startDate" label="开始时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :icon="Edit" @click="openEdit(row as Goods)">编辑</el-button>
                <el-button text type="danger" :icon="Delete" @click="removeGoods(row as Goods)">
                  下架
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination
          v-if="goodsTotal > pageSize"
          background
          layout="prev, pager, next"
          :current-page="goodsPage"
          :page-size="pageSize"
          :total="goodsTotal"
          @current-change="(value: number) => { goodsPage = value; loadGoods() }"
        />
      </template>

      <template v-else-if="activeTab === 'users'">
        <div class="surface table-panel">
          <el-table :data="users" stripe>
            <el-table-column prop="id" label="手机号" min-width="150" />
            <el-table-column label="用户" min-width="180">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :src="row.head">{{ row.nickname?.slice(0, 1) }}</el-avatar>
                  <b>{{ row.nickname }}</b>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="registerDate" label="注册时间" min-width="170" />
            <el-table-column prop="lastLoginDate" label="最近登录" min-width="170">
              <template #default="{ row }">{{ row.lastLoginDate || '暂无记录' }}</template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination
          v-if="usersTotal > pageSize"
          background
          layout="prev, pager, next"
          :current-page="usersPage"
          :page-size="pageSize"
          :total="usersTotal"
          @current-change="(value: number) => { usersPage = value; loadUsers() }"
        />
      </template>

      <template v-else>
        <el-alert
          class="refund-alert"
          title="以下记录表示系统已收到付款，但订单无法履约，需要通过支付宝后台退款并人工闭环。"
          type="warning"
          show-icon
          :closable="false"
        />
        <div class="surface table-panel">
          <el-table :data="refunds" stripe>
            <el-table-column prop="orderId" label="订单号" min-width="190" />
            <el-table-column prop="tradeNo" label="支付宝交易号" min-width="210" />
            <el-table-column label="金额" width="120">
              <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="createDate" label="创建时间" width="170" />
            <el-table-column label="状态" width="130">
              <template #default><el-tag type="danger" effect="plain">待退款</el-tag></template>
            </el-table-column>
          </el-table>
          <div v-if="!refunds.length" class="empty-panel">目前没有待退款工单</div>
        </div>
        <el-pagination
          v-if="refundsTotal > pageSize"
          background
          layout="prev, pager, next"
          :current-page="refundsPage"
          :page-size="pageSize"
          :total="refundsTotal"
          @current-change="(value: number) => { refundsPage = value; loadRefunds() }"
        />
      </template>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑秒杀商品' : '新增秒杀商品'"
      width="min(680px, 94vw)"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="商品名称" prop="goodsName">
            <el-input v-model="form.goodsName" />
          </el-form-item>
          <el-form-item label="商品标题">
            <el-input v-model="form.goodsTitle" />
          </el-form-item>
          <el-form-item class="form-span" label="图片地址">
            <el-input v-model="form.goodsImg" placeholder="https://..." />
          </el-form-item>
          <el-form-item class="form-span" label="商品描述">
            <el-input v-model="form.goodsDetail" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="商品原价" prop="goodsPrice">
            <el-input-number v-model="form.goodsPrice" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="秒杀价格" prop="seckillPrice">
            <el-input-number v-model="form.seckillPrice" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="普通库存" prop="goodsStock">
            <el-input-number v-model="form.goodsStock" :min="1" :precision="0" />
          </el-form-item>
          <el-form-item label="秒杀库存" prop="seckillStock">
            <el-input-number v-model="form.seckillStock" :min="editing ? 0 : 1" :precision="0" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startDate">
            <el-date-picker
              v-model="form.startDate"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endDate">
            <el-date-picker
              v-model="form.endDate"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGoods">保存商品</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
  min-height: calc(100vh - 72px);
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  background: #eef2f7;
}

.admin-sidebar {
  position: sticky;
  top: 72px;
  height: calc(100vh - 72px);
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 30px 18px;
  color: #d7dce5;
  background: #172033;
}

.console-title {
  padding: 0 12px 24px;
}

.console-title small,
.console-title strong {
  display: block;
}

.console-title small {
  color: #8f9bad;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.console-title strong {
  margin-top: 6px;
  color: #fff;
  font-size: 19px;
}

.admin-sidebar button {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 13px;
  border: 0;
  border-radius: 9px;
  color: #aeb8c7;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.admin-sidebar button:hover,
.admin-sidebar button.active {
  color: #fff;
  background: #29364a;
}

.admin-sidebar .logout-button {
  margin-top: auto;
  color: #fda29b;
}

.admin-content {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  padding: 36px 0 64px;
}

.admin-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.admin-heading h1 {
  margin: 0;
  font-size: 30px;
}

.heading-actions {
  display: flex;
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 15px;
  background: #fff;
}

.metric-card span,
.metric-card small {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 12px 0 8px;
  color: var(--ink);
  font-size: 34px;
}

.metric-card.warning strong {
  color: var(--danger);
}

.operation-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 18px;
  padding: 26px;
}

.operation-panel h2 {
  margin: 0;
  font-size: 18px;
}

.operation-panel p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.table-panel {
  overflow: hidden;
}

.goods-cell,
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-cell img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #eef2f7;
}

.goods-cell b,
.goods-cell small,
.table-subtext {
  display: block;
}

.goods-cell small,
.table-subtext {
  max-width: 240px;
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.refund-alert {
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.form-span {
  grid-column: 1 / -1;
}

@media (max-width: 980px) {
  .admin-page {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    height: auto;
    flex-direction: row;
    overflow-x: auto;
    padding: 12px 16px;
  }

  .console-title,
  .admin-sidebar .logout-button {
    display: none;
  }

  .admin-sidebar button {
    flex: 0 0 auto;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .admin-content {
    width: calc(100% - 28px);
  }

  .admin-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-span {
    grid-column: auto;
  }

  .operation-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
