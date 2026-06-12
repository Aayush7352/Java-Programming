package phase10.datastructures;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

class LinkedListDS<E> {

    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data) {
            this.data = data;
        }
    }

    private Node<E> head;
    private int size;

    public void addFirst(E element) {
        var newNode = new Node<>(element);
        newNode.next = head;
        head = newNode;
        size++;
    }

    public void addLast(E element) {
        var newNode = new Node<>(element);
        if (head == null) {
            head = newNode;
        } else {
            var current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }

    public E removeFirst() {
        if (head == null) throw new NoSuchElementException("List is empty");
        var data = head.data;
        head = head.next;
        size--;
        return data;
    }

    public boolean remove(E element) {
        if (head == null) return false;
        if (equals(head.data, element)) {
            head = head.next;
            size--;
            return true;
        }
        var current = head;
        while (current.next != null) {
            if (equals(current.next.data, element)) {
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public boolean contains(E element) {
        var current = head;
        while (current != null) {
            if (equals(current.data, element)) return true;
            current = current.next;
        }
        return false;
    }

    public void reverse() {
        Node<E> prev = null;
        var current = head;
        while (current != null) {
            var next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }
        head = prev;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(" -> ", "[", "]");
        var current = head;
        while (current != null) {
            sj.add(String.valueOf(current.data));
            current = current.next;
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        var list = new LinkedListDS<String>();
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        list.addFirst("Z");
        System.out.println("List: " + list);

        list.remove("B");
        System.out.println("After removing B: " + list);

        System.out.println("Contains A? " + list.contains("A"));
        System.out.println("Contains X? " + list.contains("X"));

        list.reverse();
        System.out.println("Reversed: " + list);

        System.out.println("Removed first: " + list.removeFirst());
        System.out.println("Final: " + list);
    }
}
