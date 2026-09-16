/**
 * 后端 HTTP 封装。开发环境默认走 Vite `/api` 代理，生产可通过 `VITE_API_BASE_URL` 覆盖。
 *
 * 请求拦截器同时附带用户头 `token` 与管理员头 `Admin-Token`。
 * `request()` 解包后端 `RespBean`：`code === 200` 时返回 `obj`，否则抛错。
 * 验证码与支付页是 Blob/HTML，不走该解包。
 */
import axios, { type AxiosRequestConfig } from 'axios'
import { ApiError, toApiError } from './errors'
import type {
  ApiResponse,
  Goods,
  GoodsForm,
  Order,
  PageResult,
  PaymentRecord,
  UserSummary,
} from '../types'

/** Axios 实例。验证码/支付页直接用它，以便保留 Blob 与 HTML 原文。 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
})

api.interceptors.request.use((config) => {
  // 用户头 token、管理员头 Admin-Token 可同时存在，互不影响
  const userToken = localStorage.getItem('seckill-user-token')
  const adminToken = localStorage.getItem('seckill-admin-token')
  if (userToken) config.headers.set('token', userToken)
  if (adminToken) config.headers.set('Admin-Token', adminToken)
  return config
})

/** 解包 `RespBean`：成功返回 `obj`，失败抛出带后端 code/message 的 {@link ApiError}。 */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await api.request<ApiResponse<T>>(config)
    const payload = response.data
    if (!payload || typeof payload !== 'object') {
      throw new ApiError('服务器返回了无法解析的响应', 500)
    }
    if (payload.code !== 200) {
      throw new ApiError(payload.message || '请求失败', payload.code)
    }
    return payload.obj
  } catch (error) {
    throw toApiError(error, '请求失败')
  }
}

async function parseBlobError(data: Blob, fallback: string) {
  const text = await data.text()
  try {
    const payload = JSON.parse(text) as ApiResponse
    throw new ApiError(payload.message || fallback, payload.code || 500)
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError(text || fallback, 500)
  }
}

/** 用户登录 / 注册。登录成功返回 Token 字符串。 */
export const authApi = {
  login: (mobile: string, password: string) =>
    request<string>({ method: 'POST', url: '/user/login', data: { mobile, password } }),
  register: (nickname: string, mobile: string, password: string) =>
    request<string>({
      method: 'POST',
      url: '/user/register',
      data: { nickname, mobile, password },
    }),
}

/** 秒杀会场商品列表与详情（公开接口）。 */
export const goodsApi = {
  list: (page = 1, pageSize = 12) =>
    request<PageResult<Goods>>({ url: '/goods/list', params: { page, pageSize } }),
  detail: (goodsId: number) => request<Goods>({ url: `/goods/detail/${goodsId}` }),
}

/** 秒杀链路：验证码图 → 动态 path → 提交抢购 → 轮询结果。 */
export const seckillApi = {
  /** 拉取算术验证码图，返回可给 `<img>` 使用的 Object URL。 */
  captcha: async (goodsId: number) => {
    try {
      const response = await api.get<Blob>('/seckill/captcha', {
        params: { goodsId },
        responseType: 'blob',
        validateStatus: () => true,
      })
      const data = response.data
      const asJson = data.type?.includes('json') || response.status >= 400
      if (asJson) await parseBlobError(data, '验证码获取失败')
      return URL.createObjectURL(data)
    } catch (error) {
      throw toApiError(error, '验证码获取失败')
    }
  },
  /** 校验验证码并换取一次性秒杀 path。 */
  path: (goodsId: number, captcha: string) =>
    request<string>({ url: '/seckill/path', params: { goodsId, captcha } }),
  /** 携带动态 path 提交抢购，成功表示已入队。 */
  submit: (path: string, goodsId: number) =>
    request<number>({ method: 'POST', url: `/seckill/${path}/doSeckill`, params: { goodsId } }),
  /** 查询结果：`0` 排队中，`-1` 失败，其它值为订单号。 */
  result: (goodsId: number) =>
    request<string | number>({ url: '/seckill/result', params: { goodsId } }),
}

/** 用户订单列表，以及打开支付宝收银台 HTML。 */
export const orderApi = {
  list: (page = 1, pageSize = 20) =>
    request<PageResult<Order>>({ url: '/order/list', params: { page, pageSize } }),
  /** 订单详情。 */
  detail: (orderId: string) => request<Order>({ url: `/order/detail/${orderId}` }),
  /** 返回收银台 HTML 原文，由订单页写入新窗口。 */
  paymentPage: async (orderId: string) => {
    try {
      const response = await api.get<string>(`/pay/create/${orderId}`, {
        responseType: 'text',
        validateStatus: () => true,
      })
      const body = response.data
      if (response.status >= 400 || body.trim().startsWith('{')) {
        try {
          const payload = JSON.parse(body) as ApiResponse
          throw new ApiError(payload.message || '无法打开支付页', payload.code || response.status)
        } catch (error) {
          if (error instanceof ApiError) throw error
          if (response.status >= 400) throw new ApiError('无法打开支付页', response.status)
        }
      }
      return body
    } catch (error) {
      throw toApiError(error, '无法打开支付页')
    }
  },
}

/** 运营后台：管理员登录、用户/商品 CRUD、缓存预热、待退款工单。 */
export const adminApi = {
  login: (username: string, password: string) =>
    request<string>({ method: 'POST', url: '/admin/login', data: { username, password } }),
  users: (page = 1, pageSize = 20) =>
    request<PageResult<UserSummary>>({
      url: '/admin/user/list',
      params: { page, pageSize },
    }),
  goods: (page = 1, pageSize = 20) =>
    request<PageResult<Goods>>({
      url: '/admin/goods/list',
      params: { page, pageSize },
    }),
  addGoods: (data: GoodsForm) =>
    request<string>({ method: 'POST', url: '/admin/goods/add', data }),
  updateGoods: (data: GoodsForm) =>
    request<string>({ method: 'POST', url: '/admin/goods/update', data }),
  deleteGoods: (goodsId: number) =>
    request<string>({ method: 'POST', url: `/admin/goods/delete/${goodsId}` }),
  warmup: () => request<string>({ method: 'POST', url: '/admin/warmup' }),
  refunds: (page = 1, pageSize = 20) =>
    request<PageResult<PaymentRecord>>({
      url: '/admin/payment/refund-pending',
      params: { page, pageSize },
    }),
}
