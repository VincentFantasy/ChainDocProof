-- 链上资产存证服务 数据库初始化脚本（MySQL 8.x）
-- 先执行: CREATE DATABASE chaindocproof DEFAULT CHARACTER SET utf8mb4;

-- 管理员账户表
CREATE TABLE IF NOT EXISTS t_admin (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(64)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员账户表';

-- 用户表（以钱包地址为身份）
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    address         VARCHAR(64) NOT NULL COMMENT '钱包地址（小写）',
    credit_balance  INT         NOT NULL DEFAULT 0 COMMENT '剩余存证/校验次数',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次登录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_address (address)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 存证记录表（文件不落盘，只存 hash）
CREATE TABLE IF NOT EXISTS t_proof (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    file_name  VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size  BIGINT      NOT NULL COMMENT '文件大小（字节）',
    file_hash  CHAR(64)    NOT NULL COMMENT '文件 SHA-256（小写hex）',
    upload_ts  BIGINT      NOT NULL COMMENT '上传时间戳（秒），上链使用',
    status     VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待上链 STORED-已上链',
    tx_hash    VARCHAR(66) NULL COMMENT '上链交易hash',
    stored_at  DATETIME    NULL COMMENT '上链时间',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_proof_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '存证记录表';

-- 次数流水表（购买/存证/校验）
CREATE TABLE IF NOT EXISTS t_credit_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    type       VARCHAR(16) NOT NULL COMMENT '类型: PURCHASE-购买 STORE-存证 VERIFY-校验',
    amount     INT         NOT NULL COMMENT '变动次数（正=增加 负=扣除）',
    proof_id   BIGINT      NULL COMMENT '关联存证记录ID',
    tx_hash    VARCHAR(66) NULL COMMENT '关联链上交易hash（购买时）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_credit_log_user (user_id),
    UNIQUE KEY uk_credit_log_tx (tx_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '次数流水表';

-- DocToken 发放记录表（管理端发放）
CREATE TABLE IF NOT EXISTS t_token_mint (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    admin_id   BIGINT      NOT NULL COMMENT '操作管理员ID',
    to_address VARCHAR(64) NOT NULL COMMENT '发放目标钱包地址',
    amount     VARCHAR(64) NOT NULL COMMENT '发放数量（wei，18位小数）',
    tx_hash    VARCHAR(66) NOT NULL COMMENT '链上交易hash',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'DocToken发放记录表';
