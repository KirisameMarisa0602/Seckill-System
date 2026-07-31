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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;
import java.io.PrintWriter;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Autowired private DefaultRedisScript<Long> rateLimitScript;

    private User getUser(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) { token = request.getParameter("token"); }
        if (!StringUtils.hasText(token)) return null;
        return (User) redisTemplate.opsForValue().get(UserKey.token.getPrefix() + token);
    }

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
                if (needLogin) {
                    if (user == null) {
                        render(response, RespBeanEnum.USER_NOT_EXIST);
                        UserContext.remove();
                        return false;
                    }
                    key += ":" + user.getId();
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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }
}