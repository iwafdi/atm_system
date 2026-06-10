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
LANGUAGE / TECH: Pure Java (console application, no database, no GUI, no frameworks).
                 Data is held in memory using ArrayList. Compiled to /bin, source in /src.
PURPOSE: A teaching project that turns a UML class diagram into working object-oriented
         Java code. It simulates a real ATM with TWO kinds of users:
           1) Customer  — does banking transactions.
           2) Technician — does machine maintenance (this is the "Technician Extension").

CORE IDEA / CONCEPT:
  The project demonstrates Object-Oriented Programming (OOP): each real-world thing is a
  class (ATM, Customer, Account, Card, Technician, MaintenanceLog). Each class hides its
  own data (encapsulation) and exposes methods. One main controller class (ATMSystem) runs
  the menu loop and connects all the objects together. It separates "business logic" (the
  money in the Account) from "hardware state" (the physical cash inside the ATM machine).

THE CLASSES (the heart of the project):

1) ATMSystem (the Main / controller class — file: src/ATMSystem.java)
   - Holds the program's state: currentATM, currentCustomer, currentCard, currentTechnician.
   - main() loop: authenticate a user, then show the matching menu (customer or technician).
   - initializeSampleData(): hard-codes demo data so the program is testable:
       * Customer: "John Smith" (CUST001)
       * Card: 1234567890123456, PIN 1234, expiry 12/25, Debit
       * Accounts: Savings £5000.00 and Current £2500.00
       * ATM: ATM-001A, "Main Branch", £10000.00 cash
       * Technician: TECH001, PIN 9999, clearance "Level 3"
   - Customer menu: 1 Check Balance, 2 Withdraw, 3 Deposit, 4 Transfer, 5 Exit.
   - Technician menu: 1 Run Diagnostics, 2 Replenish Cash, 3 Replenish Ink/Paper,
                      4 Upgrade Firmware/Software, 5 View Maintenance Logs, 6 Exit.
   - Security: MAX_PIN_ATTEMPTS = 3 (card/technician locked after 3 wrong PINs).
   - Prints a receipt after each transaction.

2) Customer (src/models/Customer.java)
   - Fields: customerID, name, address, phoneNumber, List<Card> cards, List<Account> accounts.
   - A customer OWNS many cards and many accounts (this is a one-to-many association).
   - Methods: addCard, addAccount, getAccount(accountNumber), getters.

3) Account (src/models/Account.java)
   - Fields: accountNumber, accountType (Savings/Current), balance, customerID.
   - This is where the MONEY logic lives:
       * withdraw(amount): rejects amount <= 0, rejects if amount > balance, else subtracts.
       * deposit(amount): rejects amount <= 0, else adds.
       * transfer(target, amount): withdraw from this account then deposit into the target.
   - withdraw/deposit/transfer are declared "synchronized" (thread-safe — prevents two
     transactions changing the same balance at once).

4) Card (src/models/Card.java)
   - Fields: cardNumber, expiryDate, cardType, bankID, pin.
   - validate(): card number must be exactly 16 digits.
   - isExpired(): parses the MM/yy expiry date and checks if it is in the past.
   - validatePIN(entered): compares the entered PIN to the stored PIN.
   - toString() MASKS the card number (shows only the last 4 digits) — a small security touch.

5) Technician (src/models/Technician.java) — the EXTENSION
   - Fields: technicianID, name, clearanceLevel, pin.
   - authenticate(pin): checks the technician PIN.
   - Lets maintenance staff log in and service the machine, separate from customers.

6) ATM (src/models/ATM.java) — the PHYSICAL machine
   - Fields: atmID, location, cashInventory, inkLevel (0-100), paperLevel (0-100),
             firmwareVersion, softwareVersion, hardwareStatus, List<MaintenanceLog> logs.
   - dispenseCash(amount): removes physical cash (fails if not enough in the machine).
   - acceptCash(amount): adds deposited cash to the machine.
   - replenishCash / replenishInkAndPaper / applyUpgrade / runSelfDiagnostics: technician actions.
   - addMaintenanceLog / printMaintenanceLogs: keeps a service history.

7) MaintenanceLog (src/models/MaintenanceLog.java)
   - Fields: logID, timestamp, actionType, description, technicianID, atmID.
   - Records WHO (technician) did WHAT (action) and WHEN on WHICH atm.
   - Every technician action and every technician login/logout creates one log entry.

KEY DESIGN DETAIL TO HIGHLIGHT (shows the student understood the system):
  A withdrawal is a TWO-PART check. First the ATM must physically have the cash
  (currentATM.getCashInventory() >= amount), THEN the Account balance is reduced
  (account.withdraw), THEN the cash is dispensed (atm.dispenseCash). If the hardware
  dispense fails, the code ROLLS BACK by depositing the money back into the account.
  This separates "the account has money" from "the machine has cash" — a realistic detail.

OOP CONCEPTS THE PROJECT DEMONSTRATES (explain each in plain words):
  - Encapsulation: every field is private with getters/setters.
  - Association / composition: Customer HAS Accounts and Cards; ATM HAS MaintenanceLogs.
  - Separation of concerns: ATMSystem = control flow; model classes = data + behavior.
  - Single Responsibility: each class does one job.
  - Two user roles (Customer vs Technician) with two different menus and two PIN checks.
  - Defensive programming: validate input, limit PIN attempts, roll back on failure.

KNOWN LIMITATIONS (be honest if the instructor asks — turn these into "future work"):
  - Data is only in memory; nothing is saved when the program closes (no database/file).
  - PINs are stored as plain text (no hashing/encryption).
  - It is single-machine console only; no network, no real bank connection.
  - Money uses 'double' (real banking would use BigDecimal to avoid rounding errors).
  - There is no formal inheritance hierarchy (Customer/Technician do not extend a shared
    "User" superclass — a possible improvement).

============================================================
HOW TO WRITE IT
============================================================
- Audience: a class instructor + classmates. Tone: confident, simple, professional.
- The student is NOT an expert speaker — every speaker note must be plain and easy to say.
- In PART A, start with a title/intro slide and end with a "Conclusion & Future Work" slide.
- One slide must be the "Live Demo walkthrough" (the steps to run a customer withdrawal and
  a technician diagnostic, so the student can demo it).
- In PART B, for any tricky question, give the student a safe honest answer (it is fine to
  say "that is a limitation / future improvement" using the limitations listed above).
- At the very end, add a short "30-second elevator pitch" the student can memorize.

Now produce PART A and PART B.
```

---

## How to run the actual project (so you can demo it)

From the project folder:

```bash
# compile
javac -d bin src/ATMSystem.java src/models/*.java
# run
java -cp bin ATMSystem
```

Demo logins (already shown on screen when it starts):
- **Customer** → card `1234567890123456`, PIN `1234`
- **Technician** → ID `TECH001`, PIN `9999`
