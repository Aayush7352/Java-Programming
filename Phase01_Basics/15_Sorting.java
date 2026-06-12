package phase01.basics;

import java.util.Arrays;

class Sorting {
    public static void main(String[] args) {
        demoSort("Bubble Sort", Sorting::bubbleSort);
        demoSort("Selection Sort", Sorting::selectionSort);
        demoSort("Insertion Sort", Sorting::insertionSort);
        demoSort("Merge Sort", Sorting::mergeSort);
        demoSort("Quick Sort", Sorting::quickSort);

        // Arrays.parallelSort
        System.out.println("\n=== Arrays.parallelSort (Java 8+) ===");
        int[] arr8 = {9, 1, 7, 3, 5, 8, 2, 6, 4};
        System.out.println("Before: " + Arrays.toString(arr8));
        Arrays.parallelSort(arr8);
        System.out.println("After:  " + Arrays.toString(arr8));

        // Arrays.sort with range
        System.out.println("\n=== Arrays.sort(range) ===");
        int[] arr9 = {9, 1, 7, 3, 5, 8, 2, 6, 4};
        System.out.println("Before: " + Arrays.toString(arr9));
        Arrays.sort(arr9, 2, 7);
        System.out.println("After sort(2,7): " + Arrays.toString(arr9));
    }

    @FunctionalInterface
    interface Sorter {
        void sort(int[] arr);
    }

    public static void demoSort(String name, Sorter sorter) {
        System.out.println("\n=== " + name + " ===");
        int[] arr = {9, 1, 7, 3, 5, 8, 2, 6, 4};
        System.out.println("Before: " + Arrays.toString(arr));
        sorter.sort(arr);
        System.out.println("After:  " + Arrays.toString(arr));
    }

    // Bubble Sort
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    // Selection Sort
    public static void selectionSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) minIdx = j;
            }
            int temp = arr[i];
            arr[i] = arr[minIdx];
            arr[minIdx] = temp;
        }
    }

    // Insertion Sort
    public static void insertionSort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    // Merge Sort
    public static void mergeSort(int[] arr) {
        if (arr.length < 2) return;
        int mid = arr.length / 2;
        int[] left = Arrays.copyOfRange(arr, 0, mid);
        int[] right = Arrays.copyOfRange(arr, mid, arr.length);
        mergeSort(left);
        mergeSort(right);
        merge(arr, left, right);
    }

    private static void merge(int[] arr, int[] left, int[] right) {
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            arr[k++] = left[i] <= right[j] ? left[i++] : right[j++];
        }
        while (i < left.length) arr[k++] = left[i++];
        while (j < right.length) arr[k++] = right[j++];
    }

    // Quick Sort
    public static void quickSort(int[] arr) {
        quickSort(arr, 0, arr.length - 1);
    }

    private static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
}
