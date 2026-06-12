package phase03.advancedoop;

import java.util.Arrays;

class Cloneable implements java.lang.Cloneable {
    private final String name;
    private final int[] scores;

    public Cloneable(String name, int[] scores) {
        this.name = name;
        this.scores = scores;
    }

    @Override
    public Cloneable clone() {
        try {
            Cloneable cloned = (Cloneable) super.clone();
            cloned.scores[0] = 999;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public Cloneable deepCopy() {
        return new Cloneable(this.name, this.scores.clone());
    }

    @Override
    public String toString() {
        return "Student{name='" + name + "', scores=" + Arrays.toString(scores) + "}";
    }

    public static void main(String[] args) {
        int[] originalScores = {85, 90, 78};
        Cloneable original = new Cloneable("Alice", originalScores);
        System.out.println("Original: " + original);

        Cloneable shallow = original.clone();
        System.out.println("Shallow : " + shallow);

        Cloneable deep = original.deepCopy();
        System.out.println("Deep    : " + deep);

        System.out.println("\nShallow copy shares scores array reference: " +
                (original.scores == shallow.scores));
        System.out.println("Deep copy has its own scores array: " +
                (original.scores != deep.scores));

        System.out.println("\nCloneable is a marker interface (no methods to implement).");
    }
}
