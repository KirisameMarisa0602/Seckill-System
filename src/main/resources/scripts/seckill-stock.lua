local stockKey = KEYS[1]
local userOrderKey = KEYS[2]


if redis.call('exists', userOrderKey) == 1 then
    return 2
end

if redis.call('exists', stockKey) == 1 then
    local stock = tonumber(redis.call('get', stockKey))
    if stock > 0 then
        redis.call('decr', stockKey)
        redis.call('set', userOrderKey, 1)
        return 1
    end
end
return 0