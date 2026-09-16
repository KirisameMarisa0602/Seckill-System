export interface ApiResponse<T = unknown> {
  code: number
  message: string
  obj: T
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export interface Goods {
  id: number
  goodsName: string
  goodsTitle?: string
  goodsImg?: string
  goodsDetail?: string
  goodsPrice: number
  goodsStock: number
  seckillPrice: number
  stockCount: number
  startDate: string
  endDate: string
}

export interface Order {
  id: string
  userId: number
  goodsId: number
  goodsName: string
  goodsCount: number
  goodsPrice: number
  status: number
  createDate: string
  payDate?: string
}

export interface UserSummary {
  id: number
  nickname: string
  head?: string
  registerDate: string
  lastLoginDate?: string
}

export interface PaymentRecord {
  id: string
  orderId: string
  tradeNo: string
  amount: number
  appId: string
  sellerId?: string
  status: string
  createDate: string
  updateDate: string
}

export interface GoodsForm {
  id?: number
  goodsName: string
  goodsTitle: string
  goodsImg: string
  goodsDetail: string
  goodsPrice: number
  goodsStock: number
  seckillPrice: number
  seckillStock: number
  startDate: string
  endDate: string
}
