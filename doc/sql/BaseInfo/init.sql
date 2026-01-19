CREATE TABLE `t_stock_pool` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `trade_date` date NOT NULL COMMENT '交易日期',
                                `stock_code` varchar(20) NOT NULL COMMENT '股票代码',
                                `stock_name` varchar(50) NOT NULL COMMENT '股票名称',
                                `stock_type` int DEFAULT '0' COMMENT '股票类型',
                                `price` decimal(10,2) DEFAULT '0.00' COMMENT '最新价（元）',
                                `change_percent` decimal(5,2) DEFAULT '0.00' COMMENT '涨跌幅（%）',
                                `turnover_ratio` decimal(5,2) DEFAULT '0.00' COMMENT '换手率（%）',
                                `circulation_market_cap` decimal(12,2) DEFAULT '0.00' COMMENT '流通市值（亿元）',
                                `total_market_cap` decimal(12,2) DEFAULT '0.00' COMMENT '总市值（亿元）',
                                `issue_price` decimal(10,2) DEFAULT '0.00' COMMENT '发行价（元）',
                                `listed_date` datetime DEFAULT NULL COMMENT '上市日期',
                                `pool_type` varchar(20) NOT NULL COMMENT '股票池类型，zt=涨停池, dt=跌停池, yesterday_zt=昨日涨停, broken_zt=炸板池, super_stock=强势股池',
                                `buy_lock_ratio` decimal(8,4) DEFAULT '0.0000' COMMENT '买盘封单比（%）',
                                `sell_lock_ratio` decimal(8,4) DEFAULT '0.0000' COMMENT '卖盘封单比（%）',
                                `current_lock_amount` decimal(16,2) DEFAULT '0.00' COMMENT '当前封单金额（元）',
                                `max_lock_amount` decimal(16,2) DEFAULT '0.00' COMMENT '最高封单金额（元）',
                                `limit_days` int DEFAULT '0' COMMENT '连板数',
                                `break_limit_times` int DEFAULT '0' COMMENT '开板次数',
                                `first_limit_up_time` datetime DEFAULT NULL COMMENT '首次封板时间',
                                `last_limit_up_time` datetime DEFAULT NULL COMMENT '最后封板时间',
                                `first_limit_down_time` datetime DEFAULT NULL COMMENT '首次封跌停时间',
                                `last_limit_down_time` datetime DEFAULT NULL COMMENT '最后封跌停时间',
                                `yesterday_break_limit_up_times` int DEFAULT '0' COMMENT '昨日开板次数',
                                `yesterday_first_limit_up_time` datetime DEFAULT NULL COMMENT '昨日首次封板时间',
                                `yesterday_last_limit_up_time` datetime DEFAULT NULL COMMENT '昨日最后封板时间',
                                `last_break_limit_up_time` datetime DEFAULT NULL COMMENT '最后炸板时间',
                                `m_days_n_boards` varchar(20) DEFAULT '' COMMENT '几天几板',
                                `stock_reason` varchar(500) DEFAULT '' COMMENT '上涨/下跌原因',
                                `related_plates` text COMMENT '相关板块（JSON字符串）',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_stock_date_pool` (`stock_code`,`trade_date`,`pool_type`) COMMENT '股票代码+交易日期+股票池类型唯一',
                                KEY `idx_trade_date` (`trade_date`),
                                KEY `idx_pool_type` (`pool_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票池数据表';


CREATE TABLE `sys_holiday` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `holiday_date` date NOT NULL COMMENT '节假日日期',
                               `holiday_name` varchar(50) DEFAULT '' COMMENT '节假日名称（如：元旦、春节）',
                               `is_holiday` tinyint(1) DEFAULT 0 COMMENT '是否为法定节假日 0-否 1-是',
                               `is_makeup_work` tinyint(1) DEFAULT 0 COMMENT '是否为调休补班日 0-否 1-是',
                               `year` int(4) NOT NULL COMMENT '年份',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_holiday_date` (`holiday_date`),
                               KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法定节假日表';

-- 1. 新增month字段（先添加字段，再添加索引）
ALTER TABLE sys_holiday
    ADD COLUMN `month` INT NOT NULL COMMENT '月份（1-12）' AFTER `year`;

-- 2. 为month字段添加索引（提升按年+月查询的效率）
CREATE INDEX idx_year_month ON sys_holiday (`year`, `month`);

-- 交割单
CREATE TABLE IF NOT EXISTS t_delivery_order (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                                broker_code VARCHAR(20) NOT NULL COMMENT '券商编码（如GTJA-国泰君安，GF-广发）',
    account_no VARCHAR(50) NOT NULL COMMENT '证券账户号',
    security_code VARCHAR(20) NOT NULL COMMENT '证券代码',
    security_name VARCHAR(50) COMMENT '证券名称',
    trade_type VARCHAR(10) COMMENT '交易类型（买入/卖出）',
    trade_time DATETIME NOT NULL COMMENT '成交时间',
    trade_price DECIMAL(10,2) COMMENT '成交价格',
    trade_quantity INT COMMENT '成交数量',
    trade_amount DECIMAL(12,2) COMMENT '成交金额',
    commission_fee DECIMAL(10,2) COMMENT '佣金',
    stamp_tax DECIMAL(10,2) COMMENT '印花税',
    transfer_fee DECIMAL(10,2) COMMENT '过户费',
    total_fee DECIMAL(10,2) COMMENT '总费用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account (account_no),
    INDEX idx_trade_time (trade_time),
    INDEX idx_security_code (security_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证券交割单表';