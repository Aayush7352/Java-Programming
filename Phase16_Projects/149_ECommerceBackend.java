package phase16.projects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

final class ECommerceBackend {

    public static record Product(String productId, String name, String category,
                                  BigDecimal price, int stockQuantity, String description) {
        public Product {
            Objects.requireNonNull(productId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(category);
            Objects.requireNonNull(price);
            Objects.requireNonNull(description);
            if (price.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Price cannot be negative");
            if (stockQuantity < 0)
                throw new IllegalArgumentException("Stock cannot be negative");
        }

        public Product withStock(int newStock) {
            return new Product(productId, name, category, price, newStock, description);
        }

        public boolean inStock() { return stockQuantity > 0; }
    }

    public static record CartItem(String productId, String productName, BigDecimal unitPrice,
                                   int quantity) {
        public CartItem {
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        }

        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        public CartItem withQuantity(int newQuantity) {
            return new CartItem(productId, productName, unitPrice, newQuantity);
        }
    }

    public static final class ShoppingCart {
        private final String cartId;
        private final String userId;
        private final Map<String, CartItem> items = new ConcurrentHashMap<>();

        public ShoppingCart(String cartId, String userId) {
            this.cartId = Objects.requireNonNull(cartId);
            this.userId = Objects.requireNonNull(userId);
        }

        public void addItem(Product product, int quantity) {
            items.merge(product.productId(),
                    new CartItem(product.productId(), product.name(), product.price(), quantity),
                    (existing, incoming) -> existing.withQuantity(existing.quantity() + quantity));
        }

        public boolean removeItem(String productId) {
            return items.remove(productId) != null;
        }

        public boolean updateQuantity(String productId, int newQuantity) {
            var existing = items.get(productId);
            if (existing == null) return false;
            if (newQuantity <= 0) {
                items.remove(productId);
            } else {
                items.put(productId, existing.withQuantity(newQuantity));
            }
            return true;
        }

        public List<CartItem> getItems() { return List.copyOf(items.values()); }
        public int itemCount() { return items.values().stream().mapToInt(CartItem::quantity).sum(); }

        public BigDecimal calculateTotal() {
            return items.values().stream()
                    .map(CartItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        public void clear() { items.clear(); }
        public boolean isEmpty() { return items.isEmpty(); }
        public String getCartId() { return cartId; }
        public String getUserId() { return userId; }
    }

    public static sealed interface OrderState permits PendingPayment, Confirmed, Processing,
            Shipped, Delivered, Cancelled, Refunded {
        String displayName();
        boolean canTransitionTo(OrderState next);
    }

    public static record PendingPayment() implements OrderState {
        @Override public String displayName() { return "Pending Payment"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Confirmed || next instanceof Cancelled;
        }
    }

    public static record Confirmed() implements OrderState {
        @Override public String displayName() { return "Confirmed"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Processing || next instanceof Cancelled;
        }
    }

    public static record Processing() implements OrderState {
        @Override public String displayName() { return "Processing"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Shipped || next instanceof Cancelled;
        }
    }

    public static record Shipped(String trackingNumber, String carrier) implements OrderState {
        @Override public String displayName() { return "Shipped"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Delivered;
        }
    }

    public static record Delivered(LocalDateTime deliveredAt) implements OrderState {
        @Override public String displayName() { return "Delivered"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Refunded;
        }
    }

    public static record Cancelled(String reason, LocalDateTime cancelledAt) implements OrderState {
        @Override public String displayName() { return "Cancelled"; }
        @Override public boolean canTransitionTo(OrderState next) {
            return next instanceof Refunded;
        }
    }

    public static record Refunded(LocalDateTime refundedAt, BigDecimal refundAmount) implements OrderState {
        @Override public String displayName() { return "Refunded"; }
        @Override public boolean canTransitionTo(OrderState next) { return false; }
    }

    public static final class Order {
        private final String orderId;
        private final String userId;
        private final List<CartItem> orderedItems;
        private final BigDecimal totalAmount;
        private final LocalDateTime createdAt;
        private OrderState state;
        private final List<OrderState> stateHistory = new CopyOnWriteArrayList<>();

        public Order(String orderId, String userId, List<CartItem> orderedItems,
                     BigDecimal totalAmount) {
            this.orderId = Objects.requireNonNull(orderId);
            this.userId = Objects.requireNonNull(userId);
            this.orderedItems = List.copyOf(orderedItems);
            this.totalAmount = totalAmount;
            this.createdAt = LocalDateTime.now();
            this.state = new PendingPayment();
            this.stateHistory.add(this.state);
        }

        public boolean transitionTo(OrderState newState) {
            if (state.canTransitionTo(newState)) {
                state = newState;
                stateHistory.add(newState);
                return true;
            }
            return false;
        }

        public OrderState getState() { return state; }
        public String getOrderId() { return orderId; }
        public String getUserId() { return userId; }
        public List<CartItem> getOrderedItems() { return orderedItems; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<OrderState> getStateHistory() { return List.copyOf(stateHistory); }

        @Override
        public String toString() {
            return "Order[%s] user=%s total=$%s state=%s items=%d"
                    .formatted(orderId, userId,
                            NumberFormat.getCurrencyInstance(Locale.US).format(totalAmount),
                            state.displayName(), orderedItems.size());
        }
    }

    public static final class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String message;

        public PaymentResult(boolean success, String transactionId, String message) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String transactionId() { return transactionId; }
        public String message() { return message; }
    }

    public static sealed interface PaymentMethod permits CreditCard, PayPal, CryptoWallet {
        String methodName();
        PaymentResult processPayment(BigDecimal amount, String orderId);
    }

    public static record CreditCard(String cardNumber, String cardHolder, String expiry,
                                     String cvv) implements PaymentMethod {
        @Override
        public String methodName() { return "Credit Card"; }

        @Override
        public PaymentResult processPayment(BigDecimal amount, String orderId) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                return new PaymentResult(false, null, "Invalid amount");
            if (cardNumber.length() < 13)
                return new PaymentResult(false, null, "Invalid card number");
            var txnId = "CC-" + orderId + "-" + System.currentTimeMillis();
            return new PaymentResult(true, txnId,
                    "Charged $%s to card ending in %s".formatted(amount, cardNumber.substring(cardNumber.length() - 4)));
        }
    }

    public static record PayPal(String email) implements PaymentMethod {
        @Override public String methodName() { return "PayPal"; }

        @Override
        public PaymentResult processPayment(BigDecimal amount, String orderId) {
            var txnId = "PP-" + orderId + "-" + System.currentTimeMillis();
            return new PaymentResult(true, txnId,
                    "Charged $%s via PayPal account %s".formatted(amount, email));
        }
    }

    public static record CryptoWallet(String walletAddress, String currency) implements PaymentMethod {
        @Override public String methodName() { return "Crypto (" + currency + ")"; }

        @Override
        public PaymentResult processPayment(BigDecimal amount, String orderId) {
            var txnId = "CR-" + orderId + "-" + System.currentTimeMillis();
            return new PaymentResult(true, txnId,
                    "Charged $%s equivalent in %s to wallet %s"
                            .formatted(amount, currency, walletAddress.substring(0, 8) + "..."));
        }
    }

    public static final class InventoryManager {
        private final Map<String, Product> products = new ConcurrentHashMap<>();
        private final AtomicLong productCounter = new AtomicLong(0);

        public Product registerProduct(String name, String category, BigDecimal price, int stock, String description) {
            var id = "PROD-" + productCounter.incrementAndGet();
            var product = new Product(id, name, category, price, stock, description);
            products.put(id, product);
            return product;
        }

        public synchronized boolean reserveStock(String productId, int quantity) {
            var product = products.get(productId);
            if (product == null) return false;
            if (product.stockQuantity() < quantity) return false;
            products.put(productId, product.withStock(product.stockQuantity() - quantity));
            return true;
        }

        public synchronized void releaseStock(String productId, int quantity) {
            var product = products.get(productId);
            if (product != null) {
                products.put(productId, product.withStock(product.stockQuantity() + quantity));
            }
        }

        public synchronized boolean restock(String productId, int quantity) {
            var product = products.get(productId);
            if (product == null) return false;
            products.put(productId, product.withStock(product.stockQuantity() + quantity));
            return true;
        }

        public Product getProduct(String id) { return products.get(id); }
        public List<Product> getAllProducts() { return List.copyOf(products.values()); }
        public List<Product> getProductsByCategory(String category) {
            return products.values().stream()
                    .filter(p -> p.category().equalsIgnoreCase(category))
                    .collect(Collectors.toUnmodifiableList());
        }

        public List<Product> getLowStockProducts(int threshold) {
            return products.values().stream()
                    .filter(p -> p.stockQuantity() <= threshold)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    public static final class OrderService {
        private final Map<String, Order> orders = new ConcurrentHashMap<>();
        private final AtomicLong orderCounter = new AtomicLong(0);
        private final InventoryManager inventory;

        public OrderService(InventoryManager inventory) {
            this.inventory = inventory;
        }

        public Order placeOrder(String userId, ShoppingCart cart, PaymentMethod payment) {
            if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

            var items = cart.getItems();
            for (var item : items) {
                var product = inventory.getProduct(item.productId());
                if (product == null) throw new IllegalArgumentException("Product not found: " + item.productId());
                if (product.stockQuantity() < item.quantity())
                    throw new IllegalStateException("Insufficient stock for " + product.name());
            }

            for (var item : items) {
                inventory.reserveStock(item.productId(), item.quantity());
            }

            var orderId = "ORD-" + orderCounter.incrementAndGet();
            var total = cart.calculateTotal();
            var paymentResult = payment.processPayment(total, orderId);

            Order order;
            if (paymentResult.isSuccess()) {
                order = new Order(orderId, userId, items, total);
                order.transitionTo(new Confirmed());
                cart.clear();
            } else {
                for (var item : items) {
                    inventory.releaseStock(item.productId(), item.quantity());
                }
                order = new Order(orderId, userId, items, total);
                order.transitionTo(new Cancelled("Payment failed: " + paymentResult.message(), LocalDateTime.now()));
            }

            orders.put(orderId, order);
            return order;
        }

        public boolean updateOrderState(String orderId, OrderState newState) {
            var order = orders.get(orderId);
            if (order == null) return false;
            return order.transitionTo(newState);
        }

        public Order getOrder(String orderId) { return orders.get(orderId); }
        public List<Order> getOrdersByUser(String userId) {
            return orders.values().stream()
                    .filter(o -> o.getUserId().equals(userId))
                    .collect(Collectors.toUnmodifiableList());
        }

        public List<Order> getOrdersByState(OrderState state) {
            return orders.values().stream()
                    .filter(o -> o.getState().equals(state))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== E-Commerce Backend ===%n".formatted());

        var inventory = new InventoryManager();
        var orderService = new OrderService(inventory);

        var laptop = inventory.registerProduct("Laptop Pro", "Electronics", new BigDecimal("1299.99"), 10,
                "High-performance laptop with 32GB RAM");
        var mouse = inventory.registerProduct("Wireless Mouse", "Electronics", new BigDecimal("49.99"), 50,
                "Ergonomic wireless mouse");
        var book = inventory.registerProduct("Java 21 Programming", "Books", new BigDecimal("59.99"), 100,
                "Comprehensive Java 21 guide");
        var headphones = inventory.registerProduct("Noise Canceling Headphones", "Electronics",
                new BigDecimal("299.99"), 25, "Premium ANC headphones");

        System.out.println("--- Inventory ---");
        inventory.getAllProducts().forEach(p ->
            System.out.println("  %s: $%-7s (stock: %d)".formatted(p.name(), p.price(), p.stockQuantity())));

        System.out.println("%n--- Shopping Cart ---%n".formatted());
        var cart = new ShoppingCart("CART-001", "USER-001");
        cart.addItem(laptop, 1);
        cart.addItem(mouse, 2);
        cart.addItem(book, 1);
        System.out.println("  Cart items: " + cart.itemCount());
        System.out.println("  Cart total: " + NumberFormat.getCurrencyInstance(Locale.US).format(cart.calculateTotal()));

        System.out.println("%n--- Order with Credit Card ---%n".formatted());
        var cc = new CreditCard("4111111111111111", "Alice Johnson", "12/26", "123");
        var order1 = orderService.placeOrder("USER-001", cart, cc);
        System.out.println("  " + order1);

        System.out.println("%n--- Order Workflow (state transitions) ---%n".formatted());
        orderService.updateOrderState(order1.getOrderId(), new Processing());
        System.out.println("  After Processing: " + order1.getState().displayName());

        orderService.updateOrderState(order1.getOrderId(), new Shipped("1Z999AA10123456784", "UPS"));
        System.out.println("  After Shipped: " + order1.getState().displayName());

        orderService.updateOrderState(order1.getOrderId(), new Delivered(LocalDateTime.now()));
        System.out.println("  After Delivered: " + order1.getState().displayName());

        System.out.println("%n--- Second Order with PayPal ---%n".formatted());
        var cart2 = new ShoppingCart("CART-002", "USER-002");
        cart2.addItem(headphones, 1);
        cart2.addItem(book, 2);
        var paypal = new PayPal("bob@email.com");
        var order2 = orderService.placeOrder("USER-002", cart2, paypal);
        System.out.println("  " + order2);

        System.out.println("%n--- Payment Methods (sealed interface) ---%n".formatted());
        var crypto = new CryptoWallet("0xab5801a7d398351b8be11c439e05c5b3259aec9b", "ETH");
        var result = crypto.processPayment(new BigDecimal("500.00"), "ORD-TEST");
        System.out.println("  Crypto payment: " + result.message());

        System.out.println("%n--- Invalid State Transition ---%n".formatted());
        var invalid = order1.transitionTo(new PendingPayment());
        System.out.println("  Can transition Delivered -> PendingPayment? " + invalid);
        var cancelAttempt = order1.transitionTo(new Cancelled("Test", LocalDateTime.now()));
        System.out.println("  Can transition Delivered -> Cancelled? " + cancelAttempt);

        System.out.println("%n--- Pattern Matching on Orders ---%n".formatted());
        for (var order : List.of(order1, order2)) {
            switch (order.getState()) {
                case Delivered d ->
                    System.out.println("  Delivered at " + d.deliveredAt());
                case Shipped s ->
                    System.out.println("  Shipped via " + s.carrier() + " tracking: " + s.trackingNumber());
                case Confirmed c ->
                    System.out.println("  Confirmed");
                case Processing p ->
                    System.out.println("  Processing");
                case PendingPayment p ->
                    System.out.println("  Awaiting payment");
                case Cancelled c ->
                    System.out.println("  Cancelled: " + c.reason());
                case Refunded r ->
                    System.out.println("  Refunded: $" + r.refundAmount());
            }
        }

        System.out.println("%n--- Inventory After Orders ---%n".formatted());
        System.out.println("  Laptop stock: " + inventory.getProduct(laptop.productId()).stockQuantity());
        System.out.println("  Mouse stock: " + inventory.getProduct(mouse.productId()).stockQuantity());
        System.out.println("  Book stock: " + inventory.getProduct(book.productId()).stockQuantity());
        System.out.println("  Headphones stock: " + inventory.getProduct(headphones.productId()).stockQuantity());

        System.out.println("%n--- Low Stock Alert ---%n".formatted());
        var lowStock = inventory.getLowStockProducts(15);
        lowStock.forEach(p -> System.out.println("  LOW STOCK: " + p.name() + " (" + p.stockQuantity() + " remaining)"));

        System.out.println("%n--- Virtual Threads: Concurrent Orders ---%n".formatted());
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        var c = new ShoppingCart("VT-CART-" + idx, "VT-USER-" + idx);
                        c.addItem(laptop, 1);
                        c.addItem(mouse, 1);
                        var pm = new PayPal("vt" + idx + "@test.com");
                        var o = orderService.placeOrder("VT-USER-" + idx, c, pm);
                        System.out.println("  [VT-%d] Order %s placed".formatted(idx, o.getOrderId()));
                    } catch (Exception e) {
                        System.out.println("  [VT-%d] Error: %s".formatted(idx, e.getMessage()));
                    }
                });
            }
        }

        System.out.println("%nFinal Stats: %d products, %d orders"
                .formatted(inventory.getAllProducts().size(),
                        orderService.getOrdersByUser("USER-001").size() + orderService.getOrdersByUser("USER-002").size()));
        System.out.println("=== Done ===");
    }
}
