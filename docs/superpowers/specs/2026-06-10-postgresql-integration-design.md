# Design: PostgreSQL Integration for Ping-Source ATM System

**Date:** 2026-06-10
**Status:** Approved (design); pending implementation plan

## 1. Goal

Replace the current in-memory (`ArrayList`) storage of the Ping-Source ATM System with
durable persistence in PostgreSQL, so that customers, accounts, cards, technicians, ATM
hardware state, maintenance logs, and a **new transaction history** all survive restarts.

## 2. Decisions (locked)

| Decision | Choice |
|---|---|
| Build / dependency management | Introduce **Maven** (`pom.xml`); fetch JDBC driver automatically |
| Persistence scope | **Everything** + a new `transactions` table |
| DB credentials | **Config file** (`db.properties`) read at startup |
| Schema + sample data | **Auto-init on startup**: run `schema.sql`, seed sample data only if tables are empty |
| PIN storage | **Hashed** — salted SHA-256 (`pin_hash` + `pin_salt`) |

## 3. Guiding principle

The OOP teaching value is preserved: **models stay plain POJOs** with their existing
business logic (`Account.withdraw`, `Card.validate`, etc.). A separate **persistence
layer (DAOs)** is added beside them — a clean separation of concerns. `ATMSystem` keeps
orchestrating: it calls a model's business method, then asks a DAO to persist the result.
No SQL lives inside model classes.

## 4. Project layout (Maven standard)

Sources move from `src/` to the standard Maven layout. The stale precompiled `bin/`
directory is removed (Maven builds into `target/`).

```
src/main/java/
  ATMSystem.java            controller — calls models + DAOs
  models/
    Account.java            (unchanged)
    Customer.java           (unchanged)
    Card.java               (unchanged)
    Technician.java         (unchanged)
    ATM.java                (unchanged)
    MaintenanceLog.java     (unchanged)
    Transaction.java        NEW model
  db/
    Database.java           reads db.properties, owns the JDBC Connection
    SchemaInitializer.java  runs schema.sql; seeds sample data if empty
    CustomerDAO.java
    AccountDAO.java         includes atomic transfer
    CardDAO.java
    TechnicianDAO.java
    AtmDAO.java
    MaintenanceLogDAO.java
    TransactionDAO.java
  util/
    PinHasher.java          salted SHA-256
src/main/resources/
  db.properties             url, user, password
  schema.sql                CREATE TABLE IF NOT EXISTS ...
src/test/java/
  db/                       JUnit DAO integration tests
pom.xml                     Maven: postgresql driver + shade plugin (fat jar)
docs/superpowers/specs/     this spec
```

## 5. Database schema (7 tables)

```sql
-- customers
customer_id    VARCHAR PRIMARY KEY
name           VARCHAR NOT NULL
address        VARCHAR
phone_number   VARCHAR

-- accounts
account_number VARCHAR PRIMARY KEY
account_type   VARCHAR NOT NULL
balance        NUMERIC(15,2) NOT NULL
customer_id    VARCHAR REFERENCES customers(customer_id)

-- cards
card_number    VARCHAR PRIMARY KEY        -- 16 digits
expiry_date    VARCHAR                    -- "MM/yy" as today
card_type      VARCHAR
bank_id        VARCHAR
pin_hash       VARCHAR NOT NULL
pin_salt       VARCHAR NOT NULL
customer_id    VARCHAR REFERENCES customers(customer_id)

-- technicians
technician_id  VARCHAR PRIMARY KEY
name           VARCHAR
clearance_level VARCHAR
pin_hash       VARCHAR NOT NULL
pin_salt       VARCHAR NOT NULL

-- atms
atm_id            VARCHAR PRIMARY KEY
location          VARCHAR
cash_inventory    NUMERIC(15,2) NOT NULL
ink_level         INT NOT NULL
paper_level       INT NOT NULL
firmware_version  VARCHAR
software_version  VARCHAR
hardware_status   VARCHAR

-- maintenance_logs
log_id         VARCHAR PRIMARY KEY
timestamp      TIMESTAMP NOT NULL
action_type    VARCHAR
description    VARCHAR
technician_id  VARCHAR REFERENCES technicians(technician_id)
atm_id         VARCHAR REFERENCES atms(atm_id)

-- transactions (NEW)
transaction_id  SERIAL PRIMARY KEY
timestamp       TIMESTAMP NOT NULL
type            VARCHAR NOT NULL          -- WITHDRAWAL | DEPOSIT | TRANSFER
account_number  VARCHAR REFERENCES accounts(account_number)
amount          NUMERIC(15,2) NOT NULL
balance_after   NUMERIC(15,2) NOT NULL
related_account VARCHAR                   -- transfer destination, nullable
atm_id          VARCHAR REFERENCES atms(atm_id)
```

