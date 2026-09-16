<script setup lang="ts">
/**
 * 秒杀会场首页：分页展示商品卡片，按时间窗与库存计算场次状态。
 *
 * 后端接口：
 * - GET /goods/list — goodsApi.list
 *
 * 关键函数：
 * - parseTime：把后端 "yyyy-MM-dd HH:mm:ss" 转成时间戳
 * - stateOf：即将开始 / 抢购中 / 已售罄 / 已结束
 * - loadGoods / changePage：拉取当前页并在翻页时滚回顶部
 */
import { onMounted, ref } from 'vue'
import { ArrowRight, Clock, Goods as GoodsIcon } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { goodsApi } from '../api'
import { errorMessage } from '../api/errors'
import StatusBanner from '../components/StatusBanner.vue'
import type { Goods } from '../types'

const router = useRouter()
const loading = ref(true)
const goods = ref<Goods[]>([])
const page = ref(1)
const pageSize = 12
const total = ref(0)
const pageError = ref('')

function parseTime(value: string) {
  return new Date(value.replace(' ', 'T')).getTime()
}

function stateOf(item: Goods) {
  const now = Date.now()
  if (now < parseTime(item.startDate)) return { label: '即将开始', type: 'info' }
  if (now > parseTime(item.endDate)) return { label: '已结束', type: 'info' }
  if (item.stockCount <= 0) return { label: '已售罄', type: 'danger' }
  return { label: '抢购中', type: 'success' }
}

async function loadGoods() {
  loading.value = true
  pageError.value = ''
  try {
    const result = await goodsApi.list(page.value, pageSize)
    goods.value = result.records
    total.value = result.total
  } catch (error) {
    goods.value = []
    pageError.value = errorMessage(error, '商品加载失败')
  } finally {
    loading.value = false
  }
}

function changePage(value: number) {
  page.value = value
  loadGoods()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadGoods)
</script>

<template>
  <div class="page-container">
    <section class="hero-panel">
      <div>
        <span class="eyebrow">Flash Sale</span>
        <h1 class="page-title">把握当下，抢到真正值得的好物</h1>
        <p class="page-description">
          实时库存、异步排队与一人一单保护，让每一次抢购都清晰、快速、可追踪。
        </p>
      </div>
      <div class="hero-metric">
        <span>当前场次</span>
        <strong>{{ total }}</strong>
        <small>件秒杀商品</small>
      </div>
    </section>

    <div class="section-heading">
      <div>
        <h2>限时会场</h2>
        <p>库存有限，提交成功后请及时完成支付</p>
      </div>
      <span class="live-indicator"><i /> 实时更新</span>
    </div>

    <StatusBanner v-if="pageError" kind="error" :text="pageError" />

    <div v-if="loading" class="goods-grid">
      <div v-for="index in 6" :key="index" class="goods-card surface skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-image" />
            <div style="padding: 20px">
              <el-skeleton-item variant="h3" style="width: 65%" />
              <el-skeleton-item variant="text" style="margin-top: 14px" />
              <el-skeleton-item variant="text" style="width: 35%; margin-top: 20px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="goods.length" class="goods-grid">
      <article
        v-for="item in goods"
        :key="item.id"
        class="goods-card surface"
        @click="router.push(`/goods/${item.id}`)"
      >
        <div class="goods-image">
          <img v-if="item.goodsImg" :src="item.goodsImg" :alt="item.goodsName" />
          <el-icon v-else><GoodsIcon /></el-icon>
          <el-tag class="goods-state" :type="stateOf(item).type as never" effect="dark">
            {{ stateOf(item).label }}
          </el-tag>
        </div>
        <div class="goods-content">
          <h3>{{ item.goodsName }}</h3>
          <p>{{ item.goodsTitle || item.goodsDetail || '精选限时秒杀商品' }}</p>
          <div class="time-row">
            <el-icon><Clock /></el-icon>
            {{ item.startDate }} — {{ item.endDate }}
          </div>
          <div class="goods-footer">
            <div>
              <span class="price">¥<strong>{{ Number(item.seckillPrice).toFixed(2) }}</strong></span>
              <span class="old-price">¥{{ Number(item.goodsPrice).toFixed(2) }}</span>
            </div>
            <span class="stock">剩余 {{ item.stockCount }}</span>
          </div>
          <el-button class="detail-button" type="primary">
            查看详情
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </article>
    </div>

    <div v-else class="surface empty-panel">
      {{ pageError ? '会场暂时无法加载，请刷新页面重试' : '当前没有可展示的秒杀商品' }}
    </div>

    <el-pagination
      v-if="total > pageSize"
      background
      layout="prev, pager, next"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      @current-change="changePage"
    />
  </div>
</template>

<style scoped>
.hero-panel {
  min-height: 285px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 190px;
  align-items: center;
  gap: 48px;
  padding: 54px 62px;
  border-radius: 24px;
  color: #fff;
  background: #172033;
}

.hero-panel .eyebrow {
  color: #84adff;
}

.hero-panel .page-title {
  max-width: 720px;
  color: #fff;
}

.hero-panel .page-description {
  color: #c5cbd6;
}

.hero-metric {
  padding: 28px;
  border: 1px solid #364153;
  border-radius: 18px;
  background: #202b3d;
}

.hero-metric span,
.hero-metric small {
  display: block;
  color: #aab3c1;
  font-size: 12px;
}

.hero-metric strong {
  display: block;
  margin: 8px 0;
  font-size: 48px;
  line-height: 1;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 42px 0 22px;
}

.section-heading h2 {
  margin: 0;
  font-size: 24px;
}

.section-heading p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--success);
  font-size: 13px;
  font-weight: 600;
}

.live-indicator i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.goods-card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease;
}

.goods-card:hover {
  transform: translateY(-4px);
  border-color: #b8c7e0;
}

.goods-image {
  position: relative;
  height: 210px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #8490a3;
  background: #eef2f7;
  font-size: 52px;
}

.goods-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 250ms ease;
}

.goods-card:hover .goods-image img {
  transform: scale(1.035);
}

.goods-state {
  position: absolute;
  top: 14px;
  right: 14px;
}

.goods-content {
  padding: 20px;
}

.goods-content h3 {
  margin: 0;
  color: var(--ink);
  font-size: 18px;
}

.goods-content > p {
  height: 42px;
  margin: 8px 0 14px;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  border-top: 1px solid var(--line);
  color: #7c8799;
  font-size: 11px;
}

.goods-footer {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stock {
  color: var(--muted);
  font-size: 12px;
}

.detail-button {
  width: 100%;
  margin-top: 14px;
}

.skeleton-card {
  cursor: default;
}

.skeleton-image {
  width: 100%;
  height: 210px;
  border-radius: 0;
}

@media (max-width: 900px) {
  .hero-panel {
    grid-template-columns: 1fr;
    padding: 42px;
  }

  .hero-metric {
    display: none;
  }

  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 580px) {
  .hero-panel {
    min-height: auto;
    padding: 34px 26px;
  }

  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
