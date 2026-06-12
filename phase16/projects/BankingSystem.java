package phase16.projects;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/**
 * BankingSystem.java
 *
 * Banking system with abstract Account, SavingsAccount, CheckingAccount,
 * Bank (account management), transactions, deposit/withdraw/transfer with
 * thread safety using locks.
 */
public class BankingSystem {

    // ═══════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════

    sealed abstract static class Account {
        private final String accountNumber;
        private final String ownerName;
        private volatile double balance;
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final String accountType;
        private final LocalDateTime createdAt;

        public Account(String accountNumber, String ownerName, double initialBalance, String accountType) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
            this.balance = initialBalance;
            this.accountType = accountType;
            this.createdAt = LocalDateTime.now();
        }

        public String getAccountNumber() { return accountNumber; }
        public String getOwnerName() { return ownerName; }
        public String getAccountType() { return accountType; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        public double getBalance() {
            rwLock.readLock().lock();
            try { return balance; } finally { rwLock.readLock().unlock(); }
        }

        protected void setBalance(double amount) {
            rwLock.writeLock().lock();
            try { this.balance = amount; } finally { rwLock.writeLock().unlock(); }
        }

        public abstract double getWithdrawalLimit();
        public abstract double getMonthlyFee();

        public boolean deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
            rwLock.writeLock().lock();
            try {
                balance += amount;
                return true;
            } finally { rwLock.writeLock().unlock(); }
        }

        public boolean withdraw(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
            if (amount > getWithdrawalLimit()) throw new IllegalArgumentException("Exceeds withdrawal limit");
            rwLock.writeLock().lock();
            try {
                if (balance < amount) throw new IllegalStateException("Insufficient funds");
                balance -= amount;
                return true;
            } finally { rwLock.writeLock().unlock(); }
        }

        // Lock methods for transfer (avoid deadlock)
        public void acquireLocks(Account other) {
            int h1 = System.identityHashCode(this);
            int h2 = System.identityHashCode(other);
            if (h1 < h2) {
                this.rwLock.writeLock().lock();
                other.rwLock.writeLock().lock();
            } else if (h1 > h2) {
                other.rwLock.writeLock().lock();
                this.rwLock.writeLock().lock();
            } else {
                this.rwLock.writeLock().lock();
                other.rwLock.writeLock().lock();
            }
        }

        public void releaseLocks(Account other) {
            this.rwLock.writeLock().unlock();
            other.rwLock.writeLock().unlock();
        }

