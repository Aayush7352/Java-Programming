package phase03.advancedoop;

class InnerClasses {
    private String message = "Hello from outer class";

    class Inner {
        private String message = "Hello from inner class";

        public void display() {
            String message = "Hello from local scope";
            System.out.println("Local: " + message);
            System.out.println("Inner: " + this.message);
            System.out.println("Outer: " + InnerClasses.this.message);
        }
    }

    public void createInner() {
        Inner inner = new Inner();
        inner.display();
    }

    public static void main(String[] args) {
        InnerClasses outer = new InnerClasses();
        outer.createInner();

        System.out.println();

        InnerClasses.Inner inner2 = outer.new Inner();
        inner2.display();

        System.out.println("\nInner classes hold a reference to the outer class instance.");
    }
}
