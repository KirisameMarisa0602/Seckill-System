/**
 * 把 Axios / RespBean 失败收成带业务码的 Error，页面用 `message` 展示即可。
 */
import axios from 'axios'

/** 对应后端 `RespBean.code`；网络层失败用 HTTP 状态或 0。 */
export class ApiError extends Error {
  readonly code: number

  constructor(message: string, code = 500) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/** 取出可读文案；优先后端 `message`。 */
export function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error && error.message) return error.message
  return fallback
}

/** 取出业务码；非 {@link ApiError} 时返回 0。 */
export function errorCode(error: unknown) {
  return error instanceof ApiError ? error.code : 0
}

/** Axios 失败、HTTP 4xx 里的 JSON、纯文本 CORS 拒绝都转成 {@link ApiError}。 */
export function toApiError(error: unknown, fallback: string): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    const data = error.response?.data
    if (data && typeof data === 'object' && 'message' in data) {
      const payload = data as { code?: number; message?: string }
      return new ApiError(payload.message || fallback, Number(payload.code) || error.response?.status || 500)
    }
    if (typeof data === 'string' && data.trim()) {
      if (data.includes('Invalid CORS request')) {
        return new ApiError('跨域被拒绝，请使用 http://localhost:3000 打开页面', 403)
      }
      return new ApiError(data, error.response?.status || 500)
    }
    if (error.code === 'ECONNABORTED') {
      return new ApiError('请求超时，请稍后重试', 504)
    }
    if (!error.response) {
      return new ApiError('无法连接服务器，请确认 Nginx 与两个后端实例已启动', 503)
    }
    if (error.response.status === 401) {
      return new ApiError('登录已失效，请重新登录', 401)
    }
    if (error.response.status === 403) {
      return new ApiError('请求被拒绝，请使用 http://localhost:3000 访问', 403)
    }
    if (error.response.status === 429) {
      return new ApiError('操作过于频繁，请稍后再试', 429)
    }
    return new ApiError(fallback, error.response.status)
  }
  if (error instanceof Error && error.message) {
    return new ApiError(error.message)
  }
  return new ApiError(fallback)
}
