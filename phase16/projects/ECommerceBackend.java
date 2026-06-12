package phase16.projects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * ECommerceBackend.java
 *
 * E-commerce backend: Product (record), User, Order, Cart, Payment processing,
 * inventory management, order workflow (PENDING -> CONFIRMED -> SHIPPED -> DELIVERED).
 */
public class ECommerceBackend {

    // ═══════════════════════════════════════════════
    // Records & Enums
    // ═══════════════════════════════════════════════

    record Product(String productId, String name, String category, double price, int stockQuantity) {
        Product withStock(int newStock) {
            return new Product(productId, name, category, price, newStock);
        }
    }

    record User(String userId, String name, String email, String address) {}

    record CartItem(String productId, String productName, double unitPrice, int quantity) {
        double subtotal() { return unitPrice * quantity; }
        CartItem withQuantity(int qty) { return new CartItem(productId, productName, unitPrice, qty); }
    }

    record Cart(String userId, List<CartItem> items, double total) {
        Cart { items = List.copyOf(items); }
    }

    enum OrderStatus { PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED }

    record Order(String orderId, String userId, List<OrderItem> items, double totalAmount,
                 OrderStatus status, Instant createdAt, Instant updatedAt, String shippingAddress) {
        Order withStatus(OrderStatus newStatus) {
            return new Order(orderId, userId, items, totalAmount, newStatus, createdAt, Instant.now(), shippingAddress);
        }
    }

    record OrderItem(String productId, String productName, double unitPrice, int quantity, double subtotal) {}

    record Payment(String paymentId, String orderId, double amount, String method, Instant timestamp, boolean success) {}

    // ═══════════════════════════════════════════════
    // Inventory Manager
    // ═══════════════════════════════════════════════

    static final class InventoryManager {
        private final ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<>();
        private final AtomicInteger productCounter = new AtomicInteger(0);

        public Product addProduct(String name, String category, double price, int stock) {
            String id = "PROD-" + productCounter.incrementAndGet();
            var product = new Product(id, name, category, price, stock);
            products.put(id, product);
            return product;
        }

        public Optional<Product> getProduct(String productId) {
            return Optional.ofNullable(products.get(productId));
        }

        public List<Product> searchProducts(String query) {
            String q = query.toLowerCase();
            return products.values().stream()
                .filter(p -> p.name().toLowerCase().contains(q) || p.category().toLowerCase().contains(q))
                .collect(Collectors.toList());
        }

        public boolean reserveStock(String productId, int quantity) {
            return products.computeIfPresent(productId, (k, p) -> {
                if (p.stockQuantity() >= quantity) {
                    return p.withStock(p.stockQuantity() - quantity);
                }
                return p; // no change
            }) != null && products.get(productId).stockQuantity() >= 0;
        }

        public void releaseStock(String productId, int quantity) {
            products.computeIfPresent(productId, (k, p) -> p.withStock(p.stockQuantity() + quantity));
        }

        public boolean hasStock(String productId, int quantity) {
            var p = products.get(productId);
            return p != null && p.stockQuantity() >= quantity;
        }

        public List<Product> getAllProducts() {
            return products.values().stream()
                .sorted(Comparator.comparing(Product::name))
                .collect(Collectors.toList());
        }

        public int getTotalProducts() { return products.size(); }
    }

    // ═══════════════════════════════════════════════
    // Cart Manager
    // ═══════════════════════════════════════════════

    static final class CartManager {
        private final ConcurrentHashMap<String, CopyOnWriteArrayList<CartItem>> userCarts = new ConcurrentHashMap<>();
        private final InventoryManager inventory;

        CartManager(InventoryManager inventory) { this.inventory = inventory; }

