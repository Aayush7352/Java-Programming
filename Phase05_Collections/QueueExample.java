package phase05.collections;

import java.util.LinkedList;
import java.util.Queue;

public class QueueExample {
    public static void main(String[] args) {
        // LinkedList as Queue
        Queue<String> queue = new LinkedList<>();

        // offer - adds element, returns false if capacity restricted
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");
        System.out.println("Queue after offers: " + queue);

        // add - throws exception if full (not shown, LinkedList is unbounded)
        queue.add("Fourth");
        System.out.println("After add: " + queue);

        // peek - retrieves head without removing, returns null if empty
        System.out.println("peek: " + queue.peek());

        // element - retrieves head without removing, throws exception if empty
        System.out.println("element: " + queue.element());

        // poll - retrieves and removes head, returns null if empty
        System.out.println("poll: " + queue.poll());
        System.out.println("After poll: " + queue);

        // remove - retrieves and removes head, throws exception if empty
        System.out.println("remove: " + queue.remove());
        System.out.println("After remove: " + queue);

        // Empty queue behavior
        System.out.println("\nEmpty queue behavior:");
        Queue<String> empty = new LinkedList<>();
        System.out.println("empty.peek(): " + empty.peek());
        // empty.element(); // throws NoSuchElementException
        System.out.println("empty.poll(): " + empty.poll());
        // empty.remove(); // throws NoSuchElementException

        // FIFO demonstration
        System.out.println("\nFIFO order:");
        Queue<Integer> fifo = new LinkedList<>();
        for (int i = 1; i <= 5; i++) fifo.offer(i * 10);
        while (!fifo.isEmpty()) System.out.print(fifo.poll() + " ");
        System.out.println();
    }
}
