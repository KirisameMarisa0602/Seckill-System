package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 把 Controller 方法中的 {@link User} 参数解析为当前请求用户。
 *
 * <p>不在此处查 Redis：{@link AccessLimitInterceptor} 已按请求头 {@code token} 取用户并写入 {@link UserContext}。
 * 由 {@link WebConfig#addArgumentResolvers} 注册。无 {@code @Order}。
 */
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 仅处理参数类型为 {@link User} 的方法参数。
     *
     * @param parameter 当前参数元数据
     * @return 是 {@link User} 时才由本解析器接管
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == User.class;
    }
    /**
     * 返回拦截器预先放入上下文的用户，未登录则为 {@code null}。
     *
     * @return {@link UserContext#getUser()}
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return UserContext.getUser();
    }
}
