package phase10.datastructures;

import java.util.EmptyStackException;

public class StackDS<T> {
    private Object[] arr;
    private int top;
    private int capacity;

    public StackDS(int capacity) {
        this.capacity = capacity;
        this.arr = new Object[capacity];
        this.top = -1;
    }

    public void push(T value) {
        if (isFull()) throw new IllegalStateException("Stack is full");
        arr[++top] = value;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) throw new EmptyStackException();
        return (T) arr[top--];
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new EmptyStackException();
        return (T) arr[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == capacity - 1;
    }

    public int size() {
        return top + 1;
    }

    public static void main(String[] args) {
        StackDS<Integer> stack = new StackDS<>(5);
        System.out.println("isEmpty: " + stack.isEmpty());

        stack.push(10);
        stack.push(20);
        stack.push(30);
        System.out.println("Peek: " + stack.peek());
        System.out.println("Pop: " + stack.pop());
        System.out.println("Pop: " + stack.pop());
        System.out.println("Size: " + stack.size());
        System.out.println("isEmpty: " + stack.isEmpty());
        System.out.println("isFull: " + stack.isFull());
    }
}
