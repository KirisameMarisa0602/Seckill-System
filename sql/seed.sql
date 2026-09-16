-- 本地/演示种子数据。会清空业务表，但保留 flyway_schema_history。
--
-- 账号：
--   管理员  admin / Admin@123456
--   测试用户（id 即手机号，密码均为 User@123456）
--     13800138001 测试用户甲
--     13800138002 测试用户乙
--     13900139001 测试用户丙
--     15800158001 测试用户丁
--
-- 三种秒杀窗口（相对执行时刻）：
--   已结束：商品 1 胶片相机、商品 2 机械键盘（库存为 0，可验证售罄展示）
--   进行中：商品 3 降噪耳机、商品 4 联名卫衣（可走完整下单与支付）
--   即将开始：商品 5 旗舰手机（约 2 天后）、商品 6 智能手表（约 7 天后）

SET NAMES utf8mb4;
SET time_zone = '+08:00';

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE t_payment_record;
TRUNCATE TABLE t_seckill_order;
TRUNCATE TABLE t_order;
TRUNCATE TABLE t_seckill_goods;
TRUNCATE TABLE t_goods;
TRUNCATE TABLE t_user;
TRUNCATE TABLE t_admin;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO t_admin (username, password) VALUES
('admin', '$2b$12$rlCi/Qx.inD2wOhOCpbGweLZ/y7DvYx0BT8F2mz/SOUSrTdkrtb.i');

INSERT INTO t_user (id, nickname, password, salt, head, register_date, last_login_date) VALUES
(13800138001, '测试用户甲', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13800138001', NOW(3), NULL),
(13800138002, '测试用户乙', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13800138002', NOW(3), NULL),
(13900139001, '测试用户丙', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13900139001', NOW(3), NULL),
(15800158001, '测试用户丁', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=15800158001', NOW(3), NULL);

INSERT INTO t_goods (goods_name, goods_title, goods_img, goods_detail, goods_price, goods_stock) VALUES
('复古胶片相机', '已结束场次 · 胶片相机',
 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=800&q=80',
 '经典胶片机身，适合复古摄影体验。本场秒杀已结束，用于验证过期场次展示。', 1299.00, 20),
('机械键盘套装', '已结束场次 · 机械键盘',
 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=80',
 '热插拔轴体与铝制上盖，上一场秒杀已结束。', 599.00, 40),
('无线降噪耳机', '进行中 · 旗舰降噪耳机',
 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80',
 '主动降噪、长续航。当前正在抢购，可用于完整秒杀与支付联调。', 1999.00, 80),
('限定联名卫衣', '进行中 · 限定联名',
 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=800&q=80',
 '库存较少的进行中场次，适合验证一人一单与售罄路径。', 399.00, 25),
('旗舰智能手机', '即将开始 · 旗舰手机',
 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80',
 '两天后开抢，用于验证未开始场次与倒计时。', 4999.00, 50),
('运动智能手表', '即将开始 · 智能手表',
 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80',
 '一周后开抢，用于验证远期预告场次。', 1299.00, 120);

INSERT INTO t_seckill_goods (goods_id, seckill_price, stock_count, start_date, end_date) VALUES
(1, 799.00, 8,  DATE_SUB(NOW(3), INTERVAL 7 DAY),  DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(2, 299.00, 0,  DATE_SUB(NOW(3), INTERVAL 5 DAY),  DATE_SUB(NOW(3), INTERVAL 12 HOUR)),
(3, 999.00, 50, DATE_SUB(NOW(3), INTERVAL 1 HOUR), DATE_ADD(NOW(3), INTERVAL 7 DAY)),
(4, 199.00, 12, DATE_SUB(NOW(3), INTERVAL 2 DAY),  DATE_ADD(NOW(3), INTERVAL 3 DAY)),
(5, 2999.00, 30, DATE_ADD(NOW(3), INTERVAL 2 DAY), DATE_ADD(NOW(3), INTERVAL 9 DAY)),
(6, 699.00, 80, DATE_ADD(NOW(3), INTERVAL 7 DAY), DATE_ADD(NOW(3), INTERVAL 14 DAY));
