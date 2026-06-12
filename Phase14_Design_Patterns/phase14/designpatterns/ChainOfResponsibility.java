package phase14.designpatterns;

import java.util.List;

// Chain of Responsibility: Handler abstract class, concrete handlers, successor chain, request processing

// Request model (sealed for Java 21)
sealed interface SupportRequest permits SimpleRequest, EscalatedRequest, PriorityRequest {
    String getDescription();
    int getLevel();
}

record SimpleRequest(String description) implements SupportRequest {
    @Override
    public String getDescription() { return description(); }
    @Override
    public int getLevel() { return 1; }
}

record EscalatedRequest(String description) implements SupportRequest {
    @Override
    public String getDescription() { return description(); }
    @Override
    public int getLevel() { return 2; }
}

record PriorityRequest(String description, int priority) implements SupportRequest {
    @Override
    public String getDescription() { return description; }
    @Override
    public int getLevel() { return 3; }
}

// Handler abstract class
abstract class SupportHandler {
    protected SupportHandler nextHandler;

    // Set the next handler in the chain (fluent)
    public SupportHandler setNext(SupportHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler;
    }

    // Template method for handling requests
    public void handle(SupportRequest request) {
        if (canHandle(request)) {
            process(request);
        } else if (nextHandler != null) {
            System.out.println("  [" + getHandlerName() + "] Cannot handle level " + request.getLevel()
                    + ", passing to " + nextHandler.getHandlerName());
            nextHandler.handle(request);
        } else {
            System.out.println("  [" + getHandlerName() + "] No one can handle: " + request.getDescription());
            System.out.println("  -> Request falls through the chain unhandled!");
        }
    }

    protected abstract boolean canHandle(SupportRequest request);
    protected abstract void process(SupportRequest request);
    protected abstract String getHandlerName();
}

// Concrete handlers
class Level1Support extends SupportHandler {
    private int handledCount = 0;

    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getLevel() == 1;
    }

    @Override
    protected void process(SupportRequest request) {
        handledCount++;
        System.out.println("  [Level 1 Support] Handling: " + request.getDescription());
        System.out.println("    -> Solution: Reset password / Check documentation / Restart application");
    }

    @Override
    protected String getHandlerName() { return "Level 1 Support"; }

    public int getHandledCount() { return handledCount; }
}

class Level2Support extends SupportHandler {
    private int handledCount = 0;

    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getLevel() == 2;
    }

    @Override
    protected void process(SupportRequest request) {
        handledCount++;
        System.out.println("  [Level 2 Support] Handling: " + request.getDescription());
        System.out.println("    -> Solution: Escalating to engineering team / Debugging logs");
    }

    @Override
    protected String getHandlerName() { return "Level 2 Support"; }

    public int getHandledCount() { return handledCount; }
}

class Level3Support extends SupportHandler {
    private int handledCount = 0;

    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getLevel() == 3;
    }

    @Override
    protected void process(SupportRequest request) {
        handledCount++;
        if (request instanceof PriorityRequest pr) {
            System.out.println("  [Level 3 Support - Priority " + pr.priority() + "] Handling: " + request.getDescription());
            System.out.println("    -> Solution: Hotfix deployment / Database rollback / Code review");
        } else {
            System.out.println("  [Level 3 Support] Handling: " + request.getDescription());
            System.out.println("    -> Solution: Core system fix / Architecture change");
        }
    }

    @Override
    protected String getHandlerName() { return "Level 3 Support"; }

    public int getHandledCount() { return handledCount; }
}

// Manager as final level
class ManagerSupport extends SupportHandler {
    @Override
    protected boolean canHandle(SupportRequest request) {
        return true; // Manager handles everything that reaches them
    }

    @Override
    protected void process(SupportRequest request) {
        System.out.println("  [Manager] Handling: " + request.getDescription());
        System.out.println("    -> Solution: Direct intervention / Customer compensation");
    }

    @Override
    protected String getHandlerName() { return "Manager"; }
}

// Another example: Logging chain
enum LogLevel { DEBUG, INFO, WARNING, ERROR }

record LogMessage(LogLevel level, String message) {}

abstract class LogHandler {
    protected LogHandler nextHandler;
    protected LogLevel level;

    public LogHandler(LogLevel level) {
        this.level = level;
    }

    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handle(LogMessage message) {
        if (message.level().ordinal() <= level.ordinal()) {
            write(message);
        }
        if (nextHandler != null) {
            nextHandler.handle(message);
        }
    }

    protected abstract void write(LogMessage message);
}

class ConsoleLogger extends LogHandler {
    public ConsoleLogger(LogLevel level) { super(level); }
    @Override
    protected void write(LogMessage message) {
        System.out.println("  [Console: " + message.level() + "] " + message.message());
    }
}

class FileLogger extends LogHandler {
    public FileLogger(LogLevel level) { super(level); }
    @Override
    protected void write(LogMessage message) {
        System.out.println("  [File: " + message.level() + "] " + message.message() + " (written to log.txt)");
    }
}

class EmailLogger extends LogHandler {
    public EmailLogger(LogLevel level) { super(level); }
    @Override
    protected void write(LogMessage message) {
        System.out.println("  [Email Alert: " + message.level() + "] " + message.message() + " (sent to admin@company.com)");
    }
}

