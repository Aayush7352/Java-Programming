package phase16.projects;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class DistributedTaskScheduler {

    public static enum TaskPriority {
        LOW(0), MEDIUM(1), HIGH(2), CRITICAL(3);

        private final int value;
        TaskPriority(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    public static enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, RETRYING, CANCELLED, TIMED_OUT
    }

    public static record TaskResult(String taskId, TaskStatus status, Object result,
                                     String errorMessage, long startTime, long endTime,
                                     long durationMs) {
        public TaskResult {
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(status);
        }

        public boolean isSuccess() { return status == TaskStatus.COMPLETED; }
    }

    public static sealed interface ScheduleSpec permits CronExpression, FixedRateSchedule,
            FixedDelaySchedule, OneTimeSchedule {
        long nextExecutionTime(long currentTimeMillis);
    }

    public static record CronExpression(String expression, String minute, String hour,
                                         String dayOfMonth, String month, String dayOfWeek)
            implements ScheduleSpec {

        public CronExpression {
            Objects.requireNonNull(expression);
        }

        public static CronExpression parse(String cron) {
            var parts = cron.trim().split("\\s+");
            if (parts.length != 5)
                throw new IllegalArgumentException("Cron must have 5 fields: " + cron);
            return new CronExpression(cron, parts[0], parts[1], parts[2], parts[3], parts[4]);
        }

        @Override
        public long nextExecutionTime(long currentTimeMillis) {
            var next = LocalDateTime.now().plusMinutes(1)
                    .withSecond(0).withNano(0);
            var attempts = 0;
            while (attempts < 525600) {
                attempts++;
                if (matches(next)) return next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                next = next.plusMinutes(1);
            }
            return currentTimeMillis + 60000;
        }

        private boolean matches(LocalDateTime dt) {
            return matchesField(minute, dt.getMinute(), 0, 59)
                    && matchesField(hour, dt.getHour(), 0, 23)
                    && matchesField(dayOfMonth, dt.getDayOfMonth(), 1, 31)
                    && matchesField(month, dt.getMonthValue(), 1, 12)
                    && matchesField(dayOfWeek, dt.getDayOfWeek().getValue() % 7, 0, 6);
        }

        private boolean matchesField(String field, int value, int min, int max) {
            if (field.equals("*")) return true;
            if (field.contains("/")) {
                var parts = field.split("/");
                var step = Integer.parseInt(parts[1]);
                var start = parts[0].equals("*") ? min : Integer.parseInt(parts[0]);
                return value >= start && (value - start) % step == 0;
            }
            if (field.contains(",")) {
                return Arrays.stream(field.split(","))
                        .anyMatch(s -> Integer.parseInt(s.trim()) == value);
            }
            if (field.contains("-")) {
                var parts = field.split("-");
                var low = Integer.parseInt(parts[0]);
                var high = Integer.parseInt(parts[1]);
                return value >= low && value <= high;
            }
            try {
                return Integer.parseInt(field) == value;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static record FixedRateSchedule(long intervalMs) implements ScheduleSpec {
        public FixedRateSchedule {
            if (intervalMs <= 0) throw new IllegalArgumentException("Interval must be positive");
        }

        @Override
        public long nextExecutionTime(long currentTimeMillis) {
            return currentTimeMillis + intervalMs;
        }
    }

    public static record FixedDelaySchedule(long delayMs) implements ScheduleSpec {
        public FixedDelaySchedule {
            if (delayMs <= 0) throw new IllegalArgumentException("Delay must be positive");
        }

        @Override
        public long nextExecutionTime(long currentTimeMillis) {
            return currentTimeMillis + delayMs;
        }
    }

    public static record OneTimeSchedule(LocalDateTime scheduledTime) implements ScheduleSpec {
        public OneTimeSchedule {
            Objects.requireNonNull(scheduledTime);
        }

        @Override
        public long nextExecutionTime(long currentTimeMillis) {
            var scheduled = scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            return scheduled > currentTimeMillis ? scheduled : -1;
        }
    }

    public static final class ScheduledTask implements Comparable<ScheduledTask> {
        private final String taskId;
        private final String name;
        private final Callable<Object> task;
        private final ScheduleSpec schedule;
        private final TaskPriority priority;
        private final int maxRetries;
        private final long timeoutMs;
        private final long backoffBaseMs;
        private final AtomicInteger retryCount = new AtomicInteger(0);
        private volatile long nextExecutionTime;
        private volatile TaskStatus status = TaskStatus.PENDING;
        private final Lock lock = new ReentrantLock();

        public ScheduledTask(String taskId, String name, Callable<Object> task,
                             ScheduleSpec schedule, TaskPriority priority,
                             int maxRetries, long timeoutMs, long backoffBaseMs) {
            this.taskId = Objects.requireNonNull(taskId);
            this.name = Objects.requireNonNull(name);
            this.task = Objects.requireNonNull(task);
            this.schedule = Objects.requireNonNull(schedule);
            this.priority = priority;
            this.maxRetries = maxRetries;
            this.timeoutMs = timeoutMs;
            this.backoffBaseMs = backoffBaseMs;
            this.nextExecutionTime = schedule.nextExecutionTime(System.currentTimeMillis());
        }

        public TaskResult execute() {
            var startTime = System.currentTimeMillis();
            status = TaskStatus.RUNNING;
            try {
                var future = Executors.newSingleThreadExecutor().submit(task);
                var result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                var endTime = System.currentTimeMillis();
                status = TaskStatus.COMPLETED;
                nextExecutionTime = schedule.nextExecutionTime(endTime);
                retryCount.set(0);
                return new TaskResult(taskId, TaskStatus.COMPLETED, result, null,
                        startTime, endTime, endTime - startTime);
            } catch (TimeoutException e) {
                status = TaskStatus.TIMED_OUT;
                return handleFailure("Timeout after " + timeoutMs + "ms", startTime);
            } catch (Exception e) {
                return handleFailure(e.getMessage(), startTime);
            }
        }

        private TaskResult handleFailure(String error, long startTime) {
            var endTime = System.currentTimeMillis();
            if (retryCount.incrementAndGet() <= maxRetries) {
                status = TaskStatus.RETRYING;
                var backoff = backoffBaseMs * (long) Math.pow(2, retryCount.get() - 1);
                nextExecutionTime = endTime + backoff;
                return new TaskResult(taskId, TaskStatus.RETRYING, null, error,
                        startTime, endTime, endTime - startTime);
            }
            status = TaskStatus.FAILED;
            return new TaskResult(taskId, TaskStatus.FAILED, null, error,
                    startTime, endTime, endTime - startTime);
        }

        public void cancel() { status = TaskStatus.CANCELLED; }
        public boolean isActive() { return status == TaskStatus.PENDING || status == TaskStatus.RETRYING; }

        @Override
        public int compareTo(ScheduledTask other) {
            var prio = Integer.compare(other.priority.getValue(), this.priority.getValue());
            if (prio != 0) return prio;
            return Long.compare(this.nextExecutionTime, other.nextExecutionTime);
        }

        public String getTaskId() { return taskId; }
        public String getName() { return name; }
        public ScheduleSpec getSchedule() { return schedule; }
        public TaskPriority getPriority() { return priority; }
        public TaskStatus getStatus() { return status; }
        public long getNextExecutionTime() { return nextExecutionTime; }
        public int getRetryCount() { return retryCount.get(); }
    }

    public static final class TaskScheduler {
        private final PriorityBlockingQueue<ScheduledTask> taskQueue = new PriorityBlockingQueue<>();
        private final Map<String, ScheduledTask> taskRegistry = new ConcurrentHashMap<>();
        private final List<TaskResult> results = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong taskCounter = new AtomicLong(0);
        private final AtomicInteger activeWorkers = new AtomicInteger(0);
        private volatile boolean running = false;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final ExecutorService workers;

        public TaskScheduler(int workerCount) {
            this.workers = Executors.newVirtualThreadPerTaskExecutor();
        }

        public ScheduledTask schedule(String name, Callable<Object> task, ScheduleSpec schedule,
                                      TaskPriority priority, int maxRetries, long timeoutMs) {
            return schedule(name, task, schedule, priority, maxRetries, timeoutMs, 1000);
        }

        public ScheduledTask schedule(String name, Callable<Object> task, ScheduleSpec schedule,
                                      TaskPriority priority, int maxRetries, long timeoutMs,
                                      long backoffBaseMs) {
            var taskId = "TASK-" + taskCounter.incrementAndGet();
            var scheduledTask = new ScheduledTask(taskId, name, task, schedule, priority,
                    maxRetries, timeoutMs, backoffBaseMs);
            taskRegistry.put(taskId, scheduledTask);
            taskQueue.offer(scheduledTask);
            return scheduledTask;
        }

        public void start() {
            running = true;
            scheduler.scheduleAtFixedRate(this::dispatchTasks, 0, 500, TimeUnit.MILLISECONDS);
            for (int i = 0; i < 4; i++) {
                workers.submit(this::workerLoop);
            }
        }

        public void stop() {
            running = false;
            scheduler.shutdown();
        }

        private void dispatchTasks() {
            if (!running) return;
            var now = System.currentTimeMillis();
            var tasksToRun = new ArrayList<ScheduledTask>();
            taskQueue.drainTo(tasksToRun);
            for (var task : tasksToRun) {
                if (!task.isActive()) continue;
                if (task.getNextExecutionTime() <= now) {
                    taskQueue.offer(task);
                } else {
                    taskQueue.offer(task);
                }
            }
        }

        private void workerLoop() {
            while (running) {
                try {
                    var task = taskQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null && task.isActive() && task.getNextExecutionTime() <= System.currentTimeMillis()) {
                        activeWorkers.incrementAndGet();
                        try {
                            var result = task.execute();
                            results.add(result);
                            if (result.status() == TaskStatus.COMPLETED) {
                                var nextTime = task.getNextExecutionTime();
                                if (nextTime > 0) {
                                    taskQueue.offer(task);
                                }
                            } else if (result.status() == TaskStatus.RETRYING) {
                                taskQueue.offer(task);
                            }
                        } finally {
                            activeWorkers.decrementAndGet();
                        }
                    } else if (task != null) {
                        taskQueue.offer(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public boolean cancel(String taskId) {
            var task = taskRegistry.get(taskId);
            if (task != null) {
                task.cancel();
                return true;
            }
            return false;
        }

        public List<TaskResult> getResults() { return List.copyOf(results); }
        public List<TaskResult> getResultsByStatus(TaskStatus status) {
            return results.stream().filter(r -> r.status() == status).collect(Collectors.toUnmodifiableList());
        }

        public ScheduledTask getTask(String taskId) { return taskRegistry.get(taskId); }
        public List<ScheduledTask> getAllTasks() { return List.copyOf(taskRegistry.values()); }
        public int activeTaskCount() { return activeWorkers.get(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Task Scheduler ===%n".formatted());

        var scheduler = new TaskScheduler(4);
        scheduler.start();

        System.out.println("--- Schedule Various Tasks ---");

        var task1 = scheduler.schedule("Print Hello",
                () -> { System.out.println("  [Task] Hello from scheduled task!"); return "Hello"; },
                new FixedRateSchedule(5000),
                TaskPriority.LOW, 2, 2000);

        var task2 = scheduler.schedule("Compute Pi",
                () -> {
                    double pi = 0;
                    for (int i = 0; i < 1000000; i++) {
                        pi += (i % 2 == 0 ? 1 : -1) / (double) (2 * i + 1);
                    }
                    pi *= 4;
                    return "Pi ≈ " + pi;
                },
                new OneTimeSchedule(LocalDateTime.now().plusSeconds(1)),
                TaskPriority.HIGH, 1, 5000);

        var task3 = scheduler.schedule("Cron Every 30s",
                () -> { System.out.println("  [Cron] Every 30 seconds tick"); return "Cron tick"; },
                CronExpression.parse("*/30 * * * *"),
                TaskPriority.MEDIUM, 3, 10000);

        var task4 = scheduler.schedule("Fail with Retry",
                () -> {
                    throw new RuntimeException("Transient error!");
                },
                new FixedDelaySchedule(2000),
                TaskPriority.LOW, 3, 2000, 500);

        var task5 = scheduler.schedule("Timeout Task",
                () -> {
                    Thread.sleep(5000);
                    return "Late result";
                },
                new OneTimeSchedule(LocalDateTime.now()),
                TaskPriority.MEDIUM, 1, 1000);

        var task6 = scheduler.schedule("Critical Computation",
                () -> {
                    var sum = 0;
                    for (int i = 0; i < 100; i++) sum += i;
                    return "Sum 0-99 = " + sum;
                },
                new FixedRateSchedule(3000),
                TaskPriority.CRITICAL, 2, 3000);

        Thread.sleep(3000);

        System.out.println("%n--- Task Statuses After 3s ---%n".formatted());
        for (var task : scheduler.getAllTasks()) {
            System.out.println("  [%s] %s: %s (next: %d, retries: %d)"
                    .formatted(task.getPriority(), task.getName(), task.getStatus(),
                            task.getNextExecutionTime() - System.currentTimeMillis(),
                            task.getRetryCount()));
        }

        System.out.println("%n--- Results ---%n".formatted());
        for (var result : scheduler.getResults()) {
            System.out.println("  [%s] %s: %s (%,dms)".formatted(
                    result.status(), result.taskId(),
                    result.isSuccess() ? result.result() : result.errorMessage(),
                    result.durationMs()));
        }

        System.out.println("%n--- Cancel Task ---%n".formatted());
        var cancelled = scheduler.cancel(task1.getTaskId());
        System.out.println("  Cancelled task1: " + cancelled);

        System.out.println("%n--- Virtual Thread Workers ---%n".formatted());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var t = scheduler.schedule("VT-Task-" + idx,
                            () -> {
                                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                                return "VT result " + idx;
                            },
                            new OneTimeSchedule(LocalDateTime.now().plusNanos(100_000_000L * idx)),
                            TaskPriority.MEDIUM, 0, 5000);
                    System.out.println("  [VT-%d] Scheduled %s".formatted(idx, t.getTaskId()));
                });
            }
        }

        Thread.sleep(4000);

        System.out.println("%n--- Pattern Matching on Results ---%n".formatted());
        for (var r : scheduler.getResults().stream().limit(10).toList()) {
            switch (r) {
                case TaskResult tr when tr.isSuccess() && tr.durationMs() > 100 ->
                    System.out.println("  Long task %s completed in %,dms: %s".formatted(tr.taskId(), tr.durationMs(), tr.result()));
                case TaskResult tr when tr.isSuccess() ->
                    System.out.println("  Quick task %s: %s".formatted(tr.taskId(), tr.result()));
                case TaskResult tr when tr.status() == TaskStatus.RETRYING ->
                    System.out.println("  Retrying %s: %s".formatted(tr.taskId(), tr.errorMessage()));
                case TaskResult tr ->
                    System.out.println("  Failed %s: %s".formatted(tr.taskId(), tr.errorMessage()));
            }
        }

        Thread.sleep(2000);
        scheduler.stop();
        System.out.println("%nFinal Stats: %d tasks scheduled, %d results".formatted(
                scheduler.getAllTasks().size(), scheduler.getResults().size()));
        System.out.println("=== Done ===");
    }
}
