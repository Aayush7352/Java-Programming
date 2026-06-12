package phase09.multithreading;

import java.util.concurrent.*;

class FutureExample {
    public static void main(String[] args) {
        System.out.println("=== Future example (84) ===");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<Integer> future = executor.submit(() -> {
            System.out.println("Task started, sleeping 2s...");
            Thread.sleep(2000);
            return 42;
        });

        System.out.println("Task submitted, isDone: " + future.isDone());

        try {
            Integer result = future.get(3, TimeUnit.SECONDS);
            System.out.println("Got result: " + result);
        } catch (TimeoutException e) {
            System.out.println("Timeout!");
            future.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            System.out.println("Exception: " + e.getMessage());
        }

        System.out.println("isDone: " + future.isDone());
        System.out.println("isCancelled: " + future.isCancelled());

        // Demonstrate cancel
        Future<Integer> cancelFuture = executor.submit(() -> {
            Thread.sleep(5000);
            return 99;
        });
        cancelFuture.cancel(true);
        System.out.println("Cancel future - isCancelled: " + cancelFuture.isCancelled());

        executor.shutdown();
    }
}
