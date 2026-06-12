package phase07.java8plus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

record Employee(String name, String department, double salary) {}

public class StreamsAPI {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(3, 7, 2, 9, 7, 2, 5, 1, 6, 8, 4);

        // filter + map + sorted + distinct
        var result = numbers.stream()
                .filter(n -> n > 4)
                .map(n -> n * 2)
                .sorted()
                .distinct()
                .toList();
        System.out.println("filter+map+sorted+distinct: " + result);

        // limit + skip
        var limited = numbers.stream()
                .sorted()
                .skip(2)
                .limit(3)
                .toList();
        System.out.println("skip 2, limit 3: " + limited);

        // reduce
        var sum = numbers.stream().reduce(0, Integer::sum);
        System.out.println("reduce sum: " + sum);

        // flatMap
        var nested = List.of(List.of(1, 2), List.of(3, 4, 5), List.of(6));
        var flat = nested.stream()
                .flatMap(List::stream)
                .toList();
        System.out.println("flatMap: " + flat);

        // collect: joining
        var joined = numbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        System.out.println("joining: " + joined);

        // groupingBy
        var employees = List.of(
                new Employee("Alice", "Eng", 90000),
                new Employee("Bob", "Eng", 85000),
                new Employee("Carol", "Sales", 70000),
                new Employee("Dave", "Sales", 72000),
                new Employee("Eve", "Eng", 95000)
        );
        Map<String, List<Employee>> byDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::department));
        System.out.println("groupingBy: " + byDept.keySet());

        // partitioningBy
        Map<Boolean, List<Employee>> highEarners = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.salary() > 80000));
        System.out.println("partitioningBy >80k: " + highEarners.get(true).size());

        // toMap
        Map<String, String> nameMap = employees.stream()
                .collect(Collectors.toMap(Employee::name, Employee::department));
        System.out.println("toMap: " + nameMap);

        // anyMatch / allMatch / noneMatch
        boolean anyHigh = employees.stream().anyMatch(e -> e.salary() > 90000);
        System.out.println("anyMatch >90k: " + anyHigh);

        // Stream.generate / iterate (infinite then limit)
        var generated = Stream.generate(() -> Math.random())
                .limit(3)
                .toList();
        System.out.println("Stream.generate: " + generated);

        var iterated = Stream.iterate(1, n -> n + 2)
                .limit(5)
                .toList();
        System.out.println("Stream.iterate: " + iterated);

        // takeWhile / dropWhile
        var taken = numbers.stream()
                .sorted()
                .takeWhile(n -> n < 6)
                .toList();
        System.out.println("takeWhile <6: " + taken);

        var dropped = numbers.stream()
                .sorted()
                .dropWhile(n -> n < 6)
                .toList();
        System.out.println("dropWhile <6: " + dropped);
    }
}
