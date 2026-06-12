package phase01.basics;

class NumberPrograms {
    public static void main(String[] args) {
        // Prime numbers
        System.out.println("=== Prime Numbers ===");
        for (int i = 2; i <= 50; i++) {
            if (isPrime(i)) System.out.print(i + " ");
        }
        System.out.println();
        System.out.println("Is 97 prime? " + isPrime(97));
        System.out.println("Is 100 prime? " + isPrime(100));

        // Palindrome
        System.out.println("\n=== Palindrome ===");
        int[] nums = {121, 12321, 12345, 123321};
        for (int n : nums) {
            System.out.println(n + " is palindrome: " + isPalindrome(n));
        }

        // Armstrong numbers
        System.out.println("\n=== Armstrong Numbers ===");
        for (int i = 1; i <= 999; i++) {
            if (isArmstrong(i)) System.out.print(i + " ");
        }
        System.out.println();
        System.out.println("Is 9474 armstrong? " + isArmstrong(9474));

        // Fibonacci series
        System.out.println("\n=== Fibonacci Series ===");
        printFibonacci(20);

        // Factorial
        System.out.println("\n=== Factorial ===");
        for (int i = 0; i <= 10; i++) {
            System.out.println(i + "! = " + factorial(i));
        }

        // GCD
        System.out.println("\n=== GCD ===");
        System.out.println("GCD(48, 18) = " + gcd(48, 18));
        System.out.println("GCD(56, 98) = " + gcd(56, 98));
        System.out.println("GCD(101, 103) = " + gcd(101, 103));

        // LCM
        System.out.println("\n=== LCM ===");
        System.out.println("LCM(12, 18) = " + lcm(12, 18));
        System.out.println("LCM(15, 25) = " + lcm(15, 25));

        // Perfect number
        System.out.println("\n=== Perfect Numbers ===");
        for (int i = 1; i <= 10_000; i++) {
            if (isPerfect(i)) System.out.print(i + " ");
        }
        System.out.println();

        // Reverse a number
        System.out.println("\n=== Reverse Number ===");
        System.out.println("reverse(12345): " + reverseNumber(12345));

        // Sum of digits
        System.out.println("\n=== Sum of Digits ===");
        System.out.println("sumDigits(12345): " + sumDigits(12345));

        // Strong number
        System.out.println("\n=== Strong Numbers ===");
        for (int i = 1; i <= 100_000; i++) {
            if (isStrong(i)) System.out.print(i + " ");
        }
        System.out.println();
    }

    public static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    public static boolean isPalindrome(int n) {
        return n == reverseNumber(n);
    }

    public static boolean isArmstrong(int n) {
        int original = n, sum = 0;
        int digits = String.valueOf(n).length();
        while (n > 0) {
            sum += (int) Math.pow(n % 10, digits);
            n /= 10;
        }
        return sum == original;
    }

    public static void printFibonacci(int count) {
        int a = 0, b = 1;
        System.out.print(a + " " + b + " ");
        for (int i = 2; i < count; i++) {
            int next = a + b;
            System.out.print(next + " ");
            a = b;
            b = next;
        }
        System.out.println();
    }

    public static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    public static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static int lcm(int a, int b) {
        return a * (b / gcd(a, b));
    }

    public static boolean isPerfect(int n) {
        if (n < 2) return false;
        int sum = 1;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                sum += i;
                if (i != n / i) sum += n / i;
            }
        }
        return sum == n;
    }

    public static int reverseNumber(int n) {
        int reversed = 0;
        while (n != 0) {
            reversed = reversed * 10 + n % 10;
            n /= 10;
        }
        return reversed;
    }

    public static int sumDigits(int n) {
        int sum = 0;
        while (n != 0) {
            sum += n % 10;
            n /= 10;
        }
        return sum;
    }

    public static boolean isStrong(int n) {
        int original = n, sum = 0;
        while (n > 0) {
            sum += factorial(n % 10);
            n /= 10;
        }
        return sum == original;
    }
}
