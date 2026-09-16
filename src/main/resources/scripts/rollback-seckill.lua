-- 回滚一次已成功的预扣库存：还库存、删一人一单标记、清售罄标记。
-- 下单失败、超时关单等补偿路径调用。用户标记不存在则视为无需回滚。
--
-- KEYS[1] 可售库存
-- KEYS[2] 用户对该商品的下单标记
-- KEYS[3] 售罄标记
--
-- 返回：1 已回滚；0 没有可回滚的用户标记。

local stockKey = KEYS[1]
local userOrderKey = KEYS[2]
local stockEmptyKey = KEYS[3]

if redis.call('exists', userOrderKey) == 0 then
    return 0
end

redis.call('incr', stockKey)
redis.call('del', userOrderKey)
redis.call('del', stockEmptyKey)
return 1
