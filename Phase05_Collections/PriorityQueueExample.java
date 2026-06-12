package phase05.collections;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PriorityQueueExample {
    public static void main(String[] args) {
        // Natural ordering (min-heap)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.offer(30);
        minHeap.offer(10);
        minHeap.offer(50);
        minHeap.offer(20);
        minHeap.offer(40);

        System.out.println("Natural order (min-heap):");
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.poll() + " ");
        }
        System.out.println();

        // Custom Comparator (max-heap)
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.offer(30);
        maxHeap.offer(10);
        maxHeap.offer(50);
        maxHeap.offer(20);
        maxHeap.offer(40);

        System.out.println("Custom Comparator (max-heap):");
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.poll() + " ");
        }
        System.out.println();

        // Custom Comparator for strings by length
        PriorityQueue<String> byLength = new PriorityQueue<>(
            Comparator.comparingInt(String::length)
        );
        byLength.offer("banana");
        byLength.offer("apple");
        byLength.offer("kiwi");
        byLength.offer("strawberry");
        byLength.offer("fig");

        System.out.println("By string length:");
        while (!byLength.isEmpty()) {
            System.out.print(byLength.poll() + " ");
        }
        System.out.println();

        // PriorityQueue with custom object
        PriorityQueue<Task> tasks = new PriorityQueue<>();
        tasks.offer(new Task("Fix bug", 3));
        tasks.offer(new Task("Write docs", 1));
        tasks.offer(new Task("Deploy", 5));
        tasks.offer(new Task("Review PR", 2));

        System.out.println("Tasks by priority (lower = higher priority):");
        while (!tasks.isEmpty()) {
            Task t = tasks.poll();
            System.out.println("  [" + t.priority + "] " + t.name);
        }
    }

    record Task(String name, int priority) implements Comparable<Task> {
        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
}
