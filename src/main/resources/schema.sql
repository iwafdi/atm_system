CREATE TABLE IF NOT EXISTS customers (
    customer_id   VARCHAR PRIMARY KEY,
    name          VARCHAR NOT NULL,
    address       VARCHAR,
    phone_number  VARCHAR
);

CREATE TABLE IF NOT EXISTS atms (
    atm_id            VARCHAR PRIMARY KEY,
    location          VARCHAR,
    cash_inventory    NUMERIC(15,2) NOT NULL,
    ink_level         INT NOT NULL,
    paper_level       INT NOT NULL,
    firmware_version  VARCHAR,
    software_version  VARCHAR,
    hardware_status   VARCHAR
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number  VARCHAR PRIMARY KEY,
    account_type    VARCHAR NOT NULL,
    balance         NUMERIC(15,2) NOT NULL,
    customer_id     VARCHAR REFERENCES customers(customer_id)
);

CREATE TABLE IF NOT EXISTS cards (
    card_number   VARCHAR PRIMARY KEY,
    expiry_date   VARCHAR,
    card_type     VARCHAR,
    bank_id       VARCHAR,
    pin_hash      VARCHAR NOT NULL,
    pin_salt      VARCHAR NOT NULL,
    customer_id   VARCHAR REFERENCES customers(customer_id)
);

CREATE TABLE IF NOT EXISTS technicians (
    technician_id    VARCHAR PRIMARY KEY,
    name             VARCHAR,
    clearance_level  VARCHAR,
    pin_hash         VARCHAR NOT NULL,
    pin_salt         VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS maintenance_logs (
    log_id         VARCHAR PRIMARY KEY,
    timestamp      TIMESTAMP NOT NULL,
    action_type    VARCHAR,
    description    VARCHAR,
    technician_id  VARCHAR REFERENCES technicians(technician_id),
    atm_id         VARCHAR REFERENCES atms(atm_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   SERIAL PRIMARY KEY,
    timestamp        TIMESTAMP NOT NULL,
    type             VARCHAR NOT NULL,
    account_number   VARCHAR REFERENCES accounts(account_number),
    amount           NUMERIC(15,2) NOT NULL,
    balance_after    NUMERIC(15,2) NOT NULL,
    related_account  VARCHAR,
    atm_id           VARCHAR
);
