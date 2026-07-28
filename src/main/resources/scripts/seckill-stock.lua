-- KEYS[1] 是商品秒杀库存的 Redis Key
local stockKey = KEYS[1]

-- 1. 判断该商品的库存 Key 是否已经预热到 Redis 中
if (redis.call('exists', stockKey) == 1) then
    -- 2. 获取当前库存值，并显式转换成数字类型
    local stock = tonumber(redis.call('get', stockKey))

    -- 3. 判断库存是否充足
    if (stock > 0) then
        -- 库存充足，执行原子扣减 (-1)
        redis.call('decr', stockKey)
        return 1 -- 代表抢购资格获取成功
    end
end

-- Key 不存在，或者库存不足，直接返回 0 (代表失败，拦截请求)
return 0