package db;

import models.ATM;
import models.Account;
import models.Card;
import models.Customer;
import models.Technician;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
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
