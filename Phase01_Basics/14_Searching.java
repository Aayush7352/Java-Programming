package phase01.basics;

import java.util.Arrays;

class Searching {
    public static void main(String[] args) {
        int[] arr = {34, 7, 23, 32, 5, 62, 31, 17, 89, 45};
        int target = 23;

        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Searching for: " + target);

        // Linear search
        System.out.println("\n=== Linear Search ===");
        int linearIdx = linearSearch(arr, target);
        System.out.println("Found at index: " + linearIdx);

        int notFound = linearSearch(arr, 999);
        System.out.println("Search 999: " + notFound + " (not found)");

        // Binary search (iterative) - array must be sorted
        int[] sortedArr = arr.clone();
        Arrays.sort(sortedArr);
        System.out.println("\nSorted array: " + Arrays.toString(sortedArr));

        System.out.println("\n=== Binary Search (Iterative) ===");
        int iterIdx = binarySearchIterative(sortedArr, 32);
        System.out.println("Found 32 at index: " + iterIdx + " (value: " + sortedArr[iterIdx] + ")");
        System.out.println("Search 999: " + binarySearchIterative(sortedArr, 999));

        // Binary search (recursive)
        System.out.println("\n=== Binary Search (Recursive) ===");
        int recIdx = binarySearchRecursive(sortedArr, 0, sortedArr.length - 1, 62);
        System.out.println("Found 62 at index: " + recIdx + " (value: " + sortedArr[recIdx] + ")");
        System.out.println("Search 999: " + binarySearchRecursive(sortedArr, 0, sortedArr.length - 1, 999));

        // Java built-in binarySearch
        System.out.println("\n=== Arrays.binarySearch ===");
        int builtinIdx = Arrays.binarySearch(sortedArr, 17);
        System.out.println("Arrays.binarySearch for 17: " + builtinIdx);

        // Linear search with strings
        System.out.println("\n=== Linear Search (String) ===");
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve"};
        int strIdx = linearSearch(names, "Charlie");
        System.out.println("Found 'Charlie' at index: " + strIdx);
    }

    public static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) return i;
        }
        return -1;
    }

    public static int linearSearch(String[] arr, String target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(target)) return i;
        }
        return -1;
    }

    public static int binarySearchIterative(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) return mid;
            if (arr[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    public static int binarySearchRecursive(int[] arr, int left, int right, int target) {
        if (left > right) return -1;
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) return binarySearchRecursive(arr, mid + 1, right, target);
        return binarySearchRecursive(arr, left, mid - 1, target);
    }
}
