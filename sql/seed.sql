SET NAMES utf8mb4;
SET time_zone = '+08:00';

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE t_payment_record;
TRUNCATE TABLE t_seckill_order;
TRUNCATE TABLE t_order;
TRUNCATE TABLE t_delivery_address;
TRUNCATE TABLE t_seckill_goods;
TRUNCATE TABLE t_goods;
TRUNCATE TABLE t_user;
TRUNCATE TABLE t_admin;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO t_admin (username, password) VALUES
('admin', '$2b$12$rlCi/Qx.inD2wOhOCpbGweLZ/y7DvYx0BT8F2mz/SOUSrTdkrtb.i');

INSERT INTO t_user (id, nickname, password, salt, head, register_date, last_login_date) VALUES
(13800138001, '测试用户甲', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13800138001', DATE_SUB(NOW(3), INTERVAL 30 DAY), DATE_SUB(NOW(3), INTERVAL 2 HOUR)),
(13800138002, '测试用户乙', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13800138002', DATE_SUB(NOW(3), INTERVAL 20 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(13900139001, '测试用户丙', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=13900139001', DATE_SUB(NOW(3), INTERVAL 10 DAY), DATE_SUB(NOW(3), INTERVAL 30 MINUTE)),
(15800158001, '测试用户丁', '$2b$12$i3ogNIVm3/uno9j1SmAe3O3DyEQn6dQvsCtvAlog7ANpeTuxYiOta', NULL,
 'https://api.dicebear.com/7.x/avataaars/svg?seed=15800158001', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 5 HOUR));

INSERT INTO t_goods (goods_name, goods_title, goods_img, goods_detail, goods_price, goods_stock) VALUES
('复古胶片相机', '已结束场次 · 胶片相机',
 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=800&q=80',
 '经典胶片机身，适合复古摄影体验。本场秒杀已结束。', 1299.00, 19),
('机械键盘套装', '已结束场次 · 机械键盘',
 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=80',
 '热插拔轴体与铝制上盖。本场秒杀已结束且已售罄。', 599.00, 39),
('无线降噪耳机', '进行中 · 旗舰降噪耳机',
 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80',
 '主动降噪、长续航。当前正在抢购。', 1999.00, 80),
('限定联名卫衣', '进行中 · 限定联名',
 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=800&q=80',
 '库存较少的进行中场次。', 399.00, 25),
('旗舰智能手机', '即将开始 · 旗舰手机',
 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80',
 '两天后开抢。', 4999.00, 50),
('运动智能手表', '即将开始 · 智能手表',
 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80',
 '一周后开抢。', 1299.00, 120);

INSERT INTO t_seckill_goods (goods_id, seckill_price, stock_count, start_date, end_date) VALUES
(1, 799.00, 8, DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(2, 299.00, 0, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 12 HOUR)),
(3, 999.00, 49, DATE_SUB(NOW(3), INTERVAL 1 HOUR), DATE_ADD(NOW(3), INTERVAL 7 DAY)),
(4, 199.00, 12, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(NOW(3), INTERVAL 3 DAY)),
(5, 2999.00, 30, DATE_ADD(NOW(3), INTERVAL 2 DAY), DATE_ADD(NOW(3), INTERVAL 9 DAY)),
(6, 699.00, 80, DATE_ADD(NOW(3), INTERVAL 7 DAY), DATE_ADD(NOW(3), INTERVAL 14 DAY));

INSERT INTO t_delivery_address (id, user_id, receiver_name, receiver_phone, detail, is_default) VALUES
(1, 13800138001, '测试用户甲', '13800138001', '杭州市西湖区龙井路 1 号', 1),
(2, 13800138002, '测试用户乙', '13800138002', '南京市玄武区中山路 1 号', 1),
(3, 13900139001, '测试用户丙', '13900139001', '上海市浦东新区世纪大道 1 号', 1),
(4, 15800158001, '测试用户丁', '15800158001', '成都市武侯区天府大道 1 号', 1);

INSERT INTO t_order (id, user_id, goods_id, delivery_addr_id, receiver_name, receiver_phone, receiver_detail, goods_name, goods_count, goods_price, order_channel, status, create_date, pay_date) VALUES
(10001, 13800138001, 1, 1, '测试用户甲', '13800138001', '杭州市西湖区龙井路 1 号', '复古胶片相机', 1, 799.00, 1, 1, DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
(10002, 13800138002, 2, 2, '测试用户乙', '13800138002', '南京市玄武区中山路 1 号', '机械键盘套装', 1, 299.00, 1, 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
(10003, 13900139001, 3, 3, '测试用户丙', '13900139001', '上海市浦东新区世纪大道 1 号', '无线降噪耳机', 1, 999.00, 1, 0, DATE_SUB(NOW(3), INTERVAL 5 MINUTE), NULL),
(10004, 15800158001, 4, 4, '测试用户丁', '15800158001', '成都市武侯区天府大道 1 号', '限定联名卫衣', 1, 199.00, 1, -2, DATE_SUB(NOW(3), INTERVAL 1 DAY), NULL),
(10005, 13800138001, 3, 1, '测试用户甲', '13800138001', '杭州市西湖区龙井路 1 号', '无线降噪耳机', 1, 999.00, 1, -1, DATE_SUB(NOW(3), INTERVAL 6 HOUR), NULL);

INSERT INTO t_seckill_order (id, user_id, order_id, goods_id) VALUES
(20001, 13800138001, 10001, 1),
(20002, 13800138002, 10002, 2),
(20003, 13900139001, 10003, 3);

INSERT INTO t_payment_record (id, order_id, trade_no, amount, app_id, seller_id, status, create_date, update_date) VALUES
(30001, 10001, '202610031200000001', 799.00, '2021000000000000', NULL, 'PAID', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
(30002, 10002, '202610041200000001', 299.00, '2021000000000000', NULL, 'PAID', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
(30003, 10004, '202610051200000001', 199.00, '2021000000000000', NULL, 'REFUND_PENDING', DATE_SUB(NOW(3), INTERVAL 20 HOUR), DATE_SUB(NOW(3), INTERVAL 20 HOUR));
