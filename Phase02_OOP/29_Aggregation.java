package phase02.oop;

import java.util.ArrayList;
import java.util.List;

class Professor {
    private String name;
    private String speciality;

    public Professor(String name, String speciality) {
        this.name = name;
        this.speciality = speciality;
    }

    public String getName() { return name; }

    public String getSpeciality() { return speciality; }

    public void teach() {
        System.out.println(name + " is teaching " + speciality);
    }

    @Override
    public String toString() {
        return "Prof. " + name + " (" + speciality + ")";
    }
}

class Department {
    private String name;
    private List<Professor> professors;

    public Department(String name) {
        this.name = name;
        this.professors = new ArrayList<>();
    }

    public void addProfessor(Professor professor) {
        professors.add(professor);
        System.out.println(professor.getName() + " joined " + name);
    }

    public void removeProfessor(Professor professor) {
        professors.remove(professor);
        System.out.println(professor.getName() + " left " + name);
    }

    public void listProfessors() {
        System.out.println(name + " department has " + professors.size() + " professor(s):");
        for (Professor p : professors) {
            System.out.println("  - " + p);
        }
    }

    public String getName() { return name; }
}

class University {
    private String name;
    private List<Department> departments;

    public University(String name) {
        this.name = name;
        this.departments = new ArrayList<>();
    }

    public void addDepartment(Department dept) {
        departments.add(dept);
    }

    public void displayStructure() {
        System.out.println("=== " + name + " ===");
        for (Department dept : departments) {
            dept.listProfessors();
        }
    }
}

class Aggregation {
    public static void main(String[] args) {
        System.out.println("=== Aggregation: Department HAS-A Professor ===");
        System.out.println("(Weak relationship - Professor can exist without Department)\n");

        // Professors exist independently
        Professor p1 = new Professor("Dr. Smith", "Computer Science");
        Professor p2 = new Professor("Dr. Johnson", "Mathematics");
        Professor p3 = new Professor("Dr. Williams", "Physics");

        // Departments
        Department csDept = new Department("Computer Science");
        Department mathDept = new Department("Mathematics");

        // Aggregation: adding professors to departments
        csDept.addProfessor(p1);
        csDept.addProfessor(p3);
        mathDept.addProfessor(p2);

        System.out.println();
        csDept.listProfessors();
        System.out.println();
        mathDept.listProfessors();

        // Department outlives Professor (weak relationship)
        System.out.println("\n=== Weak Relationship Demo ===");
        System.out.println("Professors exist independently of departments:");
        p1.teach();
        p2.teach();
        p3.teach();

        // University aggregates departments
        System.out.println("\n=== University Aggregation ===");
        University university = new University("Springfield University");
        university.addDepartment(csDept);
        university.addDepartment(mathDept);
        university.displayStructure();

        // Even if Department is removed, Professor still exists
        System.out.println("\n=== Department Removed, Professors Survive ===");
        csDept.removeProfessor(p1);
        csDept.removeProfessor(p3);
        System.out.println("\nProfessors still exist independently:");
        p1.teach();
        p3.teach();
    }
}
