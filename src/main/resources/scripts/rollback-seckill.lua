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
