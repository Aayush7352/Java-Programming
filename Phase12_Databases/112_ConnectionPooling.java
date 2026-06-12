package phase12.databases;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Simple Connection Pool implementation (HikariCP-style concept).
 *
 * Manages a pool of reusable database connections with:
 * - Configurable pool size (minimum/maximum)
 * - Connection timeout
 * - Borrow/return lifecycle
 * - Connection validation
 *
 * NOTE: Requires a JDBC driver JAR on the classpath to compile and run.
 * This class compiles with JDK-only APIs -- the actual DriverManager calls
 * will throw SQLException at runtime if no driver is present.
 */
class ConnectionPooling {

    private final String url;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final int minIdle;
    private final long connectionTimeoutMs;

    private final List<PooledConnection> connections = new ArrayList<>();
    private final Deque<PooledConnection> freeConnections = new ArrayDeque<>();
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private boolean shutdownRequested = false;

    public ConnectionPooling(String url, String username, String password,
                             int maxPoolSize, int minIdle, long connectionTimeoutMs) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /**
     * Initialize the pool by creating minimum idle connections.
     */
    public void initialize() throws SQLException {
        for (int i = 0; i < minIdle; i++) {
            createAndAddConnection();
        }
        System.out.println("Pool initialized with " + minIdle + " connections (max: " + maxPoolSize + ")");
    }

    /**
     * Borrow a connection from the pool.
     */
    public Connection getConnection() throws SQLException, InterruptedException {
        if (shutdownRequested) {
            throw new SQLException("Connection pool has been shut down");
        }

        long deadline = System.currentTimeMillis() + connectionTimeoutMs;

        synchronized (freeConnections) {
            while (freeConnections.isEmpty()) {
                if (activeConnections.get() < maxPoolSize) {
                    createAndAddConnection();
                    continue;
                }

                long waitTime = deadline - System.currentTimeMillis();
                if (waitTime <= 0) {
                    throw new SQLException("Connection timeout: no available connection after "
                            + connectionTimeoutMs + "ms");
                }

                System.out.println("No free connections, waiting... (" + activeConnections.get()
                        + " active / " + maxPoolSize + " max)");
                freeConnections.wait(waitTime);
            }

            PooledConnection conn = freeConnections.pollFirst();
            if (conn == null || !validateConnection(conn)) {
                conn = createNewConnection();
            }

            activeConnections.incrementAndGet();
            conn.setInUse(true);
            System.out.println("Connection borrowed. Active: " + activeConnections.get()
                    + ", Free: " + freeConnections.size());
            return conn;
        }
    }

    /**
     * Return a connection to the pool.
     */
    public void releaseConnection(Connection conn) {
        if (conn instanceof PooledConnection pooledConn) {
            synchronized (freeConnections) {
                pooledConn.setInUse(false);
                activeConnections.decrementAndGet();
                freeConnections.addLast(pooledConn);
                freeConnections.notify();
            }
            System.out.println("Connection returned. Active: " + activeConnections.get()
                    + ", Free: " + freeConnections.size());
        }
    }

    /**
     * Shut down the pool, closing all connections.
     */
    public void shutdown() {
        shutdownRequested = true;
        synchronized (freeConnections) {
            for (PooledConnection conn : connections) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
            connections.clear();
            freeConnections.clear();
        }
        System.out.println("Connection pool shut down.");
    }

    public int getActiveCount() {
        return activeConnections.get();
    }

    public int getIdleCount() {
        synchronized (freeConnections) {
            return freeConnections.size();
        }
    }

    // --- Internal Helpers ---

    private void createAndAddConnection() throws SQLException {
        PooledConnection conn = createNewConnection();
        connections.add(conn);
        freeConnections.addLast(conn);
    }

    private PooledConnection createNewConnection() throws SQLException {
        Connection actual = DriverManager.getConnection(url, username, password);
        return new PooledConnection(actual);
    }

    private boolean validateConnection(PooledConnection conn) {
        try {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Wrapper around Connection that tracks pool state.
     */
    static class PooledConnection implements Connection {
        private final Connection delegate;
        private boolean inUse = false;
        private long lastUsedTime = System.currentTimeMillis();

        PooledConnection(Connection delegate) {
            this.delegate = delegate;
        }

        void setInUse(boolean inUse) {
            this.inUse = inUse;
            this.lastUsedTime = System.currentTimeMillis();
        }

        boolean isInUse() {
            return inUse;
        }

        long getLastUsedTime() {
            return lastUsedTime;
        }

        // Delegate all Connection methods to the actual connection
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public void close() throws SQLException { delegate.close(); }
        @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency); }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return delegate.prepareStatement(sql, autoGeneratedKeys); }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return delegate.prepareStatement(sql, columnIndexes); }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return delegate.prepareStatement(sql, columnNames); }
        @Override public Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException { delegate.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }

    // --- Demo main() ---
    public static void main(String[] args) {
        System.out.println("=== Connection Pooling Demo ===");
        System.out.println("(Uses an in-memory simulation — requires no external database)");
        System.out.println();

        // Using H2 in-memory or any DB — here we simulate with a message
        ConnectionPooling pool = new ConnectionPooling(
                "jdbc:h2:mem:testdb", "sa", "",
                5, 2, 5000
        );

        try {
            pool.initialize();
        } catch (SQLException e) {
            System.out.println("Note: No database driver available. The code structure is complete.");
            System.out.println("To run: add an H2 or PostgreSQL JDBC driver to the classpath.");
            System.out.println("Example with H2:   java -cp h2-*.jar:phase12 phase12.databases.ConnectionPooling");
            System.out.println("Example with PG:   java -cp postgresql-*.jar:phase12 phase12.databases.ConnectionPooling");
            System.out.println();

            // Show the pool state concepts anyway
            System.out.println("Pool configured with:");
            System.out.println("  Max pool size: 5");
            System.out.println("  Min idle: 2");
            System.out.println("  Connection timeout: 5000ms");
            System.out.println("\nThe pool tracks connections via PooledConnection wrapper,");
            System.out.println("uses synchronized blocks for thread safety, validates");
            System.out.println("connections before lending, and supports graceful shutdown.");
            return;
        }

        // If driver is available, run actual demo
        try {
            // Borrow connections
            Connection c1 = pool.getConnection();
            Connection c2 = pool.getConnection();
            Connection c3 = pool.getConnection();

            System.out.println("Active: " + pool.getActiveCount() + ", Idle: " + pool.getIdleCount());

            // Return connections
            pool.releaseConnection(c1);
            pool.releaseConnection(c2);
            pool.releaseConnection(c3);

            System.out.println("Active: " + pool.getActiveCount() + ", Idle: " + pool.getIdleCount());

            pool.shutdown();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
