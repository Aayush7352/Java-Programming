package phase03.advancedoop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class ComparatorExample {
    private final String name;
    private final double salary;
    private final int age;

    public ComparatorExample(String name, double salary, int age) {
        this.name = name;
        this.salary = salary;
        this.age = age;
    }

    public String getName() { return name; }
    public double getSalary() { return salary; }
    public int getAge() { return age; }

    @Override
    public String toString() {
        return name + " (age:" + age + ", salary:$" + salary + ")";
    }

    public static void main(String[] args) {
        List<ComparatorExample> employees = new ArrayList<>();
        employees.add(new ComparatorExample("Bob", 75000, 35));
        employees.add(new ComparatorExample("Alice", 85000, 28));
        employees.add(new ComparatorExample("Charlie", 75000, 42));
        employees.add(new ComparatorExample("David", 65000, 31));

        employees.sort(Comparator.comparing(ComparatorExample::getSalary)
                .thenComparing(ComparatorExample::getAge));
        System.out.println("Sorted by salary, then age: " + employees);

        employees.sort(Comparator.comparing(ComparatorExample::getName));
        System.out.println("Sorted by name: " + employees);

        employees.sort((e1, e2) -> Integer.compare(e2.getAge(), e1.getAge()));
        System.out.println("Sorted by age descending: " + employees);

        employees.sort(Comparator.comparingDouble(ComparatorExample::getSalary).reversed());
        System.out.println("Sorted by salary descending: " + employees);
    }
}
