package com.kirisamemarisa.seckillsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动期表约束自检，{@code @Order(0)}，早于引导管理员与缓存预热。
 *
 * <p>秒杀「一人一单」和支付「按交易号入账」都依赖唯一索引；缺索引时宁可拒绝启动，
 * 也不要在高并发下写出重复订单/重复入账。依赖中间件：MySQL（{@code information_schema}）。
 */
@Component
@Order(0)
public class SchemaSafetyVerifier implements ApplicationRunner {
    @Autowired private JdbcTemplate jdbcTemplate;

    /**
     * 校验秒杀订单联合唯一索引与支付流水交易号唯一索引，缺失则抛出 {@link IllegalStateException} 阻断启动。
     */
    @Override
    public void run(ApplicationArguments args) {
        requireUniqueColumns("t_seckill_order", "user_id,goods_id",
                "缺少 t_seckill_order(user_id, goods_id) 唯一索引，禁止启动以避免重复下单");
        requireUniqueColumns("t_payment_record", "trade_no",
                "缺少 t_payment_record(trade_no) 唯一索引，禁止启动以避免重复入账");
    }

    private void requireUniqueColumns(String tableName, String columns, String errorMessage) {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                      AND non_unique = 0
                    GROUP BY index_name
                    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') = ?
                ) required_index
                """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tableName, columns);
        if (count == null || count == 0) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
