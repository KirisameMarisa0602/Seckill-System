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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            // 1. 获取登录用户并存入 ThreadLocal
            User user = getUser(request);
            UserContext.setUser(user);

            HandlerMethod hm = (HandlerMethod) handler;
            // 2. 尝试获取该方法上的 @AccessLimit 注解
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if (accessLimit == null) {
                return true; // 没加注解的方法直接放行
            }

            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();

            // 3. 校验登录状态
            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    render(response, RespBeanEnum.USER_NOT_EXIST);
                    return false;
                }
                key += ":" + user.getId();
            }

            // 4. Redis 限流核心逻辑 (固定窗口计数器算法)
            AccessKey accessKey = AccessKey.withExpire(second);
            String realKey = accessKey.getPrefix() + key;

            Integer count = (Integer) redisTemplate.opsForValue().get(realKey);
            if (count == null) {
                redisTemplate.opsForValue().set(realKey, 1, accessKey.expireSeconds(), TimeUnit.SECONDS);
            } else if (count < maxCount) {
                redisTemplate.opsForValue().increment(realKey);
            }else {
                // 如果超标了，拦截，直接往前端写出错误 JSON
                render(response, RespBeanEnum.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }

    // 线程结束后清除，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    // ========== 私有辅助方法 ==========

    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }

    private User getUser(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }
        if (!StringUtils.hasText(token)) return null;
        return (User) redisTemplate.opsForValue().get(UserKey.token.getPrefix() + token);
    }
}