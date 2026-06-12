package phase01.basics;

import java.util.Scanner;

class InputOutput {
    public static void main(String[] args) {
        // Scanner
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            String name = scanner.nextLine();
            System.out.print("Enter your age: ");
            int age = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Enter your city: ");
            String city = scanner.nextLine();

            // Formatted output with printf
            System.out.println("\n=== printf ===");
            System.out.printf("Name: %s, Age: %d, City: %s%n", name, age, city);
            System.out.printf("Formatted number: %,d%n", 1_000_000);
            System.out.printf("Floating point: %.2f%n", Math.PI);
            System.out.printf("Left-justified: %-20s|%n", name);
            System.out.printf("Right-justified: %20s|%n", name);

            // formatted (Java 15+)
            System.out.println("\n=== String.formatted ===");
            String formatted = """
                    User Profile:
                    ------------
                    Name  : %s
                    Age   : %d
                    City  : %s
                    """.formatted(name, age, city);
            System.out.println(formatted);
        }

        // System.console() (works only in terminal, not in IDE)
        var console = System.console();
        if (console != null) {
            String input = console.readLine("Console readLine: ");
            char[] password = console.readPassword("Enter password: ");
            console.printf("You entered: %s%n", input);
        } else {
            System.out.println("System.console() not available (not running in terminal)");
        }
    }
}
