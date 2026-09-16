package com.kirisamemarisa.seckillsystem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个 HTTP 响应补安全头，降低 MIME 嗅探、点击劫持与多余浏览器能力暴露。
 *
 * <p>HSTS 仅在 {@code request.isSecure()} 时下发，避免 HTTP 开发环境被浏览器强制升级。
 * 经 Nginx 反代时需正确传递 {@code X-Forwarded-Proto}，否则生产 HTTPS 也加不上 HSTS。
 * 无 {@code @Order}，由 Spring Boot 登记为 Servlet Filter。
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    /**
     * 写入安全响应头后继续过滤链。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }
}
