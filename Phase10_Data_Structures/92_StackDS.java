package phase10.datastructures;

import java.util.EmptyStackException;
import java.util.StringJoiner;

class StackDS<E> {

    private static final int DEFAULT_CAPACITY = 10;

    private E[] elements;
    private int top;

    @SuppressWarnings("unchecked")
    public StackDS(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        elements = (E[]) new Object[capacity];
        top = -1;
    }

    public StackDS() {
        this(DEFAULT_CAPACITY);
    }

    public void push(E element) {
        if (isFull()) {
            throw new IllegalStateException("Stack is full");
        }
        elements[++top] = element;
    }

    public E pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        var value = elements[top];
        elements[top--] = null;
        return value;
    }

    public E peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return elements[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == elements.length - 1;
    }

    public int size() {
        return top + 1;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(", ", "Stack[", "]");
        for (int i = 0; i <= top; i++) {
            sj.add(String.valueOf(elements[i]));
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        var stack = new StackDS<Integer>(5);

        System.out.println("Is empty? " + stack.isEmpty());

        stack.push(10);
        stack.push(20);
        stack.push(30);
        System.out.println("Stack: " + stack);

        System.out.println("Peek: " + stack.peek());
        System.out.println("Pop: " + stack.pop());
        System.out.println("After pop: " + stack);

        stack.push(40);
        stack.push(50);
        stack.push(60);

        System.out.println("Is full? " + stack.isFull());
        System.out.println("Stack: " + stack);

        while (!stack.isEmpty()) {
            System.out.println("Popping: " + stack.pop());
        }
        System.out.println("Is empty? " + stack.isEmpty());
    }
}
