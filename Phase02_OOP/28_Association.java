package phase02.oop;

import java.util.ArrayList;
import java.util.List;

class Teacher {
    private String name;
    private List<Student> students;

    public Teacher(String name) {
        this.name = name;
        this.students = new ArrayList<>();
    }

    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
            student.addTeacher(this);
        }
    }

    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public String getName() { return name; }

    public void displayInfo() {
        System.out.println("Teacher: " + name);
        System.out.println("  Students: " + students.stream()
                .map(Student::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none"));
    }
}

class Student {
    private String name;
    private List<Teacher> teachers;

    public Student(String name) {
        this.name = name;
        this.teachers = new ArrayList<>();
    }

    public void addTeacher(Teacher teacher) {
        if (!teachers.contains(teacher)) {
            teachers.add(teacher);
        }
    }

    public String getName() { return name; }

    public List<Teacher> getTeachers() {
        return new ArrayList<>(teachers);
    }

    public void displayInfo() {
        System.out.println("Student: " + name);
        System.out.println("  Teachers: " + teachers.stream()
                .map(Teacher::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none"));
    }
}

class Association {
    public static void main(String[] args) {
        System.out.println("=== Bidirectional Association (Teacher <-> Student) ===");

        Teacher t1 = new Teacher("Mr. Smith");
        Teacher t2 = new Teacher("Ms. Johnson");

        Student s1 = new Student("Alice");
        Student s2 = new Student("Bob");
        Student s3 = new Student("Charlie");

        // Establish bidirectional relationships
        t1.addStudent(s1);
        t1.addStudent(s2);
        t2.addStudent(s2);
        t2.addStudent(s3);

        System.out.println("\n--- Teacher View ---");
        t1.displayInfo();
        System.out.println();
        t2.displayInfo();

        System.out.println("\n--- Student View ---");
        s1.displayInfo();
        System.out.println();
        s2.displayInfo();
        System.out.println();
        s3.displayInfo();

        System.out.println("\n=== Unidirectional Association ===");
        System.out.println("""
                In a unidirectional association, only one class knows about the other.
                For example, a Library knows about its Books, but Books don't know about the Library.
                """);
        Library library = new Library("City Library");
        library.addBook(new Book("1984", "George Orwell"));
        library.addBook(new Book("To Kill a Mockingbird", "Harper Lee"));
        library.displayInfo();
    }
}

// Unidirectional association example
class Book {
    private String title;
    private String author;

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }

    @Override
    public String toString() {
        return "'" + title + "' by " + author;
    }
}

class Library {
    private String name;
    private List<Book> books;

    public Library(String name) {
        this.name = name;
        this.books = new ArrayList<>();
    }

    public void addBook(Book book) {
        books.add(book);
    }

    public void displayInfo() {
        System.out.println(name + " has " + books.size() + " books:");
        books.forEach(b -> System.out.println("  - " + b));
    }
}
