package phase16.projects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class BankingSystem {

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST, FEE
    }

    public static record Transaction(String id, TransactionType type, BigDecimal amount,
                                      BigDecimal balanceBefore, BigDecimal balanceAfter,
                                      String description, long timestamp) {
        public Transaction {
            Objects.requireNonNull(id);
            Objects.requireNonNull(type);
            Objects.requireNonNull(amount);
            Objects.requireNonNull(balanceBefore);
            Objects.requireNonNull(balanceAfter);
            Objects.requireNonNull(description);
        }
    }

    public static abstract sealed class Account permits SavingsAccount, CheckingAccount {
        protected final String accountNumber;
        protected final String ownerName;
        protected BigDecimal balance;
        protected final List<Transaction> transactions;
        protected final Lock lock = new ReentrantLock();
        protected static final AtomicInteger accountCounter = new AtomicInteger(1000);

        protected Account(String ownerName, BigDecimal initialDeposit) {
            this.accountNumber = "ACC-" + accountCounter.incrementAndGet();
            this.ownerName = Objects.requireNonNull(ownerName);
            this.balance = initialDeposit.compareTo(BigDecimal.ZERO) >= 0 ? initialDeposit : BigDecimal.ZERO;
            this.transactions = new ArrayList<>();
            if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                addTransaction(TransactionType.DEPOSIT, initialDeposit, "Initial deposit");
            }
        }

        public abstract String getAccountType();
        public abstract void applyMonthlyMaintenance();

        public boolean deposit(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
            lock.lock();
            try {
                var before = balance;
                balance = balance.add(amount);
                addTransaction(TransactionType.DEPOSIT, amount, "Deposit");
                return true;
            } finally {
                lock.unlock();
            }
        }

        public boolean withdraw(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
            lock.lock();
            try {
                if (balance.compareTo(amount) < 0) return false;
                var before = balance;
                balance = balance.subtract(amount);
                addTransaction(TransactionType.WITHDRAWAL, amount, "Withdrawal");
                return true;
            } finally {
                lock.unlock();
            }
        }

        public boolean transferTo(Account destination, BigDecimal amount) {
            Objects.requireNonNull(destination);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

            var firstLock = this.accountNumber.compareTo(destination.accountNumber) < 0
                    ? this.lock : destination.lock;
            var secondLock = this.accountNumber.compareTo(destination.accountNumber) < 0
                    ? destination.lock : this.lock;

            firstLock.lock();
            try {
                secondLock.lock();
                try {
                    if (balance.compareTo(amount) < 0) return false;

                    var beforeSrc = balance;
                    balance = balance.subtract(amount);
                    addTransaction(TransactionType.TRANSFER_OUT, amount,
                            "Transfer to " + destination.accountNumber);

                    destination.balance = destination.balance.add(amount);
                    destination.addTransaction(TransactionType.TRANSFER_IN, amount,
                            "Transfer from " + this.accountNumber);
                    return true;
                } finally {
                    secondLock.unlock();
                }
            } finally {
                firstLock.unlock();
            }
        }

        protected void addTransaction(TransactionType type, BigDecimal amount, String description) {
            var ts = System.currentTimeMillis();
            var before = type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN
                    ? balance.subtract(amount) : balance.add(amount);
            transactions.add(new Transaction(
                    accountNumber + "-TXN-" + (transactions.size() + 1),
                    type, amount, before, balance, description, ts));
        }

        public BigDecimal getBalance() { lock.lock(); try { return balance; } finally { lock.unlock(); } }
        public String getAccountNumber() { return accountNumber; }
        public String getOwnerName() { return ownerName; }
        public List<Transaction> getTransactions() { lock.lock(); try { return List.copyOf(transactions); } finally { lock.unlock(); } }

        @Override
        public String toString() {
            return "%s[%s] owner=%s balance=$%s".formatted(
                    getAccountType(), accountNumber, ownerName,
                    NumberFormat.getCurrencyInstance(Locale.US).format(balance));
        }
    }

    public static final class SavingsAccount extends Account {
        private static final BigDecimal INTEREST_RATE = new BigDecimal("0.035");
        private static final int FREE_WITHDRAWALS = 6;
        private int monthlyWithdrawals;

        public SavingsAccount(String ownerName, BigDecimal initialDeposit) {
            super(ownerName, initialDeposit);
            this.monthlyWithdrawals = 0;
        }

        @Override
        public String getAccountType() { return "Savings"; }

        @Override
        public void applyMonthlyMaintenance() {
            lock.lock();
            try {
                var interest = balance.multiply(INTEREST_RATE).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                balance = balance.add(interest);
                addTransaction(TransactionType.INTEREST, interest, "Monthly interest at %d%% APR"
                        .formatted(INTEREST_RATE.multiply(BigDecimal.valueOf(100)).intValue()));
                monthlyWithdrawals = 0;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean withdraw(BigDecimal amount) {
            lock.lock();
            try {
                if (monthlyWithdrawals >= FREE_WITHDRAWALS) {
                    var fee = new BigDecimal("2.00");
                    var total = amount.add(fee);
                    if (balance.compareTo(total) < 0) return false;
                    balance = balance.subtract(fee);
                    addTransaction(TransactionType.FEE, fee, "Excess withdrawal fee");
                }
                monthlyWithdrawals++;
                return super.withdraw(amount);
            } finally {
                lock.unlock();
            }
        }
    }

    public static final class CheckingAccount extends Account {
        private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("200.00");
        private static final BigDecimal MONTHLY_FEE = new BigDecimal("5.00");

        public CheckingAccount(String ownerName, BigDecimal initialDeposit) {
            super(ownerName, initialDeposit);
        }

        @Override
        public String getAccountType() { return "Checking"; }

        @Override
        public void applyMonthlyMaintenance() {
            lock.lock();
            try {
                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    balance = balance.subtract(MONTHLY_FEE);
                    addTransaction(TransactionType.FEE, MONTHLY_FEE, "Monthly maintenance fee");
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean withdraw(BigDecimal amount) {
            lock.lock();
            try {
                var maxWithdrawal = balance.add(OVERDRAFT_LIMIT);
                if (maxWithdrawal.compareTo(amount) < 0) return false;
                return super.withdraw(amount);
            } finally {
                lock.unlock();
            }
        }
    }

    public static final class Bank {
        private final String name;
        private final Map<String, Account> accounts = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public Bank(String name) {
            this.name = Objects.requireNonNull(name);
            scheduler.scheduleAtFixedRate(this::monthlyMaintenance, 30, 30, TimeUnit.SECONDS);
        }

        public SavingsAccount createSavingsAccount(String owner, BigDecimal initialDeposit) {
            var acc = new SavingsAccount(owner, initialDeposit);
            accounts.put(acc.getAccountNumber(), acc);
            return acc;
        }

        public CheckingAccount createCheckingAccount(String owner, BigDecimal initialDeposit) {
            var acc = new CheckingAccount(owner, initialDeposit);
            accounts.put(acc.getAccountNumber(), acc);
            return acc;
        }

        public Account getAccount(String accountNumber) {
            return accounts.get(accountNumber);
        }

        public boolean transfer(String fromAcc, String toAcc, BigDecimal amount) {
            var src = accounts.get(fromAcc);
            var dst = accounts.get(toAcc);
            if (src == null || dst == null) return false;
            return src.transferTo(dst, amount);
        }

        public List<Account> getAllAccounts() {
            return List.copyOf(accounts.values());
        }

        public BigDecimal getTotalBankBalance() {
            return accounts.values().stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private void monthlyMaintenance() {
            accounts.values().forEach(Account::applyMonthlyMaintenance);
        }

        public void shutdown() {
            scheduler.shutdown();
        }

        public String getName() { return name; }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Banking System ===%n".formatted());

        var bank = new Bank("Global Trust Bank");

        var aliceSavings = bank.createSavingsAccount("Alice Johnson", new BigDecimal("5000.00"));
        var bobChecking = bank.createCheckingAccount("Bob Smith", new BigDecimal("2500.00"));
        var carolSavings = bank.createSavingsAccount("Carol Williams", new BigDecimal("10000.00"));

        System.out.println("--- Accounts ---");
        bank.getAllAccounts().forEach(a -> System.out.println("  " + a));

        System.out.println("%n--- Deposits and Withdrawals ---%n".formatted());
        aliceSavings.deposit(new BigDecimal("1000.00"));
        System.out.println("Alice deposited $1,000: " + aliceSavings);

        bobChecking.withdraw(new BigDecimal("500.00"));
        System.out.println("Bob withdrew $500: " + bobChecking);

        System.out.println("%n--- Transfer: Alice -> Bob ($750) ---%n".formatted());
        var transferred = bank.transfer(aliceSavings.getAccountNumber(), bobChecking.getAccountNumber(),
                new BigDecimal("750.00"));
        System.out.println("Transfer success: " + transferred);
        System.out.println("Alice: " + aliceSavings);
        System.out.println("Bob: " + bobChecking);

        System.out.println("%n--- Thread-Safe Concurrent Transfers ---%n".formatted());
        var latch = new CountDownLatch(10);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        var amount = new BigDecimal("10.00");
                        var success = bank.transfer(aliceSavings.getAccountNumber(),
                                bobChecking.getAccountNumber(), amount);
                        System.out.println("  [VT-%d] Transfer $10: %s".formatted(idx, success));
                    } catch (Exception e) {
                        System.out.println("  [VT-%d] Error: %s".formatted(idx, e.getMessage()));
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        latch.await(5, TimeUnit.SECONDS);
        System.out.println("%nAfter 10 concurrent transfers:".formatted());
        System.out.println("  Alice: " + aliceSavings);
        System.out.println("  Bob: " + bobChecking);

        System.out.println("%n--- Overdraft Test (Checking) ---%n".formatted());
        var charlieCheck = bank.createCheckingAccount("Charlie Brown", new BigDecimal("100.00"));
        var bigWithdraw = charlieCheck.withdraw(new BigDecimal("250.00"));
        System.out.println("Withdraw $250 from $100 limit: " + bigWithdraw + " (should be true, OD limit $200)");
        var overLimit = charlieCheck.withdraw(new BigDecimal("100.00"));
        System.out.println("Withdraw another $100: " + overLimit + " (should be false)");
        System.out.println("Charlie: " + charlieCheck);

        System.out.println("%n--- Pattern Matching on Accounts ---%n".formatted());
        for (var acc : bank.getAllAccounts()) {
            switch (acc) {
                case SavingsAccount s when s.getBalance().compareTo(new BigDecimal("5000")) > 0 ->
                    System.out.println("  High-value Savings: " + s.getOwnerName() + " ($" + s.getBalance() + ")");
                case SavingsAccount s ->
                    System.out.println("  Standard Savings: " + s.getOwnerName());
                case CheckingAccount c ->
                    System.out.println("  Checking: " + c.getOwnerName() + " (OD limit: $200)");
            }
        }

        System.out.println("%n--- Alice's Transactions ---%n".formatted());
        aliceSavings.getTransactions().forEach(t ->
            System.out.println("  [%s] %-12s $%-6s -> $%-6s %s".formatted(
                    t.id(), t.type(), t.amount(), t.balanceAfter(), t.description())));

        System.out.println("%nTotal Bank Balance: %s".formatted(
                NumberFormat.getCurrencyInstance(Locale.US).format(bank.getTotalBankBalance())));

        bank.shutdown();
        System.out.println("=== Done ===");
    }
}
