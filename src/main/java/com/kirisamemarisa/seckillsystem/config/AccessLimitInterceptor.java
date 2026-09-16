package com.kirisamemarisa.seckillsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirisamemarisa.seckillsystem.config.annotation.AccessLimit;
import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.redis.AccessKey;
import com.kirisamemarisa.seckillsystem.redis.UserKey;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;
import java.util.Arrays;
import java.io.PrintWriter;

/**
 * 全站拦截器：解析登录用户、IP 黑名单、{@link AccessLimit} 限流。
 *
 * <p>必须在 Controller 参数解析之前写入 {@link UserContext}，并在请求结束时清理。
 * 限流走 Redis Lua，避免并发下窗口计数不准确。
 * 依赖中间件：Redis。由 {@link WebConfig} 注册，无 {@code @Order}。
 */
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private DefaultRedisScript<Long> rateLimitScript;

    @Value("${app.security.trusted-proxies:127.0.0.1,::1}")
    private String trustedProxies;

    private User getUser(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) return null;
        return (User) redisTemplate.opsForValue().get(UserKey.token.getPrefix() + token);
    }

    /**
     * 解析用户、检查黑名单、按注解执行 Redis 限流；未标注 {@link AccessLimit} 的接口仍会写入用户上下文。
     *
     * @return {@code false} 时已写入错误 JSON（401/429），不再进入 Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            if (handler instanceof HandlerMethod) {
                User user = getUser(request);
                UserContext.setUser(user);
                HandlerMethod hm = (HandlerMethod) handler;
                AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
                if (accessLimit == null) { return true; }
                int second = accessLimit.second();
                int maxCount = accessLimit.maxCount();
                boolean needLogin = accessLimit.needLogin();
                String key = request.getRequestURI();
                String ip = request.getRemoteAddr();
                String xff = request.getHeader("X-Forwarded-For");
                // 只在直连对端是受信反代时才采信 XFF，防止客户端伪造头绕过按 IP 限流。
                // 默认受信 127.0.0.1/::1，对应本机 Nginx；多级代理取 XFF 最左侧（原始客户端）。
                if (isTrustedProxy(ip) && StringUtils.hasText(xff)
                        && !"unknown".equalsIgnoreCase(xff)) {
                    ip = xff.split(",")[0].trim();
                }
                String blackKey = AccessKey.blacklist.getPrefix() + ip;
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blackKey))) {
                    render(response, RespBeanEnum.REQUEST_ILLEGAL);
                    UserContext.remove();
                    return false;
                }
                if (needLogin) {
                    if (user == null) {
                        render(response, RespBeanEnum.USER_NOT_EXIST);
                        UserContext.remove();
                        return false;
                    }
                    key += ":" + user.getId();
                } else {
                    key += ":" + ip;
                }
                AccessKey accessKey = AccessKey.withExpire(second);
                String realKey = accessKey.getPrefix() + key;
                Long result = stringRedisTemplate.execute(
                        rateLimitScript,
                        Collections.singletonList(realKey),
                        String.valueOf(maxCount),
                        String.valueOf(second)
                );
                if (result != null && result == 0L) {
                    render(response, RespBeanEnum.ACCESS_LIMIT_REACHED);
                    UserContext.remove();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            UserContext.remove();
            throw e;
        }
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .anyMatch(remoteAddress::equals);
    }

    /**
     * 无论成功失败都清 ThreadLocal，避免工作线程串号。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws Exception {
        if (respBeanEnum == RespBeanEnum.ACCESS_LIMIT_REACHED) {
            response.setStatus(429);
        } else if (respBeanEnum == RespBeanEnum.USER_NOT_EXIST) {
            response.setStatus(401);
        }
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(RespBean.error(respBeanEnum)));
        out.flush();
        out.close();
    }
}
