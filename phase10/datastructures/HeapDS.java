package phase10.datastructures;

import java.util.Arrays;
import java.util.NoSuchElementException;

abstract class AbstractHeap {
    protected int[] heap;
    protected int size;
    protected int capacity;

    public AbstractHeap(int capacity) {
        this.capacity = capacity;
        this.heap = new int[capacity];
        this.size = 0;
    }

    public void insert(int value) {
        if (size == capacity) throw new IllegalStateException("Heap full");
        heap[size] = value;
        siftUp(size);
        size++;
    }

    public int extractRoot() {
        if (size == 0) throw new NoSuchElementException("Heap empty");
        int root = heap[0];
        heap[0] = heap[--size];
        siftDown(0);
        return root;
    }

    public int peek() {
        if (size == 0) throw new NoSuchElementException("Heap empty");
        return heap[0];
    }

    public int size() {
        return size;
    }

    public void heapify(int[] arr) {
        this.heap = Arrays.copyOf(arr, arr.length);
        this.size = arr.length;
        this.capacity = arr.length;
        for (int i = (size / 2) - 1; i >= 0; i--) siftDown(i);
    }

    protected abstract void siftUp(int idx);
    protected abstract void siftDown(int idx);

    protected int parent(int i) {
        return (i - 1) / 2;
    }

    protected int left(int i) {
        return 2 * i + 1;
    }

    protected int right(int i) {
        return 2 * i + 2;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(heap, size));
    }
}

class MinHeap extends AbstractHeap {
    public MinHeap(int capacity) {
        super(capacity);
    }

    @Override
    protected void siftUp(int idx) {
        while (idx > 0 && heap[idx] < heap[parent(idx)]) {
            swap(idx, parent(idx));
            idx = parent(idx);
        }
    }

    @Override
    protected void siftDown(int idx) {
        int smallest = idx;
        int l = left(idx), r = right(idx);
        if (l < size && heap[l] < heap[smallest]) smallest = l;
        if (r < size && heap[r] < heap[smallest]) smallest = r;
        if (smallest != idx) {
            swap(idx, smallest);
            siftDown(smallest);
        }
    }

    private void swap(int i, int j) {
        int tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }
}

class MaxHeap extends AbstractHeap {
    public MaxHeap(int capacity) {
        super(capacity);
    }

    @Override
    protected void siftUp(int idx) {
        while (idx > 0 && heap[idx] > heap[parent(idx)]) {
            swap(idx, parent(idx));
            idx = parent(idx);
        }
    }

    @Override
    protected void siftDown(int idx) {
        int largest = idx;
        int l = left(idx), r = right(idx);
        if (l < size && heap[l] > heap[largest]) largest = l;
        if (r < size && heap[r] > heap[largest]) largest = r;
        if (largest != idx) {
            swap(idx, largest);
            siftDown(largest);
        }
    }

    private void swap(int i, int j) {
        int tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }
}

public class HeapDS {

    public static void main(String[] args) {
        int[] data = {10, 3, 7, 15, 1, 9, 20};

        MinHeap minHeap = new MinHeap(20);
        for (int v : data) minHeap.insert(v);
        System.out.println("MinHeap: " + minHeap);
        System.out.println("Extract min: " + minHeap.extractRoot());
        System.out.println("After extract: " + minHeap);

        MaxHeap maxHeap = new MaxHeap(20);
        for (int v : data) maxHeap.insert(v);
        System.out.println("MaxHeap: " + maxHeap);
        System.out.println("Extract max: " + maxHeap.extractRoot());
        System.out.println("After extract: " + maxHeap);

        // heapify demo
        MinHeap heapified = new MinHeap(data.length);
        heapified.heapify(new int[]{5, 13, 2, 25, 7, 17, 20, 8, 4});
        System.out.println("Heapified: " + heapified);
    }
}
