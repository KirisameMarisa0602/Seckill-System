-- Development-only destructive reset.
-- Accounts:
--   admin / Admin@123456
--   13800000001 / Test@123456
--   13800000002 / Test@123456
--   13800000003 / Test@123456

USE seckill;
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

INSERT INTO t_admin (id, username, password)
VALUES
    (1, 'admin', '$2a$12$Uu9UgA7TfdVpBUiH7Q3LwOBNy.Jf5HJ/tdW.0ITeiwNzZPCkulKum');

INSERT INTO t_user
    (id, nickname, password, salt, head, register_date, last_login_date)
VALUES
    (
        13800000001,
        '测试用户一',
        '$2a$12$W0lfl9/7eDhJWStHhHRiDegLTYvNa.QFXHq5YflvFYCBuarW7lwrm',
        NULL,
        'https://api.dicebear.com/7.x/avataaars/svg?seed=user-1',
        NOW(),
        NULL
    ),
    (
        13800000002,
        '测试用户二',
        '$2a$12$Pledbt.qewbjh5/Zei9ncOdUg/sj2.8/ICrw7/2EQ2UviLwxx6/aC',
        NULL,
        'https://api.dicebear.com/7.x/avataaars/svg?seed=user-2',
        NOW(),
        NULL
    ),
    (
        13800000003,
        '测试用户三',
        '$2a$12$26XLiDlAfpWkwN1tUaQw4.7AZRVI/X1CXMviu4Ri5q1XDnC4jFBxu',
        NULL,
        'https://api.dicebear.com/7.x/avataaars/svg?seed=user-3',
        NOW(),
        NULL
    );

INSERT INTO t_goods
    (id, goods_name, goods_title, goods_img, goods_detail, goods_price, goods_stock)
VALUES
    (
        1,
        'RTX 5090 显卡',
        '已结束场次 · 高性能旗舰显卡',
        'https://images.unsplash.com/photo-1591488320449-011701bb6704?auto=format&fit=crop&w=1200&q=80',
        '用于验证已经结束的秒杀活动状态。',
        16999.00,
        50
    ),
    (
        2,
        'iPhone 17 Pro',
        '正在进行 · 旗舰智能手机',
        'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=1200&q=80',
        '用于验证正常秒杀、排队、下单和支付流程。',
        8999.00,
        100
    ),
    (
        3,
        '拯救者 Y9000P',
        '正在进行 · 高性能游戏本',
        'https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=1200&q=80',
        '第二个进行中的活动，用于验证多商品并发与库存隔离。',
        10999.00,
        80
    ),
    (
        4,
        '机械键盘 Pro',
        '即将开始 · 三模客制化键盘',
        'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1200&q=80',
        '用于验证未开始活动、倒计时状态和接口拦截。',
        699.00,
        120
    );

INSERT INTO t_seckill_goods
    (id, goods_id, seckill_price, stock_count, start_date, end_date)
VALUES
    (1, 1, 9999.00, 8, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 2, 1999.00, 20, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR)),
    (3, 3, 3999.00, 12, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR)),
    (4, 4, 299.00, 30, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY));
