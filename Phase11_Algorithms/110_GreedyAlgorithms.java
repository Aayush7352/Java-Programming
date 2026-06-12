package phase11.algorithms;

import java.util.*;

record Item(String name, int weight, int value) {
    public double valuePerWeight() {
        return (double) value / weight;
    }
}

record Activity(int start, int finish) implements Comparable<Activity> {
    @Override
    public int compareTo(Activity other) {
        return Integer.compare(this.finish, other.finish);
    }
}

record Job(char id, int deadline, int profit) implements Comparable<Job> {
    @Override
    public int compareTo(Job other) {
        return Integer.compare(other.profit, this.profit);
    }
}

class GreedyAlgorithms {

    // --- Fractional Knapsack ---
    public static double fractionalKnapsack(Item[] items, int capacity) {
        Arrays.sort(items, Comparator.comparingDouble(Item::valuePerWeight).reversed());

        double totalValue = 0.0;
        int remainingCapacity = capacity;

        System.out.println("Items taken:");
        for (Item item : items) {
            if (remainingCapacity >= item.weight()) {
                totalValue += item.value();
                remainingCapacity -= item.weight();
                System.out.println("  " + item.name() + " (full) - weight: " + item.weight()
                        + ", value: " + item.value());
            } else if (remainingCapacity > 0) {
                double fraction = (double) remainingCapacity / item.weight();
                totalValue += item.value() * fraction;
                System.out.println("  " + item.name() + " (fraction: "
                        + String.format("%.2f", fraction) + ") - value: "
                        + String.format("%.2f", item.value() * fraction));
                remainingCapacity = 0;
                break;
            }
        }
        return totalValue;
    }

    // --- Activity Selection ---
    public static List<Activity> activitySelection(Activity[] activities) {
        Arrays.sort(activities);
        List<Activity> selected = new ArrayList<>();

        selected.add(activities[0]);
        int lastFinish = activities[0].finish();

        for (int i = 1; i < activities.length; i++) {
            if (activities[i].start() >= lastFinish) {
                selected.add(activities[i]);
                lastFinish = activities[i].finish();
            }
        }
        return selected;
    }

    // --- Coin Change (Greedy - works for canonical coin systems) ---
    public static Map<Integer, Integer> coinChangeGreedy(int[] coins, int amount) {
        Integer[] sortedCoins = Arrays.stream(coins).boxed()
                .sorted(Comparator.reverseOrder())
                .toArray(Integer[]::new);

        Map<Integer, Integer> result = new LinkedHashMap<>();
        int remaining = amount;

        for (int coin : sortedCoins) {
            if (remaining >= coin) {
                int count = remaining / coin;
                result.put(coin, count);
                remaining %= coin;
            }
        }

        if (remaining != 0) {
            System.out.println("Cannot make exact change for " + amount
                    + " with given coin denominations");
            return Map.of();
        }
        return result;
    }

    // --- Job Sequencing with Deadlines ---
    public static List<Job> jobSequencing(Job[] jobs) {
        Arrays.sort(jobs);

        int maxDeadline = Arrays.stream(jobs)
                .mapToInt(Job::deadline)
                .max().orElse(0);

        Job[] result = new Job[maxDeadline];
        boolean[] slots = new boolean[maxDeadline];

        for (Job job : jobs) {
            for (int j = Math.min(maxDeadline - 1, job.deadline() - 1); j >= 0; j--) {
                if (!slots[j]) {
                    slots[j] = true;
                    result[j] = job;
                    break;
                }
            }
        }

        List<Job> scheduled = new ArrayList<>();
        for (Job job : result) {
            if (job != null) scheduled.add(job);
        }
        return scheduled;
    }

    // --- Huffman Coding (simple cost calculation) ---
    public static int huffmanCodingCost(int[] frequencies) {
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int f : frequencies) pq.offer(f);

        int totalCost = 0;
        while (pq.size() > 1) {
            int a = pq.poll();
            int b = pq.poll();
            int combined = a + b;
            totalCost += combined;
            pq.offer(combined);
        }
        return totalCost;
    }

    public static void main(String[] args) {
        System.out.println("=== Fractional Knapsack ===");
        Item[] items = {
                new Item("Item1", 10, 60),
                new Item("Item2", 20, 100),
                new Item("Item3", 30, 120)
        };
        double maxValue = fractionalKnapsack(items, 50);
        System.out.println("Total value: " + String.format("%.2f", maxValue));

        System.out.println("\n=== Activity Selection ===");
        Activity[] activities = {
                new Activity(1, 4), new Activity(3, 5),
                new Activity(0, 6), new Activity(5, 7),
                new Activity(3, 8), new Activity(5, 9),
                new Activity(6, 10), new Activity(8, 11),
                new Activity(8, 12), new Activity(2, 13),
                new Activity(12, 14)
        };
        var selected = activitySelection(activities);
        System.out.println("Selected activities (" + selected.size() + "):");
        selected.forEach(a -> System.out.println("  " + a));

        System.out.println("\n=== Coin Change (Greedy) ===");
        int[] coins = {1, 2, 5, 10, 20, 50, 100};
        int amount = 143;
        var change = coinChangeGreedy(coins, amount);
        System.out.println("Change for " + amount + ": " + change);

        System.out.println("\n=== Job Sequencing ===");
        Job[] jobs = {
                new Job('a', 2, 100), new Job('b', 1, 19),
                new Job('c', 2, 27), new Job('d', 1, 25),
                new Job('e', 3, 15)
        };
        var scheduled = jobSequencing(jobs);
        System.out.println("Scheduled jobs (" + scheduled.size() + "):");
        scheduled.forEach(j -> System.out.println("  " + j));

        System.out.println("\n=== Huffman Coding Cost ===");
        int[] freqs = {5, 9, 12, 13, 16, 45};
        int cost = huffmanCodingCost(freqs);
        System.out.println("Total cost: " + cost);
    }
}
