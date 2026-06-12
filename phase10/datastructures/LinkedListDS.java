package phase10.datastructures;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

class Node<T> {
    T data;
    Node<T> next;

    Node(T data) {
        this.data = data;
    }
}

public class LinkedListDS<T> {
    private Node<T> head;
    private int size;

    public void add(T value) {
        Node<T> newNode = new Node<>(value);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> curr = head;
            while (curr.next != null) curr = curr.next;
            curr.next = newNode;
        }
        size++;
    }

    public boolean remove(T value) {
        if (head == null) return false;
        if (head.data.equals(value)) {
            head = head.next;
            size--;
            return true;
        }
        Node<T> curr = head;
        while (curr.next != null && !curr.next.data.equals(value)) curr = curr.next;
        if (curr.next == null) return false;
        curr.next = curr.next.next;
        size--;
        return true;
    }

    public boolean find(T value) {
        Node<T> curr = head;
        while (curr != null) {
            if (curr.data.equals(value)) return true;
            curr = curr.next;
        }
        return false;
    }

    public void reverse() {
        Node<T> prev = null, curr = head;
        while (curr != null) {
            Node<T> next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        head = prev;
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" -> ");
        Node<T> curr = head;
        while (curr != null) {
            sj.add(String.valueOf(curr.data));
            curr = curr.next;
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        LinkedListDS<Integer> list = new LinkedListDS<>();
        list.add(10);
        list.add(20);
        list.add(30);
        System.out.println("List: " + list);

        System.out.println("Find 20: " + list.find(20));
        System.out.println("Find 99: " + list.find(99));

        list.remove(20);
        System.out.println("After remove 20: " + list);

        list.reverse();
        System.out.println("Reversed: " + list);
    }
}
