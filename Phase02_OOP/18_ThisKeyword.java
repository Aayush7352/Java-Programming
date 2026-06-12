package phase02.oop;

class ThisKeyword {
    private int x;
    private int y;

    // Constructor chaining with this()
    public ThisKeyword() {
        this(0, 0);
        System.out.println("No-arg constructor");
    }

    public ThisKeyword(int x) {
        this(x, 0);
        System.out.println("One-arg constructor");
    }

    public ThisKeyword(int x, int y) {
        this.x = x;
        this.y = y;
        System.out.println("Two-arg constructor");
    }

    // Method chaining (fluent API)
    public ThisKeyword setX(int x) {
        this.x = x;
        return this;
    }

    public ThisKeyword setY(int y) {
        this.y = y;
        return this;
    }

    // Passing this as argument
    public void printInfo() {
        Printer.print(this);
    }

    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }

    public static void main(String[] args) {
        System.out.println("=== this() Constructor Chaining ===");
        @SuppressWarnings("unused")
        ThisKeyword p1 = new ThisKeyword();
        System.out.println();

        @SuppressWarnings("unused")
        ThisKeyword p2 = new ThisKeyword(5);
        System.out.println();

        @SuppressWarnings("unused")
        ThisKeyword p3 = new ThisKeyword(3, 7);
        System.out.println();

        // Method chaining (returning this)
        System.out.println("=== Method Chaining (fluent API) ===");
        ThisKeyword p4 = new ThisKeyword()
                .setX(10)
                .setY(20);
        System.out.println("After chaining: " + p4);

        // Passing this as argument
        System.out.println("\n=== Passing this as Argument ===");
        p4.printInfo();

        // Field shadowing
        System.out.println("\n=== Field Shadowing ===");
        String name = "Local";
        ThisKeyword demo = new ThisKeyword();
        demo.showShadowing(name);
    }

    public void showShadowing(String name) {
        // 'name' parameter shadows field (but we don't have a field 'name')
        // this.x.field shadowing example
        int x = 99; // local variable shadows field
        System.out.println("Local x: " + x);
        System.out.println("this.x: " + this.x);
    }
}

class Printer {
    public static void print(ThisKeyword obj) {
        System.out.println("Received via this: " + obj);
    }
}
