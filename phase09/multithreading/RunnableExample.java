package phase09.multithreading;

public class RunnableExample {

    static class Task implements Runnable {
        private final String name;

        Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            System.out.println(name + " running in " + Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) {
        // Runnable implemented as a class
        Thread t1 = new Thread(new Task("TaskClass"), "thread-class");

        // Runnable as lambda
        Runnable lambdaTask = () ->
                System.out.println("Lambda task in " + Thread.currentThread().getName());
        Thread t2 = new Thread(lambdaTask, "thread-lambda");

        // Anonymous Runnable
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous task in " + Thread.currentThread().getName());
            }
        }, "thread-anon");

        t1.start();
        t2.start();
        t3.start();
    }
}
