package phase07.java8plus;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CompletableFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var executor = Executors.newFixedThreadPool(4);

        try {
            // supplyAsync
            var future1 = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                sleep(500);
                return 42;
            }, executor);

            // thenApply
            var future2 = future1.thenApply(n -> n * 2);
            System.out.println("thenApply: " + future2.get());

            // thenAccept
            java.util.concurrent.CompletableFuture.supplyAsync(() -> "Hello", executor)
                    .thenAccept(s -> System.out.println("thenAccept: " + s))
                    .get();

            // thenCompose (flatMap)
            var composed = java.util.concurrent.CompletableFuture.supplyAsync(() -> 5, executor)
                    .thenCompose(n -> java.util.concurrent.CompletableFuture.supplyAsync(() -> n + 10));
            System.out.println("thenCompose: " + composed.get());

            // thenCombine (zip)
            var combined = java.util.concurrent.CompletableFuture.supplyAsync(() -> "A", executor)
                    .thenCombine(
                            java.util.concurrent.CompletableFuture.supplyAsync(() -> "B"),
                            (a, b) -> a + b
                    );
            System.out.println("thenCombine: " + combined.get());

            // allOf
            var all = java.util.concurrent.CompletableFuture.allOf(
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(200); return 1; }),
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(100); return 2; }),
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(300); return 3; })
            );
            all.get();
            System.out.println("allOf completed");

            // anyOf
            var any = java.util.concurrent.CompletableFuture.anyOf(
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(300); return "slow"; }),
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(100); return "fast"; })
            );
            System.out.println("anyOf: " + any.get());

            // exceptionally
            var faulty = java.util.concurrent.CompletableFuture.<Integer>supplyAsync(() -> {
                throw new RuntimeException("boom");
            }).exceptionally(ex -> {
                System.out.println("exceptionally caught: " + ex.getMessage());
                return -1;
            });
            System.out.println("exceptionally result: " + faulty.get());

            // handle (success + failure)
            var handled = java.util.concurrent.CompletableFuture.<Integer>supplyAsync(() -> 10)
                    .handle((res, ex) -> res != null ? res * 2 : -1);
            System.out.println("handle result: " + handled.get());

            var handledErr = java.util.concurrent.CompletableFuture.<Integer>supplyAsync(() -> {
                throw new RuntimeException("err");
            }).handle((res, ex) -> ex != null ? -99 : res);
            System.out.println("handle error: " + handledErr.get());

        } finally {
            executor.shutdown();
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
