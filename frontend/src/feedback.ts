/**
 * 跨路由一次性提示。登录/注册成功后跳转，下一页横幅仍能看到结果。
 */
export type BannerKind = 'success' | 'error' | 'warning' | 'info'

export interface BannerState {
  kind: BannerKind
  text: string
}

const FLASH_KEY = 'seckill-flash'

export function setFlash(kind: BannerKind, text: string) {
  sessionStorage.setItem(FLASH_KEY, JSON.stringify({ kind, text } satisfies BannerState))
}

export function takeFlash(): BannerState | null {
  const raw = sessionStorage.getItem(FLASH_KEY)
  if (!raw) return null
  sessionStorage.removeItem(FLASH_KEY)
  try {
    return JSON.parse(raw) as BannerState
  } catch {
    return null
  }
}
