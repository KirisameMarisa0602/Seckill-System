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
