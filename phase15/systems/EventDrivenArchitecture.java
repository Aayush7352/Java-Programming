package phase15.systems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * EventDrivenArchitecture.java
 *
 * Event bus pattern with EventPublisher, EventSubscriber, async event
 * processing, and domain events. Self-contained JDK-only implementation.
 */
public class EventDrivenArchitecture {

    // ═══════════════════════════════════════════════
    // Core Event Classes
    // ═══════════════════════════════════════════════

    sealed interface Event permits DomainEvent, SystemEvent {}

    record DomainEvent(String eventId, String aggregateId, String eventType, Object payload, Instant occurredAt)
        implements Event {}

    record SystemEvent(String eventId, String source, String eventType, Object payload, Instant occurredAt)
        implements Event {}

    // ═══════════════════════════════════════════════
    // Event Subscriber
    // ═══════════════════════════════════════════════

    @FunctionalInterface
    interface EventSubscriber {
        void onEvent(Event event);
    }

    // ═══════════════════════════════════════════════
    // Event Bus
    // ═══════════════════════════════════════════════

    static final class EventBus {
        private final ConcurrentHashMap<String, List<EventSubscriber>> subscribersByType = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<EventSubscriber>> subscribersBySource = new ConcurrentHashMap<>();
        private final List<EventSubscriber> globalSubscribers = new CopyOnWriteArrayList<>();
        private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        private final AtomicLong eventCounter = new AtomicLong(0);
        private final BlockingQueue<Event> deadLetterQueue = new LinkedBlockingQueue<>();

        // Subscribe to specific event type
        public void subscribe(String eventType, EventSubscriber subscriber) {
            subscribersByType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        }

        // Subscribe to events from a specific source
        public void subscribeToSource(String source, EventSubscriber subscriber) {
            subscribersBySource.computeIfAbsent(source, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        }

        // Subscribe to all events
        public void subscribeAll(EventSubscriber subscriber) {
            globalSubscribers.add(subscriber);
        }

        // Unsubscribe
        public void unsubscribe(String eventType, EventSubscriber subscriber) {
            var list = subscribersByType.get(eventType);
            if (list != null) list.remove(subscriber);
        }

        // Publish event synchronously
        public void publish(Event event) {
            eventCounter.incrementAndGet();
            notifySubscribers(event);
        }

        // Publish event asynchronously
        public void publishAsync(Event event) {
            eventCounter.incrementAndGet();
            asyncExecutor.submit(() -> {
                try {
                    notifySubscribers(event);
                } catch (Exception e) {
                    deadLetterQueue.offer(event);
                }
            });
        }

        private void notifySubscribers(Event event) {
            // Global subscribers
            for (var sub : globalSubscribers) {
                safeNotify(sub, event);
            }

            // Type-based subscribers
            String type = switch (event) {
                case DomainEvent de -> de.eventType();
                case SystemEvent se -> se.eventType();
            };
            var typeSubs = subscribersByType.get(type);
            if (typeSubs != null) {
                for (var sub : typeSubs) safeNotify(sub, event);
            }

            // Source-based subscribers
            String source = switch (event) {
                case DomainEvent de -> de.aggregateId();
                case SystemEvent se -> se.source();
            };
            var sourceSubs = subscribersBySource.get(source);
            if (sourceSubs != null) {
                for (var sub : sourceSubs) safeNotify(sub, event);
            }
        }

        private void safeNotify(EventSubscriber sub, Event event) {
            try {
                sub.onEvent(event);
            } catch (Exception e) {
                deadLetterQueue.offer(event);
            }
        }

        public long getEventCount() { return eventCounter.get(); }
        public int getDeadLetterCount() { return deadLetterQueue.size(); }
        public List<Event> drainDeadLetters() {
            var list = new ArrayList<Event>();
            deadLetterQueue.drainTo(list);
            return list;
        }

        public void shutdown() {
            asyncExecutor.shutdown();
        }
    }

    // ═══════════════════════════════════════════════
    // Decorator / Filtered Subscriber
    // ═══════════════════════════════════════════════

    static EventSubscriber filteredSubscriber(Predicate<Event> filter, EventSubscriber delegate) {
        return event -> { if (filter.test(event)) delegate.onEvent(event); };
    }

    static EventSubscriber loggingSubscriber() {
        return event -> System.out.println("    [LOG] " + event.getClass().getSimpleName() +
            " type=" + (switch (event) {
                case DomainEvent de -> de.eventType();
                case SystemEvent se -> se.eventType();
            }) + " at " + (switch (event) {
                case DomainEvent de -> de.occurredAt();
                case SystemEvent se -> se.occurredAt();
            }));
    }

    // ═══════════════════════════════════════════════
    // Demo Domain Services
    // ═══════════════════════════════════════════════

    static final class OrderService {
        private final EventBus eventBus;

        OrderService(EventBus eventBus) { this.eventBus = eventBus; }

        public void placeOrder(String orderId, String customerId, double amount) {
            System.out.println("  [OrderService] Placing order " + orderId);
            var event = new DomainEvent(UUID.randomUUID().toString(), orderId, "OrderPlaced",
                Map.of("customerId", customerId, "amount", amount, "orderId", orderId),
                Instant.now());
            eventBus.publish(event);
        }

