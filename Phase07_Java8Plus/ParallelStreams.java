package phase07.java8plus;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import java.util.stream.Stream;

record Task(String name, long durationMs) {}

public class ParallelStreams {
    public static void main(String[] args) {
        var numbers = IntStream.rangeClosed(1, 20).boxed().toList();

        // parallelStream on collection
        System.out.println("=== parallelStream() ===");
        var parallelResult = numbers.parallelStream()
                .map(n -> {
                    System.out.println("parallelStream processing " + n + " on " + Thread.currentThread().getName());
                    return n * n;
                })
                .toList();
        System.out.println("Result: " + parallelResult);

        // .parallel() on stream
        System.out.println("\n=== .parallel() on stream ===");
        var streamResult = numbers.stream()
                .parallel()
                .filter(n -> n % 2 == 0)
                .map(n -> n * 10)
                .toList();
        System.out.println("Parallel filtered: " + streamResult);

        // forEachOrdered to maintain encounter order
        System.out.println("\n=== forEachOrdered ===");
        numbers.parallelStream()
                .limit(10)
                .forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();

        // forEach (unordered, faster)
        System.out.println("\n=== forEach (unordered) ===");
        numbers.parallelStream()
                .limit(10)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // performance consideration: CPU-bound task
        System.out.println("\n=== CPU-bound task ===");
        var cpuTasks = IntStream.rangeClosed(1, 8).boxed().toList();
        var cpuTimed = cpuTasks.parallelStream()
                .map(n -> {
                    // simulate CPU work
                    double sum = 0;
                    for (int i = 0; i < 1_000_000; i++) sum += Math.sin(i) * Math.cos(i);
                    return n;
                })
                .toList();
        System.out.println("CPU-bound done: " + cpuTimed);

        // custom thread pool for parallel streams
        System.out.println("\n=== Custom ForkJoinPool ===");
        var customPool = new ForkJoinPool(2);
        try {
            customPool.submit(() ->
                    numbers.parallelStream()
                            .map(n -> {
                                System.out.println("Custom pool: " + n + " on " + Thread.currentThread().getName());
                                return n * 2;
                            })
                            .toList()
            ).get();
        } catch (Exception e) {
            System.err.println("Pool error: " + e.getMessage());
        } finally {
            customPool.close();
        }

        // Sequential vs parallel timing comparison (informal)
        System.out.println("\n=== Sequential vs Parallel ===");
        var large = IntStream.rangeClosed(1, 10_000_000).boxed().toList();

        var seqStart = System.nanoTime();
        var seqSum = large.stream().reduce(0, Integer::sum);
        var seqTime = (System.nanoTime() - seqStart) / 1_000_000;
        System.out.println("Sequential sum: " + seqSum + " in " + seqTime + "ms");

        var parStart = System.nanoTime();
        var parSum = large.parallelStream().reduce(0, Integer::sum);
        var parTime = (System.nanoTime() - parStart) / 1_000_000;
        System.out.println("Parallel sum: " + parSum + " in " + parTime + "ms");
    }
}
