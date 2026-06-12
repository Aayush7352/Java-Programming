package phase01.basics;

class PatternPrograms {
    public static void main(String[] args) {
        int n = 5;

        // 1. Right triangle
        System.out.println("=== 1. Right Triangle ===");
        for (int i = 1; i <= n; i++) {
            System.out.println("*".repeat(i));
        }

        // 2. Inverted right triangle
        System.out.println("\n=== 2. Inverted Right Triangle ===");
        for (int i = n; i >= 1; i--) {
            System.out.println("*".repeat(i));
        }

        // 3. Pyramid
        System.out.println("\n=== 3. Pyramid ===");
        for (int i = 1; i <= n; i++) {
            System.out.println(" ".repeat(n - i) + "*".repeat(2 * i - 1));
        }

        // 4. Inverted pyramid
        System.out.println("\n=== 4. Inverted Pyramid ===");
        for (int i = n; i >= 1; i--) {
            System.out.println(" ".repeat(n - i) + "*".repeat(2 * i - 1));
        }

        // 5. Diamond
        System.out.println("\n=== 5. Diamond ===");
        for (int i = 1; i <= n; i++) {
            System.out.println(" ".repeat(n - i) + "*".repeat(2 * i - 1));
        }
        for (int i = n - 1; i >= 1; i--) {
            System.out.println(" ".repeat(n - i) + "*".repeat(2 * i - 1));
        }

        // 6. Hollow square
        System.out.println("\n=== 6. Hollow Square ===");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print((i == 0 || i == n - 1 || j == 0 || j == n - 1) ? "* " : "  ");
            }
            System.out.println();
        }

        // 7. Number pyramid
        System.out.println("\n=== 7. Number Pyramid ===");
        for (int i = 1; i <= n; i++) {
            System.out.print(" ".repeat(n - i));
            for (int j = 1; j <= i; j++) {
                System.out.print(j + " ");
            }
            System.out.println();
        }

        // 8. Floyd's Triangle
        System.out.println("\n=== 8. Floyd's Triangle ===");
        int num = 1;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= i; j++) {
                System.out.print(num++ + " ");
            }
            System.out.println();
        }

        // 9. Pascal's Triangle
        System.out.println("\n=== 9. Pascal's Triangle ===");
        for (int i = 0; i < n; i++) {
            System.out.print(" ".repeat(2 * (n - i)));
            int val = 1;
            for (int j = 0; j <= i; j++) {
                System.out.printf("%4d", val);
                val = val * (i - j) / (j + 1);
            }
            System.out.println();
        }

        // 10. Butterfly pattern
        System.out.println("\n=== 10. Butterfly Pattern ===");
        for (int i = 1; i <= n; i++) {
            System.out.print("*".repeat(i));
            System.out.print(" ".repeat(2 * (n - i)));
            System.out.println("*".repeat(i));
        }
        for (int i = n; i >= 1; i--) {
            System.out.print("*".repeat(i));
            System.out.print(" ".repeat(2 * (n - i)));
            System.out.println("*".repeat(i));
        }

        // 11. Number triangle (increasing)
        System.out.println("\n=== 11. Number Triangle ===");
        for (int i = 1; i <= n; i++) {
            System.out.print(" ".repeat(n - i));
            for (int j = 1; j <= i; j++) {
                System.out.print(i + " ");
            }
            System.out.println();
        }

        // 12. Binary triangle
        System.out.println("\n=== 12. Binary Triangle ===");
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= i; j++) {
                System.out.print(((i + j) % 2 == 0 ? "1" : "0") + " ");
            }
            System.out.println();
        }
    }
}
