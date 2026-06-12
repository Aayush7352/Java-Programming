package phase09.multithreading;

class PrintTask implements Runnable {
    private final String prefix;

    public PrintTask(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 3; i++) {
            System.out.println(prefix + " - count " + i + " on " + Thread.currentThread().getName());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

class RunnableExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Runnable example (82) ===");

        Thread t1 = new Thread(new PrintTask("Runnable-impl"));
        t1.start();

        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                System.out.println("Lambda-Runnable - count " + i + " on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        t2.start();

        t1.join();
        t2.join();
        System.out.println("Both runnables completed.");
    }
}
