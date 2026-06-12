package phase16.projects;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class MiniSpringFramework {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Component {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Service {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Repository {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Configuration {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Bean {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
    public @interface Autowired {
        boolean required() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Qualifier {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PostConstruct {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PreDestroy {
    }

    public static final class BeanDefinition {
        private final String name;
        private final Class<?> type;
        private final Object instance;
        private final boolean isPrimary;
        private final Map<String, Object> properties = new HashMap<>();

        public BeanDefinition(String name, Class<?> type, Object instance, boolean isPrimary) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
            this.instance = instance;
            this.isPrimary = isPrimary;
        }

        @SuppressWarnings("unchecked")
        public <T> T getInstance() { return (T) instance; }
        public String getName() { return name; }
        public Class<?> getType() { return type; }
        public boolean isPrimary() { return isPrimary; }
        public void setProperty(String name, Object value) { properties.put(name, value); }
        public Object getProperty(String name) { return properties.get(name); }

        @Override
        public String toString() {
            return "Bean[name=%s, type=%s, primary=%s]".formatted(name, type.getSimpleName(), isPrimary);
        }
    }

    public static final class ApplicationContext {
        private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
        private final Map<Class<?>, String> primaryBeans = new ConcurrentHashMap<>();
        private final List<Class<?>> scanPackages = new ArrayList<>();
        private volatile boolean initialized = false;

        public ApplicationContext(String... basePackages) {
            for (var pkg : basePackages) {
                scanPackages.addAll(findPackages(pkg));
            }
        }

        private List<Class<?>> findPackages(String basePackage) {
            var classes = new ArrayList<Class<?>>();
            try {
                var path = basePackage.replace('.', '/');
                var classLoader = Thread.currentThread().getContextClassLoader();
                var resources = classLoader.getResources(path);
                while (resources.hasMoreElements()) {
                    var resource = resources.nextElement();
                    if (resource.getProtocol().equals("file")) {
                        var dir = new File(resource.toURI());
                        if (dir.exists()) {
                            scanDirectory(dir, basePackage, classes);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  Warning: could not scan package " + basePackage + ": " + e.getMessage());
            }
            return classes;
        }

        private void scanDirectory(File dir, String packageName, List<Class<?>> classes) {
            var files = dir.listFiles();
            if (files == null) return;
            for (var file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, packageName + "." + file.getName(), classes);
                } else if (file.getName().endsWith(".class")) {
                    var className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        // skip
                    }
                }
            }
        }

        public void refresh() {
            initialized = false;
            beanDefinitions.clear();
            primaryBeans.clear();

            var candidateClasses = new ArrayList<Class<?>>();

            try {
                var thisClass = getClass();
                var thisPackage = thisClass.getPackage().getName();
                var classLoader = thisClass.getClassLoader();
                var path = thisPackage.replace('.', '/');
                var resources = classLoader.getResources(path);
                while (resources.hasMoreElements()) {
                    var resource = resources.nextElement();
                    if (resource.getProtocol().equals("file")) {
                        var dir = new File(resource.toURI());
                        if (dir.exists()) {
                            scanDirectoryForAnnotated(dir, thisPackage, candidateClasses);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  Warning: scan error: " + e.getMessage());
            }

            for (var clazz : candidateClasses) {
                if (clazz.isAnnotation() || clazz.isInterface() || clazz.isEnum()) continue;
                if (clazz.isAnnotationPresent(Component.class) ||
                        clazz.isAnnotationPresent(Service.class) ||
                        clazz.isAnnotationPresent(Repository.class)) {
                    registerBean(clazz);
                }
                if (clazz.isAnnotationPresent(Configuration.class)) {
                    processConfiguration(clazz);
                }
            }

            wireDependencies();

            invokePostConstruct();

            initialized = true;
            System.out.println("  ApplicationContext refreshed with " + beanDefinitions.size() + " beans");
        }

        private void scanDirectoryForAnnotated(File dir, String packageName, List<Class<?>> result) {
            var files = dir.listFiles();
            if (files == null) return;
            for (var file : files) {
                if (file.isDirectory()) {
                    scanDirectoryForAnnotated(file, packageName + "." + file.getName(), result);
                } else if (file.getName().endsWith(".class")) {
                    var className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        var clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Component.class) ||
                                clazz.isAnnotationPresent(Service.class) ||
                                clazz.isAnnotationPresent(Repository.class) ||
                                clazz.isAnnotationPresent(Configuration.class)) {
                            result.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // skip
                    }
                }
            }
        }

        public void registerBean(Class<?> clazz) {
            try {
                var beanName = resolveBeanName(clazz);
                var constructor = findInjectableConstructor(clazz);
                var params = constructor.getParameterTypes();
                var args = new Object[params.length];

                for (int i = 0; i < params.length; i++) {
                    args[i] = resolveDependency(params[i]);
                }

                var instance = constructor.newInstance(args);
                registerBeanInstance(beanName, clazz, instance, false);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register bean " + clazz.getName(), e);
            }
        }

        private String resolveBeanName(Class<?> clazz) {
            Component comp = clazz.getAnnotation(Component.class);
            if (comp != null && !comp.value().isEmpty()) return comp.value();
            Service svc = clazz.getAnnotation(Service.class);
            if (svc != null && !svc.value().isEmpty()) return svc.value();
            Repository repo = clazz.getAnnotation(Repository.class);
            if (repo != null && !repo.value().isEmpty()) return repo.value();

            var name = clazz.getSimpleName();
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        private Constructor<?> findInjectableConstructor(Class<?> clazz) {
            var constructors = clazz.getDeclaredConstructors();
            Constructor<?> injectConstructor = null;
            for (var ctor : constructors) {
                if (ctor.isAnnotationPresent(Autowired.class)) {
                    if (injectConstructor != null)
                        throw new RuntimeException("Multiple @Autowired constructors in " + clazz);
                    injectConstructor = ctor;
                }
            }
            if (injectConstructor == null) {
                injectConstructor = constructors.length > 0 ? constructors[0] : null;
            }
            if (injectConstructor != null) injectConstructor.setAccessible(true);
            return injectConstructor;
        }

        private Object resolveDependency(Class<?> type) {
            var beanDef = beanDefinitions.values().stream()
                    .filter(b -> type.isAssignableFrom(b.getType()))
                    .findFirst()
                    .orElse(null);
            if (beanDef != null) return beanDef.getInstance();
            return null;
        }

        private void processConfiguration(Class<?> configClass) {
            try {
                var instance = configClass.getDeclaredConstructor().newInstance();
                registerBeanInstance(
                        Character.toLowerCase(configClass.getSimpleName().charAt(0)) +
                                configClass.getSimpleName().substring(1),
                        configClass, instance, false);

                for (var method : configClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Bean.class)) {
                        var beanName = method.getAnnotation(Bean.class).value();
                        if (beanName.isEmpty()) beanName = method.getName();

                        var params = method.getParameterTypes();
                        var args = new Object[params.length];
                        for (int i = 0; i < params.length; i++) {
                            args[i] = resolveDependency(params[i]);
                        }

                        var beanInstance = method.invoke(instance, args);
                        registerBeanInstance(beanName, beanInstance.getClass(), beanInstance, false);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process configuration " + configClass, e);
            }
        }

        private void registerBeanInstance(String name, Class<?> type, Object instance, boolean primary) {
            var def = new BeanDefinition(name, type, instance, primary);
            beanDefinitions.put(name, def);
            if (primary || !primaryBeans.containsKey(type)) {
                primaryBeans.put(type, name);
            }
        }

        private void wireDependencies() {
            for (var def : beanDefinitions.values()) {
                var instance = def.getInstance();
                var fields = getAllFields(instance.getClass());
                for (var field : fields) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        field.setAccessible(true);
                        var depType = field.getType();
                        var qualifier = field.getAnnotation(Qualifier.class);
                        BeanDefinition dep;
                        if (qualifier != null) {
                            dep = beanDefinitions.get(qualifier.value());
                        } else {
                            dep = findDependency(depType);
                        }
                        if (dep != null) {
                            try {
                                field.set(instance, dep.getInstance());
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Failed to wire " + field.getName() + " in " + instance.getClass(), e);
                            }
                        } else if (field.getAnnotation(Autowired.class).required()) {
                            throw new RuntimeException("No bean found for " + depType + " required by " + instance.getClass());
                        }
                    }
                }

                for (var method : instance.getClass().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Autowired.class)) {
                        var paramTypes = method.getParameterTypes();
                        var args = new Object[paramTypes.length];
                        for (int i = 0; i < paramTypes.length; i++) {
                            args[i] = findDependency(paramTypes[i]).getInstance();
                        }
                        method.setAccessible(true);
                        try {
                            method.invoke(instance, args);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to invoke @Autowired method " + method.getName(), e);
                        }
                    }
                }
            }
        }

        private BeanDefinition findDependency(Class<?> type) {
            var matches = beanDefinitions.values().stream()
                    .filter(b -> type.isAssignableFrom(b.getType()))
                    .collect(Collectors.toList());
            if (matches.isEmpty()) return null;
            if (matches.size() == 1) return matches.get(0);
            return matches.stream().filter(BeanDefinition::isPrimary).findFirst()
                    .orElse(matches.get(0));
        }

        private void invokePostConstruct() {
            for (var def : beanDefinitions.values()) {
                for (var method : def.getType().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(PostConstruct.class)) {
                        method.setAccessible(true);
                        try {
                            method.invoke(def.getInstance());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed @PostConstruct in " + def.getType(), e);
                        }
                    }
                }
            }
        }

        private List<Field> getAllFields(Class<?> clazz) {
            var fields = new ArrayList<Field>();
            var current = clazz;
            while (current != null && current != Object.class) {
                fields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(Class<T> type) {
            var name = primaryBeans.get(type);
            if (name != null) return (T) beanDefinitions.get(name).getInstance();
            var found = findDependency(type);
            if (found != null) return (T) found.getInstance();
            throw new NoSuchElementException("No bean of type " + type);
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(String name) {
            var def = beanDefinitions.get(name);
            if (def == null) throw new NoSuchElementException("No bean named " + name);
            return (T) def.getInstance();
        }

        public List<BeanDefinition> getAllBeans() { return List.copyOf(beanDefinitions.values()); }
        public boolean containsBean(String name) { return beanDefinitions.containsKey(name); }
        public int beanCount() { return beanDefinitions.size(); }

        public void close() {
            for (var def : beanDefinitions.values()) {
                for (var method : def.getType().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(PreDestroy.class)) {
                        method.setAccessible(true);
                        try {
                            method.invoke(def.getInstance());
                        } catch (Exception e) {
                            System.out.println("  Error in @PreDestroy: " + e.getMessage());
                        }
                    }
                }
            }
            beanDefinitions.clear();
            primaryBeans.clear();
        }
    }

    @Service("userService")
    public static class UserService {
        private String serviceName = "UserService";

        @PostConstruct
        public void init() {
            System.out.println("  [UserService] @PostConstruct initialized");
        }

        public String getServiceName() { return serviceName; }

        public String findUser(String id) {
            return "User{id=%s, name='Alice Johnson', email='alice@example.com'}".formatted(id);
        }

        public List<String> listAllUsers() {
            return List.of("U001: Alice", "U002: Bob", "U003: Carol");
        }
    }

    @Repository("orderRepository")
    public static class OrderRepository {
        private final Map<String, String> orders = new ConcurrentHashMap<>();

        @PostConstruct
        public void init() {
            orders.put("ORD-001", "{\"item\":\"laptop\",\"status\":\"shipped\"}");
            orders.put("ORD-002", "{\"item\":\"mouse\",\"status\":\"pending\"}");
            System.out.println("  [OrderRepository] @PostConstruct loaded " + orders.size() + " orders");
        }

        public String findById(String id) { return orders.get(id); }
        public void save(String id, String data) { orders.put(id, data); }
        public int count() { return orders.size(); }
    }

    @Component
    public static class NotificationService {
        @Autowired
        private UserService userService;

        @Autowired
        private OrderRepository orderRepository;

        private boolean initialized = false;

        @PostConstruct
        public void setup() {
            this.initialized = true;
        }

        public String sendNotification(String userId, String message) {
            var user = userService.findUser(userId);
            return "Notification sent to %s: %s".formatted(user, message);
        }

        public String getOrderStatus(String orderId) {
            var order = orderRepository.findById(orderId);
            if (order == null) return "Order not found";
            return "Order %s: %s".formatted(orderId, order);
        }

        public boolean isInitialized() { return initialized; }
        public UserService getUserService() { return userService; }
        public OrderRepository getOrderRepository() { return orderRepository; }
    }

    @Configuration
    public static class AppConfig {
        @Bean
        public String appName() {
            return "MiniSpringApp";
        }

        @Bean
        public Integer maxConnections() {
            return 100;
        }

        @Bean
        public List<String> supportedLocales() {
            return List.of("en-US", "en-GB", "fr-FR", "de-DE");
        }
    }

    @Service("paymentService")
    public static class PaymentService {
        @Autowired
        private OrderRepository orderRepository;

        @PreDestroy
        public void cleanup() {
            System.out.println("  [PaymentService] @PreDestroy cleanup");
        }

        public String processPayment(String orderId, double amount) {
            var order = orderRepository.findById(orderId);
            if (order == null) return "Order not found";
            return "Payment of $%.2f processed for %s".formatted(amount, orderId);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Mini Spring Framework ===%n".formatted());

        var context = new ApplicationContext("phase16.projects");
        context.refresh();

        System.out.println("%n--- Registered Beans ---%n".formatted());
        for (var bean : context.getAllBeans()) {
            System.out.println("  " + bean);
        }

        System.out.println("%n--- Getting Beans ---%n".formatted());
        var userService = context.getBean(UserService.class);
        System.out.println("  UserService name: " + userService.getServiceName());
        System.out.println("  Find user: " + userService.findUser("U001"));

        var orderRepo = context.getBean(OrderRepository.class);
        System.out.println("  Orders count: " + orderRepo.count());

        var notifService = context.getBean(NotificationService.class);
        System.out.println("  NotificationService initialized: " + notifService.isInitialized());
        System.out.println("  Notification: " + notifService.sendNotification("U001", "Welcome!"));
        System.out.println("  Order status: " + notifService.getOrderStatus("ORD-001"));

        System.out.println("%n--- Beans from @Configuration/@Bean ---%n".formatted());
        var appName = context.getBean(String.class);
        System.out.println("  App name: " + appName);
        var maxConns = context.getBean(Integer.class);
        System.out.println("  Max connections: " + maxConns);
        var locales = context.getBean(List.class);
        System.out.println("  Locales: " + locales);

        System.out.println("%n--- Named Bean Lookup ---%n".formatted());
        var paySvc = context.getBean("paymentService");
        System.out.println("  Payment service: " + paySvc.getClass().getSimpleName());

        System.out.println("%n--- Pattern Matching on Beans ---%n".formatted());
        for (var bean : context.getAllBeans()) {
            switch (bean.getInstance()) {
                case UserService us ->
                    System.out.println("  Service: " + us.getServiceName() + " -> " + us.listAllUsers().size() + " users");
                case OrderRepository or ->
                    System.out.println("  Repository: " + or.count() + " orders");
                case NotificationService ns when ns.isInitialized() ->
                    System.out.println("  Component: NotificationService (ready)");
                case String s ->
                    System.out.println("  Value: " + s);
                case Integer i ->
                    System.out.println("  Value: " + i);
                case List<?> list ->
                    System.out.println("  List: " + list.size() + " items");
                default ->
                    System.out.println("  Other: " + bean.getType().getSimpleName());
            }
        }

        context.close();
        System.out.println("%n=== Done ===".formatted());
    }
}
