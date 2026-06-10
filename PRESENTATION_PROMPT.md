# Prompt to give to Claude (chat) — generates the project presentation

Copy **everything** inside the code block below and paste it into Claude chat. It already
contains all the real facts about the project, so Claude does not need the source files.

---

```
You are helping a student prepare to PRESENT and DEFEND a school project in front of an
instructor. Produce TWO deliverables in clear, simple English (a student can read it aloud):

  PART A — A slide-by-slide presentation (8–12 slides). For each slide give:
           a short Title, 3–6 bullet points, and a "What to say" speaker-note paragraph
           the student can read out loud.
  PART B — A Q&A defense sheet: 15+ questions an instructor is likely to ask, each with a
           confident, correct, short answer. Include easy, medium, and tricky questions.

Keep it accurate to the facts below. Do not invent features that are not listed. If you
add anything extra, clearly mark it as a "possible improvement", not as something already built.

============================================================
PROJECT FACTS (this is the real project — use only this)
============================================================

NAME: "Ping-Source ATM System"
LANGUAGE / TECH: Java 17 console application backed by a PostgreSQL database (through JDBC).
                 Built with Maven. No GUI, no web framework. Source in src/main/java,
                 tests in src/test/java (JUnit), SQL and config in src/main/resources.
                 All data is now SAVED in the database and survives restarting the program.
PURPOSE: A teaching project that turns a UML class diagram into working object-oriented
         Java code, then adds a real PostgreSQL database so the data is permanent. It
         simulates a real ATM with TWO kinds of users:
           1) Customer  — does banking transactions.
           2) Technician — does machine maintenance (this is the "Technician Extension").

CORE IDEA / CONCEPT:
  The project demonstrates Object-Oriented Programming (OOP): each real-world thing is a
  class (ATM, Customer, Account, Card, Technician, MaintenanceLog). Each class hides its
  own data (encapsulation) and exposes methods. One main controller class (ATMSystem) runs
  the menu loop and connects all the objects together. It separates "business logic" (the
  money in the Account) from "hardware state" (the physical cash inside the ATM machine).
  A separate PERSISTENCE LAYER (the "db" package of DAO classes) saves and loads every
  object to/from PostgreSQL, so the domain classes stay clean and database-free — this is
  the "separation of concerns" / DAO (Data Access Object) pattern.

THE CLASSES (the heart of the project):

1) ATMSystem (the Main / controller class — file: src/main/java/ATMSystem.java)
   - Holds the program's state: currentATM, currentCustomer, currentCard, currentTechnician.
   - startup(): connects to PostgreSQL, creates the tables if they do not exist, seeds the
     demo data on the very first run only, builds the DAO objects, and loads the ATM. If the
     database cannot be reached it prints a clear error and stops.
   - main() loop: authenticate a user, then show the matching menu (customer or technician).
   - The seed (first-run demo data, inserted by SchemaInitializer.seedIfEmpty):
       * Customer: "John Smith" (CUST001)
       * Card: 1234567890123456, PIN 1234, expiry 12/28, Debit
       * Accounts: Savings £5000.00 and Current £2500.00
       * ATM: ATM-001A, "Main Branch", £10000.00 cash
       * Technician: TECH001, PIN 9999, clearance "Level 3"
   - Customer menu: 1 Check Balance, 2 Withdraw, 3 Deposit, 4 Transfer, 5 Exit.
   - Technician menu: 1 Run Diagnostics, 2 Replenish Cash, 3 Replenish Ink/Paper,
                      4 Upgrade Firmware/Software, 5 View Maintenance Logs, 6 Exit.
   - Every operation now also SAVES to the database: a withdraw/deposit updates the balance
     and writes a transaction row; a transfer is an atomic database transaction; technician
     actions update the ATM row and insert a maintenance-log row.
   - Security: MAX_PIN_ATTEMPTS = 3 (card/technician locked after 3 wrong PINs).
   - Prints a receipt after each transaction.

2) Customer (src/main/java/models/Customer.java)
   - Fields: customerID, name, address, phoneNumber, List<Card> cards, List<Account> accounts.
   - A customer OWNS many cards and many accounts (this is a one-to-many association).
   - Methods: addCard, addAccount, getAccount(accountNumber), getters.

3) Account (src/main/java/models/Account.java)
   - Fields: accountNumber, accountType (Savings/Current), balance, customerID.
   - This is where the MONEY logic lives:
       * withdraw(amount): rejects amount <= 0, rejects if amount > balance, else subtracts.
       * deposit(amount): rejects amount <= 0, else adds.
       * transfer(target, amount): withdraw from this account then deposit into the target.
   - withdraw/deposit/transfer are declared "synchronized" (thread-safe — prevents two
     transactions changing the same balance at once).

4) Card (src/main/java/models/Card.java)
   - Fields: cardNumber, expiryDate, cardType, bankID, pin.
   - validate(): card number must be exactly 16 digits.
   - isExpired(): parses the MM/yy expiry date and checks if it is in the past.
   - toString() MASKS the card number (shows only the last 4 digits) — a small security touch.
   - PIN checking is now done by the database layer: the PIN is stored in PostgreSQL as a
     SALTED SHA-256 HASH (never as plain text), and CardDAO.verifyPin() hashes what the user
     types and compares the hashes.

5) Technician (src/main/java/models/Technician.java) — the EXTENSION
   - Fields: technicianID, name, clearanceLevel, pin.
   - authenticate(pin): checks the technician PIN.
   - Lets maintenance staff log in and service the machine, separate from customers.

6) ATM (src/main/java/models/ATM.java) — the PHYSICAL machine
   - Fields: atmID, location, cashInventory, inkLevel (0-100), paperLevel (0-100),
             firmwareVersion, softwareVersion, hardwareStatus, List<MaintenanceLog> logs.
   - dispenseCash(amount): removes physical cash (fails if not enough in the machine).
   - acceptCash(amount): adds deposited cash to the machine.
   - replenishCash / replenishInkAndPaper / applyUpgrade / runSelfDiagnostics: technician actions.
   - addMaintenanceLog / printMaintenanceLogs: keeps a service history.

7) MaintenanceLog (src/main/java/models/MaintenanceLog.java)
   - Fields: logID, timestamp, actionType, description, technicianID, atmID.
   - Records WHO (technician) did WHAT (action) and WHEN on WHICH atm.
   - Every technician action and every technician login/logout creates one log entry.

8) Transaction (src/main/java/models/Transaction.java) — NEW with the database
   - Fields: transactionId, timestamp, type (WITHDRAWAL/DEPOSIT/TRANSFER), accountNumber,
             amount, balanceAfter, relatedAccount (for transfers), atmId.
   - A permanent history row written for every withdraw, deposit, and transfer.

THE PERSISTENCE LAYER (the "db" package — this is what makes the data permanent):
  - Database (src/main/java/db/Database.java): opens ONE shared JDBC connection to
    PostgreSQL, reading the address/username/password from src/main/resources/db.properties.
  - SchemaInitializer: runs schema.sql to CREATE the 7 tables if they don't exist, and
    seeds the demo data on the first run only.
  - DAO classes — one per table (CustomerDAO, AccountDAO, CardDAO, TechnicianDAO, AtmDAO,
    MaintenanceLogDAO, TransactionDAO). "DAO" = Data Access Object: it turns objects into
    SQL rows and back. All queries use PreparedStatement (parameterised SQL), which prevents
    SQL-injection attacks.
  - AccountDAO.transfer() runs as ONE database transaction: it debits, credits, and records
    the transfer, then COMMITS; if anything fails it ROLLS BACK so money is never half-moved.
  - PinHasher (src/main/java/util/PinHasher.java): salted SHA-256 hashing of PINs.
  - The 7 tables: customers, accounts, cards, technicians, atms, maintenance_logs,
    transactions. Money columns use NUMERIC(15,2) (exact money type, no rounding errors).

KEY DESIGN DETAIL TO HIGHLIGHT (shows the student understood the system):
  A withdrawal is a TWO-PART check. First the ATM must physically have the cash
  (currentATM.getCashInventory() >= amount), THEN the Account balance is reduced
  (account.withdraw), THEN the cash is dispensed (atm.dispenseCash). If the hardware
  dispense fails, the code ROLLS BACK by depositing the money back into the account.
  This separates "the account has money" from "the machine has cash" — a realistic detail.

SECOND KEY DETAIL (the database transfer):
  A transfer between two accounts is wrapped in ONE database transaction. The code turns
  off auto-commit, debits the source, credits the destination, writes the transaction row,
  and only then COMMITS. If any step throws, it ROLLS BACK — so the database can never end
  up with money taken from one account but not added to the other (this is "atomicity",
  the A in ACID).

OOP CONCEPTS THE PROJECT DEMONSTRATES (explain each in plain words):
  - Encapsulation: every field is private with getters/setters.
  - Association / composition: Customer HAS Accounts and Cards; ATM HAS MaintenanceLogs.
  - Separation of concerns: ATMSystem = control flow; model classes = data + behavior;
    the db/DAO classes = saving and loading. The model classes contain NO SQL.
  - DAO pattern: one Data Access Object per table hides all the database code behind a
    simple method like findById() or updateBalance().
  - Single Responsibility: each class does one job.
  - Two user roles (Customer vs Technician) with two different menus and two PIN checks.
  - Defensive programming: validate input, limit PIN attempts, roll back on failure,
    parameterised SQL (PreparedStatement) against injection, and hashed PINs.

WHAT THE DATABASE ADDED (say this proudly — it is the new work):
  - PostgreSQL persistence: balances, transactions, ATM state and logs all SURVIVE a restart.
  - A new transactions table = a full, permanent transaction history.
  - PINs are now stored hashed (salted SHA-256), not plain text.
  - Atomic transfers (commit/rollback) and parameterised SQL (injection-safe).
  - Exact money type in the database: NUMERIC(15,2).

KNOWN LIMITATIONS (be honest if the instructor asks — turn these into "future work"):
  - In-memory Java math still uses 'double'; the database column is exact NUMERIC(15,2), so
    the saved value is correct, but a fully professional build would use BigDecimal in Java too.
  - One shared database connection (fine for one console user; a real server would use a
    connection pool).
  - The database login is in a config file (db.properties); a real system would use a
    secrets manager / environment variables.
  - It is single-machine console only; no network, no real bank connection.
  - There is no formal inheritance hierarchy (Customer/Technician do not extend a shared
    "User" superclass — a possible improvement).
  - No migration tool (Flyway/Liquibase); the schema is created with CREATE TABLE IF NOT EXISTS.

============================================================
HOW TO WRITE IT
============================================================
- Audience: a class instructor + classmates. Tone: confident, simple, professional.
- The student is NOT an expert speaker — every speaker note must be plain and easy to say.
- In PART A, start with a title/intro slide and end with a "Conclusion & Future Work" slide.
- One slide must be the "Live Demo walkthrough" (the steps to run a customer withdrawal and
  a technician diagnostic, so the student can demo it). The strongest demo move: do a
  withdrawal, CLOSE the program, RE-OPEN it, and show the new balance is still there — this
  proves the database works.
- One slide should explain the database: the 7 tables, the DAO layer, and why persistence +
  hashed PINs + atomic transfers matter.
- In PART B, for any tricky question, give the student a safe honest answer (it is fine to
  say "that is a limitation / future improvement" using the limitations listed above).
- At the very end, add a short "30-second elevator pitch" the student can memorize.

Now produce PART A and PART B.
```

---

## How to run the actual project (so you can demo it)

Prerequisite: PostgreSQL must be running with a database named `atm_system`, and the
connection details in `src/main/resources/db.properties` must match. The tables and demo
data are created automatically on the first launch.

From the project folder (built with Maven):

```bash
# build a runnable jar (skip tests for a quick demo build)
mvn -q package -DskipTests
# run
java -jar target/atm-system.jar
```

(If Maven is not on your PATH on this machine, use the full path to it, e.g.
`~/apache-maven-3.9.6/bin/mvn`. You can also run directly with `mvn exec:java`.)

To run the JUnit database tests (needs a database named `atm_system_test`):

```bash
mvn test
```

Demo logins (already shown on screen when it starts):
- **Customer** → card `1234567890123456`, PIN `1234`
- **Technician** → ID `TECH001`, PIN `9999`

Tip to prove persistence live: withdraw some cash, quit, restart, then Check Balance — the
lower balance is still there because it was saved to PostgreSQL.
