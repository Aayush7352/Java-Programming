package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

// --- Demo: Constructor / Setter / Field injection, @Qualifier, @Primary, @Inject ---

// Java 21: annotations and DI concepts
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@interface Inject {}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
@interface Qualifier {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Primary {}

// Service interfaces and implementations
interface PaymentGateway {
    String processPayment(double amount);
}

@Primary
class CreditCardPayment implements PaymentGateway {
    @Override
    public String processPayment(double amount) {
        return "Credit Card: processed $" + amount;
    }
}

class PayPalPayment implements PaymentGateway {
    @Override
    public String processPayment(double amount) {
        return "PayPal: processed $" + amount;
    }
}

class CryptoPayment implements PaymentGateway {
    @Override
    public String processPayment(double amount) {
        return "Crypto: processed $" + amount + " in BTC";
    }
}

// A simple DI container to demonstrate injection types
class DIContainer {
    private final Map<String, Object> beans = new HashMap<>();

    public void register(String qualifier, Object bean) {
        beans.put(qualifier, bean);
        // If @Primary, also register under a default key
        if (bean.getClass().isAnnotationPresent(Primary.class)) {
            beans.put("__primary__" + bean.getClass().getName(), bean);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type, String qualifier) {
        // Try qualifier first
        if (qualifier != null && beans.containsKey(qualifier)) {
            return type.cast(beans.get(qualifier));
        }
        // Try primary
        String primaryKey = "__primary__" + type.getName();
        if (beans.containsKey(primaryKey)) {
            return type.cast(beans.get(primaryKey));
        }
        // Fallback: find first matching type
        for (var entry : beans.entrySet()) {
            if (type.isInstance(entry.getValue())) {
                return type.cast(entry.getValue());
            }
        }
        throw new RuntimeException("No bean of type " + type + " found");
    }
}

// Demonstrating different injection styles
class PaymentService {
    private PaymentGateway gateway;

    // Constructor injection
    @Inject
    public PaymentService(@Qualifier("creditCard") PaymentGateway gateway) {
        this.gateway = gateway;
        System.out.println("  [Constructor Injection] PaymentService created with gateway: "
                + gateway.getClass().getSimpleName());
    }

    public String pay(double amount) {
        return gateway.processPayment(amount);
    }
}

class PaymentProcessor {
    private PaymentGateway gateway;

    // Setter injection
    @Inject
    public void setPaymentGateway(@Qualifier("paypal") PaymentGateway gateway) {
        this.gateway = gateway;
        System.out.println("  [Setter Injection] PaymentProcessor received gateway: "
                + gateway.getClass().getSimpleName());
    }

    public String process(double amount) {
        if (gateway == null) {
            throw new IllegalStateException("PaymentGateway not injected via setter");
        }
        return gateway.processPayment(amount);
    }
}

class CheckoutHandler {
    @Inject
    @Qualifier("crypto")
    private PaymentGateway gateway; // Field injection

    public String checkout(double amount) {
        System.out.println("  [Field Injection] CheckoutHandler using gateway: "
                + gateway.getClass().getSimpleName());
        return gateway.processPayment(amount);
    }
}

public class DependencyInjection {
    public static void main(String[] args) {
        System.out.println("=== Dependency Injection Demo ===");

        // Set up DI container
        DIContainer container = new DIContainer();
        container.register("creditCard", new CreditCardPayment());
        container.register("paypal", new PayPalPayment());
        container.register("crypto", new CryptoPayment());

        // --- Constructor Injection ---
        System.out.println("\n1. Constructor Injection (@Inject on constructor, @Qualifier on param):");
        PaymentGateway ccGateway = container.resolve(PaymentGateway.class, "creditCard");
        var paymentService = new PaymentService(ccGateway);
        System.out.println("   Result: " + paymentService.pay(150.00));

        // --- Setter Injection ---
        System.out.println("\n2. Setter Injection (@Inject on setter method):");
        var processor = new PaymentProcessor();
        PaymentGateway ppGateway = container.resolve(PaymentGateway.class, "paypal");
        processor.setPaymentGateway(ppGateway);
        System.out.println("   Result: " + processor.process(89.99));

        // --- Field Injection ---
        System.out.println("\n3. Field Injection (@Inject + @Qualifier on field):");
        var checkout = new CheckoutHandler();
        // Simulating field injection via reflection
        try {
            var field = CheckoutHandler.class.getDeclaredField("gateway");
            field.setAccessible(true);
            field.set(checkout, container.resolve(PaymentGateway.class, "crypto"));
        } catch (Exception e) {
            System.out.println("   Reflection injection failed: " + e.getMessage());
        }
        System.out.println("   Result: " + checkout.checkout(0.025));

        // --- @Primary demonstration ---
        System.out.println("\n4. @Primary (default bean when no @Qualifier specified):");
        PaymentGateway primaryGateway = container.resolve(PaymentGateway.class, null);
        System.out.println("   Resolved primary: " + primaryGateway.getClass().getSimpleName()
                + " -> " + primaryGateway.processPayment(200));

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Constructor injection - dependencies provided through constructor");
        System.out.println("Setter injection - dependencies set via setter methods");
        System.out.println("Field injection - dependencies injected directly into fields (via reflection)");
        System.out.println("@Qualifier(\"name\") - disambiguates when multiple beans of same type exist");
        System.out.println("@Primary - marks a bean as the default candidate for injection");
        System.out.println("@Inject (CDI/JSR-330) - alternative to @Autowired");
    }
}
