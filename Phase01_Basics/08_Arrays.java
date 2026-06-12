package phase01.basics;

class ArrayDemo {
    public static void main(String[] args) {
        // 1D Array
        System.out.println("=== 1D Array ===");
        int[] arr1 = {5, 2, 8, 1, 9};
        int[] arr2 = new int[5];
        for (int i = 0; i < arr2.length; i++) {
            arr2[i] = i * 10;
        }
        System.out.println("arr1: " + java.util.Arrays.toString(arr1));
        System.out.println("arr2: " + java.util.Arrays.toString(arr2));

        // 2D Array
        System.out.println("\n=== 2D Array ===");
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        for (int i = 0; i < matrix.length; i++) {
            System.out.println(java.util.Arrays.toString(matrix[i]));
        }

        // Jagged Array
        System.out.println("\n=== Jagged Array ===");
        int[][] jagged = new int[4][];
        jagged[0] = new int[]{1};
        jagged[1] = new int[]{2, 3};
        jagged[2] = new int[]{4, 5, 6};
        jagged[3] = new int[]{7, 8, 9, 10};
        for (int i = 0; i < jagged.length; i++) {
            System.out.println(java.util.Arrays.toString(jagged[i]));
        }

        // Array copy
        System.out.println("\n=== Array Copy ===");
        int[] source = {10, 20, 30, 40, 50};
        int[] dest = new int[5];
        System.arraycopy(source, 0, dest, 0, source.length);
        System.out.println("Copied: " + java.util.Arrays.toString(dest));

        int[] partial = java.util.Arrays.copyOf(source, 3);
        System.out.println("copyOf first 3: " + java.util.Arrays.toString(partial));

        int[] range = java.util.Arrays.copyOfRange(source, 1, 4);
        System.out.println("copyOfRange(1,4): " + java.util.Arrays.toString(range));

        // java.util.Arrays utility class
        System.out.println("\n=== java.util.Arrays Utility Class ===");
        int[] nums = {9, 1, 7, 3, 5};
        java.util.Arrays.sort(nums);
        System.out.println("Sorted: " + java.util.Arrays.toString(nums));

        int idx = java.util.Arrays.binarySearch(nums, 5);
        System.out.println("Binary search for 5: index " + idx);

        int[] filled = new int[5];
        java.util.Arrays.fill(filled, 42);
        System.out.println("Fill with 42: " + java.util.Arrays.toString(filled));

        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        System.out.println("java.util.Arrays.equals: " + java.util.Arrays.equals(a1, a2));

        System.out.println("\n=== Array Stream ===");
        int sum = java.util.Arrays.stream(arr1).sum();
        System.out.println("Sum of arr1: " + sum);
    }
}
