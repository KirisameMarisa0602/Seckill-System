-- 原子扣减秒杀库存，并写入一人一单标记与 Outbox 事件。
--
-- KEYS[1] 可售库存
-- KEYS[2] 用户对该商品的下单标记
-- KEYS[3] 售罄标记
-- KEYS[4] Outbox 待投递 ZSET
-- KEYS[5] Outbox 事件 Hash 前缀（再拼接 ARGV[2] eventId）
-- ARGV[1] 用户下单标记 TTL（秒）
-- ARGV[2] 事件 ID
-- ARGV[3] 用户 ID
-- ARGV[4] 商品 ID
-- ARGV[5] 商品名
-- ARGV[6] 秒杀价
-- ARGV[7] Outbox ZSET score（一般是当前毫秒时间戳）
--
-- 返回：1 扣减成功；2 已下过单；0 无库存或库存 key 不存在。

local stockKey = KEYS[1]
local userOrderKey = KEYS[2]
local stockEmptyKey = KEYS[3]
local outboxPendingKey = KEYS[4]
local outboxEventKey = KEYS[5] .. ARGV[2]
local expireTime = tonumber(ARGV[1])
if redis.call('exists', userOrderKey) == 1 then
    return 2
end
if redis.call('exists', stockKey) == 1 then
    local stock = tonumber(redis.call('get', stockKey))
    if stock > 0 then
        redis.call('decr', stockKey)
        redis.call('set', userOrderKey, 1, 'EX', expireTime)
        redis.call('hset', outboxEventKey,
                'eventId', ARGV[2],
                'userId', ARGV[3],
                'goodsId', ARGV[4],
                'goodsName', ARGV[5],
                'seckillPrice', ARGV[6],
                'retryCount', '0')
        redis.call('zadd', outboxPendingKey, ARGV[7], ARGV[2])
        return 1
    else
        redis.call('set', stockEmptyKey, "1")
        return 0
    end
end
return 0
