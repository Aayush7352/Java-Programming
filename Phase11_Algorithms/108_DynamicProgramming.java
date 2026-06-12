package phase11.algorithms;

import java.util.*;
import java.util.function.*;

class DynamicProgramming {

    // --- Fibonacci with Memoization ---
    public static long fibonacciMemo(int n) {
        long[] memo = new long[n + 1];
        Arrays.fill(memo, -1);
        return fibHelper(n, memo);
    }

    private static long fibHelper(int n, long[] memo) {
        if (n <= 1) return n;
        if (memo[n] != -1) return memo[n];
        memo[n] = fibHelper(n - 1, memo) + fibHelper(n - 2, memo);
        return memo[n];
    }

    // --- Fibonacci with Tabulation ---
    public static long fibonacciTab(int n) {
        if (n <= 1) return n;
        long[] dp = new long[n + 1];
        dp[0] = 0;
        dp[1] = 1;
        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        return dp[n];
    }

    // --- 0/1 Knapsack with Memoization ---
    public static int knapsackMemo(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] memo = new int[n + 1][capacity + 1];
        for (int[] row : memo) Arrays.fill(row, -1);
        return knapsackHelper(weights, values, capacity, n, memo);
    }

    private static int knapsackHelper(int[] weights, int[] values, int capacity,
                                       int n, int[][] memo) {
        if (n == 0 || capacity == 0) return 0;
        if (memo[n][capacity] != -1) return memo[n][capacity];

        if (weights[n - 1] > capacity) {
            memo[n][capacity] = knapsackHelper(weights, values, capacity, n - 1, memo);
        } else {
            int include = values[n - 1] + knapsackHelper(weights, values,
                    capacity - weights[n - 1], n - 1, memo);
            int exclude = knapsackHelper(weights, values, capacity, n - 1, memo);
            memo[n][capacity] = Math.max(include, exclude);
        }
        return memo[n][capacity];
    }

    // --- 0/1 Knapsack with Tabulation ---
    public static int knapsackTab(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                if (weights[i - 1] <= w) {
                    dp[i][w] = Math.max(
                            values[i - 1] + dp[i - 1][w - weights[i - 1]],
                            dp[i - 1][w]
                    );
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }
        return dp[n][capacity];
    }

    // --- Longest Common Subsequence with Memoization ---
    public static int lcsMemo(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] memo = new int[m + 1][n + 1];
        for (int[] row : memo) Arrays.fill(row, -1);
        return lcsHelper(text1, text2, m, n, memo);
    }

    private static int lcsHelper(String text1, String text2, int m, int n, int[][] memo) {
        if (m == 0 || n == 0) return 0;
        if (memo[m][n] != -1) return memo[m][n];

        if (text1.charAt(m - 1) == text2.charAt(n - 1)) {
            memo[m][n] = 1 + lcsHelper(text1, text2, m - 1, n - 1, memo);
        } else {
            memo[m][n] = Math.max(
                    lcsHelper(text1, text2, m - 1, n, memo),
                    lcsHelper(text1, text2, m, n - 1, memo)
            );
        }
        return memo[m][n];
    }

    // --- Longest Common Subsequence with Tabulation ---
    public static int lcsTab(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    // --- Longest Increasing Subsequence ---
    public static int lis(int[] arr) {
        int n = arr.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        int maxLIS = 1;

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (arr[i] > arr[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLIS = Math.max(maxLIS, dp[i]);
        }
        return maxLIS;
    }

    // --- Edit Distance (Levenshtein) ---
    public static int editDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }

    public static void main(String[] args) {
        System.out.println("=== Fibonacci ===");
        System.out.println("fibMemo(10) = " + fibonacciMemo(10));
        System.out.println("fibTab(10)  = " + fibonacciTab(10));
        System.out.println("fibMemo(50) = " + fibonacciMemo(50));

        System.out.println("\n=== 0/1 Knapsack ===");
        int[] weights = {2, 3, 4, 5};
        int[] values = {3, 4, 5, 6};
        int capacity = 8;
        System.out.println("Knapsack Memo: " + knapsackMemo(weights, values, capacity));
        System.out.println("Knapsack Tab:  " + knapsackTab(weights, values, capacity));

        System.out.println("\n=== Longest Common Subsequence ===");
        String s1 = "ABCDGH", s2 = "AEDFHR";
        System.out.println("LCS of '" + s1 + "' and '" + s2 + "': " + lcsMemo(s1, s2));
        System.out.println("LCS Tab: " + lcsTab(s1, s2));

        System.out.println("\n=== Longest Increasing Subsequence ===");
        int[] arr = {10, 22, 9, 33, 21, 50, 41, 60, 80};
        System.out.println("LIS length: " + lis(arr));

        System.out.println("\n=== Edit Distance ===");
        System.out.println("Edit distance between 'kitten' and 'sitting': "
                + editDistance("kitten", "sitting"));
    }
}
