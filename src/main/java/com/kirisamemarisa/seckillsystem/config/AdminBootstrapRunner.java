package com.kirisamemarisa.seckillsystem.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kirisamemarisa.seckillsystem.entity.Admin;
import com.kirisamemarisa.seckillsystem.mapper.AdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 一次性引导管理员创建，{@code @Order(10)}：表约束校验通过后再写账号。
 *
 * <p>仅当 {@code app.bootstrap-admin.username/password} 均配置且库中尚无该用户名时插入；
 * 密码最短 12 位并以 BCrypt 落库。生产环境创建成功后应立刻撤掉环境变量，避免每次启动都带着明文密码。
 * 依赖中间件：MySQL。
 */
@Slf4j
@Component
@Order(10)
public class AdminBootstrapRunner implements ApplicationRunner {
    @Autowired private AdminMapper adminMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    /**
     * 按需插入引导管理员；未配置账号密码则直接跳过。
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD 至少需要 12 个字符");
        }
        Long count = adminMapper.selectCount(new QueryWrapper<Admin>().eq("username", username));
        if (count == 0) {
            Admin admin = new Admin();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            adminMapper.insert(admin);
            log.warn("已创建一次性引导管理员账号；请立即移除 BOOTSTRAP_ADMIN_PASSWORD 环境变量");
        }
    }
}
