-- Ozon ERP 建表脚本（H2 MySQL 兼容模式 / MySQL 8 均可执行）

CREATE TABLE IF NOT EXISTS shop_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_name   VARCHAR(128) NOT NULL,
    client_id   VARCHAR(64),
    api_key     VARCHAR(256),
    enabled     TINYINT      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku           BIGINT,
    offer_id      VARCHAR(128),
    name          VARCHAR(512),
    primary_image VARCHAR(1024),
    sku_code      VARCHAR(128),
    type_id       BIGINT,
    category_id   BIGINT,
    brand         VARCHAR(256),
    status        VARCHAR(64),
    visibility    VARCHAR(64),
    has_stock     TINYINT DEFAULT 0,
    limit_cancel  TINYINT DEFAULT 0,
    raw_json      TEXT,
    synced_at     TIMESTAMP,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_offer_id ON product (offer_id);
CREATE INDEX IF NOT EXISTS idx_product_sku ON product (sku);

CREATE TABLE IF NOT EXISTS product_attribute (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT,
    sku           BIGINT,
    offer_id      VARCHAR(128),
    attribute_id  BIGINT,
    attr_name     VARCHAR(256),
    value_json    TEXT,
    complex_id    BIGINT,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_attr_product ON product_attribute (product_id);
CREATE INDEX IF NOT EXISTS idx_attr_sku ON product_attribute (sku);

CREATE TABLE IF NOT EXISTS price_stock (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku                BIGINT NOT NULL,
    offer_id           VARCHAR(128),
    price              VARCHAR(32),
    old_price          VARCHAR(32),
    min_price          VARCHAR(32),
    market_price       VARCHAR(32),
    currency_code      VARCHAR(8),
    price_index        VARCHAR(32),
    stock                INT DEFAULT 0,
    reserved             INT DEFAULT 0,
    warehouse_id         BIGINT,
    warehouse_name       VARCHAR(256),
    incoming             INT DEFAULT 0,
    stocks_updated_at    TIMESTAMP,
    prices_updated_at    TIMESTAMP,
    synced_at            TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ps_offer ON price_stock (offer_id);

CREATE TABLE IF NOT EXISTS return_item (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_id              BIGINT,
    posting_number         VARCHAR(64),
    sku                    BIGINT,
    offer_id               VARCHAR(128),
    product_name           VARCHAR(512),
    status                 VARCHAR(64),
    return_mode            VARCHAR(64),
    reason_type            VARCHAR(64),
    customer_name          VARCHAR(256),
    awaiting_deliv_deadline TIMESTAMP,
    decision_date          TIMESTAMP,
    return_date            TIMESTAMP,
    rebate_date            TIMESTAMP,
    raw_json               TEXT,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,
    synced_at              TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_return_posting ON return_item (posting_number);

CREATE TABLE IF NOT EXISTS posting (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    posting_number     VARCHAR(64) NOT NULL,
    order_id           BIGINT,
    scheme             VARCHAR(16),
    status             VARCHAR(64),
    substatus          VARCHAR(64),
    tracking_number    VARCHAR(128),
    delivery_method    VARCHAR(512),
    warehouse_id       BIGINT,
    shipment_date        TIMESTAMP,
    created_at           TIMESTAMP,
    shipment_plan_date   TIMESTAMP,
    deadline_date        TIMESTAMP,
    any_at_delivery      TIMESTAMP,
    cancel_reason        VARCHAR(512),
    canceler_id          VARCHAR(32),
    is_multi_package     TINYINT DEFAULT 0,
    products_json        TEXT,
    financial_data_json  TEXT,
    customer_json        TEXT,
    barcodes_json        TEXT,
    updated_on_api       TIMESTAMP,
    synced_at            TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_posting_status ON posting (status);
CREATE INDEX IF NOT EXISTS idx_posting_scheme ON posting (scheme);

CREATE TABLE IF NOT EXISTS chat_room (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id       VARCHAR(64) NOT NULL,
    chat_type     VARCHAR(32),
    chat_status   VARCHAR(32),
    unread        INT DEFAULT 0,
    last_message  TEXT,
    user_id       BIGINT,
    updated_ts    TIMESTAMP,
    created_at    TIMESTAMP,
    synced_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_room_id ON chat_room (chat_id);

CREATE TABLE IF NOT EXISTS chat_message (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id       VARCHAR(64) NOT NULL,
    message_id    VARCHAR(64),
    user_id       BIGINT,
    sender        VARCHAR(64),
    sender_asset  VARCHAR(64),
    type          VARCHAR(32),
    status        VARCHAR(32),
    content       TEXT,
    created_dt    TIMESTAMP,
    created_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_msg_chat ON chat_message (chat_id);
