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