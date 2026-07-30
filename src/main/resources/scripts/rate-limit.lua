-- 接收键和参数
local key = KEYS[1]
local maxCount = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])

-- 获取当前访问次数，没有则默认 0
local current = tonumber(redis.call('get', key) or "0")

if current >= maxCount then
    return 0 -- 超出限流阈值，返回 0 拒绝
else
    redis.call('incr', key)
    -- 如果是第一次访问，设置过期时间
    if current == 0 then
        redis.call('expire', key, expireSeconds)
    end
    return 1 -- 未限流，返回 1 放行
end