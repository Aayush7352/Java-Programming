package phase03.advancedoop;

class NestedClasses {
    private static String outerStatic = "Outer static field";
    private String outerInstance = "Outer instance field";

    static class StaticNested {
        private String nestedField = "Static nested field";

        public void display() {
            System.out.println("StaticNested: can access outerStatic = " + outerStatic);
            System.out.println("StaticNested: own field = " + nestedField);
        }
    }

    public void demonstrateLocalClass() {
        final String localVar = "Local variable from outer method";

        class MethodLocalInner {
            private final String innerName;

            MethodLocalInner(String name) {
                this.innerName = name;
            }

            public void show() {
                System.out.println("MethodLocalInner '" + innerName
                        + "': can access localVar = " + localVar
                        + " and outerInstance = " + outerInstance);
            }
        }

        MethodLocalInner inner = new MethodLocalInner("test");
        inner.show();
    }

    public static void main(String[] args) {
        StaticNested nested = new StaticNested();
        nested.display();

        System.out.println();

        NestedClasses outer = new NestedClasses();
        outer.demonstrateLocalClass();

        String shadowed = "main-level shadow variable";
        System.out.println("\nShadowing example: shadowed = " + shadowed);
    }
}
