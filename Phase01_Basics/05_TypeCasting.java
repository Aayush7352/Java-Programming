package phase01.basics;

class TypeCasting {
    public static void main(String[] args) {
        // Implicit (widening) casting
        System.out.println("=== Implicit Casting ===");
        int i = 100;
        long l = i;
        float f = l;
        double d = f;
        System.out.println("int " + i + " -> long " + l + " -> float " + f + " -> double " + d);

        char ch = 'A';
        int charToInt = ch;
        System.out.println("char '" + ch + "' -> int " + charToInt);

        // Explicit (narrowing) casting
        System.out.println("\n=== Explicit Casting ===");
        double pi = 3.14159;
        int truncated = (int) pi;
        System.out.println("double " + pi + " -> int " + truncated);

        long big = 100_000_000_000L;
        int narrowed = (int) big;
        System.out.println("long " + big + " -> int " + narrowed + " (data loss)");

        int ascii = 66;
        char intToChar = (char) ascii;
        System.out.println("int " + ascii + " -> char '" + intToChar + "'");

        // Wrapper class conversions
        System.out.println("\n=== Wrapper Conversions ===");
        String numStr = "1234";
        int parsedInt = Integer.parseInt(numStr);
        Integer valueOfInt = Integer.valueOf(numStr);
        System.out.println("parseInt(\"" + numStr + "\"): " + parsedInt);
        System.out.println("valueOf(\"" + numStr + "\"): " + valueOfInt);

        double parsedDouble = Double.parseDouble("3.1415");
        Double valueOfDouble = Double.valueOf("3.1415");
        System.out.println("parseDouble: " + parsedDouble);
        System.out.println("valueOf: " + valueOfDouble);

        // Primitive to wrapper (autoboxing)
        int primitive = 42;
        Integer wrapped = primitive;
        System.out.println("\nAutoboxing: int " + primitive + " -> Integer " + wrapped);

        // Wrapper to primitive (unboxing)
        Integer wrapper = 99;
        int unboxed = wrapper;
        System.out.println("Unboxing: Integer " + wrapper + " -> int " + unboxed);

        // int <-> String conversions
        int number = 255;
        String binary = Integer.toBinaryString(number);
        String hex = Integer.toHexString(number);
        String octal = Integer.toOctalString(number);
        System.out.println("\nint " + number + " -> binary: " + binary + ", hex: " + hex + ", octal: " + octal);

        String binStr = "11111111";
        int fromBinary = Integer.parseInt(binStr, 2);
        System.out.println("binary \"" + binStr + "\" -> int " + fromBinary);
    }
}
