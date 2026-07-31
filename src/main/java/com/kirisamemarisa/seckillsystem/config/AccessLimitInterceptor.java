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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DefaultRedisScript<Long> rateLimitScript;

    //获取用户的方法
    private User getUser(HttpServletRequest request) {
        //去请求头里找user的登录token
        String token = request.getHeader("token");
        //请求头里没有的话去请求参数里找
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }
        //没有token就默认用户没登陆
        if (!StringUtils.hasText(token)) return null;
        //拿着token去redis里找
        return (User) redisTemplate.opsForValue().get(UserKey.token.getPrefix() + token);
    }

    @Override
    //这三个参数代表了一次 HTTP 请求的三个核心要素：
    //HttpServletRequest request：来的人是谁（请求的参数、路径、请求头）。
    //HttpServletResponse response：我们要怎么回话（负责给前端发数据、发报错）。
    //Object handler：他要去哪儿 / 他要干嘛（请求的最终目的地）。
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //可以把HandlerMethod 想象成一张“目标方法的详细档案卡”。这张卡片里记录了：
        //Method：将要执行的具体是哪个方法？
        //Bean：这个方法属于哪个 Controller 类？
        //Annotations：这个方法头上贴了什么注解？（比如有没有我们刚才说的 @AccessLimit 标签？）

        //如果是一个controller方法
        if (handler instanceof HandlerMethod) {
            //查是哪个用户
            User user = getUser(request);
            //服务用户的线程在自己的ThreadLocalMap里存下“ThreadLocal：User”，反向存储实现隔离
            UserContext.setUser(user);
            //强制类型转换
            HandlerMethod hm = (HandlerMethod) handler;

            //去看一眼目标Controller方法上，有没有贴@AccessLimit标签
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            //没有这个标签的直接放行，没必要走下面的步骤
            if (accessLimit == null) {
                return true;
            }
            //否则获取限流要求
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                //如果需要登录，但是你没有登陆的token，那不予放行
                if (user == null) {
                    render(response, RespBeanEnum.USER_NOT_EXIST);
                    //提前清理线程绑定的用户对象，防止false未能触发afterCompletion方法中的清理方法
                    UserContext.remove();
                    return false;
                }
                key += ":" + user.getId();
            }
            //生成redis中的键值对
            AccessKey accessKey = AccessKey.withExpire(second);
            String realKey = accessKey.getPrefix() + key;

            //执行Lua脚本把“查次数 -> 如果小于5 -> 存进Redis加1”合并为原子操作
            Long result = (Long) redisTemplate.execute(
                    rateLimitScript,                      // 跑哪个Lua脚本
                    Collections.singletonList(realKey),   // 告诉脚本该查哪个Key（刚才拼出来的）
                    maxCount,                             // 告诉脚本限制次数是多少
                    second                                // 告诉脚本过期时间是多少
            );

            //检查是否访问过于频繁
            if (result != null && result == 0L) {
                render(response, RespBeanEnum.ACCESS_LIMIT_REACHED);
                //提前清理线程绑定的用户对象，防止false未能触发afterCompletion方法中的清理方法
                UserContext.remove();
                return false;
            }
        }
        return true;
    }

    //只有当 preHandle 返回 true 时，才会在请求结束时回调 afterCompletion 方法（其中包含了remove 操作）
    //如果在中途因为没登录或者触发了限流，导致你直接 return false;，那么 afterCompletion 是绝对不会执行的。
    //最终导致这个带有用户信息的线程被扔回了 Tomcat 的公用线程池，一旦下个请求复用了这个线程，就会直接“盗用”上一个用户的身份（即严重越权）。

    //Tomcat里的线程是循环利用的
    //ThreadLocal需要及时清空（执行 remove()）
    //如果不清空，下一次一个没登录的请求恰好分到了这个线程，获取到上个客人的 User 对象，直接就“越权”登录了
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    //需要在Controller执行之前提前毙掉一个请求，且需要给前端返回友好的 JSON 报错提示，都要完成这部分代码
    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }
}