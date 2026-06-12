package phase07.java8plus;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

class Person {
    private final String name;

    public Person() { this.name = "Default"; }
    public Person(String name) { this.name = name; }

    public String getName() { return name; }

    public String introduce() { return "I am " + name; }

    public static String greet(String name) { return "Hello, " + name; }
}

public class MethodReferences {
    public static void main(String[] args) {
        // Static method ref
        Function<String, String> greeter = Person::greet;
        System.out.println(greeter.apply("Alice"));

        // Instance method ref (bound receiver)
        Person bob = new Person("Bob");
        Supplier<String> intro = bob::introduce;
        System.out.println(intro.get());

        // Arbitrary object method ref (unbound receiver)
        List<Person> people = List.of(new Person("Eve"), new Person("Dave"));
        people.stream()
              .map(Person::getName)
              .forEach(System.out::println);

        // Constructor ref
        Supplier<Person> factory = Person::new;
        Supplier<Person> namedFactory = () -> new Person("Charlie");
        System.out.println(namedFactory.get().getName());

        // Constructor ref with parameter
        Function<String, Person> personCreator = Person::new;
        Person p = personCreator.apply("Frank");
        System.out.println(p.introduce());

        // Array constructor ref
        Function<Integer, Person[]> arrayCreator = Person[]::new;
        Person[] arr = arrayCreator.apply(3);
        System.out.println("Array length: " + arr.length);
    }
}
