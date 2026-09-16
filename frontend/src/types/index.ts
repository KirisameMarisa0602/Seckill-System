/**
 * 与后端 VO 对齐的前端类型。字段名保持 camelCase，对应 Jackson 下划线转驼峰后的 JSON。
 */

/** 统一响应体，对应后端 `RespBean`。业务数据在 `obj`。 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  obj: T
}

/** 分页结果，对应后端 `PageResult`。 */
export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

/** 秒杀商品视图：普通商品字段 + 秒杀价、可售库存与时间窗。 */
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

/** 订单。status：-2 待退款、-1 已取消、0 待支付、1 已支付。 */
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

/** 后台用户摘要。`id` 即注册手机号。 */
export interface UserSummary {
  id: number
  nickname: string
  head?: string
  registerDate: string
  lastLoginDate?: string
}

/** 支付流水，后台待退款工单使用。 */
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

/** 后台新增/编辑商品表单。`seckillStock` 对应秒杀可售库存。 */
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
