package phase09.multithreading;

public class ThreadClassExample extends Thread {

    public ThreadClassExample(String name) {
        super(name);
    }

    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.printf("%s (priority=%d) - count: %d%n",
                    Thread.currentThread().getName(),
                    Thread.currentThread().getPriority(), i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadClassExample t1 = new ThreadClassExample("Alpha");
        ThreadClassExample t2 = new ThreadClassExample("Beta");

        t1.setPriority(Thread.MIN_PRIORITY);
        t2.setPriority(Thread.MAX_PRIORITY);

        System.out.println("t1 name: " + t1.getName() + ", priority: " + t1.getPriority());
        System.out.println("t2 name: " + t2.getName() + ", priority: " + t2.getPriority());

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Both threads finished.");
    }
}
