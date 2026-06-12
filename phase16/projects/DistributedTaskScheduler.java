package phase16.projects;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * DistributedTaskScheduler.java
 *
 * Task scheduler: ScheduledTask record, priority queue, worker threads
 * (virtual threads), cron expression parser, task persistence, retry mechanism.
 */
public class DistributedTaskScheduler {

    // ═══════════════════════════════════════════════
    // Records & Enums
    // ═══════════════════════════════════════════════

    enum TaskPriority { LOW, MEDIUM, HIGH, CRITICAL }

    enum TaskStatus { SCHEDULED, RUNNING, COMPLETED, FAILED, RETRYING, CANCELLED }

    record ScheduledTask(String taskId, String name, String cronExpression, Runnable action,
                         TaskPriority priority, TaskStatus status, Instant scheduledAt,
                         Instant lastRunAt, int retryCount, int maxRetries, int executionCount) {
        ScheduledTask withStatus(TaskStatus s) {
            return new ScheduledTask(taskId, name, cronExpression, action, priority, s,
                scheduledAt, lastRunAt, retryCount, maxRetries, executionCount);
        }
        ScheduledTask withLastRun(Instant t) {
            return new ScheduledTask(taskId, name, cronExpression, action, priority, status,
                scheduledAt, t, retryCount, maxRetries, executionCount);
        }
        ScheduledTask withRetry(int r) {
            return new ScheduledTask(taskId, name, cronExpression, action, priority, status,
                scheduledAt, lastRunAt, r, maxRetries, executionCount);
        }
        ScheduledTask withExecCount(int c) {
            return new ScheduledTask(taskId, name, cronExpression, action, priority, status,
                scheduledAt, lastRunAt, retryCount, maxRetries, c);
        }

        public boolean shouldRetry() { return retryCount < maxRetries; }

        public Instant nextExecutionTime() {
            try {
                return CronExpressionParser.nextExecution(cronExpression, lastRunAt != null ? lastRunAt : scheduledAt);
            } catch (Exception e) {
                return null;
            }
        }
    }

    record CronExpression(String second, String minute, String hour, String dayOfMonth,
                          String month, String dayOfWeek) {}

    // ═══════════════════════════════════════════════
    // Cron Expression Parser
    // ═══════════════════════════════════════════════

    static final class CronExpressionParser {
        // Simplified cron parser supporting standard 5-field format
        // minute hour day-of-month month day-of-week
        // Supports: numbers, *, */n, comma lists

        public static Instant nextExecution(String expression, Instant after) {
            var parts = expression.trim().split("\\s+");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Cron expression must have 5 fields, got: " + parts.length);
            }

            var cron = new CronExpression("*", parts[0], parts[1], parts[2], parts[3], parts[4]);
            ZonedDateTime zdt = after.atZone(ZoneId.systemDefault());
            // Search for next match within reasonable window
            for (int i = 0; i < 525600; i++) { // 1 year in minutes
                ZonedDateTime next = zdt.plusMinutes(1);
                if (matches(cron, next)) {
                    return next.toInstant();
                }
                zdt = next;
            }
            return null;
        }

        private static boolean matches(CronExpression cron, ZonedDateTime dt) {
            return fieldMatches(cron.minute(), dt.getMinute())
                && fieldMatches(cron.hour(), dt.getHour())
                && fieldMatches(cron.dayOfMonth(), dt.getDayOfMonth())
                && fieldMatches(cron.month(), dt.getMonthValue())
                && fieldMatches(cron.dayOfWeek(), dt.getDayOfWeek().getValue() % 7); // 0=Sun
        }

