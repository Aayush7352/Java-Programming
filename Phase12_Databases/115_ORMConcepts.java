package phase12.databases;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * ORM Concepts Example demonstrating:
 * - POJO (Plain Old Java Object) with entity mapping
 * - Custom annotations (@Entity, @Table, @Id, @Column, @OneToMany, @ManyToOne)
 * - EntityManager-like pattern (persist, find, merge, remove)
 * - Relationship mapping simulation
 */
class ORMConcepts {

    // ============================================================
    // Custom ORM Annotations
    // ============================================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Entity {
        String table() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Table {
        String name();
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
        CascadeType cascade() default CascadeType.NONE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ManyToOne {
        String joinColumn() default "";
    }

    public enum CascadeType { ALL, PERSIST, MERGE, REMOVE, NONE }

    // ============================================================
    // POJO Entities
    // ============================================================

    @Entity(table = "departments")
    public static class Department {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "dept_name", nullable = false, length = 100)
        private String name;

        @Column(name = "location")
        private String location;

        @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
        private List<Employee> employees = new ArrayList<>();

        public Department() {}

        public Department(String name, String location) {
            this.name = name;
            this.location = location;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public List<Employee> getEmployees() { return employees; }
        public void setEmployees(List<Employee> employees) { this.employees = employees; }

        public void addEmployee(Employee emp) {
            employees.add(emp);
            emp.setDepartment(this);
        }

        @Override
        public String toString() {
            return "Department{id=" + id + ", name='" + name + "', location='" + location + "'}";
        }
    }

    @Entity(table = "employees")
    public static class Employee {
        @Id
        @GeneratedValue(strategy = GeneratedValue.Strategy.IDENTITY)
        private Long id;

        @Column(name = "emp_name", nullable = false, length = 150)
        private String name;

        @Column(name = "email", length = 200)
        private String email;

        @Column(name = "salary")
        private Double salary;

        @ManyToOne(joinColumn = "dept_id")
        private Department department;

        public Employee() {}

        public Employee(String name, String email, Double salary) {
            this.name = name;
            this.email = email;
            this.salary = salary;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Double getSalary() { return salary; }
        public void setSalary(Double salary) { this.salary = salary; }
        public Department getDepartment() { return department; }
        public void setDepartment(Department department) { this.department = department; }

        @Override
        public String toString() {
            return "Employee{id=" + id + ", name='" + name + "', email='" + email
                    + "', salary=" + salary + ", dept=" + (department != null ? department.getName() : null) + "}";
        }
    }

    // ============================================================
    // Simple ORM Metadata
    // ============================================================

    record EntityMetadata(
            Class<?> entityClass,
            String tableName,
            Field idField,
            Map<String, Field> columnFields,
            Map<String, String> columnNames,
            Map<Field, RelationshipMeta> relationships
    ) {}

    record RelationshipMeta(
            String type, // ONE_TO_MANY, MANY_TO_ONE
            String mappedBy,
            String joinColumn,
            Class<?> targetEntity,
            CascadeType cascade
    ) {}

    // ============================================================
    // EntityManager-like Pattern
    // ============================================================

    static class EntityManager {
        private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();
        private final Map<Class<?>, Map<Object, Object>> persistenceContext = new ConcurrentHashMap<>();
        private final Map<String, List<Map<String, Object>>> database = new ConcurrentHashMap<>();
        private final Map<String, Long> idSequences = new ConcurrentHashMap<>();

        // ========== Persist ==========
        public <T> void persist(T entity) {
            var meta = getMetadata(entity.getClass());
            try {
                // Generate ID if needed
                Field idField = meta.idField();
                GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
                if (gen != null && idField.get(entity) == null) {
                    if (gen.strategy() == GeneratedValue.Strategy.IDENTITY) {
                        long newId = getNextId(meta.tableName());
                        idField.setAccessible(true);
                        idField.set(entity, newId);
                    }
                }

                // Cascade persist OneToMany children
                for (var relEntry : meta.relationships().entrySet()) {
                    var field = relEntry.getKey();
                    var rel = relEntry.getValue();
                    if ("ONE_TO_MANY".equals(rel.type()) && rel.cascade() != CascadeType.NONE) {
                        field.setAccessible(true);
                        Collection<?> children = (Collection<?>) field.get(entity);
                        if (children != null) {
                            for (Object child : children) {
                                // Set the parent reference (mappedBy)
                                if (!rel.mappedBy().isEmpty()) {
                                    Field mappedField = child.getClass().getDeclaredField(rel.mappedBy());
                                    mappedField.setAccessible(true);
                                    mappedField.set(child, entity);
                                }
                                persist(child);
                            }
                        }
                    }
                }

                // Build column values
                Map<String, Object> row = new LinkedHashMap<>();
                for (var entry : meta.columnNames().entrySet()) {
                    String fieldName = entry.getKey();
                    String columnName = entry.getValue();
                    Field field = meta.columnFields().get(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(entity);

                    // Handle relationships
                    if (meta.relationships().containsKey(field)) {
                        var rel = meta.relationships().get(field);
                        if ("MANY_TO_ONE".equals(rel.type()) && value != null) {
                            // Store foreign key
                            var relatedMeta = getMetadata(value.getClass());
                            Field relatedIdField = relatedMeta.idField();
                            relatedIdField.setAccessible(true);
                            Object fkValue = relatedIdField.get(value);
                            row.put(rel.joinColumn(), fkValue);
                            continue;
                        }
                    }

                    if (!fieldName.equals(meta.idField().getName())) {
                        row.put(columnName, value);
                    }
                }

                // Add ID after other columns
                idField.setAccessible(true);
                row.put(meta.idField().getName(), idField.get(entity));

                database.computeIfAbsent(meta.tableName(), k -> new ArrayList<>()).add(row);
                addToContext(entity);
                System.out.println("  Persisted: " + entity);

            } catch (Exception e) {
                throw new RuntimeException("Failed to persist entity: " + e.getMessage(), e);
            }
        }

        // ========== Find ==========
        @SuppressWarnings("unchecked")
        public <T> Optional<T> find(Class<T> entityClass, Object id) {
            var meta = getMetadata(entityClass);

            // Check persistence context first
            var contextMap = persistenceContext.get(entityClass);
            if (contextMap != null && contextMap.containsKey(id)) {
                return Optional.of((T) contextMap.get(id));
            }

            // Query the in-memory database
            List<Map<String, Object>> rows = database.get(meta.tableName());
            if (rows == null) return Optional.empty();

            try {
                for (var row : rows) {
                    Object rowId = row.get(meta.idField().getName());
                    if (id.equals(rowId)) {
                        T entity = entityClass.getDeclaredConstructor().newInstance();

                        for (var entry : meta.columnNames().entrySet()) {
                            String fieldName = entry.getKey();
                            String columnName = entry.getValue();
                            Field field = meta.columnFields().get(fieldName);
                            field.setAccessible(true);

                            Object value = row.get(columnName);
                            if (value != null) {
                                field.set(entity, value);
                            }
                        }

                        // Handle relationships
                        for (var relEntry : meta.relationships().entrySet()) {
                            Field field = relEntry.getKey();
                            var rel = relEntry.getValue();
                            if ("MANY_TO_ONE".equals(rel.type())) {
                                Object fkValue = row.get(rel.joinColumn());
                                if (fkValue != null) {
                                    var relatedEntity = find(rel.targetEntity(), fkValue);
                                    relatedEntity.ifPresent(e -> {
                                        try {
                                            field.setAccessible(true);
                                            field.set(entity, e);
                                        } catch (Exception ignored) {}
                                    });
                                }
                            }
                        }

                        addToContext(entity);
                        return Optional.of(entity);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to find entity: " + e.getMessage(), e);
            }
            return Optional.empty();
        }

        // ========== Merge ==========
        public <T> T merge(T entity) {
            var meta = getMetadata(entity.getClass());
            try {
                Field idField = meta.idField();
                idField.setAccessible(true);
                Object id = idField.get(entity);

                // Find existing and update
                List<Map<String, Object>> rows = database.get(meta.tableName());
                if (rows != null) {
                    for (var row : rows) {
                        Object rowId = row.get(meta.idField().getName());
                        if (id.equals(rowId)) {
                            for (var entry : meta.columnNames().entrySet()) {
                                String fieldName = entry.getKey();
                                String columnName = entry.getValue();
                                Field field = meta.columnFields().get(fieldName);
                                field.setAccessible(true);
                                row.put(columnName, field.get(entity));
                            }
                            break;
                        }
                    }
                }
                System.out.println("  Merged: " + entity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to merge entity: " + e.getMessage(), e);
            }
            return entity;
        }

        // ========== Remove ==========
        public <T> void remove(T entity) {
            var meta = getMetadata(entity.getClass());
            try {
                Field idField = meta.idField();
                idField.setAccessible(true);
                Object id = idField.get(entity);

                List<Map<String, Object>> rows = database.get(meta.tableName());
                if (rows != null) {
                    rows.removeIf(row -> id.equals(row.get(meta.idField().getName())));
                }

                var contextMap = persistenceContext.get(entity.getClass());
                if (contextMap != null) contextMap.remove(id);
                System.out.println("  Removed: " + entity);

            } catch (Exception e) {
                throw new RuntimeException("Failed to remove entity: " + e.getMessage(), e);
            }
        }

        // ========== JPQL-like Query ==========
        @SuppressWarnings("unchecked")
        public <T> List<T> findAll(Class<T> entityClass) {
            var meta = getMetadata(entityClass);
            List<T> results = new ArrayList<>();
            List<Map<String, Object>> rows = database.get(meta.tableName());
            if (rows == null) return results;

            for (var row : rows) {
                Object id = row.get(meta.idField().getName());
                find(entityClass, id).ifPresent(results::add);
            }
            return results;
        }

        // ========== Internal ==========
        private <T> void addToContext(T entity) {
            try {
                var meta = getMetadata(entity.getClass());
                Field idField = meta.idField();
                idField.setAccessible(true);
                Object id = idField.get(entity);

                persistenceContext
                        .computeIfAbsent(entity.getClass(), k -> new ConcurrentHashMap<>())
                        .put(id, entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private long getNextId(String tableName) {
            return idSequences.merge(tableName, 1L, (old, v) -> old + 1);
        }

        private EntityMetadata getMetadata(Class<?> entityClass) {
            return metadataCache.computeIfAbsent(entityClass, this::buildMetadata);
        }

        private EntityMetadata buildMetadata(Class<?> clazz) {
            Entity entityAnn = clazz.getAnnotation(Entity.class);
            if (entityAnn == null) {
                throw new RuntimeException("Class " + clazz + " is not annotated with @Entity");
            }

            String tableName = entityAnn.table();
            if (tableName.isEmpty()) {
                Table tableAnn = clazz.getAnnotation(Table.class);
                if (tableAnn != null) tableName = tableAnn.name();
                if (tableName.isEmpty()) tableName = clazz.getSimpleName().toLowerCase();
            }

            Field idField = null;
            Map<String, Field> columnFields = new LinkedHashMap<>();
            Map<String, String> columnNames = new LinkedHashMap<>();
            Map<Field, RelationshipMeta> relationships = new LinkedHashMap<>();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                }

                if (field.isAnnotationPresent(OneToMany.class)) {
                    OneToMany otm = field.getAnnotation(OneToMany.class);
                    relationships.put(field, new RelationshipMeta(
                            "ONE_TO_MANY", otm.mappedBy(), null,
                            getGenericType(field), otm.cascade()
                    ));
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    ManyToOne mto = field.getAnnotation(ManyToOne.class);
                    relationships.put(field, new RelationshipMeta(
                            "MANY_TO_ONE", null, mto.joinColumn(),
                            field.getType(), CascadeType.NONE
                    ));
                } else {
                    columnFields.put(field.getName(), field);
                    Column colAnn = field.getAnnotation(Column.class);
                    String colName = (colAnn != null && !colAnn.name().isEmpty())
                            ? colAnn.name() : field.getName();
                    columnNames.put(field.getName(), colName);
                }
            }

            if (idField == null) {
                throw new RuntimeException("No @Id field found in " + clazz);
            }

            return new EntityMetadata(clazz, tableName, idField, columnFields, columnNames, relationships);
        }

        private Class<?> getGenericType(Field field) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            return (Class<?>) type.getActualTypeArguments()[0];
        }
    }

    // ============================================================
    // Main Demo
    // ============================================================

    public static void main(String[] args) {
        System.out.println("=== ORM Concepts (Custom ORM Implementation) ===\n");

        EntityManager em = new EntityManager();

        // Create entities
        Department dept = new Department("Engineering", "Building A");
        Employee emp1 = new Employee("Alice", "alice@company.com", 85000.0);
        Employee emp2 = new Employee("Bob", "bob@company.com", 72000.0);
        Employee emp3 = new Employee("Charlie", "charlie@company.com", 95000.0);

        dept.addEmployee(emp1);
        dept.addEmployee(emp2);
        dept.addEmployee(emp3);

        // Persist (cascades to employees)
        System.out.println("--- Persisting Department (cascades to Employees) ---");
        em.persist(dept);

        System.out.println("\n--- Finding Department by ID ---");
        em.find(Department.class, 1L).ifPresentOrElse(
                d -> System.out.println("  Found: " + d),
                () -> System.out.println("  Not found")
        );

        System.out.println("\n--- Finding Employee by ID ---");
        em.find(Employee.class, 1L).ifPresentOrElse(
                e -> System.out.println("  Found: " + e),
                () -> System.out.println("  Not found")
        );

        System.out.println("\n--- Merging (Updating) Employee ---");
        em.find(Employee.class, 1L).ifPresent(emp -> {
            emp.setSalary(90000.0);
            em.merge(emp);
        });

        System.out.println("\n--- Finding Updated Employee ---");
        em.find(Employee.class, 1L).ifPresent(e ->
                System.out.println("  Updated salary: $" + e.getSalary()));

        System.out.println("\n--- Removing Employee ---");
        em.find(Employee.class, 2L).ifPresent(em::remove);

        System.out.println("\n--- Find All Employees ---");
        var allEmployees = em.findAll(Employee.class);
        allEmployees.forEach(e -> System.out.println("  " + e));

        System.out.println("\n--- ORM Mapping Metadata ---");
        var deptMeta = em.find(Department.class, 1L);
        System.out.println("  Entity: Department");
        System.out.println("  Table: departments");
        System.out.println("  Columns: id, dept_name, location");
        System.out.println("  Relationships: @OneToMany -> employees (cascade ALL)");

        System.out.println("\n--- Custom Annotations Used ---");
        System.out.println("  @Entity(table=\"...\")  - Marks a POJO as an entity");
        System.out.println("  @Id                    - Marks the primary key field");
        System.out.println("  @GeneratedValue        - Auto-generation strategy");
        System.out.println("  @Column(name, nullable, length) - Column mapping");
        System.out.println("  @OneToMany(mappedBy, cascade)   - One-to-many relationship");
        System.out.println("  @ManyToOne(joinColumn)          - Many-to-one relationship");
    }
}
