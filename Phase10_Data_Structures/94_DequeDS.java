package phase10.datastructures;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

class DequeDS<E> {

    private static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        Node(E data) {
            this.data = data;
        }
    }

    private Node<E> head;
    private Node<E> tail;
    private int size;

    public void addFirst(E element) {
        var newNode = new Node<>(element);
        if (head == null) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }

    public void addLast(E element) {
        var newNode = new Node<>(element);
        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }

    public E removeFirst() {
        if (head == null) throw new NoSuchElementException("Deque is empty");
        var data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return data;
    }

    public E removeLast() {
        if (tail == null) throw new NoSuchElementException("Deque is empty");
        var data = tail.data;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        size--;
        return data;
    }

    public E peekFirst() {
        if (head == null) throw new NoSuchElementException("Deque is empty");
        return head.data;
    }

    public E peekLast() {
        if (tail == null) throw new NoSuchElementException("Deque is empty");
        return tail.data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(" <-> ", "Deque[", "]");
        var current = head;
        while (current != null) {
            sj.add(String.valueOf(current.data));
            current = current.next;
        }
        return sj.toString();
    }

    public String toReverseString() {
        var sj = new StringJoiner(" <-> ", "DequeRev[", "]");
        var current = tail;
        while (current != null) {
            sj.add(String.valueOf(current.data));
            current = current.prev;
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        var deque = new DequeDS<String>();

        deque.addLast("B");
        deque.addLast("C");
        deque.addFirst("A");
        deque.addLast("D");
        System.out.println("Deque: " + deque);
        System.out.println("Reverse: " + deque.toReverseString());

        System.out.println("Remove first: " + deque.removeFirst());
        System.out.println("Remove last: " + deque.removeLast());
        System.out.println("After removals: " + deque);

        System.out.println("Peek first: " + deque.peekFirst());
        System.out.println("Peek last: " + deque.peekLast());

        deque.addFirst("X");
        deque.addLast("Y");
        System.out.println("Final: " + deque);
    }
}
