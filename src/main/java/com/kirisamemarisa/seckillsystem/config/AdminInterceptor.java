package com.kirisamemarisa.seckillsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirisamemarisa.seckillsystem.redis.AdminKey;
import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * 管理员接口鉴权拦截器，校验请求头 {@code Admin-Token} 是否仍存在于 Redis。
 *
 * <p>命中则滑动续期，与登录会话 TTL 对齐。由 {@link WebConfig} 挂到 {@code /admin/**}（排除登录）。
 * 依赖中间件：Redis。无 {@code @Order}。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    /**
     * 校验 Admin-Token：缺失或 Redis 中已失效则直接写 401 JSON 并中断链路。
     *
     * @return {@code true} 表示管理员会话有效，继续进入 Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminToken = request.getHeader("Admin-Token");
        if (!StringUtils.hasText(adminToken)) {
            returnError(response, "非法请求：未携带Admin-Token");
            return false;
        }
        String realKey = AdminKey.token.getPrefix() + adminToken;
        Object adminInfo = redisTemplate.opsForValue().get(realKey);
        if (adminInfo == null) {
            returnError(response, "管理员认证失败或Token已过期，请重新登录");
            return false;
        }
        redisTemplate.expire(realKey, AdminKey.token.expireSeconds(), TimeUnit.SECONDS);
        return true;
    }

    private void returnError(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(RespBeanEnum.SESSION_ERROR);
        respBean.setMessage(msg);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }
}
