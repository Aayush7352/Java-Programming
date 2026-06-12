package phase12.databases;

import java.sql.*;

/**
 * Transactions Example demonstrating commit, rollback, savepoint,
 * and transaction isolation levels using JDBC.
 *
 * NOTE: Requires a JDBC driver JAR on the classpath to compile and run.
 * This example uses H2 in-memory database for self-contained execution.
 * If H2 is not available, the code shows the patterns with detailed comments.
 */
class TransactionsExample {

    // Try H2 in-memory first, fall back to showing the pattern
    private static final String H2_URL = "jdbc:h2:mem:transaction_demo;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) {
        System.out.println("=== Transactions Example ===");
        System.out.println("Attempting H2 in-memory database...\n");

        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
                runTransactionDemo(conn);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("H2 driver not available. Showing transaction patterns with comments.");
            System.out.println();
            showTransactionPatterns();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            showTransactionPatterns();
        }
    }

    private static void runTransactionDemo(Connection conn) throws SQLException {
        System.out.println("Connected to H2 in-memory database.\n");

        // Create test tables
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE accounts (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100),
                        balance DECIMAL(10,2)
                    )
                    """);
            stmt.execute("INSERT INTO accounts VALUES (1, 'Alice', 1000.00)");
            stmt.execute("INSERT INTO accounts VALUES (2, 'Bob', 500.00)");
            stmt.execute("""
                    CREATE TABLE audit_log (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        action VARCHAR(200),
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        // === 1. Basic Transaction: Commit ===
        System.out.println("--- 1. Basic Transaction with COMMIT ---");
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE id = ?")) {

            // Transfer $200 from Alice to Bob
            pstmt.setDouble(1, 200.00);
            pstmt.setInt(2, 1);
            pstmt.executeUpdate();

            pstmt.setDouble(1, 200.00);
            pstmt.setInt(2, 2);
            pstmt.executeUpdate();

            // Log the transfer
            try (PreparedStatement logStmt = conn.prepareStatement(
                    "INSERT INTO audit_log (action) VALUES (?)")) {
                logStmt.setString(1, "Transfer $200 from Alice to Bob");
                logStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Transfer committed successfully.");
        } catch (SQLException e) {
            conn.rollback();
            System.out.println("Transfer failed, rolled back: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }

        printBalances(conn);

        // === 2. Transaction with ROLLBACK ===
        System.out.println("\n--- 2. Transaction with ROLLBACK ---");
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE id = ?")) {

                pstmt.setDouble(1, 5000.00); // More than Alice has
                pstmt.setInt(2, 1);
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated == 0) {
                    throw new SQLException("Account not found");
                }

                // Check balance constraint (simulated)
                try (Statement check = conn.createStatement();
                     ResultSet rs = check.executeQuery(
                             "SELECT balance FROM accounts WHERE id = 1")) {
                    rs.next();
                    if (rs.getDouble("balance") < 0) {
                        throw new SQLException("Insufficient funds: balance would be negative");
                    }
                }

                conn.commit();
                System.out.println("Transaction committed.");
            }
        } catch (SQLException e) {
            System.out.println("Transaction failed: " + e.getMessage());
            conn.rollback();
            System.out.println("Transaction rolled back. Balances unchanged.");
        } finally {
            conn.setAutoCommit(true);
        }

        printBalances(conn);

        // === 3. Savepoints ===
        System.out.println("\n--- 3. Savepoints ---");
        conn.setAutoCommit(false);
        Savepoint sp1 = null;
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
                pstmt.setDouble(1, 100.00);
                pstmt.setInt(2, 1);
                pstmt.executeUpdate();

                sp1 = conn.setSavepoint("after_alice_credit");
                System.out.println("Savepoint 'after_alice_credit' created.");

                pstmt.setDouble(1, 100.00);
                pstmt.setInt(2, 2);
                pstmt.executeUpdate();

                // Simulate a problem and rollback to savepoint
                pstmt.setDouble(1, 99999.00);
                pstmt.setInt(2, 999); // Non-existent account
                int rows = pstmt.executeUpdate();
                if (rows == 0) {
                    System.out.println("Simulating error — rolling back to savepoint.");
                    conn.rollback(sp1);
                    System.out.println("Rolled back to savepoint. Bob's credit was undone, Alice's remains.");
                }

                conn.commit();
                System.out.println("Committed (with partial rollback to savepoint).");
            }
        } catch (SQLException e) {
            conn.rollback();
            System.out.println("Transaction rolled back entirely: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }

        printBalances(conn);

        // === 4. Transaction Isolation Levels ===
        System.out.println("\n--- 4. Transaction Isolation Levels ---");
        int[] levels = {
                Connection.TRANSACTION_READ_UNCOMMITTED,
                Connection.TRANSACTION_READ_COMMITTED,
                Connection.TRANSACTION_REPEATABLE_READ,
                Connection.TRANSACTION_SERIALIZABLE
        };
        String[] levelNames = {
                "READ_UNCOMMITTED",
                "READ_COMMITTED",
                "REPEATABLE_READ",
                "SERIALIZABLE"
        };

        for (int i = 0; i < levels.length; i++) {
            conn.setTransactionIsolation(levels[i]);
            System.out.println("  Isolation level set to: " + levelNames[i]
                    + " (level: " + levels[i] + ")");

            // Show what phenomena each level prevents
            System.out.println("    Dirty Read: "
                    + (levels[i] >= Connection.TRANSACTION_READ_COMMITTED ? "Prevented" : "Possible"));
            System.out.println("    Non-Repeatable Read: "
                    + (levels[i] >= Connection.TRANSACTION_REPEATABLE_READ ? "Prevented" : "Possible"));
            System.out.println("    Phantom Read: "
                    + (levels[i] >= Connection.TRANSACTION_SERIALIZABLE ? "Prevented" : "Possible"));
        }

        // Cleanup
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE accounts");
            stmt.execute("DROP TABLE audit_log");
        }
        System.out.println("\nDemo tables cleaned up.");
    }

    private static void printBalances(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, balance FROM accounts ORDER BY id")) {
            System.out.println("Current balances:");
            while (rs.next()) {
                System.out.printf("  %s (id=%d): $%.2f%n",
                        rs.getString("name"), rs.getInt("id"), rs.getDouble("balance"));
            }
        }
    }

    private static void showTransactionPatterns() {
        System.out.println("""
                === Transaction Patterns ===
                
                1. Basic Commit:
                   conn.setAutoCommit(false);
                   stmt.executeUpdate(...);  // DML operations
                   conn.commit();
                   conn.setAutoCommit(true);
                
                2. Rollback on Error:
                   conn.setAutoCommit(false);
                   try {
                       // multiple operations
                       conn.commit();
                   } catch (SQLException e) {
                       conn.rollback();
                   }
                
                3. Savepoints:
                   conn.setAutoCommit(false);
                   Savepoint sp = conn.setSavepoint("my_savepoint");
                   // ... operations ...
                   conn.rollback(sp);  // partial rollback
                   conn.commit();
                
                4. Isolation Levels (JDBC constants):
                   Connection.TRANSACTION_READ_UNCOMMITTED  = 1
                   Connection.TRANSACTION_READ_COMMITTED    = 2
                   Connection.TRANSACTION_REPEATABLE_READ   = 4
                   Connection.TRANSACTION_SERIALIZABLE       = 8
                
                5. Phenomena prevented by levels:
                   READ_UNCOMMITTED  - No prevention
                   READ_COMMITTED    - Prevents dirty reads
                   REPEATABLE_READ   - Prevents dirty + non-repeatable reads
                   SERIALIZABLE      - Prevents dirty + non-repeatable + phantom reads
                """);

        // Show compiled output exists
        System.out.println("(Code compiled successfully with JDK-only APIs.)");
        System.out.println("To run with actual database: add h2-*.jar or postgresql-*.jar to classpath.");
    }
}
