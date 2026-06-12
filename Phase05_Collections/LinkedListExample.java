package phase05.collections;

import java.util.ArrayList;
import java.util.LinkedList;

public class LinkedListExample {
    public static void main(String[] args) {
        // List operations
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        System.out.println("List: " + list);
        System.out.println("get(1): " + list.get(1));
        list.set(1, "BB");
        System.out.println("After set: " + list);
        list.remove("C");
        System.out.println("After remove: " + list);

        // Deque operations - offer (adds at tail)
        list.offer("D");
        list.offer("E");
        System.out.println("After offer: " + list);

        // Deque operations - addFirst/addLast
        list.addFirst("Z");
        list.addLast("F");
        System.out.println("After addFirst/addLast: " + list);

        // Deque - peek (retrieve without removal)
        System.out.println("peekFirst: " + list.peekFirst());
        System.out.println("peekLast: " + list.peekLast());

        // Deque - poll (retrieve and remove)
        System.out.println("pollFirst: " + list.pollFirst());
        System.out.println("pollLast: " + list.pollLast());
        System.out.println("After polls: " + list);

        // Stack-like operations
        list.push("X");  // addFirst
        System.out.println("After push X: " + list);
        System.out.println("pop: " + list.pop());  // removeFirst
        System.out.println("After pop: " + list);

        // Performance comparison with ArrayList
        LinkedList<Integer> ll = new LinkedList<>();
        ArrayList<Integer> al = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) ll.add(i);
        long llAdd = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) al.add(i);
        long alAdd = System.nanoTime() - start;

        System.out.println("\nPerformance: add 100k elements");
        System.out.println("LinkedList add: " + llAdd / 1_000_000 + " ms");
        System.out.println("ArrayList  add: " + alAdd / 1_000_000 + " ms");

        start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) ll.get(i);
        long llGet = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) al.get(i);
        long alGet = System.nanoTime() - start;

        System.out.println("LinkedList get: " + llGet / 1_000_000 + " ms");
        System.out.println("ArrayList  get: " + alGet / 1_000_000 + " ms");
    }
}
