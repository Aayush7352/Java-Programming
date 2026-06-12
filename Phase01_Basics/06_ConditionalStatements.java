package phase01.basics;

class ConditionalStatements {
    public static void main(String[] args) {
        int score = 85;

        // if
        System.out.println("=== if ===");
        if (score >= 40) {
            System.out.println("Pass");
        }

        // if-else
        System.out.println("\n=== if-else ===");
        if (score >= 75) {
            System.out.println("Distinction");
        } else {
            System.out.println("Not distinction");
        }

        // else-if ladder
        System.out.println("\n=== else-if ladder ===");
        char grade;
        if (score >= 90) {
            grade = 'A';
        } else if (score >= 75) {
            grade = 'B';
        } else if (score >= 60) {
            grade = 'C';
        } else if (score >= 40) {
            grade = 'D';
        } else {
            grade = 'F';
        }
        System.out.println("Score: " + score + " -> Grade: " + grade);

        // Ternary
        System.out.println("\n=== Ternary ===");
        String result = (score >= 40) ? "PASS" : "FAIL";
        System.out.println("Result: " + result);

        // Switch statement (traditional)
        System.out.println("\n=== Switch Statement (traditional) ===");
        int day = 3;
        String dayName;
        switch (day) {
            case 1: dayName = "Monday"; break;
            case 2: dayName = "Tuesday"; break;
            case 3: dayName = "Wednesday"; break;
            case 4: dayName = "Thursday"; break;
            case 5: dayName = "Friday"; break;
            case 6: dayName = "Saturday"; break;
            case 7: dayName = "Sunday"; break;
            default: dayName = "Invalid day";
        }
        System.out.println("Day " + day + ": " + dayName);

        // Switch expression (Java 14+) with arrow
        System.out.println("\n=== Switch Expression (Java 14+) ===");
        String monthName = switch (8) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Invalid month";
        };
        System.out.println("Month 8: " + monthName);

        // Switch expression with yield
        String type = switch (score / 10) {
            case 9, 10 -> "Excellent";
            case 7, 8 -> "Good";
            case 6 -> "Average";
            case 4, 5 -> "Pass";
            default -> "Fail";
        };
        System.out.println("Performance: " + type);

        // Pattern matching for switch (Java 21)
        System.out.println("\n=== Pattern Matching for switch (Java 21) ===");
        Object value = 42;
        String description = switch (value) {
            case Integer n when n > 0 -> "Positive integer: " + n;
            case Integer n when n < 0 -> "Negative integer: " + n;
            case String s -> "String of length " + s.length();
            case null -> "null value";
            default -> "Unknown type: " + value.getClass().getSimpleName();
        };
        System.out.println("Description: " + description);


        // Nested ternary
        System.out.println("\n=== Nested Ternary ===");
        int num = 0;
        String sign = (num > 0) ? "positive" : (num < 0) ? "negative" : "zero";
        System.out.println(num + " is " + sign);
    }
}