// Request processing with middleware chain
interface Middleware {
    Middleware setNext(Middleware next);
    boolean process(String request);
}

abstract class BaseMiddleware implements Middleware {
    private Middleware next;

    @Override
    public Middleware setNext(Middleware next) {
        this.next = next;
        return next;
    }

    protected boolean processNext(String request) {
        if (next == null) return true;
        return next.process(request);
    }
}

class AuthenticationMiddleware extends BaseMiddleware {
    @Override
    public boolean process(String request) {
        if (request == null || !request.contains("token")) {
            System.out.println("  [Middleware] Authentication FAILED: missing token");
            return false;
        }
        System.out.println("  [Middleware] Authentication passed");
        return processNext(request);
    }
}

class AuthorizationMiddleware extends BaseMiddleware {
    @Override
    public boolean process(String request) {
        if (!request.contains("admin")) {
            System.out.println("  [Middleware] Authorization FAILED: admin role required");
            return false;
        }
        System.out.println("  [Middleware] Authorization passed");
        return processNext(request);
    }
}

class RateLimitMiddleware extends BaseMiddleware {
    private int requestCount = 0;
    private static final int MAX_REQUESTS = 5;

    @Override
    public boolean process(String request) {
        requestCount++;
        if (requestCount > MAX_REQUESTS) {
            System.out.println("  [Middleware] Rate limit EXCEEDED (" + MAX_REQUESTS + "/min)");
            return false;
        }
        System.out.println("  [Middleware] Rate limit OK (" + requestCount + "/" + MAX_REQUESTS + ")");
        return processNext(request);
    }
}

public class ChainOfResponsibility {
    public static void main(String[] args) {
        System.out.println("=== Chain of Responsibility Pattern Demo ===\n");

        // 1. Support Ticket System
        System.out.println("1. Support Ticket System:");
        var level1 = new Level1Support();
        var level2 = new Level2Support();
        var level3 = new Level3Support();
        var manager = new ManagerSupport();

        level1.setNext(level2).setNext(level3).setNext(manager);

        var requests = List.<SupportRequest>of(
                new SimpleRequest("Can't remember password"),
                new EscalatedRequest("Account suspended after failed login attempts"),
                new PriorityRequest("Production database corrupted", 1),
                new SimpleRequest("Need to update profile picture"),
                new EscalatedRequest("Payment transaction failed"),
                new SimpleRequest("How to export data?")
        );

        for (var request : requests) {
            System.out.println("\n  --- New Request (" + request.getClass().getSimpleName() + "): "
                    + request.getDescription() + " ---");
            level1.handle(request);
        }

        System.out.println("\n  Summary:");
        System.out.println("    Level 1 handled: " + level1.getHandledCount());
        System.out.println("    Level 2 handled: " + level2.getHandledCount());
        System.out.println("    Level 3 handled: " + level3.getHandledCount());

        // 2. Logging Chain
        System.out.println("\n2. Logging Chain (multiple handlers process same request):");
        var consoleLogger = new ConsoleLogger(LogLevel.DEBUG);
        var fileLogger = new FileLogger(LogLevel.WARNING);
        var emailLogger = new EmailLogger(LogLevel.ERROR);

        consoleLogger.setNext(fileLogger);
        fileLogger.setNext(emailLogger);

        var logMessages = List.of(
                new LogMessage(LogLevel.DEBUG, "Initializing application"),
                new LogMessage(LogLevel.INFO, "User logged in successfully"),
                new LogMessage(LogLevel.WARNING, "High memory usage detected"),
                new LogMessage(LogLevel.ERROR, "Database connection lost!")
        );

        for (var log : logMessages) {
            System.out.println("\n  [Log Handler Chain] Processing: " + log.message());
            consoleLogger.handle(log);
        }

        // 3. Middleware Chain (request processing pipeline)
        System.out.println("\n3. Middleware Chain (request processing pipeline):");
        var auth = new AuthenticationMiddleware();
        var authz = new AuthorizationMiddleware();
        var rateLimit = new RateLimitMiddleware();

        auth.setNext(authz).setNext(rateLimit);

        var middlewareRequests = List.of(
                "request with token and admin role",
                "request with token but no admin",
                "request without token",
                "request with token and admin role",
                "request with token and admin role",
                "request with token and admin role",
                "request with token and admin role",
                "request with token and admin role"
        );

        for (var req : middlewareRequests) {
            System.out.print("\n  [Middleware Chain] Request: '" + req.substring(0, Math.min(20, req.length())) + "...'\n");
            boolean result = auth.process(req);
            System.out.println("  Result: " + (result ? "ALLOWED" : "DENIED"));
        }

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Handler abstract class - defines handle() template and setNext() chain link");
        System.out.println("Concrete handlers - each handles specific request types/levels");
        System.out.println("Successor chain - each handler passes unhandled requests to the next");
        System.out.println("Request processing - request travels through chain until handled");
        System.out.println("Logging chain example - multiple handlers process the same request (vs exclusive)");
        System.out.println("Middleware pipeline example - sequential processing with early termination");
        System.out.println("Flexibility - handlers can be added/removed/reordered without changing client code");
    }
}
