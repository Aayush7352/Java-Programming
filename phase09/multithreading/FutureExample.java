package phase09.multithreading;

import java.util.concurrent.*;

public class FutureExample {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<Integer> future = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            return 42;
        });

        System.out.println("isDone before get: " + future.isDone());

        try {
            Integer result = future.get(3, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Execution failed: " + e.getMessage());
        } catch (TimeoutException e) {
            System.err.println("Timeout – cancelling task");
            future.cancel(true);
        }

        System.out.println("isDone after get: " + future.isDone());
        System.out.println("isCancelled: " + future.isCancelled());
        executor.shutdown();
    }
}
