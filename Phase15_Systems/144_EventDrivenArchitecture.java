package phase15.systems;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

class _144_EventDrivenArchitecture {

    public record DomainEvent(String name, Object payload, long timestamp) {
        public DomainEvent(String name, Object payload) {
            this(name, payload, System.currentTimeMillis());
        }
    }

    @FunctionalInterface
    public interface EventHandler<E> extends Consumer<E> {}

    public static class EventBus {
        private final Map<String, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>();
        private final ExecutorService defaultExecutor = Executors.newVirtualThreadPerTaskExecutor();

        private record RegisteredHandler(EventHandler<DomainEvent> handler, ExecutorService executor) {}

        public void subscribe(String eventName, EventHandler<DomainEvent> handler) {
            subscribe(eventName, handler, defaultExecutor);
        }

        public void subscribe(String eventName, EventHandler<DomainEvent> handler, ExecutorService executor) {
            handlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
                    .add(new RegisteredHandler(handler, executor));
        }

        public void publish(DomainEvent event) {
            var subs = handlers.get(event.name());
            if (subs == null) return;
            for (var reg : subs) {
                reg.executor().submit(() -> {
                    try {
                        reg.handler().accept(event);
                    } catch (Exception e) {
                        System.err.println("Error handling event " + event.name() + ": " + e.getMessage());
                    }
                });
            }
        }

        public void publishSync(DomainEvent event) {
            var subs = handlers.get(event.name());
            if (subs == null) return;
            for (var reg : subs) {
                reg.handler().accept(event);
            }
        }

        public void unsubscribe(String eventName, EventHandler<DomainEvent> handler) {
            var subs = handlers.get(eventName);
            if (subs != null) subs.removeIf(r -> r.handler() == handler);
        }

        public void shutdown() {
            defaultExecutor.shutdown();
        }
    }

    // --- Domain events example ---

    public record OrderCreated(String orderId, String customerId, double amount) {}
    public record PaymentProcessed(String orderId, boolean success) {}
    public record EmailSent(String to, String subject) {}

    public static class OrderService {
        private final EventBus eventBus;

        public OrderService(EventBus eventBus) { this.eventBus = eventBus; }

        public void createOrder(String orderId, String customerId, double amount) {
            System.out.println("[OrderService] Creating order " + orderId);
            eventBus.publish(new DomainEvent("order.created", new OrderCreated(orderId, customerId, amount)));
        }
    }

    public static class PaymentService {
        public PaymentService(EventBus eventBus) {
            eventBus.subscribe("order.created", event -> {
                var order = (OrderCreated) event.payload();
                System.out.println("[PaymentService] Processing payment for order " + order.orderId());
                boolean success = order.amount() <= 1000;
                eventBus.publish(new DomainEvent("payment.processed", new PaymentProcessed(order.orderId(), success)));
            });
        }
    }

    public static class NotificationService {
        public NotificationService(EventBus eventBus) {
            eventBus.subscribe("payment.processed", event -> {
                var payment = (PaymentProcessed) event.payload();
                if (payment.success()) {
                    System.out.println("[NotificationService] Sending confirmation for order " + payment.orderId());
                    eventBus.publish(new DomainEvent("email.sent", new EmailSent("customer@example.com", "Order confirmed")));
                } else {
                    System.out.println("[NotificationService] Sending failure notice for order " + payment.orderId());
                }
            });
        }
    }

    public static class AuditService {
        public AuditService(EventBus eventBus) {
            eventBus.subscribe("order.created", event -> {
                var order = (OrderCreated) event.payload();
                System.out.println("[AuditService] Logging order creation: " + order);
            });
            eventBus.subscribe("payment.processed", event -> {
                System.out.println("[AuditService] Logging payment: " + event.payload());
            });
        }
    }

    public static void main(String[] args) throws Exception {
        var eventBus = new EventBus();

        // Wire up domain services
        var orderSvc = new OrderService(eventBus);
        new PaymentService(eventBus);
        new NotificationService(eventBus);
        new AuditService(eventBus);

        System.out.println("=== Event-Driven Architecture ===\n");

        // Create orders
        orderSvc.createOrder("ORD-001", "CUST-100", 250.0);
        orderSvc.createOrder("ORD-002", "CUST-200", 1500.0);

        // Wait for async processing
        Thread.sleep(1000);

        System.out.println("\n--- Sync event test ---");
        eventBus.publishSync(new DomainEvent("order.created", new OrderCreated("ORD-SYNC", "CUST-SYNC", 99.0)));

        eventBus.shutdown();
        System.out.println("\nDone.");
    }
}
