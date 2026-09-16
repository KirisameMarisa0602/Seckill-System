CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    salt VARCHAR(32) NULL,
    head VARCHAR(512) NULL,
    register_date DATETIME(3) NOT NULL,
    last_login_date DATETIME(3) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_admin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_goods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goods_name VARCHAR(255) NOT NULL,
    goods_title VARCHAR(255) NULL,
    goods_img VARCHAR(512) NULL,
    goods_detail TEXT NULL,
    goods_price DECIMAL(18,2) NOT NULL,
    goods_stock INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_goods_price CHECK (goods_price >= 0),
    CONSTRAINT chk_goods_stock CHECK (goods_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_seckill_goods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    seckill_price DECIMAL(18,2) NOT NULL,
    stock_count INT NOT NULL,
    start_date DATETIME(3) NOT NULL,
    end_date DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_goods_goods_id (goods_id),
    KEY idx_seckill_goods_time (start_date, end_date),
    CONSTRAINT chk_seckill_price CHECK (seckill_price >= 0),
    CONSTRAINT chk_seckill_stock CHECK (stock_count >= 0),
    CONSTRAINT chk_seckill_time CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    delivery_addr_id BIGINT NOT NULL DEFAULT 0,
    goods_name VARCHAR(255) NOT NULL,
    goods_count INT NOT NULL DEFAULT 1,
    goods_price DECIMAL(18,2) NOT NULL,
    order_channel INT NOT NULL,
    status INT NOT NULL DEFAULT 0 COMMENT '-2 refund pending, -1 canceled, 0 pending, 1 paid',
    create_date DATETIME(3) NOT NULL,
    pay_date DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_order_user_create (user_id, create_date),
    KEY idx_order_goods_status (goods_id, status),
    KEY idx_order_status_create (status, create_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_seckill_order (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_order_user_goods (user_id, goods_id),
    UNIQUE KEY uk_seckill_order_order_id (order_id),
    KEY idx_seckill_order_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_payment_record (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    trade_no VARCHAR(64) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    app_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    create_date DATETIME(3) NOT NULL,
    update_date DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_trade_no (trade_no),
    KEY idx_payment_order_id (order_id),
    KEY idx_payment_status_update (status, update_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
