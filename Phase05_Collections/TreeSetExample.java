package phase05.collections;

import java.util.TreeSet;

public class TreeSetExample {
    public static void main(String[] args) {
        // TreeSet with natural ordering
        TreeSet<Integer> numbers = new TreeSet<>();
        numbers.add(50);
        numbers.add(10);
        numbers.add(30);
        numbers.add(70);
        numbers.add(20);
        numbers.add(40);

        System.out.println("TreeSet (sorted): " + numbers);

        // NavigableSet operations
        System.out.println("first: " + numbers.first());
        System.out.println("last: " + numbers.last());
        System.out.println("lower(30): " + numbers.lower(30));   // greatest < 30
        System.out.println("floor(30): " + numbers.floor(30));   // greatest <= 30
        System.out.println("ceiling(35): " + numbers.ceiling(35)); // least >= 35
        System.out.println("higher(40): " + numbers.higher(40));   // least > 40

        // headSet - elements less than
        System.out.println("headSet(30): " + numbers.headSet(30));
        System.out.println("headSet(30, true): " + numbers.headSet(30, true));

        // tailSet - elements greater than or equal
        System.out.println("tailSet(30): " + numbers.tailSet(30));

        // subSet - range view
        System.out.println("subSet(20, 50): " + numbers.subSet(20, 50));
        System.out.println("subSet(20, true, 50, true): " + numbers.subSet(20, true, 50, true));

        // descending set
        System.out.println("descendingSet: " + numbers.descendingSet());

        // pollFirst/pollLast
        System.out.println("pollFirst: " + numbers.pollFirst());
        System.out.println("After pollFirst: " + numbers);
        System.out.println("pollLast: " + numbers.pollLast());
        System.out.println("After pollLast: " + numbers);
    }
}
