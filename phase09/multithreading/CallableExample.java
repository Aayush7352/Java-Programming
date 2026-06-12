package phase09.multithreading;

import java.util.concurrent.*;

public class CallableExample {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> factorial = () -> {
            int n = 7;
            int result = 1;
            for (int i = 2; i <= n; i++) {
                result *= i;
                TimeUnit.MILLISECONDS.sleep(100);
            }
            return result;
        };

        Future<Integer> future = executor.submit(factorial);

        try {
            System.out.println("Factorial result: " + future.get());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }

        executor.shutdown();
    }
}
