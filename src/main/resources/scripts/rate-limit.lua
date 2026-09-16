-- 固定窗口计数限流。第一次 incr 时才设置过期，避免窗口被无限续期。
--
-- KEYS[1] 限流计数 key
-- ARGV[1] 窗口内最大次数
-- ARGV[2] 窗口秒数
--
-- 返回：1 放行；0 超限。

local key = KEYS[1]
local maxCount = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])
local current = tonumber(redis.call('get', key) or "0")
if current >= maxCount then
    return 0
else
    redis.call('incr', key)
    if current == 0 then
        redis.call('expire', key, expireSeconds)
    end
    return 1
end
