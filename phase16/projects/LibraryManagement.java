package phase16.projects;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * LibraryManagement.java
 *
 * Comprehensive library management system with Book (record), Member,
 * Library operations (add/remove/borrow/return/search), and fine calculation.
 * Fully working mini app with menu-driven demo.
 */
public class LibraryManagement {

    // ═══════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════

    record Book(String isbn, String title, String author, String genre, int totalCopies, int availableCopies) {
        Book {
            availableCopies = Math.min(availableCopies, totalCopies);
        }
        Book withAvailableCopies(int newAvailable) {
            return new Book(isbn, title, author, genre, totalCopies, newAvailable);
        }
    }

    record Member(String memberId, String name, String email, String phone) {}

    record BorrowRecord(String borrowId, String isbn, String memberId, LocalDate borrowDate, LocalDate dueDate,
                        LocalDate returnDate, double finePaid) {
        public boolean isOverdue() {
            return returnDate == null && LocalDate.now().isAfter(dueDate);
        }

        public long overdueDays() {
            if (returnDate != null) return ChronoUnit.DAYS.between(dueDate, returnDate);
            long days = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
            return Math.max(0, days);
        }

        public double calculateFine(double dailyRate) {
            return Math.max(0, overdueDays() * dailyRate);
        }
    }

    record SearchResult(Book book, boolean available, int borrowCount) {}

    record LibraryStats(int totalBooks, int totalMembers, int activeBorrows, int overdueBorrows,
                        double totalFinesCollected) {}

    // ═══════════════════════════════════════════════
    // Library System
    // ═══════════════════════════════════════════════

    static final class Library {
        private final ConcurrentHashMap<String, Book> booksByIsbn = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Member> membersById = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Member> membersByEmail = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<BorrowRecord>> borrowsByMember = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<BorrowRecord>> borrowsByBook = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, BorrowRecord> borrowsById = new ConcurrentHashMap<>();
        private final AtomicInteger borrowCounter = new AtomicInteger(0);
        private final AtomicInteger memberCounter = new AtomicInteger(0);
        private final double DAILY_FINE_RATE = 0.50;

        // ─── Book Management ───

        public Book addBook(String isbn, String title, String author, String genre, int totalCopies) {
            var book = new Book(isbn, title, author, genre, totalCopies, totalCopies);
            booksByIsbn.put(isbn, book);
            return book;
        }

        public boolean removeBook(String isbn) {
            var book = booksByIsbn.get(isbn);
            if (book == null) return false;
            var activeBorrows = borrowsByBook.getOrDefault(isbn, List.of()).stream()
                .filter(b -> b.returnDate() == null).count();
            if (activeBorrows > 0) return false;
            booksByIsbn.remove(isbn);
            return true;
        }

        public Optional<Book> findBookByIsbn(String isbn) {
            return Optional.ofNullable(booksByIsbn.get(isbn));
        }

        public List<Book> searchBooks(String query) {
            String q = query.toLowerCase();
            return booksByIsbn.values().stream()
                .filter(b -> b.title().toLowerCase().contains(q)
                    || b.author().toLowerCase().contains(q)
                    || b.genre().toLowerCase().contains(q)
                    || b.isbn().contains(q))
                .collect(Collectors.toList());
        }

        public List<SearchResult> searchWithAvailability(String query) {
            String q = query.toLowerCase();
            return booksByIsbn.values().stream()
                .filter(b -> b.title().toLowerCase().contains(q)
                    || b.author().toLowerCase().contains(q)
                    || b.genre().toLowerCase().contains(q)
                    || b.isbn().contains(q))
                .map(b -> new SearchResult(b, b.availableCopies() > 0,
                    borrowsByBook.getOrDefault(b.isbn(), List.of()).size()))
                .sorted(Comparator.comparing(r -> r.book().title()))
                .collect(Collectors.toList());
        }

        // ─── Member Management ───

        public Member registerMember(String name, String email, String phone) {
            if (membersByEmail.containsKey(email)) {
                throw new IllegalArgumentException("Email already registered: " + email);
            }
            String id = "MEM-" + memberCounter.incrementAndGet();
            var member = new Member(id, name, email, phone);
            membersById.put(id, member);
            membersByEmail.put(email, member);
            borrowsByMember.put(id, new CopyOnWriteArrayList<>());
            return member;
        }

        public Optional<Member> findMember(String memberId) {
            return Optional.ofNullable(membersById.get(memberId));
        }

        public Optional<Member> findMemberByEmail(String email) {
            return Optional.ofNullable(membersByEmail.get(email));
        }

        // ─── Borrow Operations ───

