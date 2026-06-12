package phase01.basics;

import java.util.List;

class Loops {
    public static void main(String[] args) {
        // for loop
        System.out.println("=== for loop ===");
        for (int i = 1; i <= 5; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        // while loop
        System.out.println("\n=== while loop ===");
        int count = 5;
        while (count > 0) {
            System.out.print(count-- + " ");
        }
        System.out.println();

        // do-while loop
        System.out.println("\n=== do-while loop ===");
        int n = 1;
        do {
            System.out.print(n + " ");
            n++;
        } while (n <= 5);
        System.out.println();

        // for-each (enhanced for)
        System.out.println("\n=== for-each loop ===");
        int[] numbers = {10, 20, 30, 40, 50};
        for (int num : numbers) {
            System.out.print(num + " ");
        }
        System.out.println();

        // for-each with List
        List<String> fruits = List.of("Apple", "Banana", "Cherry");
        for (String fruit : fruits) {
            System.out.print(fruit + " ");
        }
        System.out.println();

        // Labeled break
        System.out.println("\n=== Labeled break ===");
        outer:
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                if (i == 2 && j == 2) {
                    break outer;
                }
                System.out.print("(" + i + "," + j + ") ");
            }
        }
        System.out.println(" -> broke out of outer loop");

        // Labeled continue
        System.out.println("\n=== Labeled continue ===");
        outer:
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                if (i == 2 && j == 2) {
                    System.out.print("skip ");
                    continue outer;
                }
                System.out.print("(" + i + "," + j + ") ");
            }
        }
        System.out.println(" -> continued outer loop");

        // Infinite loop with break
        System.out.println("\n=== Infinite loop with break ===");
        int x = 0;
        while (true) {
            x++;
            if (x > 5) break;
            System.out.print(x + " ");
        }
        System.out.println(" -> broken");

        // Nested loops with continue
        System.out.println("\n=== continue in nested loop ===");
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                if (j == 2) continue;
                System.out.print("(" + i + "," + j + ") ");
            }
        }
        System.out.println();

        // for loop with comma
        System.out.println("\n=== for loop with multiple vars ===");
        for (int i = 0, j = 10; i < j; i++, j--) {
            System.out.println("i=" + i + ", j=" + j);
        }
    }
}
