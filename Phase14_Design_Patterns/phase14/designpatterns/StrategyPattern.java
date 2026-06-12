package phase14.designpatterns;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

// Strategy Pattern: Strategy interface, concrete strategies, context class, lambdas as strategies

// Strategy interface
interface PaymentStrategy {
    String pay(double amount);
}

// Concrete strategies
class CreditCardStrategy implements PaymentStrategy {
    private final String cardNumber;
    private final String cvv;

    public CreditCardStrategy(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    @Override
    public String pay(double amount) {
        return String.format("Paid $%.2f with Credit Card ****%s", amount,
                cardNumber.substring(cardNumber.length() - 4));
    }
}

class PayPalStrategy implements PaymentStrategy {
    private final String email;

    public PayPalStrategy(String email) {
        this.email = email;
    }

    @Override
    public String pay(double amount) {
        return String.format("Paid $%.2f via PayPal (%s)", amount, email);
    }
}

class CryptoStrategy implements PaymentStrategy {
    private final String walletAddress;

    public CryptoStrategy(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    @Override
    public String pay(double amount) {
        return String.format("Paid $%.2f in Crypto (wallet: %s...)", amount,
                walletAddress.substring(0, Math.min(8, walletAddress.length())));
    }
}

class CashOnDeliveryStrategy implements PaymentStrategy {
    @Override
    public String pay(double amount) {
        return String.format("Will pay $%.2f on delivery (cash)", amount);
    }
}

// Context class that uses a strategy
class ShoppingCart {
    private final List<Item> items;
    private PaymentStrategy paymentStrategy;

    public ShoppingCart() {
        this.items = new java.util.ArrayList<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
        System.out.println("  [Context] Payment strategy set to: " + strategy.getClass().getSimpleName());
    }

    public double getTotal() {
        return items.stream().mapToDouble(Item::price).sum();
    }

    public String checkout() {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not set");
        }
        double total = getTotal();
        System.out.println("  [Context] Total: $" + String.format("%.2f", total));
        return paymentStrategy.pay(total);
    }

    // Functional approach: accept strategy as lambda/function
    public String checkoutWith(Function<Double, String> paymentFunction) {
        double total = getTotal();
        System.out.println("  [Context] Total: $" + String.format("%.2f", total));
        return paymentFunction.apply(total);
    }
}

record Item(String name, double price) {}

// Shipping strategy (another example of Strategy pattern)
interface ShippingStrategy {
    double calculateCost(double weight, String destination);
}

class StandardShipping implements ShippingStrategy {
    @Override
    public double calculateCost(double weight, String destination) {
        return weight * 2.0 + (destination.equals("international") ? 15 : 0);
    }
}

class ExpressShipping implements ShippingStrategy {
    @Override
    public double calculateCost(double weight, String destination) {
        return weight * 5.0 + (destination.equals("international") ? 25 : 0) + 10;
    }
}

class FreeShipping implements ShippingStrategy {
    @Override
    public double calculateCost(double weight, String destination) {
        return 0.0;
    }
}

public class StrategyPattern {
    public static void main(String[] args) {
        System.out.println("=== Strategy Pattern Demo ===\n");

        // 1. Classic Strategy Pattern
        System.out.println("1. Classic Strategy Pattern (interface + concrete implementations):");

        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Item("Laptop", 1299.99));
        cart.addItem(new Item("Mouse", 29.99));
        cart.addItem(new Item("Keyboard", 89.99));

        // Pay with Credit Card
        cart.setPaymentStrategy(new CreditCardStrategy("1234567890123456", "123"));
        System.out.println("  Result: " + cart.checkout());

        // Change strategy to PayPal
        cart.setPaymentStrategy(new PayPalStrategy("user@example.com"));
        System.out.println("  Result: " + cart.checkout());

        // Change strategy to Crypto
        cart.setPaymentStrategy(new CryptoStrategy("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        System.out.println("  Result: " + cart.checkout());

        // Change strategy to Cash on Delivery
        cart.setPaymentStrategy(new CashOnDeliveryStrategy());
        System.out.println("  Result: " + cart.checkout());

        // 2. Lambdas as Strategies (functional approach)
        System.out.println("\n2. Lambdas as Strategies (functional):");
        ShoppingCart cart2 = new ShoppingCart();
        cart2.addItem(new Item("Book", 19.99));
        cart2.addItem(new Item("Pen", 2.99));

        // Lambda strategy
        System.out.println("  Using lambda strategy:");
        String result = cart2.checkoutWith(total ->
                String.format("Paid $%.2f using lambda strategy (no class needed!)", total));
        System.out.println("  " + result);

        // Method reference as strategy
        System.out.println("  Using method reference:");
        result = cart2.checkoutWith(StrategyPattern::processPayment);
        System.out.println("  " + result);

        // 3. Another strategy example: Shipping
        System.out.println("\n3. Shipping Strategy:");
        double weight = 2.5;
        String destination = "international";

        List<ShippingStrategy> shippingStrategies = Arrays.asList(
                new StandardShipping(),
                new ExpressShipping(),
                new FreeShipping()
        );

        for (var strategy : shippingStrategies) {
            double cost = strategy.calculateCost(weight, destination);
            System.out.println("  " + strategy.getClass().getSimpleName()
                    + ": $" + String.format("%.2f", cost));
        }

        // 4. Lambda-based shipping strategies
        System.out.println("\n4. Lambda-based Shipping Strategies:");
        Function<Double, Double> standardRate = w -> w * 2.0 + 5.0;
        Function<Double, Double> expressRate = w -> w * 5.0 + 15.0;
        Function<Double, Double> freeRate = w -> 0.0;

        System.out.println("  Standard: $" + String.format("%.2f", standardRate.apply(weight)));
        System.out.println("  Express: $" + String.format("%.2f", expressRate.apply(weight)));
        System.out.println("  Free: $" + String.format("%.2f", freeRate.apply(weight)));

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Strategy interface - defines the algorithm contract");
        System.out.println("Concrete strategies - different algorithm implementations");
        System.out.println("Context class - uses a strategy, delegates algorithm execution");
        System.out.println("Lambdas as strategies - functional programming approach (no separate class needed)");
        System.out.println("Runtime strategy switching - change behavior by swapping strategies");
        System.out.println("Open/Closed Principle - new strategies added without modifying context");
    }

    // Method reference example
    public static String processPayment(double total) {
        return String.format("Processed $%.2f via method reference", total);
    }
}