Money is `NUMERIC(15,2)` to avoid `double` rounding error (the existing code uses
`double`; the DB column is the source of truth). The `transactions` table is new — every
withdraw/deposit/transfer writes a row.

## 6. New `Transaction` model

A plain POJO mirroring the `transactions` table: `transactionId`, `timestamp`, `type`,
`accountNumber`, `amount`, `balanceAfter`, `relatedAccount`, `atmId`, with getters and a
`toString()` for receipt/history display. No persistence logic inside it.

## 7. Connection strategy

One shared `java.sql.Connection` for the whole session, owned by `Database`, opened at
startup from `db.properties` and closed at exit. Justification: single-user console app;
connection pooling would be over-engineering. DAOs receive the shared `Connection`.

`db.properties` keys:
```
db.url=jdbc:postgresql://localhost:5432/atm_system
db.user=...
db.password=...
```

## 8. PIN hashing (`PinHasher`)

- On seed/insert: generate a random per-PIN salt, compute `SHA-256(salt + pin)`, store
  both `pin_hash` (hex) and `pin_salt` (hex/base64).
- On auth: load `pin_hash` + `pin_salt`, recompute the hash of the entered PIN, compare.
- Model classes (`Card.validatePIN`, `Technician.authenticate`) keep their signatures; the
  hash comparison is performed where the DAO supplies the stored hash/salt (the controller
  or a small auth helper), so models stay free of crypto. (Exact placement decided in the
  implementation plan; the constraint is: no plaintext PIN in the DB.)

## 9. Data-flow changes

- **Startup:** `Database.connect()` → `SchemaInitializer.init()` runs `schema.sql`
  (`CREATE TABLE IF NOT EXISTS`), then, if the tables are empty, seeds the existing sample
  data (ATM-001A; John Smith CUST001; Savings ACC001 £5000; Current ACC002 £2500; card
  1234567890123456 PIN 1234; technician TECH001 PIN 9999) with PINs hashed.
- **Authentication:** technician and customer/card lookups go through DAOs instead of
  `ArrayList` scans. The loaded `Customer` includes its accounts and cards.
- **Check balance:** read-only; balance comes from the loaded/refreshed account.
- **Withdraw / Deposit:** model method runs (validates + mutates in-memory balance) →
  `AccountDAO.updateBalance` persists the new balance → `TransactionDAO.insert` records the
  transaction.
- **Transfer:** performed in a **single JDBC transaction** (autocommit off): debit source,
  credit destination, insert transaction row(s); `commit()` on success, `rollback()` on any
  failure. Durable version of the existing in-memory rollback.
- **Technician actions** (replenish cash, ink/paper, firmware/software upgrade): mutate the
  `ATM` object, then `AtmDAO.update` persists the new state.
- **Maintenance logs:** wherever `currentATM.addMaintenanceLog(...)` is called today, also
  `MaintenanceLogDAO.insert(...)` so logs survive restarts. "View Maintenance Logs" reads
  from the DB.

## 10. Error handling

- DB connection failure at startup → clear message, exit non-zero (an ATM with no backend
  cannot operate).
- DAO `SQLException`s caught and surfaced as friendly user messages; the menu loop stays
  alive where it safely can.
- Transfer failures trigger `rollback()` and an explicit failure message; no partial moves.

## 11. Testing

- Add JUnit (via Maven `test` scope).
- DAO integration tests against the local PostgreSQL: insert → read → update balance →
  verify; transfer atomicity (rollback on forced failure leaves balances unchanged);
  `PinHasher` round-trip (correct PIN verifies, wrong PIN rejected; same PIN with different
  salts yields different hashes).
- Documented manual end-to-end run for the live demo: launch, authenticate as customer,
  withdraw, restart, confirm the new balance and the transaction row persisted.

## 12. Out of scope (YAGNI)

- Connection pooling, ORM/JPA, multiple concurrent sessions.
- Migrations framework (Flyway/Liquibase) — `CREATE TABLE IF NOT EXISTS` is sufficient.
- GUI / web layer — remains a console application.
- Changing models' `double` arithmetic to `BigDecimal` end-to-end (DB column is `NUMERIC`;
  in-memory math stays `double` as today). Noted as a possible future improvement.

## 13. Note on version control

This project is not currently a git repository, so the design document is saved to disk but
not committed. If desired, run `git init` to start tracking history (and add a `.gitignore`
for `target/` and `db.properties`).