        public void addToCart(String userId, String productId, int quantity) {
            var product = inventory.getProduct(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            if (!inventory.hasStock(productId, quantity)) {
                throw new IllegalStateException("Insufficient stock for " + product.name());
            }

            var cart = userCarts.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
            // Check if product already in cart
            var existing = cart.stream().filter(i -> i.productId().equals(productId)).findFirst();
            if (existing.isPresent()) {
                int idx = cart.indexOf(existing.get());
                cart.set(idx, existing.get().withQuantity(existing.get().quantity() + quantity));
            } else {
                cart.add(new CartItem(productId, product.name(), product.price(), quantity));
            }
        }

        public void removeFromCart(String userId, String productId) {
            var cart = userCarts.get(userId);
            if (cart != null) {
                cart.removeIf(i -> i.productId().equals(productId));
            }
        }

        public void updateQuantity(String userId, String productId, int newQuantity) {
            var cart = userCarts.get(userId);
            if (cart == null) return;
            var existing = cart.stream().filter(i -> i.productId().equals(productId)).findFirst();
            existing.ifPresent(item -> {
                int idx = cart.indexOf(item);
                if (newQuantity <= 0) {
                    cart.remove(idx);
                } else {
                    cart.set(idx, item.withQuantity(newQuantity));
                }
            });
        }

        public Cart getCart(String userId) {
            var items = userCarts.getOrDefault(userId, new CopyOnWriteArrayList<>());
            double total = items.stream().mapToDouble(CartItem::subtotal).sum();
            return new Cart(userId, List.copyOf(items), total);
        }

        public void clearCart(String userId) {
            userCarts.remove(userId);
        }
    }

    // ═══════════════════════════════════════════════
    // Payment Processor
    // ═══════════════════════════════════════════════

    static final class PaymentProcessor {
        private final AtomicInteger paymentCounter = new AtomicInteger(0);
        private final double failureProbability = 0.1; // 10% chance of failure

        public Payment processPayment(String orderId, double amount, String method) {
            String paymentId = "PAY-" + paymentCounter.incrementAndGet();
            boolean success = ThreadLocalRandom.current().nextDouble() > failureProbability;

            // Simulate processing delay
            try { Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return new Payment(paymentId, orderId, amount, method, Instant.now(), success);
        }
    }

    // ═══════════════════════════════════════════════
    // Order Manager
    // ═══════════════════════════════════════════════

    static final class OrderManager {
        private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
        private final InventoryManager inventory;
        private final PaymentProcessor paymentProcessor;
        private final AtomicInteger orderCounter = new AtomicInteger(0);

        OrderManager(InventoryManager inventory, PaymentProcessor paymentProcessor) {
            this.inventory = inventory;
            this.paymentProcessor = paymentProcessor;
        }

        public Order placeOrder(String userId, Cart cart, String shippingAddress, String paymentMethod) {
            if (cart.items().isEmpty()) throw new IllegalStateException("Cart is empty");
            if (cart.userId() == null || !cart.userId().equals(userId)) {
                throw new IllegalArgumentException("Cart does not belong to user");
            }

            // Reserve stock
            for (var item : cart.items()) {
                if (!inventory.reserveStock(item.productId(), item.quantity())) {
                    // Rollback reserved stock
                    for (var prev : cart.items()) {
                        if (prev.equals(item)) break;
                        inventory.releaseStock(prev.productId(), prev.quantity());
                    }
                    throw new IllegalStateException("Insufficient stock for " + item.productName());
                }
            }

            String orderId = "ORD-" + orderCounter.incrementAndGet();
            var orderItems = cart.items().stream()
                .map(ci -> new OrderItem(ci.productId(), ci.productName(), ci.unitPrice(), ci.quantity(), ci.subtotal()))
                .collect(Collectors.toList());

            var order = new Order(orderId, userId, orderItems, cart.total(), OrderStatus.PENDING,
                Instant.now(), Instant.now(), shippingAddress);
            orders.put(orderId, order);
            return order;
        }

        public Order confirmOrder(String orderId) {
            var order = getOrder(orderId);
            if (order.status() != OrderStatus.PENDING) {
                throw new IllegalStateException("Order must be PENDING to confirm, current: " + order.status());
            }
            var updated = order.withStatus(OrderStatus.CONFIRMED);
            orders.put(orderId, updated);
            return updated;
        }

