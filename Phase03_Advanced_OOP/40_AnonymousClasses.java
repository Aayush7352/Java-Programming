package phase03.advancedoop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class AnonymousClasses {
    static abstract class Greeter {
        abstract String greet(String name);
    }

    interface ClickHandler {
        void onClick();
    }

    public static void main(String[] args) {
        Greeter greeter = new Greeter() {
            @Override
            String greet(String name) {
                return "Hello, " + name + "!";
            }
        };
        System.out.println(greeter.greet("Alice"));

        ClickHandler handler = new ClickHandler() {
            @Override
            public void onClick() {
                System.out.println("Button clicked!");
            }
        };
        handler.onClick();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous Runnable running.");
            }
        };
        new Thread(task).start();

        List<String> names = new ArrayList<>(List.of("Charlie", "Alice", "Bob"));
        names.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Integer.compare(a.length(), b.length());
            }
        });
        System.out.println("Sorted by length: " + names);
    }
}
