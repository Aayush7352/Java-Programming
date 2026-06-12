package phase14.designpatterns;

import java.util.concurrent.atomic.AtomicLong;

// Singleton Pattern: Eager, Lazy, Thread-safe (synchronized), Bill Pugh (holder), Enum

// 1. Eager Singleton
class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    private final AtomicLong counter = new AtomicLong(0);

    private EagerSingleton() {
        System.out.println("  [EagerSingleton] Constructor called (instance created at class load time)");
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }

    public long nextId() {
        return counter.incrementAndGet();
    }
}

// 2. Lazy Singleton (not thread-safe)
class LazySingleton {
    private static LazySingleton instance;
    private final AtomicLong counter = new AtomicLong(0);

    private LazySingleton() {
        System.out.println("  [LazySingleton] Constructor called (lazy initialization)");
    }

    public static LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }

    public long nextId() {
        return counter.incrementAndGet();
    }
}

// 3. Thread-safe Singleton (synchronized)
class ThreadSafeSingleton {
    private static ThreadSafeSingleton instance;
    private final AtomicLong counter = new AtomicLong(0);

    private ThreadSafeSingleton() {
        System.out.println("  [ThreadSafeSingleton] Constructor called (synchronized)");
    }

    public static synchronized ThreadSafeSingleton getInstance() {
        if (instance == null) {
            instance = new ThreadSafeSingleton();
        }
        return instance;
    }

    // Double-checked locking variant
    public static ThreadSafeSingleton getInstanceDCL() {
        if (instance == null) {
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) {
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }

    public long nextId() {
        return counter.incrementAndGet();
    }
}

// 4. Bill Pugh Singleton Holder pattern (initialization-on-demand holder idiom)
class BillPughSingleton {
    private final AtomicLong counter = new AtomicLong(0);

    private BillPughSingleton() {
        System.out.println("  [BillPughSingleton] Constructor called (holder pattern)");
    }

    // Inner static helper class - loaded only when getInstance() is called
    private static class SingletonHolder {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public long nextId() {
        return counter.incrementAndGet();
    }
}

// 5. Enum Singleton (Java's most concise, serialization-safe singleton)
enum EnumSingleton {
    INSTANCE;

    private final AtomicLong counter = new AtomicLong(0);

    EnumSingleton() {
        System.out.println("  [EnumSingleton] Constructor called (enum, JVM guarantees single instance)");
    }

    public long nextId() {
        return counter.incrementAndGet();
    }
}

public class SingletonPattern {
    public static void main(String[] args) {
        System.out.println("=== Singleton Pattern Demo ===\n");

        // 1. Eager Singleton
        System.out.println("1. Eager Singleton:");
        var eager1 = EagerSingleton.getInstance();
        var eager2 = EagerSingleton.getInstance();
        System.out.println("   Same instance? " + (eager1 == eager2));
        System.out.println("   ID: " + eager1.nextId() + ", " + eager2.nextId());

        // 2. Lazy Singleton
        System.out.println("\n2. Lazy Singleton (not thread-safe):");
        var lazy1 = LazySingleton.getInstance();
        var lazy2 = LazySingleton.getInstance();
        System.out.println("   Same instance? " + (lazy1 == lazy2));

        // 3. Thread-safe Singleton
        System.out.println("\n3. Thread-safe Singleton (synchronized):");
        var safe1 = ThreadSafeSingleton.getInstance();
        var safe2 = ThreadSafeSingleton.getInstance();
        System.out.println("   Same instance? " + (safe1 == safe2));

        // 4. Bill Pugh Singleton
        System.out.println("\n4. Bill Pugh Singleton (holder pattern):");
        var holder1 = BillPughSingleton.getInstance();
        var holder2 = BillPughSingleton.getInstance();
        System.out.println("   Same instance? " + (holder1 == holder2));
        System.out.println("   ID: " + holder1.nextId() + ", " + holder2.nextId());

        // 5. Enum Singleton
        System.out.println("\n5. Enum Singleton:");
        var enum1 = EnumSingleton.INSTANCE;
        var enum2 = EnumSingleton.INSTANCE;
        System.out.println("   Same instance? " + (enum1 == enum2));
        System.out.println("   ID: " + enum1.nextId() + ", " + enum2.nextId());

        // Verification: all return the same object within each pattern
        System.out.println("\n--- Verification ---");
        System.out.println("All patterns ensure only ONE instance exists per class loader.");

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Eager Singleton - instance created at class loading (simple, always initialized)");
        System.out.println("Lazy Singleton - instance created on first access (not thread-safe)");
        System.out.println("Thread-safe Singleton - synchronized getInstance() or double-checked locking");
        System.out.println("Bill Pugh (Holder) - inner static class loaded on demand (thread-safe without sync overhead)");
        System.out.println("Enum Singleton - simplest, serialization-safe, reflection-proof (Effective Java)");
    }
}
