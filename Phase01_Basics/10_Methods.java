package phase01.basics;

class Methods {
    // Static method
    public static int addStatic(int a, int b) {
        return a + b;
    }

    // Instance method
    public int multiply(int a, int b) {
        return a * b;
    }

    // Varargs
    public static int sumAll(int... numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }

    // Method overloading
    public static int area(int side) {
        return side * side;
    }

    public static int area(int length, int breadth) {
        return length * breadth;
    }

    public static double area(double radius) {
        return Math.PI * radius * radius;
    }

    // Pass-by-value demonstration
    public static void modifyPrimitive(int x) {
        x = 100;
    }

    public static void modifyObject(StringBuilder sb) {
        sb.append(" modified");
    }

    public static void reassignObject(StringBuilder sb) {
        sb = new StringBuilder("new object");
    }

    public static void main(String[] args) {
        // Static method
        System.out.println("=== Static Method ===");
        int sum = Methods.addStatic(10, 20);
        System.out.println("addStatic(10,20): " + sum);

        // Instance method
        System.out.println("\n=== Instance Method ===");
        Methods obj = new Methods();
        int product = obj.multiply(5, 6);
        System.out.println("obj.multiply(5,6): " + product);

        // Varargs
        System.out.println("\n=== Varargs ===");
        System.out.println("sumAll(1,2,3): " + sumAll(1, 2, 3));
        System.out.println("sumAll(1,2,3,4,5): " + sumAll(1, 2, 3, 4, 5));
        System.out.println("sumAll(): " + sumAll());

        // Method overloading
        System.out.println("\n=== Method Overloading ===");
        System.out.println("area(5): " + area(5));
        System.out.println("area(5, 10): " + area(5, 10));
        System.out.println("area(2.5): " + area(2.5));

        // Pass-by-value demonstration
        System.out.println("\n=== Pass-by-Value ===");
        int num = 50;
        modifyPrimitive(num);
        System.out.println("After modifyPrimitive: " + num + " (unchanged)");

        StringBuilder sb = new StringBuilder("Hello");
        modifyObject(sb);
        System.out.println("After modifyObject: " + sb + " (modified)");

        reassignObject(sb);
        System.out.println("After reassignObject: " + sb + " (unchanged - reference is copy)");

        // Static method call without class name (in same class)
        System.out.println("\n=== Calling static method directly ===");
        System.out.println("addStatic(100, 200): " + addStatic(100, 200));
    }
}