        public Pair<Order, Payment> checkoutAndPay(String userId, Cart cart, String shippingAddress, String paymentMethod) {
            var order = placeOrder(userId, cart, shippingAddress, paymentMethod);
            var payment = paymentProcessor.processPayment(order.orderId(), order.totalAmount(), paymentMethod);

            Order updatedOrder;
            if (payment.success()) {
                updatedOrder = order.withStatus(OrderStatus.CONFIRMED);
            } else {
                // Rollback stock
                for (var item : cart.items()) {
                    inventory.releaseStock(item.productId(), item.quantity());
                }
                updatedOrder = order.withStatus(OrderStatus.CANCELLED);
            }
            orders.put(order.orderId(), updatedOrder);
            return new Pair<>(updatedOrder, payment);
        }

        record Pair<A, B>(A first, B second) {}

        public Order shipOrder(String orderId) {
            return transitionOrder(orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);
        }

        public Order deliverOrder(String orderId) {
            return transitionOrder(orderId, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        }

        public Order cancelOrder(String orderId) {
            var order = getOrder(orderId);
            if (order.status() == OrderStatus.SHIPPED || order.status() == OrderStatus.DELIVERED) {
                throw new IllegalStateException("Cannot cancel order in status: " + order.status());
            }
            // Release stock
            for (var item : order.items()) {
                inventory.releaseStock(item.productId(), item.quantity());
            }
            var updated = order.withStatus(OrderStatus.CANCELLED);
            orders.put(orderId, updated);
            return updated;
        }

        public Order getOrder(String orderId) {
            var order = orders.get(orderId);
            if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);
            return order;
        }

        public List<Order> getOrdersByUser(String userId) {
            return orders.values().stream()
                .filter(o -> o.userId().equals(userId))
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .collect(Collectors.toList());
        }

        private Order transitionOrder(String orderId, OrderStatus from, OrderStatus to) {
            var order = getOrder(orderId);
            if (order.status() != from) {
                throw new IllegalStateException("Order must be " + from + " to transition to " + to
                    + ", current: " + order.status());
            }
            var updated = order.withStatus(to);
            orders.put(orderId, updated);
            return updated;
        }

        public int getTotalOrders() { return orders.size(); }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== E-Commerce Backend ===\n");

        InventoryManager inventory = new InventoryManager();
        PaymentProcessor paymentProcessor = new PaymentProcessor();
        CartManager cartManager = new CartManager(inventory);
        OrderManager orderManager = new OrderManager(inventory, paymentProcessor);

        // ─── Add Products ───
        System.out.println("--- Products ---");
        var laptop = inventory.addProduct("Gaming Laptop", "Electronics", 1299.99, 10);
        var mouse = inventory.addProduct("Wireless Mouse", "Electronics", 29.99, 50);
        var keyboard = inventory.addProduct("Mechanical Keyboard", "Electronics", 89.99, 25);
        var book = inventory.addProduct("Java Programming", "Books", 49.99, 100);
        var headphones = inventory.addProduct("Noise Cancelling Headphones", "Electronics", 199.99, 15);
        System.out.println("  Added " + inventory.getTotalProducts() + " products");

        // ─── Users ───
        var alice = new User("USR-1", "Alice", "alice@email.com", "123 Main St");
        var bob = new User("USR-2", "Bob", "bob@email.com", "456 Oak Ave");

        // ─── Cart Operations ───
        System.out.println("\n--- Cart Operations ---");
        cartManager.addToCart(alice.userId(), laptop.productId(), 1);
        cartManager.addToCart(alice.userId(), mouse.productId(), 2);
        cartManager.addToCart(alice.userId(), book.productId(), 1);
        var aliceCart = cartManager.getCart(alice.userId());
        System.out.println("  Alice's cart: " + aliceCart.items().size() + " items");
        System.out.println("  Total: $" + String.format("%.2f", aliceCart.total()));
        for (var item : aliceCart.items()) {
            System.out.printf("    %s x%d = $%.2f%n", item.productName(), item.quantity(), item.subtotal());
        }

        // ─── Checkout & Payment ───
        System.out.println("\n--- Checkout & Payment ---");
        var result = orderManager.checkoutAndPay(alice.userId(), aliceCart, alice.address(), "Credit Card");
        System.out.println("  Order: " + result.first().orderId() + " - Status: " + result.first().status());
        System.out.println("  Payment: " + result.second().paymentId() + " - Success: " + result.second().success());

