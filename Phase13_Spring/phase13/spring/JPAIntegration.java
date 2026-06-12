package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// --- Demo: Spring Data JPA: @Repository, JpaRepository, @Query, @Modifying, @Transactional, derived query methods ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Repository {}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Transactional {
    boolean readOnly() default false;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Query {
    String value() default "";
    boolean nativeQuery() default false;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Modifying {}

// Entity annotation (JPA concept)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Entity {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Id {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface GeneratedValue {
    String strategy() default "AUTO";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Column {
    String name() default "";
    boolean nullable() default true;
    boolean unique() default false;
}

// Entity class
@Entity
record Product(@Id @GeneratedValue Long id,
               @Column(nullable = false) String name,
               @Column(nullable = false) double price,
               @Column(nullable = true) String category,
               int stock) {
    static Product of(String name, double price, String category, int stock) {
        return new Product(null, name, price, category, stock);
    }
}

// Simple JpaRepository - mimicking Spring Data JPA
interface JpaRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
    long count();
    boolean existsById(ID id);
    void deleteAll();
}

// In-memory implementation of JpaRepository
class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {
    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    private final java.util.function.Function<T, ID> idExtractor;

    public SimpleJpaRepository(java.util.function.Function<T, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public T save(T entity) {
        ID id = idExtractor.apply(entity);
        if (id == null) {
            // Generate ID (simplified)
            id = (ID) Long.valueOf(store.size() + 1L);
        }
        store.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}

// Product repository with derived query methods and custom @Query
@Repository
class ProductRepository extends SimpleJpaRepository<Product, Long> {

    public ProductRepository() {
        super(Product::id);
    }

    // Derived query method concept: find by name
    public Optional<Product> findByName(String name) {
        return store.values().stream()
                .filter(p -> p.name().equals(name))
                .findFirst();
    }

    // Derived query: find by category
    public List<Product> findByCategory(String category) {
        return store.values().stream()
                .filter(p -> category.equals(p.category()))
                .toList();
    }

    // Derived query: find by price less than
    public List<Product> findByPriceLessThan(double maxPrice) {
        return store.values().stream()
                .filter(p -> p.price() < maxPrice)
                .toList();
    }

    // Derived query: find by name containing (ignoring case)
    public List<Product> findByNameContainingIgnoreCase(String keyword) {
        return store.values().stream()
                .filter(p -> p.name().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    // Custom @Query annotation example (simulated JPQL)
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.price > :minPrice")
    public List<Product> findByCategoryAndPriceGreaterThan(String category, double minPrice) {
        return store.values().stream()
                .filter(p -> category.equals(p.category()) && p.price() > minPrice)
                .toList();
    }

    // @Modifying + @Query for update operations
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = :newPrice WHERE p.category = :category")
    public int updatePriceByCategory(String category, double newPrice) {
        int count = 0;
        for (var entry : store.entrySet()) {
            Product p = entry.getValue();
            if (category.equals(p.category())) {
                Product updated = new Product(p.id(), p.name(), newPrice, p.category(), p.stock());
                store.put(entry.getKey(), updated);
                count++;
            }
        }
        return count;
    }

    // Derived query: find by stock less than
    public List<Product> findByStockLessThan(int threshold) {
        return store.values().stream()
                .filter(p -> p.stock() < threshold)
                .toList();
    }

    // Derived query: count by category
    public long countByCategory(String category) {
        return store.values().stream()
                .filter(p -> category.equals(p.category()))
                .count();
    }

    // Derived query: exists by name
    public boolean existsByName(String name) {
        return store.values().stream().anyMatch(p -> p.name().equals(name));
    }
}

// Service layer with @Transactional
class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Product createProduct(Product product) {
        System.out.println("  [@Transactional] Creating product: " + product.name());
        return repository.save(product);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProduct(Long id) {
        System.out.println("  [@Transactional(readOnly = true)] Finding product by id: " + id);
        return repository.findById(id);
    }

    @Transactional
    public int applyCategoryDiscount(String category, double discountPercent) {
        System.out.println("  [@Modifying + @Transactional] Applying " + discountPercent + "% discount to " + category);
        var products = repository.findByCategory(category);
        for (var p : products) {
            double newPrice = p.price() * (1 - discountPercent / 100);
            repository.save(new Product(p.id(), p.name(), newPrice, p.category(), p.stock()));
        }
        return products.size();
    }

    @Transactional(readOnly = true)
    public void printInventoryReport() {
        System.out.println("\n  --- Inventory Report ---");
        System.out.println("  Total products: " + repository.count());
        System.out.println("  Low stock items: " + repository.findByStockLessThan(10).size());
        for (var p : repository.findAll()) {
            System.out.println("  " + p.id() + ": " + p.name()
                    + " | $" + String.format("%.2f", p.price())
                    + " | Category: " + p.category()
                    + " | Stock: " + p.stock());
        }
    }
}

public class JPAIntegration {
    public static void main(String[] args) {
        System.out.println("=== Spring Data JPA Demo ===");

        var repository = new ProductRepository();
        var service = new ProductService(repository);

        // Create products
        System.out.println("\n1. Save Entities (CRUD - Create):");
        service.createProduct(Product.of("Laptop", 1299.99, "Electronics", 25));
        service.createProduct(Product.of("Mouse", 29.99, "Electronics", 100));
        service.createProduct(Product.of("Keyboard", 89.99, "Electronics", 50));
        service.createProduct(Product.of("Desk Chair", 299.99, "Furniture", 15));
        service.createProduct(Product.of("Monitor", 449.99, "Electronics", 30));
        service.createProduct(Product.of("Notebook", 4.99, "Stationery", 500));
        service.createProduct(Product.of("Pen Set", 12.99, "Stationery", 200));

        // Derived query methods
        System.out.println("\n2. Derived Query Methods:");
        System.out.println("  findByName('Laptop'): " + repository.findByName("Laptop"));
        System.out.println("  findByCategory('Stationery'): " + repository.findByCategory("Stationery"));
        System.out.println("  findByPriceLessThan(50.0): " + repository.findByPriceLessThan(50.0));
        System.out.println("  findByNameContainingIgnoreCase('key'): " + repository.findByNameContainingIgnoreCase("key"));
        System.out.println("  existsByName('Mouse'): " + repository.existsByName("Mouse"));
        System.out.println("  countByCategory('Electronics'): " + repository.countByCategory("Electronics"));

        // Custom @Query
        System.out.println("\n3. Custom @Query (simulated JPQL):");
        System.out.println("  findByCategoryAndPriceGreaterThan('Electronics', 100): "
                + repository.findByCategoryAndPriceGreaterThan("Electronics", 100));

        // @Modifying + @Transactional
        System.out.println("\n4. @Modifying + @Transactional (Bulk Update):");
        int updated = service.applyCategoryDiscount("Electronics", 10);
        System.out.println("  Updated " + updated + " products with 10% discount");

        // Read-only transaction
        System.out.println("\n5. @Transactional(readOnly = true):");
        service.printInventoryReport();

        // CRUD operations
        System.out.println("\n6. CRUD Operations:");
        System.out.println("  Find by ID 1: " + repository.findById(1L));
        repository.deleteById(3L);
        System.out.println("  After deleting ID 3, count: " + repository.count());

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@Repository - marks class as a Spring Data JPA repository");
        System.out.println("JpaRepository<T, ID> - base interface with CRUD methods");
        System.out.println("@Query - custom JPQL/SQL queries");
        System.out.println("@Modifying - indicates a modifying query (needs @Transactional)");
        System.out.println("@Transactional(readOnly) - demarcates transaction boundaries");
        System.out.println("Derived query methods - findByXxx, countByXxx, existsByXxx");
    }
}
