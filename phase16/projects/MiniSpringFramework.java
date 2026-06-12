package phase16.projects;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * MiniSpringFramework.java
 *
 * Mini Spring: @Component scanning (custom annotation), DI container,
 * @Autowired injection, @Bean definition, ApplicationContext simulation.
 */
public class MiniSpringFramework {

    // ═══════════════════════════════════════════════
    // Custom Annotations
    // ═══════════════════════════════════════════════

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Component {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
    @interface Autowired {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Bean {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Configuration {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Service {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Repository {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Qualifier {
        String value();
    }

    // ═══════════════════════════════════════════════
    // Bean Definition
    // ═══════════════════════════════════════════════

    static final class BeanDefinition {
        final String name;
        final Class<?> type;
        final Object instance;
        final boolean isSingleton;

        BeanDefinition(String name, Class<?> type, Object instance, boolean isSingleton) {
            this.name = name;
            this.type = type;
            this.instance = instance;
            this.isSingleton = isSingleton;
        }
    }

    // ═══════════════════════════════════════════════
    // Dependency Injection Container
    // ═══════════════════════════════════════════════

    static final class ApplicationContext {
        private final ConcurrentHashMap<String, BeanDefinition> beans = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Class<?>, String> primaryTypeNames = new ConcurrentHashMap<>();
        private final List<Class<?>> registeredClasses = new CopyOnWriteArrayList<>();

        public ApplicationContext(String... basePackages) {
            scanAndRegister(basePackages);
            performDependencyInjection();
        }

        // ─── Component Scanning ───

        private void scanAndRegister(String... basePackages) {
            // Scan for @Component classes and register them
            if (basePackages.length == 0) {
                // Use demo classes defined in this file
                registerDemoComponents();
            } else {
                System.out.println("  Scanning packages: " + Arrays.toString(basePackages));
            }
        }

        private void registerDemoComponents() {
            // Register built-in demo components
            registerComponent(new UserService());
            registerComponent(new OrderService());
            registerComponent(new InventoryService());
            registerComponent(new NotificationService());
            registerComponent(new PaymentService());
            registerComponent(new EmailService());
            registerComponent(new SmsService());

            // Register @Configuration classes
            registerConfiguration(new AppConfig());
        }

        public void registerComponent(Object instance) {
            Class<?> clazz = instance.getClass();
            String beanName = resolveBeanName(clazz);
            beans.put(beanName, new BeanDefinition(beanName, clazz, instance, true));

            // Register by type as well
            primaryTypeNames.put(clazz, beanName);

            // Register by interfaces
            for (var iface : clazz.getInterfaces()) {
                if (!primaryTypeNames.containsKey(iface)) {
                    primaryTypeNames.put(iface, beanName);
                }
            }
            registeredClasses.add(clazz);
        }

        public void registerBean(String name, Object instance) {
            beans.put(name, new BeanDefinition(name, instance.getClass(), instance, true));
            primaryTypeNames.put(instance.getClass(), name);
        }

        public void registerConfiguration(Object configInstance) {
            var methods = configInstance.getClass().getDeclaredMethods();
            for (var method : methods) {
                if (method.isAnnotationPresent(Bean.class)) {
                    try {
                        Object beanInstance = method.invoke(configInstance);
                        String beanName = method.getAnnotation(Bean.class).value();
                        if (beanName.isEmpty()) beanName = method.getName();
                        registerBean(beanName, beanInstance);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create @Bean: " + method.getName(), e);
                    }
                }
            }
            // Also register the config itself
            registerComponent(configInstance);
        }

        // ─── Dependency Injection ───

        private void performDependencyInjection() {
            for (var entry : beans.entrySet()) {
                injectDependencies(entry.getValue().instance);
            }
        }

        private void injectDependencies(Object target) {
            Class<?> clazz = target.getClass();

            // Field injection
            for (var field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object dependency = resolveDependency(field.getType(), field);
                    field.setAccessible(true);
                    try {
                        field.set(target, dependency);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject field: " + field.getName(), e);
                    }
                }
            }

            // Setter injection
            for (var method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Autowired.class) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object dependency = resolveDependency(paramType, method);
                    method.setAccessible(true);
                    try {
                        method.invoke(target, dependency);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to inject via setter: " + method.getName(), e);
                    }
                }
            }
        }

        private Object resolveDependency(Class<?> type, AnnotatedElement target) {
            // Check @Qualifier
            Qualifier qualifier = target.getAnnotation(Qualifier.class);
            if (qualifier != null) {
                String qualifierName = qualifier.value();
                var bd = beans.get(qualifierName);
                if (bd != null) return bd.instance;
            }

            // Lookup by type
            String beanName = primaryTypeNames.get(type);
            if (beanName != null) {
                var bd = beans.get(beanName);
                if (bd != null) return bd.instance;
            }

            // Lookup by assignable type
            for (var entry : beans.entrySet()) {
                if (type.isAssignableFrom(entry.getValue().type)) {
                    return entry.getValue().instance;
                }
            }

            throw new RuntimeException("No bean of type " + type.getSimpleName() + " found for " + target);
        }

        // ─── Bean Retrieval ───

        @SuppressWarnings("unchecked")
        public <T> T getBean(Class<T> type) {
            String name = primaryTypeNames.get(type);
            if (name != null) {
                var bd = beans.get(name);
                if (bd != null) return (T) bd.instance;
            }
            // Fallback: search all beans
            for (var entry : beans.entrySet()) {
                if (type.isAssignableFrom(entry.getValue().type)) {
                    return (T) entry.getValue().instance;
                }
            }
            throw new RuntimeException("No bean of type " + type.getSimpleName() + " found");
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(String name) {
            var bd = beans.get(name);
            if (bd == null) throw new RuntimeException("No bean named '" + name + "' found");
            return (T) bd.instance;
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(String name, Class<T> type) {
            var bd = beans.get(name);
            if (bd == null) throw new RuntimeException("No bean named '" + name + "' found");
            if (!type.isAssignableFrom(bd.type)) {
                throw new RuntimeException("Bean '" + name + "' is not of type " + type.getSimpleName());
            }
            return (T) bd.instance;
        }

        public boolean containsBean(String name) { return beans.containsKey(name); }
        public boolean containsBean(Class<?> type) {
            return primaryTypeNames.containsKey(type) ||
                beans.values().stream().anyMatch(b -> type.isAssignableFrom(b.type));
        }

        public List<String> getBeanNames() {
            return beans.keySet().stream().sorted().collect(Collectors.toList());
        }

        public int getBeanCount() { return beans.size(); }

        private String resolveBeanName(Class<?> clazz) {
            Component comp = clazz.getAnnotation(Component.class);
            if (comp != null && !comp.value().isEmpty()) return comp.value();

            Service svc = clazz.getAnnotation(Service.class);
            if (svc != null && !svc.value().isEmpty()) return svc.value();

            Repository repo = clazz.getAnnotation(Repository.class);
            if (repo != null && !repo.value().isEmpty()) return repo.value();

            // Default: camelCase class name
            String simpleName = clazz.getSimpleName();
            return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        }
    }

    // ═══════════════════════════════════════════════
    // Demo Service Classes (simulate @Component)
    // ═══════════════════════════════════════════════

    @Component
    static class EmailService {
        public void send(String to, String subject) {
            System.out.println("    [EmailService] Sending email to " + to + ": " + subject);
        }
    }

    @Component("smsService")
    static class SmsService {
        public void send(String phone, String message) {
            System.out.println("    [SmsService] Sending SMS to " + phone + ": " + message);
        }
    }

    @Service
    static class NotificationService {
        @Autowired
        private EmailService emailService;

        @Autowired
        private SmsService smsService;

        public void notifyByEmail(String to, String subject) {
            emailService.send(to, subject);
        }

        public void notifyBySms(String phone, String message) {
            smsService.send(phone, message);
        }
    }

    @Repository
    static class InventoryService {
        private final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

        public InventoryService() {
            stock.put("PROD-1", 100);
            stock.put("PROD-2", 50);
        }

        public boolean checkStock(String productId, int quantity) {
            return stock.getOrDefault(productId, 0) >= quantity;
        }
    }

    @Service
    static class PaymentService {
        public boolean processPayment(String orderId, double amount) {
            System.out.println("    [PaymentService] Processing payment for " + orderId + ": $" + amount);
            return true;
        }
    }

    @Service
    static class UserService {
        private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

        public UserService() {
            users.put("USR-1", "Alice");
            users.put("USR-2", "Bob");
        }

        public String findUser(String id) {
            return users.get(id);
        }
    }

    @Service
    static class OrderService {
        @Autowired
        private UserService userService;

        @Autowired
        private InventoryService inventoryService;

        @Autowired
        private PaymentService paymentService;

        @Autowired
        private NotificationService notificationService;

        public String placeOrder(String userId, String productId, int quantity) {
            String user = userService.findUser(userId);
            if (user == null) throw new IllegalArgumentException("User not found");

            if (!inventoryService.checkStock(productId, quantity)) {
                throw new IllegalStateException("Insufficient stock");
            }

            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
            boolean paid = paymentService.processPayment(orderId, 99.99);
            if (!paid) throw new RuntimeException("Payment failed");

            notificationService.notifyByEmail(user + "@email.com", "Order " + orderId + " confirmed");
            return orderId;
        }
    }

    @Configuration
    static class AppConfig {
        @Bean
        public String appName() {
            return "MiniSpringApp";
        }

        @Bean
        public Integer maxRetries() {
            return 3;
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Mini Spring Framework ===\n");

        // ─── Create Application Context ───
        System.out.println("--- Application Context ---");
        var context = new ApplicationContext();

        System.out.println("  Beans registered: " + context.getBeanCount());
        System.out.println("  Bean names: " + context.getBeanNames());

        // ─── Retrieve Beans ───
        System.out.println("\n--- Bean Retrieval ---");
        var emailSvc = context.getBean(EmailService.class);
        System.out.println("  EmailService: " + emailSvc.getClass().getSimpleName());

        var notifSvc = context.getBean(NotificationService.class);
        System.out.println("  NotificationService: " + notifSvc.getClass().getSimpleName());

        var orderSvc = context.getBean(OrderService.class);
        System.out.println("  OrderService: " + orderSvc.getClass().getSimpleName());

        var smsSvc = context.getBean("smsService", SmsService.class);
        System.out.println("  SmsService (by name 'smsService'): " + smsSvc.getClass().getSimpleName());

        // ─── @Configuration @Bean ───
        System.out.println("\n--- @Configuration Beans ---");
        String appName = context.getBean("appName", String.class);
        Integer maxRetries = context.getBean("maxRetries", Integer.class);
        System.out.println("  appName: " + appName);
        System.out.println("  maxRetries: " + maxRetries);

        // ─── Dependency Injection ───
        System.out.println("\n--- @Autowired Injection ---");
        System.out.println("  OrderService has UserService: " + (orderSvc.userService != null));
        System.out.println("  OrderService has InventoryService: " + (orderSvc.inventoryService != null));
        System.out.println("  OrderService has PaymentService: " + (orderSvc.paymentService != null));
        System.out.println("  OrderService has NotificationService: " + (orderSvc.notificationService != null));
        System.out.println("  NotificationService has EmailService: " + (notifSvc.emailService != null));
        System.out.println("  NotificationService has SmsService: " + (notifSvc.smsService != null));

        // ─── Full Service Invocation ───
        System.out.println("\n--- Service Invocation ---");
        try {
            String orderId = orderSvc.placeOrder("USR-1", "PROD-1", 2);
            System.out.println("  Order placed successfully: " + orderId);
        } catch (Exception e) {
            System.out.println("  Order failed: " + e.getMessage());
        }

        // ─── Notification Services ───
        System.out.println("\n--- Notifications ---");
        notifSvc.notifyByEmail("test@example.com", "Welcome!");
        notifSvc.notifyBySms("+1234567890", "Your code is 1234");

        // ─── Bean Lifecycle Check ───
        System.out.println("\n--- Singleton Verification ---");
        var orderSvc2 = context.getBean(OrderService.class);
        System.out.println("  Same OrderService instance: " + (orderSvc == orderSvc2));

        System.out.println("\n=== Mini Spring Framework Complete ===");
    }
}
