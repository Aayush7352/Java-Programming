package phase04.exceptionhandling;

class CustomExceptions {
    static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) {
            super(message);
        }

        public InsufficientFundsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class InvalidTransactionException extends RuntimeException {
        public InvalidTransactionException(String message) {
            super(message);
        }

        public InvalidTransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class BankAccount {
        private double balance;

        public BankAccount(double balance) {
            this.balance = balance;
        }

        public void withdraw(double amount) throws InsufficientFundsException {
            if (amount <= 0) {
                throw new InvalidTransactionException("Amount must be positive: " + amount);
            }
            if (amount > balance) {
                throw new InsufficientFundsException(
                        "Need $" + amount + " but only have $" + balance);
            }
            balance -= amount;
            System.out.println("Withdrew $" + amount + ", new balance: $" + balance);
        }
    }

    public static void main(String[] args) {
        BankAccount account = new BankAccount(500);

        try {
            account.withdraw(600);
        } catch (InsufficientFundsException e) {
            System.out.println("Checked: " + e.getMessage());
        }

        try {
            account.withdraw(-50);
        } catch (InvalidTransactionException e) {
            System.out.println("Unchecked: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("Checked: " + e.getMessage());
        }

        try {
            account.withdraw(100);
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage());
        }
    }
}
