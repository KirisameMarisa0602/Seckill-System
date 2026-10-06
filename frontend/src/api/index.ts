import axios, { type AxiosRequestConfig } from 'axios'
import { ApiError, toApiError } from './errors'
import type {
  ApiResponse,
  DeliveryAddress,
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

export const authApi = {
  login: (mobile: string, password: string) =>
    request<string>({ method: 'POST', url: '/user/login', data: { mobile, password } }),
  register: (nickname: string, mobile: string, password: string) =>
    request<string>({
      method: 'POST',
      url: '/user/register',
      data: { nickname, mobile, password },
    }),
  address: () => request<DeliveryAddress | null>({ url: '/user/address' }),
  saveAddress: (receiverName: string, receiverPhone: string, detail: string) =>
    request<DeliveryAddress>({
      method: 'POST',
      url: '/user/address',
      data: { receiverName, receiverPhone, detail },
    }),
}

export const goodsApi = {
  list: (page = 1, pageSize = 12) =>
    request<PageResult<Goods>>({ url: '/goods/list', params: { page, pageSize } }),
  detail: (goodsId: number) => request<Goods>({ url: `/goods/detail/${goodsId}` }),
}

export const seckillApi = {

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
