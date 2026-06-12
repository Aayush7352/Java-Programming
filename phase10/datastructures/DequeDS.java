package phase10.datastructures;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

class DNode<T> {
    T data;
    DNode<T> prev, next;

    DNode(T data) {
        this.data = data;
    }
}

public class DequeDS<T> {
    private DNode<T> head, tail;
    private int size;

    public void addFirst(T value) {
        DNode<T> node = new DNode<>(value);
        if (head == null) {
            head = tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
        size++;
    }

    public void addLast(T value) {
        DNode<T> node = new DNode<>(value);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        size++;
    }

    public T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        T data = head.data;
        head = head.next;
        if (head == null) tail = null;
        else head.prev = null;
        size--;
        return data;
    }

    public T removeLast() {
        if (isEmpty()) throw new NoSuchElementException();
        T data = tail.data;
        tail = tail.prev;
        if (tail == null) head = null;
        else tail.next = null;
        size--;
        return data;
    }

    public T peekFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        return head.data;
    }

    public T peekLast() {
        if (isEmpty()) throw new NoSuchElementException();
        return tail.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" <-> ");
        DNode<T> curr = head;
        while (curr != null) {
            sj.add(String.valueOf(curr.data));
            curr = curr.next;
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        DequeDS<Integer> deque = new DequeDS<>();
        deque.addFirst(20);
        deque.addFirst(10);
        deque.addLast(30);
        deque.addLast(40);
        System.out.println("Deque: " + deque);
        System.out.println("First: " + deque.peekFirst());
        System.out.println("Last: " + deque.peekLast());
        System.out.println("Remove first: " + deque.removeFirst());
        System.out.println("Remove last: " + deque.removeLast());
        System.out.println("After removes: " + deque);
    }
}
