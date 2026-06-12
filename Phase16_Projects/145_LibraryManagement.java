package phase16.projects;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

final class LibraryManagement {

    public sealed interface LibraryItem permits Book, DVD, Magazine {
        String getId();
        String getTitle();
        boolean isAvailable();
        void setAvailable(boolean available);
    }

    public static final class Book implements LibraryItem {
        private final String id;
        private final String title;
        private final String author;
        private final String isbn;
        private volatile boolean available;

        public Book(String id, String title, String author, String isbn) {
            this.id = Objects.requireNonNull(id);
            this.title = Objects.requireNonNull(title);
            this.author = Objects.requireNonNull(author);
            this.isbn = Objects.requireNonNull(isbn);
            this.available = true;
        }

        @Override public String getId() { return id; }
        @Override public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getIsbn() { return isbn; }
        @Override public boolean isAvailable() { return available; }
        @Override public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return "Book{id='%s', title='%s', author='%s', isbn='%s', available=%s}"
                    .formatted(id, title, author, isbn, available);
        }
    }

    public static final class DVD implements LibraryItem {
        private final String id;
        private final String title;
        private final String director;
        private final int runtimeMinutes;
        private volatile boolean available;

        public DVD(String id, String title, String director, int runtimeMinutes) {
            this.id = Objects.requireNonNull(id);
            this.title = Objects.requireNonNull(title);
            this.director = Objects.requireNonNull(director);
            this.runtimeMinutes = runtimeMinutes;
            this.available = true;
        }

        @Override public String getId() { return id; }
        @Override public String getTitle() { return title; }
        public String getDirector() { return director; }
        public int getRuntimeMinutes() { return runtimeMinutes; }
        @Override public boolean isAvailable() { return available; }
        @Override public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return "DVD{id='%s', title='%s', director='%s', runtime=%dmin, available=%s}"
                    .formatted(id, title, director, runtimeMinutes, available);
        }
    }

    public static final class Magazine implements LibraryItem {
        private final String id;
        private final String title;
        private final int issueNumber;
        private volatile boolean available;

        public Magazine(String id, String title, int issueNumber) {
            this.id = Objects.requireNonNull(id);
            this.title = Objects.requireNonNull(title);
            this.issueNumber = issueNumber;
            this.available = true;
        }

        @Override public String getId() { return id; }
        @Override public String getTitle() { return title; }
        public int getIssueNumber() { return issueNumber; }
        @Override public boolean isAvailable() { return available; }
        @Override public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return "Magazine{id='%s', title='%s', issue=%d, available=%s}"
                    .formatted(id, title, issueNumber, available);
        }
    }

    public static record Member(String memberId, String name, String email) {
        public Member {
            Objects.requireNonNull(memberId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(email);
        }
    }

    public static record BorrowRecord(String recordId, String itemId, String memberId,
                                       LocalDate borrowDate, LocalDate dueDate,
                                       LocalDate returnDate, double finePaid) {
        public BorrowRecord {
            Objects.requireNonNull(recordId);
            Objects.requireNonNull(itemId);
            Objects.requireNonNull(memberId);
            Objects.requireNonNull(borrowDate);
            Objects.requireNonNull(dueDate);
        }

        public boolean isOverdue() {
            return returnDate == null && LocalDate.now().isAfter(dueDate);
        }

        public boolean isReturned() {
            return returnDate != null;
        }

        public long daysOverdue() {
            LocalDate checkDate = returnDate != null ? returnDate : LocalDate.now();
            if (checkDate.isAfter(dueDate)) {
                return ChronoUnit.DAYS.between(dueDate, checkDate);
            }
            return 0;
        }

        public double calculateFine(double finePerDay) {
            return daysOverdue() * finePerDay;
        }

        public BorrowRecord withReturned(LocalDate returned, double fine) {
            return new BorrowRecord(recordId, itemId, memberId, borrowDate, dueDate, returned, fine);
        }
    }

    public static final class Library {
        private final String name;
        private final Map<String, LibraryItem> catalog = new ConcurrentHashMap<>();
        private final Map<String, Member> members = new ConcurrentHashMap<>();
        private final List<BorrowRecord> borrowRecords = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong recordIdCounter = new AtomicLong(0);
        private final double finePerDay;

        public Library(String name, double finePerDay) {
            this.name = Objects.requireNonNull(name);
            this.finePerDay = finePerDay;
        }

        public void addItem(LibraryItem item) {
            catalog.put(item.getId(), item);
        }

        public void addMember(Member member) {
            members.put(member.memberId(), member);
        }

        public LibraryItem getItem(String id) {
            return catalog.get(id);
        }

        public Member getMember(String memberId) {
            return members.get(memberId);
        }

        public synchronized BorrowRecord borrowItem(String memberId, String itemId) {
            var member = members.get(memberId);
            var item = catalog.get(itemId);
            if (member == null) throw new IllegalArgumentException("Member not found: " + memberId);
            if (item == null) throw new IllegalArgumentException("Item not found: " + itemId);
            if (!item.isAvailable()) throw new IllegalStateException("Item already borrowed: " + itemId);

            var hasUnreturned = borrowRecords.stream()
                    .anyMatch(r -> r.memberId().equals(memberId) && !r.isReturned() && r.isOverdue());
            if (hasUnreturned) {
                double totalFine = calculateTotalFine(memberId);
                if (totalFine > 10.0) {
                    throw new IllegalStateException("Member %s has unpaid fines of $%.2f. Max allowed: $10.00"
                            .formatted(memberId, totalFine));
                }
            }

            item.setAvailable(false);
            var recordId = "BR-%06d".formatted(recordIdCounter.incrementAndGet());
            var borrowDate = LocalDate.now();
            var dueDate = borrowDate.plusDays(14);
            var record = new BorrowRecord(recordId, itemId, memberId, borrowDate, dueDate, null, 0.0);
            borrowRecords.add(record);
            return record;
        }

        public synchronized BorrowRecord returnItem(String recordId) {
            var index = -1;
            for (int i = 0; i < borrowRecords.size(); i++) {
                if (borrowRecords.get(i).recordId().equals(recordId) && !borrowRecords.get(i).isReturned()) {
                    index = i;
                    break;
                }
            }
            if (index == -1) throw new IllegalArgumentException("Active record not found: " + recordId);

            var current = borrowRecords.get(index);
            var fine = current.calculateFine(finePerDay);
            var returned = current.withReturned(LocalDate.now(), fine);

            var item = catalog.get(current.itemId());
            if (item != null) item.setAvailable(true);

            borrowRecords.set(index, returned);
            return returned;
        }

        public List<BorrowRecord> getBorrowHistory(String memberId) {
            return borrowRecords.stream()
                    .filter(r -> r.memberId().equals(memberId))
                    .collect(Collectors.toUnmodifiableList());
        }

        public List<BorrowRecord> getActiveBorrows(String memberId) {
            return borrowRecords.stream()
                    .filter(r -> r.memberId().equals(memberId) && !r.isReturned())
                    .collect(Collectors.toUnmodifiableList());
        }

        public double calculateTotalFine(String memberId) {
            return borrowRecords.stream()
                    .filter(r -> r.memberId().equals(memberId) && !r.isReturned() && r.isOverdue())
                    .mapToDouble(r -> r.calculateFine(finePerDay))
                    .sum();
        }

        public List<LibraryItem> searchByTitle(String query) {
            var lower = query.toLowerCase();
            return catalog.values().stream()
                    .filter(i -> i.getTitle().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        public List<LibraryItem> getAvailableItems() {
            return catalog.values().stream()
                    .filter(LibraryItem::isAvailable)
                    .collect(Collectors.toList());
        }

        public List<LibraryItem> getBorrowedItems() {
            return catalog.values().stream()
                    .filter(i -> !i.isAvailable())
                    .collect(Collectors.toList());
        }

        public String getName() { return name; }
        public double getFinePerDay() { return finePerDay; }
        public int totalItems() { return catalog.size(); }
        public int totalMembers() { return members.size(); }
    }

    public static void main(String[] args) {
        System.out.println("=== Library Management System ===%n".formatted());

        var library = new Library("Central Library", 0.50);

        var book1 = new Book("B-001", "The Great Gatsby", "F. Scott Fitzgerald", "9780743273565");
        var book2 = new Book("B-002", "1984", "George Orwell", "9780451524935");
        var book3 = new Book("B-003", "To Kill a Mockingbird", "Harper Lee", "9780061120084");
        var dvd1 = new DVD("D-001", "Inception", "Christopher Nolan", 148);
        var mag1 = new Magazine("M-001", "National Geographic", 202);

        library.addItem(book1);
        library.addItem(book2);
        library.addItem(book3);
        library.addItem(dvd1);
        library.addItem(mag1);

        var alice = new Member("M-001", "Alice Johnson", "alice@email.com");
        var bob = new Member("M-002", "Bob Smith", "bob@email.com");
        library.addMember(alice);
        library.addMember(bob);

        System.out.println("--- Catalog ---");
        var availableItems = library.getAvailableItems();
        availableItems.forEach(i -> System.out.println("  " + i));

        System.out.println("%n--- Borrowing ---%n".formatted());
        var record1 = library.borrowItem("M-001", "B-001");
        System.out.println("Alice borrowed: " + record1);

        var record2 = library.borrowItem("M-002", "B-002");
        System.out.println("Bob borrowed: " + record2);

        System.out.println("%n--- Available After Borrow ---%n".formatted());
        library.getAvailableItems().forEach(i -> System.out.println("  " + i));

        System.out.println("%n--- Returning with Fine ---%n".formatted());
        var returned1 = library.returnItem(record1.recordId());
        System.out.println("Returned: " + returned1);

        System.out.println("%n--- Search Results for 'the' ---%n".formatted());
        var results = library.searchByTitle("the");
        results.forEach(i -> System.out.println("  " + i));

        System.out.println("%n--- Alice's Borrow History ---%n".formatted());
        library.getBorrowHistory("M-001").forEach(r -> System.out.println("  " + r));

        System.out.println("%n--- Using Pattern Matching on Items ---%n".formatted());
        for (var item : library.getAvailableItems()) {
            switch (item) {
                case Book b when b.getAuthor().contains("Lee") ->
                    System.out.println("  Matched: " + b.getTitle() + " by " + b.getAuthor());
                case Book b -> System.out.println("  Book: " + b.getTitle());
                case DVD d -> System.out.println("  DVD: " + d.getTitle() + " (" + d.getRuntimeMinutes() + "min)");
                case Magazine m -> System.out.println("  Magazine: " + m.getTitle() + " #" + m.getIssueNumber());
            }
        }

        System.out.println("%n--- Virtual Threads Demo: Concurrent Operations ---%n".formatted());
        var futures = new ArrayList<Thread>();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                int idx = i;
                var future = executor.submit(() -> {
                    var itemId = "B-%03d".formatted(idx + 4);
                    var bk = new Book(itemId, "Virtual Book " + idx, "Author " + idx, "000000000000" + idx);
                    library.addItem(bk);
                    var mId = "M-%03d".formatted(100 + idx);
                    library.addMember(new Member(mId, "VT User " + idx, "vt%d@email.com".formatted(idx)));
                    var rec = library.borrowItem(mId, itemId);
                    System.out.println("  [VT %d] Borrowed: %s".formatted(idx, rec));
                    return rec;
                });
            }
        } catch (Exception e) {
            System.out.println("Error in virtual threads: " + e.getMessage());
        }

        System.out.println("%nFinal Stats: %d items, %d members, %d borrow records"
                .formatted(library.totalItems(), library.totalMembers(), library.getBorrowHistory("M-001").size() + library.getBorrowHistory("M-002").size()));
        System.out.println("=== Done ===");
    }
}
