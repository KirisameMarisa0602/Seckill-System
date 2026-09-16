package com.kirisamemarisa.seckillsystem.config;

import com.kirisamemarisa.seckillsystem.entity.User;

/**
 * 当前请求登录用户的线程上下文。
 *
 * <p>由 {@link AccessLimitInterceptor} 在 {@code preHandle} 写入、{@code afterCompletion} 清理，
 * {@link UserArgumentResolver} 再把 {@link User} 注入 Controller 参数。
 * 必须成对 {@link #remove()}，否则线程池复用会串号。无中间件依赖、无 {@code @Order}。
 */
public class UserContext {
    private static ThreadLocal<User> userHolder = new ThreadLocal<>();
    /** 绑定本请求解析出的用户，可为 {@code null}（未登录）。 */
    public static void setUser(User user) { userHolder.set(user); }
    /** @return 当前线程绑定的用户；未设置或已清理时为 {@code null} */
    public static User getUser() { return userHolder.get(); }
    /** 清除 ThreadLocal，避免 Tomcat 工作线程复用时泄漏上一次请求的用户。 */
    public static void remove() { userHolder.remove(); }
}
