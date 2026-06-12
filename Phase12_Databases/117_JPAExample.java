package phase12.databases;

import java.lang.annotation.*;
import java.util.*;
import java.util.stream.*;

/**
 * JPA (Jakarta Persistence API) Example demonstrating:
 * - JPA annotations (@Entity, @Id, @GeneratedValue, @Column, etc.)
 * - EntityManager patterns (persist, find, merge, remove, createQuery)
 * - JPQL (Java Persistence Query Language)
 * - Persistence Unit / persistence.xml concept
 * - Relationships (@OneToMany, @ManyToOne)
 *
 * NOTE: This file compiles with JDK-only APIs. At runtime, it would require
 * a JPA provider (Hibernate, EclipseLink, etc.) and a JDBC driver.
 * This implementation provides a working in-memory simulation of JPA concepts.
 */
class JPAExample {

    // ============================================================
    // JPA Annotations
    // ============================================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Entity {
        String name() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Id {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface GeneratedValue {
        Strategy strategy() default Strategy.AUTO;
        enum Strategy { TABLE, SEQUENCE, IDENTITY, AUTO }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Column {
        String name() default "";
        boolean nullable() default true;
        boolean unique() default false;
        int length() default 255;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface OneToMany {
        String mappedBy() default "";
        CascadeType cascade() default CascadeType.NONE;
        FetchType fetch() default FetchType.LAZY;
        boolean orphanRemoval() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ManyToOne {
        FetchType fetch() default FetchType.EAGER;
        CascadeType cascade() default CascadeType.NONE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface JoinColumn {
        String name();
        boolean nullable() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Temporal {
        TemporalType value();
        enum TemporalType { DATE, TIME, TIMESTAMP }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Enumerated {
        EnumType value() default EnumType.ORDINAL;
        enum EnumType { ORDINAL, STRING }
    }

    enum FetchType { LAZY, EAGER }
    enum CascadeType { ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH, NONE }

    // ============================================================
    // JPA Entities
    // ============================================================

    @Entity(name = "Product")
    public static class Product {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "product_name", nullable = false, length = 200)
        private String name;

        @Column(nullable = false)
        private Double price;

        @Column(name = "qty_in_stock")
        private Integer quantity;

        @Column(unique = true, length = 50)
        private String sku;

        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<OrderItem> orderItems = new ArrayList<>();

        public Product() {}

        public Product(String name, Double price, Integer quantity, String sku) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.sku = sku;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public List<OrderItem> getOrderItems() { return orderItems; }
        public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

        @Override
        public String toString() {
            return "Product{id=" + id + ", name='" + name + "', price=" + price
                    + ", qty=" + quantity + ", sku='" + sku + "'}";
        }
    }

    @Entity(name = "Customer")
    public static class Customer {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "full_name", nullable = false, length = 150)
        private String name;

        @Column(unique = true, length = 200)
        private String email;

        @Column(name = "phone", length = 20)
        private String phone;

        @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Order> orders = new ArrayList<>();

        public Customer() {}

        public Customer(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public List<Order> getOrders() { return orders; }
        public void setOrders(List<Order> orders) { this.orders = orders; }

        public void addOrder(Order order) {
            orders.add(order);
            order.setCustomer(this);
        }

        @Override
        public String toString() {
            return "Customer{id=" + id + ", name='" + name + "', email='" + email
                    + "', phone='" + phone + "', orders=" + orders.size() + "}";
        }
    }

    @Entity(name = "Order")
    public static class Order {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "order_date")
        private String orderDate;

        @Column(name = "status", length = 20)
        @Enumerated(Enumerated.EnumType.STRING)
        private OrderStatus status;

        @Column(name = "total_amount")
        private Double totalAmount;

        @ManyToOne(cascade = CascadeType.MERGE)
        @JoinColumn(name = "customer_id")
        private Customer customer;

        @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<OrderItem> items = new ArrayList<>();

        public enum OrderStatus { PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED }

        public Order() {}

        public Order(String orderDate, OrderStatus status) {
            this.orderDate = orderDate;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public Customer getCustomer() { return customer; }
        public void setCustomer(Customer customer) { this.customer = customer; }
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }

        public void addItem(OrderItem item) {
            items.add(item);
            item.setOrder(this);
            recalculateTotal();
        }

        public void recalculateTotal() {
            this.totalAmount = items.stream()
                    .mapToDouble(OrderItem::getSubtotal)
                    .sum();
        }

        @Override
        public String toString() {
            return "Order{id=" + id + ", date='" + orderDate + "', status=" + status
                    + ", total=" + totalAmount + ", customer="
                    + (customer != null ? customer.getName() : null) + ", items=" + items.size() + "}";
        }
    }

    @Entity(name = "OrderItem")
    public static class OrderItem {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private Integer quantity;

        @Column(name = "unit_price")
        private Double unitPrice;

        @ManyToOne
        @JoinColumn(name = "order_id")
        private Order order;

        @ManyToOne
        @JoinColumn(name = "product_id")
        private Product product;

        public OrderItem() {}

        public OrderItem(Integer quantity, Double unitPrice) {
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
        public double getSubtotal() { return quantity * unitPrice; }
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) {
            this.product = product;
            if (product != null) this.unitPrice = product.getPrice();
        }

        @Override
        public String toString() {
            return "OrderItem{id=" + id + ", qty=" + quantity + ", price=" + unitPrice
                    + ", product=" + (product != null ? product.getName() : null) + "}";
        }
    }

    // ============================================================
    // JPA EntityManager Simulation
    // ============================================================

    interface EntityManagerInterface extends AutoCloseable {
        <T> void persist(T entity);
        <T> Optional<T> find(Class<T> entityClass, Object primaryKey);
        <T> T merge(T entity);
        <T> void remove(T entity);
        void flush();
        void clear();
        boolean contains(Object entity);
        void detach(Object entity);
        TypedQuery createQuery(String jpql, Class<?> resultClass);
        Query createQuery(String jpql);
        EntityTransaction getTransaction();
        void close();
    }

    interface EntityTransaction {
        void begin();
        void commit();
        void rollback();
        boolean isActive();
    }

    interface Query {
        List<?> getResultList();
        Object getSingleResult();
        Query setParameter(String name, Object value);
        Query setFirstResult(int start);
        Query setMaxResults(int max);
    }

    interface TypedQuery<X> extends Query {
        List<X> getResultList();
        X getSingleResult();
    }

    // EntityManagerFactory concept
    static class EntityManagerFactory {
        private final String persistenceUnitName;

        public EntityManagerFactory(String persistenceUnitName) {
            this.persistenceUnitName = persistenceUnitName;
        }

        public EntityManagerInterface createEntityManager() {
            return new EntityManagerImpl();
        }

        public void close() {
            System.out.println("EntityManagerFactory closed.");
        }

        @Override
        public String toString() {
            return "EntityManagerFactory{pu='" + persistenceUnitName + "'}";
        }
    }

    // Persistence class (simulated)
    static class Persistence {
        static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
            System.out.println("  JPA: Creating EntityManagerFactory for persistence unit: "
                    + persistenceUnitName);
            System.out.println("  JPA: Loading META-INF/persistence.xml");
            System.out.println("  JPA: Scanning for entities...");
            return new EntityManagerFactory(persistenceUnitName);
        }
    }

    // ============================================================
    // In-Memory JPA Implementation
    // ============================================================

    static class EntityManagerImpl implements EntityManagerInterface {
        private final JpaDatabase db = new JpaDatabase();
        private final Set<Object> managedEntities = new LinkedHashSet<>();
        private final JpaTransaction currentTransaction = new JpaTransaction();
        private boolean closed = false;

        @Override
        public <T> void persist(T entity) {
            checkOpen();
            System.out.println("  JPA: persist(" + entity.getClass().getSimpleName() + ")");
            db.insert(entity);
            managedEntities.add(entity);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> find(Class<T> entityClass, Object primaryKey) {
            checkOpen();
            System.out.println("  JPA: find(" + entityClass.getSimpleName() + ", " + primaryKey + ")");

            // Check managed entities first
            for (Object entity : managedEntities) {
                if (entityClass.isInstance(entity)) {
                    Object id = getId(entity);
                    if (primaryKey.equals(id)) {
                        return Optional.of((T) entity);
                    }
                }
            }
            return (Optional<T>) db.find(entityClass, primaryKey);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T merge(T entity) {
            checkOpen();
            System.out.println("  JPA: merge(" + entity.getClass().getSimpleName() + ")");
            managedEntities.add(entity);
            return (T) db.update(entity);
        }

        @Override
        public <T> void remove(T entity) {
            checkOpen();
            System.out.println("  JPA: remove(" + entity.getClass().getSimpleName() + ")");
            db.delete(entity);
            managedEntities.remove(entity);
        }

        @Override
        public void flush() {
            checkOpen();
            System.out.println("  JPA: flush() - synchronizing with database");
        }

        @Override
        public void clear() {
            checkOpen();
            System.out.println("  JPA: clear() - clearing persistence context");
            managedEntities.clear();
        }

        @Override
        public boolean contains(Object entity) {
            checkOpen();
            return managedEntities.contains(entity);
        }

        @Override
        public void detach(Object entity) {
            checkOpen();
            System.out.println("  JPA: detach(" + entity.getClass().getSimpleName() + ")");
            managedEntities.remove(entity);
        }

        @Override
        public EntityTransaction getTransaction() {
            return currentTransaction;
        }

        @Override
        public Query createQuery(String jpql) {
            checkOpen();
            System.out.println("  JPA: createQuery(\"" + jpql + "\")");
            return new JpaQuery(db, jpql);
        }

        @Override
        public TypedQuery createQuery(String jpql, Class<?> resultClass) {
            checkOpen();
            System.out.println("  JPA: createQuery(\"" + jpql + "\", " + resultClass.getSimpleName() + ")");
            return new JpaTypedQuery<>(db, jpql, resultClass);
        }

        @Override
        public void close() {
            this.closed = true;
            System.out.println("  JPA: EntityManager closed");
        }

        private void checkOpen() {
            if (closed) throw new IllegalStateException("EntityManager is closed");
        }

        private Object getId(Object entity) {
            try {
                for (var field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    static class JpaTransaction implements EntityTransaction {
        private boolean active = false;

        @Override
        public void begin() {
            active = true;
            System.out.println("  JPA: Transaction begun");
        }

        @Override
        public void commit() {
            active = false;
            System.out.println("  JPA: Transaction committed");
        }

        @Override
        public void rollback() {
            active = false;
            System.out.println("  JPA: Transaction rolled back");
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    static class JpaDatabase {
        private final Map<Class<?>, Map<Object, Object>> store = new LinkedHashMap<>();
        private final Map<Class<?>, Long> idGen = new LinkedHashMap<>();

        synchronized void insert(Object entity) {
            var map = store.computeIfAbsent(entity.getClass(), k -> new LinkedHashMap<>());
            try {
                var idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object id = idField.get(entity);
                if (id == null) {
                    long next = idGen.merge(entity.getClass(), 1L, (o, v) -> o + 1);
                    id = next;
                    idField.set(entity, next);
                }
                map.put(id, entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        synchronized Optional<?> find(Class<?> clazz, Object id) {
            var map = store.get(clazz);
            if (map == null) return Optional.empty();
            return Optional.ofNullable(map.get(id));
        }

        synchronized Object update(Object entity) {
            try {
                var idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object id = idField.get(entity);
                var map = store.get(entity.getClass());
                if (map != null && id != null) map.put(id, entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return entity;
        }

        synchronized void delete(Object entity) {
            try {
                var idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object id = idField.get(entity);
                var map = store.get(entity.getClass());
                if (map != null) map.remove(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        synchronized List<?> getAll(Class<?> clazz) {
            var map = store.get(clazz);
            if (map == null) return List.of();
            return List.copyOf(map.values());
        }

        private java.lang.reflect.Field getIdField(Class<?> clazz) {
            for (var f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) return f;
            }
            throw new RuntimeException("No @Id in " + clazz);
        }
    }

    static class JpaQuery implements Query {
        final JpaDatabase db;
        final String jpql;
        int firstResult = 0;
        int maxResults = Integer.MAX_VALUE;

        JpaQuery(JpaDatabase db, String jpql) {
            this.db = db;
            this.jpql = jpql;
        }

        @Override
        public List<?> getResultList() {
            return executeQuery().stream().skip(firstResult).limit(maxResults).toList();
        }

        @Override
        public Object getSingleResult() {
            var results = getResultList();
            if (results.isEmpty()) throw new RuntimeException("No result found");
            return results.getFirst();
        }

        @Override
        public Query setParameter(String name, Object value) {
            System.out.println("    JPQL param: " + name + " = " + value);
            return this;
        }

        @Override
        public Query setFirstResult(int start) { this.firstResult = start; return this; }

        @Override
        public Query setMaxResults(int max) { this.maxResults = max; return this; }

        List<?> executeQuery() {
            String lower = jpql.toLowerCase().trim();

            if (lower.startsWith("select p from product p")) return db.getAll(Product.class);
            if (lower.startsWith("select c from customer c")) return db.getAll(Customer.class);
            if (lower.startsWith("select o from order o")) return db.getAll(Order.class);
            if (lower.startsWith("from product")) return db.getAll(Product.class);
            if (lower.startsWith("from customer")) return db.getAll(Customer.class);
            if (lower.startsWith("from order")) return db.getAll(Order.class);
            if (lower.startsWith("from orderitem")) return db.getAll(OrderItem.class);

            if (lower.contains("where p.price >")) {
                // Parse simple condition
                return db.getAll(Product.class).stream()
                        .filter(e -> ((Product) e).getPrice() > 50.0)
                        .toList();
            }
            if (lower.contains("where o.status")) {
                return db.getAll(Order.class).stream()
                        .filter(e -> ((Order) e).getStatus() == Order.OrderStatus.PENDING)
                        .toList();
            }

            System.out.println("    (JPQL query simulated - returning all matching entities)");
            return db.getAll(detectEntityClass()) != null
                    ? db.getAll(detectEntityClass()) : List.of();
        }

        Class<?> detectEntityClass() {
            String upper = jpql.toUpperCase();
            if (upper.contains("PRODUCT")) return Product.class;
            if (upper.contains("CUSTOMER")) return Customer.class;
            if (upper.contains("ORDER")) return Order.class;
            if (upper.contains("ORDERITEM")) return OrderItem.class;
            return null;
        }
    }

    static class JpaTypedQuery<T> extends JpaQuery implements TypedQuery<T> {
        private final Class<T> resultClass;

        @SuppressWarnings("unchecked")
        JpaTypedQuery(JpaDatabase db, String jpql, Class<T> resultClass) {
            super(db, jpql);
            this.resultClass = resultClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<T> getResultList() {
            return (List<T>) super.getResultList();
        }

        @Override
        public T getSingleResult() {
            return (T) super.getSingleResult();
        }
    }

    // ============================================================
    // Main Demo
    // ============================================================

    public static void main(String[] args) {
        System.out.println("=== JPA (Jakarta Persistence) Example ===\n");

        // Create EntityManagerFactory from persistence unit
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("my-persistence-unit");
        System.out.println("  Factory: " + emf);

        EntityManagerInterface em = emf.createEntityManager();

        // === Transaction 1: Create Products ===
        System.out.println("\n--- Creating Products ---");
        EntityTransaction tx1 = em.getTransaction();
        tx1.begin();

        Product laptop = new Product("Gaming Laptop", 1499.99, 10, "LAP-001");
        Product phone = new Product("Smartphone", 799.99, 25, "PHN-001");
        Product headphones = new Product("Wireless Headphones", 199.99, 50, "HPH-001");
        Product mouse = new Product("Ergonomic Mouse", 49.99, 100, "MSE-001");

        em.persist(laptop);
        em.persist(phone);
        em.persist(headphones);
        em.persist(mouse);
        tx1.commit();

        // === Transaction 2: Create Customer with Order ===
        System.out.println("\n--- Creating Customer and Order ---");
        EntityTransaction tx2 = em.getTransaction();
        tx2.begin();

        Customer customer = new Customer("John Doe", "john@example.com", "+1-555-0100");
        em.persist(customer);

        // Create order with items
        Order order1 = new Order("2026-06-12", Order.OrderStatus.PENDING);

        // Find products and add as order items
        em.find(Product.class, 1L).ifPresent(p -> {
            OrderItem item = new OrderItem(1, p.getPrice());
            item.setProduct(p);
            order1.addItem(item);
        });

        em.find(Product.class, 3L).ifPresent(p -> {
            OrderItem item = new OrderItem(2, p.getPrice());
            item.setProduct(p);
            order1.addItem(item);
        });

        customer.addOrder(order1);
        em.persist(order1);
        tx2.commit();

        // === Transaction 3: Create another order ===
        System.out.println("\n--- Creating Second Order ---");
        EntityTransaction tx3 = em.getTransaction();
        tx3.begin();

        Order order2 = new Order("2026-06-12", Order.OrderStatus.PENDING);
        em.find(Product.class, 2L).ifPresent(p -> {
            OrderItem item = new OrderItem(1, p.getPrice());
            item.setProduct(p);
            order2.addItem(item);
        });
        em.find(Product.class, 4L).ifPresent(p -> {
            OrderItem item = new OrderItem(3, p.getPrice());
            item.setProduct(p);
            order2.addItem(item);
        });

        customer.addOrder(order2);
        em.persist(order2);
        tx3.commit();

        // === Query Operations ===
        System.out.println("\n--- JPQL Query Examples ---");

        // Find all products
        System.out.println("\n  JPQL: SELECT p FROM Product p");
        TypedQuery<Product> allProducts = em.createQuery("SELECT p FROM Product p", Product.class);
        allProducts.getResultList().forEach(p -> System.out.println("    " + p));

        // Find all customers
        System.out.println("\n  JPQL: SELECT c FROM Customer c");
        TypedQuery<Customer> allCustomers = em.createQuery("SELECT c FROM Customer c", Customer.class);
        allCustomers.getResultList().forEach(c -> System.out.println("    " + c));

        // Find orders
        System.out.println("\n  JPQL: SELECT o FROM Order o");
        TypedQuery<Order> allOrders = em.createQuery("SELECT o FROM Order o", Order.class);
        allOrders.getResultList().forEach(o -> System.out.println("    " + o));

        // Flush and clear
        System.out.println("\n--- EntityManager Lifecycle ---");
        em.flush();
        em.clear();

        // Contains check
        System.out.println("  Contains laptop after clear: " + em.contains(laptop));

        // Find again (re-loads from database)
        em.find(Product.class, 1L).ifPresent(p -> {
            System.out.println("  Re-found: " + p);
            System.out.println("  Contains after find: " + em.contains(p));
        });

        // Merge (update) product
        System.out.println("\n--- Merging (Updating) Product ---");
        EntityTransaction tx4 = em.getTransaction();
        tx4.begin();
        em.find(Product.class, 1L).ifPresent(p -> {
            p.setPrice(1399.99);
            em.merge(p);
            System.out.println("  Updated price: $" + p.getPrice());
        });
        tx4.commit();

        // Remove product
        System.out.println("\n--- Removing Product ---");
        EntityTransaction tx5 = em.getTransaction();
        tx5.begin();
        em.find(Product.class, 4L).ifPresent(p -> {
            System.out.println("  Removing: " + p);
            em.remove(p);
        });
        tx5.commit();

        em.close();
        emf.close();

        // === Summary ===
        System.out.println("\n=== JPA Concepts Summary ===");
        System.out.println("""
                Key JPA Concepts Demonstrated:
                
                1. @Entity(name="...")      - Marks POJO as JPA entity
                2. @Id                      - Primary key field
                3. @GeneratedValue(strategy) - ID generation strategy (IDENTITY, SEQUENCE, TABLE, AUTO)
                4. @Column(name, nullable, unique, length) - Column mapping
                5. @OneToMany(mappedBy, cascade, orphanRemoval) - One-to-many relationship
                6. @ManyToOne(fetch, cascade) - Many-to-one relationship  
                7. @JoinColumn(name)        - Foreign key column
                8. @Enumerated(STRING)      - Enum mapping
                9. @Temporal(DATE/TIME/TIMESTAMP) - Temporal type mapping
                10. EntityManager           - Persistence context / unit of work
                11. EntityTransaction       - ACID transaction management
                12. JPQL                    - Java Persistence Query Language
                13. Persistence Unit        - persistence.xml configuration
                14. Cascade Operations      - ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH
                15. Orphan Removal          - Auto-delete orphaned child entities
                """);
        System.out.println("(Compiles with JDK-only APIs. Requires JPA provider + JDBC driver at runtime.)");
    }
}
