package phase10.datastructures;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

interface Heap {
    void insert(int value);
    int extractRoot();
    int peek();
    int size();
    boolean isEmpty();
}

class MinHeap implements Heap {

    private static final int DEFAULT_CAPACITY = 16;
    private int[] heap;
    private int size;

    public MinHeap() {
        heap = new int[DEFAULT_CAPACITY];
        size = 0;
    }

    public MinHeap(int[] array) {
        heap = Arrays.copyOf(array, Math.max(array.length, DEFAULT_CAPACITY));
        size = array.length;
        heapify();
    }

    private void heapify() {
        for (int i = (size / 2) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    @Override
    public void insert(int value) {
        ensureCapacity();
        heap[size] = value;
        siftUp(size);
        size++;
    }

    @Override
    public int extractRoot() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        var root = heap[0];
        heap[0] = heap[--size];
        heap[size] = 0;
        siftDown(0);
        return root;
    }

    @Override
    public int peek() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        return heap[0];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap[index] >= heap[parent]) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (true) {
            int smallest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            if (left < size && heap[left] < heap[smallest]) smallest = left;
            if (right < size && heap[right] < heap[smallest]) smallest = right;
            if (smallest == index) break;
            swap(index, smallest);
            index = smallest;
        }
    }

    private void ensureCapacity() {
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length * 2);
        }
    }

    private void swap(int i, int j) {
        int tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(heap, size));
    }
}

class MaxHeap implements Heap {

    private static final int DEFAULT_CAPACITY = 16;
    private int[] heap;
    private int size;

    public MaxHeap() {
        heap = new int[DEFAULT_CAPACITY];
        size = 0;
    }

    public MaxHeap(int[] array) {
        heap = Arrays.copyOf(array, Math.max(array.length, DEFAULT_CAPACITY));
        size = array.length;
        heapify();
    }

    private void heapify() {
        for (int i = (size / 2) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    @Override
    public void insert(int value) {
        ensureCapacity();
        heap[size] = value;
        siftUp(size);
        size++;
    }

    @Override
    public int extractRoot() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        var root = heap[0];
        heap[0] = heap[--size];
        heap[size] = 0;
        siftDown(0);
        return root;
    }

    @Override
    public int peek() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        return heap[0];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap[index] <= heap[parent]) break;
            swap(index, parent);
            index = parent;
        }
    }

    public void siftDown(int index) {
        while (true) {
            int largest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            if (left < size && heap[left] > heap[largest]) largest = left;
            if (right < size && heap[right] > heap[largest]) largest = right;
            if (largest == index) break;
            swap(index, largest);
            index = largest;
        }
    }

    private void ensureCapacity() {
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length * 2);
        }
    }

    private void swap(int i, int j) {
        int tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(heap, size));
    }
}

class HeapDS {

    public static void main(String[] args) {
        System.out.println("=== Min-Heap ===");
        var minHeap = new MinHeap(new int[]{9, 4, 7, 1, 3, 8, 2, 5, 6});
        System.out.println("Heapified array: " + minHeap);
        System.out.println("Peek min: " + minHeap.peek());
        System.out.println("Extract min: " + minHeap.extractRoot());
        System.out.println("After extract: " + minHeap);
        minHeap.insert(0);
        System.out.println("After insert 0: " + minHeap);
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.extractRoot() + " ");
        }
        System.out.println();

        System.out.println("\n=== Max-Heap ===");
        var maxHeap = new MaxHeap(new int[]{1, 3, 5, 7, 9, 2, 4, 6, 8});
        System.out.println("Heapified array: " + maxHeap);
        System.out.println("Peek max: " + maxHeap.peek());
        System.out.println("Extract max: " + maxHeap.extractRoot());
        System.out.println("After extract: " + maxHeap);
        maxHeap.insert(10);
        System.out.println("After insert 10: " + maxHeap);
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.extractRoot() + " ");
        }
        System.out.println();
    }
}
