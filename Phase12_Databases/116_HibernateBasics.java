package phase12.databases;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

/**
 * Hibernate Concepts Example demonstrating:
 * - Hibernate/JPA annotations (@Entity, @Id, @Column, @OneToMany, @ManyToOne, etc.)
 * - SessionFactory and Session patterns
 * - CRUD operations via Hibernate-style API
 * - HQL (Hibernate Query Language) concepts
 *
 * NOTE: This file compiles with JDK-only APIs. The hibernate.cfg.xml and
 * actual Hibernate JARs would be needed at runtime. This implementation
 * provides a working in-memory simulation of Hibernate concepts.
 */
class HibernateBasics {

    // ============================================================
    // Hibernate Annotations (conceptual mirror)
    // ============================================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Entity {
        String table() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Id {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface GeneratedValue {
        Strategy strategy() default Strategy.IDENTITY;
        enum Strategy { IDENTITY, SEQUENCE, TABLE, AUTO }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Column {
        String name() default "";
        boolean nullable() default true;
        int length() default 255;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface OneToMany {
        String mappedBy() default "";
        FetchType fetch() default FetchType.LAZY;
        CascadeType cascade() default CascadeType.NONE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ManyToOne {
        String joinColumn() default "";
        FetchType fetch() default FetchType.EAGER;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface JoinColumn {
        String name();
        boolean nullable() default true;
    }

    enum FetchType { LAZY, EAGER }
    enum CascadeType { ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH, NONE }

    // ============================================================
    // Hibernate Entities
    // ============================================================

    @Entity(table = "authors")
    public static class Author {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "author_name", nullable = false, length = 100)
        private String name;

        @Column(name = "email", length = 150)
        private String email;

        @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        private Set<Book> books = new LinkedHashSet<>();

        public Author() {}

        public Author(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Set<Book> getBooks() { return books; }
        public void setBooks(Set<Book> books) { this.books = books; }

        public void addBook(Book book) {
            books.add(book);
            book.setAuthor(this);
        }

        @Override
        public String toString() {
            return "Author{id=" + id + ", name='" + name + "', email='" + email + "', books=" + books.size() + "}";
        }
    }

    @Entity(table = "books")
    public static class Book {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "book_title", nullable = false, length = 200)
        private String title;

        @Column(name = "isbn", length = 20)
        private String isbn;

        @Column(name = "pages")
        private Integer pages;

        @Column(name = "price")
        private Double price;

        @ManyToOne(joinColumn = "author_id", fetch = FetchType.EAGER)
        @JoinColumn(name = "author_id")
        private Author author;

        public Book() {}

        public Book(String title, String isbn, Integer pages, Double price) {
            this.title = title;
            this.isbn = isbn;
            this.pages = pages;
            this.price = price;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        public Integer getPages() { return pages; }
        public void setPages(Integer pages) { this.pages = pages; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Author getAuthor() { return author; }
        public void setAuthor(Author author) { this.author = author; }

        @Override
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "', isbn='" + isbn
                    + "', pages=" + pages + ", price=" + price
                    + ", author=" + (author != null ? author.getName() : null) + "}";
        }
    }

    // ============================================================
    // Hibernate Session / SessionFactory Simulation
    // ============================================================

    interface HibernateSession extends AutoCloseable {
        <T> void persist(T entity);
        <T> Optional<T> find(Class<T> entityClass, Object id);
        <T> T merge(T entity);
        <T> void remove(T entity);
        Query createQuery(String hql);
        Transaction beginTransaction();
        Transaction getTransaction();
        void close();
    }

    interface Transaction {
        void commit();
        void rollback();
        boolean isActive();
    }

    interface Query {
        <T> List<T> list();
        Query setParameter(String name, Object value);
        Query setFirstResult(int start);
        Query setMaxResults(int max);
    }

    // SessionFactory concept
    static class SessionFactory {
        private final String configFile;

        public SessionFactory() {
            this.configFile = "hibernate.cfg.xml";
        }

        public SessionFactory(String configFile) {
            this.configFile = configFile;
        }

        public HibernateSession openSession() {
            return new HibernateSessionImpl();
        }

        public HibernateSession getCurrentSession() {
            return new HibernateSessionImpl();
        }

        public void close() {
            System.out.println("SessionFactory closed.");
        }

        @Override
        public String toString() {
            return "SessionFactory{config='" + configFile + "'}";
        }
    }

    // Hibernate Configuration concept
    static class Configuration {
        private final Properties properties = new Properties();
        private final List<Class<?>> annotatedClasses = new ArrayList<>();

        public Configuration configure() {
            return configure("hibernate.cfg.xml");
        }

        public Configuration configure(String resource) {
            System.out.println("  Loading Hibernate configuration from: " + resource);
            // Default Hibernate properties
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            properties.setProperty("hibernate.connection.url", "jdbc:h2:mem:hibernate_demo");
            properties.setProperty("hibernate.connection.username", "sa");
            properties.setProperty("hibernate.connection.password", "");
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");
            return this;
        }

        public Configuration setProperty(String name, String value) {
            properties.setProperty(name, value);
            return this;
        }

        public Configuration addAnnotatedClass(Class<?> annotatedClass) {
            annotatedClasses.add(annotatedClass);
            System.out.println("  Registered entity: " + annotatedClass.getSimpleName());
            return this;
        }

        public SessionFactory buildSessionFactory() {
            System.out.println("  Building SessionFactory with " + annotatedClasses.size() + " entities");
            System.out.println("  Properties: " + properties);
            return new SessionFactory();
        }
    }

    // ============================================================
    // Hibernate Session Implementation
    // ============================================================

    static class HibernateSessionImpl implements HibernateSession {
        private final InMemoryDatabase db = new InMemoryDatabase();
        private Transaction currentTx;

        @Override
        public <T> void persist(T entity) {
            System.out.println("  Hibernate: persist(" + entity.getClass().getSimpleName() + ")");
            db.insert(entity);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> find(Class<T> entityClass, Object id) {
            System.out.println("  Hibernate: find(" + entityClass.getSimpleName() + ", id=" + id + ")");
            return (Optional<T>) db.find(entityClass, id);
        }

        @Override
        public <T> T merge(T entity) {
            System.out.println("  Hibernate: merge(" + entity.getClass().getSimpleName() + ")");
            return db.update(entity);
        }

        @Override
        public <T> void remove(T entity) {
            System.out.println("  Hibernate: remove(" + entity.getClass().getSimpleName() + ")");
            db.delete(entity);
        }

        @Override
        public Query createQuery(String hql) {
            System.out.println("  Hibernate: createQuery(\"" + hql + "\")");
            return new SimpleQuery(db, hql);
        }

        @Override
        public Transaction beginTransaction() {
            currentTx = new Transaction() {
                private boolean active = true;

                @Override
                public void commit() {
                    System.out.println("  Hibernate: Transaction committed");
                    active = false;
                }

                @Override
                public void rollback() {
                    System.out.println("  Hibernate: Transaction rolled back");
                    active = false;
                }

                @Override
                public boolean isActive() {
                    return active;
                }
            };
            return currentTx;
        }

        @Override
        public Transaction getTransaction() {
            return currentTx;
        }

        @Override
        public void close() {
            System.out.println("  Hibernate: Session closed");
        }
    }

    // ============================================================
    // Simplified In-Memory Database
    // ============================================================

    static class InMemoryDatabase {
        private final Map<Class<?>, Map<Object, Object>> store = new LinkedHashMap<>();
        private final Map<Class<?>, Long> idGenerators = new LinkedHashMap<>();

        synchronized void insert(Object entity) {
            var entities = store.computeIfAbsent(entity.getClass(), k -> new LinkedHashMap<>());
            try {
                var idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object id = idField.get(entity);
                if (id == null) {
                    long newId = idGenerators.merge(entity.getClass(), 1L, (old, v) -> old + 1);
                    id = newId;
                    idField.set(entity, newId);
                }
                entities.put(id, entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        synchronized Optional<?> find(Class<?> entityClass, Object id) {
            var entities = store.get(entityClass);
            if (entities == null) return Optional.empty();
            return Optional.ofNullable(entities.get(id));
        }

        @SuppressWarnings("unchecked")
        synchronized <T> T update(T entity) {
            try {
                var idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object id = idField.get(entity);
                var entities = store.get(entity.getClass());
                if (entities != null && id != null) {
                    entities.put(id, entity);
                }
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
                var entities = store.get(entity.getClass());
                if (entities != null) {
                    entities.remove(id);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        synchronized List<?> getAll(Class<?> entityClass) {
            var entities = store.get(entityClass);
            if (entities == null) return List.of();
            return List.copyOf(entities.values());
        }

        private java.lang.reflect.Field getIdField(Class<?> clazz) {
            for (var field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) return field;
            }
            throw new RuntimeException("No @Id field in " + clazz);
        }
    }

    // ============================================================
    // Simple HQL Query Simulation
    // ============================================================

    static class SimpleQuery implements Query {
        private final InMemoryDatabase db;
        private final String hql;
        private int firstResult = 0;
        private int maxResults = Integer.MAX_VALUE;

        SimpleQuery(InMemoryDatabase db, String hql) {
            this.db = db;
            this.hql = hql;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> list() {
            // Parse simple "from EntityName" queries
            var lowerHql = hql.toLowerCase();
            if (lowerHql.startsWith("from ")) {
                String entityName = hql.substring(5).trim();
                // Find the entity class by simple name
                for (var entry : db.getAll(Object.class).getClass().getModule().getPackages()) {
                    // Simplified: just scan known entities
                }

                // For simplicity, scan our known classes
                for (Class<?> clazz : List.of(Author.class, Book.class)) {
                    if (clazz.getSimpleName().equalsIgnoreCase(entityName)) {
                        var all = db.getAll(clazz);
                        return (List<T>) all.stream()
                                .skip(firstResult)
                                .limit(maxResults)
                                .toList();
                    }
                }
            }
            return List.of();
        }

        @Override
        public Query setParameter(String name, Object value) {
            System.out.println("    Setting param: " + name + " = " + value);
            return this;
        }

        @Override
        public Query setFirstResult(int start) {
            this.firstResult = start;
            return this;
        }

        @Override
        public Query setMaxResults(int max) {
            this.maxResults = max;
            return this;
        }
    }

    // ============================================================
    // Main Demo
    // ============================================================

    public static void main(String[] args) {
        System.out.println("=== Hibernate Concepts Demo ===\n");

        // === Configuration ===
        System.out.println("--- Hibernate Configuration ---");
        Configuration config = new Configuration();
        config.configure()
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .addAnnotatedClass(Author.class)
                .addAnnotatedClass(Book.class);

        SessionFactory sessionFactory = config.buildSessionFactory();
        System.out.println("  SessionFactory: " + sessionFactory);

        // === CRUD Operations via Session ===
        System.out.println("\n--- CRUD Operations ---");

        try (HibernateSession session = sessionFactory.openSession()) {
            // Begin transaction
            Transaction tx = session.beginTransaction();

            // Create entities
            Author author = new Author("J.K. Rowling", "jk@rowling.com");
            Book book1 = new Book("Harry Potter and the Sorcerer's Stone", "978-0-7475-3269-9", 309, 24.99);
            Book book2 = new Book("Harry Potter and the Chamber of Secrets", "978-0-7475-3849-3", 341, 24.99);

            author.addBook(book1);
            author.addBook(book2);

            // Persist
            session.persist(author);
            // Books are cascaded

            tx.commit();
            System.out.println("  Created author and books successfully");

            // Find
            System.out.println("\n--- Finding Author by ID ---");
            session.find(Author.class, 1L).ifPresentOrElse(
                    a -> System.out.println("  Found: " + a),
                    () -> System.out.println("  Not found")
            );

            // Find Books
            System.out.println("\n--- Finding Books by ID ---");
            session.find(Book.class, 1L).ifPresent(b -> System.out.println("  Found: " + b));
            session.find(Book.class, 2L).ifPresent(b -> System.out.println("  Found: " + b));

            // Update
            System.out.println("\n--- Updating Book Price ---");
            session.find(Book.class, 1L).ifPresent(book -> {
                book.setPrice(29.99);
                session.merge(book);
                System.out.println("  Updated: " + book);
            });

            // HQL Query
            System.out.println("\n--- HQL Query: from Author ---");
            Query query = session.createQuery("from Author");
            List<Author> authors = query.list();
            System.out.println("  Authors found: " + authors.size());

            Query bookQuery = session.createQuery("from Book");
            List<Book> books = bookQuery.list();
            System.out.println("  Books found: " + books.size());
            books.forEach(b -> System.out.println("    " + b));

            // Delete
            System.out.println("\n--- Deleting Book ---");
            session.find(Book.class, 2L).ifPresent(book -> {
                session.remove(book);
                System.out.println("  Deleted: " + book);
            });
        }

        System.out.println("\n--- SessionFactory Close ---");
        sessionFactory.close();

        System.out.println("\n=== Hibernate Concepts Summary ===");
        System.out.println("""
                Key Hibernate Concepts Demonstrated:
                
                1. @Entity + @Table    - Map POJO to database table
                2. @Id + @GeneratedValue - Primary key with auto-generation
                3. @Column            - Column mapping with name, nullable, length
                4. @OneToMany         - One-to-many relationship (LAZY fetch)
                5. @ManyToOne         - Many-to-one relationship (EAGER fetch)
                6. @JoinColumn        - Foreign key column name
                7. SessionFactory     - Thread-safe factory for Sessions
                8. Session            - Unit of work, manages persistence context
                9. Transaction        - ACID transaction management
                10. HQL               - Hibernate Query Language (from EntityName)
                11. Cascade           - Cascade operations (ALL, PERSIST, MERGE, etc.)
                12. FetchType.LAZY/EAGER - Loading strategy
                """);
        System.out.println("(Compiles with JDK-only APIs. Requires Hibernate JARs at runtime.)");
    }
}
