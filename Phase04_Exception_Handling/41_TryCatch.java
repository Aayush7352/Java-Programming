package phase04.exceptionhandling;

class TryCatch {
    public static void main(String[] args) {
        try {
            int result = 10 / 0;
            System.out.println("Result: " + result);
        } catch (ArithmeticException e) {
            System.out.println("Exception caught!");
            System.out.println("getMessage(): " + e.getMessage());
            System.out.println("getClass(): " + e.getClass());
            System.out.println("toString(): " + e);
            System.out.println("Stack trace:");
            e.printStackTrace(System.out);
        }

        try {
            int[] arr = new int[3];
            System.out.println(arr[10]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("\nArray index out of bounds: " + e.getMessage());
        }

        System.out.println("Program continues after exception handling.");
    }
}
