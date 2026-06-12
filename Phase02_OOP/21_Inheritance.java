package phase02.oop;

class Animal {
    protected String name;

    public Animal(String name) {
        this.name = name;
    }

    public void eat() {
        System.out.println(name + " eats food");
    }

    public void sleep() {
        System.out.println(name + " sleeps");
    }
}

class Mammal extends Animal {
    protected boolean hasFur;

    public Mammal(String name, boolean hasFur) {
        super(name);
        this.hasFur = hasFur;
    }

    public void breatheAir() {
        System.out.println(name + " breathes air");
    }

    @Override
    public void eat() {
        System.out.println(name + " eats like a mammal");
    }
}

class Dog extends Mammal {
    private String breed;

    public Dog(String name, String breed) {
        super(name, true);
        this.breed = breed;
    }

    public void bark() {
        System.out.println(name + " barks!");
    }

    @Override
    public void eat() {
        super.eat();
        System.out.println(name + " specifically eats dog food");
    }

    public void displayHierarchy() {
        System.out.println("Dog -> Mammal -> Animal");
    }
}

class Inheritance {
    public static void main(String[] args) {
        System.out.println("=== Single Inheritance ===");
        Animal animal = new Animal("Generic Animal");
        animal.eat();
        animal.sleep();

        System.out.println("\n=== Multi-level Inheritance ===");
        Dog dog = new Dog("Buddy", "Golden Retriever");
        dog.eat();
        dog.sleep();
        dog.breatheAir();
        dog.bark();
        dog.displayHierarchy();

        System.out.println("\n=== super Keyword ===");
        Mammal mammal = new Mammal("Generic Mammal", true);
        mammal.eat();
        mammal.breatheAir();

        System.out.println("\n=== Method Inheritance ===");
        // sleep() is inherited all the way from Animal
        System.out.println("Dog can sleep (inherited):");
        dog.sleep();

        System.out.println("\n=== instanceof Checks ===");
        System.out.println("dog instanceof Dog: " + (dog instanceof Dog));
        System.out.println("dog instanceof Mammal: " + (dog instanceof Mammal));
        System.out.println("dog instanceof Animal: " + (dog instanceof Animal));
        System.out.println("dog instanceof Object: " + (dog instanceof Object));

        // Pattern matching with instanceof (Java 16+)
        System.out.println("\n=== Pattern Matching instanceof ===");
        Animal ref = new Dog("Max", "Beagle");
        if (ref instanceof Dog d) {
            d.bark();
        }
    }
}
