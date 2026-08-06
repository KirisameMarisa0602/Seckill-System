package com.kirisamemarisa.seckillsystem;

import com.kirisamemarisa.seckillsystem.entity.User;
import com.kirisamemarisa.seckillsystem.redis.SeckillKey;
import com.kirisamemarisa.seckillsystem.redis.UserKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SeckillSystemApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void generateChaosJmeterData() throws Exception {
        File file = new File("D:\\user_data.csv"); // Mac/Linux改路径
        if (file.exists()) { file.delete(); }
        PrintWriter writer = new PrintWriter(file);
        Random random = new Random();

        int totalUsers = 10000; // 1万并发大军！
        System.out.println("====== [高强度模式] 开始初始化 10,000 条压测数据 ======");

        for (long i = 1; i <= totalUsers; i++) {
            long userId = 13000000000L + i;

            // 随机分配这个用户要去抢购哪件商品 (1 到 5)
            long targetGoodsId = random.nextInt(5) + 1;

            User user = new User();
            user.setId(userId);
            user.setNickname("chaos_user_" + i);
            user.setRegisterDate(new Date());

            String token = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(UserKey.token.getPrefix() + token, user, 4, TimeUnit.HOURS);

            String path = UUID.randomUUID().toString().replace("-", "");
            String pathKey = SeckillKey.getSeckillPath.getPrefix() + userId + ":" + targetGoodsId;
            redisTemplate.opsForValue().set(pathKey, path, 4, TimeUnit.HOURS);

            // CSV 多了一列商品ID： userId, token, targetGoodsId, path
            writer.println(userId + "," + token + "," + targetGoodsId + "," + path);
        }
        writer.close();
        System.out.println("====== [高强度模式] 备战完成！文件路径: D:\\user_data.csv ======");
    }
}