        if (result.first().status() == OrderStatus.CONFIRMED) {
            // ─── Order Workflow ───
            System.out.println("\n--- Order Workflow ---");
            var shipped = orderManager.shipOrder(result.first().orderId());
            System.out.println("  Shipped: " + shipped.orderId() + " - Status: " + shipped.status());

            var delivered = orderManager.deliverOrder(result.first().orderId());
            System.out.println("  Delivered: " + delivered.orderId() + " - Status: " + delivered.status());
        }

        // ─── Bob's Shopping ───
        System.out.println("\n--- Bob's Purchase ---");
        cartManager.addToCart(bob.userId(), keyboard.productId(), 1);
        cartManager.addToCart(bob.userId(), headphones.productId(), 1);
        var bobCart = cartManager.getCart(bob.userId());
        System.out.println("  Bob's cart total: $" + String.format("%.2f", bobCart.total()));

        var bobResult = orderManager.checkoutAndPay(bob.userId(), bobCart, bob.address(), "PayPal");
        System.out.println("  Bob's order: " + bobResult.first().orderId() + " - " + bobResult.first().status());

        // ─── Inventory Check ───
        System.out.println("\n--- Inventory After Orders ---");
        for (var p : inventory.getAllProducts()) {
            System.out.printf("  %-30s %d in stock%n", p.name(), p.stockQuantity());
        }

        // ─── Order History ───
        System.out.println("\n--- Alice's Order History ---");
        for (var o : orderManager.getOrdersByUser(alice.userId())) {
            System.out.printf("  %s: %s - $%.2f (%s -> %s)%n",
                o.orderId(), o.status(), o.totalAmount(), o.createdAt(), o.updatedAt());
            for (var item : o.items()) {
                System.out.printf("    %s x%d $%.2f%n", item.productName(), item.quantity(), item.subtotal());
            }
        }

        // ─── Cancellation ───
        System.out.println("\n--- Order Cancellation ---");
        // Create a cart and checkout for cancellation demo
        cartManager.addToCart(alice.userId(), mouse.productId(), 1);
        var tempCart = cartManager.getCart(alice.userId());
        var cancelResult = orderManager.checkoutAndPay(alice.userId(), tempCart, alice.address(), "Debit Card");

        if (cancelResult.first().status() == OrderStatus.CONFIRMED) {
            var cancelled = orderManager.cancelOrder(cancelResult.first().orderId());
            System.out.println("  Cancelled: " + cancelled.orderId() + " - Status: " + cancelled.status());
        } else {
            System.out.println("  Order failed (payment declined), auto-cancelled");
        }

        // ─── Concurrent Checkout ───
        System.out.println("\n--- Concurrent Checkout (Virtual Threads) ---");
        var concurrentInventory = new InventoryManager();
        var concurrentPayment = new PaymentProcessor();
        var concurrentCart = new CartManager(concurrentInventory);
        var concurrentOrders = new OrderManager(concurrentInventory, concurrentPayment);

        var prod = concurrentInventory.addProduct("Popular Item", "General", 99.99, 5);
        var users = new User[10];
        var orderIds = new ConcurrentLinkedQueue<String>();
        var failures = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            users[i] = new User("USR-C-" + i, "User" + i, "u" + i + "@x.com", "Addr" + i);
        }

        var threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            int idx = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                var u = users[idx];
                concurrentCart.addToCart(u.userId(), prod.productId(), 1);
                var c = concurrentCart.getCart(u.userId());
                try {
                    var r = concurrentOrders.checkoutAndPay(u.userId(), c, u.address(), "Card");
                    orderIds.add(r.first().orderId() + ":" + r.first().status());
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }
        for (var t : threads) t.join();

        System.out.println("  Successful orders: " + orderIds.size());
        System.out.println("  Failed: " + failures.get());
        System.out.println("  Remaining stock: " + concurrentInventory.getProduct(prod.productId())
            .map(Product::stockQuantity).orElse(0));

        System.out.println("\n=== E-Commerce Backend Complete ===");
    }
}
