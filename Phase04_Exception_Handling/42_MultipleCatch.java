package phase04.exceptionhandling;

class MultipleCatch {
    public static void main(String[] args) {
        try {
            String input = args.length > 0 ? args[0] : "abc";
            int num = Integer.parseInt(input);
            int result = 100 / num;
            System.out.println("Result: " + result);
        } catch (ArithmeticException e) {
            System.out.println("Arithmetic: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Bad number: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("No argument: " + e.getMessage());
        }

        try {
            String s = null;
            System.out.println(s.length());
            int[] arr = new int[2];
            System.out.println(arr[5]);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Multi-catch caught: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }
    }
}
