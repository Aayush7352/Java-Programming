package phase04.exceptionhandling;

class Finally {
    public static void main(String[] args) {
        System.out.println("=== Finally always executes ===");
        try {
            System.out.println("Inside try block");
            int[] arr = new int[2];
            System.out.println(arr[5]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Inside catch block");
        } finally {
            System.out.println("Finally block always executes");
        }

        System.out.println("\n=== Finally with exception still propagating ===");
        try {
            throw new RuntimeException("Boom!");
        } catch (RuntimeException e) {
            System.out.println("Caught: " + e.getMessage());
        } finally {
            System.out.println("Finally still runs");
        }

        System.out.println("\n=== Finally with System.exit() ===");
        try {
            System.out.println("Before System.exit(0) - finally will NOT run");
            System.exit(0);
        } finally {
            System.out.println("This will NOT execute (JVM shuts down)");
        }
    }
}