        public BorrowRecord borrowBook(String isbn, String memberId) {
            var book = booksByIsbn.get(isbn);
            if (book == null) throw new IllegalArgumentException("Book not found: " + isbn);
            if (book.availableCopies() <= 0) throw new IllegalStateException("No copies available: " + isbn);

            var member = membersById.get(memberId);
            if (member == null) throw new IllegalArgumentException("Member not found: " + memberId);

            // Check for overdue books by this member
            var memberBorrows = borrowsByMember.getOrDefault(memberId, List.of());
            long overdueCount = memberBorrows.stream().filter(b -> b.returnDate() == null && b.isOverdue()).count();
            if (overdueCount >= 3) {
                throw new IllegalStateException("Member has " + overdueCount + " overdue books. Cannot borrow more.");
            }

            // Update book availability
            booksByIsbn.put(isbn, book.withAvailableCopies(book.availableCopies() - 1));

            String borrowId = "BRW-" + borrowCounter.incrementAndGet();
            var record = new BorrowRecord(borrowId, isbn, memberId, LocalDate.now(),
                LocalDate.now().plusDays(14), null, 0);

            borrowsById.put(borrowId, record);
            borrowsByMember.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(record);
            borrowsByBook.computeIfAbsent(isbn, k -> new CopyOnWriteArrayList<>()).add(record);

            return record;
        }

        public BorrowReturnResult returnBook(String borrowId) {
            var record = borrowsById.get(borrowId);
            if (record == null) throw new IllegalArgumentException("Borrow record not found: " + borrowId);
            if (record.returnDate() != null) throw new IllegalStateException("Book already returned");

            LocalDate returnDate = LocalDate.now();
            double fine = record.calculateFine(DAILY_FINE_RATE);

            var updated = new BorrowRecord(borrowId, record.isbn(), record.memberId(),
                record.borrowDate(), record.dueDate(), returnDate, fine);
            borrowsById.put(borrowId, updated);

            // Update list entries (replace old with new)
            replaceBorrowInList(borrowsByMember.get(record.memberId()), record, updated);
            replaceBorrowInList(borrowsByBook.get(record.isbn()), record, updated);

            // Restore book availability
            var book = booksByIsbn.get(record.isbn());
            if (book != null) {
                booksByIsbn.put(record.isbn(), book.withAvailableCopies(book.availableCopies() + 1));
            }

            return new BorrowReturnResult(updated, fine);
        }

        record BorrowReturnResult(BorrowRecord record, double fine) {}

        private void replaceBorrowInList(List<BorrowRecord> list, BorrowRecord old, BorrowRecord updated) {
            if (list != null) {
                int idx = list.indexOf(old);
                if (idx >= 0) list.set(idx, updated);
            }
        }

        // ─── Fine Management ───

        public double calculateFine(String borrowId) {
            var record = borrowsById.get(borrowId);
            if (record == null) throw new IllegalArgumentException("Borrow record not found");
            return record.calculateFine(DAILY_FINE_RATE);
        }

        public List<BorrowRecord> getOverdueBorrows() {
            return borrowsById.values().stream()
                .filter(b -> b.returnDate() == null && b.isOverdue())
                .sorted(Comparator.comparing(BorrowRecord::dueDate))
                .collect(Collectors.toList());
        }

        // ─── Reports ───

        public LibraryStats getStats() {
            int totalBooks = booksByIsbn.size();
            int totalMembers = membersById.size();
            long activeBorrows = borrowsById.values().stream().filter(b -> b.returnDate() == null).count();
            long overdueBorrows = borrowsById.values().stream().filter(b -> b.returnDate() == null && b.isOverdue()).count();
            double totalFines = borrowsById.values().stream().mapToDouble(BorrowRecord::finePaid).sum();
            return new LibraryStats(totalBooks, totalMembers, (int) activeBorrows, (int) overdueBorrows, totalFines);
        }

        public List<BorrowRecord> getBorrowsByMember(String memberId) {
            return List.copyOf(borrowsByMember.getOrDefault(memberId, List.of()));
        }

        public List<BorrowRecord> getBorrowsByBook(String isbn) {
            return List.copyOf(borrowsByBook.getOrDefault(isbn, List.of()));
        }

        public List<Book> getAllBooks() {
            return booksByIsbn.values().stream()
                .sorted(Comparator.comparing(Book::title))
                .collect(Collectors.toList());
        }

