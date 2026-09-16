package com.kirisamemarisa.seckillsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

/**
 * Spring MVC 装配：用户参数解析、限流/管理员拦截器、CORS。
 *
 * <p>生产经 Nginx 同源反代时 {@code app.security.allowed-origins} 应留空，不发 CORS 头，避免放开任意源。
 * 仅当前端独立域名直连后端时才配置白名单。无 {@code @Order}，随 Web 容器启动。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private UserArgumentResolver userArgumentResolver;

    @Autowired
    private AccessLimitInterceptor accessLimitInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Value("${app.security.allowed-origins:}")
    private String allowedOrigins;

    /**
     * 注册用户参数解析器，使 Controller 方法可直接声明 {@link com.kirisamemarisa.seckillsystem.entity.User}。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }

    /**
     * 全路径挂限流拦截器；{@code /admin/**} 再挂管理员鉴权，登录口排除以免无法拿 Token。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLimitInterceptor);
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

    /**
     * 按配置白名单跨域。未配置则不注册 CORS，配合 Nginx 同源；显式列出允许的方法与鉴权头，避免 {@code *} 带凭证。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "token", "Admin-Token")
                .maxAge(3600);
    }
}
