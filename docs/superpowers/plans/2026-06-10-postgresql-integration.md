# PostgreSQL Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the Ping-Source ATM System (customers, accounts, cards, technicians, ATM state, maintenance logs, and a new transaction history) to PostgreSQL, replacing in-memory `ArrayList` storage.

**Architecture:** Models stay as plain POJOs with their business logic. A new `db` package (a `Database` connection holder, a `SchemaInitializer`, and one DAO per entity) handles all persistence. `ATMSystem` orchestrates: it calls a model's business method, then a DAO to persist. PINs are stored as salted SHA-256 hashes via a `util.PinHasher`.

**Tech Stack:** Java 17, Maven, PostgreSQL 16 (JDBC driver `org.postgresql:postgresql`), JUnit 5.

**Environment already prepared:**
- App database `atm_system` and test database `atm_system_test` exist on `localhost:5432`.
- Credentials: user `postgres`, password `postgres`.

---

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | Maven build: JDBC driver, JUnit 5, shade (fat jar), exec plugin |
| `.gitignore` | Ignore `target/`, real `db.properties` is kept (demo) |
| `src/main/java/ATMSystem.java` | Controller — calls models + DAOs (moved + modified) |
| `src/main/java/models/*.java` | Existing POJOs (moved, unchanged) + new `Transaction.java` |
| `src/main/java/db/Database.java` | Loads properties, owns the shared JDBC `Connection` |
| `src/main/java/db/SchemaInitializer.java` | Runs `schema.sql`; seeds sample data if empty |
| `src/main/java/db/*DAO.java` | One DAO per entity (Customer, Account, Card, Technician, Atm, MaintenanceLog, Transaction) |
| `src/main/java/util/PinHasher.java` | Salted SHA-256 hashing/verification |
| `src/main/resources/db.properties` | App DB connection settings |
| `src/main/resources/schema.sql` | `CREATE TABLE IF NOT EXISTS` for 7 tables |
| `src/test/resources/db.test.properties` | Test DB connection settings |
| `src/test/java/db/TestDb.java` | Test helper: connect to test DB + reset schema |
| `src/test/java/**/*Test.java` | JUnit integration/unit tests |

---

## Task 1: Maven project + git + standard layout

**Files:**
- Create: `pom.xml`, `.gitignore`
- Move: `src/ATMSystem.java` → `src/main/java/ATMSystem.java`
- Move: `src/models/*.java` → `src/main/java/models/*.java`
- Delete: `bin/` (stale precompiled classes)

- [ ] **Step 1: Initialize git so the commit cadence works**

```bash
cd /home/zakaria/Bureau/atm_system
git init
```

- [ ] **Step 2: Create `.gitignore`**

```gitignore
target/
*.class
bin/
```

- [ ] **Step 3: Move sources to the Maven standard layout and remove stale build output**

```bash
cd /home/zakaria/Bureau/atm_system
mkdir -p src/main/java/models src/main/resources src/test/java/db src/test/resources
git mv src/ATMSystem.java src/main/java/ATMSystem.java 2>/dev/null || mv src/ATMSystem.java src/main/java/ATMSystem.java
for f in MaintenanceLog Customer Technician Card ATM Account; do
  mv "src/models/$f.java" "src/main/java/models/$f.java"
done
rm -rf bin src/models
```

