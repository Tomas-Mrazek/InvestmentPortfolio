CREATE TABLE app_user (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 1),
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    disabled BOOLEAN NOT NULL,
    password_expired BOOLEAN NOT NULL,
    fio_ebroker_password VARCHAR(255),
    fio_ebroker_username VARCHAR(255),

    PRIMARY KEY (id),
    UNIQUE (email),
    UNIQUE (username)
);

CREATE TABLE app_user_roles (
    app_user_id BIGINT NOT NULL,
    roles VARCHAR(255) NOT NULL
);

CREATE TABLE asset (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 20),
    name VARCHAR(255) NOT NULL,
    ticker VARCHAR(255),
    isin VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    nominal_price DECIMAL(38,18),
    nominal_price_asset_id BIGINT,

    PRIMARY KEY (id),
    UNIQUE (ticker),
    UNIQUE (isin),
    UNIQUE (name, ticker, isin, type)
);

CREATE TABLE exchange (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 1),
    name VARCHAR(255) NOT NULL,
    abbreviation VARCHAR(255) NOT NULL,
    opening_time time,
    closing_time time,
    timezone VARCHAR(255),

    PRIMARY KEY (id),
    UNIQUE (name),
    UNIQUE (abbreviation)
);

CREATE TABLE transaction (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 10),
    timestamp TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    in_id BIGINT,
    out_id BIGINT,
    comment VARCHAR(255),
    imported BOOLEAN NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE transaction_movement (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 20),
    exchange_id BIGINT,
    location VARCHAR(255),
    asset_id BIGINT NOT NULL,
    amount DECIMAL(38,18) NOT NULL,
    fee_asset_id BIGINT,
    fee_amount DECIMAL(38,18),
    source_asset_id BIGINT,

    PRIMARY KEY (id)
);

CREATE TABLE ledger (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 20),
    timestamp TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    exchange_id BIGINT,
    location VARCHAR(255),
    asset_id BIGINT NOT NULL,
    amount DECIMAL(38,18) NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE price (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 100),
    date DATE NOT NULL,
    asset_id BIGINT NOT NULL,
    exchange_id BIGINT,
    price_asset_id BIGINT,
    opening_price DECIMAL(38,18),
    low_price DECIMAL(38,18),
    high_price DECIMAL(38,18),
    closing_price DECIMAL(38,18),
    price_change DECIMAL(38,18),
    volume DECIMAL(38,18),
    turnover DECIMAL(38,18),

    PRIMARY KEY (id),
    UNIQUE (date, asset_id, exchange_id, price_asset_id)
);

CREATE TABLE cookie_cache (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY (INCREMENT BY 1),
    created_at timestamp,
    domain VARCHAR(255),
    expires timestamp,
    httponly BOOLEAN,
    name VARCHAR(255),
    path VARCHAR(255),
    secure BOOLEAN,
    value VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE app_user_roles ADD FOREIGN KEY (app_user_id) REFERENCES app_user;

ALTER TABLE asset ADD FOREIGN KEY (nominal_price_asset_id) REFERENCES asset;

ALTER TABLE ledger ADD FOREIGN KEY (asset_id) REFERENCES asset;

ALTER TABLE ledger ADD FOREIGN KEY (exchange_id) REFERENCES exchange;

ALTER TABLE ledger ADD FOREIGN KEY (transaction_id) REFERENCES TRANSACTION;

ALTER TABLE ledger ADD FOREIGN KEY (user_id) REFERENCES app_user;

ALTER TABLE price ADD FOREIGN KEY (asset_id) REFERENCES asset;

ALTER TABLE price ADD FOREIGN KEY (exchange_id) REFERENCES exchange;

ALTER TABLE price ADD FOREIGN KEY (price_asset_id) REFERENCES asset;

ALTER TABLE transaction ADD FOREIGN KEY (in_id) REFERENCES transaction_movement;

ALTER TABLE transaction ADD FOREIGN KEY (out_id) REFERENCES transaction_movement;

ALTER TABLE transaction ADD FOREIGN KEY (user_id) REFERENCES app_user;

ALTER TABLE transaction_movement ADD FOREIGN KEY (asset_id) REFERENCES asset;

ALTER TABLE transaction_movement ADD FOREIGN KEY (exchange_id) REFERENCES exchange;

ALTER TABLE transaction_movement ADD FOREIGN KEY (fee_asset_id) REFERENCES asset;

ALTER TABLE transaction_movement ADD FOREIGN KEY (source_asset_id) REFERENCES asset;