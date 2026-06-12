package phase10.datastructures;

import java.util.NoSuchElementException;

public class QueueDS<T> {
    private Object[] arr;
    private int front, rear, size, capacity;

    public QueueDS(int capacity) {
        this.capacity = capacity;
        this.arr = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    public void enqueue(T value) {
        if (isFull()) throw new IllegalStateException("Queue is full");
        rear = (rear + 1) % capacity;
        arr[rear] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) throw new NoSuchElementException("Queue is empty");
        T value = (T) arr[front];
        front = (front + 1) % capacity;
        size--;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("Queue is empty");
        return (T) arr[front];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int size() {
        return size;
    }

    public static void main(String[] args) {
        QueueDS<String> queue = new QueueDS<>(4);
        System.out.println("isEmpty: " + queue.isEmpty());

        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        System.out.println("Peek: " + queue.peek());
        System.out.println("Dequeue: " + queue.dequeue());
        System.out.println("Dequeue: " + queue.dequeue());
        queue.enqueue("D");
        queue.enqueue("E");
        System.out.println("Size: " + queue.size());
        System.out.println("isFull: " + queue.isFull());

        while (!queue.isEmpty()) {
            System.out.println("Dequeue: " + queue.dequeue());
        }
    }
}
