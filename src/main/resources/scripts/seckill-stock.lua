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