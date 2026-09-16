<script setup lang="ts">
/**
 * 商品详情与秒杀提交页。未登录可浏览；活动进行中才拉验证码并允许下单。
 *
 * 后端接口：
 * - GET /goods/detail/{id} — goodsApi.detail
 * - GET /seckill/captcha — seckillApi.captcha（算术验证码 Blob）
 * - GET /seckill/path — seckillApi.path（校验验证码，换动态 path）
 * - POST /seckill/{path}/doSeckill — seckillApi.submit
 * - GET /seckill/result — seckillApi.result（0 排队中，-1 失败，其它为订单号）
 *
 * 关键 computed / 函数：
 * - phase：upcoming / active / soldout / ended
 * - actionLabel / canSubmit：按钮文案与是否允许提交
 * - refreshCaptcha：刷新验证码并释放旧 Object URL
 * - submit：换 path 后入队，再 pollResult 最多约 30 秒
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Back, Goods as GoodsIcon, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { goodsApi, seckillApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { Goods } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const goodsId = Number(route.params.id)
const goods = ref<Goods>()
const loading = ref(true)
const captchaUrl = ref('')
const captchaAnswer = ref('')
const submitting = ref(false)
const queueState = ref('')
const now = ref(Date.now())
let clockTimer: number | undefined
let pollTimer: number | undefined

const phase = computed(() => {
  if (!goods.value) return 'invalid'
  const start = new Date(goods.value.startDate.replace(' ', 'T')).getTime()
  const end = new Date(goods.value.endDate.replace(' ', 'T')).getTime()
  if (now.value < start) return 'upcoming'
  if (now.value > end) return 'ended'
  if (goods.value.stockCount <= 0) return 'soldout'
  return 'active'
})

const actionLabel = computed(() => {
  if (!auth.isLoggedIn) return '登录后参与抢购'
  if (phase.value === 'upcoming') return '活动尚未开始'
  if (phase.value === 'ended') return '活动已经结束'
  if (phase.value === 'soldout') return '商品已售罄'
  return '验证并提交抢购'
})

const canSubmit = computed(
  () => auth.isLoggedIn && phase.value === 'active' && Boolean(captchaAnswer.value),
)

async function loadGoods() {
  try {
    goods.value = await goodsApi.detail(goodsId)
    if (!goods.value) throw new Error('商品不存在')
    if (auth.isLoggedIn && phase.value === 'active') await refreshCaptcha()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshCaptcha() {
  if (captchaUrl.value) URL.revokeObjectURL(captchaUrl.value)
  captchaAnswer.value = ''
  try {
    captchaUrl.value = await seckillApi.captcha(goodsId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '验证码获取失败')
  }
}

async function submit() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const path = await seckillApi.path(goodsId, captchaAnswer.value)
    await seckillApi.submit(path, goodsId)
    queueState.value = '请求已进入队列，正在确认订单…'
    pollResult(0)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '抢购提交失败')
    await refreshCaptcha()
    submitting.value = false
  }
}

function pollResult(attempt: number) {
  window.clearTimeout(pollTimer)
  pollTimer = window.setTimeout(async () => {
    try {
      const result = await seckillApi.result(goodsId)
      if (String(result) === '-1') {
        queueState.value = '库存不足，本轮未抢到'
        ElMessage.warning(queueState.value)
        submitting.value = false
        return
      }
      if (String(result) !== '0') {
        queueState.value = `订单创建成功：${result}`
        ElMessage.success('抢购成功，请尽快支付')
        submitting.value = false
        router.push('/orders')
        return
      }
      if (attempt >= 30) {
        queueState.value = '仍在排队处理中，请稍后前往订单页查看'
        submitting.value = false
        return
      }
      pollResult(attempt + 1)
    } catch (error) {
      queueState.value = error instanceof Error ? error.message : '结果查询失败'
      submitting.value = false
    }
  }, 1000)
}

onMounted(() => {
  loadGoods()
  clockTimer = window.setInterval(() => (now.value = Date.now()), 1000)
})

onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  window.clearTimeout(pollTimer)
  if (captchaUrl.value) URL.revokeObjectURL(captchaUrl.value)
})
</script>

<template>
  <div class="page-container">
    <el-button text :icon="Back" @click="router.push('/')">返回会场</el-button>

    <div v-if="loading" class="detail-card surface">
      <el-skeleton animated :rows="8" />
    </div>

    <div v-else-if="goods" class="detail-card surface">
      <div class="detail-image">
        <img v-if="goods.goodsImg" :src="goods.goodsImg" :alt="goods.goodsName" />
        <el-icon v-else><GoodsIcon /></el-icon>
      </div>

      <section class="detail-content">
        <el-tag
          :type="phase === 'active' ? 'success' : phase === 'soldout' ? 'danger' : 'info'"
          effect="plain"
        >
          {{
            phase === 'active'
              ? '抢购进行中'
              : phase === 'upcoming'
                ? '即将开始'
                : phase === 'soldout'
                  ? '已售罄'
                  : '已结束'
          }}
        </el-tag>
        <h1>{{ goods.goodsName }}</h1>
        <p class="subtitle">{{ goods.goodsTitle || goods.goodsDetail }}</p>

        <div class="price-panel">
          <div>
            <span>秒杀价</span>
            <div class="price">¥<strong>{{ Number(goods.seckillPrice).toFixed(2) }}</strong></div>
          </div>
          <div class="price-meta">
            <span>日常价 <s>¥{{ Number(goods.goodsPrice).toFixed(2) }}</s></span>
            <span>剩余库存 <b>{{ goods.stockCount }}</b></span>
          </div>
        </div>

        <dl class="sale-info">
          <div><dt>开始时间</dt><dd>{{ goods.startDate }}</dd></div>
          <div><dt>结束时间</dt><dd>{{ goods.endDate }}</dd></div>
          <div><dt>购买规则</dt><dd>每位用户限购一件，15 分钟内完成支付</dd></div>
        </dl>

        <div v-if="auth.isLoggedIn && phase === 'active'" class="captcha-panel">
          <div class="captcha-image" title="点击刷新" @click="refreshCaptcha">
            <img v-if="captchaUrl" :src="captchaUrl" alt="算术验证码" />
            <span v-else>加载中</span>
          </div>
          <el-input
            v-model="captchaAnswer"
            size="large"
            placeholder="请输入计算结果"
            @keyup.enter="submit"
          />
          <el-button :icon="Refresh" size="large" @click="refreshCaptcha">刷新</el-button>
        </div>

        <el-alert v-if="queueState" :title="queueState" type="info" :closable="false" show-icon />

        <el-button
          class="seckill-button"
          size="large"
          type="primary"
          :disabled="auth.isLoggedIn && !canSubmit"
          :loading="submitting"
          @click="submit"
        >
          {{ actionLabel }}
        </el-button>
      </section>
    </div>

    <div v-else class="surface empty-panel">商品不存在或已下架</div>
  </div>
</template>

<style scoped>
.detail-card {
  display: grid;
  grid-template-columns: minmax(340px, 0.9fr) minmax(420px, 1.1fr);
  gap: 48px;
  margin-top: 18px;
  padding: 40px;
}

.detail-image {
  min-height: 480px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 16px;
  color: #8490a3;
  background: #eef2f7;
  font-size: 80px;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-content h1 {
  margin: 16px 0 8px;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.subtitle {
  min-height: 48px;
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.price-panel {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 26px 0;
  padding: 20px 22px;
  border-radius: 14px;
  background: #fff6f5;
}

.price-panel span {
  color: var(--muted);
  font-size: 12px;
}

.price-meta {
  display: flex;
  flex-direction: column;
  gap: 9px;
  text-align: right;
}

.sale-info {
  margin: 0 0 24px;
}

.sale-info div {
  display: grid;
  grid-template-columns: 92px 1fr;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.sale-info dt {
  color: var(--muted);
}

.sale-info dd {
  margin: 0;
}

.captcha-panel {
  display: grid;
  grid-template-columns: 130px 1fr auto;
  gap: 10px;
  margin-bottom: 16px;
}

.captcha-image {
  height: 40px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.captcha-image img {
  width: 130px;
  height: 32px;
}

.captcha-image span {
  color: var(--muted);
  font-size: 12px;
}

.seckill-button {
  width: 100%;
  height: 48px;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .detail-card {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .detail-image {
    min-height: 320px;
  }
}

@media (max-width: 560px) {
  .captcha-panel {
    grid-template-columns: 130px 1fr;
  }

  .captcha-panel .el-button {
    display: none;
  }
}
</style>