        protected void unsafeDeposit(double amount) { this.balance += amount; }
        protected void unsafeWithdraw(double amount) { this.balance -= amount; }
    }

    static final class SavingsAccount extends Account {
        private static final double WITHDRAWAL_LIMIT = 10000;
        private static final double MONTHLY_FEE = 5.0;
        private final double interestRate;

        SavingsAccount(String accountNumber, String ownerName, double initialBalance, double interestRate) {
            super(accountNumber, ownerName, initialBalance, "Savings");
            this.interestRate = interestRate;
        }

        public double getInterestRate() { return interestRate; }
        @Override public double getWithdrawalLimit() { return WITHDRAWAL_LIMIT; }
        @Override public double getMonthlyFee() { return MONTHLY_FEE; }

        public double calculateInterest(int months) {
            return getBalance() * interestRate * months / 12.0;
        }
    }

    static final class CheckingAccount extends Account {
        private static final double WITHDRAWAL_LIMIT = 50000;
        private static final double MONTHLY_FEE = 12.0;
        private final double overdraftLimit;

        CheckingAccount(String accountNumber, String ownerName, double initialBalance, double overdraftLimit) {
            super(accountNumber, ownerName, initialBalance, "Checking");
            this.overdraftLimit = overdraftLimit;
        }

        public double getOverdraftLimit() { return overdraftLimit; }
        @Override public double getWithdrawalLimit() { return WITHDRAWAL_LIMIT; }
        @Override public double getMonthlyFee() { return MONTHLY_FEE; }

        @Override
        public boolean withdraw(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
            if (amount > getWithdrawalLimit()) throw new IllegalArgumentException("Exceeds withdrawal limit");
            // Allow overdraft up to limit
            if (getBalance() - amount < -overdraftLimit) {
                throw new IllegalStateException("Overdraft limit exceeded");
            }
            setBalance(getBalance() - amount);
            return true;
        }
    }

    record Transaction(String transactionId, String fromAccount, String toAccount, double amount,
                       LocalDateTime timestamp, TransactionType type, String description) {
        enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, FEE, INTEREST }
    }

    record AccountSummary(String accountNumber, String ownerName, String type, double balance,
                          double withdrawalLimit, double monthlyFee, LocalDateTime created) {}

    // ═══════════════════════════════════════════════
    // Bank
    // ═══════════════════════════════════════════════

    static final class Bank {
        private final String name;
        private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<Transaction>> transactionsByAccount = new ConcurrentHashMap<>();
        private final AtomicLong transactionCounter = new AtomicLong(0);
        private final AtomicInteger accountCounter = new AtomicInteger(1000);

        Bank(String name) { this.name = name; }

        public String getName() { return name; }

        public SavingsAccount createSavingsAccount(String ownerName, double initialDeposit, double interestRate) {
            if (initialDeposit < 0) throw new IllegalArgumentException("Initial deposit must be >= 0");
            String acctNum = "SAV-" + accountCounter.incrementAndGet();
            var account = new SavingsAccount(acctNum, ownerName, initialDeposit, interestRate);
            accounts.put(acctNum, account);
            transactionsByAccount.put(acctNum, new CopyOnWriteArrayList<>());
            if (initialDeposit > 0) {
                recordTransaction(acctNum, null, initialDeposit, Transaction.TransactionType.DEPOSIT,
                    "Initial deposit");
            }
            return account;
        }

        public CheckingAccount createCheckingAccount(String ownerName, double initialDeposit, double overdraftLimit) {
            if (initialDeposit < 0) throw new IllegalArgumentException("Initial deposit must be >= 0");
            String acctNum = "CHK-" + accountCounter.incrementAndGet();
            var account = new CheckingAccount(acctNum, ownerName, initialDeposit, overdraftLimit);
            accounts.put(acctNum, account);
            transactionsByAccount.put(acctNum, new CopyOnWriteArrayList<>());
            if (initialDeposit > 0) {
                recordTransaction(acctNum, null, initialDeposit, Transaction.TransactionType.DEPOSIT,
                    "Initial deposit");
            }
            return account;
        }

        public Optional<Account> findAccount(String accountNumber) {
            return Optional.ofNullable(accounts.get(accountNumber));
        }

        public Transaction deposit(String accountNumber, double amount) {
            var account = accounts.get(accountNumber);
            if (account == null) throw new IllegalArgumentException("Account not found: " + accountNumber);
            account.deposit(amount);
            return recordTransaction(accountNumber, null, amount, Transaction.TransactionType.DEPOSIT,
                "Deposit");
        }

        public Transaction withdraw(String accountNumber, double amount) {
            var account = accounts.get(accountNumber);
            if (account == null) throw new IllegalArgumentException("Account not found: " + accountNumber);
            account.withdraw(amount);
            return recordTransaction(accountNumber, null, amount, Transaction.TransactionType.WITHDRAWAL,
                "Withdrawal");
        }

        public Transaction transfer(String fromAccountNum, String toAccountNum, double amount) {
            if (fromAccountNum.equals(toAccountNum)) {
                throw new IllegalArgumentException("Cannot transfer to same account");
            }
            var fromAcct = accounts.get(fromAccountNum);
            var toAcct = accounts.get(toAccountNum);
            if (fromAcct == null) throw new IllegalArgumentException("Source account not found");
            if (toAcct == null) throw new IllegalArgumentException("Destination account not found");

            // Deadlock-free lock acquisition
            fromAcct.acquireLocks(toAcct);
            try {
                if (fromAcct.getBalance() < amount) {
                    throw new IllegalStateException("Insufficient funds in " + fromAccountNum);
                }
                fromAcct.unsafeWithdraw(amount);
                toAcct.unsafeDeposit(amount);

                String txId = "TXN-" + transactionCounter.incrementAndGet();
                var ts = LocalDateTime.now();
                var txOut = new Transaction(txId, fromAccountNum, toAccountNum, amount, ts,
                    Transaction.TransactionType.TRANSFER_OUT, "Transfer to " + toAccountNum);
                var txIn = new Transaction(txId, fromAccountNum, toAccountNum, amount, ts,
                    Transaction.TransactionType.TRANSFER_IN, "Transfer from " + fromAccountNum);

                transactionsByAccount.get(fromAccountNum).add(txOut);
                transactionsByAccount.get(toAccountNum).add(txIn);
                return txOut;
            } finally {
                fromAcct.releaseLocks(toAcct);
            }
        }

        public List<Transaction> getTransactionHistory(String accountNumber) {
            return List.copyOf(transactionsByAccount.getOrDefault(accountNumber, List.of()));
        }

        public AccountSummary getAccountSummary(String accountNumber) {
            var acct = accounts.get(accountNumber);
            if (acct == null) throw new IllegalArgumentException("Account not found");
            return new AccountSummary(acct.getAccountNumber(), acct.getOwnerName(), acct.getAccountType(),
                acct.getBalance(), acct.getWithdrawalLimit(), acct.getMonthlyFee(), acct.getCreatedAt());
        }

        public List<AccountSummary> getAllAccounts() {
            return accounts.values().stream()
                .map(a -> new AccountSummary(a.getAccountNumber(), a.getOwnerName(), a.getAccountType(),
                    a.getBalance(), a.getWithdrawalLimit(), a.getMonthlyFee(), a.getCreatedAt()))
                .sorted(Comparator.comparing(AccountSummary::accountNumber))
                .collect(Collectors.toList());
        }

        public double getTotalBankBalance() {
            return accounts.values().stream().mapToDouble(Account::getBalance).sum();
        }

        private Transaction recordTransaction(String acctNum, String toAcct, double amount,
                                               Transaction.TransactionType type, String desc) {
            String txId = "TXN-" + transactionCounter.incrementAndGet();
            var tx = new Transaction(txId, acctNum, toAcct, amount, LocalDateTime.now(), type, desc);
            transactionsByAccount.computeIfAbsent(acctNum, k -> new CopyOnWriteArrayList<>()).add(tx);
            return tx;
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Banking System ===\n");

        Bank bank = new Bank("National Java Bank");

        // ─── Create Accounts ───
        System.out.println("--- Account Creation ---");
        var aliceSavings = bank.createSavingsAccount("Alice Johnson", 5000.0, 0.035);
        var bobChecking = bank.createCheckingAccount("Bob Smith", 3000.0, 1000.0);
        var charlieChecking = bank.createCheckingAccount("Charlie Brown", 10000.0, 2000.0);

        System.out.println("  Alice: " + aliceSavings.getAccountNumber() + " (Savings) - $" + aliceSavings.getBalance());
        System.out.println("  Bob: " + bobChecking.getAccountNumber() + " (Checking) - $" + bobChecking.getBalance());
        System.out.println("  Charlie: " + charlieChecking.getAccountNumber() + " (Checking) - $" + charlieChecking.getBalance());

        // ─── Deposit ───
        System.out.println("\n--- Deposits ---");
        bank.deposit(aliceSavings.getAccountNumber(), 1000.0);
        bank.deposit(bobChecking.getAccountNumber(), 500.0);
        System.out.println("  Alice balance: $" + aliceSavings.getBalance());
        System.out.println("  Bob balance: $" + bobChecking.getBalance());

        // ─── Withdraw ───
        System.out.println("\n--- Withdrawals ---");
        bank.withdraw(aliceSavings.getAccountNumber(), 2000.0);
        System.out.println("  Alice withdrew $2000, balance: $" + aliceSavings.getBalance());

        // Test overdraft
        try {
            bank.withdraw(aliceSavings.getAccountNumber(), 10000.0);
        } catch (Exception e) {
            System.out.println("  Alice cannot withdraw $10000: " + e.getMessage());
        }

        // Checking overdraft
        bank.withdraw(bobChecking.getAccountNumber(), 3500.0);
        System.out.println("  Bob withdrew $3500 (overdraft), balance: $" + bobChecking.getBalance());

        // ─── Transfer ───
        System.out.println("\n--- Transfers ---");
        var tx = bank.transfer(aliceSavings.getAccountNumber(), charlieChecking.getAccountNumber(), 1500.0);
        System.out.println("  Transferred $1500 from Alice to Charlie");
        System.out.println("  Alice balance: $" + aliceSavings.getBalance());
        System.out.println("  Charlie balance: $" + charlieChecking.getBalance());

        // ─── Transaction History ───
        System.out.println("\n--- Alice's Transaction History ---");
        for (var t : bank.getTransactionHistory(aliceSavings.getAccountNumber())) {
            System.out.printf("  [%s] %s: $%.2f (%s)%n", t.timestamp(), t.type(), t.amount(), t.description());
        }

        // ─── Account Summary ───
        System.out.println("\n--- Charlie's Account Summary ---");
        var summary = bank.getAccountSummary(charlieChecking.getAccountNumber());
        System.out.println("  Account: " + summary.accountNumber());
        System.out.println("  Type: " + summary.type());
        System.out.println("  Balance: $" + summary.balance());
        System.out.println("  Withdrawal Limit: $" + summary.withdrawalLimit());
        System.out.println("  Monthly Fee: $" + summary.monthlyFee());

        // ─── Concurrent Transfers ───
        System.out.println("\n--- Concurrent Transfers (Virtual Threads) ---");
        var vtBank = new Bank("Virtual Thread Bank");
        var acct1 = vtBank.createSavingsAccount("User1", 10000, 0.02);
        var acct2 = vtBank.createCheckingAccount("User2", 10000, 5000);

        var vtThreads = new Thread[20];
        var transferResults = new AtomicInteger(0);
        var failedTransfers = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            vtThreads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 50; j++) {
                    double amount = ThreadLocalRandom.current().nextDouble(10, 200);
                    try {
                        boolean fromFirst = ThreadLocalRandom.current().nextBoolean();
                        if (fromFirst) {
                            vtBank.transfer(acct1.getAccountNumber(), acct2.getAccountNumber(), amount);
                        } else {
                            vtBank.transfer(acct2.getAccountNumber(), acct1.getAccountNumber(), amount);
                        }
                        transferResults.incrementAndGet();
                    } catch (Exception e) {
                        failedTransfers.incrementAndGet();
                    }
                }
            });
        }
        for (var t : vtThreads) t.join();

        System.out.println("  Successful transfers: " + transferResults.get());
        System.out.println("  Failed transfers: " + failedTransfers.get());
        System.out.println("  Acct1 balance: $" + acct1.getBalance());
        System.out.println("  Acct2 balance: $" + acct2.getBalance());
        System.out.println("  Combined: $" + (acct1.getBalance() + acct2.getBalance()) + " (expected: $20000)");

        // ─── Bank Summary ───
        System.out.println("\n--- Bank Summary ---");
        System.out.println("  " + bank.getName());
        System.out.println("  Total Accounts: " + bank.getAllAccounts().size());
        System.out.println("  Total Balance: $" + String.format("%.2f", bank.getTotalBankBalance()));

        System.out.println("\n=== Banking System Complete ===");
    }
}
