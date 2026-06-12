package phase09.multithreading;

import java.util.concurrent.*;
import java.util.List;

class CallableExample {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== Callable example (85) ===");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Callable<String> task1 = () -> {
            Thread.sleep(300);
            return "Result from task1";
        };

        Callable<String> task2 = () -> {
            Thread.sleep(200);
            return "Result from task2";
        };

        Callable<String> task3 = () -> {
            Thread.sleep(100);
            return "Result from task3";
        };

        List<Future<String>> futures = executor.invokeAll(List.of(task1, task2, task3));

        for (Future<String> f : futures) {
            System.out.println("Future: " + f.get());
        }

        String fastest = executor.invokeAny(List.of(task1, task2, task3));
        System.out.println("Fastest result (invokeAny): " + fastest);

        executor.shutdown();
    }
}
