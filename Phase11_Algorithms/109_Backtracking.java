package phase11.algorithms;

import java.util.*;
import java.util.stream.*;

class Backtracking {

    // --- N-Queens ---
    public static List<List<String>> solveNQueens(int n) {
        List<List<String>> results = new ArrayList<>();
        char[][] board = new char[n][n];
        for (char[] row : board) Arrays.fill(row, '.');
        solveNQueensHelper(board, 0, results);
        return results;
    }

    private static void solveNQueensHelper(char[][] board, int row, List<List<String>> results) {
        int n = board.length;
        if (row == n) {
            List<String> solution = new ArrayList<>();
            for (char[] r : board) solution.add(new String(r));
            results.add(solution);
            return;
        }
        for (int col = 0; col < n; col++) {
            if (isSafe(board, row, col)) {
                board[row][col] = 'Q';
                solveNQueensHelper(board, row + 1, results);
                board[row][col] = '.';
            }
        }
    }

    private static boolean isSafe(char[][] board, int row, int col) {
        int n = board.length;

        for (int i = 0; i < row; i++) {
            if (board[i][col] == 'Q') return false;
        }

        for (int i = row - 1, j = col - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j] == 'Q') return false;
        }

        for (int i = row - 1, j = col + 1; i >= 0 && j < n; i--, j++) {
            if (board[i][j] == 'Q') return false;
        }
        return true;
    }

    // --- Sudoku Solver ---
    public static boolean solveSudoku(int[][] board) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (board[row][col] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isValidSudokuMove(board, row, col, num)) {
                            board[row][col] = num;
                            if (solveSudoku(board)) return true;
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isValidSudokuMove(int[][] board, int row, int col, int num) {
        for (int x = 0; x < 9; x++) {
            if (board[row][x] == num) return false;
            if (board[x][col] == num) return false;
        }

        int boxRow = row - row % 3;
        int boxCol = col - col % 3;
        for (int i = boxRow; i < boxRow + 3; i++) {
            for (int j = boxCol; j < boxCol + 3; j++) {
                if (board[i][j] == num) return false;
            }
        }
        return true;
    }

    // --- Permutations ---
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> results = new ArrayList<>();
        boolean[] used = new boolean[nums.length];
        permuteHelper(nums, used, new ArrayList<>(), results);
        return results;
    }

    private static void permuteHelper(int[] nums, boolean[] used,
                                       List<Integer> current, List<List<Integer>> results) {
        if (current.size() == nums.length) {
            results.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current.add(nums[i]);
                permuteHelper(nums, used, current, results);
                current.removeLast();
                used[i] = false;
            }
        }
    }

    // --- Subsets ---
    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> results = new ArrayList<>();
        subsetsHelper(nums, 0, new ArrayList<>(), results);
        return results;
    }

    private static void subsetsHelper(int[] nums, int index,
                                       List<Integer> current, List<List<Integer>> results) {
        results.add(new ArrayList<>(current));
        for (int i = index; i < nums.length; i++) {
            current.add(nums[i]);
            subsetsHelper(nums, i + 1, current, results);
            current.removeLast();
        }
    }

    // --- Combination Sum ---
    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> results = new ArrayList<>();
        combinationSumHelper(candidates, target, 0, new ArrayList<>(), results);
        return results;
    }

    private static void combinationSumHelper(int[] candidates, int remaining, int start,
                                              List<Integer> current, List<List<Integer>> results) {
        if (remaining == 0) {
            results.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] <= remaining) {
                current.add(candidates[i]);
                combinationSumHelper(candidates, remaining - candidates[i], i, current, results);
                current.removeLast();
            }
        }
    }

    public static void printBoard(List<String> board) {
        board.forEach(System.out::println);
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("=== N-Queens (4x4) ===");
        var queens = solveNQueens(4);
        System.out.println("Total solutions: " + queens.size());
        for (int i = 0; i < queens.size(); i++) {
            System.out.println("Solution " + (i + 1) + ":");
            printBoard(queens.get(i));
        }

        System.out.println("=== Sudoku Solver ===");
        int[][] sudoku = {
                {5, 3, 0, 0, 7, 0, 0, 0, 0},
                {6, 0, 0, 1, 9, 5, 0, 0, 0},
                {0, 9, 8, 0, 0, 0, 0, 6, 0},
                {8, 0, 0, 0, 6, 0, 0, 0, 3},
                {4, 0, 0, 8, 0, 3, 0, 0, 1},
                {7, 0, 0, 0, 2, 0, 0, 0, 6},
                {0, 6, 0, 0, 0, 0, 2, 8, 0},
                {0, 0, 0, 4, 1, 9, 0, 0, 5},
                {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };
        if (solveSudoku(sudoku)) {
            System.out.println("Solved:");
            for (int[] row : sudoku) {
                System.out.println(Arrays.toString(row));
            }
        } else {
            System.out.println("No solution exists");
        }

        System.out.println("\n=== Permutations of [1,2,3] ===");
        var permutations = permute(new int[]{1, 2, 3});
        System.out.println(permutations);

        System.out.println("\n=== Subsets of [1,2,3] ===");
        var subsetList = subsets(new int[]{1, 2, 3});
        System.out.println(subsetList);

        System.out.println("\n=== Combination Sum (target=7, candidates=[2,3,6,7]) ===");
        var combos = combinationSum(new int[]{2, 3, 6, 7}, 7);
        System.out.println(combos);
    }
}
