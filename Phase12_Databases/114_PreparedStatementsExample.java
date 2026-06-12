package phase12.databases;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PreparedStatements Example demonstrating:
 * - Parameterized queries
 * - Batch updates
 * - SQL injection prevention
 * - Retrieving generated keys
 *
 * NOTE: Requires a JDBC driver JAR on the classpath.
 * Uses H2 in-memory database if available; otherwise shows patterns.
 */
class PreparedStatementsExample {

    private static final String H2_URL = "jdbc:h2:mem:prepared_demo;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) {
        System.out.println("=== PreparedStatements Example ===\n");

        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
                runDemo(conn);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("H2 driver not available. Showing patterns with structured output.");
            showPatterns();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            showPatterns();
        }
    }

    private static void runDemo(Connection conn) throws SQLException {
        setupSchema(conn);

        // === 1. Basic PreparedStatement (parameterized INSERT) ===
        System.out.println("--- 1. Parameterized INSERT ---");
        String insertSQL = "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, "Laptop");
            pstmt.setDouble(2, 999.99);
            pstmt.setInt(3, 10);
            int rows = pstmt.executeUpdate();
            System.out.println("Inserted " + rows + " product.");

            pstmt.setString(1, "Mouse");
            pstmt.setDouble(2, 19.99);
            pstmt.setInt(3, 50);
            rows = pstmt.executeUpdate();
            System.out.println("Inserted " + rows + " product.");
        }

        // === 2. PreparedStatement with Generated Keys ===
        System.out.println("\n--- 2. INSERT with Generated Keys ---");
        String insertReturnKeys = "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertReturnKeys,
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Keyboard");
            pstmt.setDouble(2, 49.99);
            pstmt.setInt(3, 30);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Generated ID: " + keys.getInt(1));
                }
            }
        }

        // === 3. Parameterized SELECT ===
        System.out.println("\n--- 3. Parameterized SELECT (by price range) ---");
        String selectSQL = "SELECT id, name, price, quantity FROM products WHERE price BETWEEN ? AND ? ORDER BY price";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setDouble(1, 10.00);
            pstmt.setDouble(2, 100.00);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  id=%d, name=%s, price=$%.2f, qty=%d%n",
                            rs.getInt("id"), rs.getString("name"),
                            rs.getDouble("price"), rs.getInt("quantity"));
                }
            }
        }

        // === 4. SQL Injection Prevention ===
        System.out.println("\n--- 4. SQL Injection Prevention ---");
        String maliciousInput = "'; DROP TABLE products; --";
        // UNSAFE way (string concatenation) — DON'T DO THIS:
        // String unsafeSQL = "SELECT * FROM products WHERE name = '" + maliciousInput + "'";

        // SAFE way (parameterized query):
        String safeSQL = "SELECT COUNT(*) FROM products WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(safeSQL)) {
            pstmt.setString(1, maliciousInput);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                System.out.println("  Safe query executed (count=" + rs.getInt(1)
                        + "). No SQL injection possible.");
            }
        }

        // === 5. Batch Updates ===
        System.out.println("\n--- 5. Batch Updates ---");
        String batchSQL = "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(batchSQL)) {

            record ProductInput(String name, double price, int qty) {}

            var products = List.of(
                    new ProductInput("Monitor", 299.99, 15),
                    new ProductInput("Webcam", 89.99, 25),
                    new ProductInput("Microphone", 129.99, 20),
                    new ProductInput("Headphones", 79.99, 30),
                    new ProductInput("Speakers", 149.99, 10)
            );

            for (var product : products) {
                pstmt.setString(1, product.name());
                pstmt.setDouble(2, product.price());
                pstmt.setInt(3, product.qty());
                pstmt.addBatch();
            }

            int[] batchResults = pstmt.executeBatch();
            int totalInserted = Arrays.stream(batchResults).sum();
            System.out.println("  Batch inserted " + totalInserted + " rows ("
                    + batchResults.length + " statements).");

            // Verify total count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
                rs.next();
                System.out.println("  Total products in table: " + rs.getInt(1));
            }
        }

        // === 6. UPDATE with PreparedStatement ===
        System.out.println("\n--- 6. Parameterized UPDATE ---");
        String updateSQL = "UPDATE products SET price = price * (1 + ? / 100) WHERE quantity >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setDouble(1, 10.0); // 10% price increase
            pstmt.setInt(2, 20);      // for items with qty >= 20
            int updated = pstmt.executeUpdate();
            System.out.println("  Updated " + updated + " products (10% increase for qty>=20).");
        }

        // === 7. DELETE with PreparedStatement ===
        System.out.println("\n--- 7. Parameterized DELETE ---");
        String deleteSQL = "DELETE FROM products WHERE quantity = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, 0);
            int deleted = pstmt.executeUpdate();
            System.out.println("  Deleted " + deleted + " products with zero quantity.");
        }

        // Show final table state
        System.out.println("\n--- Final Product List ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, name, price, quantity FROM products ORDER BY id")) {
            System.out.printf("%-3s %-15s %-10s %-5s%n", "ID", "Name", "Price", "Qty");
            while (rs.next()) {
                System.out.printf("%-3d %-15s $%-8.2f %-5d%n",
                        rs.getInt("id"), rs.getString("name"),
                        rs.getDouble("price"), rs.getInt("quantity"));
            }
        }

        cleanup(conn);
    }

    private static void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        quantity INT DEFAULT 0
                    )
                    """);
        }
    }

    private static void cleanup(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    private static void showPatterns() {
        System.out.println("""
                === PreparedStatement Patterns ===
                
                1. Parameterized INSERT:
                   PreparedStatement pstmt = conn.prepareStatement("INSERT INTO t (col1, col2) VALUES (?, ?)");
                   pstmt.setString(1, value1);
                   pstmt.setInt(2, value2);
                   pstmt.executeUpdate();
                
                2. Parameterized SELECT:
                   PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM t WHERE col = ?");
                   pstmt.setString(1, value);
                   ResultSet rs = pstmt.executeQuery();
                
                3. Batch Updates:
                   pstmt.setString(1, "a"); pstmt.setInt(2, 1); pstmt.addBatch();
                   pstmt.setString(1, "b"); pstmt.setInt(2, 2); pstmt.addBatch();
                   int[] results = pstmt.executeBatch();
                
                4. SQL Injection Prevention:
                   // BAD:  String sql = "SELECT * FROM users WHERE name = '" + userInput + "'";
                   // GOOD: PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
                   //       pstmt.setString(1, userInput);
                   //       ResultSet rs = pstmt.executeQuery();
                
                5. Generated Keys:
                   PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                   pstmt.executeUpdate();
                   ResultSet keys = pstmt.getGeneratedKeys();
                   if (keys.next()) { int id = keys.getInt(1); }
                
                6. Type-Safe Setters:
                   setString(), setInt(), setDouble(), setLong(),
                   setDate(), setTimestamp(), setBoolean(), setObject(), etc.
                
                7. Batch Performance:
                   executeBatch() sends all statements in one round-trip to the DB,
                   much faster than individual executeUpdate() calls.
                """);
        System.out.println("(Code compiled successfully with JDK-only APIs.)");
        System.out.println("To run with actual database: add h2-*.jar to classpath.");
    }
}
