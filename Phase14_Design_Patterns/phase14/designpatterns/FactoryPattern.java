package phase14.designpatterns;

import java.util.function.Supplier;

// Factory Pattern: Factory interface, concrete factories, product interface, concrete products

// Product interface
interface Payment {
    void pay(double amount);
}

// Concrete products
class CreditCardPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.printf("  Paid $%.2f via Credit Card (fee: 2.5%%)%n", amount);
    }
}

class PayPalPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.printf("  Paid $%.2f via PayPal (no fee)%n", amount);
    }
}

class CryptoPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.printf("  Paid $%.2f via Cryptocurrency (BTC)%n", amount);
    }
}

class CashPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.printf("  Paid $%.2f in Cash%n", amount);
    }
}

// Factory interface
interface PaymentFactory {
    Payment createPayment();
    String getPaymentType();
}

// Concrete factories
class CreditCardFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new CreditCardPayment();
    }

    @Override
    public String getPaymentType() {
        return "Credit Card";
    }
}

class PayPalFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new PayPalPayment();
    }

    @Override
    public String getPaymentType() {
        return "PayPal";
    }
}

class CryptoFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new CryptoPayment();
    }

    @Override
    public String getPaymentType() {
        return "Cryptocurrency";
    }
}

class CashFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new CashPayment();
    }

    @Override
    public String getPaymentType() {
        return "Cash";
    }
}

// Factory registry (using Java 21 features)
class PaymentFactoryRegistry {
    // Factory method using Supplier (functional approach)
    public static Payment createPayment(String type) {
        return switch (type.toLowerCase()) {
            case "creditcard", "credit" -> new CreditCardPayment();
            case "paypal" -> new PayPalPayment();
            case "crypto" -> new CryptoPayment();
            case "cash" -> new CashPayment();
            default -> throw new IllegalArgumentException("Unknown payment type: " + type);
        };
    }

    // Factory registry with Suppliers (Java 8+ functional style)
    private static final java.util.Map<String, Supplier<Payment>> FACTORY_MAP = java.util.Map.of(
            "creditcard", CreditCardPayment::new,
            "credit", CreditCardPayment::new,
            "paypal", PayPalPayment::new,
            "crypto", CryptoPayment::new,
            "cash", CashPayment::new
    );

    public static Payment createFromRegistry(String type) {
        var supplier = FACTORY_MAP.get(type.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown payment type: " + type);
        }
        return supplier.get();
    }
}

public class FactoryPattern {
    public static void main(String[] args) {
        System.out.println("=== Factory Pattern Demo ===\n");

        // 1. Using concrete factories (Factory Method pattern)
        System.out.println("1. Factory Method Pattern (concrete factories):");
        PaymentFactory[] factories = {
                new CreditCardFactory(),
                new PayPalFactory(),
                new CryptoFactory(),
                new CashFactory()
        };

        for (var factory : factories) {
            System.out.println("  [" + factory.getPaymentType() + "]");
            var payment = factory.createPayment();
            payment.pay(99.99);
        }

        // 2. Using factory registry with switch (Java 21 pattern matching)
        System.out.println("\n2. Factory Registry (switch expression):");
        String[] types = {"credit", "paypal", "crypto", "cash"};
        for (var type : types) {
            var payment = PaymentFactoryRegistry.createPayment(type);
            System.out.print("  [" + type + "] ");
            payment.pay(50.00);
        }

        // 3. Using Supplier-based factory map
        System.out.println("\n3. Supplier-based Factory Map:");
        for (var type : types) {
            var payment = PaymentFactoryRegistry.createFromRegistry(type);
            System.out.print("  [" + type + "] ");
            payment.pay(25.00);
        }

        // 4. Lambda function as factory (functional approach)
        System.out.println("\n4. Lambda as Factory (functional):");
        java.util.function.Function<Double, String> paymentProcessor = amount -> {
            Payment payment = PaymentFactoryRegistry.createFromRegistry("paypal");
            payment.pay(amount);
            return "Processed: $" + amount;
        };
        System.out.println("  " + paymentProcessor.apply(150.00));

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Product interface - defines the common interface for created objects");
        System.out.println("Concrete products - different implementations of the product");
        System.out.println("Factory interface - declares factory method returning products");
        System.out.println("Concrete factories - each creates a specific product type");
        System.out.println("Supplier/Functional factories - lambda-based factory creation");
    }
}
