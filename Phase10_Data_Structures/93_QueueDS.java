package phase10.datastructures;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

class QueueDS<E> {

    private static final int DEFAULT_CAPACITY = 10;

    private final E[] elements;
    private int front;
    private int rear;
    private int count;

    @SuppressWarnings("unchecked")
    public QueueDS(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        elements = (E[]) new Object[capacity];
        front = 0;
        rear = -1;
        count = 0;
    }

    public QueueDS() {
        this(DEFAULT_CAPACITY);
    }

    public void enqueue(E element) {
        if (isFull()) {
            throw new IllegalStateException("Queue is full");
        }
        rear = (rear + 1) % elements.length;
        elements[rear] = element;
        count++;
    }

    public E dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        var value = elements[front];
        elements[front] = null;
        front = (front + 1) % elements.length;
        count--;
        return value;
    }

    public E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return elements[front];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == elements.length;
    }

    public int size() {
        return count;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(", ", "Queue[", "]");
        for (int i = 0; i < count; i++) {
            int idx = (front + i) % elements.length;
            sj.add(String.valueOf(elements[idx]));
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        var queue = new QueueDS<String>(5);

        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        System.out.println("Queue: " + queue);

        System.out.println("Peek: " + queue.peek());
        System.out.println("Dequeue: " + queue.dequeue());
        System.out.println("After dequeue: " + queue);

        queue.enqueue("D");
        queue.enqueue("E");
        queue.enqueue("F");
        System.out.println("Is full? " + queue.isFull());
        System.out.println("Queue: " + queue);

        while (!queue.isEmpty()) {
            System.out.println("Dequeue: " + queue.dequeue());
        }
        System.out.println("Is empty? " + queue.isEmpty());
    }
}
