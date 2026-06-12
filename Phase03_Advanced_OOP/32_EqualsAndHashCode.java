package phase03.advancedoop;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class EqualsAndHashCode {
    static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    record PersonRecord(String name, int age) {}

    public static void main(String[] args) {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        Person p3 = new Person("Bob", 25);

        System.out.println("=== Hand-written equals/hashCode ===");
        System.out.println("p1.equals(p2): " + p1.equals(p2));
        System.out.println("p1.equals(p3): " + p1.equals(p3));

        Set<Person> personSet = new HashSet<>();
        personSet.add(p1);
        personSet.add(p2);
        personSet.add(p3);
        System.out.println("Set size (expected 2): " + personSet.size());

        System.out.println("\n=== Record auto-implementation ===");
        PersonRecord r1 = new PersonRecord("Alice", 30);
        PersonRecord r2 = new PersonRecord("Alice", 30);
        PersonRecord r3 = new PersonRecord("Bob", 25);

        System.out.println("r1.equals(r2): " + r1.equals(r2));
        System.out.println("r1.equals(r3): " + r1.equals(r3));

        Set<PersonRecord> recordSet = new HashSet<>();
        recordSet.add(r1);
        recordSet.add(r2);
        recordSet.add(r3);
        System.out.println("Record set size (expected 2): " + recordSet.size());
    }
}
