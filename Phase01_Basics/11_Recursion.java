package phase01.basics;

import java.util.Arrays;

class Recursion {
    // Factorial
    public static long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    // Fibonacci (inefficient, good for demo)
    public static int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    // Fibonacci with memoization
    public static long fibonacciMemo(int n, long[] memo) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        memo[n] = fibonacciMemo(n - 1, memo) + fibonacciMemo(n - 2, memo);
        return memo[n];
    }

    // Tower of Hanoi
    public static void towerOfHanoi(int n, char from, char to, char aux) {
        if (n == 1) {
            System.out.println("Move disk 1 from " + from + " to " + to);
            return;
        }
        towerOfHanoi(n - 1, from, aux, to);
        System.out.println("Move disk " + n + " from " + from + " to " + to);
        towerOfHanoi(n - 1, aux, to, from);
    }

    // Binary search via recursion
    public static int binarySearch(int[] arr, int left, int right, int target) {
        if (left > right) return -1;
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] > target) return binarySearch(arr, left, mid - 1, target);
        return binarySearch(arr, mid + 1, right, target);
    }

    // Sum of digits
    public static int sumOfDigits(int n) {
        if (n == 0) return 0;
        return n % 10 + sumOfDigits(n / 10);
    }

    // Power (exponentiation)
    public static long power(int base, int exp) {
        if (exp == 0) return 1;
        return base * power(base, exp - 1);
    }

    // GCD using Euclidean algorithm
    public static int gcd(int a, int b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    public static void main(String[] args) {
        System.out.println("=== Factorial ===");
        for (int i = 0; i <= 10; i++) {
            System.out.println(i + "! = " + factorial(i));
        }

        System.out.println("\n=== Fibonacci (recursive) ===");
        for (int i = 0; i <= 15; i++) {
            System.out.print(fibonacci(i) + " ");
        }
        System.out.println();

        System.out.println("\n=== Fibonacci (memoized) ===");
        int n = 40;
        long[] memo = new long[n + 1];
        System.out.println("fib(" + n + ") = " + fibonacciMemo(n, memo));

        System.out.println("\n=== Tower of Hanoi (3 disks) ===");
        towerOfHanoi(3, 'A', 'C', 'B');

        System.out.println("\n=== Binary Search ===");
        int[] sorted = {2, 5, 8, 12, 16, 23, 38, 56, 72, 91};
        System.out.println("Array: " + Arrays.toString(sorted));
        int target = 23;
        int idx = binarySearch(sorted, 0, sorted.length - 1, target);
        System.out.println("Found " + target + " at index: " + idx);
        System.out.println("Search for 99: " + binarySearch(sorted, 0, sorted.length - 1, 99));

        System.out.println("\n=== Sum of Digits ===");
        System.out.println("sumOfDigits(12345): " + sumOfDigits(12345));

        System.out.println("\n=== Power ===");
        System.out.println("2^10 = " + power(2, 10));

        System.out.println("\n=== GCD ===");
        System.out.println("gcd(48, 18): " + gcd(48, 18));
        System.out.println("gcd(56, 98): " + gcd(56, 98));
    }
}
