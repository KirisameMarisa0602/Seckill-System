package com.kirisamemarisa.seckillsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaSafetyVerifier implements ApplicationRunner {
    @Autowired private JdbcTemplate jdbcTemplate;

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
