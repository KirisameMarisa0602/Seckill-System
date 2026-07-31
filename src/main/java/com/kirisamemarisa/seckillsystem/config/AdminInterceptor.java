package com.kirisamemarisa.seckillsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminToken = request.getHeader("Admin-Token");
        if (!StringUtils.hasText(adminToken)) {
            returnError(response, "非法请求：未携带Admin-Token");
            return false;
        }
        Object adminInfo = redisTemplate.opsForValue().get("admin:ticket:" + adminToken);
        if (adminInfo == null) {
            returnError(response, "管理员认证失败或Token已过期，请重新登录");
            return false;
        }
        redisTemplate.expire("admin:ticket:" + adminToken, 30, TimeUnit.MINUTES);
        return true;
    }

    private void returnError(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(RespBeanEnum.SESSION_ERROR);
        respBean.setMessage(msg);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }
}