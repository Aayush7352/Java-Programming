package phase03.advancedoop;

record Person(String name, int age) {
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative: " + age);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
    }

    public Person(String name) {
        this(name, 0);
    }

    public String greeting() {
        return "Hello, my name is " + name() + " and I am " + age() + " years old.";
    }
}

class Records {
    public static void main(String[] args) {
        Person alice = new Person("Alice", 30);
        Person bob = new Person("Bob");
        Person charlie = new Person("Charlie", 25);

        System.out.println("alice: " + alice);
        System.out.println("bob: " + bob);
        System.out.println("charlie: " + charlie);

        System.out.println("\nAccessors: name=" + alice.name() + ", age=" + alice.age());
        System.out.println(alice.greeting());

        System.out.println("\nalice.equals(charlie): " + alice.equals(charlie));

        record LocalPoint(int x, int y) {}
        LocalPoint p = new LocalPoint(10, 20);
        System.out.println("Local record: " + p);

        try {
            new Person(null, 25);
        } catch (IllegalArgumentException e) {
            System.out.println("Validation works: " + e.getMessage());
        }
    }
}
