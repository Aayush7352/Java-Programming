package phase12.databases;

import java.sql.*;
import java.util.*;

/**
 * JDBC Example demonstrating core JDBC operations.
 *
 * NOTE: To compile and run this file, you need a JDBC driver JAR for your database
 * (e.g., PostgreSQL: postgresql-42.x.x.jar, MySQL: mysql-connector-j-8.x.x.jar).
 * Compile with: javac -cp ".:/path/to/driver.jar" JDBCExample.java
 * Run with:     java -cp ".:/path/to/driver.jar" phase12.databases.JDBCExample
 */
class JDBCExample {

    // Replace with your database URL, username, and password
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "password";

    public static void main(String[] args) {
        // Register the driver (not always required in JDBC 4+ with SPI)
        // DriverManager automatically loads drivers from classpath in JDBC 4+

        System.out.println("=== JDBC Example ===");
        System.out.println("Database URL: " + DB_URL);
        System.out.println("(This code is a template — requires a running database and driver JAR)\n");

        // 1. Get Connection
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("No database connection available (driver/database not configured).");
            System.out.println("--- Showing code structure only ---");
            return;
        }

        try (conn; Statement stmt = conn.createStatement()) {

            // 2. Create table
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS users (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(100) UNIQUE NOT NULL,
                        age INTEGER
                    )
                    """;
            stmt.executeUpdate(createTableSQL);
            System.out.println("Table 'users' created (or already exists).");

            // 3. INSERT using executeUpdate
            String insertSQL = """
                    INSERT INTO users (name, email, age) VALUES
                    ('Alice', 'alice@example.com', 30),
                    ('Bob', 'bob@example.com', 25),
                    ('Charlie', 'charlie@example.com', 35)
                    """;
            int inserted = stmt.executeUpdate(insertSQL);
            System.out.println("Inserted " + inserted + " rows.");

            // 4. SELECT using executeQuery and ResultSet
            String selectSQL = "SELECT id, name, email, age FROM users ORDER BY id";
            try (ResultSet rs = stmt.executeQuery(selectSQL)) {
                System.out.println("\n=== Users Table ===");
                System.out.printf("%-5s %-15s %-25s %-5s%n", "ID", "Name", "Email", "Age");
                System.out.println("-".repeat(55));
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    int age = rs.getInt("age");
                    System.out.printf("%-5d %-15s %-25s %-5d%n", id, name, email, age);
                }
            }

            // 5. UPDATE using executeUpdate
            String updateSQL = "UPDATE users SET age = ? WHERE name = ?";
            // In real code, use PreparedStatement — see PreparedStatementsExample.java
            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                pstmt.setInt(1, 31);
                pstmt.setString(2, "Alice");
                int updated = pstmt.executeUpdate();
                System.out.println("\nUpdated " + updated + " row(s) for Alice.");
            }

            // 6. DELETE using executeUpdate
            String deleteSQL = "DELETE FROM users WHERE name = 'Charlie'";
            int deleted = stmt.executeUpdate(deleteSQL);
            System.out.println("Deleted " + deleted + " row(s) for Charlie.");

            // 7. Scrollable ResultSet
            String scrollSQL = "SELECT id, name FROM users";
            try (Statement scrollStmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
                 ResultSet rs = scrollStmt.executeQuery(scrollSQL)) {

                rs.last();
                System.out.println("\nTotal rows in users table: " + rs.getRow());
                rs.first();
                System.out.println("First user: " + rs.getString("name"));
                rs.last();
                System.out.println("Last user: " + rs.getString("name"));
            }

            // 8. Metadata
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("\n=== Database Metadata ===");
            System.out.println("DB Product: " + metaData.getDatabaseProductName());
            System.out.println("DB Version: " + metaData.getDatabaseProductVersion());
            System.out.println("Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion());

            // 9. Drop table (cleanup)
            stmt.executeUpdate("DROP TABLE IF EXISTS users");
            System.out.println("\nTable 'users' dropped (cleanup).");

        } catch (SQLException e) {
            System.out.println("\nSQL Error: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("\nNote: To run this example, ensure:");
            System.out.println("  1. A database is running on localhost:5432");
            System.out.println("  2. The JDBC driver JAR is on the classpath");
            System.out.println("  3. Database 'mydb' exists with user 'postgres'");
        }
    }

    /**
     * Attempts to establish a database connection.
     * Returns null if the driver is not available (so code structure still compiles).
     */
    private static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver not found in classpath.");
            return null;
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return null;
        }
    }
}
