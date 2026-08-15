-- T2__asset_schema.sql
-- STB asset inventory, history, sales and sale items for a tenant schema.
-- No tenant_id column — the schema itself is the tenant boundary.

CREATE TABLE stb_assets (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    serial_number    VARCHAR(100)    NOT NULL,
    box_number       VARCHAR(100),
    model            VARCHAR(100),
    manufacturer     VARCHAR(100),
    batch            VARCHAR(100),
    purchase_date    DATE,
    purchase_cost    DECIMAL(15,2),
    status           VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    retailer_id      BIGINT,
    tagging_date     DATE,
    sale_date        DATE,
    activation_date  DATE,
    return_date      DATE,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stb_assets PRIMARY KEY (id),
    CONSTRAINT uq_asset_serial UNIQUE (serial_number),
    CONSTRAINT fk_assets_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id)
);

CREATE INDEX idx_assets_status   ON stb_assets (status);
CREATE INDEX idx_assets_retailer ON stb_assets (retailer_id);
CREATE INDEX idx_assets_serial   ON stb_assets (serial_number);

CREATE TABLE stb_asset_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    asset_id    BIGINT       NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    retailer_id BIGINT,
    changed_by  VARCHAR(100),
    remarks     VARCHAR(500),
    changed_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_stb_asset_history PRIMARY KEY (id),
    CONSTRAINT fk_history_asset FOREIGN KEY (asset_id) REFERENCES stb_assets (id)
);

CREATE INDEX idx_asset_history_asset ON stb_asset_history (asset_id);
CREATE INDEX idx_asset_history_date  ON stb_asset_history (changed_at);

CREATE TABLE stb_sales (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    retailer_id      BIGINT          NOT NULL,
    transaction_date DATE            NOT NULL,
    total_amount     DECIMAL(15,2)   NOT NULL,
    payment_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reference        VARCHAR(100),
    remarks          VARCHAR(500),
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    CONSTRAINT pk_stb_sales PRIMARY KEY (id),
    CONSTRAINT fk_sales_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id)
);

CREATE INDEX idx_sales_retailer ON stb_sales (retailer_id);
CREATE INDEX idx_sales_date     ON stb_sales (transaction_date);

CREATE TABLE stb_sale_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id    BIGINT        NOT NULL,
    asset_id   BIGINT        NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    CONSTRAINT pk_stb_sale_items PRIMARY KEY (id),
    CONSTRAINT uq_sale_item_asset UNIQUE (sale_id, asset_id),
    CONSTRAINT fk_sale_items_sale  FOREIGN KEY (sale_id)  REFERENCES stb_sales  (id),
    CONSTRAINT fk_sale_items_asset FOREIGN KEY (asset_id) REFERENCES stb_assets (id)
);

CREATE INDEX idx_sale_items_sale  ON stb_sale_items (sale_id);
CREATE INDEX idx_sale_items_asset ON stb_sale_items (asset_id);
