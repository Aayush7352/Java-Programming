package phase05.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

public class StackExample {
    public static void main(String[] args) {
        // Legacy Stack
        Stack<String> stack = new Stack<>();
        stack.push("Bottom");
        stack.push("Middle");
        stack.push("Top");
        System.out.println("Stack: " + stack);

        System.out.println("peek: " + stack.peek());
        System.out.println("pop: " + stack.pop());
        System.out.println("After pop: " + stack);
        System.out.println("search('Bottom'): " + stack.search("Bottom"));
        System.out.println("search('Missing'): " + stack.search("Missing"));
        System.out.println("empty: " + stack.empty());

        // Deque as Stack (preferred, Java 21)
        Deque<String> dequeStack = new ArrayDeque<>();
        dequeStack.push("First");
        dequeStack.push("Second");
        dequeStack.push("Third");
        System.out.println("\nDeque as Stack: " + dequeStack);

        System.out.println("peek: " + dequeStack.peek());
        System.out.println("pop: " + dequeStack.pop());
        System.out.println("After pop: " + dequeStack);

        // Deque-specific stack operations
        dequeStack.addFirst("NewTop");
        System.out.println("After addFirst: " + dequeStack);
        System.out.println("removeFirst: " + dequeStack.removeFirst());
        System.out.println("After removeFirst: " + dequeStack);

        // Demonstrate LIFO behavior
        System.out.println("\nLIFO order:");
        Deque<Integer> lifo = new ArrayDeque<>();
        for (int i = 1; i <= 5; i++) lifo.push(i);
        while (!lifo.isEmpty()) System.out.print(lifo.pop() + " ");
        System.out.println();
    }
}