        public List<Member> getAllMembers() {
            return membersById.values().stream()
                .sorted(Comparator.comparing(Member::name))
                .collect(Collectors.toList());
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Library Management System ===\n");

        Library lib = new Library();

        // ─── Add Books ───
        System.out.println("--- Adding Books ---");
        lib.addBook("978-0-13-468599-1", "The Great Gatsby", "F. Scott Fitzgerald", "Fiction", 5);
        lib.addBook("978-0-06-112008-4", "To Kill a Mockingbird", "Harper Lee", "Fiction", 3);
        lib.addBook("978-0-452-28423-4", "1984", "George Orwell", "Dystopian", 4);
        lib.addBook("978-0-14-103614-4", "Animal Farm", "George Orwell", "Satire", 2);
        lib.addBook("978-0-262-13472-9", "Structure and Interpretation of Computer Programs", "Harold Abelson", "CS", 1);
        lib.addBook("978-0-13-110362-7", "The C Programming Language", "Brian Kernighan", "CS", 2);
        System.out.println("  Added 6 books");

        // ─── Register Members ───
        System.out.println("\n--- Registering Members ---");
        var alice = lib.registerMember("Alice Johnson", "alice@email.com", "555-0101");
        var bob = lib.registerMember("Bob Smith", "bob@email.com", "555-0102");
        var charlie = lib.registerMember("Charlie Brown", "charlie@email.com", "555-0103");
        System.out.println("  Registered: " + alice.name() + " (" + alice.memberId() + ")");
        System.out.println("  Registered: " + bob.name() + " (" + bob.memberId() + ")");
        System.out.println("  Registered: " + charlie.name() + " (" + charlie.memberId() + ")");

        // ─── Search Books ───
        System.out.println("\n--- Search Books ---");
        var results = lib.searchWithAvailability("George Orwell");
        System.out.println("  Search 'George Orwell':");
        for (var r : results) {
            System.out.println("    " + r.book().title() + " (" + r.book().isbn() + ") - "
                + (r.available() ? "Available" : "Checked out") + " [borrowed " + r.borrowCount() + " times]");
        }

        // ─── Borrow Books ───
        System.out.println("\n--- Borrowing Books ---");
        var br1 = lib.borrowBook("978-0-452-28423-4", alice.memberId());
        System.out.println("  Alice borrowed '1984' (due: " + br1.dueDate() + ")");

        var br2 = lib.borrowBook("978-0-13-468599-1", bob.memberId());
        System.out.println("  Bob borrowed 'The Great Gatsby' (due: " + br2.dueDate() + ")");

        var br3 = lib.borrowBook("978-0-06-112008-4", alice.memberId());
        System.out.println("  Alice borrowed 'To Kill a Mockingbird' (due: " + br3.dueDate() + ")");

        // Try borrowing unavailable
        try {
            lib.borrowBook("978-0-262-13472-9", alice.memberId());
            System.out.println("  Alice borrowed SICP");
        } catch (Exception e) {
            System.out.println("  Alice cannot borrow more: " + e.getMessage());
        }

        // ─── Return Books ───
        System.out.println("\n--- Returning Books ---");
        var ret1 = lib.returnBook(br1.borrowId());
        System.out.println("  Alice returned '1984' - Fine: $" + String.format("%.2f", ret1.fine()));
        System.out.println("  Return date: " + ret1.record().returnDate());

        // ─── Check availability after return ───
        var book1984 = lib.findBookByIsbn("978-0-452-28423-4").orElseThrow();
        System.out.println("  '1984' availability: " + book1984.availableCopies() + "/" + book1984.totalCopies());

        // ─── Member Borrow History ───
        System.out.println("\n--- Alice's Borrow History ---");
        for (var br : lib.getBorrowsByMember(alice.memberId())) {
            String status = br.returnDate() != null ? "Returned " + br.returnDate()
                : "Due " + br.dueDate() + (br.isOverdue() ? " (OVERDUE)" : "");
            System.out.println("  " + br.isbn() + " - Borrowed: " + br.borrowDate() + " " + status);
        }

        // ─── Statistics ───
        System.out.println("\n--- Library Statistics ---");
        var stats = lib.getStats();
        System.out.println("  Total Books: " + stats.totalBooks());
        System.out.println("  Total Members: " + stats.totalMembers());
        System.out.println("  Active Borrows: " + stats.activeBorrows());
        System.out.println("  Overdue: " + stats.overdueBorrows());
        System.out.println("  Fines Collected: $" + String.format("%.2f", stats.totalFinesCollected()));

        // ─── All Books ───
        System.out.println("\n--- All Books ---");
        for (var b : lib.getAllBooks()) {
            System.out.printf("  %-50s %s (%d/%d)%n", b.title(), b.isbn(), b.availableCopies(), b.totalCopies());
        }

        System.out.println("\n=== Library Management Complete ===");
    }
}
