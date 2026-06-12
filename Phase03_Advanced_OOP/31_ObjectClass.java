package phase03.advancedoop;

import java.util.Objects;

class ObjectClass {
    private final String name;
    private final int id;

    public ObjectClass(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ObjectClass{name='" + name + "', id=" + id + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectClass that = (ObjectClass) o;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    public static void main(String[] args) {
        ObjectClass obj1 = new ObjectClass("Alice", 101);
        ObjectClass obj2 = new ObjectClass("Alice", 101);
        ObjectClass obj3 = new ObjectClass("Bob", 102);

        System.out.println("obj1 = " + obj1);
        System.out.println("obj2 = " + obj2);
        System.out.println("obj3 = " + obj3);

        System.out.println("obj1.equals(obj2): " + obj1.equals(obj2));
        System.out.println("obj1.equals(obj3): " + obj1.equals(obj3));
        System.out.println("obj1.hashCode() = " + obj1.hashCode());
        System.out.println("obj2.hashCode() = " + obj2.hashCode());
        System.out.println("obj1.getClass() = " + obj1.getClass());
        System.out.println("obj1.getClass().getName() = " + obj1.getClass().getName());
    }
}