- [ ] **Step 4: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.pingsource</groupId>
    <artifactId>atm-system</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <exec.mainClass>ATMSystem</exec.mainClass>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.4</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>atm-system</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>ATMSystem</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <mainClass>ATMSystem</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: Verify it compiles**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q compile`
Expected: BUILD SUCCESS (downloads the PostgreSQL driver; existing code compiles unchanged).

- [ ] **Step 6: Commit**

```bash
cd /home/zakaria/Bureau/atm_system
git add -A
git commit -m "chore: convert to Maven layout, add PostgreSQL JDBC dependency"
```

---

## Task 2: Database connection holder

**Files:**
- Create: `src/main/resources/db.properties`
- Create: `src/test/resources/db.test.properties`
- Create: `src/main/java/db/Database.java`
- Test: `src/test/java/db/DatabaseTest.java`

- [ ] **Step 1: Create `src/main/resources/db.properties`**

```properties
db.url=jdbc:postgresql://localhost:5432/atm_system
db.user=postgres
db.password=postgres
```

- [ ] **Step 2: Create `src/test/resources/db.test.properties`**

```properties
db.url=jdbc:postgresql://localhost:5432/atm_system_test
db.user=postgres
db.password=postgres
```

- [ ] **Step 3: Write the failing test**

`src/test/java/db/DatabaseTest.java`
```java
package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void connectReturnsUsableConnection() throws Exception {
        Connection conn = Database.connect("db.test.properties");
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        assertTrue(conn.isValid(2));
    }

    @Test
    void getBeforeConnectThrows() {
        Database.close();
        assertThrows(IllegalStateException.class, Database::get);
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=DatabaseTest test`
Expected: FAIL/compile error — `db.Database` does not exist.

- [ ] **Step 5: Write `src/main/java/db/Database.java`**

```java
package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Owns the single shared JDBC connection for the ATM session.
 */
public class Database {
    private static Connection connection;

    /** Connects using the default app properties file. */
    public static Connection connect() {
        return connect("db.properties");
    }

    /** Connects using the given classpath properties resource. */
    public static Connection connect(String resourceName) {
        Properties props = loadProps(resourceName);
        try {
            connection = DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password"));
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    /** Returns the live connection, or throws if connect() was not called. */
    public static Connection get() {
        if (connection == null) {
            throw new IllegalStateException("Database not connected. Call connect() first.");
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private static Properties loadProps(String resourceName) {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new RuntimeException("Properties resource not found: " + resourceName);
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + resourceName, e);
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=DatabaseTest test`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: add Database connection holder and db.properties"
```

---

## Task 3: PinHasher utility (TDD)

**Files:**
- Create: `src/main/java/util/PinHasher.java`
- Test: `src/test/java/util/PinHasherTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/util/PinHasherTest.java`
```java
package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PinHasherTest {

    @Test
    void correctPinVerifies() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertTrue(PinHasher.verify("1234", salt, hash));
    }

    @Test
    void wrongPinRejected() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertFalse(PinHasher.verify("0000", salt, hash));
    }

    @Test
    void samePinDifferentSaltsProduceDifferentHashes() {
        String hashA = PinHasher.hash("1234", PinHasher.newSalt());
        String hashB = PinHasher.hash("1234", PinHasher.newSalt());
        assertNotEquals(hashA, hashB);
    }

    @Test
    void hashIsDeterministicForSameSalt() {
        String salt = PinHasher.newSalt();
        assertEquals(PinHasher.hash("9999", salt), PinHasher.hash("9999", salt));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=PinHasherTest test`
Expected: FAIL — `util.PinHasher` does not exist.

- [ ] **Step 3: Write `src/main/java/util/PinHasher.java`**

```java
package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted SHA-256 hashing for PINs. No plaintext PIN is ever persisted.
 */
public class PinHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a new random 16-byte salt, hex-encoded. */
    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** SHA-256 hash of (salt || pin), hex-encoded. */
    public static String hash(String pin, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(fromHex(saltHex));
            byte[] digest = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String pin, String saltHex, String expectedHash) {
        return hash(pin, saltHex).equals(expectedHash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=PinHasherTest test`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add salted SHA-256 PinHasher"
```

---

## Task 4: schema.sql + SchemaInitializer (schema creation)

**Files:**
- Create: `src/main/resources/schema.sql`
- Create: `src/main/java/db/SchemaInitializer.java`
- Create: `src/test/java/db/TestDb.java`
- Test: `src/test/java/db/SchemaInitializerTest.java`

- [ ] **Step 1: Create `src/main/resources/schema.sql`**

```sql
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
    atm_id           VARCHAR REFERENCES atms(atm_id)
);
```

- [ ] **Step 2: Create `src/test/java/db/TestDb.java` (test helper)**

```java
package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Test helper: connects to the test database and resets the schema to a clean state.
 */
public final class TestDb {

    private TestDb() {
    }

    /** Connects to the test DB and drops + recreates all tables. Returns the connection. */
    public static Connection freshConnection() {
        Connection conn = Database.connect("db.test.properties");
        dropAll(conn);
        SchemaInitializer.runSchema(conn);
        return conn;
    }

    private static void dropAll(Connection conn) {
        String sql = "DROP TABLE IF EXISTS transactions, maintenance_logs, cards, "
                + "accounts, technicians, atms, customers CASCADE";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop test tables", e);
        }
    }
}
```

- [ ] **Step 3: Write the failing test**

`src/test/java/db/SchemaInitializerTest.java`
```java
package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SchemaInitializerTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void runSchemaCreatesAllSevenTables() throws Exception {
        Connection conn = TestDb.freshConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) FROM information_schema.tables "
                   + "WHERE table_schema = 'public' AND table_name IN "
                   + "('customers','accounts','cards','technicians','atms',"
                   + "'maintenance_logs','transactions')")) {
            assertTrue(rs.next());
            assertEquals(7, rs.getInt(1));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=SchemaInitializerTest test`
Expected: FAIL — `SchemaInitializer.runSchema` does not exist.

- [ ] **Step 5: Write `src/main/java/db/SchemaInitializer.java` (schema only for now)**

```java
package db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the schema (idempotent) and, later, seeds sample data when empty.
 */
public class SchemaInitializer {

    /** Executes schema.sql against the given connection. */
    public static void runSchema(Connection conn) {
        String sql = readResource("schema.sql");
        try (Statement st = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    st.execute(statement);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to run schema.sql: " + e.getMessage(), e);
        }
    }

    private static String readResource(String name) {
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new RuntimeException("Resource not found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + name, e);
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=SchemaInitializerTest test`
Expected: PASS (1 test).

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: add schema.sql and SchemaInitializer.runSchema + test helper"
```

---

## Task 5: Transaction model

**Files:**
- Create: `src/main/java/models/Transaction.java`
- Test: `src/test/java/models/TransactionTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/models/TransactionTest.java`
```java
package models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void gettersReturnConstructorValues() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
        Transaction t = new Transaction(1, now, "WITHDRAWAL", "ACC001",
                100.0, 4900.0, null, "ATM-001A");
        assertEquals(1, t.getTransactionId());
        assertEquals(now, t.getTimestamp());
        assertEquals("WITHDRAWAL", t.getType());
        assertEquals("ACC001", t.getAccountNumber());
        assertEquals(100.0, t.getAmount());
        assertEquals(4900.0, t.getBalanceAfter());
        assertNull(t.getRelatedAccount());
        assertEquals("ATM-001A", t.getAtmId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TransactionTest test`
Expected: FAIL — `models.Transaction` does not exist.

- [ ] **Step 3: Write `src/main/java/models/Transaction.java`**

```java
package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single banking transaction (withdrawal, deposit, or transfer).
 */
public class Transaction {
    private int transactionId;
    private LocalDateTime timestamp;
    private String type;
    private String accountNumber;
    private double amount;
    private double balanceAfter;
    private String relatedAccount; // transfer destination; null otherwise
    private String atmId;

    public Transaction(int transactionId, LocalDateTime timestamp, String type,
                       String accountNumber, double amount, double balanceAfter,
                       String relatedAccount, String atmId) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedAccount = relatedAccount;
        this.atmId = atmId;
    }

    public int getTransactionId() { return transactionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getAccountNumber() { return accountNumber; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getRelatedAccount() { return relatedAccount; }
    public String getAtmId() { return atmId; }

    @Override
    public String toString() {
        return String.format("[%s] %s %s £%.2f -> balance £%.2f%s",
                timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                type, accountNumber, amount, balanceAfter,
                relatedAccount == null ? "" : " (to " + relatedAccount + ")");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TransactionTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add Transaction model"
```

---

## Task 6: AccountDAO (insert, find, updateBalance, atomic transfer)

**Files:**
- Create: `src/main/java/db/AccountDAO.java`
- Create: `src/main/java/db/TransactionDAO.java` (needed by transfer; full version completed in Task 12 — this minimal insert is its first form)
- Test: `src/test/java/db/AccountDAOTest.java`

> Note: `AccountDAO.transfer` records a `TRANSFER` row, so it depends on `TransactionDAO.insert`. We introduce `TransactionDAO` with its `insert` method here and extend it in Task 12.

- [ ] **Step 1: Write the failing test**

`src/test/java/db/AccountDAOTest.java`
```java
package db;

import models.Account;
import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountDAOTest {

    private AccountDAO accountDAO;
    private CustomerDAO customerDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        accountDAO = new AccountDAO(conn);
        customerDAO = new CustomerDAO(conn);
        customerDAO.insert(new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByNumber() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        Account found = accountDAO.findByNumber("ACC001");
        assertNotNull(found);
        assertEquals("Savings", found.getAccountType());
        assertEquals(5000.0, found.getBalance());
        assertEquals("CUST001", found.getCustomerID());
    }

    @Test
    void findByCustomerIdReturnsAll() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.insert(new Account("ACC002", "Current", 2500.0, "CUST001"));
        List<Account> accounts = accountDAO.findByCustomerId("CUST001");
        assertEquals(2, accounts.size());
    }

    @Test
    void updateBalancePersists() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.updateBalance("ACC001", 4900.0);
        assertEquals(4900.0, accountDAO.findByNumber("ACC001").getBalance());
    }

    @Test
    void transferMovesFundsAtomically() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.insert(new Account("ACC002", "Current", 2500.0, "CUST001"));
        accountDAO.transfer("ACC001", "ACC002", 1000.0, "ATM-001A");
        assertEquals(4000.0, accountDAO.findByNumber("ACC001").getBalance());
        assertEquals(3500.0, accountDAO.findByNumber("ACC002").getBalance());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=AccountDAOTest test`
Expected: FAIL — `AccountDAO`, `CustomerDAO`, `TransactionDAO` do not exist.

- [ ] **Step 3: Create minimal `src/main/java/db/TransactionDAO.java`**

```java
package db;

import models.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Persists banking transactions. Extended with query methods in a later task.
 */
public class TransactionDAO {
    private final Connection conn;

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserts a transaction row using the supplied connection's current
     * transaction context (so it can participate in an atomic transfer).
     */
    public void insert(String type, String accountNumber, double amount,
                       double balanceAfter, String relatedAccount, String atmId) {
        String sql = "INSERT INTO transactions "
                + "(timestamp, type, account_number, amount, balance_after, related_account, atm_id) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, type);
            ps.setString(3, accountNumber);
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceAfter);
            ps.setString(6, relatedAccount);
            ps.setString(7, atmId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Create stub `src/main/java/db/CustomerDAO.java` (insert only; full version in Task 7)**

```java
package db;

import models.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Persists customers. Extended with query methods in a later task.
 */
public class CustomerDAO {
    private final Connection conn;

    public CustomerDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, name, address, phone_number) "
                + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerID());
            ps.setString(2, customer.getName());
            ps.setString(3, customer.getAddress());
            ps.setString(4, customer.getPhoneNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert customer: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: Write `src/main/java/db/AccountDAO.java`**

```java
package db;

import models.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists accounts and performs atomic transfers.
 */
public class AccountDAO {
    private final Connection conn;

    public AccountDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Account account) {
        String sql = "INSERT INTO accounts (account_number, account_type, balance, customer_id) "
                + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountType());
            ps.setDouble(3, account.getBalance());
            ps.setString(4, account.getCustomerID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert account: " + e.getMessage(), e);
        }
    }

    public Account findByNumber(String accountNumber) {
        String sql = "SELECT account_number, account_type, balance, customer_id "
                + "FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account: " + e.getMessage(), e);
        }
    }

    public List<Account> findByCustomerId(String customerId) {
        String sql = "SELECT account_number, account_type, balance, customer_id "
                + "FROM accounts WHERE customer_id = ? ORDER BY account_number";
        List<Account> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list accounts: " + e.getMessage(), e);
        }
        return result;
    }

    public void updateBalance(String accountNumber, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically debits source, credits destination, and records a TRANSFER
     * transaction. Rolls back on any failure.
     */
    public void transfer(String fromAccount, String toAccount, double amount, String atmId) {
        try {
            conn.setAutoCommit(false);
            double fromBalance = findByNumber(fromAccount).getBalance() - amount;
            double toBalance = findByNumber(toAccount).getBalance() + amount;
            updateBalance(fromAccount, fromBalance);
            updateBalance(toAccount, toBalance);
            new TransactionDAO(conn).insert("TRANSFER", fromAccount, amount,
                    fromBalance, toAccount, atmId);
            conn.commit();
        } catch (RuntimeException e) {
            rollbackQuietly();
            throw new RuntimeException("Transfer failed and was rolled back: " + e.getMessage(), e);
        } finally {
            setAutoCommitQuietly(true);
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void setAutoCommitQuietly(boolean value) {
        try {
            conn.setAutoCommit(value);
        } catch (SQLException ignored) {
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("account_number"),
                rs.getString("account_type"),
                rs.getDouble("balance"),
                rs.getString("customer_id"));
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=AccountDAOTest test`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: add AccountDAO with atomic transfer; stub Customer/Transaction DAOs"
```

---

## Task 7: CustomerDAO (find)

**Files:**
- Modify: `src/main/java/db/CustomerDAO.java`
- Test: `src/test/java/db/CustomerDAOTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/CustomerDAOTest.java`
```java
package db;

import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDAOTest {

    private CustomerDAO customerDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        customerDAO = new CustomerDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindById() {
        customerDAO.insert(new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
        Customer found = customerDAO.findById("CUST001");
        assertNotNull(found);
        assertEquals("John Smith", found.getName());
        assertEquals("123 Main St", found.getAddress());
        assertEquals("07700900000", found.getPhoneNumber());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(customerDAO.findById("NOPE"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=CustomerDAOTest test`
Expected: FAIL — `CustomerDAO.findById` does not exist.

- [ ] **Step 3: Add `findById` to `src/main/java/db/CustomerDAO.java`**

Add these imports at the top (next to the existing ones):
```java
import java.sql.ResultSet;
```

Add this method inside the class (after `insert`):
```java
    public Customer findById(String customerId) {
        String sql = "SELECT customer_id, name, address, phone_number "
                + "FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getString("customer_id"),
                            rs.getString("name"),
                            rs.getString("address"),
                            rs.getString("phone_number"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer: " + e.getMessage(), e);
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=CustomerDAOTest test`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add CustomerDAO.findById"
```

---

## Task 8: CardDAO (insert with hashed PIN, lookups, verifyPin)

**Files:**
- Create: `src/main/java/db/CardDAO.java`
- Test: `src/test/java/db/CardDAOTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/CardDAOTest.java`
```java
package db;

import models.Card;
import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardDAOTest {

    private CardDAO cardDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new CustomerDAO(conn).insert(
                new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
        cardDAO = new CardDAO(conn);
        cardDAO.insert(new Card("1234567890123456", "12/25", "Debit", "BANK001", "1234"), "CUST001");
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void findByCardNumberReturnsCardWithoutPlaintextPin() {
        Card card = cardDAO.findByCardNumber("1234567890123456");
        assertNotNull(card);
        assertEquals("12/25", card.getExpiryDate());
        assertEquals("Debit", card.getCardType());
        assertEquals("BANK001", card.getBankID());
    }

    @Test
    void findByCardNumberReturnsNullWhenMissing() {
        assertNull(cardDAO.findByCardNumber("0000000000000000"));
    }

    @Test
    void findCustomerIdByCardNumber() {
        assertEquals("CUST001", cardDAO.findCustomerIdByCardNumber("1234567890123456"));
    }

    @Test
    void findByCustomerIdReturnsCards() {
        List<Card> cards = cardDAO.findByCustomerId("CUST001");
        assertEquals(1, cards.size());
    }

    @Test
    void verifyPinAcceptsCorrectAndRejectsWrong() {
        assertTrue(cardDAO.verifyPin("1234567890123456", "1234"));
        assertFalse(cardDAO.verifyPin("1234567890123456", "0000"));
    }

    @Test
    void pinIsNotStoredInPlaintext() throws Exception {
        Connection conn = Database.get();
        try (var st = conn.createStatement();
             var rs = st.executeQuery("SELECT pin_hash FROM cards WHERE card_number = '1234567890123456'")) {
            assertTrue(rs.next());
            assertNotEquals("1234", rs.getString("pin_hash"));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=CardDAOTest test`
Expected: FAIL — `CardDAO` does not exist.

- [ ] **Step 3: Write `src/main/java/db/CardDAO.java`**

```java
package db;

import models.Card;
import util.PinHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists cards. PINs are stored as salted hashes; the model never receives
 * a plaintext PIN when loaded from the DB.
 */
public class CardDAO {
    private final Connection conn;

    public CardDAO(Connection conn) {
        this.conn = conn;
    }

    /** Inserts a card, hashing the plaintext PIN held by the model. */
    public void insert(Card card, String customerId) {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash(card.getPinForSeeding(), salt);
        String sql = "INSERT INTO cards "
                + "(card_number, expiry_date, card_type, bank_id, pin_hash, pin_salt, customer_id) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getCardNumber());
            ps.setString(2, card.getExpiryDate());
            ps.setString(3, card.getCardType());
            ps.setString(4, card.getBankID());
            ps.setString(5, hash);
            ps.setString(6, salt);
            ps.setString(7, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert card: " + e.getMessage(), e);
        }
    }

    /** Returns the card (with an empty PIN — auth goes through verifyPin). */
    public Card findByCardNumber(String cardNumber) {
        String sql = "SELECT card_number, expiry_date, card_type, bank_id "
                + "FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find card: " + e.getMessage(), e);
        }
    }

    public String findCustomerIdByCardNumber(String cardNumber) {
        String sql = "SELECT customer_id FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("customer_id") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find card owner: " + e.getMessage(), e);
        }
    }

    public List<Card> findByCustomerId(String customerId) {
        String sql = "SELECT card_number, expiry_date, card_type, bank_id "
                + "FROM cards WHERE customer_id = ?";
        List<Card> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list cards: " + e.getMessage(), e);
        }
        return result;
    }

    /** Verifies the entered PIN against the stored salted hash. */
    public boolean verifyPin(String cardNumber, String enteredPin) {
        String sql = "SELECT pin_hash, pin_salt FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PinHasher.verify(enteredPin, rs.getString("pin_salt"), rs.getString("pin_hash"));
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify card PIN: " + e.getMessage(), e);
        }
    }

    private Card mapRow(ResultSet rs) throws SQLException {
        return new Card(
                rs.getString("card_number"),
                rs.getString("expiry_date"),
                rs.getString("card_type"),
                rs.getString("bank_id"),
                ""); // no plaintext PIN for DB-loaded cards
    }
}
```

- [ ] **Step 4: Add a PIN accessor to `src/main/java/models/Card.java`**

The DAO needs the plaintext PIN only at seed/insert time. Add this getter to `Card` (the field `pin` already exists):
```java
    /** Plaintext PIN, used only when seeding a card into the database. */
    public String getPinForSeeding() {
        return pin;
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=CardDAOTest test`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: add CardDAO with hashed-PIN storage and verifyPin"
```

---

## Task 9: TechnicianDAO (insert with hashed PIN, find, verifyPin)

**Files:**
- Create: `src/main/java/db/TechnicianDAO.java`
- Modify: `src/main/java/models/Technician.java` (add seed-PIN accessor)
- Test: `src/test/java/db/TechnicianDAOTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/TechnicianDAOTest.java`
```java
package db;

import models.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class TechnicianDAOTest {

    private TechnicianDAO technicianDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        technicianDAO = new TechnicianDAO(conn);
        technicianDAO.insert(new Technician("TECH001", "Alice Tech", "Level 3", "9999"));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void findByIdReturnsTechnician() {
        Technician t = technicianDAO.findById("TECH001");
        assertNotNull(t);
        assertEquals("Alice Tech", t.getName());
        assertEquals("Level 3", t.getClearanceLevel());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(technicianDAO.findById("NOPE"));
    }

    @Test
    void verifyPinAcceptsCorrectAndRejectsWrong() {
        assertTrue(technicianDAO.verifyPin("TECH001", "9999"));
        assertFalse(technicianDAO.verifyPin("TECH001", "0000"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TechnicianDAOTest test`
Expected: FAIL — `TechnicianDAO` does not exist.

- [ ] **Step 3: Add a PIN accessor to `src/main/java/models/Technician.java`**

The field `pin` already exists. Add this getter:
```java
    /** Plaintext PIN, used only when seeding a technician into the database. */
    public String getPinForSeeding() {
        return pin;
    }
```

- [ ] **Step 4: Write `src/main/java/db/TechnicianDAO.java`**

```java
package db;

import models.Technician;
import util.PinHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists technicians. PINs are stored as salted hashes.
 */
public class TechnicianDAO {
    private final Connection conn;

    public TechnicianDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Technician technician) {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash(technician.getPinForSeeding(), salt);
        String sql = "INSERT INTO technicians "
                + "(technician_id, name, clearance_level, pin_hash, pin_salt) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technician.getTechnicianID());
            ps.setString(2, technician.getName());
            ps.setString(3, technician.getClearanceLevel());
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert technician: " + e.getMessage(), e);
        }
    }

    public Technician findById(String technicianId) {
        String sql = "SELECT technician_id, name, clearance_level "
                + "FROM technicians WHERE technician_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Technician(
                            rs.getString("technician_id"),
                            rs.getString("name"),
                            rs.getString("clearance_level"),
                            ""); // no plaintext PIN for DB-loaded technicians
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find technician: " + e.getMessage(), e);
        }
    }

    public boolean verifyPin(String technicianId, String enteredPin) {
        String sql = "SELECT pin_hash, pin_salt FROM technicians WHERE technician_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PinHasher.verify(enteredPin, rs.getString("pin_salt"), rs.getString("pin_hash"));
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify technician PIN: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TechnicianDAOTest test`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: add TechnicianDAO with hashed-PIN storage and verifyPin"
```

---

## Task 10: AtmDAO (insert, find, update)

**Files:**
- Create: `src/main/java/db/AtmDAO.java`
- Modify: `src/main/java/models/ATM.java` (add a full-state constructor + status setter)
- Test: `src/test/java/db/AtmDAOTest.java`

> The existing `ATM` constructor only takes `(atmID, location, initialCash)` and always
> resets ink/paper/versions/status to defaults. To load persisted state we add a second
> constructor that takes every field. The existing constructor is preserved.

- [ ] **Step 1: Add a full-state constructor to `src/main/java/models/ATM.java`**

Add this constructor after the existing one:
```java
    /** Full-state constructor used when loading an ATM from the database. */
    public ATM(String atmID, String location, double cashInventory, int inkLevel,
               int paperLevel, String firmwareVersion, String softwareVersion,
               String hardwareStatus) {
        this.atmID = atmID;
        this.location = location;
        this.cashInventory = cashInventory;
        this.inkLevel = inkLevel;
        this.paperLevel = paperLevel;
        this.firmwareVersion = firmwareVersion;
        this.softwareVersion = softwareVersion;
        this.hardwareStatus = hardwareStatus;
        this.logs = new java.util.ArrayList<>();
    }
```

- [ ] **Step 2: Write the failing test**

`src/test/java/db/AtmDAOTest.java`
```java
package db;

import models.ATM;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class AtmDAOTest {

    private AtmDAO atmDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        atmDAO = new AtmDAO(conn);
        atmDAO.insert(new ATM("ATM-001A", "Main Branch", 10000.0));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindById() {
        ATM atm = atmDAO.findById("ATM-001A");
        assertNotNull(atm);
        assertEquals("Main Branch", atm.getLocation());
        assertEquals(10000.0, atm.getCashInventory());
        assertEquals(100, atm.getInkLevel());
        assertEquals("OPERATIONAL", atm.getHardwareStatus());
    }

    @Test
    void updatePersistsNewState() {
        ATM atm = atmDAO.findById("ATM-001A");
        atm.replenishCash(5000.0);     // cash -> 15000
        atm.applyUpgrade("firmware", "v2.0.0");
        atmDAO.update(atm);

        ATM reloaded = atmDAO.findById("ATM-001A");
        assertEquals(15000.0, reloaded.getCashInventory());
        assertEquals("v2.0.0", reloaded.getFirmwareVersion());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=AtmDAOTest test`
Expected: FAIL — `AtmDAO` does not exist.

- [ ] **Step 4: Write `src/main/java/db/AtmDAO.java`**

```java
package db;

import models.ATM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists ATM hardware/software state.
 */
public class AtmDAO {
    private final Connection conn;

    public AtmDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(ATM atm) {
        String sql = "INSERT INTO atms (atm_id, location, cash_inventory, ink_level, "
                + "paper_level, firmware_version, software_version, hardware_status) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, atm);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert ATM: " + e.getMessage(), e);
        }
    }

    public ATM findById(String atmId) {
        String sql = "SELECT atm_id, location, cash_inventory, ink_level, paper_level, "
                + "firmware_version, software_version, hardware_status FROM atms WHERE atm_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atmId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ATM(
                            rs.getString("atm_id"),
                            rs.getString("location"),
                            rs.getDouble("cash_inventory"),
                            rs.getInt("ink_level"),
                            rs.getInt("paper_level"),
                            rs.getString("firmware_version"),
                            rs.getString("software_version"),
                            rs.getString("hardware_status"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ATM: " + e.getMessage(), e);
        }
    }

    public void update(ATM atm) {
        String sql = "UPDATE atms SET location = ?, cash_inventory = ?, ink_level = ?, "
                + "paper_level = ?, firmware_version = ?, software_version = ?, "
                + "hardware_status = ? WHERE atm_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atm.getLocation());
            ps.setDouble(2, atm.getCashInventory());
            ps.setInt(3, atm.getInkLevel());
            ps.setInt(4, atm.getPaperLevel());
            ps.setString(5, atm.getFirmwareVersion());
            ps.setString(6, atm.getSoftwareVersion());
            ps.setString(7, atm.getHardwareStatus());
            ps.setString(8, atm.getAtmID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update ATM: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, ATM atm) throws SQLException {
        ps.setString(1, atm.getAtmID());
        ps.setString(2, atm.getLocation());
        ps.setDouble(3, atm.getCashInventory());
        ps.setInt(4, atm.getInkLevel());
        ps.setInt(5, atm.getPaperLevel());
        ps.setString(6, atm.getFirmwareVersion());
        ps.setString(7, atm.getSoftwareVersion());
        ps.setString(8, atm.getHardwareStatus());
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=AtmDAOTest test`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: add AtmDAO and full-state ATM constructor"
```

---

## Task 11: MaintenanceLogDAO (insert, find by ATM)

**Files:**
- Create: `src/main/java/db/MaintenanceLogDAO.java`
- Test: `src/test/java/db/MaintenanceLogDAOTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/MaintenanceLogDAOTest.java`
```java
package db;

import models.ATM;
import models.MaintenanceLog;
import models.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceLogDAOTest {

    private MaintenanceLogDAO logDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new AtmDAO(conn).insert(new ATM("ATM-001A", "Main Branch", 10000.0));
        new TechnicianDAO(conn).insert(new Technician("TECH001", "Alice Tech", "Level 3", "9999"));
        logDAO = new MaintenanceLogDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByAtmId() {
        logDAO.insert(new MaintenanceLog("LOG-1", "DIAGNOSTICS", "Ran diagnostics",
                "TECH001", "ATM-001A"));
        List<MaintenanceLog> logs = logDAO.findByAtmId("ATM-001A");
        assertEquals(1, logs.size());
        assertEquals("DIAGNOSTICS", logs.get(0).getActionType());
        assertEquals("TECH001", logs.get(0).getTechnicianID());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=MaintenanceLogDAOTest test`
Expected: FAIL — `MaintenanceLogDAO` does not exist.

- [ ] **Step 3: Write `src/main/java/db/MaintenanceLogDAO.java`**

```java
package db;

import models.MaintenanceLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists ATM maintenance logs.
 */
public class MaintenanceLogDAO {
    private final Connection conn;

    public MaintenanceLogDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(MaintenanceLog log) {
        String sql = "INSERT INTO maintenance_logs "
                + "(log_id, timestamp, action_type, description, technician_id, atm_id) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.getLogID());
            ps.setTimestamp(2, Timestamp.valueOf(log.getTimestamp()));
            ps.setString(3, log.getActionType());
            ps.setString(4, log.getDescription());
            ps.setString(5, log.getTechnicianID());
            ps.setString(6, log.getAtmID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert maintenance log: " + e.getMessage(), e);
        }
    }

    public List<MaintenanceLog> findByAtmId(String atmId) {
        String sql = "SELECT log_id, action_type, description, technician_id, atm_id "
                + "FROM maintenance_logs WHERE atm_id = ? ORDER BY timestamp";
        List<MaintenanceLog> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atmId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MaintenanceLog(
                            rs.getString("log_id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("technician_id"),
                            rs.getString("atm_id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list maintenance logs: " + e.getMessage(), e);
        }
        return result;
    }
}
```

> Note: `MaintenanceLog`'s constructor sets `timestamp` to `now()` on construction, so the
> reloaded timestamp will differ from the original. This is acceptable — logs are
> append-only and displayed in insertion order. The test does not assert on timestamp.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=MaintenanceLogDAOTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add MaintenanceLogDAO"
```

---

## Task 12: TransactionDAO query method + insert test

**Files:**
- Modify: `src/main/java/db/TransactionDAO.java` (add `findByAccount`)
- Test: `src/test/java/db/TransactionDAOTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/TransactionDAOTest.java`
```java
package db;

import models.Account;
import models.Customer;
import models.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDAOTest {

    private TransactionDAO transactionDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new CustomerDAO(conn).insert(new Customer("CUST001", "John", "addr", "phone"));
        new AccountDAO(conn).insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        transactionDAO = new TransactionDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByAccount() {
        transactionDAO.insert("WITHDRAWAL", "ACC001", 100.0, 4900.0, null, "ATM-001A");
        List<Transaction> txns = transactionDAO.findByAccount("ACC001");
        assertEquals(1, txns.size());
        Transaction t = txns.get(0);
        assertEquals("WITHDRAWAL", t.getType());
        assertEquals(100.0, t.getAmount());
        assertEquals(4900.0, t.getBalanceAfter());
        assertTrue(t.getTransactionId() > 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TransactionDAOTest test`
Expected: FAIL — `TransactionDAO.findByAccount` does not exist.

- [ ] **Step 3: Add `findByAccount` to `src/main/java/db/TransactionDAO.java`**

Add these imports next to the existing ones:
```java
import models.Transaction;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
```

Add this method inside the class:
```java
    public List<Transaction> findByAccount(String accountNumber) {
        String sql = "SELECT transaction_id, timestamp, type, account_number, amount, "
                + "balance_after, related_account, atm_id FROM transactions "
                + "WHERE account_number = ? ORDER BY transaction_id";
        List<Transaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Transaction(
                            rs.getInt("transaction_id"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("type"),
                            rs.getString("account_number"),
                            rs.getDouble("amount"),
                            rs.getDouble("balance_after"),
                            rs.getString("related_account"),
                            rs.getString("atm_id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list transactions: " + e.getMessage(), e);
        }
        return result;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=TransactionDAOTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add TransactionDAO.findByAccount"
```

---

## Task 13: SchemaInitializer.seedIfEmpty (seed sample data)

**Files:**
- Modify: `src/main/java/db/SchemaInitializer.java`
- Test: `src/test/java/db/SchemaSeedTest.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/db/SchemaSeedTest.java`
```java
package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SchemaSeedTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void seedsSampleDataWhenEmptyAndIsIdempotent() throws Exception {
        Connection conn = TestDb.freshConnection();

        SchemaInitializer.seedIfEmpty(conn);
        SchemaInitializer.seedIfEmpty(conn); // second call must not duplicate

        assertEquals(1, count(conn, "customers"));
        assertEquals(2, count(conn, "accounts"));
        assertEquals(1, count(conn, "cards"));
        assertEquals(1, count(conn, "technicians"));
        assertEquals(1, count(conn, "atms"));

        // Seeded PINs verify through the DAOs (hashed, not plaintext)
        assertTrue(new CardDAO(conn).verifyPin("1234567890123456", "1234"));
        assertTrue(new TechnicianDAO(conn).verifyPin("TECH001", "9999"));
    }

    private int count(Connection conn, String table) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=SchemaSeedTest test`
Expected: FAIL — `SchemaInitializer.seedIfEmpty` does not exist.

- [ ] **Step 3: Add `seedIfEmpty` (and a private `isEmpty`) to `src/main/java/db/SchemaInitializer.java`**

Add these imports next to the existing ones:
```java
import models.ATM;
import models.Account;
import models.Card;
import models.Customer;
import models.Technician;
import java.sql.ResultSet;
```

Add these methods inside the class:
```java
    /** Seeds the original sample data, but only if the customers table is empty. */
    public static void seedIfEmpty(Connection conn) {
        if (!isEmpty(conn)) {
            return;
        }
        new AtmDAO(conn).insert(new ATM("ATM-001A", "Main Branch", 10000.00));

        Customer customer = new Customer("CUST001", "John Smith", "123 Main St", "07700900000");
        new CustomerDAO(conn).insert(customer);

        AccountDAO accountDAO = new AccountDAO(conn);
        accountDAO.insert(new Account("ACC001", "Savings", 5000.00, "CUST001"));
        accountDAO.insert(new Account("ACC002", "Current", 2500.00, "CUST001"));

        new CardDAO(conn).insert(
                new Card("1234567890123456", "12/25", "Debit", "BANK001", "1234"), "CUST001");

        new TechnicianDAO(conn).insert(
                new Technician("TECH001", "Alice Tech", "Level 3", "9999"));
    }

    private static boolean isEmpty(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM customers")) {
            rs.next();
            return rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check seed state: " + e.getMessage(), e);
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q -Dtest=SchemaSeedTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Run the full test suite to confirm nothing regressed**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q test`
Expected: PASS (all tests across all DAO/util/model test classes).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: seed sample data on first run via SchemaInitializer.seedIfEmpty"
```

---

## Task 14: Wire ATMSystem — startup, DB-backed auth, shared DAOs

**Files:**
- Modify: `src/main/java/ATMSystem.java`

> This task rewires the controller to use the database. It replaces the in-memory
> `technicians`/`customers` lists and `initializeSampleData()` with DB startup + DAO lookups,
> and routes PIN checks through the DAOs. Balance/ATM/log persistence is added in Tasks 15-17.

- [ ] **Step 1: Replace the field declarations and `main` startup**

In `src/main/java/ATMSystem.java`, replace the imports block and the static fields/`main`/`initializeSampleData` region (lines from `import models.*;` through the end of `initializeSampleData()`) with:

```java
import models.*;
import db.*;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

/**
 * Main ATM System Application (PostgreSQL-backed).
 */
public class ATMSystem {
    private static Scanner scanner = new Scanner(System.in);

    // Global session state
    private static ATM currentATM = null;
    private static Customer currentCustomer = null;
    private static Card currentCard = null;
    private static Technician currentTechnician = null;

    // DAOs (initialized after the DB connection opens)
    private static AccountDAO accountDAO;
    private static CustomerDAO customerDAO;
    private static CardDAO cardDAO;
    private static TechnicianDAO technicianDAO;
    private static AtmDAO atmDAO;
    private static MaintenanceLogDAO logDAO;
    private static TransactionDAO transactionDAO;

    private static final String ATM_ID = "ATM-001A";
    private static final int MAX_PIN_ATTEMPTS = 3;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   WELCOME TO PING-SOURCE ATM SYSTEM");
        System.out.println("===========================================\n");

        if (!startup()) {
            return;
        }

        boolean running = true;
        while (running) {
            running = authenticateUser();
            if (running) {
                if (currentTechnician != null) {
                    showTechnicianMenu();
                } else if (currentCustomer != null) {
                    showMainMenu();
                }
            }
        }

        Database.close();
        System.out.println("\nThank you for using Ping-Source ATM!");
        scanner.close();
    }

    /**
     * Connects to PostgreSQL, ensures the schema exists, seeds sample data on
     * first run, builds the DAOs, and loads the ATM. Returns false on failure.
     */
    private static boolean startup() {
        try {
            Connection conn = Database.connect();
            SchemaInitializer.runSchema(conn);
            SchemaInitializer.seedIfEmpty(conn);

            accountDAO = new AccountDAO(conn);
            customerDAO = new CustomerDAO(conn);
            cardDAO = new CardDAO(conn);
            technicianDAO = new TechnicianDAO(conn);
            atmDAO = new AtmDAO(conn);
            logDAO = new MaintenanceLogDAO(conn);
            transactionDAO = new TransactionDAO(conn);

            currentATM = atmDAO.findById(ATM_ID);
            if (currentATM == null) {
                System.out.println("❌ ATM " + ATM_ID + " not found in database.");
                return false;
            }
            System.out.println("Connected to database. ATM " + ATM_ID + " ready.\n");
            return true;
        } catch (RuntimeException e) {
            System.out.println("❌ Could not start ATM: " + e.getMessage());
            System.out.println("   Check that PostgreSQL is running and db.properties is correct.");
            return false;
        }
    }
```

> This deletes the old `private static List<Technician> technicians` / `List<Customer> customers`
> fields and the entire `initializeSampleData()` method. Keep the rest of the class for now;
> the next steps fix the methods that referenced the deleted lists.

- [ ] **Step 2: Rewrite `authenticateUser` to look up the technician via the DAO**

Replace the body of `authenticateUser()` with:
```java
    private static boolean authenticateUser() {
        currentCustomer = null;
        currentTechnician = null;
        currentCard = null;

        System.out.println("\n--- AUTHENTICATION ---");
        System.out.print("Insert card or Enter Technician ID (or 'exit' to quit): ");
        String authInput = scanner.nextLine();

        if (authInput.equalsIgnoreCase("exit")) {
            return false;
        }

        Technician tech = technicianDAO.findById(authInput);
        if (tech != null) {
            return authenticateTechnician(tech);
        }

        return authenticateCustomer(authInput);
    }
```

- [ ] **Step 3: Rewrite `authenticateTechnician` to verify the PIN via the DAO**

Replace the existing `authenticateTechnician(...)` method (the one taking `initialInput`) with:
```java
    private static boolean authenticateTechnician(Technician tech) {
        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter Technician PIN: ");
            String pin = scanner.nextLine();

            if (technicianDAO.verifyPin(tech.getTechnicianID(), pin)) {
                return finalizeTechnicianLogin(tech);
            }
            attempts++;
            if (attempts < MAX_PIN_ATTEMPTS) {
                System.out.println("❌ Incorrect PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempts remaining.");
            }
        }
        System.out.println("❌ Maximum PIN attempts exceeded. Technician account locked.");
        return true;
    }
```

- [ ] **Step 4: Update `finalizeTechnicianLogin` to persist the login log**

Replace `finalizeTechnicianLogin(...)` with:
```java
    private static boolean finalizeTechnicianLogin(Technician tech) {
        System.out.println("✓ Technician Authentication successful!\n");
        currentTechnician = tech;
        recordLog("TECHNICIAN_LOGIN", "Technician logged in successfully.");
        return true;
    }
```

- [ ] **Step 5: Rewrite `authenticateCustomer` to load from the DB**

Replace `authenticateCustomer(...)` with:
```java
    private static boolean authenticateCustomer(String cardInput) {
        Card card = cardDAO.findByCardNumber(cardInput);
        if (card == null) {
            System.out.println("❌ Invalid card number. Card returned.");
            return true;
        }
        if (!card.validate()) {
            System.out.println("❌ Invalid card format. Card returned.");
            return true;
        }
        if (card.isExpired()) {
            System.out.println("❌ Card has expired. Please contact your bank.");
            return true;
        }

        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine();

            if (cardDAO.verifyPin(cardInput, pin)) {
                currentCard = card;
                currentCustomer = loadCustomer(cardDAO.findCustomerIdByCardNumber(cardInput));
                System.out.println("✓ Authentication successful!\n");
                return true;
            }
            attempts++;
            if (attempts < MAX_PIN_ATTEMPTS) {
                System.out.println("❌ Incorrect PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempts remaining.");
            }
        }
        System.out.println("❌ Maximum PIN attempts exceeded. Card blocked.");
        return true;
    }

    /** Loads a customer with its accounts and cards attached. */
    private static Customer loadCustomer(String customerId) {
        Customer customer = customerDAO.findById(customerId);
        for (Account account : accountDAO.findByCustomerId(customerId)) {
            customer.addAccount(account);
        }
        for (Card card : cardDAO.findByCustomerId(customerId)) {
            customer.addCard(card);
        }
        return customer;
    }
```

- [ ] **Step 6: Replace `logTechnicianAction` usages with a `recordLog` helper**

Replace the existing `logTechnicianAction(String type, String description)` method with a
helper that both updates the in-memory ATM log and persists it:
```java
    /** Records a maintenance log both in the ATM object and the database. */
    private static void recordLog(String type, String description) {
        String technicianId = currentTechnician != null ? currentTechnician.getTechnicianID() : "SYSTEM";
        MaintenanceLog log = new MaintenanceLog(
                "LOG-" + System.currentTimeMillis(), type, description, technicianId, ATM_ID);
        currentATM.addMaintenanceLog(log);
        logDAO.insert(log);
    }
```

Then replace every call to `logTechnicianAction(` in the file with `recordLog(`.

- [ ] **Step 7: Compile (menus still reference old persistence — fixed next tasks, but it must build)**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q compile`
Expected: BUILD SUCCESS. If the compiler reports an unused/old reference to `technicians`,
`customers`, or `logTechnicianAction`, remove that leftover — those symbols no longer exist.

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat: wire ATMSystem startup and authentication to PostgreSQL"
```

---

## Task 15: Persist withdraw and deposit + record transactions

**Files:**
- Modify: `src/main/java/ATMSystem.java`

- [ ] **Step 1: Update `withdrawCash` to persist the balance and record the transaction**

Replace `withdrawCash()` with:
```java
    private static void withdrawCash() {
        System.out.println("\n--- WITHDRAW CASH ---");
        Account account = selectAccount();
        if (account == null) {
            return;
        }
        System.out.print("Enter amount to withdraw: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (currentATM.getCashInventory() < amount) {
                System.out.println("❌ ATM has insufficient funds to fulfill this withdrawal.");
                return;
            }
            if (account.withdraw(amount)) {
                if (currentATM.dispenseCash(amount)) {
                    accountDAO.updateBalance(account.getAccountNumber(), account.getBalance());
                    atmDAO.update(currentATM);
                    transactionDAO.insert("WITHDRAWAL", account.getAccountNumber(), amount,
                            account.getBalance(), null, ATM_ID);
                    System.out.println("\n✓ Please collect your cash");
                    printReceipt("Withdrawal", account, amount, account.getBalance());
                } else {
                    account.deposit(amount); // rollback in-memory
                    System.out.println("❌ ATM Hardware failure during dispense. Rolled back.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        }
    }
```

- [ ] **Step 2: Update `depositFunds` to persist the balance and record the transaction**

Replace `depositFunds()` with:
```java
    private static void depositFunds() {
        System.out.println("\n--- DEPOSIT FUNDS ---");
        Account account = selectAccount();
        if (account == null) {
            return;
        }
        System.out.print("Enter amount to deposit: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (account.deposit(amount)) {
                currentATM.acceptCash(amount);
                accountDAO.updateBalance(account.getAccountNumber(), account.getBalance());
                atmDAO.update(currentATM);
                transactionDAO.insert("DEPOSIT", account.getAccountNumber(), amount,
                        account.getBalance(), null, ATM_ID);
                printReceipt("Deposit", account, amount, account.getBalance());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        }
    }
```

- [ ] **Step 3: Compile**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: persist withdraw/deposit balances and record transactions"
```

---

## Task 16: Persist transfers atomically

**Files:**
- Modify: `src/main/java/ATMSystem.java`

- [ ] **Step 1: Update `transferFunds` to delegate the money movement to the atomic DAO transfer**

Replace `transferFunds()` with:
```java
    private static void transferFunds() {
        System.out.println("\n--- TRANSFER FUNDS ---");
        System.out.println("Select source account:");
        Account sourceAccount = selectAccount();
        if (sourceAccount == null) {
            return;
        }
        System.out.println("\nSelect destination account:");
        Account destinationAccount = selectAccount();
        if (destinationAccount == null) {
            return;
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            System.out.println("❌ Cannot transfer to the same account");
            return;
        }
        System.out.print("Enter amount to transfer: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (amount <= 0) {
                System.out.println("❌ Invalid transfer amount");
                return;
            }
            if (amount > sourceAccount.getBalance()) {
                System.out.println("❌ Insufficient funds for transfer");
                return;
            }
            accountDAO.transfer(sourceAccount.getAccountNumber(),
                    destinationAccount.getAccountNumber(), amount, ATM_ID);
            // Refresh in-memory balances from the persisted truth
            double newSourceBalance = accountDAO.findByNumber(sourceAccount.getAccountNumber()).getBalance();
            sourceAccount.updateBalance(newSourceBalance - sourceAccount.getBalance());
            System.out.println("✓ Transfer successful");
            printReceipt("Transfer", sourceAccount, amount, newSourceBalance);
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        } catch (RuntimeException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }
```

- [ ] **Step 2: Compile**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: persist transfers atomically via AccountDAO.transfer"
```

---

## Task 17: Persist technician ATM actions + read logs from DB

**Files:**
- Modify: `src/main/java/ATMSystem.java`

- [ ] **Step 1: Persist ATM state after each technician action and read logs from the DB**

Replace `showTechnicianMenu()` with:
```java
    private static void showTechnicianMenu() {
        boolean sessionActive = true;
        while (sessionActive) {
            System.out.println("\n========== TECHNICIAN MENU ==========");
            System.out.println("1. Run Diagnostics");
            System.out.println("2. Replenish Cash Inventory");
            System.out.println("3. Replenish Ink and Paper");
            System.out.println("4. Upgrade Firmware/Software");
            System.out.println("5. View Maintenance Logs");
            System.out.println("6. Exit");
            System.out.println("=====================================");
            System.out.print("Select action: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    currentATM.runSelfDiagnostics();
                    recordLog("DIAGNOSTICS", "Ran ATM self-diagnostics.");
                    break;
                case "2":
                    handleCashReplenishment();
                    break;
                case "3":
                    currentATM.replenishInkAndPaper();
                    atmDAO.update(currentATM);
                    recordLog("REPLENISH", "Replenished Ink and Paper levels.");
                    break;
                case "4":
                    handleSystemUpgrade();
                    break;
                case "5":
                    printMaintenanceLogs();
                    break;
                case "6":
                    sessionActive = false;
                    recordLog("LOGOUT", "Technician logged out.");
                    System.out.println("\n✓ Technician Session ended.");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
        }
    }

    /** Prints maintenance logs read back from the database. */
    private static void printMaintenanceLogs() {
        List<MaintenanceLog> logs = logDAO.findByAtmId(ATM_ID);
        if (logs.isEmpty()) {
            System.out.println("No maintenance logs found.");
            return;
        }
        System.out.println("\n--- MAINTENANCE LOGS ---");
        for (MaintenanceLog log : logs) {
            System.out.println(log.toString());
        }
        System.out.println("------------------------");
    }
```

- [ ] **Step 2: Persist cash + upgrades in their handlers**

Replace `handleCashReplenishment()` and `handleSystemUpgrade()` with:
```java
    private static void handleCashReplenishment() {
        System.out.print("Enter amount £ to replenish: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            currentATM.replenishCash(amount);
            atmDAO.update(currentATM);
            recordLog("CASH_REPLENISH", "Added £" + amount + " to ATM Inventory.");
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount format.");
        }
    }

    private static void handleSystemUpgrade() {
        System.out.println("Upgrade Type: 1. Firmware, 2. Software");
        System.out.print("Select: ");
        String typeChoice = scanner.nextLine();
        String typeName = typeChoice.equals("1") ? "Firmware" : typeChoice.equals("2") ? "Software" : null;
        if (typeName == null) {
            System.out.println("❌ Invalid selection");
            return;
        }
        System.out.print("Enter new version string (e.g., v2.1.0): ");
        String version = scanner.nextLine();
        currentATM.applyUpgrade(typeName, version);
        atmDAO.update(currentATM);
        recordLog("SYSTEM_UPGRADE", "Upgraded " + typeName + " to " + version);
    }
```

- [ ] **Step 3: Confirm `checkBalance`, `selectAccount`, `printReceipt` are unchanged and compile**

These three methods do not touch persistence (balances are already current on the loaded
account) and remain as in the original file.

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Run the full test suite**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q test`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: persist technician ATM actions and read logs from DB"
```

---

## Task 18: End-to-end verification (persistence across restart)

**Files:** none (manual verification + packaging)

- [ ] **Step 1: Reset the app database to a clean state for the demo**

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d atm_system -c \
  "DROP TABLE IF EXISTS transactions, maintenance_logs, cards, accounts, technicians, atms, customers CASCADE;"
```

- [ ] **Step 2: Build the runnable jar**

Run: `cd /home/zakaria/Bureau/atm_system && mvn -q package`
Expected: BUILD SUCCESS; `target/atm-system.jar` produced.

- [ ] **Step 3: First run — withdraw, then exit (feed input via heredoc)**

```bash
cd /home/zakaria/Bureau/atm_system
printf '1234567890123456\n1234\n2\n1\n100\n5\nexit\n' | java -jar target/atm-system.jar
```
Expected: seeds on first launch; customer authenticates; withdraw £100 from Savings;
receipt shows balance £4900.00; session ends.

- [ ] **Step 4: Verify the balance and transaction persisted in PostgreSQL**

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d atm_system -c \
  "SELECT account_number, balance FROM accounts WHERE account_number='ACC001';"
PGPASSWORD=postgres psql -h localhost -U postgres -d atm_system -c \
  "SELECT type, amount, balance_after FROM transactions ORDER BY transaction_id;"
```
Expected: `ACC001` balance is `4900.00`; one `WITHDRAWAL` row for `100.00` with `balance_after` `4900.00`.

- [ ] **Step 5: Second run — check balance reflects persisted state (no re-seed)**

```bash
cd /home/zakaria/Bureau/atm_system
printf '1234567890123456\n1234\n1\n1\n5\nexit\n' | java -jar target/atm-system.jar
```
Expected: NO "seeds on first launch" effect (data already present); Check Balance on Savings
shows £4900.00 — confirming persistence survived the restart.

- [ ] **Step 6: Commit any final docs/notes**

```bash
git add -A && git commit -m "docs: verified end-to-end PostgreSQL persistence" --allow-empty
```

---

## Self-Review

**Spec coverage:**
- Maven build + JDBC driver → Task 1 ✓
- `db.properties` config → Task 2 ✓
- Salted SHA-256 PINs → Task 3 (`PinHasher`), enforced in Tasks 8/9 ✓
- 7-table schema, auto-init on startup → Tasks 4, 14 ✓
- Seed-if-empty sample data → Task 13 ✓
- Models stay POJOs; persistence in DAOs → Tasks 5-12 ✓ (only additive accessors/constructor added to Card/Technician/ATM)
- New `transactions` table + recording on every withdraw/deposit/transfer → Tasks 12, 15, 16 ✓
- Atomic transfer (commit/rollback) → Task 6 (`AccountDAO.transfer`), wired in Task 16 ✓
- ATM state + maintenance logs persisted; logs read from DB → Task 17 ✓
- Shared single connection → Task 2 ✓
- Startup DB-failure handling → Task 14 (`startup()` returns false with a clear message) ✓
- JUnit DAO integration tests + manual end-to-end run → Tasks 2-13 + Task 18 ✓

**Placeholder scan:** No TBD/TODO; every code step contains complete code; every command has expected output.

**Type consistency:** DAO constructors all take `Connection`. `verifyPin(id, pin)` signature consistent across `CardDAO`/`TechnicianDAO`. `TransactionDAO.insert(type, accountNumber, amount, balanceAfter, relatedAccount, atmId)` used identically in Tasks 6, 15, 16. `AccountDAO.transfer(from, to, amount, atmId)` consistent in Tasks 6 and 16. `recordLog(type, description)` replaces `logTechnicianAction` everywhere (Task 14 Step 6). `getPinForSeeding()` added to `Card` (Task 8) and `Technician` (Task 9) and used by their DAOs.

**Note on git:** Task 1 initializes a git repo so the commit cadence works (the project was not previously under version control).