        public void cancelOrder(String orderId, String reason) {
            System.out.println("  [OrderService] Cancelling order " + orderId);
            var event = new DomainEvent(UUID.randomUUID().toString(), orderId, "OrderCancelled",
                Map.of("orderId", orderId, "reason", reason),
                Instant.now());
            eventBus.publishAsync(event);
        }
    }

    static final class InventoryService {
        InventoryService(EventBus eventBus) {
            eventBus.subscribe("OrderPlaced", event -> {
                if (event instanceof DomainEvent de) {
                    @SuppressWarnings("unchecked")
                    var payload = (Map<String, Object>) de.payload();
                    System.out.println("  [InventoryService] Reserving stock for order " + payload.get("orderId"));
                }
            });
        }
    }

    static final class NotificationService {
        NotificationService(EventBus eventBus) {
            eventBus.subscribe("OrderPlaced", event -> {
                if (event instanceof DomainEvent de) {
                    @SuppressWarnings("unchecked")
                    var payload = (Map<String, Object>) de.payload();
                    System.out.println("  [NotificationService] Sending confirmation to customer "
                        + payload.get("customerId"));
                }
            });

            eventBus.subscribe("OrderCancelled", event -> {
                if (event instanceof DomainEvent de) {
                    @SuppressWarnings("unchecked")
                    var payload = (Map<String, Object>) de.payload();
                    System.out.println("  [NotificationService] Sending cancellation notice for order "
                        + payload.get("orderId") + " reason: " + payload.get("reason"));
                }
            });
        }
    }

    static final class AuditService {
        AuditService(EventBus eventBus) {
            eventBus.subscribeAll(event -> {
                String desc = switch (event) {
                    case DomainEvent de -> "Domain:" + de.eventType() + " aggregate=" + de.aggregateId();
                    case SystemEvent se -> "System:" + se.eventType() + " source=" + se.source();
                };
                System.out.println("  [AuditService] Auditing event: " + desc);
            });
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Event-Driven Architecture Example ===\n");

        EventBus eventBus = new EventBus();

        // Add logging subscriber
        eventBus.subscribeAll(loggingSubscriber());

        // Register services
        OrderService orderService = new OrderService(eventBus);
        InventoryService inventoryService = new InventoryService(eventBus);
        NotificationService notificationService = new NotificationService(eventBus);
        AuditService auditService = new AuditService(eventBus);

        // Also subscribe to system events
        eventBus.subscribe("SystemStarted", event -> {
            System.out.println("  [Main] System started event received");
        });

        // ─── Publish domain events synchronously ───
        System.out.println("--- Synchronous Domain Events ---");
        orderService.placeOrder("ORD-001", "CUST-123", 299.99);
        System.out.println();

        orderService.placeOrder("ORD-002", "CUST-456", 149.50);
        System.out.println();

        // ─── Publish domain events asynchronously ───
        System.out.println("--- Async Domain Events ---");
        orderService.cancelOrder("ORD-001", "Customer requested");
        Thread.sleep(500);
        System.out.println();

        // ─── System Events ───
        System.out.println("--- System Events ---");
        var sysEvent = new SystemEvent(UUID.randomUUID().toString(), "SystemMonitor", "SystemStarted",
            Map.of("version", "1.0.0", "env", "production"), Instant.now());
        eventBus.publish(sysEvent);
        System.out.println();

        // ─── Filtered Subscriber ───
        System.out.println("--- Filtered Subscriber (high value only) ---");
        eventBus.subscribe("OrderPlaced", filteredSubscriber(
            event -> event instanceof DomainEvent de
                && de.payload() instanceof Map<?, ?> m
                && m.get("amount") instanceof Number n
                && n.doubleValue() > 200,
            event -> System.out.println("  [HighValueNotifier] Order over $200: " +
                ((DomainEvent) event).aggregateId())
        ));

        orderService.placeOrder("ORD-003", "CUST-789", 500.00);
        orderService.placeOrder("ORD-004", "CUST-012", 50.00);
        System.out.println();

        // ─── Event count and dead letters ───
        System.out.println("--- Event Bus Stats ---");
        System.out.println("  Total events published: " + eventBus.getEventCount());
        System.out.println("  Dead letters: " + eventBus.getDeadLetterCount());

        // ─── Virtual Thread Event Processing ───
        System.out.println("\n--- Virtual Thread Concurrent Events ---");
        var vtEventBus = new EventBus();
        var resultCount = new AtomicInteger(0);

        vtEventBus.subscribe("TestEvent", event -> {
            resultCount.incrementAndGet();
            try { Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        var vtThreads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            int id = i;
            vtThreads[i] = Thread.ofVirtual().start(() -> {
                var event = new DomainEvent(UUID.randomUUID().toString(), "agg-" + id, "TestEvent",
                    Map.of("id", id), Instant.now());
                vtEventBus.publishAsync(event);
            });
        }
        for (var t : vtThreads) t.join();
        Thread.sleep(500);
        System.out.println("  Processed " + resultCount.get() + " async events via virtual threads");
        System.out.println("  Total published: " + vtEventBus.getEventCount());

        eventBus.shutdown();
        vtEventBus.shutdown();

        System.out.println("\n=== Event-Driven Architecture Example Complete ===");
    }
}