        private static boolean fieldMatches(String field, int value) {
            if (field.equals("*")) return true;
            // Handle */n
            if (field.startsWith("*/")) {
                int step = Integer.parseInt(field.substring(2));
                return value % step == 0;
            }
            // Handle comma list
            if (field.contains(",")) {
                return Arrays.stream(field.split(","))
                    .anyMatch(f -> fieldMatches(f.trim(), value));
            }
            // Handle range
            if (field.contains("-")) {
                var range = field.split("-");
                int lo = Integer.parseInt(range[0]);
                int hi = Integer.parseInt(range[1]);
                return value >= lo && value <= hi;
            }
            return Integer.parseInt(field) == value;
        }
    }

    // ═══════════════════════════════════════════════
    // Task Persistence (In-Memory)
    // ═══════════════════════════════════════════════

    static final class TaskStore {
        private final ConcurrentHashMap<String, ScheduledTask> tasks = new ConcurrentHashMap<>();

        public void save(ScheduledTask task) { tasks.put(task.taskId(), task); }
        public Optional<ScheduledTask> get(String taskId) { return Optional.ofNullable(tasks.get(taskId)); }
        public boolean remove(String taskId) { return tasks.remove(taskId) != null; }
        public List<ScheduledTask> getAll() { return List.copyOf(tasks.values()); }
        public List<ScheduledTask> getByStatus(TaskStatus status) {
            return tasks.values().stream().filter(t -> t.status() == status).collect(Collectors.toList());
        }
        public int count() { return tasks.size(); }
    }

    // ═══════════════════════════════════════════════
    // Task Scheduler Engine
    // ═══════════════════════════════════════════════

    static final class TaskSchedulerEngine implements AutoCloseable {
        private final TaskStore store;
        private final PriorityBlockingQueue<ScheduledTask> taskQueue;
        private final ExecutorService workerPool;
        private final ScheduledExecutorService scheduler;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicInteger taskCounter = new AtomicInteger(0);
        private final AtomicLong tasksExecuted = new AtomicLong(0);
        private final AtomicLong tasksFailed = new AtomicLong(0);
        private volatile Consumer<ScheduledTask> completionCallback;

        TaskSchedulerEngine(int numWorkers) {
            this.store = new TaskStore();
            this.taskQueue = new PriorityBlockingQueue<>(11,
                Comparator.<ScheduledTask, Integer>comparing(t -> t.priority().ordinal(), Comparator.reverseOrder())
                    .thenComparing(ScheduledTask::scheduledAt));
            this.workerPool = Executors.newVirtualThreadPerTaskExecutor();
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            startWorker();
        }

        public String scheduleTask(String name, String cronExpression, Runnable action) {
            return scheduleTask(name, cronExpression, action, TaskPriority.MEDIUM, 0);
        }

        public String scheduleTask(String name, String cronExpression, Runnable action,
                                    TaskPriority priority, int maxRetries) {
            String taskId = "TASK-" + taskCounter.incrementAndGet();
            Instant now = Instant.now();
            Instant firstRun = CronExpressionParser.nextExecution(cronExpression, now);
            if (firstRun == null) {
                throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
            }

            var task = new ScheduledTask(taskId, name, cronExpression, action, priority,
                TaskStatus.SCHEDULED, firstRun, null, 0, maxRetries, 0);
            store.save(task);
            scheduleExecution(task);
            return taskId;
        }

        public String scheduleOneShot(String name, long delayMillis, Runnable action) {
            return scheduleOneShot(name, delayMillis, action, TaskPriority.MEDIUM, 0);
        }

        public String scheduleOneShot(String name, long delayMillis, Runnable action,
                                       TaskPriority priority, int maxRetries) {
            String taskId = "TASK-" + taskCounter.incrementAndGet();
            var task = new ScheduledTask(taskId, name, null, action, priority,
                TaskStatus.SCHEDULED, Instant.now().plusMillis(delayMillis), null, 0, maxRetries, 0);
            store.save(task);
            scheduleExecution(task);
            return taskId;
        }

        public boolean cancelTask(String taskId) {
            var task = store.get(taskId);
            if (task.isEmpty()) return false;
            var updated = task.get().withStatus(TaskStatus.CANCELLED);
            store.save(updated);
            taskQueue.remove(task.get());
            return true;
        }

        public Optional<ScheduledTask> getTask(String taskId) {
            return store.get(taskId);
        }

        public List<ScheduledTask> getAllTasks() {
            return store.getAll().stream()
                .sorted(Comparator.comparing(ScheduledTask::scheduledAt))
                .collect(Collectors.toList());
        }

        public List<ScheduledTask> getPendingTasks() {
            return store.getByStatus(TaskStatus.SCHEDULED).stream()
                .filter(t -> t.nextExecutionTime() != null)
                .sorted(Comparator.comparing(t -> t.nextExecutionTime()))
                .collect(Collectors.toList());
        }

        public void onTaskComplete(Consumer<ScheduledTask> callback) {
            this.completionCallback = callback;
        }

        private void scheduleExecution(ScheduledTask task) {
            if (!running.get()) return;
            long delay = Duration.between(Instant.now(), task.scheduledAt()).toMillis();
            if (delay < 0) delay = 0;

            scheduler.schedule(() -> {
                if (!running.get()) return;
                taskQueue.offer(task);
            }, delay, TimeUnit.MILLISECONDS);
        }

        private void startWorker() {
            Thread worker = Thread.ofVirtual().start(() -> {
                while (running.get()) {
                    try {
                        var task = taskQueue.poll(1, TimeUnit.SECONDS);
                        if (task != null) {
                            executeTask(task);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            worker.setName("scheduler-worker");
        }

        private void executeTask(ScheduledTask task) {
            var runningTask = task.withStatus(TaskStatus.RUNNING);
            store.save(runningTask);

            try {
                task.action().run();
                tasksExecuted.incrementAndGet();

                var completed = runningTask.withStatus(TaskStatus.COMPLETED)
                    .withLastRun(Instant.now())
                    .withExecCount(task.executionCount() + 1);
                store.save(completed);

                if (completionCallback != null) {
                    completionCallback.accept(completed);
                }

                // Re-schedule if cron-based
                if (task.cronExpression() != null) {
                    Instant next = CronExpressionParser.nextExecution(task.cronExpression(), Instant.now());
                    if (next != null) {
                        var reScheduled = new ScheduledTask(task.taskId(), task.name(), task.cronExpression(),
                            task.action(), task.priority(), TaskStatus.SCHEDULED, next, Instant.now(),
                            0, task.maxRetries(), 0);
                        store.save(reScheduled);
                        scheduleExecution(reScheduled);
                    }
                }
            } catch (Exception e) {
                tasksFailed.incrementAndGet();
                var failed = runningTask.withStatus(TaskStatus.FAILED).withLastRun(Instant.now());
                store.save(failed);

                if (task.shouldRetry()) {
                    var retryTask = runningTask.withStatus(TaskStatus.RETRYING)
                        .withRetry(task.retryCount() + 1)
                        .withLastRun(Instant.now());
                    store.save(retryTask);

                    // Re-schedule with exponential backoff
                    long backoff = (long) Math.pow(2, task.retryCount()) * 1000;
                    var retryScheduled = new ScheduledTask(task.taskId(), task.name(), task.cronExpression(),
                        task.action(), task.priority(), TaskStatus.SCHEDULED,
                        Instant.now().plusMillis(backoff), Instant.now(),
                        task.retryCount() + 1, task.maxRetries(), task.executionCount());
                    store.save(retryScheduled);
                    scheduleExecution(retryScheduled);
                }
            }
        }

        public long getTasksExecuted() { return tasksExecuted.get(); }
        public long getTasksFailed() { return tasksFailed.get(); }

        @Override
        public void close() {
            running.set(false);
            scheduler.shutdown();
            workerPool.shutdown();
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Task Scheduler ===\n");

        // ─── Cron Expression Test ───
        System.out.println("--- Cron Parser ---");
        var now = Instant.now();
        var nextMinute = CronExpressionParser.nextExecution("* * * * *", now);
        System.out.println("  '*/1 * * * *' next: " + nextMinute);

        var every5 = CronExpressionParser.nextExecution("*/5 * * * *", now);
        System.out.println("  '*/5 * * * *' next: " + every5);

        var atMidnight = CronExpressionParser.nextExecution("0 0 * * *", now);
        System.out.println("  '0 0 * * *'  next: " + atMidnight);

        // ─── Create Engine ───
        System.out.println("\n--- Scheduler Engine ---");
        var scheduler = new TaskSchedulerEngine(4);

        var completedTasks = new ConcurrentLinkedQueue<String>();

        scheduler.onTaskComplete(task -> {
            completedTasks.add(task.taskId() + ":" + task.name());
        });

        // ─── Schedule Tasks ───
        System.out.println("\n--- Scheduling Tasks ---");

        // One-shot delayed tasks
        String task1 = scheduler.scheduleOneShot("Send Email", 500, () ->
            System.out.println("  [Task] Email sent at " + Instant.now()));
        System.out.println("  Scheduled: " + task1 + " (Send Email, 500ms)");

        String task2 = scheduler.scheduleOneShot("Generate Report", 800, () ->
            System.out.println("  [Task] Report generated at " + Instant.now()));
        System.out.println("  Scheduled: " + task2 + " (Generate Report, 800ms)");

        // High priority task
        String task3 = scheduler.scheduleOneShot("Critical Alert", 200, () ->
            System.out.println("  [Task] CRITICAL ALERT processed at " + Instant.now()),
            TaskPriority.CRITICAL, 2);
        System.out.println("  Scheduled: " + task3 + " (Critical Alert, 200ms, HIGH)");

        // Task with retry
        AtomicInteger retryCounter = new AtomicInteger(0);
        String task4 = scheduler.scheduleOneShot("Flaky Operation", 600, () -> {
            int attempt = retryCounter.incrementAndGet();
            if (attempt <= 2) {
                throw new RuntimeException("Simulated failure on attempt " + attempt);
            }
            System.out.println("  [Task] Flaky operation succeeded on attempt " + attempt);
        }, TaskPriority.MEDIUM, 3);
        System.out.println("  Scheduled: " + task4 + " (Flaky Operation, 600ms, maxRetries=3)");

        // ─── Cron recurring task ───
        AtomicInteger cronCounter = new AtomicInteger(0);
        String task5 = scheduler.scheduleTask("Heartbeat", "*/1 * * * *", () -> {
            int n = cronCounter.incrementAndGet();
            System.out.println("  [Cron] Heartbeat #" + n + " at " + Instant.now());
        }, TaskPriority.LOW, 0);
        System.out.println("  Scheduled: " + task5 + " (Heartbeat, every minute)");

        // ─── Wait and check ───
        System.out.println("\n--- Waiting 2 seconds for execution ---");
        Thread.sleep(2000);

        // ─── Cancel a task ───
        var cancelled = scheduler.cancelTask(task2);
        System.out.println("\n--- Cancel Task ---");
        System.out.println("  Cancelled " + task2 + ": " + cancelled);

        // ─── Task Status ───
        System.out.println("\n--- Task Status ---");
        for (var t : scheduler.getAllTasks()) {
            System.out.printf("  %-12s %-20s %-10s executions=%d retries=%d%n",
                t.taskId(), t.name(), t.status(), t.executionCount(), t.retryCount());
        }

        // ─── Statistics ───
        System.out.println("\n--- Statistics ---");
        System.out.println("  Executed: " + scheduler.getTasksExecuted());
        System.out.println("  Failed: " + scheduler.getTasksFailed());
        System.out.println("  Completed tasks: " + completedTasks.size());
        completedTasks.forEach(t -> System.out.println("    " + t));

        // ─── Concurrent task execution ───
        System.out.println("\n--- Burst of 100 Tasks ---");
        var burstScheduler = new TaskSchedulerEngine(10);
        var burstResults = new AtomicInteger(0);

        var burstThreads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            int id = i;
            burstThreads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 10; j++) {
                    burstScheduler.scheduleOneShot("Burst-" + id + "-" + j, 100,
                        () -> burstResults.incrementAndGet(),
                        TaskPriority.values()[ThreadLocalRandom.current().nextInt(4)], 0);
                }
            });
        }
        for (var t : burstThreads) t.join();
        Thread.sleep(2000);

        System.out.println("  Total burst tasks executed: " + burstResults.get());
        System.out.println("  Total burst tasks count: " + burstScheduler.getTasksExecuted());

        scheduler.close();
        burstScheduler.close();

        System.out.println("\n=== Distributed Task Scheduler Complete ===");
    }
}
