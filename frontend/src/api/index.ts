import axios, { type AxiosRequestConfig } from 'axios'
import type {
  ApiResponse,
  Goods,
  GoodsForm,
  Order,
  PageResult,
  PaymentRecord,
  UserSummary,
} from '../types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
})

api.interceptors.request.use((config) => {
  const userToken = localStorage.getItem('seckill-user-token')
  const adminToken = localStorage.getItem('seckill-admin-token')
  if (userToken) config.headers.set('token', userToken)
  if (adminToken) config.headers.set('Admin-Token', adminToken)
  return config
})

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await api.request<ApiResponse<T>>(config)
  const payload = response.data
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.obj
}

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

export const goodsApi = {
  list: (page = 1, pageSize = 12) =>
    request<PageResult<Goods>>({ url: '/goods/list', params: { page, pageSize } }),
  detail: (goodsId: number) => request<Goods>({ url: `/goods/detail/${goodsId}` }),
}

export const seckillApi = {
  captcha: async (goodsId: number) => {
    const response = await api.get<Blob>('/seckill/captcha', {
      params: { goodsId },
      responseType: 'blob',
    })
    return URL.createObjectURL(response.data)
  },
  path: (goodsId: number, captcha: string) =>
    request<string>({ url: '/seckill/path', params: { goodsId, captcha } }),
  submit: (path: string, goodsId: number) =>
    request<number>({ method: 'POST', url: `/seckill/${path}/doSeckill`, params: { goodsId } }),
  result: (goodsId: number) =>
    request<string | number>({ url: '/seckill/result', params: { goodsId } }),
}

export const orderApi = {
  list: (page = 1, pageSize = 20) =>
    request<PageResult<Order>>({ url: '/order/list', params: { page, pageSize } }),
  detail: (orderId: string) => request<Order>({ url: `/order/detail/${orderId}` }),
  paymentPage: async (orderId: string) => {
    const response = await api.get<string>(`/pay/create/${orderId}`, { responseType: 'text' })
    return response.data
  },
}

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
