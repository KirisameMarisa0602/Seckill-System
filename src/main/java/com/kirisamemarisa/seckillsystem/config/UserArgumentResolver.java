package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 全局用户参数解析器
 * 拦截所有 Controller 方法，只要形参有 User，就会自动执行这里的方法去 redis 找身份
 */
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 1. 判断什么时候进这个解析器？（当Controller形参是 User 类的时候）
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == User.class;
    }

    // 2. 具体怎么解析？
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // 拦截器已经做过解析了，直接从当前线程拿即可
        return UserContext.getUser();
    }
}